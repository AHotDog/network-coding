package com.example.android.nc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import android.net.DhcpInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends Activity {
    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("native-lib");
    }
    private TextView tvMsg;
    private EditText  txtEt;
    private Button btnSend;
    private Button btnRec;
    private Button btnOpen;
    private Button btnEncode;
    private Button btnReencode;
    private Button btnRecover;
    private Button btnBrowse;
    private Button btnTest;

    private LinearLayout ll;
    private LinearLayout l2;
    private Handler handler;
    private SocketManager socketManager;
    private List<String> ip=null;
    final static String TAG = "robin";
    private WifiAdmin mWifiAdmin=null;
    private Context context;
    private WifiApAdmin wifiAp;
    private WifiManager wm;
    private long exittime=0;
    private ArrayList<String> fileName;
    private ArrayList<String> filePath;
    private ArrayList<String> recFileName;
    private ArrayList<String> filePath2;

    private ArrayList<String> refileName;
    private ArrayList<String> refilePath;
    private String searchPath;
    private String fpath;
    private ArrayList<TextView> progresspics;
    private int cutFileNum;
    private int encodeFileNum;
    private SimpleDateFormat format;
    private static final int SendCode= 1;
    private static final int OpenCode= 2;
    private static final int ReencodeCode=3;
    private static final int BrowseCode=4;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recFileName=new ArrayList<String>();

        format=new SimpleDateFormat("hh:mm:ss");


        context = this;
        ll=(LinearLayout) findViewById(R.id.lll);
        tvMsg = (TextView)findViewById(R.id.tvMsg);
        txtEt=(EditText) findViewById(R.id.et);

		/*btnSend按钮点击事件处理函数----打开wifi热点；选择文件*/
        btnSend = (Button)findViewById(R.id.btnSend);
        btnSend.setOnClickListener(new OnClickListener(){
            @Override
            public void onClick(View v) {
                wifiAp = new WifiApAdmin(context);
                wifiAp.startWifiAp(Constant.HOST_SPOT_SSID,
                        Constant.HOST_SPOT_PASS_WORD);
                //	Message.obtain(handler, 0, "开启热点成功").sendToTarget();
                //	Message.obtain(handler, 1, ).sendToTarget();

                Intent intent = new Intent(getApplicationContext(), FilesViewActivity.class);
                startActivityForResult(intent, SendCode);
                //	btnRec.setVisibility(View.INVISIBLE);
            }
        });
//		btnopen = (Button)findViewById(R.id.btnOpen);
//		btnopen.setOnClickListener(new OnClickListener() {
//
//			@Override
//			public void onClick(View v) {
//				wifiAp = new WifiApAdmin(context);
//				wifiAp.startWifiAp(Constant.HOST_SPOT_SSID,
//						Constant.HOST_SPOT_PASS_WORD);
//				Message.obtain(handler, 0, "开启热点成功").sendToTarget();
////				new Thread(){
////					public void run() {
////						while(true)
////						try {
////							currentThread().sleep(10000);
////							ip=getConnectedIP();
////							Message.obtain(handler, 0, "当前连接数 "+ip.size()).sendToTarget();
////							ip.clear();
////						} catch (InterruptedException e) {
////							e.printStackTrace();
////						}
////
////					};
////				}.start();
//
//			}
//		});

