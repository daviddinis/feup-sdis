package peers;


import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
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
    private static final String MYFILES_FILENAME = "my_files_names";

    private final String serverId;
    private String protocolVersion;

    private PeerChannel controlChannel;
    private PeerChannel dataBackupChannel;
    private PeerChannel dataRestoreChannel;

    /**
     * Socket used on restore enhancement
     */
    private ServerSocket restoreTCPSocket;

    /**
     * Port used on restore enhancement
     */
    private final int dataRestorePort;

    private String restoredFilesPath;

    private ChunkManager chunkManager;

    /**
     * registers the number of chunks written to a file
     * key = <fileID>
     * value = true if the file is restored false otherwise
     */
    private ConcurrentHashMap<String, FileRestorer> restoredChunksObjects;

    /**
     * space available to store chunks
     * in 10^3 bytes
     **/
    private long availableSpace;

    /**
     * Stores chunk identfiers i.e, <FileID>_<chunkNo>
     * of chunks that are in the process of being backed up by other peers
     * and the amount of times the PUTCHUNK request has been made
     *
     * key = <fileID>_<chunkNo>
     * value = amount of times the PUTCHUNK request has been made
     */
    private ConcurrentHashMap<String, Integer> markedForBackup;

    private ArrayList<String> myFileIDs;

    private ArrayList<String> myFileNames;

    public PeerService(String serverId, String protocolVersion, String serviceAccessPoint, InetAddress mcAddr, int mcPort, InetAddress mdbAddr, int mdbPort,
                       InetAddress mdrAddr, int mdrPort) throws IOException {

        this.serverId = serverId;
        this.protocolVersion = protocolVersion;

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
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(serviceAccessPoint, initiatorPeer);
        } catch (Exception e) {
            System.out.println("Peer error: " + e.getMessage());
            e.printStackTrace();
        }


        String chunksPath = serverId + "/chunks";
        String myFilesPath = serverId + "/my_files";
        restoredFilesPath = serverId + "/restored_files";

        myFileIDs = new ArrayList<>();
        myFileNames = new ArrayList<>();

        createDir(serverId);
        createDir(myFilesPath);
        createDir(chunksPath);
        createDir(restoredFilesPath);

        controlChannel.receiveMessage();
        dataBackupChannel.receiveMessage();
        dataRestoreChannel.receiveMessage();

        restoredChunksObjects = new ConcurrentHashMap<>();

        chunkManager = new ChunkManager(serverId, chunksPath);

        //6 400 000 bytes (100 full chunks, ~6MB)
        availableSpace = 6400;

        // for restore enhancement
        dataRestorePort = mdrPort;

        markedForBackup = new ConcurrentHashMap<>();

        if(protocolVersion.equals("2.0")) {
            try {
                Thread.sleep(500);
                sendGreeting();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        loadMyFiles();
    }


    private void sendGreeting(){
        String header = makeHeader("AHOY",protocolVersion,serverId);
        controlChannel.sendMessage(header.getBytes());
        printHeader(header,true);
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
            String version;
            String header = makeHeader("PUTCHUNK", protocolVersion, serverId, fileId,
                    Integer.toString(chunkNo), Integer.toString(replicationDegree));

            byte[] headerBytes = header.getBytes();
            byte[] buf = new byte[headerBytes.length + chunk.length];

            //concatenate contents of header and body
            System.arraycopy(headerBytes, 0, buf, 0, headerBytes.length);
            System.arraycopy(chunk, 0, buf, headerBytes.length, chunk.length);

            do {
                counter++;
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
                multiplier *= 2;
            }
            while (counter <= 5 && chunkManager.getReplicationDegree(fileId, Integer.toString(chunkNo)) < replicationDegree);

            int achievedRepDeg = chunkManager.getReplicationDegree(fileId, Integer.toString(chunkNo));
            if (counter > 5) {
                System.out.println("Timed out!");
                System.out.format("Achieved replication degree: %d\n", achievedRepDeg);
            } else if (achievedRepDeg >= replicationDegree) {
                System.out.format("Successfully backed up chunk %s of file %s\n", chunkNo, fileId);
            }
        };

        new Thread(task).start();
    }

    /**
     * Updates the space this peer has to store chunks, deleting chunks if necessary
     * @param maxSpace new maximum available space
     */
    public void updateAvailableSpace(int maxSpace){
        availableSpace = maxSpace;
        ArrayList<String> deletedChunks = chunkManager.reclaimSpace(availableSpace * 1000);
        if(deletedChunks.isEmpty()) { //no chunks were deleted
            System.out.println("No chunks were deleted");
            return;
        }

        System.out.format("%d chunks were deleted\n", deletedChunks.size());
        for(String deletedChunk : deletedChunks){
	    System.out.println(deletedChunk);
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
     * @param address
     */
    public void messageHandler(byte[] message, int messageLength, InetAddress address) {

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
        String messageType = messageHeader[0];
        String protocolVersion = messageHeader[1];
        String senderID = messageHeader[2];

        if (senderID.equals(this.serverId))// message sent from this peer, ignore
            return;

        switch (messageType) {
            case "PUTCHUNK": {
                if (messageHeader.length < 6) {
                    System.err.println("Not enough fields on header for PUTCHUNK");
                    break;
                }
                printHeader(header, false);
                String fileID = messageHeader[3];

                /* File the chunk belongs to belongs to this peer, ignore */
                if (myFileIDs.contains(fileID))
                    break;

                String chunkNo = messageHeader[4];
                String replicationDegree = messageHeader[5];

                /* Check if there is space to store the chunk */
                byte[] chunk = new byte[input.available()];
                if(chunkManager.getOccupiedSpace() + chunk.length > availableSpace*1000){
		    System.err.format("No space available to store chunks. Space occupied = %d\n", chunkManager.getOccupiedSpace());	
                    break;
		}

		        if(!isMarkedForBackup(fileID,chunkNo)) {
                    markForBackup(fileID, chunkNo);
                    System.out.println("MARKING BACKUP!");
                    if(protocolVersion.equals("2.0"))
                        trackBackup(fileID,chunkNo);
                }
                else {
                    incrementBackupRequests(fileID, chunkNo);
                    System.out.format("Number of backup requests: %d\n", getNumBackupRequests(fileID, chunkNo));
                }

                input.read(chunk, 0, input.available());
                if(chunkManager.storeChunk(protocolVersion, fileID, chunkNo, replicationDegree, chunk)) {
                    String response = makeHeader("STORED", protocolVersion, serverId, fileID, chunkNo);
                    chunkManager.registerChunk(fileID,chunkNo,replicationDegree);
                    controlChannel.sendMessage(response.getBytes());
                    chunkManager.registerStorage(protocolVersion, this.serverId, fileID, chunkNo);
                    printHeader(response, true);
                }


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
                if(chunkManager.getReplicationDegree(fileID, chunkNo) >= chunkManager.getDesiredReplicationDegree(fileID))
                    unmarkForBackup(fileID, chunkNo);
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
                sendChunk(protocolVersion, senderID, fileID, chunkNo,address);
                break;
            }
            case "CHUNK": {
                if (messageHeader.length < 5) {
                    System.err.println("Not enough fields on header for GETCHUNK");
                    break;
                }
                printHeader(header, false);

                String fileID = messageHeader[3];
                String chunkNo = messageHeader[4];

                if (requestedRestore(fileID)){
                    byte[] chunk = new byte[input.available()];
                    input.read(chunk, 0, input.available());

                    FileRestorer fileRestorer = restoredChunksObjects.get(fileID);
                    fileRestorer.processRestoredChunks(chunkNo, chunk);
                }
                else {
                    chunkManager.registerChunkMessage(fileID,chunkNo);
                }

                break;
            }
            case "DELETE": {
                if (messageHeader.length < 4) {
                    System.err.println("Not enough fields on header for DELETE");
                    break;
                }
                printHeader(header, false);
                String fileID = messageHeader[3];
                ArrayList<String> deletedChunks = chunkManager.deleteFile(fileID);

                if(protocolVersion.equals("2.0") && deletedChunks != null){
                    for(String chunkNo : deletedChunks){
                        String response = makeHeader("DELETED",protocolVersion,serverId,fileID,chunkNo);
                        printHeader(response, true);
                        controlChannel.sendMessage(response.getBytes());
                    }
                }

                break;
            }
            case "REMOVED":{
                if (messageHeader.length < 5){
                    System.err.println("Not enough fields on header for REMOVE");
                    break;
                }
                String fileID = messageHeader[3];
                String chunkNo = messageHeader[4];
                printHeader(header, false);

                if(!chunkManager.registerRemoval(protocolVersion,senderID,fileID, chunkNo)
                        | !chunkManager.hasChunk(fileID,Integer.parseInt(chunkNo))) //file not registered on the peer
                    break;

                int desiredReplicationDegree = chunkManager.getDesiredReplicationDegree(fileID);
                int perceivedReplicationDegree = chunkManager.getReplicationDegree(fileID,chunkNo);

                if(perceivedReplicationDegree < desiredReplicationDegree && chunkManager.hasChunk(fileID,Integer.parseInt(chunkNo))) {
                    System.out.format("Replication degree for chunk %s of file %s is under the desired level.\n" +
                            "Desired = %d; Perceived = %d\n", chunkNo, fileID, desiredReplicationDegree, perceivedReplicationDegree
                    );
                    try {
                        byte[] chunk = chunkManager.getChunkData(fileID, chunkNo);
                        Random random = new Random();
                        Thread.sleep((long) random.nextInt(400));
                        if(!isMarkedForBackup(fileID, chunkNo)) {
                            requestChunkBackup(fileID, Integer.parseInt(chunkNo), desiredReplicationDegree, chunk);

                            if(protocolVersion.equals("2.0")){
                                trackBackup(fileID, chunkNo);
                            }
                        }
                    } catch (IOException | InterruptedException e) {
                        e.printStackTrace();
                    }
                } else { // Desired replication degree has been satisfied
                    if(isMarkedForBackup(fileID,chunkNo))
                        unmarkForBackup(fileID,chunkNo);
                }
                break;
            }
            case "DELETED": {
                String fileID = messageHeader[3];
                String chunkNo = messageHeader[4];
                printHeader(header,false);
                chunkManager.registerDeletion(senderID,fileID,chunkNo);
                break;
            }
            case "AHOY": {
                if(!protocolVersion.equals("1.3"))
                    break;
                ArrayList<String> filesToDelete = chunkManager.checkDeletion(senderID);
                if(filesToDelete == null)
                    break;

                for(String file : filesToDelete)
                    requestFileDeletion(file);
                break;
            }
            default: {
                System.out.format("Unrecognized operation: %s\n", messageType);
                break;
            }
        }
    }

    private void trackBackup(String fileID, String chunkNo){
        Runnable task = () -> {
            System.out.println("\n\nLAUNCHED\n\n");
            try {
                Thread.sleep(35000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if(isMarkedForBackup(fileID,chunkNo) && getNumBackupRequests(fileID, chunkNo) >= 5){
                unmarkForBackup(fileID, chunkNo);
                System.out.format("Backup Timed out normally, no action to be taken");
                return;
            }

            int desiredReplicationDegree = chunkManager.getDesiredReplicationDegree(fileID);
            int perceivedReplicationDegree = chunkManager.getReplicationDegree(fileID,chunkNo);

            if(desiredReplicationDegree > perceivedReplicationDegree){
                /*
                   Check if the initiator peer made 5 backup requests
                   if not, it indicates something went wrong with the initiator
                   peer during the backup
                 */
                System.out.format("Replication degree for chunk %s of file %s is under the desired level.\n" +
                        "Desired = %d; Perceived = %d\n", chunkNo, fileID, desiredReplicationDegree, perceivedReplicationDegree
                );

                if(getNumBackupRequests(fileID,chunkNo) < 5){
                    unmarkForBackup(fileID,chunkNo);

                    try {
                        Random random = new Random();
                        long waitTime = (long)random.nextInt(400);
                        System.out.println(waitTime);
                        Thread.sleep(waitTime);

                        if(isMarkedForBackup(fileID, chunkNo)) { // Other peer took the responsibility
                            return;
                        }

                        byte[] chunk = chunkManager.getChunkData(fileID, chunkNo);
                        requestChunkBackup(fileID,Integer.parseInt(chunkNo),desiredReplicationDegree, chunk);

                    } catch (IOException | InterruptedException e ) {
                        System.err.println("Unable to read chunk data");
                    }
                }
                else {
                    unmarkForBackup(fileID,chunkNo);
                }
            }
            else {
                System.out.println("Chunk Backup request successfully complete! No action to take!");
                unmarkForBackup(fileID,chunkNo);
            }
        };
        task.run();
    }


    /**
     * Constructs and sends a DELETE message for a given file, identified
     * by its file ID
     *
     * @param fileID file ID of the file to be deleted
     */
    public void requestFileDeletion(String fileID) {
        myFileNames.remove(myFileIDs.indexOf(fileID));
        saveMyFiles();
        
        if(myFileIDs.remove(fileID) || chunkManager.isMarkedForDeletion(fileID)){
            chunkManager.markForDeletion(fileID);
            chunkManager.deleteFile(fileID);
            for(int i = 0; i < 5; i++){
                String message = makeHeader("DELETE", protocolVersion, serverId, fileID);
                controlChannel.sendMessage(message.getBytes());
                printHeader(message, true);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
        else{
            System.out.format("File %s does not belong to this peer\n", fileID);
        }
    }


    /**
     * Prints the header fields
     *
     * @param header the header string
     * @param sent   true if the message is being sent
     */
    private void printHeader(String header, boolean sent) {
        System.out.println("Message " + (sent ? "sent:" : "received:") +"\n"+header);
    }

    /**
     * Creates and sends a GETCHUNK message
     *
     * @param fileId  id of the file to be restored
     * @param chunkNo Chunk number
     */
    public void requestChunkRestore(String fileId, int chunkNo) throws IOException {

        Runnable task = () -> {
            int counter = 1, multiplier = 1;

            FileRestorer fileRestorer = restoredChunksObjects.get(fileId);

            String header = makeHeader("GETCHUNK", protocolVersion, serverId, fileId,
                    Integer.toString(chunkNo));

            byte[] headerBytes = header.getBytes();

            do {
                if (controlChannel.sendMessage(headerBytes))
                    printHeader(header, true);

                else {
                    System.err.println("IOException :: PeerService :: Failed to send GETCHUNK message");

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
            while (!fileRestorer.getRestoredChunks().containsKey(chunkNo) && restoredChunksObjects.containsKey(fileId));
        };

        new Thread(task).start();
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


    private boolean sendChunk(String protocolVersion, String senderID, String fileID, String chunkNo, InetAddress address) {

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

        Random random = new Random();
        long waitTime = random.nextInt(400);
        try {
            Thread.sleep(waitTime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(chunkManager.canSendChunkMessage(fileID,chunkNo)){

            if(protocolVersion.equals("2.0")){
                try {
                    Socket restoreSocket = new Socket(address,dataRestorePort);

                    DataOutputStream output = new DataOutputStream(restoreSocket.getOutputStream());

                    output.write(buf,0,buf.length);
                    output.flush();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            else {
                dataRestoreChannel.sendMessage(buf);
            }
            printHeader(header, true);
            //TODO random time uniformly distributed
        }

        return true;
    }

    /**
     * Called when a file backup is requested by the client
     * Registers the file as belonging to this peer and registers the file and the number of chunks
     * in the ChunkManager
     *  @param fileId            file ID of the file to backup
     * @param replicationDegree desired replication degree of the file to backup
     * @param numChunks         number of chunks in the file to backup
     * @param filepath
     */
    public void registerFile(String fileId, int replicationDegree, int numChunks, String filepath) {
        myFileIDs.add(fileId);
        myFileNames.add(filepath);
        saveMyFiles();

        chunkManager.registerFile(fileId, replicationDegree);
        chunkManager.registerNumChunks(fileId, numChunks);

    }

    public int getNumChunks(String fileID) {
        return chunkManager.getNumChunks(fileID);
    }

    public void markRestored(String fileID) {
        restoredChunksObjects.remove(fileID);
    }

    /**
     * Returns the protocol version
     * @return protocol version
     */
    public String getProtocolVersion() {
        return protocolVersion;
    }

    /**
     * Function used to create the tcp socket and to listen for messages
     * @throws IOException
     */
    public void tcpServer() throws IOException {
        if(restoreTCPSocket != null){
            restoreTCPSocket.close();
        }

        restoreTCPSocket = new ServerSocket(dataRestorePort);

        Runnable task = () -> {
            Socket dataSocket = null;

            while (true){

                try {
                    dataSocket=restoreTCPSocket.accept();
                    processRestoreMessage(dataSocket);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        new Thread(task).start();
    }

    /**
     * Process a message from the restore socket
     * @param dataSocket socket from where the message is read
     * @throws IOException
     */
    public void processRestoreMessage(Socket dataSocket) throws IOException {

        Runnable task = () -> {
            DataInputStream input = null;
            try {
                input = new DataInputStream(dataSocket.getInputStream());
                byte[] chunkData = new byte[input.available()];

                input.readFully(chunkData);

                messageHandler(chunkData,chunkData.length, dataSocket.getInetAddress());

            } catch (IOException e) {
                e.printStackTrace();
            }

        };

        ExecutorService service = Executors.newFixedThreadPool(10);

        service.execute(task);
    }

    private void markForBackup(String fileID, String chunkNo){
        String key = fileID + '_' + chunkNo;
        if(!markedForBackup.containsKey(key))
            markedForBackup.put(key,1);
    }

    private boolean isMarkedForBackup(String fileID, String chunkNo){
        String key = fileID + '_' + chunkNo;
        return markedForBackup.containsKey(key);
    }

    private void unmarkForBackup(String fileID, String chunkNo){
        String key = fileID + '_' + chunkNo;
        if(markedForBackup.containsKey(key))
            markedForBackup.remove(key);
    }

    private int getNumBackupRequests(String fileID, String chunkNo){
        String key = fileID + '_' + chunkNo;
        return markedForBackup.getOrDefault(key,-1);
    }

    private void incrementBackupRequests(String fileID, String chunkNo) {
        String key = fileID + '_' + chunkNo;
        markedForBackup.replace(key, markedForBackup.get(key) + 1);
    }

    public String getCurrentState() {

        String currentState = "\t\t============ FILES ============\n\n";

        for (int i=0;i<myFileIDs.size();i++){
            currentState += "File number " + i + "\n";
            currentState += "\tFilename: " + myFileNames.get(i) + ";\n";
            currentState += "\tFile backup service id: " + myFileIDs.get(i) + ";\n";
            currentState += chunkManager.getFileCurrentState(myFileIDs.get(i));
        }

        currentState += "\n\t\t============ CHUNKS ============\n\n";
        currentState += chunkManager.getChunksState();

        currentState += "\n\t\t============ SIZE ============\n\n";
        currentState += "\tMaximum Amount to store chunks: " + availableSpace + " Kb\n";
        currentState += "\tStorage used to backup chunks: " + (chunkManager.getOccupiedSpace()/1000) + " Kb\n";
        currentState += "\tSpace available: " + (availableSpace - (chunkManager.getOccupiedSpace()/1000)) + " Kb\n";

        return currentState;
    }

    public void saveMyFiles(){
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serverId + '/' + MYFILES_FILENAME));
            oos.writeObject(myFileIDs);
            oos.writeObject(myFileNames);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void loadMyFiles(){
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serverId + '/' + MYFILES_FILENAME));
            myFileIDs = (ArrayList<String>) ois.readObject();
            myFileNames = (ArrayList<String>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Unable to load peer filenames");
        }
    }
}
