/* AUTO-GENERATED FILE.  DO NOT MODIFY.
 *
 * This class was automatically generated by the
 * java mavlink generator tool. It should not be modified by hand.
 */

// MESSAGE MISSION_WRITE_PARTIAL_LIST PACKING
package com.MAVLink.common;

import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.Messages.MAVLinkPayload;

/**
 * This message is sent to the MAV to write a partial list. If start index == end index, only one item will be transmitted / updated. If the start index is NOT 0 and above the current list size, this request should be REJECTED!
 */
public class msg_mission_write_partial_list extends MAVLinkMessage {

    public static final int MAVLINK_MSG_ID_MISSION_WRITE_PARTIAL_LIST = 38;
    public static final int MAVLINK_MSG_LENGTH = 7;
    private static final long serialVersionUID = MAVLINK_MSG_ID_MISSION_WRITE_PARTIAL_LIST;


    /**
     * Start index. Must be smaller / equal to the largest index of the current onboard list.
     */
    public short start_index;

    /**
     * End index, equal or greater than start index.
     */
    public short end_index;

    /**
     * System ID
     */
    public short target_system;

    /**
     * Component ID
     */
    public short target_component;

    /**
     * Mission type.
     */
    public short mission_type;


    /**
     * Generates the payload for a mavlink message for a message of this type
     *
     * @return
     */
    public MAVLinkPacket pack() {
        MAVLinkPacket packet = new MAVLinkPacket(MAVLINK_MSG_LENGTH);
        packet.sysid = 255;
        packet.compid = 190;
        packet.msgid = MAVLINK_MSG_ID_MISSION_WRITE_PARTIAL_LIST;

        packet.payload.putShort(start_index);

        packet.payload.putShort(end_index);

        packet.payload.putUnsignedByte(target_system);

        packet.payload.putUnsignedByte(target_component);

        packet.payload.putUnsignedByte(mission_type);

        return packet;
    }

    /**
     * Decode a mission_write_partial_list message into this class fields
     *
     * @param payload The message to decode
     */
    public void unpack(MAVLinkPayload payload) {
        payload.resetIndex();

        this.start_index = payload.getShort();

        this.end_index = payload.getShort();

        this.target_system = payload.getUnsignedByte();

        this.target_component = payload.getUnsignedByte();

        this.mission_type = payload.getUnsignedByte();

    }

    /**
     * Constructor for a new message, just initializes the msgid
     */
    public msg_mission_write_partial_list() {
        msgid = MAVLINK_MSG_ID_MISSION_WRITE_PARTIAL_LIST;
    }

    /**
     * Constructor for a new message, initializes the message with the payload
     * from a mavlink packet
     */
    public msg_mission_write_partial_list(MAVLinkPacket mavLinkPacket) {
        this.sysid = mavLinkPacket.sysid;
        this.compid = mavLinkPacket.compid;
        this.msgid = MAVLINK_MSG_ID_MISSION_WRITE_PARTIAL_LIST;
        unpack(mavLinkPacket.payload);
    }


    /**
     * Returns a string with the MSG name and data
     */
    public String toString() {
        return "MAVLINK_MSG_ID_MISSION_WRITE_PARTIAL_LIST - sysid:" + sysid + " compid:" + compid + " start_index:" + start_index + " end_index:" + end_index + " target_system:" + target_system + " target_component:" + target_component + " mission_type:" + mission_type + "";
    }
}
        