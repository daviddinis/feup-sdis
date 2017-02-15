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
    private int length;
    private byte[] buf;
    private DatagramSocket socket;
    private DatagramPacket packet;

    private int destinationPort;
    private InetAddress destinationAddress;

    private HashMap<String,String> dataBase;

    Server(int port)throws IOException {
        this.portNumber = port;

        this.length = 100;
        this.buf = new byte[length];

        this.socket = new DatagramSocket(portNumber);
        this.packet = new DatagramPacket(buf, length);

        this.dataBase = new HashMap();
    }

    public void running()throws IOException {

        /**
         * recv_request();
         * process_req();
         * send_resp();
         */

        String clientReply = new String();

        while(true){
            this.socket.receive(this.packet);
            clientReply = processResponse(this.packet);
            sendResponse(clientReply);
        }
    }

    public String processResponse(DatagramPacket packet) throws UnsupportedEncodingException {
        System.out.println("New Request");

        String ret = new String();

        byte[] buf = packet.getData();
        String str = new String(buf,"UTF-8");
        str.trim();
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

        packet = new DatagramPacket(buf, buf.length, destinationAddress, destinationPort);

        socket.send(packet);

    }
}
