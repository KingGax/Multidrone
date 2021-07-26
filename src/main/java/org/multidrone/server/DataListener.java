package org.multidrone.server;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import com.MAVLink.Messages.MAVLinkMessage;
import com.MAVLink.MAVLinkPacket;
import com.MAVLink.Parser;

import com.MAVLink.common.*;

import com.MAVLink.enums.MAV_RESULT;

import org.multidrone.sharedclasses.UserDroneData;

public class DataListener implements Runnable {

    private Parser mavParser = new Parser();
    private static final int MAVLINK_MSG_ID_BATTERY_STATUS = 147;
    private int notificationsPort = 32323;

    private int myPort;
    private int myID;
    private String delimiter = ";";


    @Override
    public void run() {
        try {

            this.initialize();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void setMyPort(int _myPort) {
        this.myPort = _myPort;
    }

    public int getMyPort() {
        return myPort;
    }

    public int getMyID() {
        return myID;
    }

    public void setMyID(int myID) {
        this.myID = myID;
    }

    private MAVLinkPacket decodePacket(byte[] buffer, int payloadSize) {
        MAVLinkPacket pack = new MAVLinkPacket(payloadSize);
        pack.seq = Byte.toUnsignedInt(buffer[2]);
        pack.sysid = Byte.toUnsignedInt(buffer[3]);
        pack.compid = Byte.toUnsignedInt(buffer[4]);
        pack.msgid = Byte.toUnsignedInt(buffer[5]);
        //buffer[0] = (byte) MAVLINK_STX;
        //buffer[1] = (byte) len;
        //buffer[2] = (byte) seq;
        //buffer[3] = (byte) sysid;
        //buffer[4] = (byte) compid;
        //buffer[5] = (byte) msgid;

        for (int j = 0; j < payloadSize; j++) {
            pack.payload.payload.put(buffer[6 + j]);
        }
        return pack;
        //generateCRC();
        //buffer[i++] = (byte) (crc.getLSB());
        //buffer[i++] = (byte) (crc.getMSB());
        //return buffer;
    }

    public void initialize() throws Exception {
        DatagramSocket socket = new DatagramSocket(myPort);
        System.out.println("Data listener started on port " + this.myPort);
        MAVLinkPacket currentPack = null;
        while (true) {

            byte[] buffer = new byte[1500]; // MTU = 1500 bytes
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            //System.out.println("Waiting on port " + getMyPort() + " id: " + getMyID());
            socket.receive(packet);
            //System.out.println("Received data");
            int recvSize = packet.getLength();
            byte[] myObject = new byte[recvSize];

            for (int i = 0; i < recvSize; i++) {
                myObject[i] = buffer[i];
            }

            try {
                int payloadSize = buffer[1];
                currentPack = decodePacket(buffer, payloadSize);

                //ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(myObject));
                //MAVLinkPacket data = (MAVLinkPacket) iStream.readObject();
                //iStream.close();
                if (currentPack != null) {
                    this.handleMavMessage(currentPack);
                } else {
                    System.out.println("Mav packet parsing failed");
                }

            } catch (Exception e) {
                e.printStackTrace();
                System.out.println("Mav packet deserialisation failed");
                try {

                    ObjectInputStream iStream = new ObjectInputStream(new ByteArrayInputStream(myObject));
                    UserDroneData data = (UserDroneData) iStream.readObject();
                    iStream.close();
                    this.handleMessage(data);
                } catch (Exception e2) {
                    System.out.println("User data deserialisation failed");
                }
            }
        }
    }


    private void handleMessage(UserDroneData data) throws Exception {
        ServerController controller = ServerController.getInstance();
        controller.handleData(data);
    }

    private void handleMavMessage(MAVLinkPacket packet) throws Exception {
        ServerController controller = ServerController.getInstance();
        MAVLinkMessage message = packet.unpack();


        switch (message.msgid) {
            case MAVLINK_MSG_ID_BATTERY_STATUS:
                msg_battery_status batMsg = (msg_battery_status) message;
                controller.updateBattery(batMsg.battery_remaining, myID);
                break;
            case msg_global_position_int.MAVLINK_MSG_ID_GLOBAL_POSITION_INT:
                msg_global_position_int posMsg = (msg_global_position_int) message;
                controller.receivePosInt(posMsg, myID);
                break;
            case msg_heartbeat.MAVLINK_MSG_ID_HEARTBEAT:
                break;
            case msg_vfr_hud.MAVLINK_MSG_ID_VFR_HUD:
                msg_vfr_hud vfrMsg = (msg_vfr_hud) message;
                controller.updateAlt(vfrMsg.alt, myID);
                break;
            case msg_sys_status.MAVLINK_MSG_ID_SYS_STATUS:
                msg_sys_status sysMsg = (msg_sys_status) message;
                controller.updateBattery(sysMsg.battery_remaining, myID);
                break;
            case msg_command_ack.MAVLINK_MSG_ID_COMMAND_ACK:
                msg_command_ack ackMsg = (msg_command_ack) message;
                System.out.println("Recieved ack for command ID " + ackMsg.command + " from user " + myID + " result: " + resultToString(ackMsg.result));
                break;
            case msg_attitude.MAVLINK_MSG_ID_ATTITUDE:
                msg_attitude attMsg = (msg_attitude) message;
                controller.updateAttitude(attMsg, myID);
                break;
            case msg_named_value_int.MAVLINK_MSG_ID_NAMED_VALUE_INT:
                msg_named_value_int valMsg = (msg_named_value_int)message;
                switch (valMsg.name[0]){
                    case 'R':
                      controller.updateRCBattery(valMsg.value,myID);
                    break;
                }
                break;
            case msg_gps_raw_int.MAVLINK_MSG_ID_GPS_RAW_INT:
                msg_gps_raw_int gpsMsg = (msg_gps_raw_int)message;
                controller.updateGPSStrength(gpsMsg.fix_type,myID);
                        break;
            default:
                break;

        }


    }

    private String resultToString(short result) {
        switch (result) {
            case MAV_RESULT.MAV_RESULT_ACCEPTED:
                return "accepted";
            case MAV_RESULT.MAV_RESULT_DENIED:
                return "denied";
            case MAV_RESULT.MAV_RESULT_UNSUPPORTED:
                return "command ignored - unknown";
            case MAV_RESULT.MAV_RESULT_IN_PROGRESS:
                return "command accepted and in progress";
            case MAV_RESULT.MAV_RESULT_ENUM_END:
                return "cancelled";
            case MAV_RESULT.MAV_RESULT_FAILED:
                return "execution failed, command accepted";
            case MAV_RESULT.MAV_RESULT_TEMPORARILY_REJECTED:
                return "cannot currently execute, try again";
            default:
                return "invalid result enum";
        }


    }

}