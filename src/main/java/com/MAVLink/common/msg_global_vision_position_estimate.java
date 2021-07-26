/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE GLOBAL_VISION_POSITION_ESTIMATE PACKING
package com.MAVLink.common;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPayload;

/**
 * Global position/attitude estimate from a vision source.
 */
public class msg_global_vision_position_estimate extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_GLOBAL_VISION_POSITION_ESTIMATE = 101;
    public static final int MAVLINK_MSG_LENGTH = 117;
    private static final long serialVersionUID = MAVLINK_MSG_ID_GLOBAL_VISION_POSITION_ESTIMATE;


    /**
     * Timestamp (UNIX time or since system boot)
     */
    public long usec;

    /**
     * Global X position
     */
    public float x;

    /**
     * Global Y position
     */
    public float y;

    /**
     * Global Z position
     */
    public float z;

    /**
     * Roll angle
     */
    public float roll;

    /**
     * Pitch angle
     */
    public float pitch;

    /**
     * Yaw angle
     */
    public float yaw;

    /**
     * Row-major representation of pose 6x6 cross-covariance matrix upper right triangle (states: x_global, y_global, z_global, roll, pitch, yaw; first six entries are the first ROW, next five entries are the second ROW, etc.). If unknown, assign NaN value to first element in the array.
     */
    public float covariance[] = new float[21];

    /**
     * Estimate reset counter. This should be incremented when the estimate resets in any of the dimensions (position, velocity, attitude, angular speed). This is designed to be used when e.g an external SLAM system detects a loop-closure and the estimate jumps.
     */
    public short reset_counter;


    /**
     * Generates the payload for a mavlink message for a message of this type
     *
     * @return
     */
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH);
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_GLOBAL_VISION_POSITION_ESTIMATE;

        packet.payload.putUnsignedLong(usec);

        packet.payload.putFloat(x);

        packet.payload.putFloat(y);

        packet.payload.putFloat(z);

        packet.payload.putFloat(roll);

        packet.payload.putFloat(pitch);

        packet.payload.putFloat(yaw);


        for (int i = 0; i < covariance.length; i++) {
            packet.payload.putFloat(covariance[i]);
        }


        packet.payload.putUnsignedByte(reset_counter);

        return packet;
    }

    /**
     * Decode a global_vision_position_estimate message into this class fields
     *
     * @param payload The message to decode
     */
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();

        this.usec = payload.getUnsignedLong();

        this.x = payload.getFloat();

        this.y = payload.getFloat();

        this.z = payload.getFloat();

        this.roll = payload.getFloat();

        this.pitch = payload.getFloat();

        this.yaw = payload.getFloat();


        for (int i = 0; i < this.covariance.length; i++) {
            this.covariance[i] = payload.getFloat();
        }


        this.reset_counter = payload.getUnsignedByte();

    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_global_vision_position_estimate() {
        msgid = MAVLINK_MSG_ID_GLOBAL_VISION_POSITION_ESTIMATE;
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     */
    public msg_global_vision_position_estimate(MAVLinkPacket mavLinkPacket) {
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.msgid = MAVLINK_MSG_ID_GLOBAL_VISION_POSITION_ESTIMATE;
        unpack(mavLinkPacket.payload);
    }


    /**
     * Returns a string with the MSG name and data
     */
    public String toString() {
        return "MAVLINK_MSG_ID_GLOBAL_VISION_POSITION_ESTIMATE - sysid:" + sysid + " compid:" + compid + " usec:" + usec + " x:" + x + " y:" + y + " z:" + z + " roll:" + roll + " pitch:" + pitch + " yaw:" + yaw + " covariance:" + covariance + " reset_counter:" + reset_counter + "";
    }
}
        