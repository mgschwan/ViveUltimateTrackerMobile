package io.xrworkout.htctrackerdongle;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ToolsFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class ToolsFragment extends Fragment {

    final String TAG = "HIDTester";
    public TextView ackText;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View tmp =  inflater.inflate(R.layout.fragment_tools, container, false);

        ackText = (TextView) tmp.findViewById(R.id.ackText);

        Button button = (Button) tmp.findViewById(R.id.endMapButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (DeviceContainer.deviceOpen){
                    for ( int i = 0; i < DeviceContainer.bridge.trackers.length; i++ ) {
                        //if ( bridge.trackers[0].IsActive() ) {
                        Log.d(TAG, "End map of tracker " + i );
                        DeviceContainer.bridge.AckEndMap( i );
                        //}
                    }
                } else {
                    Log.d(TAG, "Device not found");
                }
            }
        });



        ((Button) tmp.findViewById(R.id.resetMapButton)).setOnClickListener( new View.OnClickListener(){
            public void onClick(View v){
                if (DeviceContainer.deviceOpen) {
                    for ( int i = 0; i < DeviceContainer.bridge.trackers.length; i++ ) {
                        if (DeviceContainer.bridge.trackers[i].IsActive()) {
                            Log.d(TAG, "Reset Map " + i);
                            DeviceContainer.bridge.ResetMapFull( i );
                        }
                    }
                }
            }
        });

        ((Button) tmp.findViewById(R.id.turnOffButton)).setOnClickListener( new View.OnClickListener(){
            public void onClick(View v){
                if (DeviceContainer.deviceOpen) {
                    for ( int i = 0; i < DeviceContainer.bridge.trackers.length; i++ ) {
                        if (DeviceContainer.bridge.trackers[i].IsActive()) {
                            Log.d(TAG, "Turn off tracker " + i);
                            DeviceContainer.bridge.AckPowerOff(i);
                        }
                    }
                }
            }
        });

        ((Button) tmp.findViewById(R.id.genericAckButton)).setOnClickListener( new View.OnClickListener(){
            public void onClick(View v){
                Log.d(TAG, "Generic Ack");
                if (DeviceContainer.deviceOpen) {
                    for ( int i = 0; i < DeviceContainer.bridge.trackers.length; i++ ) {
                        if (DeviceContainer.bridge.trackers[i].IsActive()) {
                            String genericAckText = ackText.getText().toString();
                            Log.d(TAG, "Generic Ack " + i + " " +genericAckText);
                            DeviceContainer.bridge.AckGeneric(i, genericAckText );

                        }
                    }
                }
            }
        });


        ((Button) tmp.findViewById(R.id.resetTrackerButton)).setOnClickListener( new View.OnClickListener(){
            public void onClick(View v){
                if (DeviceContainer.deviceOpen) {
                    for ( int i = 0; i < DeviceContainer.bridge.trackers.length; i++ ) {
                        if (DeviceContainer.bridge.trackers[i].IsActive()) {
                            Log.d(TAG, "Tracker Reset " + i);
                            DeviceContainer.bridge.AckTrackerReset(i);
                        }
                    }
                }
            }
        });

        return tmp;


    }
}