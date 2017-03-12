package peers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

/**
 * Created by nuno on 12-03-2017.
 */
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
                DatagramPacket mcastPacket = new DatagramPacket(buf, buf.length);

                System.out.println("Starting reading messages");

                try {
                    socket.receive(mcastPacket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(task).start();
    }

    public void sendMessage(){

    }

}
