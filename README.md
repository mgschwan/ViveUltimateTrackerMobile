# Vive Ultimate Tracker Mobile App

Mobile app to use the Vive Ultimate Trackers without a PC

## Disclaimer

**This is experimental software, use at your own risk.**

## Installation

Download the apk [htctrackerdongle.apk](https://github.com/mgschwan/ViveUltimateTrackerMobile/raw/refs/heads/main/binary/htctrackerdongle.apk)

Install via ADB:
```
adb install htctrackerdongle.apk
```

## Important Notes & Troubleshooting

*   **Error Recovery:** The current version cannot recover from errors. If something goes wrong, disconnect the dongle, exit the app, reconnect the dongle, and start the app again.
*   **OSC Parameters:** If you are using OSC, set the OSC parameters **before** clicking connect.
*   **Master Tracker Connection:** Always connect the master tracker first. The map of the dongles that are connected later will be overwritten.
*   **Mapping Features:** Do not use the mapping features in the app for now. Ensure the mapping is already properly set up before using the app (either using the official HTC PCVR software or an HTC headset).
*   **Quest On-Device Usage:** When running on the Quest directly, make sure to **not record videos**. This will interfere with the USB communication, and the dongles will not connect properly.
