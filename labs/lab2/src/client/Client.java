package client;

import server.Server;

import javax.xml.crypto.Data;
import java.io.IOException;
import java.net.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by epassos on 2/20/17.
 */
public class Client {
    private static final String plateRegex = "^([0-9A-Za-z])([0-9A-Za-z])-([0-9A-Za-z])([0-9A-Za-z])-([0-9A-Za-z])([0-9A-Za-z])$";
    public static int port = 8085;

    public static void main(String args[]) throws IOException {
        if(args.length < 4){
            System.out.println("Invalid number of arguments");
            System.out.println("Usage:");
            System.out.println("Client <mcast_addr> <mcast_port> <oper> <opnd>*");
            System.exit(1);
        }

        DatagramSocket socket = new DatagramSocket(port);
        socket.setSoTimeout(2500);
        String mcastAddr = args[0];
        String mcastPort = args[1];
        String operation = args[2];
        String plate = args[3];

        Pattern platePattern = Pattern.compile(plateRegex);
        Matcher plateMatcher = platePattern.matcher(plate);
        if(!plateMatcher.find()){
            System.out.println("Invalid format for license plate");
            System.exit(1);
        }


        DatagramPacket mcastPacket = getSvInfo(mcastAddr, Integer.parseInt(mcastPort));
        InetAddress svAddr = mcastPacket.getAddress();
        int svPort = Integer.parseInt(new String(mcastPacket.getData()).trim());

        if(operation.equalsIgnoreCase("register")){
            String name = args[4];
            for(int i = 3; i < args.length; i++){
                name += ' ' + args[i];

                if(name.length() > 256) {
                    name = name.substring(0, 256);
                    break;
                }
            }

            sendRegisterRequest(plate, name, socket, svAddr, svPort);
        }

        else if(operation.equalsIgnoreCase("lookup"))
            sendLookupRequest(plate, socket, svAddr, svPort);

        else{
            System.err.println("Invalid operation.");
            System.exit(1);
        }

        String resp = receiveResponse(socket, svAddr, svPort);

        System.out.println(resp);
        socket.close();
    }

    private static int sendRegisterRequest(String plate, String name, DatagramSocket socket, InetAddress svAddr, int svPort) throws IOException {
        String req = "register" + ';' + plate + ';' + name;
        byte[] buf = req.getBytes();
        DatagramPacket reqPacket = new DatagramPacket(buf,buf.length,svAddr,svPort);
        try {
            socket.send(reqPacket);
        }catch (IOException e){
            throw new IOException("Error sending register request");
        }
        return 0;
    }

    private static int sendLookupRequest(String plate, DatagramSocket socket, InetAddress svAddr, int svPort) throws  IOException {
        String req = "lookup" + ';' + plate;
        System.out.println(plate);
        byte[] buf = req.getBytes();
        DatagramPacket reqPacket = new DatagramPacket(buf, buf.length, svAddr, svPort);
        try {
            socket.send(reqPacket);
        }catch (IOException e){
            throw new IOException("Error sending lookup request");
        }
        return 0;
    }

    private static String receiveResponse(DatagramSocket socket, InetAddress svAddr, int svPort) throws IOException {
        byte[] buf = new byte[512];
        DatagramPacket packet = new DatagramPacket(buf,buf.length,svAddr,svPort);
        socket.receive(packet);
        return new String(packet.getData());
    }

    private static DatagramPacket getSvInfo(String mcastAddr, int mcastPort) throws IOException {
        MulticastSocket mcastSocket = new MulticastSocket(mcastPort);
        mcastSocket.setSoTimeout(5000);
        mcastSocket.joinGroup(InetAddress.getByName(mcastAddr));

        byte[] buf = new byte[32];
        DatagramPacket packet = new DatagramPacket(buf, buf.length);

        try {
            mcastSocket.receive(packet);
        } catch(IOException e){
            System.err.println("Multicast Socket Timed Out!");
            return null;
        }

        mcastSocket.leaveGroup(InetAddress.getByName(mcastAddr));
        mcastSocket.close();
        return packet;
    }
}

