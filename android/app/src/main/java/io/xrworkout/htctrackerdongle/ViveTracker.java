package io.xrworkout.htctrackerdongle;

public class ViveTracker {
    public Vector3 pose_received = new Vector3(0,0,0);
    public Quaternion rotation = new Quaternion(0,0,0,1);
    public Vector3 position = new Vector3(0,0,0);
    public Vector3 acceleration = new Vector3(0,0,0);
    public long time = 0;
    public long last_valid_time = 0;
    public int pkt_idx = 0;
    public int pose_btns = 0;
    public int last_post_btns = 0;
    public int wip_pose_btns = 0;

    public int poses_received = 0;
    public int tracker_id_number = 0;

    public int tracker_map_state = 0;
    public long last_map_reset = 0;

    public int stuck_on_static = 0;
    public int stuck_on_exists = 0;
    public int stuck_on_not_checked = 0;
    public boolean bump_map_once = true;
    public boolean bump_map_once_2 = true;
    public boolean has_host_map = false;

    public int tracking_state = 0;

    public long last_main_button_press = 0;
    public long last_main_button_release = 0;



    byte[] device_address;

    byte[] raw_pose_message = new byte[128];
    int raw_pose_message_len = 0;
    int last_pose_message_idx = 0;
    int current_pose_message_idx = 0;


    public boolean IsDevice( byte[] device_addr ) {
        boolean retVal = false;
        if ( device_address != null && device_addr != null && device_addr.length == device_address.length ) {
            retVal = true;
            for ( int i = 0; i < device_addr.length; i++ )
            {
                if ( device_addr[i] != device_address[i] ) {
                    retVal = false;
                    break;
                }
            }
        }
        return retVal;
    }


    public String TrackingState() {
        if ( tracking_state == 2 ) return "Pose + Rot";
        if (tracking_state == 3 ) return "Rot only";
        if ( tracking_state == 4 ) return "Pose(lost) Rot";

        return "Unknown";
    }

    public boolean IsFullyTracking() {
        return tracking_state == 2;
    }

    public boolean ValidPoseOlderThan( long duration ) {
        return time - last_valid_time > duration;
    }


    public boolean IsActive() {
        return System.currentTimeMillis() - time < 1000;
    }


    public boolean IsMainButtonPressed() {
        return (pose_btns & 0x81) == 0x81;
    }

    public void ResetValues() {
        tracker_map_state = 0;
        stuck_on_static  = 0;
        stuck_on_exists = 0;
        stuck_on_not_checked = 0;
        bump_map_once = true;
        bump_map_once_2 = true;
        has_host_map = false;
        poses_received = 0;
        device_address = null;
        pkt_idx = 0;
        last_map_reset = 0;
        time = 0;
        last_valid_time = 0;
        last_pose_message_idx = 0;
        current_pose_message_idx = 0;
        raw_pose_message_len = 0;
        tracker_id_number = -1;

        last_main_button_press = 0;
        last_main_button_release = 0;
    }
    
    
    
}
