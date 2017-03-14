package peers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class PeerChannel {

    private InetAddress addr;
    private int port;

    private MulticastSocket socket;

    public PeerChannel(InetAddress addr, int port) throws IOException {
        this.addr = addr;
        this.port = port;

        socket = new MulticastSocket(this.port);

        socket.joinGroup(this.addr);
    }

    public void receiveMessage() throws IOException {

        Runnable task = () -> {

            while (true) {

                byte[] buf = new byte[500];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                System.out.println("Starting reading messages");

                try {
                    socket.receive(packet);
                    processMessage(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(task).start();
    }

    public void sendMessage(byte[] message) throws IOException {

        DatagramPacket packet = new DatagramPacket(message,message.length,addr,port);

        socket.send(packet);

    }

    public void processMessage(DatagramPacket packet) throws UnsupportedEncodingException {
        byte[] buffer = packet.getData();

        String str = new String(buffer, 0, packet.getLength());

        System.out.println(str);
        System.out.println(str.length());
    }

}
