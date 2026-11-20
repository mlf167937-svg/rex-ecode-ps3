// com/rexofficial/rextestbenchmark/PreviewActivity.java
package com.rexofficial.rextestbenchmark;

import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import org.mozilla.geckoview.GeckoRuntime;
import org.mozilla.geckoview.GeckoSession;
import org.mozilla.geckoview.GeckoView;

public class PreviewActivity extends AppCompatActivity {

    private static GeckoRuntime runtime;
    private GeckoSession session;
    private GeckoView geckoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preview);

        String url = getIntent().getStringExtra("url");
        TextView tvUrl = findViewById(R.id.tvPreviewUrl);
        tvUrl.setText(url);
        FrameLayout container = findViewById(R.id.geckoContainer);
        MaterialButton btnClose = findViewById(R.id.btnClosePreview);
        btnClose.setOnClickListener(v -> finish());

        if (runtime == null) {
            runtime = GeckoRuntime.create(this);
        }
        session = new GeckoSession();
        session.open(runtime);
        geckoView = new GeckoView(this);
        geckoView.setSession(session);
        container.addView(geckoView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));
        if (url != null && !url.isEmpty()) {
            session.loadUri(url);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (session != null) {
            session.close();
            session = null;
        }
    }
}