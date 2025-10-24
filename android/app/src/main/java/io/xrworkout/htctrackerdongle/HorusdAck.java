package io.xrworkout.htctrackerdongle;

public class HorusdAck {

    final static String ACK_CATEGORY_CALIB_1 = "C";
    final static String ACK_CATEGORY_CALIB_2 = "c";
    final static String ACK_CATEGORY_DEVICE_INFO = "N";
    final static String ACK_CATEGORY_PLAYER = "P"; //Lambda (SLAM) related cmds

    final static String ACK_ANA = "ANA";// # TODO, "OT1"? recv'd from tracker on connect (NANA?)
    final static String ACK_DEVICE_SN = "ADS";// # recv'd from tracker on connect (NADS?)
    final static String ACK_SHIP_SN = "ASS";// # recv'd from tracker on connect (NASS?)
    final static String ACK_SKU_ID = "ASI";// # recv'd from tracker on connect (NASI?)
    final static String ACK_PCB_ID = "API";// # recv'd from tracker on connect (NAPI?)
    final static String ACK_VERSION = "AV1";// # recv'd from tracker on connect (NAV?)
    final static String ACK_VERSION_ALT = "Av1";// # not actually sent
    final static String ACK_AZZ = "AZZ";// # NAZZ? no data.
    final static String ACK_ARI = "ARI";// # NARI?
    final static String ACK_AGN = "AGN";// # NAGN?

    final static String ACK_LAMBDA_PROPERTY = "LP";// # identical to AGN? 0,1,0 -- trans_setup, normalmode, 3rdhost. Can also be sent to check status.
    final static String ACK_LAMBDA_STATUS = "LS";//

    final static String ACK_START_FOTA = "AFM";//
    final static String ACK_CAMERA_FPS = "ACF";//
    final static String ACK_CAMERA_POLICY = "ACP";//
    final static String ACK_TRACKING_MODE = "ATM";//
    final static String ACK_TRACKING_HOST = "ATH";//
    final static String ACK_WIFI_HOST = "AWH";//
    final static String ACK_TIME_SET = "ATS";// # SetUserTime, calls clock_settime in seconds
    final static String ACK_ROLE_ID = "ARI";//
    final static String ACK_GET_INFO = "AGI";// # complicated for some reason, takes a list of ints, fusionmode related
    final static String ACK_END_MAP = "ALE";//
    final static String ACK_NEW_ID = "ANI";// # sets DeviceID, WiFi related?
    final static String ACK_ATW = "ATW";// # enables acceleration data?

    final static String ACK_POWER_OFF_CLEAR_PAIRING_LIST = "APC";//
    final static String ACK_POWER_OFF = "APF";//
    final static String ACK_STANDBY = "APS";//
    final static String ACK_RESET = "APR";//

    final static String ACK_WIFI_SSID_PASS = "WS";//
    final static String ACK_WIFI_SSID_FULL = "Ws";//
    final static String ACK_WIFI_IP = "WI";//
    final static String ACK_WIFI_IP_2 = "Wi";//
    final static String ACK_WIFI_CONNECT = "WC";//
    final static String ACK_WIFI_COUNTRY = "Wc";//
    final static String ACK_WIFI_FREQ = "Wf";//
    final static String ACK_WIFI_PW = "Wp";//
    final static String ACK_WIFI_SSID_APPEND = "Wt";//
    final static String ACK_WIFI_ERROR = "WE";//
    final static String ACK_WIFI_HOST_SSID = "WH";//

    final static String ACK_FT_KEEPALIVE = "FK";//
    final static String ACK_FW = "FW";// # TODO
    final static String ACK_FILE_DOWNLOAD = "FD";//

    final static String ACK_LAMBDA_SET_STATUS = "P61:";//
    final static String ACK_LAMBDA_ASK_STATUS = "P63:";//
    final static String ACK_LAMBDA_COMMAND = "P64:";//
    final static String ACK_LAMBDA_MESSAGE = "P82:";// # PR?

    final static int LAMBDA_PROP_DEVICE_CONNECTED = 58;// # 0x3a
    final static int LAMBDA_PROP_GET_STATUS = 61;//
    final static int LAMBDA_PROP_ASK_STATUS = 63;//
    final static int LAMBDA_PROP_COMMAND = 64;//
    final static int LAMBDA_PROP_MESSAGE = 82;//
    final static int LAMBDA_PROP_SAVE_MAP = 80;// # internal

    final static int LAMBDA_CMD_ASK_ED = 0;//
    final static int LAMBDA_CMD_ASK_MAP = 1;//
    final static int LAMBDA_CMD_ASK_KEYFRAME_SYNC = 2;//
    final static int LAMBDA_CMD_RESET_MAP = 3;//

    final static String ACK_ERROR_CODE = "DEC";// # DEC?
    final static String ACK_NA = "NA";// # resends device info?
    final static String ACK_MAP_STATUS = "MS";//

    final static int ERROR_NO_CAMERA = 1100;//
    final static int ERROR_CAMERA_SSR_1 = 1121;//
    final static int ERROR_CAMERA_SSR_2 = 1122;//
    final static int ERROR_NO_IMU = 1200;//
    final static int ERROR_NO_POSE = 1300;//

    final static int LAMBDA_MESSAGE_ERROR = 0;//
    final static int LAMBDA_MESSAGE_UPDATE_MAP_UUID = 1;//

}
