package io.xrworkout.htctrackerdongle;

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;
import java.util.Formatter;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.util.Log;

import com.illposed.osc.OSCMessage;
import com.illposed.osc.transport.OSCPortOut;
import com.illposed.osc.transport.OSCPortOutBuilder;

/**
 * This class is used for talking to hid of the dongle, connecting, disconnencting and enumerating the devices.
 * @author gai
 */
public class HidBridge {
    private Context _context;
    private int _productId;
    private int _vendorId;

    float yOffset = 0;

    public ViveTracker[] trackers = new ViveTracker[5];
    public String calib_1;
    public String calib_2;

    public boolean autoMapReset = false;

    public boolean shouldLog = false;

    public boolean invertX = false;
    public boolean invertY = false;
    public boolean invertZ = false;

    public boolean flipXRot = false;

    public boolean flipYRot = false;

    public boolean flipZRot = false;




    // Can be used for debugging.
    @SuppressWarnings("unused")
    //private HidBridgeLogSupporter _logSupporter = new HidBridgeLogSupporter();
    private static final String ACTION_USB_PERMISSION =
            "com.example.company.app.testhid.USB_PERMISSION";

    // Locker object that is responsible for locking read/write thread.
    private Object _locker = new Object();
    private Thread _readingThread = null;
    private Thread _MainLoop = null;
    private Thread _PoseSender = null;
    private String _deviceName;

    private UsbManager _usbManager;
    private UsbDevice _usbDevice;

    UsbInterface readIntf;
    UsbEndpoint readEp;
    UsbDeviceConnection readConnection;

    // The queue that contains the read data.
    private Queue<byte[]> _receivedQueue;

    /**
     * Creates a hid bridge to the dongle. Should be created once.
     * @param context is the UI context of Android.
     * @param productId of the device.
     * @param vendorId of the device.
     */
    public HidBridge(Context context, int productId, int vendorId) {
        _context = context;
        _productId = productId;
        _vendorId = vendorId;
        _receivedQueue = new LinkedList<byte[]>();

        for ( int i = 0; i < trackers.length; i++ ) {
            trackers[i] = new ViveTracker();
        }
        Reset();

    }

    public void SetFloorOffset ( float offset ) {
        yOffset = offset;
    }

    /**
     * Searches for the device and opens it if successful
     * @return true, if connection was successful
     */
    public boolean OpenDevice() {
        _usbManager = (UsbManager) _context.getSystemService(Context.USB_SERVICE);

        HashMap<String, UsbDevice> deviceList = _usbManager.getDeviceList();

        Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();
        _usbDevice = null;

        // Iterate all the available devices and find ours.
        while(deviceIterator.hasNext()){
            UsbDevice device = deviceIterator.next();
            if (device.getProductId() == _productId && device.getVendorId() == _vendorId) {
                _usbDevice = device;
                _deviceName = _usbDevice.getDeviceName();
            }
        }

        if (_usbDevice == null) {
            Log("Cannot find the device. Did you forgot to plug it?");
            Log(String.format("\t I search for VendorId: %s and ProductId: %s", _vendorId, _productId));
            return false;
        }

        // Create and intent and request a permission.
        PendingIntent mPermissionIntent = PendingIntent.getBroadcast(_context, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE);
        IntentFilter filter = new IntentFilter(ACTION_USB_PERMISSION);
        _context.registerReceiver(mUsbReceiver, filter);

        _usbManager.requestPermission(_usbDevice, mPermissionIntent);

        Log("Found the device");
        return true;
    }

    /**
     * Closes the reading thread of the device.
     */
    public void CloseTheDevice() {
        StopReadingThread();
    }

    /**
     * Starts the thread that continuously reads the data from the device.
     * Should be called in order to be able to talk with the device.
     */
    public void StartReadingThread() {

        if ( readConnection == null ) {
            readIntf = _usbDevice.getInterface(0);
            readEp = readIntf.getEndpoint(0);
            readConnection = _usbManager.openDevice(_usbDevice);
        }

        // Lock the usb interface.
        readConnection.claimInterface(readIntf, true);

        if (_readingThread == null) {
            _readingThread = new Thread(readerReceiver);
            _readingThread.start();

            _MainLoop = new Thread( MainLoop );
            _MainLoop.start();

        } else {
            Log("Reading thread already started");
        }

    }

    /**
     * Stops the thread that continuously reads the data from the device.
     * If it is stopped - talking to the device would be impossible.
     */
    @SuppressWarnings("deprecation")
    public void StopReadingThread() {
        if (_readingThread != null) {
            // Just kill the thread. It is better to do that fast if we need that asap.
            _readingThread.stop();
            _readingThread = null;

            _MainLoop.stop();
            _MainLoop = null;
        } else {
            Log("No reading thread to stop");
        }
        if ( readConnection != null ) {
            readConnection.releaseInterface(readIntf);
            readConnection.close();
        }

    }

    /**
     * Write data to the usb hid. Data is written as-is, so calling method is responsible for adding header data.
     * @param bytes is the data to be written.
     * @return true if succeed.
     */
    public boolean WriteData(byte[] bytes , boolean set_report) {
        try
        {
            // Lock that is common for read/write methods.
            //synchronized (_locker) {
            //UsbInterface writeIntf = _usbDevice.getInterface(0);
            //UsbEndpoint writeEp = writeIntf.getEndpoint(0);
            //UsbDeviceConnection writeConnection = _usbManager.openDevice(_usbDevice);


            // Write the data as a bulk transfer with defined data length.
            int r = -1;
            if ( set_report ) {
                r = readConnection.controlTransfer(0x21, 0x09,0x0300, 0x0, bytes, bytes.length, 0);
            } else {
                r = readConnection.controlTransfer(0xa1, 0x01, 0x0300, 0x0, bytes, bytes.length, 0);
            }
            //int r = writeConnection.bulkTransfer(writeEp, bytes, bytes.length, 0);
            if (r != -1) {
                //Log(String.format("Written %s bytes to the dongle. Data written: %s", r, composeString(bytes)));
            } else {
                Log("Error happened while writing data. No ACK");
            }

            // Release the usb interface.
            //writeConnection.releaseInterface(writeIntf);
            //writeConnection.close();
            //}

        } catch(NullPointerException e)
        {
            Log("Error happend while writing. Could not connect to the device or interface is busy?");
            Log.e("HidBridge", Log.getStackTraceString(e));
            return false;
        }
        return true;
    }

    /**
     * @return true if there are any data in the queue to be read.
     */
    public boolean IsThereAnyReceivedData() {
        //synchronized(_locker) {
        return !_receivedQueue.isEmpty();
        //}
    }

    /**
     * Queue the data from the read queue.
     * @return queued data.
     */
    public byte[] GetReceivedDataFromQueue() {
        //synchronized(_locker) {
        return _receivedQueue.poll();
        //}
    }

