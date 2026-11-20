// com/rexofficial/rextestbenchmark/MainActivity.java
package com.rexofficial.rextestbenchmark;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText etUsername, etToken, etUrl, etThreads, etConnections, etDuration;
    private MaterialButton btnVerify, btnRunLocal, btnRunActions, btnPreview;
    private TextView tvStatus;
    private ExecutorService executor = Executors.newFixedThreadPool(4);
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    private GithubClient githubClient;
    private EncryptedSharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etUsername = findViewById(R.id.etUsername);
        etToken = findViewById(R.id.etToken);
        etUrl = findViewById(R.id.etUrl);
        etThreads = findViewById(R.id.etThreads);
        etConnections = findViewById(R.id.etConnections);
        etDuration = findViewById(R.id.etDuration);
        btnVerify = findViewById(R.id.btnVerify);
        btnRunLocal = findViewById(R.id.btnRunLocal);
        btnRunActions = findViewById(R.id.btnRunActions);
        btnPreview = findViewById(R.id.btnPreview);
        tvStatus = findViewById(R.id.tvStatus);

        try {
            MasterKey masterKey = new MasterKey.Builder(this)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            prefs = (EncryptedSharedPreferences) EncryptedSharedPreferences.create(
                    this,
                    "rex_secure_prefs",
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            etUsername.setText(prefs.getString("username", ""));
            etToken.setText(prefs.getString("token", ""));
        } catch (Exception e) {
            setStatus("secure prefs error: " + e.getMessage());
        }

        btnVerify.setOnClickListener(v -> verifyAndPush());
        btnRunLocal.setOnClickListener(v -> runLocal());
        btnRunActions.setOnClickListener(v -> runActions());
        btnPreview.setOnClickListener(v -> openPreview());
    }

    private void setStatus(String msg) {
        mainHandler.post(() -> tvStatus.setText("Status: " + msg));
    }

    private String getText(TextInputEditText et) {
        return et.getText() == null ? "" : et.getText().toString().trim();
    }

    private void verifyAndPush() {
        String user = getText(etUsername);
        String token = getText(etToken);
        if (user.isEmpty() || token.isEmpty()) {
            setStatus("username/token kosong");
            return;
        }
        prefs.edit().putString("username", user).putString("token", token).apply();
        setStatus("verifying token...");
        executor.execute(() -> {
            try {
                githubClient = new GithubClient(token);
                String login = githubClient.getLogin();
                if (login == null || login.isEmpty()) {
                    setStatus("token invalid");
                    return;
                }
                setStatus("token valid: " + login);
                boolean exists = githubClient.repoExists(login, "RexApsTestBenchmark");
                if (!exists) {
                    setStatus("creating repo...");
                    boolean created = githubClient.createRepo("RexApsTestBenchmark");
                    if (!created) {
                        setStatus("gagal buat repo");
                        return;
                    }
                    setStatus("repo created");
                } else {
                    setStatus("repo exists");
                }
                setStatus("pushing workflow...");
                boolean pushed = GithubPusher.pushFiles(token, login, "RexApsTestBenchmark");
                if (pushed) {
                    setStatus("workflow pushed. siap run actions.");
                } else {
                    setStatus("push gagal");
                }
            } catch (Exception e) {
                setStatus("error: " + e.getMessage());
            }
        });
    }

    private void runLocal() {
        String url = getText(etUrl);
        int threads = parseInt(getText(etThreads), 10);
        int connections = parseInt(getText(etConnections), 100);
        int duration = parseInt(getText(etDuration), 30);
        if (url.isEmpty()) {
            setStatus("url kosong");
            return;
        }
        if (!url.startsWith("http")) {
            setStatus("url harus http/https");
            return;
        }
        setStatus("local benchmark jalan...");
        executor.execute(() -> {
            try {
                LocalBenchmark.BenchResult result = LocalBenchmark.run(url, threads, connections, duration);
                mainHandler.post(() -> {
                    Intent i = new Intent(MainActivity.this, ResultActivity.class);
                    i.putExtra("result_json", result.toJson());
                    i.putExtra("mode", "local");
                    startActivity(i);
                    setStatus("local selesai");
                });
            } catch (Exception e) {
                setStatus("local error: " + e.getMessage());
            }
        });
    }

    private void runActions() {
        String user = getText(etUsername);
        String token = getText(etToken);
        String url = getText(etUrl);
        int threads = parseInt(getText(etThreads), 10);
        int connections = parseInt(getText(etConnections), 100);
        int duration = parseInt(getText(etDuration), 30);
        if (user.isEmpty() || token.isEmpty()) {
            setStatus("isi credentials dulu");
            return;
        }
        if (url.isEmpty()) {
            setStatus("url kosong");
            return;
        }
        setStatus("dispatch workflow...");
        executor.execute(() -> {
            try {
                GithubClient client = new GithubClient(token);
                boolean ok = client.dispatchWorkflow(user, "RexApsTestBenchmark", "benchmark.yml",
                        url, threads, connections, duration);
                if (!ok) {
                    setStatus("dispatch gagal");
                    return;
                }
                setStatus("workflow jalan. polling...");
                String artifactText = client.pollAndDownloadResult(user, "RexApsTestBenchmark", duration);
                if (artifactText == null) {
                    setStatus("artifact gagal diambil");
                    return;
                }
                LocalBenchmark.BenchResult result = LocalBenchmark.parseWrkOutput(artifactText, "actions");
                mainHandler.post(() -> {
                    Intent i = new Intent(MainActivity.this, ResultActivity.class);
                    i.putExtra("result_json", result.toJson());
                    i.putExtra("mode", "actions");
                    startActivity(i);
                    setStatus("actions selesai");
                });
            } catch (Exception e) {
                setStatus("actions error: " + e.getMessage());
            }
        });
    }

    private void openPreview() {
        String url = getText(etUrl);
        if (url.isEmpty()) {
            setStatus("url kosong");
            return;
        }
        Intent i = new Intent(this, PreviewActivity.class);
        i.putExtra("url", url);
        startActivity(i);
    }

    private int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }
}