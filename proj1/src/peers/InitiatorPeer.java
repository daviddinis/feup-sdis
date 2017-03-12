package peers;

import common.InitiatorInterface;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * Created by nuno on 12-03-2017.
 */
public class InitiatorPeer extends UnicastRemoteObject implements InitiatorInterface {

    public InitiatorPeer() throws RemoteException {

    }

    @Override
    public void backup(String pathname, int replicationDegree) throws RemoteException {
        System.out.println("Parece estar a pintar");
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