    // The thread that continuously receives data from the dongle and put it to the queue.
    private Runnable readerReceiver = new Runnable() {
        public void run() {
            if (_usbDevice == null) {
                Log("No device to read from");
                return;
            }

            //UsbEndpoint readEp;
            //UsbDeviceConnection readConnection = null;
            //UsbInterface readIntf = null;
            boolean readerStartedMsgWasShown = false;
            byte[] trancatedBytes = new byte[0];

            int loopCounter = 0;

            // We will continuously ask for the data from the device and store it in the queue.
            while (true) {
                // Lock that is common for read/write methods.
                boolean dataReceived = false;

                synchronized (_locker) {
                    try
                    {
                        if (_usbDevice == null) {
                            OpenDevice();
                            Log("No device. Recheking in 10 sec...");

                            Sleep(10000);
                            continue;
                        }

                        //readIntf = _usbDevice.getInterface(0);
                        //readEp = readIntf.getEndpoint(0);
                        /*if (!_usbManager.getDeviceList().containsKey(_deviceName)) {
                            Log("Failed to connect to the device. Retrying to acquire it.");
                            OpenDevice();
                            if (!_usbManager.getDeviceList().containsKey(_deviceName)) {
                                Log("No device. Recheking in 10 sec...");

                                Sleep(10000);
                                continue;
                            }
                        }*/

                        try
                        {
                            //if ( readConnection == null ) continue;
                            //readConnection = _usbManager.openDevice(_usbDevice);

                            if (readConnection == null) {
                                Log("Cannot start reader because the user didn't gave me permissions or the device is not present. Retrying in 2 sec...");
                                Sleep(2000);
                                continue;
                            }

                            // Claim and lock the interface in the android system.
                            //readConnection.claimInterface(readIntf, true);
                        }
                        catch (SecurityException e) {
                            Log("Cannot start reader because the user didn't gave me permissions. Retrying in 2 sec...");

                            Sleep(2000);
                            continue;
                        }

                        // Show the reader started message once.
                        if (!readerStartedMsgWasShown) {
                            Log("!!! Reader was started !!!");
                            readerStartedMsgWasShown = true;
                        }

                        // Read the data as a bulk transfer with the size = MaxPacketSize
                        int packetSize = readEp.getMaxPacketSize();
                        byte[] bytes = new byte[packetSize];
                        for ( int i = 0; i < bytes.length; i++) bytes[i]=0;
                        int r = readConnection.bulkTransfer(readEp, bytes, packetSize, 50);
                        if (r >= 0) {
                            dataReceived = true;


                            if ( bytes[0] == HorusdDongleCmds.DRESP_TRACKER_INCOMING && r > 10 && bytes[9] == 1 && bytes[10] == 16 ) {
                                //Make a shortcut for the pose messages to prevent them from piling up and blocking the rest
                                byte[] device_addr = sub_array(bytes,3,6);
                                int idx = MacToIdx( device_addr );
                                if ( idx >= 0 && idx < trackers.length ) {
                                    for ( int i = 0; i < r; i++ ) trackers[idx].raw_pose_message[i] = bytes[i];
                                    trackers[idx].raw_pose_message_len = r;
                                    trackers[idx].current_pose_message_idx = trackers[idx].last_pose_message_idx + 1;
                                    }
                            } else {

                                trancatedBytes = new byte[r]; // Truncate bytes in the honor of r

                                int i = 0;
                                for (byte b : bytes) {
                                    trancatedBytes[i] = b;
                                    i++;
                                }

                                _receivedQueue.add(trancatedBytes); // Store received data
                                //Log(String.format("Message received of lengths %s and content: %s", r, composeString(bytes)));
                            }
                        }

                        loopCounter += 1;
                        //if ( loopCounter % 3 != 0 ) continue;

                        boolean packetReceived = false;
                        if ( IsThereAnyReceivedData() ) {
                            byte[] data = GetReceivedDataFromQueue();
                            packetReceived = true;
                            if (data.length > 0) ProcessIncomingMessage(data);
                        }


                        // Release the interface lock.
                        //readConnection.releaseInterface(readIntf);
                        //readConnection.close();
                    }

                    catch (NullPointerException e) {
                        Log("Error happened while reading. No device or the connection is busy");
                        Log.e("HidBridge", Log.getStackTraceString(e));
                    }
                    catch (ThreadDeath e) {
                        //if (readConnection != null) {
                        //     readConnection.releaseInterface(readIntf);
                        //    readConnection.close();
                        //}

                        throw e;
                    }
                }


                // Sleep for 10 ms to pause, so other thread can write data or anything.
                // As both read and write data methods lock each other - they cannot be run in parallel.
                // Looks like Android is not so smart in planning the threads, so we need to give it a small time
                // to switch the thread context.
                //if ( !dataReceived ) {
                    Sleep(0);
                //}
            }
        }
    };


    private String poseAddress = "localhost";
    private DatagramSocket poseSocket;
    private InetAddress poseTarget;
    private int posePort = 11011;
    private boolean udpDisabled = false;
    private boolean useOSC = false;


    public void SetPoseTargetAddress( String address, int port ) {
        udpDisabled = true;
        poseAddress = address;
        posePort = port;

        if ( poseSocket != null ) {
            poseSocket.close();
            poseSocket = null;
        }

        if ( _PoseSender == null ) {
            _PoseSender = new Thread ( PoseSender );
            _PoseSender.start();
        }

        udpDisabled = false;
    }

    public void EnableOSC() {
        if ( poseTarget == null ) {
            useOSC = true;
        } else  {
            Log("Error: OSC must be enabled before initialization");
        }
    }

    private OSCPortOut oscPort = null;


    private void SendOSCPoses() {
        if (oscPort == null) {
            try {
                poseTarget = InetAddress.getByName(poseAddress);
            } catch (Exception e) {
                Log(e.getMessage());
                poseTarget = null;
            }
            if (poseTarget != null) {
                try {
                    oscPort = new OSCPortOut( poseTarget, posePort );
                } catch (Exception e ) {
                    Log(e.getMessage());
                    oscPort = null;
                }
            }
        } else {
            ArrayList<Object> posList = new ArrayList<Object>(3);
            posList.add((float)0.0);posList.add((float)0.0);posList.add((float)0.0);
            ArrayList<Object> rotList = new ArrayList<Object>(3);
            rotList.add((float)0.0);rotList.add((float)0.0);rotList.add((float)0.0);

            for (int i = 0; i < trackers.length; i++) {
                if ( trackers[i].IsFullyTracking() )
                {
                    Vector3 pos = trackers[i].position;
                    Vector3 rot = trackers[i].rotation.toEuler();

                    posList.set(0, (float) ( invertX ? -pos.getX() : pos.getX() ) );
                    posList.set(1, (float) ( invertY ? -pos.getY() : pos.getY() ) - yOffset);
                    posList.set(2, (float) ( invertZ ? -pos.getZ() : pos.getZ() ) );

                    OSCMessage message = new OSCMessage(String.format("/tracking/trackers/%d/position", i + 1), posList);

                    try {
                        oscPort.send(message);
                    } catch (Exception e ) {
                        Log(e.getMessage());
                    }


                    float x = (float) ( flipXRot ? -rot.getX() : rot.getX() );
                    float y = (float) ( flipYRot ? -rot.getY() : rot.getY() );
                    float z = (float) ( flipZRot ? -rot.getZ() : rot.getZ() );

                    if ( x < 0 ) x+=2*Math.PI;
                    if ( y < 0 ) y+=2*Math.PI;
                    if ( z < 0 ) z+= 2*Math.PI;
                    if ( x > 2*Math.PI ) x -= 2*Math.PI;
                    if ( y > 2*Math.PI ) y -= 2*Math.PI;
                    if ( z > 2*Math.PI ) z -= 2*Math.PI;

                    float xRot = (float) ( x * 180/Math.PI  );
                    float yRot = (float) ( y * 180/Math.PI  );
                    float zRot = (float) ( z * 180/Math.PI  );

                    rotList.set(0, (float) xRot);
                    rotList.set(1, (float) yRot);
                    rotList.set(2, (float) zRot);

                    message = new OSCMessage(String.format("/tracking/trackers/%d/rotation", i + 1), rotList);

                    try {
                        oscPort.send(message);
                    } catch (Exception e ) {
                        Log(e.getMessage());
                    }
                }
            }

        }
    }

