package peers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class RestoreFile {

    public enum RestoreState {
        WAITING_FOR_CHUNKS, PREPARED_TO_RESTORE
    }

    /**
     * Stores chunks to be written to the file
     * key = chunk number
     * value = byte array
     */
    private ConcurrentHashMap<String,byte[]> restoredChunks;

    private RestoreState currentState;

    private int totalNumberOfChunks;

    private String filepath;
    private String restoredFilesPath;

    public RestoreFile(String filepath, String restoreFilePath){
        this.filepath = filepath;
        this.restoredFilesPath = restoreFilePath;
        currentState = RestoreState.WAITING_FOR_CHUNKS;
        totalNumberOfChunks = -1;
        restoredChunks = new ConcurrentHashMap<>();
    }

    public void processRestoredChunks(String chunkNo, byte[] chunkData){
        if(!restoredChunks.containsKey(chunkNo)){
            restoredChunks.put(chunkNo,chunkData);

            System.out.println("Chunk Number: "+chunkNo+" Length: "+ chunkData.length);

            if(isTheLastChunk(chunkData.length)){
                totalNumberOfChunks = Integer.parseInt(chunkNo) + 1;
            }
            System.out.println("Restored chunks size: "+restoredChunks.size());
            if(restoredChunks.size() == totalNumberOfChunks){
                System.out.println("Entrei");
                currentState = RestoreState.PREPARED_TO_RESTORE;
                try {
                    restoreFile();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isTheLastChunk(int chunkLength){
        if(chunkLength == PeerService.CHUNK_SIZE){
            return false;
        }
        if(chunkLength > PeerService.CHUNK_SIZE-5){
            return false;
        }

        return true;
    }

    public boolean restoreFile() throws FileNotFoundException {
        System.out.println("Esperando");
        while(currentState == RestoreState.WAITING_FOR_CHUNKS){
        }

        System.out.println("Estou vivo");

        FileOutputStream chunkFile = new FileOutputStream(restoredFilesPath + "/" + filepath, true);;
        restoredChunks.forEach((k, chunkData)->{

            try {
                chunkFile.write(chunkData);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        return true;
    }

}
