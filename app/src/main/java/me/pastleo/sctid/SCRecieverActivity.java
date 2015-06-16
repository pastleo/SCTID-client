package me.pastleo.sctid;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.tech.IsoDep;
import android.nfc.tech.MifareClassic;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcB;
import android.nfc.tech.NfcF;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.graphics.Point;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class SCRecieverActivity extends Activity implements View.OnClickListener,Response.Listener<JSONObject>,Response.ErrorListener {

    private final String[][] techList = new String[][] { new String[] {
            NfcA.class.getName(), NfcB.class.getName(), NfcF.class.getName(),
            NfcV.class.getName(), IsoDep.class.getName(),
            MifareClassic.class.getName(), MifareUltralight.class.getName(),
            Ndef.class.getName() } };

    SharedPreferences settings;
    TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Creates the layout for the window and the look of it
        requestWindowFeature(Window.FEATURE_ACTION_BAR);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND,
                WindowManager.LayoutParams.FLAG_DIM_BEHIND);

        // Params for the window.
        // You can easily set the alpha and the dim behind the window from here
        WindowManager.LayoutParams params = getWindow().getAttributes();
        params.alpha = 1.0f;    // lower than one makes it more transparent
        params.dimAmount = 0f;  // set it higher if you want to dim behind the window
        getWindow().setAttributes(params);

        // Gets the display size so that you can set the window to a percent of that
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int width = size.x;
        int height = size.y;

        // You could also easily used an integer value from the shared preferences to set the percent
        if (height > width) {
            getWindow().setLayout((int) (width * .9), (int) (height * .7));
        } else {
            getWindow().setLayout((int) (width * .7), (int) (height * .8));
        }

        setContentView(R.layout.activity_screciever);

        findViewById(R.id.scanner_frame).setOnClickListener(this);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        status = (TextView) findViewById(R.id.scanner_msg);
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
    public void onClick(View v){
        finish();
    }

    private boolean GOT_INTENT = false;

    @Override
    protected void onNewIntent(Intent intent) {
        if (intent.getAction().equals(NfcAdapter.ACTION_TAG_DISCOVERED) && !GOT_INTENT) {
            try {
                String card_id = Helper.ByteArrayToHexString(intent.getByteArrayExtra(NfcAdapter.EXTRA_ID));
                GOT_INTENT = true;
                String url = getResources().getString(R.string.server_base_url) + getResources().getString(R.string.get_uri);
                HashMap<String, String> params = new HashMap<>();
                params.put("card_id", card_id);
                CookieJsonObjectRequest.getRequestQueue(null,this).add(
                        new CookieJsonObjectRequest(Request.Method.POST, url, params, this, this, settings)
                );
                status.setText(getResources().getString(R.string.action_pending));
                status.setTextColor(MainActivity.PENDING_COLOR);
            }catch(Exception e) {
                onError("onNewIntent",e);
            }
        }
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
//        new Handler().postDelayed(new Runnable() {
//            @Override
//            public void run() {
//                Log.d("haaaaaaaaa","this shit ran...");
//                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).showSoftInput(getCallingActivity().get
//                , InputMethodManager.SHOW_IMPLICIT);
//            }
//        }, 5000);
    }

    @Override
    public void onErrorResponse(VolleyError e) {
        onError("onErrorResponse",e);
    }

    private void onError(String from,Exception e){
        Log.e(from, e.toString());
        e.printStackTrace();
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(
                getResources().getString(R.string.share_settings_status),
                false
        );
        editor.putString(
                getResources().getString(R.string.share_settings_msg),
                e.getMessage()
        );
        editor.commit();
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override
    public void onResponse(JSONObject response) {
        try {
            boolean success = response.getBoolean("success");
            String message = response.getString("message");
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean(
                    getResources().getString(R.string.share_settings_status),
                    success
            );
            editor.putString(
                    getResources().getString(R.string.share_settings_msg),
                    message
            );

            if(!success)
                throw new Exception(message);

            editor.putString(
                    getResources().getString(R.string.share_settings_rev_tmp),
                    response.getString("student_id")
            );

            editor.commit();
            Toast.makeText(this, message + "\n" + getResources().getString(R.string.got_sid_hint), Toast.LENGTH_SHORT).show();
            finish();
        } catch (Exception e) {
            onError("onResponse", e);
        }
    }
}