//		 TextView et = new TextView(this);
//		 et.setWidth(20);
//	        et.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_launcher));
//	        TextView et1 = new TextView(this);
//			 et1.setWidth(20);
//		        et1.setBackgroundDrawable(getResources().getDrawable(R.drawable.ic_launcher));
//		ll.addView(et );
//		ll.addView(et1 );
        btnRec = (Button)findViewById(R.id.btnrec);
        btnRec.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {

                mWifiAdmin = new WifiAdmin(context) {

                    @Override
                    public void myUnregisterReceiver(BroadcastReceiver receiver) {
                        unregisterReceiver(receiver);
                    }

                    @Override
                    public Intent myRegisterReceiver(
                            BroadcastReceiver receiver, IntentFilter filter) {
                        registerReceiver(receiver, filter);
                        return null;
                    }

                    @Override
                    public void onNotifyWifiConnected() {
                        Message.obtain(handler, 0, "连接Wifi成功").sendToTarget();
                    }

                    @Override
                    public void onNotifyWifiConnectFailed() {
                        //if(wm.getWifiState()==WifiManager.WIFI_STATE_ENABLED)
                        //	Message.obtain(handler, 0, "连接Wifi成功").sendToTarget();
                        //	else
                        // Message.obtain(handler, 0, "连接Wifi失败,请重试！！！").sendToTarget();
                    }

                };

                //接收文件
                socketManager.recfile();
                wm=(WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
                mWifiAdmin.openWifi();
                new Thread(){
                    public void run() {
                        Message.obtain(handler, 0, "正在连接Wifi。。。").sendToTarget();
                        while(!wm.isWifiEnabled()){
                            //mWifiAdmin.openWifi();
                        }

                        mWifiAdmin.addNetwork(mWifiAdmin.createWifiInfo(
                                Constant.HOST_SPOT_SSID, Constant.HOST_SPOT_PASS_WORD,
                                WifiAdmin.TYPE_WPA));
                        DhcpInfo info=wm.getDhcpInfo();
                        int i=info.serverAddress;
                        String s= (i & 0xFF) + "." +
                                ((i >> 8 ) & 0xFF) + "." +
                                ((i >> 16 ) & 0xFF)+ "." +
                                ((i >> 24 ) & 0xFF );
                        Message.obtain(handler, 0, "连接到 "+s+"成功").sendToTarget();
                    }
                }.start();
//			while(!wm.isWifiEnabled()){
//				//mWifiAdmin.openWifi();
//				};
//				Message.obtain(handler, 0, "正在连接Wifi。。。").sendToTarget();
//				mWifiAdmin.addNetwork(mWifiAdmin.createWifiInfo(
//						Constant.HOST_SPOT_SSID, Constant.HOST_SPOT_PASS_WORD,
//						WifiAdmin.TYPE_WPA));

            }
        });
        btnTest=(Button)findViewById(R.id.btnTest);
        btnTest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                //byte[][] temp2={{(byte) 1,(byte) 2,(byte) 3},{(byte) 4,(byte) 5,(byte) 6},{(byte) 7,(byte) 8,(byte) 9}};
