package peers;

import common.InitiatorInterface;

import javax.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;

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
        int chunkNo = 0;

        String fileId = getFileHash(filepath);

        peer.registerFile(fileId, replicationDegree);

        while(file.available() > 0){
            int readableBytes = file.available();
            byte[] chunk;

            if(readableBytes > PeerService.CHUNK_SIZE)
                chunk = new byte[PeerService.CHUNK_SIZE];
            else
                chunk = new byte[readableBytes];

            file.read(chunk);
            peer.requestChunkBackup(fileId,chunkNo,replicationDegree,chunk);
            System.out.println(chunkNo);
            chunkNo++;
        }
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

    private String getFileHash(String filepath) throws IOException {
        Path path = Paths.get(filepath);

        BasicFileAttributes basicAttr = Files.readAttributes(path, BasicFileAttributes.class);

        //Getting format date
        FileTime creationTime = basicAttr.creationTime();
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        String dateCreated = df.format(creationTime.toMillis());

        String stringToHash = filepath + dateCreated;

        //Making hash
        MessageDigest hashDigest;
        byte[] hash = null;

        try {
            hashDigest = MessageDigest.getInstance("SHA-256");

            hashDigest.update(stringToHash.getBytes());

            hash = hashDigest.digest();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }

        return DatatypeConverter.printHexBinary(hash);
    }
}
