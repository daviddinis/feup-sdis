package peers;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerChannel {

    private InetAddress addr;
    private int port;

    private MulticastSocket socket;

    private PeerService peer;

    public PeerChannel(InetAddress addr, int port, PeerService peer) throws IOException {
        this.addr = addr;
        this.port = port;
        this.peer = peer;

        socket = new MulticastSocket(this.port);

        socket.joinGroup(this.addr);
    }

    public void receiveMessage() throws IOException {

        Runnable task = () -> {

            //noinspection InfiniteLoopStatement
            while (true) {

                byte[] buf = new byte[PeerService.CHUNK_SIZE + 100];
                DatagramPacket packet = new DatagramPacket(buf, buf.length);

                try {
                    socket.receive(packet);

                    channelMessageHandler(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(task).start();
    }

    public boolean sendMessage(byte[] message) {

        DatagramPacket packet = new DatagramPacket(message, message.length, addr, port);

        try {
            socket.send(packet);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    private void channelMessageHandler(DatagramPacket packet) throws UnsupportedEncodingException {
        byte[] buffer = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

        Runnable task = () -> {
            peer.messageHandler(buffer, packet.getLength());
        };

        ExecutorService service = Executors.newFixedThreadPool(30);

        service.execute(task);

        //new Thread(task).start();
    }
}
