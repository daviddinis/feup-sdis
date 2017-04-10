package peers;

import common.InitiatorInterface;

import javax.xml.bind.DatatypeConverter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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

public class PeerClientLink extends UnicastRemoteObject implements InitiatorInterface {

    private final PeerService peer;

    public PeerClientLink(PeerService peer) throws RemoteException {
        this.peer = peer;
    }

    @Override
    public void backup(String filepath, int replicationDegree) throws IOException {
        if (filepath == null || replicationDegree < 1) {
            throw new IllegalArgumentException("Invalid arguments for backup");
        }

        FileInputStream file = null;
        String path;
        if (filepath.startsWith(peer.getMyFilesPath()))
            path = filepath;
        else path = peer.getMyFilesPath() + '/' + filepath;
        try {
            file = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            System.out.format("File %s not found in the peer's files directory. Place it in the myPeers/<peer_id>/myFiles directory\n", path);
            return;
        }

        System.out.println("New backup request for file " + filepath);
        int chunkNo = 0;
        int readableBytes = -1;
        byte[] chunk;

        String fileId = getFileHash(path);

        while (file.available() > 0) {
            readableBytes = file.available();

            if (readableBytes > PeerService.CHUNK_SIZE)
                chunk = new byte[PeerService.CHUNK_SIZE];
            else
                chunk = new byte[readableBytes];

            file.read(chunk);
            peer.requestChunkBackup(fileId, chunkNo, replicationDegree, chunk);
            chunkNo++;
        }

        /*
           Check if the last chunk had exactly CHUNK_SIZE
           If so, send an empty chunk
         */
        if (readableBytes == PeerService.CHUNK_SIZE) {
            chunk = new byte[0];
            peer.requestChunkBackup(fileId, chunkNo, replicationDegree, chunk);
            chunkNo++;
        }

        peer.registerFile(fileId, replicationDegree, chunkNo, filepath);
    }

    @Override
    public void restore(String filepath) throws IOException {
        if (filepath == null)
            throw new IllegalArgumentException("Invalid argument for restore");

        System.out.println("New restore request for file " + filepath);

        String path = peer.getMyFilesPath() + '/' + filepath;
        String fileID = getFileHash(path);

        // Verifying if the file was already backed up
        int nChunks = peer.getNumChunks(fileID);
        if (nChunks == PeerService.ERROR) {
            System.err.format("File %s is not known to this peer", path);
            return;
        }

        if (peer.getProtocolVersion().equals("1.2")) {
            peer.tcpServer();
        }

        FileRestorer fileRestorer = new FileRestorer(peer, filepath, peer.getRestoredFilesPath(), fileID);
        peer.addToRestoredHashMap(fileID, fileRestorer);

        for (int chunkNo = 0; chunkNo < nChunks; chunkNo++) {
            peer.requestChunkRestore(fileID, chunkNo);
        }
    }

    @Override
    public void delete(String filepath) throws IOException {
        if (filepath == null)
            throw new IllegalArgumentException("Invalid arguments for delete");

        String path = peer.getMyFilesPath() + '/' + filepath;

        System.out.println("New delete request for file " + filepath);

        String fileID = getFileHash(path);
        peer.requestFileDeletion(fileID);
    }

    @Override
    public void reclaim(int maxAvailableBytes) throws RemoteException {
        if (maxAvailableBytes < 0)
            throw new IllegalArgumentException("Invalid arguments for reclaim");
        peer.updateAvailableSpace(maxAvailableBytes);
    }

    @Override
    public String state() throws RemoteException {
        String state = peer.getCurrentState();

        System.out.println(state);

        return state;
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
