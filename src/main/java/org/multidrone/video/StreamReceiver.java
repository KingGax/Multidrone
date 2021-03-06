package org.multidrone.video;

import com.MAVLink.MAVLinkPacket;
import com.esri.arcgisruntime.internal.httpclient.util.ByteArrayBuffer;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.JCodecUtil2;
import org.jcodec.common.StringUtils;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.AWTUtil;
import org.multidrone.server.ServerController;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;

public class StreamReceiver implements Runnable {

    ServerSocket serverSocket;
    private int userID;
    boolean reopenSocket = true;

    public void setUserID(int id) {
        userID = id;
    }

    @Override
    public void run() {
        try {

            this.initialize();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public StreamReceiver(ServerSocket socket){
        serverSocket = socket;
    }



    private void initialize() throws Exception {

        int recvcount = 0;
        Socket socket = null;
        InputStream in = null;
        ByteArrayOutputStream out = null;

        out = new ByteArrayOutputStream(250_000);
        byte[] buffer = new byte[1024 * 64];

        OutputStream socketOutputStream;


        System.out.println("video waiting");
        while (true) {
            try{
                if (reopenSocket){
                    System.out.println("waiting for video client. id: " + userID );
                    socket = serverSocket.accept();
                    in = socket.getInputStream();
                    reopenSocket = false;
                    System.out.println("found video client " + userID);
                }
                int count;
                int dataSize = 0;
                //System.out.println("reading size");
                byte[] sizeBytes = in.readNBytes(4);
                int promisedSize = (((sizeBytes[0] << 24)&0xFF000000) | ((sizeBytes[1] << 16)&0xFF0000) | ((sizeBytes[2] << 8)&0xFF00) | ((sizeBytes[3])&0xFF ))& 0xFFFFFFFF;
                //System.out.println("size to read: " + promisedSize);
                while ((dataSize < promisedSize) && (count = in.read(buffer)) > 0) {

                    out.write(buffer, 0, count);
                    dataSize += count;
                }
            /*Frame f = frameGrabber.grab();

            if (f != null){
                BufferedImage bufferedImage = converter.convert(f);
                System.out.println("we have buffered image");
            }*/
                socketOutputStream = socket.getOutputStream();
                //System.out.println(" received " + dataSize);

                socketOutputStream.write(1);
                //System.out.println(" sent ack ");
                recvcount++;
                //System.out.println("COUNT: " + recvcount);

                /*for (int i = 0; i < 14; i++) {
                    String st = String.format("%02X ", out.toByteArray()[i]);
                    System.out.print(st);
                }
                System.out.println();*/
                decode(out.toByteArray());

                H264Decoder decoder = new H264Decoder();
                Picture pic = Picture.create(1088, 720, ColorSpace.YUV420); // Allocate output frame of max size
                ByteBuffer bb = ByteBuffer.wrap(out.toByteArray());
                //System.out.println("LEN " + out.toByteArray().length);
                Picture real = decoder.decodeFrame(bb, pic.getData());

                if (real!=null){
                    Image i = convertToFxImage(AWTUtil.toBufferedImage(pic));
                    ServerController.getInstance().setImage(userID,i);
                }
                out.reset();
            } catch (SocketException e){
                System.out.println("SOCKET EXCEPTION " + e.getMessage());
                e.printStackTrace();
                //System.out.println(socket.isConnected() + " " + socket.isClosed() + " " + serverSocket.isClosed() + " " + serverSocket.isBound());
                reopenSocket = true;
            } catch (Exception e){
                System.out.println("EXCEPTION" + e.getMessage());
                //System.out.println(socket.isConnected() + " " + socket.isClosed() + " " + serverSocket.isClosed() + " " + serverSocket.isBound());
                reopenSocket = true;
            }
            Thread.sleep(40);
        }
    }


    static int pos;
    static byte[] data;

    private static Image convertToFxImage(BufferedImage image) {
        WritableImage wr = null;
        if (image != null) {
            wr = new WritableImage(image.getWidth(), image.getHeight());
            PixelWriter pw = wr.getPixelWriter();
            for (int x = 0; x < image.getWidth(); x++) {
                for (int y = 0; y < image.getHeight(); y++) {
                    pw.setArgb(x, y, image.getRGB(x, y));
                }
            }
        }

        return new ImageView(wr).getImage();
    }

    private static void decode(byte[] _data) {
        try {
            System.out.println();
            //System.out.println(StringUtils.toHex(data));
            //System.out.println(StringUtils.toBin(data, 0, data.length, true));
            System.out.println();
            pos = 0;
            data = _data;

            int forbidden_zero_bit = getU(1);
            //System.out.println("forbidden_zero_bit " + forbidden_zero_bit);
            int nal_ref_idc = getU(2);
            int nal_unit_type = getU(5);
            //System.out.println("nal_unit_type (should be 7 for SPS) " + nal_unit_type);
            //END of NAL_header

            //Start of SPS data
            int profile_idc = getU(8);
            int constraint_set0_flag = getU(1);
            int constraint_set1_flag = getU(1);
            int constraint_set2_flag = getU(1);
            int constraint_set3_flag = getU(1);
            int constraint_set4_flag = getU(1);
            int constraint_set5_flag = getU(1);
            //The current version of the spec states that there are two reserved bits
            int reserved_zero_2bits = getU(2);
            //System.out.println("reserved_zero_2bits" + reserved_zero_2bits);
            int level_idc = getU(8);
            int seq_parameter_set_id = uev();
            int log2_max_frame_num_minus4 = uev();
            int pict_order_cnt_type = uev();
            //System.out.println("pict_order_cnt_type=" + pict_order_cnt_type);
            if (pict_order_cnt_type == 0) {
                uev();
            } else if (pict_order_cnt_type == 1) {
                getU(1);
                sev();
                sev();
                int n = uev();
                //System.out.println("n*sev, n=" + n);
                for (int i = 0; i < n; i++)
                    sev();
            }
            int num_ref_frames = uev();
            getU(1);
            int pic_width = (uev() + 1) * 16;
            int pic_height = (uev() + 1) * 16;
            int frame_mbs_only_flag = getU(1);
            System.out.println(pic_width + " x " + pic_height);
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
    private static int ev(boolean signed) {
        int bitcount = 0;
        StringBuilder expGolomb = new StringBuilder();
        while (getBit() == 0) {
            expGolomb.append('0');
            bitcount++;
        }
        expGolomb.append("/1");
        int result = 1;
        for (int i = 0; i < bitcount; i++) {
            int b = getBit();
            expGolomb.append(b);
            result = result * 2 + b;
        }
        result--;
        if (signed) {
            result = (result + 1) / 2 * (result % 2 == 0 ? -1 : 1);
            //System.out.println("getSe(v) = " + (result) + " " + expGolomb);
        } else {
            //System.out.println("getUe(v) = " + (result) + " " + expGolomb);
        }
        return result;
    }

    private static int uev() {
        return ev(false);
    }

    private static int sev() {
        return ev(true);
    }

    private static int getU(int bits) {
        int result = 0;
        for (int i = 0; i < bits; i++) {
            result = result * 2 + getBit();
        }
        //System.out.println("getU(" + bits + ") = " + result);
        return result;
    }

    private static int getBit() {
        int mask = 1 << (7 - (pos & 7));
        int idx = pos >> 3;
        pos++;
        return ((data[idx] & mask) == 0) ? 0 : 1;
    }


    public int getPort() {
        return serverSocket.getLocalPort();
    }
}
