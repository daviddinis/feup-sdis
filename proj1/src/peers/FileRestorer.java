package peers;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

class FileRestorer {

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

    /**
     * File Restorer, responsible for aggregating the chunks and restoring a given file
     *
     * @param peer              peer this restorer belongs to
     * @param filepath          path of the file to restore
     * @param restoredFilesPath directory where the restored file will be placed
     * @param fileID            id of the file to be restored
     */
    FileRestorer(PeerService peer, String filepath, String restoredFilesPath, String fileID) {
        this.peer = peer;
        this.filepath = filepath;
        this.restoredFilesPath = restoredFilesPath;
        this.fileID = fileID;
        nChunks = -1;
        restoredChunks = new ConcurrentHashMap<>();
    }

    /**
     * Checks if the processed chunk is the last one and, if so, restore the file
     *
     * @param chunkNo   chunk number
     * @param chunkData chunk content
     */
    void processRestoredChunks(String chunkNo, byte[] chunkData) {
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

    /**
     * @return chunks that have already been restored
     */
    ConcurrentHashMap<String, byte[]> getRestoredChunks() {
        return restoredChunks;
    }
}
