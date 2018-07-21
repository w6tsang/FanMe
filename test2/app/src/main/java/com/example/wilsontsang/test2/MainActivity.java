package com.example.wilsontsang.test2;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private BluetoothAdapter myBluetooth = null;
    private Set<BluetoothDevice> pairedDevices;
    boolean isItemSelected;
    Button btnPaired;
    Button btnLeft;
    Button btnRight;
    Button btnUp;
    Button btnDown;

    Button btnManual;
    Button btnAuto;

    TextView humid;
    TextView lblManual;
    ListView devicelist;
    SeekBar speedControl;
    TextView status;
    private BluetoothSocket mBTSocket = null; // bi-directional client-to-client data path
    private Handler mHandler;
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    String address;
    String btFanName = "Comp";
    String name;
    private ConnectedThread mConnectedThread;
    Boolean isConnected=false;
    Boolean isMsgAck=false;

    // #defines for identifying shared types between calling functions
    private final static int REQUEST_ENABLE_BT = 1; // used to identify adding bluetooth names
    private final static int MESSAGE_READ = 2; // used in bluetooth handler to identify message update
    private final static int CONNECTING_STATUS = 3; // used in bluetooth handler to identify message status

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnPaired = (Button)findViewById(R.id.Btn_ConnectBlueTooth);
         humid = (TextView)findViewById(R.id.humid);
         btnLeft = (Button)findViewById(R.id.btn_left);
         btnRight = (Button)findViewById(R.id.btn_right);

        btnDown = (Button)findViewById(R.id.btn_down);
        status = (TextView)findViewById(R.id.connected);
        lblManual = (TextView)findViewById(R.id.txt_manual);
        btnUp = (Button)findViewById(R.id.btn_up);
        speedControl = (SeekBar)findViewById(R.id.sb_speedControl);

        btnAuto = (RadioButton)findViewById(R.id.btn_Auto);
        btnManual = (RadioButton)findViewById(R.id.btn_Manual);

        //Check Bluetooth
        myBluetooth = BluetoothAdapter.getDefaultAdapter();
        if(myBluetooth == null)
        {
            //Show a mensag. that thedevice has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            //finish apk
            finish();
        }
        else
        {
            if (myBluetooth.isEnabled())
            {

                status.setText("BOOM IM HERE");
            }
            else
            {
                //Ask to the user turn the bluetooth on
                Intent turnBTon = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(turnBTon,1);
            }
        }
        mHandler = new Handler(){
            public void handleMessage(android.os.Message msg){
                if(msg.what == MESSAGE_READ){//reciver
                    String readMessage = null;
                    try {
                        readMessage = new String((byte[]) msg.obj, "UTF-8");

                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    lblManual.setText(readMessage);
                }

                if(msg.what == CONNECTING_STATUS){//status
                    if(msg.arg1 == 1)
                        status.setText("Connected to Device: " + (String)(msg.obj));
                    else
                        status.setText("Connection Failed");
                }
            }
        };

        btnManual.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isConnected){
                    mConnectedThread.write("manual");
                    Toast.makeText(getApplicationContext(), "Sent Manual", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(isConnected){
                    mConnectedThread.write("auto");
                    Toast.makeText(getApplicationContext(), "Sent Auto", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //buttons
        btnLeft.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(isConnected){
                    mConnectedThread.write("left");
                    Toast.makeText(getApplicationContext(), "Sent LEFT", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnRight.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(isConnected){
                    mConnectedThread.write("right");
                    Toast.makeText(getApplicationContext(), "Sent RIGHT", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnUp.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(isConnected){
                    mConnectedThread.write("up");
                    Toast.makeText(getApplicationContext(), "Sent UP", Toast.LENGTH_SHORT).show();
                }
            }
        });

        btnDown.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(isConnected){
                    mConnectedThread.write("down");
                    Toast.makeText(getApplicationContext(), "Sent Down", Toast.LENGTH_SHORT).show();
                }
            }
        });


        //bluetooth button
        btnPaired.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                Communications btComms = new BluetoothComms();
               // pairedDevicesList();

                isItemSelected = false;
                showBTDialog();
              // Spawn a new thread to avoid blocking the GUI one

            }

        });

        //buttons
        btnLeft.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(isConnected){
                    mConnectedThread.write("left");
                    Toast.makeText(getApplicationContext(), "Sent LEFT", Toast.LENGTH_SHORT).show();
                }
            }
        });

        //buttons
        btnLeft.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View view) {
                if(isConnected){
                    mConnectedThread.write("left");
                    Toast.makeText(getApplicationContext(), "Sent LEFT", Toast.LENGTH_SHORT).show();
                }
            }
        });


        speedControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                if(isConnected){
                    switch (i){
                        case 0:
                            mConnectedThread.write("off");
                            break;
                        case 1:
                            mConnectedThread.write("slow");
                            break;
                        case 2:
                            mConnectedThread.write("medium");
                            break;
                        case 3:
                            mConnectedThread.write("fast");
                            break;
                        default:
                            return;
                    }
                    Toast.makeText(getApplicationContext(), "Sent Speed" + i, Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void pairedDevicesList()
    {
        pairedDevices = myBluetooth.getBondedDevices();
        ArrayList list = new ArrayList();

        if (pairedDevices.size()>0)
        {
            TextView humid = (TextView)findViewById(R.id.humid);
            String bluetooth = "";
                // put it's one to the adapter
            int counter = 0;
                for (BluetoothDevice device : pairedDevices){
                    bluetooth += device.getName() + " " + device.getAddress() + "\n";
                    if(counter == 1 ) {
                        address = device.getAddress();
                        name = device.getName();
                    }
                    counter++;
                }
                bluetooth += address +"\n";
                humid.setText(bluetooth);
                Toast.makeText(getApplicationContext(), "Show Paired Devices", Toast.LENGTH_SHORT).show();

        }
        else
        {
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

       // final ArrayAdapter adapter = new ArrayAdapter(this,android.R.layout.simple_list_item_1, list);
        //devicelist.setAdapter(adapter);

    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
        //creates secure outgoing connection with BT device using UUID
    }


    //Bluetooth Connection
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
                mmSocket = socket;
                InputStream tmpIn = null;
                OutputStream tmpOut = null;

                // Get the input and output streams, using temp objects because
                // member streams are final
                try {
                    tmpIn = socket.getInputStream();
                    tmpOut = socket.getOutputStream();
                } catch (IOException e) { }

                mmInStream = tmpIn;
                mmOutStream = tmpOut;
            isConnected = true;
            }

            public void run() {
                byte[] buffer = new byte[1024];  // buffer store for the stream
                int bytes; // bytes returned from read()
                // Keep listening to the InputStream until an exception occurs
                while (true) {
                    try {
                        // Read from the InputStream
                        bytes = mmInStream.available();
                        if(bytes != 0) {
                            SystemClock.sleep(100); //pause and wait for rest of data. Adjust this depending on your sending speed.
                            bytes = mmInStream.available(); // how many bytes are ready to be read?
                            bytes = mmInStream.read(buffer, 0, bytes); // record how many bytes we actually read

                            String msg = buffer.toString();


                            mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                    .sendToTarget(); // Send the obtained bytes to the UI activity
                            mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer)
                                    .sendToTarget(); // Send the obtained bytes to the UI activity
                        }
                    } catch (IOException e) {
                        e.printStackTrace();

                        break;
                    }
                }
            }

            /* Call this from the main activity to send data to the remote device */
            public void write(String input) {
                byte[] bytes = input.getBytes();           //converts entered String into bytes
                try {
                    mmOutStream.write(bytes);
                } catch (IOException e) {

                }
            }

        /* Call this from the main activity to send data to the remote device */
        public void writeACK(String input) {
            while(true) {
                byte[] bytes = input.getBytes();           //converts entered String into bytes
                try {
                    mmOutStream.write(bytes);
                } catch (IOException e) {

                }
            }
        }

            /* Call this from the main activity to shutdown the connection */
            public void cancel() {
                try {
                    mmSocket.close();
                } catch (IOException e) { }
            }
    }

    public void showBTDialog() {

        final AlertDialog.Builder popDialog = new AlertDialog.Builder(this);
        final LayoutInflater inflater = (LayoutInflater) this.getSystemService(LAYOUT_INFLATER_SERVICE);
        final View ViewLayout = inflater.inflate(R.layout.bt_list, (ViewGroup) findViewById(R.id.bt_list));

        popDialog.setTitle("Paired Bluetooth Devices");
        popDialog.setView(ViewLayout);

        // create the arrayAdapter that contains the BTDevices, and set it to a ListView
        ListView devicelist = (ListView) ViewLayout.findViewById(R.id.BTList);
        ArrayAdapter BTArrayAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
        devicelist.setAdapter(BTArrayAdapter);

        // get paired devices
        pairedDevices = myBluetooth.getBondedDevices();

        // put it's one to the adapter
        for (BluetoothDevice device : pairedDevices)
            BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

        // Button OK
        popDialog.setPositiveButton("Pair",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        isItemSelected = true;
                        connectBT();

                        dialog.dismiss();
                    }

                });

        // Create popup and show
        popDialog.create();
        popDialog.show();

        devicelist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!myBluetooth.isEnabled()) {
                    makeToast("Bluetooth not On");
                    return;
                }

                // Get the device MAC address, which is the last 17 chars in the View
                String info = ((TextView) view).getText().toString();
                address = info.substring(info.length() - 17);
                name = info.substring(0, info.length() - 17);
            }

        });
    }
    public void makeToast(String msg){
        Toast.makeText(getBaseContext(), msg, Toast.LENGTH_SHORT).show();
    }

    public void connectBT(){

        if(isConnected){
            mConnectedThread.cancel();
        }

        BluetoothDevice device = myBluetooth.getRemoteDevice(address);
        boolean fail = false;

        try {
            mBTSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            fail = true;
            makeToast("Socket creation failed");
        }
        // Establish the Bluetooth socket connection.
        try {
            mBTSocket.connect();
        } catch (IOException e) {
            try {
                fail = true;
                mBTSocket.close();

                status.setText("Failed");
            } catch (IOException e2) {
                //insert code to deal with this

            }
        }
        if(fail == false) {
            mConnectedThread = new ConnectedThread(mBTSocket);
            mConnectedThread.start();
            mConnectedThread.write("I AM THE COOLEST");
            mHandler.obtainMessage(CONNECTING_STATUS, 1, -1, name)
                    .sendToTarget();
        }
    }

}

