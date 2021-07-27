package com.example.reader.test;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.format.Time;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fpreader.fpdevice.BluetoothReader;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

public class BluetoothDeviceTest1Activity extends AppCompatActivity {
    private static final int MY_PERMISSION_REQUEST_CODE = 10000;
    private String curPath="";

    private boolean bUsePIN=false;
    private boolean bAutoSetPIN=false;

    private BluetoothAdapter mBluetoothAdapter = null;
    private BluetoothReader  mBluetoothReader =null;

    private String btAddress="";
    private boolean bAutoLink=true;
    private Timer linkTimer;
    private TimerTask linkTask;
    Handler linkHandler;

    private TextView mTitle;
    private ListView mConversationView;
    private ImageView fingerprintImage;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private EditText editText1;

    public  int worktype=0;

    public byte mRefData[]=new byte[512];
    public int mRefSize=0;
    public byte mMatData[]=new byte[512];
    public int mMatSize=0;

    public byte mBat[]=new byte[2];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device_test1);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if(Build.VERSION.SDK_INT < 23) {
            initData();
        }else {
            checkPermission();
        }

        SharedPreferences sp;
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        btAddress =sp.getString("device","");

    }

    @Override
    public void onStart(){
        super.onStart();

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, BluetoothDeviceListActivity.REQUEST_ENABLE_BT);
        }else{
            if(mBluetoothReader==null){
                InitViews();
                mBluetoothReader = new BluetoothReader(this, mHandler);
                mBluetoothReader.InitMatch();
                AddStatusList(btAddress);
                if(bAutoLink){
                    LinkStart();
                }
            }
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (mBluetoothReader != null) {
            if (mBluetoothReader.getState() == mBluetoothReader.STATE_NONE) {
                mBluetoothReader.start();
            }
        }
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBluetoothReader != null)
            mBluetoothReader.stop();
    }

    @SuppressLint("HandlerLeak")
    public void LinkStart(){
        linkTimer = new Timer();
        linkHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                linkStop();

                if(btAddress.length()>=12){
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(btAddress);
                    mBluetoothReader.connect(device);
                }

                super.handleMessage(msg);
            }
        };
        linkTask = new TimerTask() {
            @Override
            public void run() {
                Message message = new Message();
                message.what = 1;
                linkHandler.sendMessage(message);
            }
        };
        linkTimer.schedule(linkTask, 1000, 1000);
    }

    public void linkStop(){
        if (linkTimer!=null){
            linkTimer.cancel();
            linkTimer = null;
            linkTask.cancel();
            linkTask=null;
        }
    }

    private void initData(){
        getDirectory();
    }

    private void InitViews(){
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        fingerprintImage=(ImageView)findViewById(R.id.imageView1);
        mTitle=(TextView)findViewById(R.id.textView1);
        editText1=(EditText)findViewById(R.id.editText1);
        editText1.setText("0000");

        if(!bUsePIN) {
            LinearLayout linearLayoutPIN = (LinearLayout) findViewById(R.id.linearLayoutPIN);
            linearLayoutPIN.setVisibility(View.INVISIBLE);//View.GONE
        }

        final Button btnEnrolNoImage = (Button) findViewById(R.id.btnEnrolNoImage);
        btnEnrolNoImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    AddStatusList(getString(R.string.txt_placefinger2));
                    mBluetoothReader.EnrolToHost();
                }
            }
        });

        final Button btnCaptureNoImage = (Button) findViewById(R.id.btnCaptureNoImage);
        btnCaptureNoImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    AddStatusList(getString(R.string.txt_placefinger1));
                    mBluetoothReader.CaptureToHost();
                }
            }
        });

        final Button btnEnrolWithImage = (Button) findViewById(R.id.btnEnrolWithImage);
        btnEnrolWithImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    worktype = 0;
                    AddStatusList(getString(R.string.txt_placefinger1));
                    mBluetoothReader.GetImageAndDate();
                }
            }
        });

        final Button btnCaptureWithImage = (Button) findViewById(R.id.btnCaptureWithImage);
        btnCaptureWithImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    worktype = 1;
                    AddStatusList(getString(R.string.txt_placefinger1));
                    mBluetoothReader.GetImageAndDate();
                }
            }
        });

        final Button btnGetSN = (Button) findViewById(R.id.btnGetSN);
        btnGetSN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    mBluetoothReader.GetDeviceSn();
                }
            }
        });

        final Button btnSetPIN = (Button) findViewById(R.id.btnSetPIN);
        btnSetPIN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    byte[] pinval = editText1.getText().toString().getBytes();
                    //String pins="0000";
                    //byte[] pinval=pins.getBytes();
                    mBluetoothReader.VerifyPIN(pinval);
                }
            }
        });

        final Button btnGetBatVal = (Button) findViewById(R.id.btnGetBatVal);
        btnGetBatVal.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    mBluetoothReader.GetBatVal();
                }
            }
        });

        final Button btnCloseDevice = (Button) findViewById(R.id.btnCloseDevice);
        btnCloseDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(mBluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    mBluetoothReader.CloseDevvice();
                }
            }
        });

    }

    private void AddStatusList(String text) {
        mConversationArrayAdapter.add(text);
    }

    private void AddStatusListHex(byte[] data,int size) {
        String text="";
        for(int i=0;i<size;i++) {
            text=text+","+Integer.toHexString(data[i]&0xFF).toUpperCase();
        }
        mConversationArrayAdapter.add(text);
    }

    @SuppressLint("HandlerLeak")
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothReader.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothReader.STATE_CONNECTED:
                            mTitle.setText(R.string.title_connected_to);
                            mTitle.append(mConnectedDeviceName);
                            mConversationArrayAdapter.clear();
                            break;
                        case BluetoothReader.STATE_CONNECTING:
                            mTitle.setText(R.string.title_connecting);
                            break;
                        case BluetoothReader.STATE_LISTEN:
                        case BluetoothReader.STATE_NONE:
                            mTitle.setText(R.string.title_not_connected);
                            break;
                    }
                    break;
                case BluetoothReader.MESSAGE_WRITE:
                    break;
                case BluetoothReader.MESSAGE_READ:
                    break;
                case BluetoothReader.MESSAGE_DEVICE_NAME:
                    mConnectedDeviceName = msg.getData().getString(BluetoothReader.DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), getString(R.string.txt_connectedto) + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case BluetoothReader.MESSAGE_TOAST:
                    //Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothReader.TOAST),Toast.LENGTH_SHORT).show();
                    switch(msg.getData().getInt(BluetoothReader.MSGVAL)) {
                        case BluetoothReader.MSG_UNABLE:
                            Toast.makeText(getApplicationContext(),getString(R.string.title_unable_connected),Toast.LENGTH_SHORT).show();
                            break;
                        case BluetoothReader.MSG_LOST:
                            Toast.makeText(getApplicationContext(), getString(R.string.title_lost_connected),Toast.LENGTH_SHORT).show();
                            break;
                    }
                    break;
                case BluetoothReader.CMD_GETDEVTYPE:
                    if(msg.arg1==1){
                        AddStatusList(getString(R.string.txt_devicetype)+String.format("%08X",msg.arg2));
                    }else
                        AddStatusList(getString(R.string.txt_getdevicefail));
                    break;
                case BluetoothReader.CMD_CHKPIN:
                    if(msg.arg1==1)
                        AddStatusList(getString(R.string.txt_pinok));
                    else
                        AddStatusList(getString(R.string.txt_pinfail));
                    break;
                case BluetoothReader.CMD_GETDEVINFO:
                    if(msg.arg1==1){
                        AddStatusList(getString(R.string.txt_devicetype)+String.format("%08X",mBluetoothReader.mDeviceInfo.DeviceType));

                        String sn=new String(mBluetoothReader.mDeviceInfo.DeviceSN);
                        AddStatusList(getString(R.string.txt_devicesn)+sn);

                        AddStatusList(getString(R.string.txt_devicever)+String.valueOf(mBluetoothReader.mDeviceInfo.Version));
                        AddStatusList(getString(R.string.txt_devicesensor)+String.valueOf(mBluetoothReader.mDeviceInfo.SensorType));

                        //PIN Code
                        if(bUsePIN){
                            if(bAutoSetPIN){
                                //Auto Verify Pin
                                //byte[] pinval=editText1.getText().toString().getBytes();
                                {
                                    AddStatusList(getString(R.string.txt_autosetpin));
                                    String pins="0000";
                                    mBluetoothReader.VerifyPIN(pins.getBytes());
                                }
                            }else{
                                AddStatusList(getString(R.string.txt_usepin));
                            }
                        }else{
                            AddStatusList(getString(R.string.txt_notusepin));
                        }

                    }else
                        AddStatusList(getString(R.string.txt_getdevicefail));
                    break;

                case BluetoothReader.CMD_GETSTDIMAGE:
                    if(msg.arg1==1){
                        byte[] bmpdata = null;
                        bmpdata=mBluetoothReader.getFingerprintImage((byte[]) msg.obj,256,288,0);
                        Bitmap image = BitmapFactory.decodeByteArray(bmpdata, 0,bmpdata.length);
                        fingerprintImage.setImageBitmap(image);
                        //SaveImage(image);
                        AddStatusList(getString(R.string.txt_getimageok));
                    }else{
                        AddStatusList(getString(R.string.txt_getimagefail));
                    }
                    break;
                case BluetoothReader.CMD_GETRESIMAGE:
                    if(msg.arg1==1){
                        byte[] bmpdata = null;
                        switch(msg.arg2){
                            case BluetoothReader.IMAGESIZE_152_200:
                                bmpdata=mBluetoothReader.getFingerprintImage((byte[]) msg.obj,152,200,0);
                                break;
                            case BluetoothReader.IMAGESIZE_256_288:
                                bmpdata=mBluetoothReader.getFingerprintImage((byte[]) msg.obj,256,288,0);
                                break;
                            case BluetoothReader.IMAGESIZE_256_360:
                                bmpdata=mBluetoothReader.getFingerprintImage((byte[]) msg.obj,256,360,0);
                                break;
                        }
                        Bitmap image = BitmapFactory.decodeByteArray(bmpdata, 0,bmpdata.length);
                        fingerprintImage.setImageBitmap(image);
                        //SaveImage(image);
                        AddStatusList(getString(R.string.txt_getimageok));
                    }else{
                        AddStatusList(getString(R.string.txt_getimagefail));
                    }
                    break;
                case BluetoothReader.CMD_GETSTDCHAR:
                case BluetoothReader.CMD_GETRESCHAR:
                    if(msg.arg1==1){
                        if(worktype==1){
                            mBluetoothReader.memcpy(mMatData,0,(byte[]) msg.obj,0,msg.arg2);
                            mMatSize=msg.arg2;
                            //AddStatusList("Len="+String.valueOf(mMatSize));
                            AddStatusList(getString(R.string.txt_captureok));
                            //AddStatusListHex(mMatData,mMatSize);

                            if(mRefSize>0) {
                                int score = mBluetoothReader.MatchTemplate(mRefData, mMatData);
                                AddStatusList(getString(R.string.txt_matchscore) + String.valueOf(score));
                            }

                        }else{
                            mBluetoothReader.memcpy(mRefData,0,(byte[]) msg.obj,0,msg.arg2);
                            mRefSize=msg.arg2;
                            //AddStatusList("Len="+String.valueOf(mRefSize));
                            AddStatusList(getString(R.string.txt_enrolok));
                            //AddStatusListHex(mRefData,mRefSize);
                        }

                    }else {
                        if(worktype==1){
                            AddStatusList(getString(R.string.txt_capturefail));
                        }else{
                            AddStatusList(getString(R.string.txt_enrolfail));
                        }
                    }
                    break;
                case BluetoothReader.CMD_ENROLHOST:
                    if(msg.arg1==1){
                        //byte[] readBuf = (byte[]) msg.obj;
                        mRefSize=msg.arg2;
                        mBluetoothReader.memcpy(mRefData,0, (byte[])msg.obj,0,msg.arg2);
                        AddStatusList(getString(R.string.txt_enrolok));
                        //AddStatusListHex(mRefData,512);
                    }else
                        AddStatusList(getString(R.string.txt_enrolfail));
                    break;
                case BluetoothReader.CMD_CAPTUREHOST:
                    if(msg.arg1==1){
                        //byte[] readBuf = (byte[]) msg.obj;
                        mMatSize=msg.arg2;
                        mBluetoothReader.memcpy(mMatData,0, (byte[]) msg.obj,0,msg.arg2);
                        AddStatusList(getString(R.string.txt_captureok));
                        //AddStatusListHex(mMatData,256);

                        if(mRefSize>0) {
                            int score = mBluetoothReader.MatchTemplate(mRefData, mMatData);
                            AddStatusList(getString(R.string.txt_matchscore) + String.valueOf(score));
                        }
                    }else
                        AddStatusList(getString(R.string.txt_capturefail));
                    break;
                case BluetoothReader.CMD_GETSN:
                    if(msg.arg1==1){
                        String sn=new String(mBluetoothReader.mDeviceInfo.DeviceSN);
                        AddStatusList(getString(R.string.txt_devicesn)+sn);
                    }else
                        AddStatusList(getString(R.string.txt_getsnfail));
                    break;
                case BluetoothReader.CMD_GETBAT:
                    if(msg.arg1==1){
                        mBluetoothReader.memcpy(mBat,0,(byte[]) msg.obj,0,msg.arg2);
                        AddStatusList(getString(R.string.txt_batval)+Integer.toString(mBat[0]/10)+"."+Integer.toString(mBat[0]%10)+"V");
                    }else
                        AddStatusList(getString(R.string.txt_getbatvalfail));
                    break;
                case BluetoothReader.CMD_SHUTDOWNDEVICE: {
                    if(msg.arg1==1){
                        AddStatusList(getString(R.string.txt_closeok));
                    } else
                        AddStatusList(getString(R.string.txt_closefail));
                }
                break;
            }
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch(id){
            case R.id.action_linkdevice:
                LinkBluetoothReader();
                return true;
            case android.R.id.home:
                this.finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void LinkBluetoothReader(){
        Intent serverIntent = new Intent(BluetoothDeviceTest1Activity.this, BluetoothDeviceListActivity.class);
        startActivityForResult(serverIntent, BluetoothDeviceListActivity.REQUEST_CONNECT_DEVICE);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case BluetoothDeviceListActivity.REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {

                    mBluetoothReader.stop();

                    String address = data.getExtras().getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    mBluetoothReader.connect(device);

                    btAddress = address;

                    SharedPreferences sp;
                    sp = PreferenceManager.getDefaultSharedPreferences(this);
                    SharedPreferences.Editor edit=sp.edit();
                    edit.putString("device",btAddress);
                    edit.commit();
                }
                break;
            case BluetoothDeviceListActivity.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    if(mBluetoothReader==null)
                        InitViews();
                    mBluetoothReader = new BluetoothReader(this, mHandler);
                    mBluetoothReader.InitMatch();

                    if(bAutoLink){
                        if(btAddress.length()>=12){
                            BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(btAddress);
                            mBluetoothReader.connect(device);
                        }
                    }
                }else{
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    //
    public void getDirectory(){
        String storageDir = Environment.getExternalStorageDirectory().toString();
        //Toast.makeText(this,storageDir,Toast.LENGTH_LONG).show();
        //curPath=storageDir+"/Android/data/com.dropbox.android/files";
        curPath=storageDir+"/Fingerprint";
        File file = new File(curPath);
        if (!file.exists()) {
            file.mkdirs();
        }
    }

    public static String getDataTimeForID(){
        Time t=new Time();
        t.setToNow();
        int year = t.year;
        int month = t.month+1;
        int date = t.monthDay;
        int hour = t.hour;
        int minute = t.minute;
        int second = t.second;
        return String.format("%d%02d%02d%02d%02d%02d",year,month,date,hour,minute,second);
    }

    private void SaveImage(Bitmap image){
        File f = new File(curPath+"/"+getDataTimeForID()+editText1.getText().toString()+".png");
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            image.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        /*
        f = new File(curPath+"/"+getDataTimeForID()+editText1.getText().toString()+".jpg");
        if (f.exists()) {
            f.delete();
        }
        try {
            FileOutputStream out = new FileOutputStream(f);
            image.compress(Bitmap.CompressFormat.JPEG, 90, out);
            out.flush();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */
    }

    //Permission
    public void checkPermission() {
        boolean isAllGranted = checkPermissionAllGranted(
                new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                }
        );
        if (isAllGranted) {
            initData();
            return;
        }
        ActivityCompat.requestPermissions(
                this,
                new String[] {
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                },MY_PERMISSION_REQUEST_CODE
        );
    }

    private boolean checkPermissionAllGranted(String[] permissions) {
        for (String permission : permissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                toast("Permission Fail");
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == MY_PERMISSION_REQUEST_CODE) {
            boolean isAllGranted = true;

            for (int grant : grantResults) {
                if (grant != PackageManager.PERMISSION_GRANTED) {
                    isAllGranted = false;
                    break;
                }
            }

            if (isAllGranted) {
                initData();
            } else {
                toast("Permission Fail");
            }
        }
    }
    public void toast(String content){
        Toast.makeText(getApplicationContext(),content,Toast.LENGTH_SHORT).show();
    }
}
