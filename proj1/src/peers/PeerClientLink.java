package peers;

import common.InitiatorInterface;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class PeerClientLink extends UnicastRemoteObject implements InitiatorInterface {

    private PeerService peer;

    public PeerClientLink(PeerService peer) throws RemoteException {
        this.peer = peer;

    }

    @Override
    public void backup(String filepath, int replicationDegree) throws IOException {
        if(filepath == null || replicationDegree < 1){
            throw new IllegalArgumentException("Invalid arguments for backup");
        }

        FileInputStream file;
        try{
           file = new FileInputStream(filepath);
        } catch (FileNotFoundException e){
            throw e;
        }
        System.out.println("New backup request for file " + filepath);

        while(file.available() > 0){
            byte[] chunk = new byte[PeerService.CHUNK_SIZE];
            file.read(chunk);
            System.out.print(chunk);
        }
//
//        //Add for
//        byte[] chunk = "so para nao dar erro".getBytes();


//        peer.requestChunkBackup(filepath,Integer.toString(replicationDegree),chunk);
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
