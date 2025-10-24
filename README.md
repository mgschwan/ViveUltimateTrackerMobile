Update October 24th 2025: Since the app can be installed on the Samsung Galaxy XR without the need for sideloading I will be populating this repository with the source code of the app soon, so that more people can work on it.

# Vive Ultimate Tracker Mobile App

Mobile app to use the Vive Ultimate Trackers without a PC

## Disclaimer

**This is experimental software, use at your own risk.**

## Installation

Download the apk [htctrackerdongle.apk](https://github.com/mgschwan/ViveUltimateTrackerMobile/releases/download/alpha/htctrackerdongle.apk)

Install via ADB:
```
adb install htctrackerdongle.apk
```

## Usage with phone

1. Connect the USB dongle to your phone
2. Open the Tracker app
3. Accept USB permissions
4. Enter the OSC target host and port and make sure the OSC switch is enabled
5. Click connect and wait until the status log shows that it's connected
6. Go to the Tracker tab
7. Connect your master tracker by holding the connect button on the tracker for a moment
8. Wait until it shows as connected
9. Move your tracker around until it has found it's position, this will be indicated if it shows **Pos+Rot** in the log
10. Connect the other trackers by going back to 5.
11. Adjust the toggles in the tracker tab as needed to match the coordinate system with your software.
  * Flip/Regular will either flip a coordinate axis or leave it unchanged
  * Inv Rot/No Rot will invert the rotation around an axis or leave it unchanged

## Usage with Meta Quest standalone

The process is the same as with the phone but the OSC target address will be **127.0.0.1** because you will be sending the OSC messages locally 

## Important Notes & Troubleshooting

*   **Error Recovery:** The current version cannot recover from errors. If something goes wrong, disconnect the dongle, exit the app, reconnect the dongle, and start the app again.
*   **OSC Parameters:** If you are using OSC, set the OSC parameters **before** clicking connect.
*   **Master Tracker Connection:** Always connect the master tracker first. The map of the dongles that are connected later will be overwritten.
*   **Mapping Features:** Do not use the mapping features in the app for now. Ensure the mapping is already properly set up before using the app (either using the official HTC PCVR software or an HTC headset).
*   **Quest On-Device Usage:** When running on the Quest directly, make sure to **not record videos**. This will interfere with the USB communication, and the dongles will not connect properly.
