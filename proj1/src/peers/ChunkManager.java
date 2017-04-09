package peers;


import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkManager {

    private static final String CHUNK_MAP_FILENAME = "chunk_info";
    private static final String STATE_FILENAME = ".peer_data";
    private static final int MAX_SLEEP_TIME = 400;
    /**
     * stores the number of chunks every file has
     */
    private ConcurrentHashMap<String, Integer> numChunksFile;

    /**
     * registers the peers that have stored chunks
     * key = <fileID>_<ChunkNo>
     * value = array with the peer id of the peers that have stored that chunk
     */
    private ConcurrentHashMap<String, ArrayList<Integer>> chunkMap;

    /**
     * registers the perceived replication degree of the chunks,
     * used by Properties to write information to a file
     *
     * key = <fileID>_<ChunkNo>
     * value = Perceived replication degree for the chunk
     */
    private ConcurrentHashMap<String, String> perceivedChunkRepDeg;

    /**
     * stores the desired replication degree for every file the
     * peer has stored or has chunks of
     */
    private ConcurrentHashMap<String, Integer> desiredFileReplicationDegrees;

    /**
     * registers the chunk number of the stored chunks
     * key = <fileID>
     * value = array with the chunk numbers of the stored chunks
     */
    private ConcurrentHashMap<String, ArrayList<Integer>> storedChunks;

    /**
     * when the peer receives a CHUNK message verifies if he has that chunk,
     * and if he has then he put on this arraylist
     * String will be <fileID>_<chunkNo>
     */
    private ArrayList<String> restoredChunkList;

    /**
     * Stores the chunks that are marked for deletion
     * Used in the enhanced version of the File Deletion Subprotocol
     *
     * key = <fileID>_<ChunkNo>
     * value = array with the peer id of the peers that have stored that chunk
     */
    private ConcurrentHashMap<String, ArrayList<Integer>> markedForDeletion;

    /**
     * Stores chunk identfiers i.e, <FileID>_<chunkNo>
     * of chunks that are in the process of being backed up
     * by other peers
     */
    private ArrayList<String> markedForBackup;

    /**
     * used to write perceived chunk replication degrees to a file
     */
    private Properties chunkRepDegProperties;

    private final String chunksPath;
    private final String serverId;
    private long occupiedSpace;

    public ChunkManager(String serverId, String chunksPath) {

        this.serverId = serverId;
        this.chunksPath = chunksPath;

        if(!loadState()) {
            desiredFileReplicationDegrees = new ConcurrentHashMap<>();
            storedChunks = new ConcurrentHashMap<>();
            chunkMap = new ConcurrentHashMap<>();
            numChunksFile = new ConcurrentHashMap<>();
            perceivedChunkRepDeg = new ConcurrentHashMap<>();
            occupiedSpace = 0;
            restoredChunkList = new ArrayList<>();
            markedForDeletion = new ConcurrentHashMap<>();
        }

        markedForBackup = new ArrayList<>();
        chunkRepDegProperties = new Properties();

        /* Create chunk replication degree file */
        String filepath = serverId + "/" + CHUNK_MAP_FILENAME;
        File file = new File(filepath);
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.err.println("Unable to create chunk info file!");
            System.exit(1);
        }
    }

    /**
     * Saves the chunk replication degrees to a file
     */
    private boolean saveReplicationDegrees(){
        chunkRepDegProperties.putAll(perceivedChunkRepDeg);
        try {
            chunkRepDegProperties.store(new FileOutputStream(serverId + '/' + CHUNK_MAP_FILENAME),"FileID_ChunkNo=PerceivedReplicationDegree");
        } catch (IOException e) {
            System.err.println("Failed to write to chunk file");
            return false;
        }
        return true;
    }

    /**
     * Writes the state of the peer to a file
     * @return true if write was successful
     */
    private synchronized boolean saveState(){
        try {
            ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(serverId + '/' + STATE_FILENAME));
            oos.writeObject(numChunksFile);
            oos.writeObject(chunkMap);
            oos.writeObject(perceivedChunkRepDeg);
            oos.writeObject(desiredFileReplicationDegrees);
            oos.writeObject(storedChunks);
            oos.writeObject(restoredChunkList);
            oos.writeObject(markedForDeletion);
            oos.close();
        } catch (IOException e) {
            System.err.println("Unable to open state file");
            return false;
        }
        return true;
    }

    private boolean loadState(){
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(serverId+'/'+STATE_FILENAME));
            numChunksFile = (ConcurrentHashMap<String, Integer>) ois.readObject();
            chunkMap = (ConcurrentHashMap<String,ArrayList<Integer>>) ois.readObject();
            perceivedChunkRepDeg = (ConcurrentHashMap<String,String>) ois.readObject();
            desiredFileReplicationDegrees = (ConcurrentHashMap<String,Integer>) ois.readObject();
            storedChunks = (ConcurrentHashMap<String,ArrayList<Integer>>) ois.readObject();
            restoredChunkList = (ArrayList<String>) ois.readObject();
            markedForDeletion = (ConcurrentHashMap<String,ArrayList<Integer>>) ois.readObject();
            occupiedSpace = getOccupiedSpace();
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Unable to load peer state");
            return false;
        }
        return true;
    }


    /**
     * Places a file in the replication degree hash map and
     * initializes a list in the peer list
     *
     * @param fileID            file ID for the sent file
     * @param replicationDegree desired replication degree for the file
     */
    public void registerFile(String fileID, int replicationDegree) {
        if (desiredFileReplicationDegrees.get(fileID) != null) {
            desiredFileReplicationDegrees.remove(fileID);
        }
        desiredFileReplicationDegrees.put(fileID, replicationDegree);
    }

    /**
     * Returns the desired replication degree of a file,
     * identified by it's ID
     *
     * @param fileID file to check
     * @return desired replication degree of the file
     */
    public int getDesiredReplicationDegree(String fileID){
        return desiredFileReplicationDegrees.getOrDefault(fileID,-1);
    }

    /**
     * Registers a chunk in the storedChunks HashMap
     * checks if the file is already registered, if not, it is registered
     * with the desired replication degree
     * checks if the chunk is already stored, if not, it is stored
     *
     * @param fileID            file ID of the file the chunk belongs to
     * @param chunkNo           chunk number of the chunk to register
     * @param replicationDegree desired replication degree of the chunk
     */
    public void registerChunk(String fileID, String chunkNo, String replicationDegree) {
        int chkNo = Integer.parseInt(chunkNo);
        ArrayList<Integer> fileChunks = storedChunks.get(fileID);

        if (hasChunk(fileID, chkNo))
            return;

        // no chunks registered for this file, register the file and register the chunk
        if (fileChunks == null) {
            registerFile(fileID, Integer.parseInt(replicationDegree));
            fileChunks = new ArrayList<>();
            fileChunks.add(chkNo);
            storedChunks.put(fileID, fileChunks);
            return;
        }
        fileChunks.add(chkNo);
    }

    /**
     * Function called when the peer receives a PUTCHUNK message from another peer
     * registers the file and the chunk
     *
     * @param protocolVersion   version of the Chunk Backup Subprotocol
     * @param fileID            file ID of the file the chunk belongs to
     * @param chunkNo           chunk number of the chunk to be stored
     * @param replicationDegree desired file replication degree
     * @param chunkData         chunk data
     * @return true if the chunk was registered and stored
     */
    public boolean storeChunk(String protocolVersion, String fileID, String chunkNo, String replicationDegree, byte[] chunkData) {
        if (protocolVersion == null || fileID == null || chunkData == null
                || replicationDegree == null
                || chunkNo == null)
            return false;

        /* Check if the chunk is already stored */
        if(hasChunk(fileID,Integer.parseInt(chunkNo)))
            return true;

        String chunkKey = fileID+"_"+chunkNo;


        try {

            if(protocolVersion.equals("1.1")){ 
                sleep();
                if(chunkMap.containsKey(chunkKey)){
                    //verifying if the replication degree desire by the peer was already reached
                    if(getReplicationDegree(fileID,chunkNo) >= Integer.parseInt(replicationDegree)){
                        return false;
                    }
                }
                writeChunkToMemory(fileID,chunkNo,chunkData);
            }
            else {
                writeChunkToMemory(fileID,chunkNo,chunkData);
                sleep();
            }


        } catch (IOException e) {
            System.err.println("IOException :: PeerService :: Unable to backup chunk.");
            return false;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    /**
     * Writes a chunk of a given file to memory
     * @param fileID id of the file
     * @param chunkNo number of the chunk
     * @param chunkData data of the chunk
     * @throws IOException
     */
    public void writeChunkToMemory(String fileID, String chunkNo, byte[] chunkData) throws IOException {
        String filename = fileID + "_" + chunkNo;
        FileOutputStream chunkFile = new FileOutputStream(chunksPath + "/" + filename);
        chunkFile.write(chunkData);
    }

    /**
     * Sleeps inside of a range from 0 to 400 ms
     * @throws InterruptedException
     */
    public void sleep() throws InterruptedException {
        Random random = new Random();
        long waitTime = random.nextInt(MAX_SLEEP_TIME);
        Thread.sleep(waitTime);
    }

    /**
     * Called when a peer receives a STORED message from another peer
     * It updates the peer's chunkMap to reflect the perceived
     * replication degree of the chunk
     *
     * @param protocolVersion protocol version used by the message sender
     * @param senderID        id of the sender of the STORED message
     * @param fileID          id of the file whose chunk was stored
     * @param chunkNo         chunk number of the stored chunk
     */
    public boolean registerStorage(String protocolVersion, String senderID, String fileID, String chunkNo) {
        if (protocolVersion == null || senderID == null || fileID == null || chunkNo == null)
            return false;

        ArrayList<Integer> chunkPeers = chunkMap.get(fileID + '_' + chunkNo);
        String chunkKey = fileID + '_' + chunkNo;
        int sender = Integer.parseInt(senderID);
        if (chunkPeers == null) {
            chunkPeers = new ArrayList<>();
            chunkPeers.add(sender);
            chunkMap.put(chunkKey, chunkPeers);
            perceivedChunkRepDeg.put(chunkKey,"1");
        } else {
            for (Integer chunkPeer : chunkPeers) {
                if (chunkPeer == sender)    // peer was already registered
                    return true;
            }
            chunkPeers.add(sender);
            perceivedChunkRepDeg.replace(chunkKey, Integer.toString(chunkPeers.size()));
        }
        saveReplicationDegrees();
        saveState();
        return true;
    }

    /**
     *
     * Called when a peer receives a REMOVED message from another peer
     * It updates the peer's chunkMap to reflect the perceived
     * replication degree of the chunk
     *
     * @param protocolVersion protocol version used by the message sender
     * @param senderID        id of the sender of the REMOVED message
     * @param fileID          id of the file whose chunk was removed
     * @param chunkNo         chunk number of the removed chunk
     * @return                true on success, false if the file was not registered on this peer
     */
    public boolean registerRemoval(String protocolVersion, String senderID, String fileID, String chunkNo){
        if (protocolVersion == null || senderID == null || fileID == null || chunkNo == null)
            return false;

        String chunkKey = fileID + '_' + chunkNo;
        ArrayList<Integer> chunkPeers = chunkMap.get(chunkKey);
        if(chunkPeers == null) //file not registered on the server
            return false;

        Object sender = Integer.parseInt(senderID);
        chunkPeers.remove(sender);
        perceivedChunkRepDeg.replace(chunkKey,Integer.toString(chunkPeers.size()));
        saveReplicationDegrees();
        saveState();
        return true;
    }

    public void markForBackup(String fileID, String chunkNo){
        String key = fileID + '_' + chunkNo;
        if(!markedForBackup.contains(key))
            markedForBackup.add(key);
    }

    public boolean isMarkedForBackup(String fileID, String chunkNo){
        String key = fileID + '_' + chunkNo;
        return markedForBackup.contains(key);
    }

    public void unmarkForBackup(String fileID, String chunkNo){
        String key = fileID + '_' + chunkNo;
        if(markedForBackup.contains(key))
            markedForBackup.remove(key);
    }

    /**
     * Verifies if a given chunk of a given file is stored on the peer
     *
     * @param fileID  id of the file
     * @param chunkNo Number of the chunk to be searched
     * @return true if the chunk exists on the filesystem, false otherwise
     */
    public boolean hasChunk(String fileID, Integer chunkNo) {

        ArrayList<Integer> fileStoredChunks;
        fileStoredChunks = storedChunks.get(fileID);

        return fileStoredChunks != null && fileStoredChunks.contains(chunkNo);
    }

    /**
     * Get the perceived replication degree of a chunk
     * Checks the peer's chunk map to see how many peers have a copy of the chunk
     *
     * @param fileID  file ID of the file the chunk belongs to
     * @param chunkNo chunk number of the chunk
     * @return the perceived replication degree of the chunk
     */
    public int getReplicationDegree(String fileID, String chunkNo) {
        String key = fileID + '_' + chunkNo;
        return chunkMap.containsKey(key) ? chunkMap.get(key).size() : -1;
    }

    public boolean registerNumChunks(String fileID, int numChunks) {
        if (numChunksFile.containsKey(fileID))
            return false;

        numChunksFile.put(fileID, numChunks);
        return true;
    }

    /**
     * Returns the number of chunks a file has
     *
     * @param fileID file ID of the file
     * @return number of chunks the file has or ERROR (-1) if the file is not registered
     */
    public int getNumChunks(String fileID) {
        return numChunksFile.getOrDefault(fileID, PeerService.ERROR);
    }

    /**
     * Marks the chunks belonging to a file as set for deletion
     * Used in the enhanced version of the File Deletion Subprotocol
     * @param fileID ID of the file to be deleted
     */
    public void markForDeletion(String fileID){

        for(Map.Entry<String,ArrayList<Integer>> entry : chunkMap.entrySet()){
            if(entry.getKey().startsWith(fileID)) {
                markedForDeletion.put(entry.getKey(),chunkMap.get(entry.getKey()));
                chunkMap.remove(entry.getKey());
                perceivedChunkRepDeg.remove(entry.getKey());
            }
        }
        saveReplicationDegrees();
        saveState();
    }


    /**
     * Called when a peer receives a DELETED message
     * Updates the markedForDeletion map
     * @param senderID  ID of the peer who sent the DELETED message
     * @param fileID    ID of the file who's chunk was deleted
     * @param chunkNo   Number of the deleted chunk
     */
    public void registerDeletion(String senderID, String fileID, String chunkNo){
        String key = fileID + '_' + chunkNo;
        ArrayList<Integer> chunkPeers = markedForDeletion.get(key);
        if(chunkPeers == null)
            return;

        Integer sender = Integer.parseInt(senderID);

        if(chunkPeers.contains(sender))
            chunkPeers.remove(sender);

        if(chunkPeers.isEmpty())
            markedForDeletion.remove(key);

        saveState();
    }

    public ArrayList<String> checkDeletion(String senderID){
        if(markedForDeletion.isEmpty())
            return null;

        ArrayList<String> toDelete = new ArrayList<>();
        if(!markedForDeletion.isEmpty()){
            Integer sender = Integer.parseInt(senderID);
            for(Map.Entry<String,ArrayList<Integer>> entry : markedForDeletion.entrySet()){
                ArrayList<Integer> chunkPeers = entry.getValue();
                if(chunkPeers.contains(sender)){
                    String fileID = entry.getKey().split("_")[0];
                    if(!toDelete.contains(fileID))
                        toDelete.add(fileID);
                }
            }
        }

        return toDelete;
    }

    public boolean isMarkedForDeletion(String fileID){
        if(!markedForDeletion.isEmpty()){
            for(Map.Entry<String,ArrayList<Integer>> entry : markedForDeletion.entrySet()){
                if(entry.getKey().startsWith(fileID))
                    return true;
            }
        }
        return false;
    }

    /**
     * Deletes the stored chunks (if any) the peer has
     * belonging to file identified by the parameter file ID
     *
     * @param fileID file ID of the file whose chunks are to be deleted
     */
    public ArrayList<String> deleteFile(String fileID) {
        ArrayList<Integer> fileChunks = storedChunks.get(fileID);
        ArrayList<String> deletedChunks = null;
        if (fileChunks == null) {  // peer has no chunks belonging to this file
            System.out.format("This peer has no chunks belonging to the file with file ID %s\n", fileID);
        }

        else {
            deletedChunks = new ArrayList<>();
            for (Integer fileChunk : fileChunks) {
                String chunkNo = Integer.toString(fileChunk);
                String chunkName = fileID + '_' + chunkNo;

                deletedChunks.add(chunkNo);
                chunkMap.remove(chunkName);
                perceivedChunkRepDeg.remove(chunkName);

                String chunkPath = chunksPath + '/' + chunkName;
                File chunk = new File(chunkPath);
                if (chunk.delete())
                    System.out.format("Chunk %d, belonging to file %s deleted\n", fileChunk, fileID);
            }
            storedChunks.remove(fileID);
        }

        if(desiredFileReplicationDegrees.containsKey(fileID))
            desiredFileReplicationDegrees.remove(fileID);


        saveReplicationDegrees();
        saveState();

        return deletedChunks;
    }

    public byte[] getChunkData(String fileID, String chunkNo) throws IOException {

        String filename = fileID + "_" + chunkNo;
        FileInputStream chunkFile;

        chunkFile = new FileInputStream(chunksPath + "/" + filename);

        byte[] chunkData;
        int readableBytes = chunkFile.available();

        if (readableBytes > PeerService.CHUNK_SIZE)
            chunkData = new byte[PeerService.CHUNK_SIZE];
        else
            chunkData = new byte[readableBytes];


        chunkFile.read(chunkData);

        return chunkData;
    }

    /**
     * Checks if the space made available for chunks is enough for the currently stored chunks,
     * if it is, return, if not, delete the smallest chunk and check again
     *
     * @param availableSpace maximum space to be occupied by the stored chunks
     * @return ArrayList with the names of the deleted files
     */
    public ArrayList<String> reclaimSpace(long availableSpace){
        File chunkDir = new File(chunksPath);
        ArrayList<String> deletedChunks = new ArrayList<>();

        System.out.println(getOccupiedSpace());
        while(getOccupiedSpace() > availableSpace) {
            System.out.println(getOccupiedSpace());

            File[] chunks = chunkDir.listFiles();
            if (chunks == null)
                break;

            File toDelete = chunks[0];

            String[] toDeleteInfo = toDelete.getName().split("_");

            String toDeleteFileID = toDeleteInfo[0];
            String toDeleteChunkNo = toDeleteInfo[1];

            String fileID;
            String chunkNo;

            for (File chunk : chunks) {

                String[] chunkInfo = chunk.getName().split("_");
                if(chunkInfo.length < 2)
                    chunk.delete();

                fileID = chunkInfo[0];
                chunkNo = chunkInfo[1];

                if(getReplicationDegree(fileID,chunkNo) > getReplicationDegree(toDeleteFileID,toDeleteChunkNo)){
                    toDelete = chunk;
                    toDeleteFileID = fileID;
                    toDeleteChunkNo = chunkNo;
                }
            }
            deletedChunks.add(toDelete.getName());
            toDelete.delete();

            String key = toDeleteFileID + '_' + toDeleteChunkNo;

            ArrayList<Integer> chunkPeers = chunkMap.get(key);
            chunkPeers.remove(this.serverId);
            perceivedChunkRepDeg.replace(key, Integer.toString(Integer.parseInt(perceivedChunkRepDeg.get(key)) + 1));
            ArrayList<Integer> fileChunks = storedChunks.get(toDeleteFileID);
            fileChunks.remove(Integer.parseInt(toDeleteChunkNo));

            saveReplicationDegrees();
            saveState();

        }
        return deletedChunks;
    }

    /**
     * Get the space occupied by the chunks this peer is storing
     * @return occupied space, in bytes
     */
    public long getOccupiedSpace(){
        File chunkDir = new File(chunksPath);
        long occupiedSpace = 0;
        for(File file : chunkDir.listFiles()){
            occupiedSpace += file.length();
        }
        return occupiedSpace;
    }

    /**
     * Verifies if a given chunk from a file is stored on the peer,
     * and if this is the case, the peer adds to the restoreChunkList
     * @param fileID id from the file
     * @param chunkNo number of the chunk
     * @return true if it has the chunk, false otherwise
     */
    public boolean registerChunkMessage(String fileID, String chunkNo){

        if(!hasChunk(fileID, Integer.parseInt(chunkNo))){
            return false;
        }

        if(!restoredChunkList.contains(fileID+"_"+chunkNo)){
            restoredChunkList.add(fileID+"_"+chunkNo);
        }

        return true;
    }

    /**
     * verifies if a chunk message was already sent for that specific chunk
     * @param fileID id from the file
     * @param chunkNo number of the chunk
     * @return true if the peer can send the message, false otherwise
     */
    public boolean canSendChunkMessage(String fileID, String chunkNo){

        if(restoredChunkList.contains(fileID+"_"+chunkNo)){
            restoredChunkList.remove(fileID+"_"+chunkNo);
            return false;
        }

        return true;
    }
}
