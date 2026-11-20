// com/rexofficial/rextestbenchmark/GithubClient.java
package com.rexofficial.rextestbenchmark;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GithubClient {

    private static final String API = "https://api.github.com";
    private final String token;
    private final OkHttpClient http;

    public GithubClient(String token) {
        this.token = token;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();
    }

    private Request.Builder authBuilder(String url) {
        return new Request.Builder()
                .url(url)
                .header("Authorization", "token " + token)
                .header("Accept", "application/vnd.github+json")
                .header("X-GitHub-Api-Version", "2022-11-28");
    }

    public String getLogin() throws IOException {
        Request req = authBuilder(API + "/user").get().build();
        try (Response res = http.newCall(req).execute()) {
            if (!res.isSuccessful() || res.body() == null) return null;
            JsonObject obj = JsonParser.parseString(res.body().string()).getAsJsonObject();
            return obj.has("login") ? obj.get("login").getAsString() : null;
        }
    }

    public boolean repoExists(String owner, String repo) throws IOException {
        Request req = authBuilder(API + "/repos/" + owner + "/" + repo).get().build();
        try (Response res = http.newCall(req).execute()) {
            return res.isSuccessful();
        }
    }

    public boolean createRepo(String name) throws IOException {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        body.addProperty("private", false);
        body.addProperty("auto_init", false);
        Request req = authBuilder(API + "/user/repos")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();
        try (Response res = http.newCall(req).execute()) {
            return res.isSuccessful();
        }
    }

    public boolean dispatchWorkflow(String owner, String repo, String workflowFile,
                                    String target, int threads, int connections, int duration) throws IOException {
        JsonObject inputs = new JsonObject();
        inputs.addProperty("target", target);
        inputs.addProperty("threads", String.valueOf(threads));
        inputs.addProperty("connections", String.valueOf(connections));
        inputs.addProperty("duration", String.valueOf(duration));
        JsonObject body = new JsonObject();
        body.addProperty("ref", "main");
        body.add("inputs", inputs);
        Request req = authBuilder(API + "/repos/" + owner + "/" + repo + "/actions/workflows/" + workflowFile + "/dispatches")
                .post(RequestBody.create(body.toString(), MediaType.parse("application/json")))
                .build();
        try (Response res = http.newCall(req).execute()) {
            return res.isSuccessful();
        }
    }

    public String pollAndDownloadResult(String owner, String repo, int duration) throws IOException, InterruptedException {
        long deadline = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(10);
        long runId = -1;
        while (System.currentTimeMillis() < deadline) {
            Request req = authBuilder(API + "/repos/" + owner + "/" + repo + "/actions/runs?per_page=5").get().build();
            try (Response res = http.newCall(req).execute()) {
                if (res.isSuccessful() && res.body() != null) {
                    JsonObject obj = JsonParser.parseString(res.body().string()).getAsJsonObject();
                    JsonArray runs = obj.getAsJsonArray("workflow_runs");
                    if (runs != null && runs.size() > 0) {
                        JsonObject first = runs.get(0).getAsJsonObject();
                        String status = first.get("status").getAsString();
                        runId = first.get("id").getAsLong();
                        if ("completed".equals(status)) {
                            break;
                        }
                    }
                }
            }
            Thread.sleep(5000);
        }
        if (runId == -1) return null;
        Request artReq = authBuilder(API + "/repos/" + owner + "/" + repo + "/actions/runs/" + runId + "/artifacts").get().build();
        long artId = -1;
        try (Response res = http.newCall(artReq).execute()) {
            if (res.isSuccessful() && res.body() != null) {
                JsonObject obj = JsonParser.parseString(res.body().string()).getAsJsonObject();
                JsonArray arts = obj.getAsJsonArray("artifacts");
                if (arts != null && arts.size() > 0) {
                    artId = arts.get(0).getAsJsonObject().get("id").getAsLong();
                }
            }
        }
        if (artId == -1) return null;
        Request dlReq = authBuilder(API + "/repos/" + owner + "/" + repo + "/actions/artifacts/" + artId + "/zip").get().build();
        try (Response res = http.newCall(dlReq).execute()) {
            if (!res.isSuccessful() || res.body() == null) return null;
            InputStream in = res.body().byteStream();
            ZipInputStream zis = new ZipInputStream(in);
            ZipEntry entry;
            StringBuilder sb = new StringBuilder();
            byte[] buf = new byte[8192];
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().endsWith("result.txt")) {
                    int n;
                    while ((n = zis.read(buf)) > 0) {
                        sb.append(new String(buf, 0, n));
                    }
                }
            }
            return sb.toString();
        }
    }
}