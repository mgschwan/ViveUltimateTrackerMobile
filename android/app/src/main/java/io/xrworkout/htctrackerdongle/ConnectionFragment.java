package io.xrworkout.htctrackerdongle;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;

import androidx.appcompat.widget.AppCompatToggleButton;
import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectionFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ConnectionFragment extends Fragment {

    final String TAG = "HIDTester";

    MediaPlayer backgroundPlayer = null;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View tmp = inflater.inflate(R.layout.fragment_connection, container, false);


        Context context = getActivity();
        SharedPreferences sharedPref = context.getSharedPreferences(
                "htc_tracker_preferences", Context.MODE_PRIVATE);

        int port_pref = sharedPref.getInt("target_port",9000);
        float flooroffset_pref = sharedPref.getFloat( "floor_offset", 0.0f );

        String address_pref = sharedPref.getString("target_address","127.0.0.1");

        EditText address_field = tmp.findViewById(R.id.poseTargetHost);
        EditText port_field = tmp.findViewById(R.id.poseTargetPort);
        TextView flooroffset_field = tmp.findViewById(R.id.floorOffset);

        address_field.setText( address_pref );
        port_field.setText( Integer.toString( port_pref ) );
        flooroffset_field.setText( Float.toString( flooroffset_pref ) );

        tmp.findViewById(R.id.connectButton).setEnabled(false);

        ((Button) tmp.findViewById(R.id.grantUsbPermissionButton)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("BUTTONS", "User tapped the Grant USB Permission Button");
                if ( DeviceContainer.bridge == null )
                {
                    // 0x0bb4 = 2996
                    // 0x0350 = 848
                    DeviceContainer.bridge = new HidBridge(context, 848, 2996);
                }

                if ( DeviceContainer.bridge.OpenDevice() )
                {
                    // Disable the grantUsbPermissionButton
                    v.setEnabled(false);
                    // Enable the connectButton
                    tmp.findViewById(R.id.connectButton).setEnabled(true);
                }
            }
        });


        ((Button) tmp.findViewById(R.id.calibrateFloor)).setOnClickListener( new View.OnClickListener() {

    @Override
            public void onClick(View view) {

                if ( DeviceContainer.deviceOpen ) {
                    float offset = 0;

                    if (DeviceContainer.bridge.trackers[0] != null && DeviceContainer.bridge.trackers[0].IsActive()) {
                        offset = (float) DeviceContainer.bridge.trackers[0].position.getY();
                        DeviceContainer.bridge.SetFloorOffset( offset );
                    }

                    flooroffset_field.setText( Float.toString(offset));

                    SharedPreferences.Editor editor = sharedPref.edit();
                    editor.putFloat("floor_offset", offset);
                    editor.apply();
                }

            }
        });

        ((Button) tmp.findViewById(R.id.connectButton)).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Log.d("BUTTONS", "User tapped the Connect Button");

                if ( DeviceContainer.bridge == null || !DeviceContainer.deviceOpen ) {
                    Log.d( TAG, "Device not connected" );
                    return;
                }

                String address = address_field.getText().toString();
                String portText = port_field.getText().toString();
                String offsetText = flooroffset_field.getText().toString();

                float offset = Float.parseFloat( offsetText );

                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putInt("target_port", Integer.parseInt(portText));
                editor.putString("target_address", address);
                editor.putFloat("floor_offset", offset);
                editor.apply();


//                String address = ((EditText) tmp.findViewById(R.id.poseTargetHost)).getText().toString();
//                String portText = ((EditText) tmp.findViewById(R.id.poseTargetPort)).getText().toString();
                    if ( DeviceContainer.bridge != null ) {
                        int port = Integer.parseInt(portText);
                        DeviceContainer.bridge.SetPoseTargetAddress( address, port );
                        DeviceContainer.bridge.SetFloorOffset( offset );

                        if ( ((Switch) tmp.findViewById(R.id.oscSwitch)).isChecked() ) {
                            Log.d(TAG,"Enabling OSC");
                            DeviceContainer.bridge.EnableOSC();
                        }




                    }



                if (DeviceContainer.deviceOpen) {
                    DeviceContainer.bridge.StartReadingThread();

                    if ( backgroundPlayer == null ) {
                        backgroundPlayer = MediaPlayer.create(getContext(), R.raw.silence_30sec);
                        backgroundPlayer.setLooping( true );
                        backgroundPlayer.start();
                        Log.d(TAG,"Background audio player started");
                    }



                }

                EditText statusMessages = (EditText) tmp.findViewById(R.id.statusMessages);

                if (DeviceContainer.deviceOpen){
                    statusMessages.append("Initializing HTC Tracker\n");
                    statusMessages.append("PCB ID: " + DeviceContainer.bridge.GetPCBID()+ "\n");
                    statusMessages.append("SKU ID: " + DeviceContainer.bridge.GetSKUID()+ "\n");
                    statusMessages.append("Serial Number: " + DeviceContainer.bridge.GetSN()+ "\n");
                    statusMessages.append("Sending data to: "+address);
                    DeviceContainer.bridge.InitDongle();

                } else {
                    Log.d(TAG, "Device not found");
                }

            }
        });

        ((AppCompatToggleButton) tmp.findViewById(R.id.shouldLogButton)).setOnCheckedChangeListener(
                (buttonView, isChecked) -> {
                    DeviceContainer.bridge.shouldLog = isChecked ;
                }
        );




        return tmp;

    }
}
