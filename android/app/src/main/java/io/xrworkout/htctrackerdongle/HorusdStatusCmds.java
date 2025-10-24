package io.xrworkout.htctrackerdongle;

public class HorusdStatusCmds {

    // Work state enum, set by HORUS_CMD_POWER;
    final static int WS_STANDBY = 0x0;
    final static int WS_CONNECTING = 0x1;
    final static int WS_REPAIRING = 0x2;
    final static int WS_CONNECTED = 0x3;
    final static int WS_TRACKING = 0x4;
    final static int WS_RECOVERY = 0x5;
    final static int WS_REBOOT = 0x6;
    final static int WS_SHUTDOWN = 0x7;
    final static int WS_OCVR = 0x8;
    final static int WS_PCVR = 0x9;
    final static int WS_EYVR = 0xa;
    final static int WS_RESTART = 0xb;

    // Error returns;
    final static int ERR_BUSY = 0x2;
    final static int ERR_03 = 0x3;
    final static int ERR_UNSUPPORTED = 0xEE;


    final static int PAIRSTATE_1 = 0x0001;
    final static int PAIRSTATE_2 = 0x0002;
    final static int PAIRSTATE_4 = 0x0004;
    final static int PAIR_STATE_PAIRED = 0x0008;
    final static int PAIRSTATE_10 = 0x0010;

    // SetStatus/GetStatus;
    final static int HDCC_BATTERY = 0x0;
    final static int HDCC_IS_CHARGING = 0x1;
    final static int HDCC_POGO_PINS = 0x3;
    final static int HDCC_DEVICE_ID = 0x4;
    final static int HDCC_TRACKING_HOST = 0x5;
    final static int HDCC_WIFI_HOST = 0x6;
    final static int HDCC_7 = 0x7; // LED?;
    final static int HDCC_FT_OVER_WIFI = 0x8;
    final static int HDCC_ROLE_ID = 0xA;
    final static int HDCC_WIFI_CONNECTED = 0xC;
    final static int HDCC_HID_CONNECTED = 0xD;
    final static int HDCC_E = 0xE; // related to ROLE_ID? Sent on pairing.;
    final static int HDCC_WIFI_ONLY_MODE = 0xF;
    final static int HDCC_10 = 0x10; // pose related;

    final static int TRACKING_MODE_NONE = -1; // checks persist.lambda.3rdhost;
    final static int TRACKING_MODE_1 = 1; // gyro only? persist.lambda.3rdhost=0, persist.lambda.normalmode=1 persist.lambda.trans_setup=0;
    final static int TRACKING_MODE_2 = 2; // body?;
    final static int TRACKING_MODE_SLAM_CLIENT = 11; // gyro only? persist.lambda.3rdhost=0, persist.lambda.normalmode=0; // client?;
    final static int TRACKING_MODE_21 = 21; // body tracking? persist.lambda.3rdhost;
    final static int TRACKING_MODE_SLAM_HOST = 20; // SLAM persist.lambda.3rdhost=1, persist.lambda.normalmode=0;
    // 22 also triggers wifi hosting but doesn't set as host?;
    final static int TRACKING_MODE_51 = 51; // SetUVCStatus?;

    // GET_STATUS;
    final static int KEY_TRANSMISSION_READY = 0;
    final static int KEY_RECEIVED_FIRST_FILE = 1;
    final static int KEY_RECEIVED_HOST_ED = 2;
    final static int KEY_RECEIVED_HOST_MAP = 3;
    final static int KEY_CURRENT_MAP_ID = 4;
    final static int KEY_MAP_STATE = 5;
    final static int KEY_CURRENT_TRACKING_STATE = 6;

    public static String slam_key_to_str( int idx ) {

        String[] _slam_key_strs = new String[]{"TRANSMISSION_READY", "RECEIVED_FIRST_FILE", "RECEIVED_HOST_ED", "RECEIVED_HOST_MAP", "CURRENT_MAP_ID", "MAP_STATE", "CURRENT_TRACKING_STATE"};

        return _slam_key_strs[idx];
    }

    // Commands;
    final static int ASK_ED = 0;
    final static int ASK_MAP = 1;
    final static int KF_SYNC = 2;
    final static int RESET_MAP = 3;


    public static String map_status_to_str( int idx ) {

        String[] _map_status_strs = new String[]{"MAP_NOT_CHECKED","MAP_EXIST","MAP_NOTEXIST","MAP_REBUILT","MAP_SAVE_OK","MAP_SAVE_FAIL","MAP_REUSE_OK","MAP_REUSE_FAIL_FEATURE_DIFF","MAP_REUSE_FAIL_FEATURE_LESS","MAP_REBUILD_WAIT_FOR_STATIC","MAP_REBUILD_CREATE_MAP"};

        return _map_status_strs[idx];

    }

    // Map status;
    final static int MAP_NOT_CHECKED = 0;
    final static int MAP_EXIST = 1;
    final static int MAP_NOTEXIST = 2;
    final static int MAP_REBUILT = 3;
    final static int MAP_SAVE_OK = 4;
    final static int MAP_SAVE_FAIL = 5;
    final static int MAP_REUSE_OK = 6;
    final static int MAP_REUSE_FAIL_FEATURE_DIFF = 7;
    final static int MAP_REUSE_FAIL_FEATURE_LESS = 8;
    final static int MAP_REBUILD_WAIT_FOR_STATIC = 9;
    final static int MAP_REBUILD_CREATE_MAP = 10;

    // Pose state;
    public static String _pose_status_strs( int idx ) {

        String[] _pose_status_strs = new String[]{"NO_IMAGES_YET", "NOT_INITIALIZED", "OK", "LOST", "RECENTLY_LOST", "SYSTEM_NOT_READY"};

        return _pose_status_strs[idx];

    }

    final static int POSE_SYSTEM_NOT_READY = -1;
    final static int POSE_NO_IMAGES_YET = 0;
    final static int POSE_NOT_INITIALIZED = 1;
    final static int POSE_OK = 2;
    final static int POSE_LOST = 3;
    final static int POSE_RECENTLY_LOST = 4;

    // imu state;
    final static int POSESTATE_OK = 0;
    final static int POSESTATE_LOST = 1;
    final static int POSESTATE_UNINITIALIZED = 2;
    final static int POSESTATE_RECOVER = 3;
    final static int POSESTATE_FOV_BOUNDARY = 4;
    final static int POSESTATE_FOV_OCCLUSION = 5;
    final static int POSESTATE_DEAD_ZONE = 6;
    final static int POSESTATE_NOMEASUREMENT = 7;
    final static int POSESTATE_NONCONVERGE = 8;
    final static int POSESTATE_IK = 9;
    final static int POSESTATE_INTEGRATOR = 10;
    final static int POSESTATE_NEW_MAP = 11;

    final static int SYSTEM_STATE_NONE = 0;
    final static int SYSTEM_STATE_INIT = 1;
    final static int SYSTEM_STATE_SAVE_ROOM_SETUP = 2;
    final static int SYSTEM_STATE_ACTIVE = 3;


}
