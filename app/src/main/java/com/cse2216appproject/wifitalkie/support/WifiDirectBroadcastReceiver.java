package com.cse2216appproject.wifitalkie.support;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.widget.Toast;

import com.cse2216appproject.wifitalkie.main.MainActivity;

public class WifiDirectBroadcastReceiver extends BroadcastReceiver {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private MainActivity mainActivity;

    public WifiDirectBroadcastReceiver(WifiP2pManager mManager, WifiP2pManager.Channel mChannel, MainActivity mainActivity)
    {
        this.mManager=mManager;
        this.mChannel=mChannel;
        this.mainActivity=mainActivity;
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();
        if(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action))
        {
            //do something;
            int state=intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE,-1);
            if(state==WifiP2pManager.WIFI_P2P_STATE_ENABLED)
            {
                Toast.makeText(context,"WiFi is On.",Toast.LENGTH_SHORT).show();
                mainActivity.getStatusView().setText("Wifi is On");
            }
            else {
                Toast.makeText(context,"WiFi is Off.",Toast.LENGTH_SHORT).show();
                mainActivity.getStatusView().setText("Wifi is Off");
            }
        }
        else if(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action))
        {
            if (mManager!=null)
            {
                mManager.requestPeers(mChannel,mainActivity.getPeerListListener());
            }

        }
        else if(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action))
        {
            //do something
            if(mManager == null)
            {
                return ;
            }
            NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
            if(networkInfo.isConnected())
            {
                mManager.requestConnectionInfo(mChannel,mainActivity.getConnectionInfoListener());
            }
            else {
                Toast.makeText(mainActivity, "Disconnected", Toast.LENGTH_LONG).show();
                mainActivity.getStatusView().setText("Disconnected");
            }
        }
        else if(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action))
        {
            //do something
        }
    }
}
