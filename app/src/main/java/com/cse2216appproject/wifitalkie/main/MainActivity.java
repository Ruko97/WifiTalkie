package com.cse2216appproject.wifitalkie.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.cse2216appproject.wifitalkie.R;
import com.cse2216appproject.wifitalkie.support.Data;
import com.cse2216appproject.wifitalkie.support.WifiDirectBroadcastReceiver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public static final int PORT_NO = 8888;

    private MenuItem onOffMenuItem, discoverMenuItem;
    private ListView peerListView;
    private TextView statusView;

    private WifiManager wifiManager;
    private WifiP2pManager wifiP2pManager;
    private WifiP2pManager.Channel wifiP2pChannel;
    private BroadcastReceiver broadcastReceiver;
    private IntentFilter intentFilter;
    private List<WifiP2pDevice> peers = new ArrayList<>();

    private String[] deviceNames;
    private WifiP2pDevice[] devices;

    private WifiP2pManager.PeerListListener peerListListener = new WifiP2pManager.PeerListListener() {
        @Override
        public void onPeersAvailable(WifiP2pDeviceList peerList) {
            Collection<WifiP2pDevice> refreshedPeers = peerList.getDeviceList();
            if ( !refreshedPeers.equals(peers) ) {
                peers.clear();
                peers.addAll(refreshedPeers);

                deviceNames = new String[refreshedPeers.size()];
                devices = new WifiP2pDevice[refreshedPeers.size()];
                int index = 0;
                for (WifiP2pDevice device : refreshedPeers) {
                    deviceNames[index] = device.deviceName;
                    devices[index] = device;
                    index++;
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(getApplicationContext(),
                        android.R.layout.simple_list_item_1, deviceNames);
                peerListView.setAdapter(adapter);
            }
            if (peers.size() == 0) {
                Toast.makeText(getApplicationContext(), "No device found", Toast.LENGTH_LONG).show();
            }
        }
    };

    private WifiP2pManager.ConnectionInfoListener connectionInfoListener = new WifiP2pManager.ConnectionInfoListener() {
        @Override
        public void onConnectionInfoAvailable(WifiP2pInfo wifiP2pInfo) {
            final InetAddress groupOwnerAddress = wifiP2pInfo.groupOwnerAddress;
            if (wifiP2pInfo.groupFormed) {
                if (wifiP2pInfo.isGroupOwner) {
                    statusView.setText("Host");
                    new Server().start();
                } else {
                    statusView.setText("Client");
                    new Client(groupOwnerAddress).start();
                }
            }
        }
    };

    public WifiP2pManager.PeerListListener getPeerListListener() {
        return peerListListener;
    }

    public WifiP2pManager.ConnectionInfoListener getConnectionInfoListener() {
        return connectionInfoListener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initialize();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        onOffMenuItem = menu.findItem(R.id.wifi_menu_item);
        discoverMenuItem = menu.findItem(R.id.discover_menu_item);
        if (wifiManager != null) {
            if (wifiManager.isWifiEnabled()) {
                onOffMenuItem.setIcon(R.drawable.ic_wifi_on);
                discoverMenuItem.setVisible(true);
            } else {
                onOffMenuItem.setIcon(R.drawable.ic_wifi_off);
                discoverMenuItem.setVisible(false);
            }
            return true;
        } else {
            onOffMenuItem.setIcon(R.drawable.ic_sentiment_satisfied);
            return false;
        }
    }

    private void initialize() {
        peerListView = (ListView) findViewById(R.id.peer_list);
        statusView = (TextView) findViewById(R.id.status);

        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiP2pManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        wifiP2pChannel = wifiP2pManager.initialize(this, getMainLooper(), null);

        broadcastReceiver = new WifiDirectBroadcastReceiver(wifiP2pManager, wifiP2pChannel, this);

        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        intentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);

        peerListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int i, long id) {
                final WifiP2pDevice device = devices[i];
                WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = device.deviceAddress;

                wifiP2pManager.connect(wifiP2pChannel, config, new WifiP2pManager.ActionListener() {
                    @Override
                    public void onSuccess() {
                        Toast.makeText(getApplicationContext(), "Connected to " + device.deviceName,
                                Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onFailure(int i) {
                        Toast.makeText(getApplicationContext(), "Not connected", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.wifi_menu_item:
                if (wifiManager != null) {
                    if (wifiManager.isWifiEnabled()) {
                        wifiManager.setWifiEnabled(false);
                        onOffMenuItem.setIcon(R.drawable.ic_wifi_off);
                        discoverMenuItem.setVisible(false);
                    } else {
                        wifiManager.setWifiEnabled(true);
                        onOffMenuItem.setIcon(R.drawable.ic_wifi_on);
                        discoverMenuItem.setVisible(true);
                    }
                    return true;
                } else {
                    onOffMenuItem.setIcon(R.drawable.ic_sentiment_satisfied);
                    return false;
                }
            case R.id.discover_menu_item:
                if (wifiP2pManager != null) {
                    wifiP2pManager.discoverPeers(wifiP2pChannel, new WifiP2pManager.ActionListener() {
                        @Override
                        public void onSuccess() {
                            statusView.setText("Discovery Started");
                        }

                        @Override
                        public void onFailure(int reason) {
                            statusView.setText("Discovery Failed");
                        }
                    });
                    return true;
                } else {
                    return false;
                }
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
        broadcastReceiver = new WifiDirectBroadcastReceiver(wifiP2pManager, wifiP2pChannel, this);
        registerReceiver(broadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(broadcastReceiver);
    }

    private class Server extends Thread {

        private ServerSocket serverSocket;
        private Socket socket;

        public Server() {
            super("Server");
        }

        @Override
        public void run() {
            try {
                serverSocket = new ServerSocket(MainActivity.PORT_NO);
                socket = serverSocket.accept();
                Data.socket = socket;
                Intent intent = new Intent(MainActivity.this, PersonActivity.class);
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private class Client extends Thread {

        private Socket socket;
        private String hostAddress;

        public Client(InetAddress hostAddress) {
            this.hostAddress = hostAddress.getHostAddress();
        }

        @Override
        public void run() {
            try {
                socket = new Socket(hostAddress, MainActivity.PORT_NO);
                Data.socket = socket;
                Intent intent = new Intent(MainActivity.this, PersonActivity.class);
                startActivity(intent);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
