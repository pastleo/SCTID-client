package me.pastleo.sctid;

import android.graphics.Color;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NfcAdapter;
import android.content.SharedPreferences;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends ActionBarActivity implements View.OnClickListener,Response.Listener<JSONObject>,Response.ErrorListener {
    private final String[][] techList = new String[][] { new String[] {
            NfcA.class.getName(), NfcB.class.getName(), NfcF.class.getName(),
            NfcV.class.getName(), IsoDep.class.getName(),
            MifareClassic.class.getName(), MifareUltralight.class.getName(),
            Ndef.class.getName() } };

    SharedPreferences settings;
    EditText username;
    EditText password;
    EditText card_id;
    EditText sid;
    TextView result;

    protected final static int SUCCESS_COLOR = Color.GREEN;
    protected final static int FAIL_COLOR = Color.RED;
    protected final static int PENDING_COLOR = Color.YELLOW;

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        settings = PreferenceManager.getDefaultSharedPreferences(this);

        findViewById(R.id.login).setOnClickListener(this);
        findViewById(R.id.logout).setOnClickListener(this);
        findViewById(R.id.register).setOnClickListener(this);

        username = (EditText) findViewById(R.id.username);
        password = (EditText) findViewById(R.id.password);
        card_id = (EditText) findViewById(R.id.card_id);
        sid = (EditText) findViewById(R.id.sid);
        result = (TextView) findViewById(R.id.result);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // creating pending intent:
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), 0);
        // creating intent receiver for NFC events:
        IntentFilter filter = new IntentFilter();
        filter.addAction(NfcAdapter.ACTION_TAG_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filter.addAction(NfcAdapter.ACTION_TECH_DISCOVERED);
        // enabling foreground dispatch for getting intent from NFC event:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.enableForegroundDispatch(this, pendingIntent, new IntentFilter[]{filter}, this.techList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // disabling foreground dispatch:
        NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(this);
        nfcAdapter.disableForegroundDispatch(this);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED)) {
            String cid = Helper.ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
            result.setText("NFC READ: " + cid);
            result.setTextColor(SUCCESS_COLOR);
            card_id.setText(cid);
        }
    }

    public static final int HTTP_STATE_LOGIN = 1;
    public static final int HTTP_STATE_LOGOUT = 2;
    public static final int HTTP_STATE_REGISTER = 3;
    public static final int HTTP_STATE_GET = 4;
    private static final int HTTP_STATE_READY = 0;
    private int HttpState = 0;
    RequestQueue rq;

    @Override
    public void onClick(View view) {
        if(HttpState != HTTP_STATE_READY) return;

        String url = getResources().getString(R.string.server_base_url);
        HashMap<String, String> params;
        CookieJsonObjectRequest req;
        rq = CookieJsonObjectRequest.getRequestQueue(rq,this);
        try{
            switch (view.getId()) {
                case R.id.login:
                    url += getResources().getString(R.string.login_uri);
                    params = new HashMap<>();
                    params.put("username", username.getText().toString());
                    params.put("password", password.getText().toString());
                    req = new CookieJsonObjectRequest(Request.Method.POST, url, params, this, this, settings);
                    rq.add(req);
                    HttpState = HTTP_STATE_LOGIN;
                    break;
                case R.id.logout:
                    url += getResources().getString(R.string.logout_uri);
                    rq.add(new CookieJsonObjectRequest(Request.Method.GET,url, null, this, this, settings));
                    HttpState = HTTP_STATE_LOGOUT;
                    break;
                case R.id.register:
                    params = new HashMap<>();
                    url += getResources().getString(R.string.register_uri);
                    params.put("card_id", card_id.getText().toString());
                    params.put("student_id", sid.getText().toString());
                    req = new CookieJsonObjectRequest(Request.Method.POST, url, params, this, this, settings);
                    rq.add(req);
                    HttpState = HTTP_STATE_REGISTER;
                    break;
            }

            result.setText(getResources().getString(R.string.action_pending));
            result.setTextColor(PENDING_COLOR);
        }catch(Exception e) {
            onError("onClick", e);
        }
    }

    @Override
    public void onResponse(JSONObject response) {
        try {
            boolean success = response.getBoolean("success");
            String message = response.getString("message");
            result.setText(message);
            result.setTextColor(success ? SUCCESS_COLOR : FAIL_COLOR);
            switch (HttpState){
                case HTTP_STATE_LOGIN:
                    break;
                case HTTP_STATE_LOGOUT:
                    break;
                case HTTP_STATE_REGISTER:
                    break;
                case HTTP_STATE_GET:
                    break;
            }

            HttpState = HTTP_STATE_READY;
        } catch (JSONException e) {
            onError("onResponse", e);
        }
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        onError("onErrorResponse", error);
    }

    private void onError(String from,Exception e){
        Log.e(from, e.toString());
        e.printStackTrace();
        result.setText(e.getMessage());
        result.setTextColor(FAIL_COLOR);

        HttpState = HTTP_STATE_READY;
    }
}
