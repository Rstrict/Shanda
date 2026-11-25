package cn.rstrict.shanda;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.net.Uri;
import android.webkit.*;
import android.widget.Toast;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class MainActivity extends Activity {
    private static final String ORIGIN = "https://shanda.invalid/";
    private ValueCallback<Uri[]> chooser;
    private String exportJson;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        exportJson = state == null ? null : state.getString("exportJson");
        WebView web = new WebView(this);
        setContentView(web);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDomStorageEnabled(true);
        web.getSettings().setAllowFileAccess(false);
        web.setWebViewClient(new WebViewClient() {
            @Override public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest req) { return true; }
            @Override public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest req) {
                String url = req.getUrl().toString();
                String[] files = {"index.html", "app.js", "app.css"};
                String[] types = {"text/html", "application/javascript", "text/css"};
                for (int i = 0; i < files.length; i++) {
                    if (url.equals(ORIGIN + files[i])) {
                        try { return new WebResourceResponse(types[i], "utf-8", getAssets().open(files[i])); }
                        catch (IOException ignored) { break; }
                    }
                }
                return new WebResourceResponse("text/plain", "utf-8", null);
            }
        });
        web.setWebChromeClient(new WebChromeClient() {
            @Override public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (chooser != null) chooser.onReceiveValue(null);
                chooser = callback;
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE).setType("*/*");
                startActivityForResult(intent, 1);
                return true;
            }
        });
        web.addJavascriptInterface(new Object() {
            @JavascriptInterface public void saveBackup(String json) {
                runOnUiThread(() -> {
                    exportJson = json;
                    Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT).addCategory(Intent.CATEGORY_OPENABLE)
                        .setType("application/json").putExtra(Intent.EXTRA_TITLE, "山大844复习进度.json");
                    startActivityForResult(intent, 2);
                });
            }
        }, "ShandaAndroid");
        web.loadUrl(ORIGIN + "index.html");
    }

    @Override protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("exportJson", exportJson);
    }
    @Override protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        Uri uri = result == RESULT_OK && data != null ? data.getData() : null;
        if (request == 1 && chooser != null) {
            chooser.onReceiveValue(uri == null ? null : new Uri[]{uri});
            chooser = null;
        } else if (request == 2) {
            if (uri != null && exportJson != null) {
                try (OutputStream stream = getContentResolver().openOutputStream(uri, "wt")) {
                    if (stream == null) throw new IOException();
                    stream.write(exportJson.getBytes(StandardCharsets.UTF_8));
                    Toast.makeText(this, "备份已保存", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    Toast.makeText(this, "备份写入失败", Toast.LENGTH_LONG).show();
                }
            }
            exportJson = null;
        }
    }
}