    private void SendUdpPoses( ) {

        if ( udpDisabled ) return;

        if ( poseSocket == null ) {

            try {
                poseSocket = new DatagramSocket();
            } catch ( Exception e ) {
                Log(e.getMessage());
                udpDisabled = true;
            }
            try {
                poseTarget = InetAddress.getByName(poseAddress);
            } catch ( Exception e ) {
                Log(e.getMessage());
                poseTarget = null;
            }
        }

        if ( poseSocket == null || poseTarget == null ) return;
        if ( poseSocket.isClosed() ) return;

        boolean anyTrackerActive = false;

        for ( int i = 0; i < trackers.length; i++ ) {
            if ( trackers[i].IsActive() ) {
                anyTrackerActive = true;
                break;
            }
        }

        if ( !anyTrackerActive ) return;

        ByteBuffer data = ByteBuffer.allocate( 4*8* trackers.length );

        for ( int i = 0; i < trackers.length; i++ ) {
            data.putFloat( (float) trackers[i].tracking_state );
            Vector3 pos = trackers[i].position;
            Quaternion rot = trackers[i].rotation;

            data.putFloat( (float)pos.getX() );
            data.putFloat( (float)pos.getY() - yOffset);
            data.putFloat( (float)pos.getZ() );

            data.putFloat( (float)rot.getX() );
            data.putFloat( (float)rot.getY() );
            data.putFloat( (float)rot.getZ() );
            data.putFloat( (float)rot.getW() );

        }

        byte[] msg = data.array();
        DatagramPacket packet = new DatagramPacket( msg, msg.length, poseTarget, posePort );

        try {
            poseSocket.send(packet);
        } catch ( IOException e ) {
            Log(e.getMessage());
            // Yeah, well, nothing to do if the network doesn't work ¯\_(ツ)_/¯
        }

    }


    public long poseUpdateIntervalMS = 30;
    public long lastPoseUpdate = 0;

    private Runnable MainLoop = new Runnable() {
        @Override
        public void run() {

            try {
                while (true) {

                    for ( int i = 0; i < trackers.length; i++ ) {
                        if ( trackers[i].current_pose_message_idx > trackers[i].last_pose_message_idx ) {
                            byte[] rawMessage = sub_array( trackers[i].raw_pose_message, 0, trackers[i].raw_pose_message_len );
                            ParseTrackerIncoming( rawMessage );
                        }
                    }

                    if ( reset_map_stage == 1 && reset_map_started_ts + 2000 < System.currentTimeMillis() ) {
                        Log ( "Finish map reset ");
                        reset_map_stage = 2;

                        for ( int i = 0; i < trackers.length; i++ ) {
                            ResetMapFullFinish( MacToIdx( trackers[i].device_address ) );
                        }
                        reset_map_stage = 0;
                    }


                    Sleep((int)poseUpdateIntervalMS);
                }
            } catch ( ThreadDeath e ) {
                if ( poseSocket != null ) {
                    poseSocket.close();
                }
            }
        }
    };

    private Runnable PoseSender = new Runnable() {

        @Override
        public void run() {
            try {
                while (true) {
                    if (lastPoseUpdate + poseUpdateIntervalMS < System.currentTimeMillis()) {
                        if ( useOSC ) SendOSCPoses();
                        else SendUdpPoses();
                        lastPoseUpdate = System.currentTimeMillis();
                    }
                    Sleep( (int)poseUpdateIntervalMS);
                }
            } catch (ThreadDeath e) {
                if (poseSocket != null) {
                    poseSocket.close();
                }
            }
        }
    };