//                int a=0;
//                byte[][] temp2={{1,2,3},{4,5,6},{7,8,9}};
//                byte[][] result=Encode();
//                for(int i=0;i<3;i++){
//                    txtEt.append("\n");
//                    for(int j=0;j<3;j++) {
//                        a = result[i][j] & 0x000000FF;
//                        txtEt.append(a + ",");
//                    }
//                }
//                byte temp=Testcode();
//                txtEt.append("\n["+temp+"]");
//                txtEt.append("\n[" + format.format(new Date()) + "]" +"编码成功");

                //删除文件

                //删除文件
                recFileName=new ArrayList<String>();
                searchPath = Environment.getExternalStorageDirectory().getPath() + "/cutfiles/";
                String[] files = new File(searchPath).list(new FilenameFilter() {
                    @Override
                    public boolean accept(File dir, String name) {
                        if ( name.endsWith(".db"))
                            return true;
                        else
                            return false;
                    }
                });
                if(files!=null&&files.length>0) {
                    txtEt.append("\n[" + format.format(new Date()) + "]" + "接收到" + files.length + "个文件");
                    for (int i = 0; i < files.length; i++) {
                            recFileName.add(files[i]);

                    }

                    try {
                        FileCut fc = new FileCut();

                        for (int i = 0; i < files.length; i++) {
                            fc.delete(searchPath + files[i]);
                            txtEt.append("\n[" + format.format(new Date()) + "]" + "已删除文件" + files[i]);
                        }

                    } catch (Exception e) {
                        e.printStackTrace();

                    }
                }
            }
        });

        btnOpen=(Button)findViewById(R.id.btnOpen);
        btnOpen.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FilesViewActivity.class);
                startActivityForResult(intent, OpenCode);
            }
        });
        btnEncode=(Button)findViewById(R.id.btnEncode);
        btnEncode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    if(fileName.size()>0) {
                        FileCut fc = new FileCut(filePath.get(0), fileName.get(0), ".db");
                        encodeFileNum = fc.Encode();
                        txtEt.append("\n[" + format.format(new Date()) + "]" + fileName + "文件已分割");
                    }else{
                        txtEt.append("\n[" + format.format(new Date()) + "]" + "文件读取失败！");
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    txtEt.append("\n[" + format.format(new Date()) + "]" + "文件读取失败！");
                }
            }
        });
        btnReencode=(Button)findViewById(R.id.btnReencode);
        btnReencode.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FilesViewActivity.class);
                intent.putExtra("key",ReencodeCode);
                startActivityForResult(intent, ReencodeCode);
            }
        });

        btnRecover=(Button)findViewById(R.id.btnRecover);
        btnRecover.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                recFileName=new ArrayList<String>();

                //查找所有文件名中包含选中文件名的文件
                if(fileName.size()>0) {
                    cutFileNum = 4;
                    searchPath = Environment.getExternalStorageDirectory().getPath() + "/cutfiles/";
                    String[] files = new File(searchPath).list(new FilenameFilter() {
                        @Override
                        public boolean accept(File dir, String name) {
                            if (name.startsWith(fileName.get(0)) && name.endsWith(".db"))
                                return true;
                            else
                                return false;
                        }
                    });
                    if(files!=null&&files.length>0) {
                        txtEt.append("\n[" + format.format(new Date()) + "]" + "接收到" + files.length + "个文件");
                        for (int i = 0; i < files.length; i++) {
                            if (i < cutFileNum)
                                recFileName.add(files[i]);
                            txtEt.append("\n[" + format.format(new Date()) + "]" + files[i]);

                        }
                        txtEt.append("\n[" + format.format(new Date()) + "]" + "使用了" + cutFileNum + "个文件");
                        String dst = Environment.getExternalStorageDirectory().getPath()
                                + "/files/" + fileName.get(0);
                        try {
                            FileCut fc=new FileCut();
                            fc.Recover(recFileName, dst);
                            txtEt.append("\n[" + format.format(new Date()) + "]" + "已恢复文件："+fileName.get(0)+",该文件存储在"+ Environment.getExternalStorageDirectory().getPath()
                                    + "/files/");
                            for(int i=0;i<files.length;i++){
                                fc.delete(searchPath+files[i]);
                                txtEt.append("\n[" + format.format(new Date()) + "]" + "已删除文件"+files[i]);
                            }

                        } catch (Exception e) {
                            e.printStackTrace();

                        }
                    }else{
                        txtEt.append("\n[" + format.format(new Date()) + "]" + "未选中文件！");
                    }

                }
                else{
                    txtEt.append("\n[" + format.format(new Date()) + "]" + "未选中文件！");
                }

            }
        });

        btnBrowse=(Button)findViewById(R.id.btnBrowse);
        btnBrowse.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getApplicationContext(), FilesViewActivity.class);
                intent.putExtra("key",BrowseCode);
                startActivityForResult(intent, BrowseCode);
            }
        });

        handler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                switch(msg.what){
                    case 0:
                        SimpleDateFormat format = new SimpleDateFormat("hh:mm:ss");
                        txtEt.append("\n[" + format.format(new Date()) + "]" + msg.obj.toString());
                        break;
                    case 1:
                        tvMsg.setText("本机mAC：" + GetMacAddress());
                        break;
                    case 2:
                        Toast.makeText(getApplicationContext(), msg.obj.toString(), Toast.LENGTH_SHORT).show();
                        break;
                    case 3: Thread sendThread = new Thread(new Runnable(){
                        @Override
                        public void run() {
                            wifiAp = new WifiApAdmin(context);
                            wifiAp.startWifiAp(Constant.HOST_SPOT_SSID,
                                    Constant.HOST_SPOT_PASS_WORD);
                            while(socketManager.getConnectedIP().size()<=0){

                            }
                            Log.i("log", fpath);
                            //socketManager.SendFile(fpath);
                        }
                    });
                        sendThread.start();break;
                    case 4:progressbar(msg);break;
                    case 5:handd(msg);break;
                }
            }

            private void handd(Message msg) {
                int n=Integer.parseInt(msg.obj.toString());
                progresspics.get(n-1).setBackgroundDrawable(getResources().getDrawable(R.drawable.done));
            }

            private void progressbar(Message msg) {
                progresspics=new ArrayList<TextView>();
                int n=Integer.parseInt(msg.obj.toString());
                DisplayMetrics metric = new DisplayMetrics();
                getWindowManager().getDefaultDisplay().getMetrics(metric);
                for(int i=0;i<n;i++)
                {
                    TextView et = new TextView(MainActivity.this);
                    int w=metric.widthPixels;
                    et.setWidth(w/n);
                    et.setBackgroundDrawable(getResources().getDrawable(R.drawable.undone));
                    // et.setVisibility(View.INVISIBLE);
                    progresspics.add(et);
                    ll.addView(et);
                }

            }
        };
        fpath="";
        socketManager = new SocketManager(handler,this,fpath);
        if(!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED))
            Message.obtain(handler, 0, "未发现SD卡，程序无法完成接受文件！！！").sendToTarget();
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode){
            case SendCode://选择了文件发送
                if (resultCode == RESULT_OK){
                    fileName = data.getStringArrayListExtra("fileName");
                    filePath = data.getStringArrayListExtra("safeFileName");
                    txtEt.append("\n[" + format.format(new Date()) + "]" +"选中文件："+fileName);

                    Thread sendThread = new Thread(new Runnable(){
                        @Override
                        public void run() {
                            socketManager.SendFile1(fileName,filePath);
                        }
                    });
                    sendThread.start();
                }
                break;

            case OpenCode:
                if (resultCode == RESULT_OK){
                    fileName = data.getStringArrayListExtra("fileName");
                    filePath = data.getStringArrayListExtra("safeFileName");
                    txtEt.append("\n[" + format.format(new Date()) + "]" +"选中文件："+fileName);
                }
                break;

            case ReencodeCode:
                refileName=new ArrayList<String>();
                if (resultCode == RESULT_OK){
                    refileName = data.getStringArrayListExtra("fileName");
                    refilePath = data.getStringArrayListExtra("safeFileName");
                    txtEt.append("\n[" + format.format(new Date()) + "]" +"选中文件："+refileName);
                    ArrayList<String> resultfileName= new FileCut().Reencode(refileName);
                    txtEt.append("\n[" + format.format(new Date()) + "]" +"再编码文件保存为："+resultfileName);
                }
                break;
            case BrowseCode:
                refileName=new ArrayList<String>();
                if (resultCode == RESULT_OK){
                    refileName = data.getStringArrayListExtra("fileName");
                    refilePath = data.getStringArrayListExtra("safeFileName");
                    File f=new File(refilePath.get(0));
                    Openfile(f);
                }
                break;
            default:
                break;

        }

    }
    private void Openfile(File currentPath){
        if(currentPath!=null&&currentPath.isFile())
        {
            String fileName = currentPath.toString();
            Intent intent;
            if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingImage))){
                intent = OpenFiles.getImageFileIntent(currentPath);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingWebText))){
                intent = OpenFiles.getHtmlFileIntent(currentPath);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingPackage))){
                intent = OpenFiles.getApkFileIntent(currentPath);
                startActivity(intent);

            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingAudio))){
                intent = OpenFiles.getAudioFileIntent(currentPath);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingVideo))){
                intent = OpenFiles.getVideoFileIntent(currentPath);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingText))){
                intent = OpenFiles.getTextFileIntent(currentPath);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingPdf))){
                intent = OpenFiles.getPdfFileIntent(currentPath);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingWord))){
                intent = OpenFiles.getWordFileIntent(currentPath);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingExcel))){
                intent = OpenFiles.getExcelFileIntent(currentPath);
                startActivity(intent);
            }else if(checkEndsWithInStringArray(fileName, getResources().
                    getStringArray(R.array.fileEndingPPT))){
                intent = OpenFiles.getPPTFileIntent(currentPath);
                startActivity(intent);
            }else
            {
                //showMessage("无法打开，请安装相应的软件！");
            }
        }else
        {
         //   showMessage("对不起，这不是文件！");
        }
    }

    private boolean checkEndsWithInStringArray(String checkItsEnd,
                                               String[] fileEndings){
        for(String aEnd : fileEndings){
            if(checkItsEnd.endsWith(aEnd))
                return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            wifiAp.closeWifiAp(this);
            if(mWifiAdmin==null){
                mWifiAdmin=new WifiAdmin(this) {

                    @Override
                    public void onNotifyWifiConnected() {
                    }

                    @Override
                    public void onNotifyWifiConnectFailed() {
                    }

                    @Override
                    public void myUnregisterReceiver(BroadcastReceiver receiver) {
                    }

                    @Override
                    public Intent myRegisterReceiver(BroadcastReceiver receiver,
                                                     IntentFilter filter) {
                        return null;
                    }
                };
            }
            mWifiAdmin.closeWifi();
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.exit(0);
    }
    public String GetIpAddress() {
        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int i = wifiInfo.getIpAddress();
        return (i & 0xFF) + "." +
                ((i >> 8 ) & 0xFF) + "." +
                ((i >> 16 ) & 0xFF)+ "." +
                ((i >> 24 ) & 0xFF );
    }
    public String GetMacAddress() {
        WifiManager wifiManager = (WifiManager)context.getApplicationContext().getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        String i = wifiInfo.getMacAddress();
        return i;
    }



    @Override
    public void onBackPressed() {
        if (System.currentTimeMillis() - exittime > 2000) {
            Toast.makeText(getApplicationContext(), "再按一次退出程序",
                    Toast.LENGTH_SHORT).show();
            exittime = System.currentTimeMillis();
        } else
            finish();
    }
    public ArrayList<String> getConnectedIP() {
        ArrayList<String> connectedIP = new ArrayList<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader("/proc/net/arp"));
            String line;
            br.readLine();
            while ((line = br.readLine()) != null) {
                String[] splitted = line.split(" +");
                if (splitted != null && splitted.length >= 4) {
                    String ip = splitted[0];
                    connectedIP.add(ip);
                    Log.i("log", ip);
                }
            }
            br.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return connectedIP;
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native byte[][] Encode();
    public native byte Testcode();
    //public native byte[][] TestDecode(byte[][] buffer,int nPart,int nLength);
    public native byte[][] TestDecode(byte[][] buffer,int nPart,int nLength);
//    public native String UninitGalois();
//    public native int ADD(int i,int j);
//    public native byte[]  NCDecoding(byte[] Data, int nLen);
//    public native byte[]  InverseMatrix(byte[] Data, int nK);
//    public native byte[] matrixMul(byte[] matrixA,int aRow,int aCol,byte[] matrixB,int bRow,int bCol);
}

