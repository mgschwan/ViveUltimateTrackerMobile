package io.xrworkout.htctrackerdongle;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ToggleButton;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link StatusFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class StatusFragment extends Fragment {

    public Handler mHandler;
    public EditText[] positionDataFields = new EditText[5];

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View tmp = inflater.inflate(R.layout.fragment_status, container, false);

        mHandler = new Handler();

        ToggleButton flipXPos = tmp.findViewById(R.id.flipXPos);
        ToggleButton flipYPos = tmp.findViewById(R.id.flipYPos);
        ToggleButton flipZPos = tmp.findViewById(R.id.flipZPos);

        ToggleButton flipXRot = tmp.findViewById(R.id.flipXRot);
        ToggleButton flipYRot = tmp.findViewById(R.id.flipYRot);
        ToggleButton flipZRot = tmp.findViewById(R.id.flipZRot);

        positionDataFields[0] = (EditText) tmp.findViewById(R.id.positionData1);
        positionDataFields[1] = (EditText) tmp.findViewById(R.id.positionData2);
        positionDataFields[2] = (EditText) tmp.findViewById(R.id.positionData3);
        positionDataFields[3] = (EditText) tmp.findViewById(R.id.positionData4);
        positionDataFields[4] = (EditText) tmp.findViewById(R.id.positionData5);

        Runnable mUpdateTimeTask = new Runnable() {
            public void run() {
                positionDataFields[0].setText("");
                positionDataFields[1].setText("");
                positionDataFields[2].setText("");
                positionDataFields[3].setText("");
                positionDataFields[4].setText("");

                if (DeviceContainer.deviceOpen) {
                    DeviceContainer.bridge.flipXRot = flipXRot.isChecked();
                    DeviceContainer.bridge.flipYRot = flipYRot.isChecked();
                    DeviceContainer.bridge.flipZRot = flipZRot.isChecked();

                    DeviceContainer.bridge.invertX = flipXPos.isChecked();
                    DeviceContainer.bridge.invertY = flipYPos.isChecked();
                    DeviceContainer.bridge.invertZ = flipZPos.isChecked();

                    for ( int i = 0; i < Math.min(positionDataFields.length, DeviceContainer.bridge.trackers.length) ; i++ ) {

                        if ( DeviceContainer.bridge.trackers[i] != null && DeviceContainer.bridge.trackers[i].IsActive() ) {
                            float xPos = (float) ( DeviceContainer.bridge.invertX ? -DeviceContainer.bridge.trackers[i].position.getX() : DeviceContainer.bridge.trackers[i].position.getX() );
                            float yPos = (float) ( DeviceContainer.bridge.invertY ? -DeviceContainer.bridge.trackers[i].position.getY() : DeviceContainer.bridge.trackers[i].position.getY() );
                            float zPos = (float) ( DeviceContainer.bridge.invertZ ? -DeviceContainer.bridge.trackers[i].position.getZ() : DeviceContainer.bridge.trackers[i].position.getZ() );

                            Vector3 rot = DeviceContainer.bridge.trackers[i].rotation.toEuler();
                            float x = (float)rot.getX();
                            float y = (float)rot.getY();
                            float z = (float)rot.getZ();

                            x = (float) ( DeviceContainer.bridge.flipXRot ? -x : x );
                            y = (float) ( DeviceContainer.bridge.flipYRot ? -y : y );
                            z = (float) ( DeviceContainer.bridge.flipZRot ? -z : z );

                            if ( x < 0 ) x+=2*Math.PI;
                            if ( y < 0 ) y+=2*Math.PI;
                            if ( z < 0 ) z+= 2*Math.PI;
                            if ( x > 2*Math.PI ) x -= 2*Math.PI;
                            if ( y > 2*Math.PI ) y -= 2*Math.PI;
                            if ( z > 2*Math.PI ) z -= 2*Math.PI;

                            float xRot = (float) ( x * 180/Math.PI  );
                            float yRot = (float) ( y * 180/Math.PI  );
                            float zRot = (float) ( z * 180/Math.PI  );

                            positionDataFields[i].append("Pos: " + String.format("%.3f | %.3f,%.3f",xPos,yPos,zPos ) + " \n " +  xRot + " | " + yRot + " | " + zRot + " \n " + String.format("%.3f | %.3f,%.3f",x,y,z ) + " \n " + DeviceContainer.bridge.trackers[i].TrackingState() + " / " + HorusdStatusCmds.map_status_to_str( DeviceContainer.bridge.trackers[i].tracker_map_state ) );
                        } else {
                            positionDataFields[i].append("Tracker #"+i + " inactive");
                        }
                    }

                    mHandler.postDelayed(this, 200);
                }
            }
        };




        mHandler.postDelayed(mUpdateTimeTask, 200);

        return tmp;
    }
}