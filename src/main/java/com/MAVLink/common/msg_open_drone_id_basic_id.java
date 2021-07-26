/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE OPEN_DRONE_ID_BASIC_ID PACKING
package com.MAVLink.common;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPayload;

/**
 * Data for filling the OpenDroneID Basic ID message. This and the below messages are primarily meant for feeding data to/from an OpenDroneID implementation. E.g. https://github.com/opendroneid/opendroneid-core-c
 */
public class msg_open_drone_id_basic_id extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_OPEN_DRONE_ID_BASIC_ID = 12900;
    public static final int MAVLINK_MSG_LENGTH = 22;
    private static final long serialVersionUID = MAVLINK_MSG_ID_OPEN_DRONE_ID_BASIC_ID;


    /**
     * Indicates the format for the uas_id field of this message.
     */
    public short id_type;

    /**
     * Indicates the type of UA (Unmanned Aircraft).
     */
    public short ua_type;

    /**
     * UAS (Unmanned Aircraft System) ID following the format specified by id_type. Shall be filled with nulls in the unused portion of the field.
     */
    public short uas_id[] = new short[20];


    /**
     * Generates the payload for a mavlink message for a message of this type
     *
     * @return
     */
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH);
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_OPEN_DRONE_ID_BASIC_ID;

        packet.payload.putUnsignedByte(id_type);

        packet.payload.putUnsignedByte(ua_type);


        for (int i = 0; i < uas_id.length; i++) {
            packet.payload.putUnsignedByte(uas_id[i]);
        }


        return packet;
    }

    /**
     * Decode a open_drone_id_basic_id message into this class fields
     *
     * @param payload The message to decode
     */
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();

        this.id_type = payload.getUnsignedByte();

        this.ua_type = payload.getUnsignedByte();


        for (int i = 0; i < this.uas_id.length; i++) {
            this.uas_id[i] = payload.getUnsignedByte();
        }


    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_open_drone_id_basic_id() {
        msgid = MAVLINK_MSG_ID_OPEN_DRONE_ID_BASIC_ID;
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     */
    public msg_open_drone_id_basic_id(MAVLinkPacket mavLinkPacket) {
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.msgid = MAVLINK_MSG_ID_OPEN_DRONE_ID_BASIC_ID;
        unpack(mavLinkPacket.payload);
    }


    /**
     * Returns a string with the MSG name and data
     */
    public String toString() {
        return "MAVLINK_MSG_ID_OPEN_DRONE_ID_BASIC_ID - sysid:" + sysid + " compid:" + compid + " id_type:" + id_type + " ua_type:" + ua_type + " uas_id:" + uas_id + "";
    }
}
        