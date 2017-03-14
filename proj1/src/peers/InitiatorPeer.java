package peers;

import common.InitiatorInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class InitiatorPeer extends UnicastRemoteObject implements InitiatorInterface {

    /////// Constants ////////
    public static final String CRLF = "\r\n";

    /////// Class variables ////////

    private PeerChannel multiChannel;
    private PeerChannel multiDataBackUpChannel;
    private PeerChannel multiDataRestoreChannel;

    private PeerService peer;

    public InitiatorPeer(PeerChannel multiChannel,PeerChannel multiDataBackUpChannel,
                         PeerChannel multiDataRestoreChannel, PeerService peer) throws RemoteException {

        this.multiChannel = multiChannel;
        this.multiDataBackUpChannel = multiDataBackUpChannel;
        this.multiDataRestoreChannel = multiDataRestoreChannel;

        this.peer = peer;

    }

    private String getHeader(String... fields) {

        String header = "";

        for(String field : fields){
            header = header.concat(field+" ");
        }

        header.concat(CRLF + CRLF);

        return header;
    }

    @Override
    public void backup(String pathname, int replicationDegree) throws IOException {
        System.out.println("New backup request");

        String repliDegree = Integer.toString(replicationDegree);

        String header = getHeader("PUTCHUNK",peer.getProtocolVersion(),pathname,
                "ChunkNO",repliDegree);

        System.out.println(header);

        byte[] buf = header.getBytes();

        multiDataBackUpChannel.sendMessage(buf);
    }

    @Override
    public void restore(String pathname) throws RemoteException {

    }

    @Override
    public void delete(String pathname) throws RemoteException {

    }

    @Override
    public void reclaim(int maxDiskSpace) throws RemoteException {

    }

    @Override
    public void state() throws RemoteException {

    }
}
