package com.example.android.nc;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.DetailedState;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.WifiManager.WifiLock;
import android.util.Log;

//wifi接入方
public abstract class WifiAdmin {
    
    private static final String TAG = "WifiAdmin";
    //提供wifi管理的各种主要API，主要包含wifi的扫描，建立连接，配置等
    private WifiManager mWifiManager;
    //描述已建立连接后的wifi信息。包含ip、mac地址、连接速度等信息
    private WifiInfo mWifiInfo;
    private WifiApAdmin wifiapadmin;
    // 扫描出的网络连接列表,描述开启扫描后的Wifi热点的信息。包含SSID、Capabilities、frequency、level（信号强度）等
    private List<ScanResult> mWifiList;
    //描述wifi的连接信息，包含SSID、SSID隐藏、Password等的设置
    private List<WifiConfiguration> mWifiConfiguration;
//    用于给wifi加锁，通常用于下载大体积文件的时候。
//    如果应用程序想在屏幕被关掉后继续使用WiFi则可以调用 acquireWifiLock来锁住WiFi，
// 该操作会阻止WiFi进入睡眠状态。当应用程序不再使用WiFi时需要调用 releaseWifiLock来释放WiFi。
// 之后WiFi可以进入睡眠状态以节省电源。
    private WifiLock mWifiLock;

    private String mPasswd = "";
    private String mSSID = "";
    private Context mContext = null;

    public WifiAdmin(Context context) {
        
        mContext = context;
        // 取得WifiManager对象
        mWifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        // 取得WifiInfo对象
        mWifiInfo = mWifiManager.getConnectionInfo();
        wifiapadmin=new WifiApAdmin(context);
        Log.v(TAG, "getIpAddress = " + mWifiInfo.getIpAddress());
    }

    // 打开WIFI
    public void openWifi() {
    	try {
			wifiapadmin.closeWifiAp(mContext);
		} catch (Exception e) {
			e.printStackTrace();
		}
        if (!mWifiManager.isWifiEnabled()) {
				mWifiManager.setWifiEnabled(true);
//				while(checkState()==WifiManager.WIFI_STATE_ENABLING){
//					//Log.i("log", "opening");
//					//mWifiManager.setWifiEnabled(true);
//				}
			
        }
     
    }

    // 关闭WIFI
    public void closeWifi() {
        if (mWifiManager.isWifiEnabled()) {
            mWifiManager.setWifiEnabled(false);
        }
    }

    public abstract Intent myRegisterReceiver(BroadcastReceiver receiver, IntentFilter filter);
    
    public abstract void myUnregisterReceiver(BroadcastReceiver receiver);
    
    public abstract void onNotifyWifiConnected();
    
    public abstract void onNotifyWifiConnectFailed();
    
    // 添加一个网络并连接
    public boolean addNetwork(WifiConfiguration newConfig) {
        
        register();
        //关闭热点
        WifiApAdmin.closeWifiAp(mContext);
        
        int netID = mWifiManager.addNetwork(newConfig);
        boolean bRet = mWifiManager.enableNetwork(netID, true);
        return  bRet;
    }

