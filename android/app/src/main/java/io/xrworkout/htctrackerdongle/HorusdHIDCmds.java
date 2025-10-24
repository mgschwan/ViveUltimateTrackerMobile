package io.xrworkout.htctrackerdongle;

public class HorusdHIDCmds {

/* deprecated    public static int MacToIdx( byte[] device_address ) {
        int retVal = -1;

        if ( device_address.length > 1 )
        {
            retVal = device_address[1] & 0xf;
        }
        retVal = 0;
        return retVal;
    }
*/


    //FILE OPERATIONS
    final static int PACKET_FILE_READ = 0x10;
    final static int PACKET_READ_FILEDATA = 0x11;
    final static int PACKET_READ_FILEDATA_END = 0x12;
    final static int PACKET_READ_FILEBIGDATA = 0x13;
    final static int PACKET_WRITE_FILESIZE = 0x16;
    final static int PACKET_WRITE_FILEDATA = 0x17;
    final static int PACKET_FILE_WRITE = 0x18;
    final static int PACKET_FILE_DELETE = 0x19;

    //ACCESSORY B4
    final static int PACKET_ACCESSORY_B4 = 0xab4;

    //NFO/STATUS
    final static int PACKET_GET_STR_INFO = 0xa001;
    final static int PACKET_GET_STATUS = 0xa002;
    final static int PACKET_SET_TRACKING_MODE =  0xa003;
    final static int PACKET_SET_REBOOT =  0xa004;
    final static int PACKET_SET_STR_INFO = 0xa005;

    //CAMERA
    final static int PACKET_SET_POWER_OCVR = 0xa101;
    final static int PACKET_SET_POWER_PCVR = 0xa102;
    final static int PACKET_SET_POWER_EYVR = 0xa103;

    //?
    final static int PACKET_SET_CAMERA_FPS = 0xa105;
    final static int PACKET_SET_CAMERA_POLICY = 0xa106;

    // ??
    final static int PACKET_SET_USER_TIME = 0xa111;
    final static int PACKET_SET_OT_STATUS = 0xa112;
    final static int PACKET_GET_ACK = 0xa113;
    final static int PACKET_SET_PLAYER_STR = 0xa114;
    final static int PACKET_SET_HAPTIC = 0xa115;
    final static int PACKET_SET_ACK = 0xa116;

    final static int PACKET_SET_WATCHDOG_KICK = 0xa121;
    final static int PACKET_SET_FOTA_BY_PC = 0xa122;

    // WIFI
    final static int PACKET_SET_WIFI_SSID_PW = 0xa151;
    final static int PACKET_SET_WIFI_FREQ = 0xa152;
    final static int PACKET_SET_WIFI_SSID = 0xa153;
    final static int PACKET_SET_WIFI_PW = 0xa154;
    final static int PACKET_SET_WIFI_COUNTRY = 0xa155;
    final static int PACKET_GET_WIFI_HOST_INFO = 0xa156;
    final static int PACKET_SET_WIFI_ONLY_MODE = 0xa157;
    final static int PACKET_GET_WIFI_ONLY_MODE = 0xa158;

    // FACE TRACKING?
    final static int PACKET_SET_FT_LOCK = 0xa171;
    final static int PACKET_SET_FT_UNLOCK = 0xa172;
    final static int PACKET_SET_FT_FAIL = 0xa173;

    // MISC/FACTORY
    final static int PACKET_GET_PROPERTY = 0xa201;
    final static int PACKET_SET_PROPERTY = 0xa202;

    final static int PACKET_SET_FACTORY = 0xafff;

}
