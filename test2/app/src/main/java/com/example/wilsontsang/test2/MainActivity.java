package com.example.wilsontsang.test2;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    
    private Set<BluetoothDevice> pairedDevices;
    boolean isItemSelected;

    //-------------- Views ----------------------------------


    TextView humid;





    //-------------- Views ----------------------------------
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); // "random" unique identifier

    String address;
    String name;
    int speed;

    private static final String TAG = "MainActivity";

    /**
     * Name of the connected device
     */
    private String mConnectedDeviceName = null;

    ArrayList<String> btStrings = new ArrayList<String>();
    /**
     * String buffer for outgoing messages
     */
    private StringBuffer mOutStringBuffer;

    /**
     * String buffer for ingoing messages
     */
    private StringBuffer mInStringBuffer;

    /**
     * Local Bluetooth adapter
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the chat services
     */
    private BluetoothService mBTService = null;

    private String lastMessage = "";
    private Boolean messageAck = true;
    private Boolean sentMessage = false;
    private int retryCounter = 0;

    private View manualViews;
    private View connectedViews;
    private View manualButtons;
    private Boolean isManualMode = false;

    private EditText et_setTemp;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        int speed = Integer.parseInt(getString(R.string.status_speed));

        //Check Bluetooth
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if(mBluetoothAdapter == null)
        {
            //Show a mensag. that thedevice has no bluetooth adapter
            Toast.makeText(getApplicationContext(), "Bluetooth Device Not Available", Toast.LENGTH_LONG).show();
            //finish apk
            //finish();
        }

    }


    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (mBTService == null) {
            setupApplication();
        }
        populateBtString(btStrings);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mBTService != null) {
            mBTService.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mBTService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mBTService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                mBTService.start();
            }
        }
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

    private void setupApplication(){
        Log.d(TAG, "setupApplication()");

        // Initialize the BluetoothService to perform bluetooth connections
        mBTService = new BluetoothService(getApplicationContext(), mBTHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
        mInStringBuffer = new StringBuffer("");

        manualButtons = (View)findViewById(R.id.group_manual);
        manualViews = (View)findViewById(R.id.view_speed);
        connectedViews = (View)findViewById(R.id.view_connected);
        et_setTemp = (EditText)findViewById(R.id.et_temp);

        et_setTemp.setOnEditorActionListener(new EditText.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    String btMsg = v.getText().toString();
                    int desiredTemp = Integer.parseInt(btMsg);

                    //validates temp
                    if(desiredTemp > 50){
                        desiredTemp = 50;
                    }
                    if(desiredTemp < 0){
                        desiredTemp = 0;
                    }

                    v.setText(Integer.toString(desiredTemp));

                    sendBTMessage(Integer.toString(desiredTemp));
                    return true;
                }
                return false;
            }
        });

    }

    // For the buttons
    @Override
    public void onClick(View v) {
        String btMsg;
        switch (v.getId()) {
            case R.id.btn_ConnectBlueTooth:
                //opens device selection dialogue
                showBTDialog();
                break;
            case R.id.tv_speed:
                // sets speed to 0
                btMsg = "off";
                sendBTMessage(btMsg);
                lastMessage = btMsg;
                speed = 0;
                btMsg = Integer.toString(speed);
                updateStatus(R.id.tv_speed,btMsg);
                sentMessage = true;
                break;

            case R.id.btn_inc:
                btMsg = v.getTag().toString();
                sendBTMessage(btMsg);
                lastMessage = btMsg;

                speed++;
                if (speed > 5){
                    speed = 5;
                    btMsg = "5";
                }
                else{
                    btMsg = Integer.toString(speed);
                }

                updateStatus(R.id.tv_speed,btMsg);
                sentMessage = true;

                break;

            case R.id.btn_dec:
                // sets speed to 0
                btMsg = v.getTag().toString();;
                sendBTMessage(btMsg);
                lastMessage = btMsg;

                speed--;
                if (speed < 1){
                    speed = 0;
                    btMsg = "0";
                }
                else{
                    btMsg = Integer.toString(speed);
                }

                updateStatus(R.id.tv_speed,btMsg);
                sentMessage = true;

                break;
            default:
                //sends message with their respective tag
                btMsg = v.getTag().toString();
                sendBTMessage(btMsg);
                lastMessage = btMsg;
                sentMessage = true;
                break;
        }
    }

    private void populateBtString(ArrayList<String> btArray){
        btArray.add(getString(R.string.bt_hot));
        btArray.add(getString(R.string.bt_cold));
        btArray.add(getString(R.string.bt_down));
        btArray.add(getString(R.string.bt_right));
        btArray.add(getString(R.string.bt_left));
        btArray.add(getString(R.string.bt_up));
        btArray.add(getString(R.string.bt_manual));
        btArray.add(getString(R.string.bt_auto));
        btArray.add(getString(R.string.bt_speed_down));
        btArray.add(getString(R.string.bt_speed_up));
        btArray.add(getString(R.string.bt_stop));
    }
    // selecting BT device dialog
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
        pairedDevices = mBluetoothAdapter.getBondedDevices();
        isItemSelected = false;
        // put it's one to the adapter
        for (BluetoothDevice device : pairedDevices)
            BTArrayAdapter.add(device.getName() + "\n" + device.getAddress());

        // Button OK
        popDialog.setPositiveButton("Pair",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        isItemSelected = true;
                        //make intent to service connect
                        Intent intent = new Intent();
                        intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
                        connectDevice(intent, true);
                        dialog.dismiss();
                    }

                });

        // Create popup and show
        popDialog.create();
        popDialog.show();

        devicelist.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if (!mBluetoothAdapter.isEnabled()) {
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

    /**
     * Updates the status of Bluetooth Connection.
     *
     * @param resId a string resource ID
     */
    private void setStatus(int resId) {
        // updates Bluetooth connection status
        TextView tvConnected = (TextView)findViewById(R.id.tv_bluetooth);
        tvConnected.setText(resId);

        //update status circle
        TextView statusCircle = (TextView) findViewById(R.id.statuscircle);
        if (resId == R.string.connected) {
            statusCircle.setBackgroundColor(Color.GREEN);
        }
        else{
            statusCircle.setBackgroundColor(Color.RED);
        }
    }


    /**
     * The Handler that gets information back from the BluetoothService
     */
    private final Handler mBTHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(R.string.connected);
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    break;
                case Constants.MESSAGE_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                    // Format of status: "#tempC+humidity+surface+speed+tracking+heat~"
                    String readMsg = new String(readBuf, 0,msg.arg1);

                    if (readMsg != null) {
                        mInStringBuffer.append(readMsg); //string buffer for inc status
                    }
                    int begOfLineInfex = mInStringBuffer.indexOf("#");
                    int endOfLineIndex = mInStringBuffer.indexOf("~");

                    int begOfLineIndexAck = mInStringBuffer.indexOf("<");
                    int endOfLineIndexAck = mInStringBuffer.indexOf(">");

                    //mInStringBuffer.setLength(0);
                    //mInStringBuffer.append("<bds>cvx  \n#26.00,22.00,29.68,5,0,0~ddd");


                    Pattern regexStatus = Pattern.compile("#(.*?)~");
                    Pattern regexAck = Pattern.compile("<(.*?)>");


                    Log.d(TAG,"Buffer: " + mInStringBuffer.toString());
                    Matcher matcherStatus = regexStatus.matcher(mInStringBuffer);
                    Matcher matcherAck = regexAck.matcher(mInStringBuffer);
                    //betweenString("<", ">", mInStringBuffer);
                    if (matcherStatus.find()) {
                        String dataInPrint = matcherStatus.group(1);    // extract string
                        String[] data = dataInPrint.split(",");

                        try{
                            speed = Integer.parseInt(data[3]);

                            // Temperature Text View
                            updateStatus(R.id.temp, String.format(getString(R.string.status_temperature), data[0]));
                            updateStatus(R.id.humid, String.format(getString(R.string.status_humidity), data[1] ));
                            updateStatus(R.id.surface, String.format(getString(R.string.status_surface) , data[2] ));
                            updateStatus(R.id.tv_speed, data[3]);


                            if(data[4].equals("1") && !isManualMode){
                                //manual mode
                                toggleManualViews();

                            }
                            else if(data[4].equals("0") &&  isManualMode){
                                //auto
                                toggleManualViews();

                            }
                            //updateStatus(R.id.temperature,getString(R.string.status_temperature, data[4]));
                            //manual mode 1
                            //cooling mode 0


                        }
                        catch (Exception e){
                            Log.d(TAG,"Parsing problem:\n" + mInStringBuffer.toString());
                        }
                        mInStringBuffer.setLength(0);

                    }
                    if (matcherAck.find()) {//acknowledge messages
                        String dataInPrint = matcherAck.group(1);    // extract string
                        Log.d(TAG,"Ack msg rec: "+dataInPrint);
                        if(btStrings.contains(dataInPrint)) {
                            lastMessage = "";
                            messageAck = true;
                            sentMessage = false;
                            retryCounter = 0;
                            Log.d(TAG, "Message Acknowledged: " + dataInPrint);
                        }
                        else{//message sent failure
                            //sendBTMessage(lastMessage);
                            messageAck = false;
                            retryCounter++;
                            Log.d(TAG, "Counter" + retryCounter);

                        }
                        if(retryCounter == 10){
                            sentMessage = false;
                            messageAck = false;
                            retryCounter = 0;
                            makeToast("Failed retry");
                        }

                        mInStringBuffer.setLength(0);
                    }
                    if (mInStringBuffer.length() > 50){//corrupt mesage
                        mInStringBuffer.setLength(0);
                        Log.d(TAG,"Corrupt Message");
                    }
               // mInStringBuffer.setLength(0);
//                    else if( endOfLineIndexAck > 0 && sentMessage == true){//the Acknowledge messages
//                        try{
//                            String ackString = cleanString(mInStringBuffer.toString());
//                            Log.d(TAG, mInStringBuffer.toString());
//                            if(btStrings.contains(ackString)){
//                                lastMessage = "";
//                                messageAck = true;
//                                sentMessage = false;
//                                retryCounter = 0;
//                                Log.d(TAG,"Message Acknowledged: "+mInStringBuffer.toString());
//                                mInStringBuffer.delete(0, mInStringBuffer.length());
//                            }
//                            else{//message sent failure
//                                sendBTMessage(lastMessage);
//                                messageAck = false;
//                                retryCounter++;
//                                Log.d(TAG, "Counter" + retryCounter);
//
//                            }
//                            if(retryCounter == 10){
//                                sentMessage = false;
//                                messageAck = false;
//                                retryCounter = 0;
//                                makeToast("Failed retry");
//                                mInStringBuffer.delete(0, mInStringBuffer.length());
//                            }
//                        }
//                        catch (Exception e){
//                            Log.d(TAG, "Parsing failure : "+mInStringBuffer.toString());
//                        }
//
//                    }

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                        Toast.makeText(getApplicationContext(), "Connected to "
                                + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAAGE_STATUS:
                    // updates
                case Constants.MESSAGE_TOAST:
                        Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    break;
            }
        }
    };

    private String betweenString(String beg, String end, String s){
        return s.substring(s.indexOf(beg) + 1, s.indexOf(end));
    }
    //hides or unhides manual views
    public void toggleView(View view){
        if(view.getVisibility()==View.INVISIBLE){
            view.setVisibility(View.VISIBLE);
        }
        else if(view.getVisibility()==View.VISIBLE){
            view.setVisibility(View.INVISIBLE);
        }
    }

    //toggles bluetooth connceted views
    private void toggleBluetoothViews(){

    }

    //toggle manual mode views
    private void toggleManualViews(){

    }

    private void updateManualMode(){

    }

    private void updateStatusStrings(String[] dataStrings){
        // Temperature Text View
        updateStatus(R.id.temp, String.format(getString(R.string.status_temperature), dataStrings[0]));
        updateStatus(R.id.humid, String.format(getString(R.string.status_humidity), dataStrings[1] ));
        updateStatus(R.id.surface, String.format(getString(R.string.status_surface) , dataStrings[2] ));
        updateStatus(R.id.tv_speed, dataStrings[3]);
    }

    private String cleanString(String str){
        str = str.replaceAll("(\\r|\\n)", "");
        return str;
    }

    private void updateStatus(int resID, String data){
        TextView tvStatus = (TextView)findViewById(resID);
        tvStatus.setText(data);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupApplication();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(getApplicationContext(), R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT).show();
                  finish();
                }
        }
    }

    /**
     * Establish connection with other device
     *
     * @param data   An {@link Intent} with {@link #EXTRA_DEVICE_ADDRESS} extra.
     * @param secure Socket Security type - Secure (true) , Insecure (false)
     */
    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras()
                .getString(EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mBTService.connect(device, secure);
    }

    private void sendBTMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mBTService.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(getApplicationContext(), R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothService to write
            byte[] send = message.getBytes();
            mBTService.write(send);
            makeToast(message);

            // Reset out string buffer to zero and clear the edit text field
            mOutStringBuffer.setLength(0);
        }
    }


}

