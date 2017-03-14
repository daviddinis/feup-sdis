package peers;

import common.InitiatorInterface;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class InitiatorPeer extends UnicastRemoteObject implements InitiatorInterface {

    private PeerService peer;

    public InitiatorPeer(PeerService peer) throws RemoteException {
        this.peer = peer;

    }

    @Override
    public void backup(String pathname, int replicationDegree) throws IOException {
        System.out.println("New backup request");
        
        //Add for
        byte[] chunk = "so para nao dar erro".getBytes();


        peer.requestChunkBackup(pathname,Integer.toString(replicationDegree),chunk);
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
