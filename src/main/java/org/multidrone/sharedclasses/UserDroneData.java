package org.multidrone.sharedclasses;

import com.MAVLink.enums.GPS_FIX_TYPE;

public class UserDroneData implements java.io.Serializable {
    public float height;
    public float batteryPercent;
    public float alt;
    public float lat;
    public float lng;
    public float yaw;

    public float xNED;
    public float yNED;
    public float zNED;

    public float xBOD;
    public float yBOD;
    public float zBOD;

    public float vx;
    public float vy;
    public float vz;

    public short gpsSig = 235;//unknown initially

    public int rcBatteryPercentage;

    public int id;
}