    private void Sleep(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private final BroadcastReceiver mUsbReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbDevice device = (UsbDevice)intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if(device != null){

                            //call method to set up device communication
                        }
                    }
                    else {
                        Log.d("TAG", "permission denied for the device " + device);
                    }
                }
            }
        }
    };




    public int MacToIdx( byte[] device_addr ) {
        int retVal = -1;

        for ( int i = 0; i < trackers.length; i++ ) {
            if ( trackers[i] != null && trackers[i].IsDevice( device_addr )) {
                retVal = i;
                break;
            }
        }
        //Log (" Find Mac Idx " + composeString( device_addr ) + " id " + retVal );

        return retVal;
    }

    public int AddMac( byte[] device_addr ) {
        int retVal = -1;

        Log (" Add new device address " + composeString( device_addr ) );

        for ( int i = 0; i < trackers.length; i++)
        {
            if ( trackers[i].device_address == null ) {
                trackers[i].device_address = new byte[device_addr.length];
                for ( int j = 0; j < device_addr.length; j++ ) trackers[i].device_address[j] = device_addr[j];
                //Log ("Found new device slot: " + i);
                //trackers[i].device_address = device_addr;
                return i;
            }

        }
        return retVal;
    }




    int[] get_pcbid = new int[]{0xf0,0x03,0x06,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00};

    public String GetPCBID() {
        byte[] response = SendCommand( HorusdDongleCmds.DCMD_GET_CR_ID, new int[]{HorusdDongleCmds.CR_ID_PCBID} );
        String retVal = new String(response, StandardCharsets.UTF_8);
        return retVal;
    }

    public String GetSKUID (){
        byte[] response = SendCommand( HorusdDongleCmds.DCMD_GET_CR_ID, new int[]{HorusdDongleCmds.CR_ID_SKUID} );
        String retVal = new String(response, StandardCharsets.UTF_8);
        return retVal;
    }

    public String GetSN () {
        byte[] response = SendCommand( HorusdDongleCmds.DCMD_GET_CR_ID, new int[]{HorusdDongleCmds.CR_ID_SN} );
        String retVal = new String(response, StandardCharsets.UTF_8);
        return retVal;
    }

    public void SetPowerPCVR( int mode ) {
        Log("Setting Tracking mode " +( mode&0xff));
        SendCommand(HorusdHIDCmds.PACKET_SET_POWER_PCVR, new int[] {mode & 0xFF});

    }

    public void SetPairing() {
        //bEnabled = 1
        //print(self.send_cmd(DCMD_REQUEST_RF_CHANGE_BEHAVIOR, struct.pack("<BBBBBBB", RF_BEHAVIOR_PAIR_DEVICE, bEnabled, 1, 1, 1, 0, 0))) # PairDevice
    }


    public void GetStatus( )
    {


    }



    public byte[] ParseHIDResponse( byte[] data ) {
        //err_ret, cmd_id, data_len, unk2 = struct.unpack("<BBBH", data[:5])
        ByteArrayInputStream s = new ByteArrayInputStream( data );
        BinaryIn input = new BinaryIn( s );

        int cmd_id = input.readByte();
        int dataLen = input.readByte();
        int unknown = input.readShort();

        int responseLen = Math.max(0,dataLen-4);

        byte[] response = new byte[responseLen];
        for ( int i = 0; i < response.length; i++ ) {
            response[i] = input.readByte();
        }

        //Log(String.format("Command Id %d, Data length %d", cmd_id, dataLen));

        return response;
    }


    public byte[] SendCommand( int cmd_id, int[] data) {
        ByteArrayOutputStream s = new ByteArrayOutputStream();

        BinaryOut output = new BinaryOut(s);

        output.writeByte( cmd_id );
        output.writeByte(data.length + 2);

        int written = 2;

        for ( int i = 0; i < data.length ; i++ ) {
            written++;
            output.writeByte( data[i] );
        }

        for ( int i = 0; i < 0x40-written; i++ ) {
            output.writeByte(0);
        }

        output.close();

        byte[] tmp = s.toByteArray();
        //Log("Buffer to write is " + tmp.length);

        byte[] empty = new byte[63];
        for ( int j = 0; j < empty.length; j++ ) empty[j] = 0;

        synchronized (_locker) {

            WriteData(tmp, true);


            for (int i = 0; i < 10; i++) {
                WriteData(empty, false);
                if (empty[0] == (byte)cmd_id) break;
            }

        }
        if (empty[0] != (byte)cmd_id) Log("Failed reading report");

        return ParseHIDResponse(empty);

    }


    /**
     * Logs the message from HidBridge.
     * @param message to log.
     */
    private void Log(String message) {
        if ( shouldLog )  Log.d("HidBridge", message);
        //LogHandler logHandler = LogHandler.getInstance();
        //logHandler.WriteMessage("HidBridge: " + message, LogHandler.GetNormalColor());
    }

    /**
     * Composes a string from byte array.
     */
    private String composeString(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b: bytes) {

            builder.append( Integer.toHexString( ((int)b) & 0xff  ));
            builder.append(" ");
        }

        return builder.toString();
    }

    public void ReportUSBStatus() {
        if ( _usbDevice != null )
        {
            UsbInterface writeIntf = _usbDevice.getInterface(0);
            int endpoints = writeIntf.getEndpointCount();
            Log(String.format("We have %d endpoints", endpoints));
        }

    }


    public void ParseTrackerStatus( byte[] data ) {
        ByteArrayInputStream s = new ByteArrayInputStream( data );
        BinaryIn in = new BinaryIn(s);

        byte tmp = in.readByte();
        int data_len = (int)in.readChar();
        tmp = in.readByte();
        int[] pair_states = new int[5];

        for ( int i = 0; i < 5; i++) {
            pair_states[i] = in.readInt();
            Log("Tracker " + i + " status: "+String.format("%02x:%02x:%02x:%02x", pair_states[0]&0xff, (pair_states[0]>>8)&0xff,(pair_states[0]>>16)&0xff,(pair_states[0]>>24)&0xff   ));
        }


        // pair state:
        // 0x4000003 - unpaired, pairing info present?
        // 0x1000003 - unpaired, pairing info not present?
        // 0x320fc008 - paired
        // 0x320ff808 - paired
        // 0x320fa808 - paired


    }


    long last_host_map_ask_ms = 0;
    long last_map_reset = 0;
    int current_map_state = 0;
    String host_ssid = "";
    String host_freq = "";
    String host_passwd = "";
    String host_country = "US";


    public void ParseAck ( byte[] device_addr, byte[] data_raw) {

        String data = new String(data_raw, StandardCharsets.UTF_8);

        if ( data.startsWith(HorusdAck.ACK_CATEGORY_CALIB_1) )
        {
            calib_1 = calib_1 + data.substring(1);
            Log("Got Calib 1 " + calib_1);
        } else if ( data.startsWith(HorusdAck.ACK_CATEGORY_CALIB_2 ) )
        {
            calib_2 = calib_2 + data.substring(1);
            Log("Got Calib 2" + calib_2);
        } else if ( data.startsWith( HorusdAck.ACK_CATEGORY_DEVICE_INFO )) {
            String data_real = data.substring(1);
            Log("Got device info "+data_real);
            if ( data_real.startsWith(HorusdAck.ACK_AZZ) )
            {
                if ( !IsHost(device_addr) ) {
                    SendAckTo(MacToIdx(device_addr), String.format("%s%d",HorusdAck.ACK_LAMBDA_COMMAND,HorusdStatusCmds.RESET_MAP).getBytes(StandardCharsets.UTF_8));
                }

                int id_tmp = MacToIdx( device_addr );

                Log ("AZZ response device id " + id_tmp + " address " + composeString( device_addr ));
                LambdaSetFW( MacToIdx(device_addr), 1);
                //SendAckTo(MacToIdx(device_addr),String.format("%s1",HorusdAck.ACK_FW).getBytes(StandardCharsets.UTF_8) );
            }
        } else if ( data.startsWith(HorusdAck.ACK_CATEGORY_PLAYER)) {
            Log(" ACK PLAYER "+data);
            String data_real = data.substring(1);
            String[] parts = data_real.split(":");
            int idx = -1;
            if (parts.length > 0)
                idx = Integer.decode(parts[0]);

            String args = "";
            if (parts.length > 1) args = parts[1];

            if (idx == HorusdAck.LAMBDA_PROP_GET_STATUS) {
                String[] args_split = args.split(",");
                int key_id = Integer.decode(args_split[0]);
                int state = Integer.decode(args_split[1]);
                String finalMessage = "";


                if (key_id == HorusdStatusCmds.KEY_RECEIVED_HOST_MAP) {
                    if (state == 0 && (System.currentTimeMillis() - last_host_map_ask_ms) > 10000 && IsClientConnected(device_addr)) {
                        last_host_map_ask_ms = System.currentTimeMillis();
                        SendAckTo(MacToIdx(device_addr), String.format("%s%d", HorusdAck.ACK_LAMBDA_COMMAND, HorusdStatusCmds.ASK_MAP).getBytes(StandardCharsets.UTF_8));
                        trackers[MacToIdx(device_addr)].bump_map_once = true;
                        trackers[MacToIdx(device_addr)].has_host_map = false;
                    } else {
                        if (state > 0)
                            trackers[MacToIdx(device_addr)].has_host_map = true;
                        if (trackers[MacToIdx(device_addr)].bump_map_once) trackers[MacToIdx(device_addr)].bump_map_once = false;
                    }

                } else if (key_id == HorusdStatusCmds.KEY_MAP_STATE) {
                    Log ("Map status: " + HorusdStatusCmds.map_status_to_str( state ) );
                    //TODO HandleMapState
                    finalMessage = HorusdStatusCmds.map_status_to_str(state);
                } else if ( key_id == HorusdStatusCmds.KEY_CURRENT_TRACKING_STATE ) {
                    finalMessage = HorusdStatusCmds._pose_status_strs(state);
                }


                Log(" Status returned for SLAM key " + HorusdStatusCmds.slam_key_to_str(key_id) + " Device: (" + composeString( device_addr )+"): " + state + " " + finalMessage);

            }
        } else if ( data.startsWith( HorusdAck.ACK_LAMBDA_STATUS )) {
            Log ( " ACK LAMBDA STATUS " + data);
            SendAckToAll(String.format("%s0", HorusdAck.ACK_FW).getBytes(StandardCharsets.UTF_8) );
        } else if ( data.startsWith( HorusdAck.ACK_WIFI_HOST_SSID)) {
            String data_real = data.substring(2);
            String[] parts = data_real.split(",");

            host_ssid = parts[0];
            host_passwd = parts[1];
            host_freq = parts[2];

            Log (" ACK GOT WIFI: "+host_ssid+" " +host_passwd+" "+ host_freq);


        } else if ( data.startsWith( HorusdAck.ACK_MAP_STATUS)) {
            String data_real = data.substring(2);
            String[] parts = data_real.split(",");

            int map_state = Integer.decode( parts[1] );

            HandleMapState( device_addr, map_state );

            Log ("Map status: " + HorusdStatusCmds.map_status_to_str( map_state ) + " ( " + data_real + " ) " );

        } else if ( data.startsWith(HorusdAck.ACK_WIFI_SSID_PASS)) {
            Log(" ACK SET WIFI");
            WifiSetSSID(MacToIdx(device_addr), host_ssid);
            WifiSetPaassword(MacToIdx(device_addr), host_passwd);
            WifiSetFreq(MacToIdx(device_addr), host_freq);
            WifiSetCountry(MacToIdx(device_addr), host_country);
        } else if ( data.startsWith(HorusdAck.ACK_WIFI_CONNECT)) {

            Log("   Got WIFI_CONNECT ACK " + composeString(device_addr) + " " + Integer.decode ( data.substring(2) ));
            //comms.connected_to_host[mac_to_idx(device_addr)] = (ret > 0)

        } else {
            Log("Got ACK " + data);
        }

    }


    public void CheckMapStatus( byte[] device_addr ) {
        if ( !autoMapReset ) return;

        int idx = MacToIdx( device_addr );
        if ( idx >= 0 && idx < trackers.length )
        {

            boolean bad_tracking_state = false;

            /* If the tracker was in one of the "good states" that but lost tracking for too long */
            if ( ( trackers[idx].tracker_map_state == HorusdStatusCmds.MAP_EXIST
                    || trackers[idx].tracker_map_state == HorusdStatusCmds.MAP_REBUILT
                    || trackers[idx].tracker_map_state == HorusdStatusCmds.MAP_REUSE_OK )
                    && !trackers[idx].IsFullyTracking()
                    && trackers[idx].ValidPoseOlderThan( 10000 )
            ) bad_tracking_state = true;

            if ( bad_tracking_state && trackers[idx].last_map_reset + 10000 < System.currentTimeMillis() ) {
                Log("Tracking lost for too long, restart mapping");
                AckEndMap(MacToIdx(device_addr));
            }
        }
    }


    public void HandleMapState( byte[] device_addr, int map_state ) {
        ///if ( map_state == HorusdStatusCmds.MAP_EXIST )

        int idx = MacToIdx( device_addr );
        if ( idx >= 0 && idx < trackers.length )
        {

            if ( trackers[idx].tracker_map_state != map_state) {
                trackers[idx].last_map_reset = System.currentTimeMillis();
            }

            trackers[idx].tracker_map_state = map_state;
        }
    }



    public static float toFloat(int nHalf)
    {
        int S = (nHalf >>> 15) & 0x1;
        int E = (nHalf >>> 10) & 0x1F;
        int T = (nHalf       ) & 0x3FF;

        E = E == 0x1F
                ? 0xFF  // it's 2^w-1; it's all 1's, so keep it all 1's for the 32-bit float
                : E - 15 + 127;     // adjust the exponent from the 16-bit bias to the 32-bit bias

        // sign S is now bit 31
        // exp E is from bit 30 to bit 23
        // scale T by 13 binary digits (it grew from 10 to 23 bits)
        return Float.intBitsToFloat(S << 31 | E << 23 | T << 13);
    }


    public void CheckButtonInteractions( int id ) {
        long now = System.currentTimeMillis();
        if ( id >= 0 && id < trackers.length ) {
            if ( trackers[id].IsMainButtonPressed() ) {
                Log("Press button");
                if ( now - trackers[id].last_main_button_press < 1000 ) {
                    Log ("Double button click. Force end map");
                    AckEndMap( id );
                }
                trackers[id].last_main_button_press = now;
            } else {
                Log("Release button");
                trackers[id].last_main_button_release = now;
            }
        }
    }

    public void ParsePose ( byte[] device_addr, byte[] data, int pkt_idx) {
        //Log("Parsing pose");
        int id = MacToIdx(device_addr);

        if ( id < 0 || trackers[id].device_address == null ) return ;

        trackers[id].poses_received += 1;

        if ( data.length < 2 ) return;

        int button = ((int)data[1]) & 0xff  ;

        if ( button >= 0x80 ) {
            trackers[id].last_post_btns = trackers[id].pose_btns;
            trackers[id].pose_btns = button;

            if ( trackers[id].last_post_btns != trackers[id].pose_btns ) {
                Log("Tracker " + id + " button changed " + Integer.toHexString( button ) ) ;
                CheckButtonInteractions( id );
            }
        }

        if ( data.length != 0x25 && data.length != 0x27 ) return;

        ByteArrayInputStream s = new ByteArrayInputStream( data );
        BinaryIn in = new BinaryIn(s);

        int idx = in.readByte();
        int btns = in.readByte();

        float x = in.readFloat();
        float y = in.readFloat();
        float z = in.readFloat();

        float rot_x = toFloat( in.readShort() );
        float rot_y = toFloat( in.readShort() );
        float rot_z = toFloat( in.readShort() );
        float rot_w = toFloat( in.readShort() );

        float accel_x = toFloat( in.readShort() );
        float accel_y = toFloat( in.readShort() );
        float accel_z = toFloat( in.readShort() );

        float rot_vel_x = toFloat( in.readShort() );
        float rot_vel_y = toFloat( in.readShort() );
        float rot_vel_z = toFloat( in.readShort() );
        float rot_vel_w = toFloat( in.readShort() );

        int status = in.readByte();

        trackers[id].tracking_state = status;

        trackers[id].time = System.currentTimeMillis();

        if ( trackers[id].IsFullyTracking() ) trackers[id].last_valid_time = trackers[id].time;

        trackers[id].position = new Vector3( x,y,z );
        trackers[id].acceleration = new Vector3( accel_x, accel_y, accel_z);
        trackers[id].rotation = new Quaternion( rot_x, rot_y, rot_z, rot_w );
        trackers[id].pkt_idx = pkt_idx;

        CheckMapStatus( device_addr );
        //Log("Pose received "+x+" "+y+" "+z);


        /*
        idx, btns, pos, rot, acc, rot_vel, tracking_status = struct.unpack("<BB12s8s6s8sB", data[:0x25])

        # tracking_status = 2 => pose + rot
        # tracking_status = 3 => rot only
        # tracking_status = 4 => pose frozen (lost tracking), rots

        pos_arr = np.frombuffer(pos, dtype=np.float32, count=3)
        rot_arr = np.frombuffer(rot, dtype=np.float16, count=4)
        acc_arr = np.frombuffer(acc, dtype=np.float16, count=3)
        rot_vel_arr = np.frombuffer(rot_vel, dtype=np.float16, count=4)
        #print(f"({mac_str(mac)})", hex(idx), pose_status_to_str(tracking_status), "btns:", hex(btns), "pos:", pos_arr, "rot:", rot_arr, "acc:", acc_arr, "rot_vel:", rot_vel_arr, "time_delta", current_milli_time() - self.pose_time[mac_to_idx(mac)])

        #print(hex(idx), hex(btns), hex(self.pose_btns[mac_to_idx(mac)]), hex(self.last_pose_btns[mac_to_idx(mac)]))
        self.pose_quat[mac_to_idx(mac)] = rot_arr
        self.pose_pos[mac_to_idx(mac)] = pos_arr
        self.pose_time[mac_to_idx(mac)] = current_milli_time()

        if btns & 0x80:
            self.last_pose_btns[mac_to_idx(mac)] = self.pose_btns[mac_to_idx(mac)]
            self.pose_btns[mac_to_idx(mac)] = ((btns & 0x7F) << 8) | self.pose_btns[mac_to_idx(mac)] & 0xFF
        else:
            self.last_pose_btns[mac_to_idx(mac)] = self.pose_btns[mac_to_idx(mac)]
            self.pose_btns[mac_to_idx(mac)] = btns | self.pose_btns[mac_to_idx(mac)] & 0xFF00

        if (self.pose_btns[mac_to_idx(mac)] & 0x100) == 0x100 and (self.last_pose_btns[mac_to_idx(mac)] & 0x100) == 0x0:# and comms.get_map_state(mac) == MAP_EXIST:
            print("end map.")
            comms.lambda_end_map(mac)
         */
    }

    public int ticker = 900;
    public void Tick() {
        ticker ++;

        if ( ticker > 1000 ) {
            Log("Tick ...");
            for (int i = 0; i < trackers.length; i++ ) {
                if (  IsClientConnected( i ) ) {
                    AckLambdaAskStatus( i , HorusdStatusCmds.KEY_TRANSMISSION_READY);
                    AckLambdaAskStatus( i , HorusdStatusCmds.KEY_CURRENT_MAP_ID);
                    AckLambdaAskStatus( i , HorusdStatusCmds.KEY_MAP_STATE);
                    AckLambdaAskStatus( i , HorusdStatusCmds.KEY_CURRENT_TRACKING_STATE);

                    if ( trackers[i].tracker_id_number != current_host ) {
                        AckLambdaAskStatus( i , HorusdStatusCmds.KEY_RECEIVED_HOST_ED);
                        AckLambdaAskStatus( i , HorusdStatusCmds.KEY_RECEIVED_HOST_MAP);
                    }
                }
            }
            ticker = 0;
        }
    }


    public void ParseTrackerIncoming( byte[] data ) {
        ByteArrayInputStream s = new ByteArrayInputStream(data);
        BinaryIn in = new BinaryIn(s);

        int cmd_id = in.readByte();
        int pkt_idx = in.readShort();
        byte[] device_addr = sub_array(data,3,6);
        in.readByte();in.readByte();in.readByte();in.readByte();in.readByte();in.readByte();
        int type_maybe = in.readShort();
        int data_len = in.readByte();

        //Log(String.format("Incoming decoded cmd_id: %d pkt_idx: %d type_maybe: %d data_len: %d", cmd_id, pkt_idx, type_maybe, data_len));

        if ( type_maybe == 0x101) {
            //Log("Ackky");
            byte[] data_raw = sub_array( data,0xc, data_len);
            ParseAck( device_addr, data_raw );

        } else if ( type_maybe == 0x110) {
            //Log("Posey");
            byte[] data_raw = sub_array( data,0xc, data_len);
            ParsePose( device_addr, data_raw, pkt_idx );
        }


            /*

        cmd_id, pkt_idx, device_addr, type_maybe, data_len = struct.unpack("<BH6sHB", resp[:0xC])
        #hex_dump(resp)
        #print(hex(cmd_id), hex(pkt_idx), mac_str(device_addr), hex(type_maybe), "data_len:", hex(data_len))

        if type_maybe == 0x101:
            data_raw = resp[0xC:0xC+data_len]
            if self.ack_callback:
                self.ack_callback(self, device_addr, data_raw)
            return
        elif type_maybe == 0x110:
            data = resp[0xC:0xC+data_len]
            #self.parse_pose_data(device_addr, data)
            if self.pose_callback:
                self.pose_callback(self, device_addr, data)
            return

        data = resp[0xC:0xC+data_len]
        data_id = data[0]
        data_real = data[1:]
        print("   data_id:", hex(data_id), "data:", data_real)
        */






    }




    public int[] sub_array( int[] in, int start, int length ){
        if ( start+length > in.length ) return new int[0];

        int[] retVal = new int[length];
        for (int i = start; i < start+length; i++ ) {
            retVal[i-start] = in[i];
        }
        return retVal;
    }

    public byte[] sub_array( byte[] in, int start, int length ){
        if ( start+length > in.length ) return new byte[0];

        byte[] retVal = new byte[length];
        for (int i = start; i < start+length; i++ ) {
            retVal[i-start] = in[i];
        }
        return retVal;
    }

    public void ProcessIncomingMessage( byte[] data ) {
        if ( data.length > 0 ) {
            switch ( data[0] )
            {
                case HorusdDongleCmds.DRESP_PAIR_EVENT:
                    Log("Pair event received");
                    byte[] response_a = ParseHIDResponse( data );
                    int is_unpair = response_a[1];
                    byte[] device_addr = sub_array(response_a, 2, response_a.length-2);
                    Log("Unpairing event?: "+is_unpair + "  " + composeString(response_a) + " " + composeString(device_addr));

                    int idx = MacToIdx(device_addr);

                    if ( idx < 0 ) idx = AddMac( device_addr );

                    int id = device_addr[1] - 0x30;

                    if ( idx >= 0 && idx < trackers.length ) {
                        trackers[idx].tracker_id_number = id;
                    }

                    if ( is_unpair > 0 ) {
                        Log ("Tracker " + id + " Disconnected");
                        HandleDisconnected( id );

                        if ( current_host == id ) current_host = -1;
                        //TODO how to handle disconnect of the SLAM MASTER?
                        return;
                    }

                    AckSetRoleId( MacToIdx( device_addr ), 1 );
                    AckSetTrackingMode( MacToIdx( device_addr ), -1 );

                    if ( current_host == -1 || IsHost(device_addr) )
                    {
                        current_host = id;
                        WifiSetCountry( MacToIdx(device_addr), host_country);
                        //AckSetTrackingHost( MacToIdx(device_addr), current_host);
                        //AckSetWifiHost( MacToIdx(device_addr), current_host);
                        AckSetNewId(  MacToIdx(device_addr), id);
                        LambdaSetFW( MacToIdx(device_addr), 2);
                        AckSetTrackingMode( MacToIdx( device_addr ),  HorusdStatusCmds.TRACKING_MODE_21 ); //HorusdStatusCmds.TRACKING_MODE_SLAM_HOST
                    } else {
                        //#self.wifi_connect(mac_to_idx(paired_mac))

                        AckSetTrackingHost( MacToIdx(device_addr), current_host);
                        AckSetWifiHost( MacToIdx(device_addr), current_host);
                        AckSetNewId( MacToIdx(device_addr), id );
                        LambdaSetFW( MacToIdx(device_addr), 2);
                        AckSetTrackingMode( MacToIdx(device_addr), HorusdStatusCmds.TRACKING_MODE_SLAM_CLIENT);
                        AckEndMap( MacToIdx(device_addr) );
                    }

/*                    if self.current_host_id == -1 or self.is_host(paired_mac):
                    test_mode = TRACKING_MODE_SLAM_HOST
                    self.current_host_id = mac_to_idx(paired_mac)
                    print(f"Making {paired_mac_str} the SLAM host")
                    self.wifi_set_country(mac_to_idx(paired_mac), self.wifi_info["country"])
                    self.ack_set_tracking_host(mac_to_idx(paired_mac), 1)
                    self.ack_set_wifi_host(mac_to_idx(paired_mac), 1)
                    self.ack_set_new_id(mac_to_idx(paired_mac), 0)
            else:
                    test_mode = TRACKING_MODE_SLAM_CLIENT
                #self.wifi_connect(mac_to_idx(paired_mac))
                    new_id = int(mac_to_idx(paired_mac))

                    self.ack_set_tracking_host(mac_to_idx(paired_mac), 0)
                    self.ack_set_wifi_host(mac_to_idx(paired_mac), 0)
                    self.ack_set_new_id(mac_to_idx(paired_mac), new_id)

                    self.ack_set_tracking_mode(mac_to_idx(paired_mac), test_mode)
*/


                    break;
                case HorusdDongleCmds.DRESP_TRACKER_RF_STATUS:
                case HorusdDongleCmds.DRESP_TRACKER_NEW_RF_STATUS:
                    Log("RF Status message");
                    byte[] response_b = ParseHIDResponse( data );
                    ParseTrackerStatus( response_b   );
                    break;
                case 0x29:
                    Log("RF Status message???");
                    break;
                case HorusdDongleCmds.DRESP_TRACKER_INCOMING:
                    //Log("Tracker incoming message");
                    ParseTrackerIncoming( data );
                    Tick();
                    break;
                default:
                    Log("Unknown message");
                    break;
            }
        } else {
            Log("Empty message received");
        }


    }


    private boolean dongleInitialized = false;
    public void InitDongle() {
        if ( !dongleInitialized )
        {
            Log("Initializing Dongle");
            int[] data = new int[7];

            data[0] = HorusdDongleCmds.RF_BEHAVIOR_PAIR_DEVICE;
            data[1] = 1;
            data[2] = 1;
            data[3] = 1;
            data[4] = 1;
            data[5] = 0;
            data[6] = 0;

            SendCommand(HorusdDongleCmds.DCMD_REQUEST_RF_CHANGE_BEHAVIOR, data);

            dongleInitialized = true;
        }

    }


    public static int[] Mac ( int idx ) {
        int[] mac = new int[] {0x23, 0x30, 0x42, 0xB7, 0x82, 0xD3};
        mac[1] |= idx;
        return mac;
    }


    public void SendAckToAll( byte[] ack ) {
        for ( int i = 0; i < trackers.length; i++ ) {
            SendAckTo(i, ack);
        }
    }

    public byte[] SendAckTo(int idx, byte[] ack) {

        if ( idx < 0 || trackers[idx].device_address == null ) return new byte[0];

        //int[] mac = new int[] {0x23, 0x30, 0x42, 0xB7, 0x82, 0xD3};
        //mac[1] |= idx;

        byte[] mac = trackers[idx].device_address;

        //# TX_ACK_TO_MAC checks all MAC bytes,
        //# TX_ACK_TO_PARTIAL_MAC checks the first 2

        ByteArrayOutputStream s = new ByteArrayOutputStream();
        BinaryOut out = new BinaryOut(s);

        //preamble = struct.pack("<BBBBBBBBB", TX_ACK_TO_PARTIAL_MAC, mac[0],mac[1],mac[2],mac[3],mac[4],mac[5],0, 1)
        out.writeByte( HorusdDongleCmds.TX_ACK_TO_PARTIAL_MAC);
        for ( int i = 0; i < mac.length; i++ ) out.writeByte( mac[i] );
        out.writeByte(0);
        out.writeByte(1);

        //ack = struct.pack("<B", len(ack)) + ack.encode("utf-8")

        out.writeByte( ack.length );
        for ( int i = 0; i < ack.length; i++ ) out.writeByte(ack[i]);

        out.close();


        byte[] byte_payload = s.toByteArray();
        int[] payload = new int[byte_payload.length];
        for ( int i = 0; i < payload.length; i++ ) payload[i] = byte_payload[i];

        try {
            Log(String.format("Send ack %s",new String(ack, StandardCharsets.UTF_8)));
        } catch ( Exception e ) {
            Log(String.format("Send ack (raw)%s", composeString(ack)));
        }




        return SendCommand( HorusdDongleCmds.DCMD_TX, payload  ) ;
    }


    //-------------- Tracker Methods -----------------------------
    public int current_host = -1;


    public void Reset() {
        current_host = -1;
        calib_1 = "";
        calib_2 = "";
        last_host_map_ask_ms = 0;
        host_ssid = "";
        host_freq = "";
        lastPoseUpdate = 0;



        host_passwd = "";

        for ( int i = 0; i < trackers.length; i++ ) {
            trackers[i].device_address = null;
            HandleDisconnected( i );
        }

    }


    public void HandleDisconnected ( int idx ) {
        if ( idx < 0 ) return;

        trackers[idx].ResetValues();



    }


    public boolean IsHost ( int id ) {
        boolean retVal = false;

        if ( id >= 0 && trackers[id].tracker_id_number >= 0 && trackers[id].tracker_id_number == current_host ) retVal = true;

        return retVal;

    }
    public boolean IsHost ( byte[] device_addr ) {
        int id = MacToIdx( device_addr );
        return IsHost( id );
    }

    public boolean IsClient( byte[] device_addr ) {
        return !IsHost( device_addr );
    }

    public boolean ClientHasHostMap( byte[] device_addr ) {
        boolean retVal = false;

        int id = MacToIdx( device_addr );

        if ( id >= 0 && id < trackers.length ) {
            retVal = trackers[id].has_host_map;
        }

        return retVal;

    }


    public boolean IsClientConnected( int id ) {
        if ( IsHost( id ) ) return true;

        boolean retVal = false;

        if ( id >= 0 && id < trackers.length ) {
            retVal = trackers[id].has_host_map;
        }

        return retVal;

    }

    public boolean IsClientConnected( byte[] device_addr ) {
        int id = MacToIdx( device_addr );

        return IsClientConnected( id );
    }

    public void AckSetRoleId( int idx, int roleId  ) {
        String msg = String.format("%s%d",HorusdAck.ACK_ROLE_ID, roleId);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));
    }

    public void AckSetTrackingMode( int idx, int mode ) {
        String msg = String.format("%s%d",HorusdAck.ACK_TRACKING_MODE,mode);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));
    }

    public void WifiSetCountry( int idx, String country)
    {
        String msg = String.format("%s%s",HorusdAck.ACK_WIFI_COUNTRY,country);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));

    }

    public void AckSetTrackingHost( int idx, int value) {
        String msg = String.format("%s%d",HorusdAck.ACK_TRACKING_HOST,value);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));

    }

    public void AckSetWifiHost( int idx, int value) {
        String msg = String.format("%s%d",HorusdAck.ACK_WIFI_HOST,value);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));

    }

    public void AckSetNewId( int idx, int value) {
        String msg = String.format("%s%d",HorusdAck.ACK_NEW_ID,value);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));

    }




    int reset_map_stage = 0;
    long reset_map_started_ts = 0;
    public void ResetMapFull ( int idx ) {
        Log("Reset map full");
        AckSetTrackingMode( idx, 20 );
        reset_map_stage = 1;
        reset_map_started_ts = System.currentTimeMillis();
    }

    public void ResetMapFullFinish( int idx ) {
        if ( IsHost(idx ) ) {
            AckSetTrackingMode( idx,  HorusdStatusCmds.TRACKING_MODE_21 );
        } else {
            AckSetTrackingMode( idx,  HorusdStatusCmds.TRACKING_MODE_SLAM_CLIENT );
        }
    }


    // Identify tracker AHT1,1,1,1

    public void AckGeneric ( int idx, String msg) {
        Log("ACK " + msg);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));
    }

    public void AckLambdaAskStatus( int idx, int key_id ) {

        String msg = String.format("%s%d",HorusdAck.ACK_LAMBDA_ASK_STATUS, key_id);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));

    }

    public void AckEndMap ( int idx ) {
        SendAckTo(idx, HorusdAck.ACK_END_MAP.getBytes(StandardCharsets.UTF_8));
        trackers[idx].last_map_reset = System.currentTimeMillis();
        trackers[idx].tracker_map_state = 0;
    }

    public void AckPowerOff ( int idx ) {
        SendAckTo(idx, HorusdAck.ACK_POWER_OFF.getBytes(StandardCharsets.UTF_8));
    }

    public void AckTrackerReset ( int idx ) {
        SendAckTo(idx, HorusdAck.ACK_RESET.getBytes(StandardCharsets.UTF_8));
    }



    public void WifiSetSSID( int idx, String ssid) {
        WifiSetSSIDFull(idx, ssid.substring(0, Math.min(8, ssid.length())));
        int i = 8;
        while ( i < ssid.length() ) {
            WifiSetSSIDAppend(idx, ssid.substring(i, Math.min(i+8, ssid.length())));
            i = i + 8;
        }

    }

    public void WifiSetSSIDFull( int idx, String ssid ) {

        String msg = String.format("%s%s",HorusdAck.ACK_WIFI_SSID_FULL, ssid);

        Log("ACK " + msg);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));

    }
    public void WifiSetSSIDAppend( int idx, String ssid ) {

        String msg = String.format("%s%s",HorusdAck.ACK_WIFI_SSID_APPEND, ssid);

        Log("ACK " + msg);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));

    }

    public void WifiSetFreq( int idx, String freq ) {

        String msg = String.format("%s%s",HorusdAck.ACK_WIFI_FREQ, freq );

        Log("ACK " + msg);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));

    }

    public void WifiSetPaassword( int idx, String password ) {

        String msg = String.format("%s%s",HorusdAck.ACK_WIFI_PW, password );

        Log("ACK " + msg);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));


    }



    public void LambdaSetFW( int idx, int fw ) {

        String msg = String.format("%s%d",HorusdAck.ACK_FW, fw );

        Log("ACK " + msg);

        SendAckTo(idx, msg.getBytes(StandardCharsets.UTF_8));

    }

