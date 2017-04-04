package peers;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PeerService {

    public static final int CHUNK_SIZE = 64000;
    public static final int ERROR = -1;
    private static final byte CR = 0xD;
    private static final byte LF = 0xA;
    private static final String CRLF = "\r\n";
    private final String serverId;
    private final String protocolVersion;

    private PeerChannel controlChannel;
    private PeerChannel dataBackupChannel;
    private PeerChannel dataRestoreChannel;

    private String restoredFilesPath;

    private ChunkManager chunkManager;

    /**
     * registers the number of chunks written to a file
     * key = <fileID>_<ChunkNo>
     * value = true if the file is restored false otherwise
     */
    private ConcurrentHashMap<String, FileRestorer> restoredChunksObjects;

    /**
     * space available to store chunks
     * in 10^3 bytes
     **/
    private long availableSpace;


    public PeerService(String serverId, String protocolVersion, String serviceAccessPoint, InetAddress mcAddr, int mcPort, InetAddress mdbAddr, int mdbPort,
                       InetAddress mdrAddr, int mdrPort) throws IOException {

        this.serverId = serverId;
        this.protocolVersion = protocolVersion;
        String serviceAccessPoint1 = serviceAccessPoint;

        controlChannel = new PeerChannel(mcAddr, mcPort, this);
        System.out.println("Control Channel ready! Listening...");
        dataBackupChannel = new PeerChannel(mdbAddr, mdbPort, this);
        System.out.println("Data Backup Channel ready! Listening...");
        dataRestoreChannel = new PeerChannel(mdrAddr, mdrPort, this);
        System.out.println("Restore Channel ready! Listening...");

        System.out.println("Multicast channel addr: " + mcAddr + " port: " + mcPort);
        System.out.println("Multicast data backup addr: " + mdbAddr + " port: " + mdbPort);
        System.out.println("Multicast data restore addr: " + mdrAddr + " port: " + mdrPort);

        System.out.println("Server ID: " + serverId);

        PeerClientLink initiatorPeer = new PeerClientLink(this);

        try {
            //TODO add ip address
            Registry registry = LocateRegistry.getRegistry();
            registry.bind(serviceAccessPoint1, initiatorPeer);
        } catch (Exception e) {
            //TODO add rebind
            System.out.println("Peer error: " + e.getMessage());
            e.printStackTrace();
        }


        String chunksPath = serverId + "/chunks";
        String myFilesPath = serverId + "/my_files";
        restoredFilesPath = serverId + "/restored_files";

        createDir(serverId);
        createDir(myFilesPath);
        createDir(chunksPath);
        createDir(restoredFilesPath);

        controlChannel.receiveMessage();
        dataBackupChannel.receiveMessage();
        dataRestoreChannel.receiveMessage();

        restoredChunksObjects = new ConcurrentHashMap<>();

        chunkManager = new ChunkManager(serverId, chunksPath);

        availableSpace = 1000;
    }

    /**
     * Create a directory
     *
     * @param path path of the directory to be created
     */
    private void createDir(String path) {

        File file = new File(path);

        if (file.mkdir()) {
            System.out.println("Directory: " + path + " created");
        }
    }

    /**
     * Constructs a message header with the given fields
     *
     * @param fields header fields
     * @return message header
     */
    private String makeHeader(String... fields) {

        String header = "";

        for (String field : fields) {
            header = header.concat(field + " ");
        }

        header = header.concat(CRLF + CRLF);

        return header;
    }

    /**
     * Verifies if restoredChunksObjects contains fileID
     *
     * @param fileID id of the file
     * @return true if restoredChunksObjects contains fileID, false otherwise
     */
    private boolean requestedRestore(String fileID) {
        return restoredChunksObjects.get(fileID) != null;
    }

    public String getRestoredFilesPath() {
        return restoredFilesPath;
    }

    /**
     * Chunk Backup Subprotocol
     * Launches a Thread which constructs and sends a PUTCHUNK message for a given chunk
     * After sending the message, it waits, at first, one second, and checks if the
     * desired replication degree was reached for that chunk
     * if not, it doubles the waiting interval and tries again,
     * up to a maximum of five times
     *
     * @param fileId            file ID for the file that the chunk belongs to
     * @param chunkNo           number of the chunk to be backed up
     * @param replicationDegree desired replication degree of the chunk
     * @param chunk             chunk data
     */
    public void requestChunkBackup(String fileId, int chunkNo, int replicationDegree, byte[] chunk) {

        Runnable task = () -> {
            int counter = 1, multiplier = 1;
            String header = makeHeader("PUTCHUNK", protocolVersion, serverId, fileId,
                    Integer.toString(chunkNo), Integer.toString(replicationDegree));

            byte[] headerBytes = header.getBytes();
            byte[] buf = new byte[headerBytes.length + chunk.length];

            //concatenate contents of header and body
            System.arraycopy(headerBytes, 0, buf, 0, headerBytes.length);
            System.arraycopy(chunk, 0, buf, headerBytes.length, chunk.length);

            do {
                if (dataBackupChannel.sendMessage(buf))
                    printHeader(header, true);

                else {
                    System.err.println("IOException :: PeerService :: Failed to send PUTCHUNK message");

                    counter++;
                    multiplier *= 2;

                    if (counter > 5)
                        break;
                    else continue;
                }

                // wait and process response
                try {
                    Thread.sleep(1000 * multiplier);
                } catch (InterruptedException e) {
                    System.err.println("InterruptedException :: PeerService :: Retrying");
                    continue;
                }
                counter++;
                multiplier *= 2;
            }
            while (counter <= 5 && chunkManager.getReplicationDegree(fileId, Integer.toString(chunkNo)) < replicationDegree);

            int achievedRepDeg = chunkManager.getReplicationDegree(fileId, Integer.toString(chunkNo));
            if (counter > 5) {
                System.out.println("Timed out!");
                System.out.format("Achieved replication degree: %d", achievedRepDeg);
            } else if (achievedRepDeg >= replicationDegree) {
                System.out.println("Success!");
            }
        };

        ExecutorService service = Executors.newFixedThreadPool(10);

        service.execute(task);

        //new Thread(task).start();
    }

    /**
     * Updates the space this peer has to store chunks, deleting chunks if necessary
     * @param maxSpace new maximum available space
     */
    public void updateAvailableSpace(int maxSpace){
        availableSpace = maxSpace;
        ArrayList<String> deletedChunks = chunkManager.reclaimSpace(availableSpace);
        if(deletedChunks.isEmpty()) //no chunks were deleted
            return;

        for(String deletedChunk : deletedChunks){
            String[] deletedChunkInfo = deletedChunk.split("_");
            String fileID = deletedChunkInfo[0];
            String chunkNo = deletedChunkInfo[1];

            String header = makeHeader("REMOVED", protocolVersion, serverId, fileID, chunkNo);
            controlChannel.sendMessage(header.getBytes());
            printHeader(header,true);
        }
    }
    /**
     * Called when a message is received,
     * checks the instruction and calls the appropriate protocol
     *
     * @param message received message
     */
    public void messageHandler(byte[] message, int messageLength) {

        ByteArrayInputStream input = new ByteArrayInputStream(message);

        byte character;
        String header = "";

        do {
            character = (byte) input.read();
            header += (char) character;
        } while (character != -1 && character != CR);

        if (input.read() != LF || input.read() != CR || input.read() != LF) {
            System.out.println("Bad header");
        }

        header = header.trim();
        String messageHeader[] = header.split(" ");

        //check message type
        System.out.println(header);
        String messageType = messageHeader[0];
        String protocolVersion = messageHeader[1];
        String senderID = messageHeader[2];
        if (senderID.equals(this.serverId))// backup request sent from this peer, ignore
            return;

        switch (messageType) {
            case "PUTCHUNK": {
                if (messageHeader.length < 6) {
                    System.err.println("Not enough fields on header for PUTCHUNK");
                    break;
                }
                printHeader(header, false);
                String fileID = messageHeader[3];
                String chunkNo = messageHeader[4];
                String replicationDegree = messageHeader[5];
                byte[] chunk = new byte[input.available()];
                input.read(chunk, 0, input.available());
                chunkManager.storeChunk(protocolVersion, fileID, chunkNo, replicationDegree, chunk);
                String response = makeHeader("STORED", protocolVersion, serverId, fileID, chunkNo);
                controlChannel.sendMessage(response.getBytes());
                printHeader(response, true);
                break;
            }
            case "STORED": {
                if (messageHeader.length < 5) {
                    System.err.println("Not enough fields on header for STORED");
                    break;
                }
                printHeader(header, false);
                String fileID = messageHeader[3];
                String chunkNo = messageHeader[4];
                chunkManager.registerStorage(protocolVersion, senderID, fileID, chunkNo);
                break;
            }
            case "GETCHUNK": {
                if (messageHeader.length < 5) {
                    System.err.println("Not enough fields on header for GETCHUNK");
                    break;
                }
                printHeader(header, false);

                String fileID = messageHeader[3];
                String chunkNo = messageHeader[4];

                // if the file doesn't make part of the filesystem, the peer discard the message
                if (!chunkManager.hasChunk(fileID, Integer.parseInt(chunkNo))) {
                    break;
                }
                sendChunk(protocolVersion, senderID, fileID, chunkNo);
                break;
            }
            case "CHUNK": {
                if (messageHeader.length < 5) {
                    System.err.println("Not enough fields on header for GETCHUNK");
                    break;
                }
                printHeader(header, false);

                String fileID = messageHeader[3];

                if (!requestedRestore(fileID))
                    break;

                String chunkNo = messageHeader[4];
                byte[] chunk = new byte[input.available()];
                input.read(chunk, 0, input.available());

                FileRestorer fileRestorer = restoredChunksObjects.get(fileID);
                fileRestorer.processRestoredChunks(chunkNo, chunk);
                break;
            }
            case "DELETE": {
                if (messageHeader.length < 4) {
                    System.err.println("Not enough fields on header for DELETE");
                    break;
                }
                printHeader(header, false);
                String fileID = messageHeader[3];
                chunkManager.deleteFile(fileID);
                break;
            }
            default: {
                System.out.format("Unrecognized operation: %s", messageType);
                break;
            }
        }
    }


    /**
     * Constructs and sends a DELETE message for a given file, identified
     * by its file ID
     *
     * @param fileID file ID of the file to be deleted
     */
    public void requestFileDeletion(String fileID) {
        String message = makeHeader("DELETE", protocolVersion, serverId, fileID);
        controlChannel.sendMessage(message.getBytes());
        printHeader(message, true);
    }


    /**
     * Prints the header fields
     *
     * @param header the header string
     * @param sent   true if the message is being sent
     */
    private void printHeader(String header, boolean sent) {
        System.out.println("Message " + (sent ? "sent" : "received"));
        System.out.println(header);
    }

    /**
     * Creates the message "GETCHUNK" and send it
     *
     * @param fileId  id of the file to be restored
     * @param chunkNo Chunk number
     */
    public void requestChunkRestore(String fileId, int chunkNo) {

        Runnable task = () -> {

            String header = makeHeader("GETCHUNK", protocolVersion, serverId, fileId,
                    Integer.toString(chunkNo));

            byte[] headerBytes = header.getBytes();
            controlChannel.sendMessage(headerBytes);
            printHeader(header,true);
        };
        ExecutorService service = Executors.newFixedThreadPool(10);
        service.execute(task);
    }

    /**
     * Adds a file_id to the restore hash map
     *
     * @param fileId id of the file to be added
     */
    public void addToRestoredHashMap(String fileId, FileRestorer fileRestorer) {

        if (restoredChunksObjects.containsKey(fileId))
            restoredChunksObjects.replace(fileId, fileRestorer);

        restoredChunksObjects.put(fileId, fileRestorer);
    }


    private boolean sendChunk(String protocolVersion, String senderID, String fileID, String chunkNo) {

        if (protocolVersion == null || senderID == null || fileID == null || chunkNo == null)
            return false;

        byte[] chunkData = new byte[0];
        try {
            chunkData = chunkManager.getChunkData(fileID, chunkNo);
        } catch (IOException e) {
            System.err.println("Unable to get chunk data");
        }

        String header = makeHeader("CHUNK", protocolVersion, serverId, fileID, chunkNo);
        byte[] headerBytes = header.getBytes();

        byte[] buf = new byte[headerBytes.length + chunkData.length];

        //concatenate contents of header and body
        System.arraycopy(headerBytes, 0, buf, 0, headerBytes.length);
        System.arraycopy(chunkData, 0, buf, headerBytes.length, chunkData.length);

        Random random = new Random(System.currentTimeMillis());
        long waitTime = random.nextInt(400);
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        dataRestoreChannel.sendMessage(buf);
        printHeader(header, true);
        //TODO random time uniformly distributed
        return true;
    }

    public void registerFile(String fileId, int replicationDegree, int numChunks) {
        chunkManager.registerFile(fileId, replicationDegree);
        chunkManager.registerNumChunks(fileId, numChunks);
    }

    public int getNumChunks(String fileID) {
        return chunkManager.getNumChunks(fileID);
    }

    public void markRestored(String fileID) {
        restoredChunksObjects.remove(fileID);
    }
}
