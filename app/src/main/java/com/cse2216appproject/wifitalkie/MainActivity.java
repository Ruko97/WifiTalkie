package com.cse2216appproject.wifitalkie;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.*;

import android.os.AsyncTask;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import android.view.View;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends Activity {

    Button btnOnOff,btnDiscover,btnSend;
    ListView listView;
    TextView read_msg_box,connectionStatus;
    EditText writeMsg;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;

    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;

    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;

    static final int MESSAGE_READ=1;
    InputStream inputStream;
    OutputStream outputStream;

    Server server;
    Client client;
    SendReceive sendReceive;

    String sendData;
    boolean send;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
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
    public void exqListener()
    {

        btnOnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(wifiManager.isWifiEnabled())
                {
                    wifiManager.setWifiEnabled(false);
                    btnOnOff.setText("Turn On WiFi");
                }
                else {
                    wifiManager.setWifiEnabled(true);
                    btnOnOff.setText("Turn Off WiFi");
                }
            }
        });

        btnDiscover.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mManager.discoverPeers( mChannel, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        connectionStatus.setText ("Discovery Started");
                    }

                    @Override
                    public void onFailure(int reason) {
                        connectionStatus.setText("Discovery Failed");
                    }
                });
            }
        });
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                final WifiP2pDevice device = deviceArray[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress=device.deviceAddress;

                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(),"Connected to "+device.deviceName,LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int reason) {
                        Toast.makeText(getApplicationContext(),"Not connected ",LENGTH_SHORT).show();
                    }
                });
            }
        });

        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendData = writeMsg.getText().toString();
                //send=true;
                //System.out.println("data to be sent (update) : "+sendData );
                sendReceive.writeto(sendData);
            }
        });
    }
    public void initialWork()
    {
        send=false;
        btnOnOff = (Button)findViewById(R.id.onOff);
        btnDiscover = (Button)findViewById(R.id.discover);
        btnSend = (Button)findViewById(R.id.sendButton);
        listView =(ListView)findViewById(R.id.peerListView);
        read_msg_box = (TextView)findViewById(R.id.readMsg);
        connectionStatus=(TextView)findViewById(R.id.connectionStatus);
        writeMsg=(EditText)findViewById(R.id.writeMsg);

        wifiManager= (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        mManager =(WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel=mManager.initialize(this,getMainLooper(),null);
        if(wifiManager.isWifiEnabled())
        {
            btnOnOff.setText("Turn Off WiFi");
        }
        else {
            btnOnOff.setText("Turn On WiFi");
        }

        mReceiver = new WifiDirectBroadcastReceiver(mManager,mChannel,this);
        mIntentFilter=new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
    }

    WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if(wifiP2pInfo.groupFormed && wifiP2pInfo.isGroupOwner)
            {
                connectionStatus.setText("Host");
                server = new Server();
            }
            else
            {
                connectionStatus.setText("Client");
                client = new Client(groupOwnerAddress);
            }
        }
    };
    PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {

            Collection<WifiP2pDevice> refreshedPeers =  peerList.getDeviceList();
            if(!refreshedPeers.equals(peers))
            {
                peers.clear();
                peers.addAll(refreshedPeers);

                deviceNameArray=new String[refreshedPeers.size()];
                deviceArray=new WifiP2pDevice[refreshedPeers.size()];
                int index=0;
                for(WifiP2pDevice device : refreshedPeers)
                {
                    deviceNameArray[index]=device.deviceName;
                    deviceArray[index]=device;
                    index++;
                }
                ArrayAdapter<String>adapter=new ArrayAdapter<String>(getApplicationContext(),android.R.layout.simple_list_item_1,deviceNameArray);
                listView.setAdapter(adapter);

            }
            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(),"No device found",Toast.LENGTH_SHORT).show();
                return;
            }
        }
    };
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver,mIntentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);
    }
    @Override
    protected void onStop()
    {
        super.onStop();
    }
    public class Server implements Runnable{

        Thread thread;
        ServerSocket serverSocket;
        Socket socket;

        Server()
        {
            thread=new Thread(this,"Server");
            thread.start();
        }
        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(8888);
                socket=serverSocket.accept();
                sendReceive=new SendReceive(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public class Client implements Runnable{

        Thread thread;
        Socket socket;
        String hostAdd;
        Client(InetAddress hostAddress)
        {
            hostAdd=hostAddress.getHostAddress();
            thread=new Thread(this,"Client");
            thread.start();
        }
        @Override
        public void run() {
            try {
                socket=new Socket(hostAdd,8888);
                sendReceive=new SendReceive(socket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class SendReceive implements Runnable{
        private Thread thread;
        private Socket socket;
        //PrintStream printStream;


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
                    /*if(send==true)
                    {
                        System.out.println("now sendData is not null . it is : "+sendData);
                        printStream.write(sendData.getBytes());
                        send=false;
                    }*/
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


}