/*

    def lambda_end_map(self, device_addr):
            self.send_ack_to(mac_to_idx(device_addr), ACK_END_MAP)

    def send_ack_to(self, idx, ack):
    print("UNIMPLEMENTED")

    def send_ack_to_all(self, ack):
            for i in range(0, 5):
            self.send_ack_to(i, ack)

    def wifi_connect(self, idx):
            self.send_ack_to(idx, ACK_WIFI_CONNECT)

            #def wifi_set_ssid_password(self, idx, ssid, passwd):
            #    self.send_ack_to(idx, f"{ACK_WIFI_SSID_PASS}{ssid},{passwd}")

    def wifi_set_ssid(self, idx, ssid):
    print(ssid[:8])
        self._wifi_set_ssid_full(idx, ssid[:8])
            for i in range(8, len(ssid), 8):
    print(ssid[i:i+8])
            self._wifi_set_ssid_append(idx, ssid[i:i+8])

    def _wifi_set_ssid_full(self, idx, ssid):
            self.send_ack_to(idx, ACK_WIFI_SSID_FULL + ssid)

    def _wifi_set_ssid_append(self, idx, ssid):
            self.send_ack_to(idx, ACK_WIFI_SSID_APPEND + ssid)

    def wifi_set_country(self, idx, country):
            self.send_ack_to(idx, ACK_WIFI_COUNTRY + country)

    def wifi_set_password(self, idx, passwd):
            self.send_ack_to(idx, ACK_WIFI_PW + passwd)

    def wifi_set_freq(self, idx, freq):
            self.send_ack_to(idx, f"{ACK_WIFI_FREQ}{freq}")



    def ack_set_tracking_host(self, idx, val):
            self.send_ack_to(idx, f"{ACK_TRACKING_HOST}{val}")

    def ack_set_wifi_host(self, idx, val):
            self.send_ack_to(idx, f"{ACK_WIFI_HOST}{val}")

    def ack_set_new_id(self, idx, val):
            self.send_ack_to(idx, f"{ACK_NEW_ID}{val}")

    def ack_lambda_ask_status(self, idx, key_id):
            self.send_ack_to(idx, f"{ACK_LAMBDA_ASK_STATUS}{key_id}")

    def ack_lambda_property(self, idx):
            self.send_ack_to(idx, ACK_LAMBDA_PROPERTY)
*/





}