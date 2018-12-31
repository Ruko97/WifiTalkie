package com.cse2216appproject.wifitalkie.main;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.cse2216appproject.wifitalkie.R;
import com.cse2216appproject.wifitalkie.support.AudioCall;
import com.cse2216appproject.wifitalkie.support.Data;


public class CallActivity extends AppCompatActivity {

    private String personName;
    private AudioCall audioCall;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        personName = getIntent().getStringExtra(MainActivity.PERSON_NAME);
        TextView personNameView = findViewById(R.id.person_name_call);
        personNameView.setText(personName);
    }

    @Override
    protected void onStart() {
        super.onStart();
        audioCall = new AudioCall(Data.socket.getInetAddress());
        audioCall.startCall();
    }

    @Override
    protected void onStop() {
        super.onStop();
        audioCall.endCall();
    }

    public void endCall(View view) {
        audioCall.endCall();
        finish();
    }
}
