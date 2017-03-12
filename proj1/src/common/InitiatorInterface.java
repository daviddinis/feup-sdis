package common;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface InitiatorInterface extends Remote {

    /**
     * Function used by the user to back-up a determined file
     * @param pathname
     * @param replicationDegree
     * @throws RemoteException
     */
    void backup(String pathname, int replicationDegree) throws IOException;

    /**
     * Function used by the user to restore a determined file
     * @param pathname
     * @throws RemoteException
     */
    void restore(String pathname) throws RemoteException;

    /**
     * Function used by the user to delete a determined file
     * @param pathname
     * @throws RemoteException
     */
    void delete(String pathname) throws  RemoteException;

    /**
     * Function used by the user to tell to a determined peer the maximum disk space used for
     * storing chunks
     * @param maxDiskSpace
     * @throws RemoteException
     */
    void reclaim(int maxDiskSpace) throws RemoteException;

    /**
     * Function used by the user to observe the service state
     * @throws RemoteException
     */
    void state() throws RemoteException;
}
