package me.pastleo.sctid;

import android.content.Context;
import android.content.SharedPreferences;
import android.inputmethodservice.*;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;
import android.content.Intent;

public class SctidIMEService extends InputMethodService implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener{

    private TextView msg;

    SharedPreferences settings;
    String share_settings_rev_tmp = "";

    @Override
    public View onCreateInputView(){
        View v = getLayoutInflater().inflate(R.layout.input, null);
        msg = (TextView) v.findViewById(R.id.msg);

        v.findViewById(R.id.scan).setOnClickListener(this);
        v.findViewById(R.id.clear).setOnClickListener(this);
        v.findViewById(R.id.enter).setOnClickListener(this);
        v.findViewById(R.id.swi).setOnClickListener(this);
        v.findViewById(R.id.settings).setOnClickListener(this);

        settings = PreferenceManager.getDefaultSharedPreferences(this);
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);

        return v;
    }

//    @Override
//    public void onStartInputView (EditorInfo info, boolean restarting) {
//        super.onStartInputView(info,restarting);
//        receive();
//    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,String key) {
        receive();
    }

    private void receive(){
        Log.d("receive","receive!");
        share_settings_rev_tmp = settings.getString(getResources().getString(R.string.share_settings_rev_tmp), "");
        String share_settings_msg = settings.getString(getResources().getString(R.string.share_settings_msg), "");
        boolean status =  settings.getBoolean(getResources().getString(R.string.share_settings_status),false);

        if(share_settings_msg.length() > 0) {
            msg.setText(share_settings_msg);
            msg.setTextColor(status ? MainActivity.SUCCESS_COLOR : MainActivity.FAIL_COLOR);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
    }

    @Override
    public void onClick(View v){
        InputConnection ic = getCurrentInputConnection();
        switch(v.getId()){
            case R.id.scan:
                Intent revIntent = new Intent(this, SCRecieverActivity.class);
                revIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(revIntent);
                break;
            case R.id.clear:
                ic.deleteSurroundingText(1000,1000);
                break;
            case R.id.enter:
                ic.commitText(share_settings_rev_tmp,0);
                break;
            case R.id.swi:
                InputMethodManager mgr =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (mgr != null) {
                    mgr.showInputMethodPicker();
                }
                break;
            case R.id.settings:
                Intent setIntent = new Intent(this, MainActivity.class);
                setIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(setIntent);
                break;
        }
    }
}