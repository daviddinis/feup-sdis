package peers;

import common.InitiatorInterface;

import javax.xml.bind.DatatypeConverter;
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

public class InitiatorPeer extends UnicastRemoteObject implements InitiatorInterface {

    private PeerService peer;

    public InitiatorPeer(PeerService peer) throws RemoteException {
        this.peer = peer;

    }

    private String getFileHash(String pathname) throws IOException {
        Path path = Paths.get(pathname);

        BasicFileAttributes basicAttr = Files.readAttributes(path, BasicFileAttributes.class);

        //Getting format date
        FileTime creationTime = basicAttr.creationTime();
        DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
        String dateCreated = df.format(creationTime.toMillis());

        String stringToHash = pathname + dateCreated;

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

    @Override
    public void backup(String pathname, int replicationDegree) throws IOException {
        System.out.println("New backup request");

        String fileId = getFileHash(pathname);

        System.out.println(fileId);

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
