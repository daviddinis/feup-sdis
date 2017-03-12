package peers;

import common.InitiatorInterface;

import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class InitiatorPeer extends UnicastRemoteObject implements InitiatorInterface {

    private PeerChannel multiChannel;
    private PeerChannel multiDataBackUpChannel;
    private PeerChannel multiDataRestoreChannel;

    public InitiatorPeer(PeerChannel multiChannel,PeerChannel multiDataBackUpChannel,
                         PeerChannel multiDataRestoreChannel) throws RemoteException {

        this.multiChannel = multiChannel;
        this.multiDataBackUpChannel = multiDataBackUpChannel;
        this.multiDataRestoreChannel = multiDataRestoreChannel;

    }

    @Override
    public void backup(String pathname, int replicationDegree) throws IOException {
        System.out.println("New backup request");

        multiDataBackUpChannel.sendMessage(pathname);
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
