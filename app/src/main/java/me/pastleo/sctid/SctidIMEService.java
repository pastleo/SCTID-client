package me.pastleo.sctid;

import android.inputmethodservice.*;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;
//import android.content.Intent;
//import android.os.IBinder;

public class SctidIMEService extends InputMethodService implements View.OnClickListener{

    private View v;
    private ToggleButton scan;
    private TextView msg;
    private Button enter;

//    private Keyboard keyboard;

//    private boolean caps = false;

    @Override
    public View onCreateInputView(){
        v = getLayoutInflater().inflate(R.layout.input, null);
        scan = (ToggleButton) v.findViewById(R.id.scan);
        msg = (TextView) v.findViewById(R.id.msg);
        enter = (Button) v.findViewById(R.id.enter);
        enter.setOnClickListener(this);
        return v;
    }

    @Override
    public void onClick(View v){
        InputConnection ic = getCurrentInputConnection();
        ic.commitText("k",0);
    }
}