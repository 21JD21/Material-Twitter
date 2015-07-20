package edu.virginia.jtd5qe.twitter;

/**
 * Created by jackding on 6/4/15.
 */

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
public class LoginActivity extends Activity {

    public static final String LOGIN_MESSAGE = "edu.virginia.Twitter.login_message";
    static String OAUTH_SIGNATURE_METHOD = "HMAC-SHA1";
    static String CONSUMER_KEY = "nF8GoCuOJDuYA6WTYUPRJEFgZ";
    static String CONSUMER_SECRET = "RlyeN4GXMMNRSZ1FsRZGjRKUGZ74mJfSVlGTCyXZaOAsdkRC9h";
    static String CALLBACK_URL = "http://jackdingilian.com";
    String requestToken = "";
    String requestTokenSecret = "";
    String verifier = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_login, menu);
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

    public void myClickHandler(View view) {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            Twitter_Login t = new Twitter_Login();
            t.execute();
            while(!t.isCancelled()) {

            }
            String url = "https://api.twitter.com/oauth/authenticate?oauth_token=" + requestToken;
            Intent intent = new Intent(this, AuthorizeActivity.class);
            intent.putExtra("URL", url);
            intent.putExtra("RequestToken", requestToken);
            intent.putExtra("RequestTokenSecret", requestTokenSecret);
            startActivity(intent);
        }
        else {
        }


    }

    private class Twitter_Login extends AsyncTask {

        public void getRequestToken() {
            String nonce = UUID.randomUUID().toString().replaceAll("-","");
            Date date = new Date();
            Timestamp time = new Timestamp(date.getTime());
            Long lng = time.getTime() / 1000;
            String timestamp = lng.toString();
            String parameter_string = "oauth_consumer_key=" + CONSUMER_KEY + "&" +
                    "oauth_nonce=" + nonce + "&" + "oauth_signature_method=" +
                    OAUTH_SIGNATURE_METHOD + "&" + "oauth_timestamp=" + timestamp +
                    "&" + "oauth_version=1.0";
            String signature_base_string = "";
            try {
                signature_base_string = "POST&https%3A%2F%2Fapi.twitter.com%2Foauth%2Frequest_token&" +
                        URLEncoder.encode(parameter_string, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String oauth_signature = "";
            try {
                oauth_signature = computeSignature(signature_base_string, CONSUMER_SECRET + "&");
            } catch (GeneralSecurityException e) {
                e.printStackTrace();
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            String DST = "";
            try {
                DST = "OAuth " + "oauth_consumer_key=" + '"' + CONSUMER_KEY + '"' + ", " +
                        "oauth_nonce=" + '"' + nonce + '"' + ", " + "oauth_signature=" + '"' +
                        URLEncoder.encode(oauth_signature, "UTF-8") + '"' + ", " + "oauth_signature_method=" +
                        '"' + OAUTH_SIGNATURE_METHOD + '"' + ", " + "oauth_timestamp=" + '"' + timestamp +
                        '"' + ", " + "oauth_version=" + '"' + "1.0" + '"';
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            HttpClient httpClient = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost("https://api.twitter.com/oauth/request_token");
            httpPost.setHeader("Authorization", DST);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String response = null;
            try {
                response = httpClient.execute(httpPost, responseHandler).toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
            String request_token = "";

            if (response != null) {
                requestToken = response.substring(response.indexOf("oauth_token=") + 12,
                        response.indexOf("&oauth_token_secret="));
                requestTokenSecret = response.substring(response.indexOf("&oauth_token_secret="),
                        response.length());

            }
            httpClient.getConnectionManager().shutdown();
            this.cancel(false);
        }

        private String computeSignature(String baseString, String keyString)
                throws GeneralSecurityException, UnsupportedEncodingException {

            SecretKey secretKey = null;
            byte[] keyBytes = keyString.getBytes();
            secretKey = new SecretKeySpec(keyBytes, "HmacSHA1");

            Mac mac = Mac.getInstance("HmacSHA1");

            mac.init(secretKey);

            byte[] text = baseString.getBytes();

            return new String(Base64.encodeBase64(mac.doFinal(text))).trim();
        }

        protected Object doInBackground(Object[] params) {
            getRequestToken();
            return null;
        }

        protected void onPostExecute() {

        }
    }
}
