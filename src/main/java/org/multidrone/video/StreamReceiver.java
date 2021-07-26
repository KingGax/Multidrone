package org.multidrone.video;

import com.MAVLink.MAVLinkPacket;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class StreamReceiver implements Runnable {
    @Override
    public void run() {
        try {

            this.initialize();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    private void initialize() throws Exception {
        DatagramSocket udpSocket = new DatagramSocket(50003);
        //System.out.println("Video listener started on port " + socket.getLocalPort());

        ByteBuffer buf = ByteBuffer.allocate(30000);
        int recvcount = 0;

        ServerSocket serverSocket = new ServerSocket(4444);




        try {
            serverSocket = new ServerSocket(44444);
            System.out.println("setup video listener on port " + serverSocket.getLocalPort());
        } catch (IOException ex) {
            System.out.println("Can't setup server on this port number. ");
        }

        Socket socket = null;
        InputStream in = null;
        OutputStream out = null;

        try {
            System.out.println("waiting for video client. ");
            socket = serverSocket.accept();
            System.out.println("client accepted! ");
        } catch (IOException ex) {
            System.out.println("Can't accept client connection. ");
        }

        try {
            in = socket.getInputStream();
        } catch (IOException ex) {
            System.out.println("Can't get socket input stream. ");
        }

        byte[] bytes = new byte[250000];
        byte[] buffer = new byte[1024 * 8];


        //out.close();
        //in.close();
        //socket.close();
        //serverSocket.close();

        OutputStream socketOutputStream;


        System.out.println("video waiting");
        while (true) {

            out = new ByteArrayOutputStream(250000);



            in = socket.getInputStream();
            int count;
            int dataSize = 0;
            byte[] sizeBytes = in.readNBytes(4);
            int promisedSize = (((sizeBytes[0] << 24)&0xFF000000) | ((sizeBytes[1] << 16)&0xFF0000) | ((sizeBytes[2] << 8)&0xFF00) | ((sizeBytes[3])&0xFF ))& 0x0000FFFF;
            System.out.println("size to read: " + promisedSize);
            while ((count = in.read(buffer)) > 0) {
                dataSize += count;
                out.write(buffer, 0, count);
                System.out.println("reading " + count + " cursize: " + dataSize);
            }

            System.out.println(" received " + dataSize);
            in.close();
            System.out.println(" received " + dataSize);
            socketOutputStream = socket.getOutputStream();
            System.out.println(" received " + dataSize);

            socketOutputStream.write(1);
            System.out.println(" sent ack ");
            System.out.println("COUNT: " + recvcount);
            /*byte[] buffer = new byte[1500]; // MTU = 1500 bytes
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            //System.out.println("Waiting on port " + getMyPort() + " id: " + getMyID());
            socket.receive(packet);
            System.out.println("Received data " + packet.getLength() + " offset:" + packet.getOffset());
            //buf.put(packet.getData(),0,packet.getLength());
            //long timestep = buf.getLong(4);

            int fragment_type = packet.getData()[1] & 0x1F;
            int nal_type = packet.getData()[4] & 0x1F;
            int nri = (packet.getData()[4] & 0x60);
            int start_bit = packet.getData()[1] & 0x80;
            int end_bit = packet.getData()[1] & 0x40;
            int paytype = packet.getData()[1]&0xFF;
            //int type = packet.getData()[0] & 0x1F;
            int seq_num = (((packet.getData()[2] << 8)&0xFF00)|packet.getData()[3]&0xFF) & 0x0000FFFF;
            long timestamp = (((packet.getData()[4] << 24)&0xFF000000) | ((packet.getData()[5] << 16)&0xFF0000) | ((packet.getData()[6] << 8)&0xFF00) | ((packet.getData()[7])&0xFF ))& 0x0000FFFF;
            System.out.println("num " + seq_num + " timestamp: " + timestamp + " type:" + paytype);
            System.out.println("frag type " + fragment_type + " nal type:" + nal_type + " start bit " + start_bit + " end bit " + end_bit);
            int recvSize = packet.getLength();
            byte[] myObject = new byte[recvSize];
            count++;
            if (count %100 == 0){
                System.out.println("COUNT: " + count);
            }*/
        }
    }
}
