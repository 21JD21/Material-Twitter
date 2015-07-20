package edu.virginia.jtd5qe.twitter;

/**
 * Created by jackding on 6/12/15.
 */

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class AuthorizeActivity extends Activity{

    static String AccessToken = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authorize);
        final String pendingUrl = getIntent().getStringExtra("URL");
        final WebView webView = (WebView) findViewById(R.id.web);

        WebViewClient webChromeClient = new WebViewClient(){
            public String access;
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if(url.startsWith("http://jackdingilian.com")) {
                    AccessToken = url;
                    webView.stopLoading();
                    move();

                }
            }

        };
        webView.setWebViewClient(webChromeClient);
        webView.loadUrl(pendingUrl);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_authorize, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void move() {
        String requestToken = getIntent().getStringExtra("RequestToken");
        String requestTokenSecret = getIntent().getStringExtra("RequestTokenSecret");
        SharedPreferences sharedPref = getSharedPreferences("data", MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPref.edit();
        prefEditor.putString("verifier", AccessToken);
        prefEditor.putString("RequestToken", requestToken);
        prefEditor.putString("RequestTokenSecret", requestTokenSecret);
        prefEditor.commit();
        Intent intent = new Intent(this, TimelineActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
