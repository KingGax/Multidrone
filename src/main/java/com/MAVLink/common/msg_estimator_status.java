/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE ESTIMATOR_STATUS PACKING
package com.MAVLink.common;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPayload;

/**
 * Estimator status message including flags, innovation test ratios and estimated accuracies. The flags message is an integer bitmask containing information on which EKF outputs are valid. See the ESTIMATOR_STATUS_FLAGS enum definition for further information. The innovation test ratios show the magnitude of the sensor innovation divided by the innovation check threshold. Under normal operation the innovation test ratios should be below 0.5 with occasional values up to 1.0. Values greater than 1.0 should be rare under normal operation and indicate that a measurement has been rejected by the filter. The user should be notified if an innovation test ratio greater than 1.0 is recorded. Notifications for values in the range between 0.5 and 1.0 should be optional and controllable by the user.
 */
public class msg_estimator_status extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_ESTIMATOR_STATUS = 230;
    public static final int MAVLINK_MSG_LENGTH = 42;
    private static final long serialVersionUID = MAVLINK_MSG_ID_ESTIMATOR_STATUS;


    /**
     * Timestamp (UNIX Epoch time or time since system boot). The receiving end can infer timestamp format (since 1.1.1970 or since system boot) by checking for the magnitude the number.
     */
    public long time_usec;

    /**
     * Velocity innovation test ratio
     */
    public float vel_ratio;

    /**
     * Horizontal position innovation test ratio
     */
    public float pos_horiz_ratio;

    /**
     * Vertical position innovation test ratio
     */
    public float pos_vert_ratio;

    /**
     * Magnetometer innovation test ratio
     */
    public float mag_ratio;

    /**
     * Height above terrain innovation test ratio
     */
    public float hagl_ratio;

    /**
     * True airspeed innovation test ratio
     */
    public float tas_ratio;

    /**
     * Horizontal position 1-STD accuracy relative to the EKF local origin
     */
    public float pos_horiz_accuracy;

    /**
     * Vertical position 1-STD accuracy relative to the EKF local origin
     */
    public float pos_vert_accuracy;

    /**
     * Bitmap indicating which EKF outputs are valid.
     */
    public int flags;


    /**
     * Generates the payload for a mavlink message for a message of this type
     *
     * @return
     */
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH);
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_ESTIMATOR_STATUS;

        packet.payload.putUnsignedLong(time_usec);

        packet.payload.putFloat(vel_ratio);

        packet.payload.putFloat(pos_horiz_ratio);

        packet.payload.putFloat(pos_vert_ratio);

        packet.payload.putFloat(mag_ratio);

        packet.payload.putFloat(hagl_ratio);

        packet.payload.putFloat(tas_ratio);

        packet.payload.putFloat(pos_horiz_accuracy);

        packet.payload.putFloat(pos_vert_accuracy);

        packet.payload.putUnsignedShort(flags);

        return packet;
    }

    /**
     * Decode a estimator_status message into this class fields
     *
     * @param payload The message to decode
     */
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();

        this.time_usec = payload.getUnsignedLong();

        this.vel_ratio = payload.getFloat();

        this.pos_horiz_ratio = payload.getFloat();

        this.pos_vert_ratio = payload.getFloat();

        this.mag_ratio = payload.getFloat();

        this.hagl_ratio = payload.getFloat();

        this.tas_ratio = payload.getFloat();

        this.pos_horiz_accuracy = payload.getFloat();

        this.pos_vert_accuracy = payload.getFloat();

        this.flags = payload.getUnsignedShort();

    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_estimator_status() {
        msgid = MAVLINK_MSG_ID_ESTIMATOR_STATUS;
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     */
    public msg_estimator_status(MAVLinkPacket mavLinkPacket) {
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.msgid = MAVLINK_MSG_ID_ESTIMATOR_STATUS;
        unpack(mavLinkPacket.payload);
    }


    /**
     * Returns a string with the MSG name and data
     */
    public String toString() {
        return "MAVLINK_MSG_ID_ESTIMATOR_STATUS - sysid:" + sysid + " compid:" + compid + " time_usec:" + time_usec + " vel_ratio:" + vel_ratio + " pos_horiz_ratio:" + pos_horiz_ratio + " pos_vert_ratio:" + pos_vert_ratio + " mag_ratio:" + mag_ratio + " hagl_ratio:" + hagl_ratio + " tas_ratio:" + tas_ratio + " pos_horiz_accuracy:" + pos_horiz_accuracy + " pos_vert_accuracy:" + pos_vert_accuracy + " flags:" + flags + "";
    }
}
        