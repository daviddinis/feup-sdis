package peers;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class PeerChannel {

    private final InetAddress addr;
    private final int port;
    private final PeerService peer;
    private MulticastSocket socket;

    /**
     * Peer Channel, responsible for the communication between peers
     *
     * @param addr address
     * @param port port
     * @param peer peer this channel belongs to
     * @throws IOException
     */
    PeerChannel(InetAddress addr, int port, PeerService peer) throws IOException {
        this.addr = addr;
        this.port = port;
        this.peer = peer;

        socket = new MulticastSocket(this.port);

        socket.joinGroup(this.addr);
    }

    /**
     * Listens to the channel and waits for a packet
     * Launches a thread to treat it
     */
    void receiveMessage() {

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

    /**
     * Sends a message given by the peer
     *
     * @param message message to send
     * @return
     */
    boolean sendMessage(byte[] message) {

        DatagramPacket packet = new DatagramPacket(message, message.length, addr, port);

        try {
            socket.send(packet);
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    /**
     * Calls the peer's message handler to treat a message
     *
     * @param packet received packet
     */
    private void channelMessageHandler(DatagramPacket packet) {
        byte[] buffer = Arrays.copyOfRange(packet.getData(), 0, packet.getLength());

        Runnable task = () -> peer.messageHandler(buffer, packet.getAddress());

        ExecutorService service = Executors.newFixedThreadPool(10);

        service.execute(task);

    }
}
