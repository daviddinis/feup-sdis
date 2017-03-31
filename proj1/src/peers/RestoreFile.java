package peers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class RestoreFile {

    /**
     * Stores chunks to be written to the file
     * key = chunk number
     * value = byte array
     */
    private ConcurrentHashMap<String, byte[]> restoredChunks;

    private int totalNumberOfChunks;

    private String filepath;
    private String restoredFilesPath;

    public RestoreFile(String filepath, String restoreFilePath) {
        this.filepath = filepath;
        this.restoredFilesPath = restoreFilePath;
        totalNumberOfChunks = -1;
        restoredChunks = new ConcurrentHashMap<>();
    }

    public void processRestoredChunks(String chunkNo, byte[] chunkData) {
        if (!restoredChunks.containsKey(chunkNo)) {
            restoredChunks.put(chunkNo, chunkData);

            System.out.println("Chunk Number: "+chunkNo+" Length: "+ chunkData.length);

            if (isTheLastChunk(chunkData.length)) {
                totalNumberOfChunks = Integer.parseInt(chunkNo) + 1;
            }
            System.out.println("Restored chunks size: "+restoredChunks.size());
            if (restoredChunks.size() == totalNumberOfChunks) {
                System.out.println("Entrei");
                try {
                    restoreFile();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public boolean isTheLastChunk(int chunkLength) {
        return chunkLength != PeerService.CHUNK_SIZE && chunkLength <= PeerService.CHUNK_SIZE - 5;
    }

    public boolean restoreFile() throws FileNotFoundException {

        FileOutputStream chunkFile = new FileOutputStream(restoredFilesPath + "/" + filepath, true);
        ;

        for (int i = 0; i < restoredChunks.size(); i++) {
            try {
                chunkFile.write(restoredChunks.get(Integer.toString(i)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Restore done from file " + filepath);

        return true;
    }

}
