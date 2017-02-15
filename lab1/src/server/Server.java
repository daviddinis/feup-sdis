package server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.sql.ClientInfoStatus;
import java.util.HashMap;

/**
 * Created by epassos on 2/14/17.
 */
public class Server {

    private int portNumber;
    private DatagramSocket socket;

    private int destinationPort;
    private InetAddress destinationAddress;

    private HashMap<String,String> dataBase;

    Server(int port)throws IOException {
        this.portNumber = port;

        this.socket = new DatagramSocket(portNumber);
        this.dataBase = new HashMap();
    }

    public void running()throws IOException {

        String clientReply;
        DatagramPacket packet;
        byte[] buf;

        while(true){
            buf = new byte[255];
            packet = new DatagramPacket(buf, buf.length);

            this.socket.receive(packet);
            clientReply = processResponse(packet);
            sendResponse(clientReply);
        }
    }

    public String processResponse(DatagramPacket packet) throws UnsupportedEncodingException {
        System.out.println("New Request");

        String ret = "ERROR";

        byte[] buf = packet.getData();
        String str = new String(buf,"UTF-8");
        str = str.trim();
        String[] results = str.split(":");


        if(results[0].equals("register")){

            //TODO verificar se já existe uma key , se existir enviar um erro ao cliente
            dataBase.put(results[1],results[2]);
            ret = Integer.toString(dataBase.size());

        } else if(results[0].equals("lookup")){

            String getResult = dataBase.get(results[1]);
            if(getResult == null){
                ret = "ERROR";
            }
            else {
                ret = getResult;
            }
        }

        destinationPort = packet.getPort();
        destinationAddress = packet.getAddress();

        System.out.println(str);

        return ret;
    }

    public void sendResponse(String clientResponse) throws IOException {

        System.out.println("Replying: " + clientResponse);

        byte[] buf = clientResponse.getBytes();

        DatagramPacket packet = new DatagramPacket(buf, buf.length, destinationAddress, destinationPort);

        socket.send(packet);

    }
}
