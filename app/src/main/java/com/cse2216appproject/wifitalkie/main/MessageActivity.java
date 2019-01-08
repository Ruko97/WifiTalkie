package com.cse2216appproject.wifitalkie.main;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.cse2216appproject.wifitalkie.R;
import com.cse2216appproject.wifitalkie.support.Data;
import com.cse2216appproject.wifitalkie.support.MyRecyclerViewAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;

public class MessageActivity extends AppCompatActivity {

    private SendReceive sendReceive;
    private EditText writeMessage;
    //private TextView readMessageBox;

    RecyclerView recyclerView;
    MyRecyclerViewAdapter adapter;
    ArrayList<String> sentMessages;

    private static final int MESSAGE_READ = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message);

        //readMessageBox = findViewById(R.id.readMessage);
        writeMessage = findViewById(R.id.writeMessage);
        sendReceive = new SendReceive(Data.socket);

        sentMessages = new ArrayList<>();
        // set up the RecyclerView
        recyclerView = findViewById(R.id.chat);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MyRecyclerViewAdapter(this, sentMessages);
        recyclerView.setAdapter(adapter);
    }

    public void sendMessage(View view) {
        String dataToSend = writeMessage.getText().toString();
        sendReceive.writeTo(dataToSend);
        sentMessages.add(dataToSend);
        recyclerView.refreshDrawableState();
    }

    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message message) {
            switch (message.what) {
                case MESSAGE_READ:
                    byte[] readBuff = (byte[]) message.obj;
                    String tempMessage = new String(readBuff, 0, message.arg1);
                    //readMessageBox.setText(tempMessage);
                    sentMessages.add(tempMessage);
                    recyclerView.refreshDrawableState();

                    break;
            }
            return true;
        }
    });

    private class SendReceive extends Thread {

        private Socket socket;
        private InputStream inputStream;
        private OutputStream outputStream;

        SendReceive(Socket socket) {
            super("Client");
            this.socket = socket;

            try {
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.start();
        }

        @Override
        public void run() {
            byte[] buffer = new byte[1024];
            int bytes;

            while (socket != null) {
                try {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        handler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        public void writeTo(String message) {
            new SendReceiveTask().execute(message);
        }

        private class SendReceiveTask extends AsyncTask<String, String, Void> {
            @Override
            protected Void doInBackground(String... strings) {
                String message = strings[0];
                try {
                    outputStream.write(message.getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
    }

}
