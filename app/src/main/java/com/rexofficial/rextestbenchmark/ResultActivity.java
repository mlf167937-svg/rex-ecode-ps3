// com/rexofficial/rextestbenchmark/ResultActivity.java
package com.rexofficial.rextestbenchmark;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class ResultActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result);

        String json = getIntent().getStringExtra("result_json");
        LocalBenchmark.BenchResult r = LocalBenchmark.BenchResult.fromJson(json);

        RecyclerView rv = findViewById(R.id.rvResult);
        rv.setLayoutManager(new LinearLayoutManager(this));

        List<String[]> data = new ArrayList<>();
        data.add(new String[]{"mode", r.mode});
        data.add(new String[]{"target", r.target});
        data.add(new String[]{"threads", String.valueOf(r.threads)});
        data.add(new String[]{"connections", String.valueOf(r.connections)});
        data.add(new String[]{"duration", r.durationSec + "s"});
        data.add(new String[]{"total requests", String.valueOf(r.totalRequests)});
        data.add(new String[]{"errors", String.valueOf(r.errorCount)});
        data.add(new String[]{"requests/sec", String.format("%.2f", r.rps)});
        data.add(new String[]{"avg latency", String.format("%.2f ms", r.avgMs)});
        data.add(new String[]{"min latency", String.format("%.2f ms", r.minMs)});
        data.add(new String[]{"max latency", String.format("%.2f ms", r.maxMs)});
        data.add(new String[]{"p50", String.format("%.2f ms", r.p50Ms)});
        data.add(new String[]{"p90", String.format("%.2f ms", r.p90Ms)});
        data.add(new String[]{"p99", String.format("%.2f ms", r.p99Ms)});

        rv.setAdapter(new ResultAdapter(data));

        MaterialButton btnCopy = findViewById(R.id.btnCopyResult);
        MaterialButton btnClose = findViewById(R.id.btnCloseResult);
        btnCopy.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            for (String[] d : data) {
                sb.append(d[0]).append(": ").append(d[1]).append("\n");
            }
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("benchmark", sb.toString()));
            Toast.makeText(this, "copied", Toast.LENGTH_SHORT).show();
        });
        btnClose.setOnClickListener(v -> finish());
    }

    static class ResultAdapter extends RecyclerView.Adapter<ResultAdapter.VH> {
        private final List<String[]> data;

        ResultAdapter(List<String[]> data) {
            this.data = data;
        }

        @Override
        public VH onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            android.view.View v = android.view.LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_result, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH holder, int position) {
            String[] d = data.get(position);
            holder.tvKey.setText(d[0]);
            holder.tvValue.setText(d[1]);
        }

        @Override
        public int getItemCount() {
            return data.size();
        }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvKey, tvValue;

            VH(android.view.View v) {
                super(v);
                tvKey = v.findViewById(R.id.tvKey);
                tvValue = v.findViewById(R.id.tvValue);
            }
        }
    }
}