    //定义几种加密方式，一种是WEP，一种是WPA，还有没有密码的情况
    public static final int TYPE_NO_PASSWD = 0x11;
    public static final int TYPE_WEP = 0x12;
    public static final int TYPE_WPA = 0x13;

    
    public void addNetwork(String ssid, String passwd, int type) {
        if (ssid == null || passwd == null || ssid.equals("")) {
            Log.e(TAG, "addNetwork() ## nullpointer error!");
            return;
        }
        
        if (type != TYPE_NO_PASSWD && type != TYPE_WEP && type != TYPE_WPA) {
            Log.e(TAG, "addNetwork() ## unknown type = " + type);
        }
        
        stopTimer();
        unRegister();
        
        addNetwork(createWifiInfo(ssid, passwd, type));
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            // TODO Auto-generated method stub
            if (intent.getAction().equals(WifiManager.RSSI_CHANGED_ACTION)) {
                Log.d(TAG, "RSSI changed");
                
                //有可能是正在获取，或者已经获取了
                Log.d(TAG, " intent is " + WifiManager.RSSI_CHANGED_ACTION);
                
                if (isWifiContected(mContext) == WIFI_CONNECTED) {
                    stopTimer();
                    onNotifyWifiConnected();
                    unRegister();
                } else if (isWifiContected(mContext) == WIFI_CONNECT_FAILED) {
                    stopTimer();
                    //closeWifi();
                    onNotifyWifiConnectFailed();
                    unRegister();
                } else if (isWifiContected(mContext) == WIFI_CONNECTING) {
                    
                }
            }
        }
    };
    
    private final int STATE_REGISTRING = 0x01;
    private final int STATE_REGISTERED = 0x02;
    private final int STATE_UNREGISTERING = 0x03;
    private final int STATE_UNREGISTERED = 0x04;
    
    private int mHaveRegister = STATE_UNREGISTERED;
    private synchronized void register() {
        Log.v(TAG, "register() ##mHaveRegister = " + mHaveRegister);

        if (mHaveRegister == STATE_REGISTRING 
                || mHaveRegister == STATE_REGISTERED) {
            return ;
        }
        
        mHaveRegister = STATE_REGISTRING;
        myRegisterReceiver(mBroadcastReceiver, new IntentFilter(WifiManager.RSSI_CHANGED_ACTION));
        mHaveRegister = STATE_REGISTERED;
        
        startTimer();
    }
    
    private synchronized void unRegister() {
        Log.v(TAG, "unRegister() ##mHaveRegister = " + mHaveRegister);
        
        if (mHaveRegister == STATE_UNREGISTERED 
                || mHaveRegister == STATE_UNREGISTERING) {
            return ;
        }
        
        mHaveRegister = STATE_UNREGISTERING;
        myUnregisterReceiver(mBroadcastReceiver);
        mHaveRegister = STATE_UNREGISTERED;
    }
    
    private Timer mTimer = null;
    private void startTimer() {
        if (mTimer != null) {
            stopTimer();
        }
        
        mTimer = new Timer(true);
//      mTimer.schedule(mTimerTask, 0, 20 * 1000);// 20s
        mTimer.schedule(mTimerTask, 20 * 1000);
    }
    
    private TimerTask mTimerTask = new TimerTask() {
        
        @Override
        public void run() {
            // TODO Auto-generated method stub
            Log.e(TAG, "timer out!");
            onNotifyWifiConnectFailed();
         
            unRegister();
        }
    };
    
    private void stopTimer() {
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }
    
    @Override
    protected void finalize() {
        try {
            super.finalize();
            unRegister();
        } catch (Throwable e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public WifiConfiguration createWifiInfo(String SSID, String password, int type) {
        
        Log.v(TAG, "SSID = " + SSID + "## Password = " + password + "## Type = " + type);
        
        WifiConfiguration config = new WifiConfiguration();
        config.allowedAuthAlgorithms.clear();
        config.allowedGroupCiphers.clear();
        config.allowedKeyManagement.clear();
        config.allowedPairwiseCiphers.clear();
        config.allowedProtocols.clear();
        config.SSID = "\"" + SSID + "\"";


        WifiConfiguration tempConfig = this.IsExsits(SSID); //获取已存在的netID配置
        if (tempConfig != null) {
            mWifiManager.removeNetwork(tempConfig.networkId); //移除配置
        }
        
        // 分为三种情况：1没有密码  2用wep加密  3用wpa加密
        if (type == TYPE_NO_PASSWD) {// WIFICIPHER_NOPASS
            config.wepKeys[0] = "";
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
            
        } else if (type == TYPE_WEP) {  //  WIFICIPHER_WEP 
            config.hiddenSSID = true;
            config.wepKeys[0] = "\"" + password + "\"";
            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.SHARED);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP40);
            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.WEP104);
            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
            config.wepTxKeyIndex = 0;
        } else if (type == TYPE_WPA) {   // WIFICIPHER_WPA
            config.preSharedKey = "\"" + password + "\"";
            config.hiddenSSID = true;
//            config.allowedAuthAlgorithms.set(WifiConfiguration.AuthAlgorithm.OPEN);
//            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.TKIP);
//            config.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.WPA_PSK);
//            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.TKIP);
//            // config.allowedProtocols.set(WifiConfiguration.Protocol.WPA);
//            config.allowedGroupCiphers.set(WifiConfiguration.GroupCipher.CCMP);
//            config.allowedPairwiseCiphers.set(WifiConfiguration.PairwiseCipher.CCMP);
//            config.status = WifiConfiguration.Status.ENABLED;
        } 
        
        return config;
    }
    
    public static final int WIFI_CONNECTED = 0x01;
    public static final int WIFI_CONNECT_FAILED = 0x02;
    public static final int WIFI_CONNECTING = 0x03;
    /**
     * 判断wifi是否连接成功,不是network
     * 
     * @param context
     * @return
     */
    public int isWifiContected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo wifiNetworkInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        
        Log.v(TAG, "isConnectedOrConnecting = " + wifiNetworkInfo.isConnectedOrConnecting());
        Log.d(TAG, "wifiNetworkInfo.getDetailedState() = " + wifiNetworkInfo.getDetailedState());
        if (wifiNetworkInfo.getDetailedState() == DetailedState.OBTAINING_IPADDR
                || wifiNetworkInfo.getDetailedState() == DetailedState.CONNECTING) {
            return WIFI_CONNECTING;
        } else if (wifiNetworkInfo.getDetailedState() == DetailedState.CONNECTED) {
            return WIFI_CONNECTED;
        } else {
            Log.d(TAG, "getDetailedState() == " + wifiNetworkInfo.getDetailedState());
            return WIFI_CONNECT_FAILED;
        }
    }

    //查看以前是否也配置过这个网络
    private WifiConfiguration IsExsits(String SSID) {
        List<WifiConfiguration> existingConfigs = mWifiManager.getConfiguredNetworks();
        for (WifiConfiguration existingConfig : existingConfigs) {
            if (existingConfig.SSID.equals("\"" + SSID + "\"")) {
                return existingConfig;
            }
        }
        return null;
    }
    

    // 断开指定ID的网络
    public void disconnectWifi(int netId) {
        mWifiManager.disableNetwork(netId);
        mWifiManager.disconnect();
    }
    
    // 检查当前WIFI状态
    public int checkState() {
        return mWifiManager.getWifiState();
    }

    // 锁定WifiLock
    public void acquireWifiLock() {
        mWifiLock.acquire();
    }

    // 解锁WifiLock
    public void releaseWifiLock() {
        // 判断时候锁定
        if (mWifiLock.isHeld()) {
            mWifiLock.acquire();
        }
    }

    // 创建一个WifiLock
    public void creatWifiLock() {
        mWifiLock = mWifiManager.createWifiLock("Test");
    }

    // 得到配置好的网络
    public List<WifiConfiguration> getConfiguration() {
        return mWifiConfiguration;
    }

    // 指定配置好的网络进行连接
    public void connectConfiguration(int index) {
        // 索引大于配置好的网络索引返回
        if (index > mWifiConfiguration.size()) {
            return;
        }
        // 连接配置好的指定ID的网络
        mWifiManager.enableNetwork(mWifiConfiguration.get(index).networkId,true);
    }

    public void startScan() {
        if(mWifiManager.startScan()){
            mWifiList = mWifiManager.getScanResults();
            mWifiConfiguration = mWifiManager.getConfiguredNetworks();
        }
    }

    // 得到网络列表
    public List<ScanResult> getWifiList() {
        return mWifiList;
    }

    // 查看扫描结果
    public StringBuilder lookUpScan() {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < mWifiList.size(); i++) {
            stringBuilder
                    .append("Index_" + new Integer(i + 1).toString() + ":");
            // 将ScanResult信息转换成一个字符串包
            // 其中把包括：BSSID、SSID、capabilities、frequency、level
            stringBuilder.append((mWifiList.get(i)).toString());
            stringBuilder.append("/n");
        }
        return stringBuilder;
    }

    // 得到MAC地址
    public String getMacAddress() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getMacAddress();
    }

    // 得到接入点的BSSID
    public String getBSSID() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.getBSSID();
    }

    // 得到IP地址
    public int getIPAddress() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getIpAddress();
    }

    // 得到连接的ID
    public int getNetworkId() {
        return (mWifiInfo == null) ? 0 : mWifiInfo.getNetworkId();
    }

    // 得到WifiInfo的所有信息包
    public String getWifiInfo() {
        return (mWifiInfo == null) ? "NULL" : mWifiInfo.toString();
    }
}
