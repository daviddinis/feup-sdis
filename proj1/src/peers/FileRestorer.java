package peers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class FileRestorer {

    /**
     * Stores chunks to be written to the file
     * key = chunk number
     * value = byte array
     */
    private final ConcurrentHashMap<String, byte[]> restoredChunks;
    private final String filepath;
    private final String restoredFilesPath;
    private final PeerService peer;
    private final String fileID;
    private int nChunks;

    public FileRestorer(PeerService peer, String filepath, String restoredFilesPath, String fileID) {
        this.peer = peer;
        this.filepath = filepath;
        this.restoredFilesPath = restoredFilesPath;
        this.fileID = fileID;
        nChunks = -1;
        restoredChunks = new ConcurrentHashMap<>();
    }

    public void processRestoredChunks(String chunkNo, byte[] chunkData) {
        if (!restoredChunks.containsKey(chunkNo)) {
            restoredChunks.put(chunkNo, chunkData);

            /* Is this the last chunk? */
            if (chunkData.length != PeerService.CHUNK_SIZE) {
                nChunks = Integer.parseInt(chunkNo) + 1;
            }

            /* Are all the chunks restored? */
            if (restoredChunks.size() == nChunks) {
                try {
                    restoreFile();
                    peer.markRestored(fileID);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Write the complete file when the chunks are restored
     *
     * @throws FileNotFoundException
     */
    private void restoreFile() throws FileNotFoundException {
        FileOutputStream chunkFile = new FileOutputStream(restoredFilesPath + "/" + filepath, true);

        for (int i = 0; i < restoredChunks.size(); i++) {
            try {
                chunkFile.write(restoredChunks.get(Integer.toString(i)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.format("File %s restored", filepath);
        System.out.println();
    }

    public ConcurrentHashMap<String, byte[]> getRestoredChunks() {
        return restoredChunks;
    }
}
