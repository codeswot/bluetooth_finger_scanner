package com.example.reader.test;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.fpreader.fpdevice.AsyncBluetoothReader;
import com.fpreader.fpdevice.BluetoothReader;

public class BluetoothDeviceTest2Activity extends AppCompatActivity {
    private static final int MY_PERMISSION_REQUEST_CODE = 10000;
    private AsyncBluetoothReader asyncBluetoothReader=null;

    private String btAddress="";
    private BluetoothAdapter mBluetoothAdapter = null;
    private String mConnectedBtName = null;
    private int worktype=0;

    private TextView mTitle;
    private ListView mConversationView;
    private ImageView fingerprintImage;
    private String mConnectedDeviceName = null;
    private ArrayAdapter<String> mConversationArrayAdapter;
    private EditText editText1;

    public byte mRefData[]=new byte[512];
    public int mRefSize=0;
    public byte mMatData[]=new byte[512];
    public int mMatSize=0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_device_test2);

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }

        if(Build.VERSION.SDK_INT < 23) {
            //initData();
        }else {
            checkPermission();
        }

        InitViews();

        SharedPreferences sp;
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        btAddress =sp.getString("device","");
        BluetoothReaderInit();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(mBluetoothAdapter!=null) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableIntent, BluetoothDeviceListActivity.REQUEST_ENABLE_BT);
            } else {
                //if(btAddress.length()>=12){
                //    LinkStart();
                //}
            }
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        asyncBluetoothReader.start();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        asyncBluetoothReader.stop();
    }

    private void InitViews(){
        mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        mConversationView = (ListView) findViewById(R.id.in);
        mConversationView.setAdapter(mConversationArrayAdapter);

        fingerprintImage=(ImageView)findViewById(R.id.imageView1);
        mTitle=(TextView)findViewById(R.id.textView1);
        editText1=(EditText)findViewById(R.id.editText1);
        editText1.setText("0000");


        LinearLayout linearLayoutPIN = (LinearLayout) findViewById(R.id.linearLayoutPIN);
        linearLayoutPIN.setVisibility(View.INVISIBLE);//View.GONE


        final Button btnEnrolNoImage = (Button) findViewById(R.id.btnEnrolNoImage);
        btnEnrolNoImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(asyncBluetoothReader.bluetoothReader.getState()== BluetoothReader.STATE_CONNECTED) {
                    AddStatusList(getString(R.string.txt_placefinger2));
                    asyncBluetoothReader.EnrolTempatelNoImage();
                }
            }
        });

        final Button btnCaptureNoImage = (Button) findViewById(R.id.btnCaptureNoImage);
        btnCaptureNoImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(asyncBluetoothReader.bluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    AddStatusList(getString(R.string.txt_placefinger1));
                    asyncBluetoothReader.CaptureTemplateNoImage();
                }
            }
        });

        final Button btnEnrolWithImage = (Button) findViewById(R.id.btnEnrolWithImage);
        btnEnrolWithImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(asyncBluetoothReader.bluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    worktype = 0;
                    AddStatusList(getString(R.string.txt_placefinger1));
                    asyncBluetoothReader.GetImageAndTemplate();
                }
            }
        });

        final Button btnCaptureWithImage = (Button) findViewById(R.id.btnCaptureWithImage);
        btnCaptureWithImage.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(asyncBluetoothReader.bluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    worktype = 1;
                    AddStatusList(getString(R.string.txt_placefinger1));
                    asyncBluetoothReader.GetImageAndTemplate();
                }
            }
        });

        final Button btnGetSN = (Button) findViewById(R.id.btnGetSN);
        btnGetSN.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(asyncBluetoothReader.bluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    asyncBluetoothReader.GetDeviceSN();
                }
            }
        });

        final Button btnGetBatVal = (Button) findViewById(R.id.btnGetBatVal);
        btnGetBatVal.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(asyncBluetoothReader.bluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    asyncBluetoothReader.GetDeviceBat();
                }
            }
        });

        final Button btnCloseDevice = (Button) findViewById(R.id.btnCloseDevice);
        btnCloseDevice.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(asyncBluetoothReader.bluetoothReader.getState()==BluetoothReader.STATE_CONNECTED) {
                    asyncBluetoothReader.ShutDownDevice();
                }
            }
        });
    }

    private  void ClearStatusList(){
        mConversationArrayAdapter.clear();
    }

    private void AddStatusList(String text) {
        mConversationArrayAdapter.add(text);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_test, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
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

    //Bluetooth Fingerprint Reader
    public void LinkBluetoothReader(){
        if(mBluetoothAdapter!=null) {
            Intent serverIntent = new Intent(BluetoothDeviceTest2Activity.this, BluetoothDeviceListActivity.class);
            startActivityForResult(serverIntent, BluetoothDeviceListActivity.REQUEST_CONNECT_DEVICE);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        switch (requestCode) {
            case BluetoothDeviceListActivity.REQUEST_CONNECT_DEVICE:
                if (resultCode == Activity.RESULT_OK) {

                    asyncBluetoothReader.stop();

                    String address = data.getExtras().getString(BluetoothDeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                    asyncBluetoothReader.connect(device);

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

                    if(btAddress.length()>=12){
                        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(btAddress);
                        asyncBluetoothReader.connect(device);
                    }
                }else{
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void BluetoothReaderInit(){
        asyncBluetoothReader = new AsyncBluetoothReader();

        asyncBluetoothReader.setOnBluetoothStateListener(new AsyncBluetoothReader.OnBluetoothStateListener() {
            @Override
            public void onBluetoothStateChange(int arg) {
                switch (arg) {
                    case BluetoothReader.STATE_CONNECTED:
                        mTitle.setText(R.string.title_connected_to);
                        mTitle.append(mConnectedBtName);
                        AddStatusList(R.string.title_connected_to+mConnectedBtName);
                        //mConversationArrayAdapter.clear();
                        break;
                    case BluetoothReader.STATE_CONNECTING:
                        mTitle.setText(R.string.title_connecting);
                        break;
                    case BluetoothReader.STATE_LISTEN:
                    case BluetoothReader.STATE_NONE:
                        mTitle.setText(R.string.title_not_connected);
                        break;
                }
            }

            @Override
            public void onBluetoothStateDevice(String devicename) {
                mConnectedBtName = devicename;
            }

            @Override
            public void onBluetoothStateLost(int arg) {
                switch(arg) {
                    case BluetoothReader.MSG_UNABLE:
                        Toast.makeText(getApplicationContext(), getString(R.string.title_unable_connected), Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothReader.MSG_LOST:
                        Toast.makeText(getApplicationContext(), getString(R.string.title_lost_connected), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });

        asyncBluetoothReader.setOnDeviceInfoListener(new AsyncBluetoothReader.OnDeviceInfoListener() {
            @Override
            public void onDeviceInfoDeviceType(String devicetype) {
                AddStatusList(getString(R.string.txt_devicetype)+devicetype);
            }

            @Override
            public void onDeviceInfoDeviceSN(String devicesn) {
                AddStatusList(getString(R.string.txt_devicesn)+devicesn);
            }

            @Override
            public void onDeviceInfoDeviceSensor(int sensor) {
                AddStatusList(getString(R.string.txt_devicesensor)+String.valueOf(sensor));
            }

            @Override
            public void onDeviceInfoDeviceVer(int ver) {
                AddStatusList(getString(R.string.txt_devicever)+String.valueOf(ver));
            }

            @Override
            public void onDeviceInfoDeviceBat(byte[] args, int size) {
                AddStatusList(getString(R.string.txt_batval)+Integer.toString(args[0]/10)+"."+Integer.toString(args[0]%10)+"V");
            }

            @Override
            public void onDeviceInfoDeviceShutdown(int arg) {
                if(arg==1)
                    AddStatusList(getString(R.string.txt_closeok));
                else
                    AddStatusList(getString(R.string.txt_closefail));
            }

            @Override
            public void onDeviceInfoDeviceError(int arg) {
                AddStatusList(getString(R.string.txt_getdevicefail));
            }
        });

        asyncBluetoothReader.setOnGetStdImageListener(new AsyncBluetoothReader.OnGetStdImageListener() {
            @Override
            public void onGetStdImageSuccess(byte[] data) {
                Bitmap image = BitmapFactory.decodeByteArray(data, 0,data.length);
                fingerprintImage.setImageBitmap(image);
                //SaveImage(image);
                AddStatusList(getString(R.string.txt_getimageok));
            }

            @Override
            public void onGetStdImageFail() {
                AddStatusList(getString(R.string.txt_getimagefail));
            }
        });

        asyncBluetoothReader.setOnGetResImageListener(new AsyncBluetoothReader.OnGetResImageListener() {
            @Override
            public void onGetResImageSuccess(byte[] data) {
                Bitmap image = BitmapFactory.decodeByteArray(data, 0,data.length);
                fingerprintImage.setImageBitmap(image);
                //SaveImage(image);
                AddStatusList(getString(R.string.txt_getimageok));
            }

            @Override
            public void onGetResImageFail() {
                AddStatusList(getString(R.string.txt_getimagefail));
            }
        });

        asyncBluetoothReader.setOnUpTemplateListener(new AsyncBluetoothReader.OnUpTemplateListener() {
            @Override
            public void onUpTemplateSuccess(byte[] model) {
                if(worktype==1){
                    System.arraycopy(model,0,mMatData,0,model.length);
                    mMatSize=model.length;
                    //AddStatusList("Len="+String.valueOf(mMatSize));
                    AddStatusList(getString(R.string.txt_captureok));
                    //AddStatusListHex(mMatData,mMatSize);

                    if(mRefSize>0) {
                        int score = asyncBluetoothReader.bluetoothReader.MatchTemplate(mRefData, mMatData);
                        AddStatusList(getString(R.string.txt_matchscore) + String.valueOf(score));
                    }

                }else{
                    System.arraycopy(model,0,mRefData,0,model.length);
                    mRefSize=model.length;
                    //AddStatusList("Len="+String.valueOf(mRefSize));
                    AddStatusList(getString(R.string.txt_enrolok));
                    //AddStatusListHex(mRefData,mRefSize);
                }
            }

            @Override
            public void onUpTemplateFail() {
                if(worktype==1){
                    AddStatusList(getString(R.string.txt_capturefail));
                }else{
                    AddStatusList(getString(R.string.txt_enrolfail));
                }
            }
        });

        asyncBluetoothReader.setOnEnrolTemplateListener(new AsyncBluetoothReader.OnEnrolTemplateListener() {
            @Override
            public void onEnrolTemplateSuccess(byte[] model) {
                System.arraycopy(model,0,mRefData,0,model.length);
                mRefSize=model.length;
                AddStatusList(getString(R.string.txt_enrolok));
            }

            @Override
            public void onEnrolTemplateFail() {
                AddStatusList(getString(R.string.txt_enrolfail));
            }
        });

        asyncBluetoothReader.setOnCaptureTemplateListener(new AsyncBluetoothReader.OnCaptureTemplateListener() {
            @Override
            public void onCaptureTemplateSuccess(byte[] model) {
                System.arraycopy(model,0,mMatData,0,model.length);
                mMatSize=model.length;
                AddStatusList(getString(R.string.txt_captureok));

                if(mRefSize>0) {
                    int score = asyncBluetoothReader.bluetoothReader.MatchTemplate(mRefData, mMatData);
                    AddStatusList(getString(R.string.txt_matchscore) + String.valueOf(score));
                }
            }

            @Override
            public void onCaptureTemplateFail() {
                AddStatusList(getString(R.string.txt_capturefail));
            }
        });
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
            //initData();
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
                //initData();
            } else {
                toast("Permission Fail");
            }
        }
    }
    public void toast(String content){
        Toast.makeText(getApplicationContext(),content,Toast.LENGTH_SHORT).show();
    }
}
