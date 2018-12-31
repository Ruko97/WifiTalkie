package com.cse2216appproject.wifitalkie.main;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import com.cse2216appproject.wifitalkie.R;


public class PersonActivity extends AppCompatActivity {

    private String personName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person);

        personName = getIntent().getStringExtra(MainActivity.PERSON_NAME);
        TextView personNameView = findViewById(R.id.person_name);
        personNameView.setText(personName);
    }

    public void startCall(View view) {
        Intent intent = new Intent(this, CallActivity.class);
        intent.putExtra(MainActivity.PERSON_NAME, personName);
        startActivity(intent);
    }

    public void startMessage(View view) {
        Intent intent = new Intent(this, MessageActivity.class);
        startActivity(intent);
    }
}
