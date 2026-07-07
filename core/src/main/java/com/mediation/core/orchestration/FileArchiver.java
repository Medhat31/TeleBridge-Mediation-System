package com.mediation.core.orchestration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/**
 * Handles the secure archiving of original raw CDR files after they have been
 * successfully processed and distributed by the mediation pipeline.
 */
public class FileArchiver {

    private static final Logger log = LoggerFactory.getLogger(FileArchiver.class);

    /**
     * Moves a successfully processed raw CDR file from the staging input directory
     * into the permanent archive directory. Appends a '.processed' extension to mark completion.
     * Overwrites any existing file with the same name in the archive.
     *
     * @param processedFile  The raw file that was just successfully processed.
     * @param archiveDirPath The absolute path to the long-term storage directory.
     */
    public void archiveFile(File processedFile, String archiveDirPath) {
        try {
            File archiveDir = new File(archiveDirPath);
            if (!archiveDir.exists()) {
                archiveDir.mkdirs();
            }

            File destFile = new File(archiveDir.getAbsolutePath() + File.separator + processedFile.getName() + ".processed");
            
            Files.move(processedFile.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            log.info("  -> [ARCHIVE] Moved {} to archive.", processedFile.getName());
            
        } catch (Exception e) {
            log.warn("  -> [WARNING] Failed to archive file: {}", processedFile.getName(), e);
        }
    }
}