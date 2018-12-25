package com.cse2216appproject.wifitalkie;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;

public class ChatActivity extends Activity {

    Button btnSend;
    Button btnCall;
    Button btnEndCall;
    TextView read_msg_box;
    EditText writeMsg;

    SendReceive sendReceive;
    String sendData;

    static final int MESSAGE_READ=1;
    InputStream inputStream;
    OutputStream outputStream;

    boolean status;
    private static final int SAMPLE_RATE = 8000; // Hertz
    private static final int SAMPLE_INTERVAL = 20; // Milliseconds
    private static final int SAMPLE_SIZE = 2; // Bytes
    private static final int BUF_SIZE = SAMPLE_INTERVAL * SAMPLE_INTERVAL * SAMPLE_SIZE * 2; //Bytes
    private InetAddress address; // Address to call
    private int port = 8888; // Port the packets are addressed to


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        initialWork();
        exqtListener();
        read_msg_box.setText("counter = "+MainActivity.counter);
        sendReceive=new SendReceive(Data.socket);
    }

    void exqtListener()
    {
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData = writeMsg.getText().toString();
                sendReceive.writeto(sendData);
            }
        });
        btnCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                address=Data.socket.getInetAddress();
                status=true;
                startReceiving();
                startStreaming();
            }
        });
        btnEndCall.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                status=false;
            }
        });
    }

    void initialWork()
    {
        btnSend = (Button)findViewById(R.id.sendButton);
        btnCall=(Button)findViewById(R.id.callButton);
        btnEndCall=(Button)findViewById(R.id.endCall);
        read_msg_box = (TextView)findViewById(R.id.readMsg);
        writeMsg=(EditText)findViewById(R.id.writeMsg);
    }


    Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            switch (msg.what)
            {
                case MESSAGE_READ:
                    byte []readBuff = (byte[])msg.obj;
                    String tempMsg = new String(readBuff,0,msg.arg1);
                    read_msg_box.setText(tempMsg);
                    break;
            }
            return true;
        }
    });

    private class SendReceive implements Runnable{
        private Thread thread;
        private Socket socket;

        SendReceive(Socket socket)
        {

            thread=new Thread(this,"Client");
            this.socket=socket;
            try {
                inputStream=socket.getInputStream();
                outputStream=socket.getOutputStream();
                //printStream = new PrintStream(outputStream);
            } catch (IOException e) {
                e.printStackTrace();
            }
            thread.start();
        }

        @Override
        public void run() {

            byte []buffer = new byte[1024];
            int bytes;
            while (socket!=null)
            {
                try {
                    bytes =inputStream.read(buffer);
                    if(bytes>0)
                    {
                        handler.obtainMessage(MESSAGE_READ,bytes,-1,buffer).sendToTarget();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        }
        public void writeto(String msg)
        {
            new SendReceiveTask().execute(msg);
        }
    }

    class SendReceiveTask extends AsyncTask<String, String, String>
    {
        @Override
        protected String doInBackground(String... strings) {
            String msg=strings[0];
            try {
                outputStream.write(msg.getBytes());
                //printStream.write(bytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        //MainActivity.disconnect();
    }


    public void startReceiving()
    {
        Thread receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                AudioTrack track = new AudioTrack(AudioManager.STREAM_MUSIC, SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO,
                        AudioFormat.ENCODING_PCM_16BIT, BUF_SIZE, AudioTrack.MODE_STREAM);
                track.play();
                try {
                    DatagramSocket socket = new DatagramSocket(port);
                    byte[] buf = new byte[BUF_SIZE];
                    while (status)
                    {
                        DatagramPacket packet = new DatagramPacket(buf, BUF_SIZE);
                        socket.receive(packet);
                        track.write(packet.getData(), 0, BUF_SIZE);
                    }
                    socket.disconnect();
                    socket.close();
                    track.stop();
                    track.flush();
                    track.release();
                    status = false;
                    return;

                }catch (Exception e)
                {

                }
            }
        });
        receiveThread.start();
    }
    public void startStreaming()
    {
        Thread streamThread = new Thread(new Runnable() {
            @Override
            public void run() {
                AudioRecord audioRecorder = new AudioRecord (MediaRecorder.AudioSource.MIC, SAMPLE_RATE,
                        AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                        AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)*10);
                int bytes_read = 0;
                int bytes_sent = 0;
                byte[] buf = new byte[BUF_SIZE];
                try {

                    DatagramSocket socket = new DatagramSocket();
                    audioRecorder.startRecording();
                    while (status)
                    {
                        bytes_read = audioRecorder.read(buf, 0, BUF_SIZE);
                        DatagramPacket packet = new DatagramPacket(buf, bytes_read, address, port);
                        socket.send(packet);
                        bytes_sent += bytes_read;
                        Thread.sleep(SAMPLE_INTERVAL, 0);
                    }
                    audioRecorder.stop();
                    audioRecorder.release();
                    socket.disconnect();
                    socket.close();
                    status = false;
                    return;
                }catch (Exception e)
                {

                }


            }


        });
        streamThread.start();
    }
}
