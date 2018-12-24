package com.cse2216appproject.wifitalkie;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.sip.SipManager;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pGroup;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.net.wifi.p2p.WifiP2pManager.*;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import static android.widget.Toast.LENGTH_SHORT;

public class MainActivity extends Activity {

    Button btnOnOff,btnDiscover;
    ListView listView;
    TextView connectionStatus;

    WifiManager wifiManager;
    WifiP2pManager mManager;
    WifiP2pManager.Channel mChannel;
    BroadcastReceiver mReceiver;
    IntentFilter mIntentFilter;
    List<WifiP2pDevice> peers = new ArrayList<WifiP2pDevice>();
    String[] deviceNameArray;
    WifiP2pDevice[] deviceArray;
    Server server;
    Client client;
    boolean send;
    Intent intent;
    public static int counter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initialWork();
        exqListener();
        counter=0;
    }

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


    }
    public void initialWork()
    {
        send=false;
        btnOnOff = (Button)findViewById(R.id.onOff);
        btnDiscover = (Button)findViewById(R.id.discover);
        listView =(ListView)findViewById(R.id.peerListView);
        connectionStatus=(TextView)findViewById(R.id.connectionStatus);

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

        intent=new Intent(this,ChatActivity.class);
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
            else if(wifiP2pInfo.groupFormed)
            {
                connectionStatus.setText("Client");
                client = new Client(groupOwnerAddress);
            }
        }
    };
    PeerListListener peerListListener = new PeerListListener() {
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

    public void disconnect()
    {
        if(mManager !=null && mChannel !=null)
        {
            mManager.requestGroupInfo(mChannel, new GroupInfoListener() {
                @Override
                public void onGroupInfoAvailable(WifiP2pGroup group) {
                    if(group!=null && mManager !=null && mChannel!=null && group.isGroupOwner())
                    {
                        mManager.removeGroup(mChannel, new ActionListener() {
                            @Override
                            public void onSuccess() {
                                //Toast.makeText(getApplicationContext(),"removeGroup Success",Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onFailure(int reason) {
                                //Toast.makeText(getApplicationContext(),"removeGroup Failure",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        wifiManager.setWifiEnabled(false);
        wifiManager.setWifiEnabled(true);
        mReceiver=new WifiDirectBroadcastReceiver(mManager,mChannel,this);
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
                Data data=new Data(socket);
                counter++;
                startActivity(intent);
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
                Data data=new Data(socket);
                counter=counter+2;
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
