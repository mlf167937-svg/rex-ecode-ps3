// com/rexofficial/rextestbenchmark/LocalBenchmark.java
package com.rexofficial.rextestbenchmark;

import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class LocalBenchmark {

    public static class BenchResult {
        public String mode;
        public String target;
        public int threads;
        public int connections;
        public int durationSec;
        public long totalRequests;
        public long errorCount;
        public double rps;
        public double avgMs;
        public double minMs;
        public double maxMs;
        public double p50Ms;
        public double p90Ms;
        public double p99Ms;
        public String raw;

        public String toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("mode", mode);
            o.addProperty("target", target);
            o.addProperty("threads", threads);
            o.addProperty("connections", connections);
            o.addProperty("durationSec", durationSec);
            o.addProperty("totalRequests", totalRequests);
            o.addProperty("errorCount", errorCount);
            o.addProperty("rps", rps);
            o.addProperty("avgMs", avgMs);
            o.addProperty("minMs", minMs);
            o.addProperty("maxMs", maxMs);
            o.addProperty("p50Ms", p50Ms);
            o.addProperty("p90Ms", p90Ms);
            o.addProperty("p99Ms", p99Ms);
            o.addProperty("raw", raw);
            return o.toString();
        }

        public static BenchResult fromJson(String json) {
            JsonObject o = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            BenchResult r = new BenchResult();
            r.mode = o.get("mode").getAsString();
            r.target = o.get("target").getAsString();
            r.threads = o.get("threads").getAsInt();
            r.connections = o.get("connections").getAsInt();
            r.durationSec = o.get("durationSec").getAsInt();
            r.totalRequests = o.get("totalRequests").getAsLong();
            r.errorCount = o.get("errorCount").getAsLong();
            r.rps = o.get("rps").getAsDouble();
            r.avgMs = o.get("avgMs").getAsDouble();
            r.minMs = o.get("minMs").getAsDouble();
            r.maxMs = o.get("maxMs").getAsDouble();
            r.p50Ms = o.get("p50Ms").getAsDouble();
            r.p90Ms = o.get("p90Ms").getAsDouble();
            r.p99Ms = o.get("p99Ms").getAsDouble();
            r.raw = o.get("raw").getAsString();
            return r;
        }
    }

    public static BenchResult run(String url, int threads, int connections, int durationSec) throws InterruptedException {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .build();
        Request request = new Request.Builder().url(url).get().build();

        AtomicLong total = new AtomicLong();
        AtomicLong errors = new AtomicLong();
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        long endTime = System.currentTimeMillis() + (durationSec * 1000L);

        int workerThreads = Math.max(1, threads);
        ExecutorService pool = Executors.newFixedThreadPool(workerThreads);
        for (int i = 0; i < workerThreads; i++) {
            pool.execute(() -> {
                while (System.currentTimeMillis() < endTime) {
                    long start = System.nanoTime();
                    try (Response res = client.newCall(request).execute()) {
                        if (!res.isSuccessful()) errors.incrementAndGet();
                    } catch (IOException e) {
                        errors.incrementAndGet();
                    }
                    long elapsed = System.nanoTime() - start;
                    total.incrementAndGet();
                    latencies.add(elapsed / 1000L);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(durationSec + 30, TimeUnit.SECONDS);

        BenchResult r = new BenchResult();
        r.mode = "local";
        r.target = url;
        r.threads = threads;
        r.connections = connections;
        r.durationSec = durationSec;
        r.totalRequests = total.get();
        r.errorCount = errors.get();
        r.rps = durationSec > 0 ? (double) r.totalRequests / durationSec : 0;
        if (!latencies.isEmpty()) {
            List<Long> sorted = new ArrayList<>(latencies);
            Collections.sort(sorted);
            long sum = 0;
            for (long v : sorted) sum += v;
            r.avgMs = (double) sum / sorted.size() / 1000.0;
            r.minMs = sorted.get(0) / 1000.0;
            r.maxMs = sorted.get(sorted.size() - 1) / 1000.0;
            r.p50Ms = percentile(sorted, 50) / 1000.0;
            r.p90Ms = percentile(sorted, 90) / 1000.0;
            r.p99Ms = percentile(sorted, 99) / 1000.0;
        }
        r.raw = "local benchmark done";
        return r;
    }

    private static long percentile(List<Long> sorted, int p) {
        if (sorted.isEmpty()) return 0;
        int idx = (int) Math.ceil((p / 100.0) * sorted.size()) - 1;
        if (idx < 0) idx = 0;
        if (idx >= sorted.size()) idx = sorted.size() - 1;
        return sorted.get(idx);
    }

    public static BenchResult parseWrkOutput(String text, String mode) {
        BenchResult r = new BenchResult();
        r.mode = mode;
        r.raw = text;
        Pattern targetPat = Pattern.compile("target=(\\S+)");
        Matcher mTarget = targetPat.matcher(text);
        if (mTarget.find()) r.target = mTarget.group(1);
        Pattern rpsPat = Pattern.compile("Requests/sec:\\s+([\\d.]+)");
        Matcher mRps = rpsPat.matcher(text);
        if (mRps.find()) r.rps = Double.parseDouble(mRps.group(1));
        Pattern latPat = Pattern.compile("Latency\\s+([\\d.]+)(\\w+)\\s+([\\d.]+)(\\w+)\\s+([\\d.]+)(\\w+)");
        Matcher mLat = latPat.matcher(text);
        if (mLat.find()) {
            r.avgMs = toMs(mLat.group(1), mLat.group(2));
            r.maxMs = toMs(mLat.group(5), mLat.group(6));
        }
        Pattern reqPat = Pattern.compile("(\\d+)\\s+requests in");
        Matcher mReq = reqPat.matcher(text);
        if (mReq.find()) r.totalRequests = Long.parseLong(mReq.group(1));
        Pattern threadPat = Pattern.compile("threads=(\\d+)");
        Matcher mThread = threadPat.matcher(text);
        if (mThread.find()) r.threads = Integer.parseInt(mThread.group(1));
        Pattern connPat = Pattern.compile("connections=(\\d+)");
        Matcher mConn = connPat.matcher(text);
        if (mConn.find()) r.connections = Integer.parseInt(mConn.group(1));
        Pattern durPat = Pattern.compile("duration=(\\d+)");
        Matcher mDur = durPat.matcher(text);
        if (mDur.find()) r.durationSec = Integer.parseInt(mDur.group(1));
        r.p50Ms = r.avgMs;
        r.p90Ms = r.avgMs;
        r.p99Ms = r.maxMs;
        r.minMs = 0;
        return r;
    }

    private static double toMs(String v, String unit) {
        double val = Double.parseDouble(v);
        if (unit.startsWith("m")) return val;
        if (unit.startsWith("u")) return val / 1000.0;
        if (unit.startsWith("s")) return val * 1000.0;
        return val;
    }
}