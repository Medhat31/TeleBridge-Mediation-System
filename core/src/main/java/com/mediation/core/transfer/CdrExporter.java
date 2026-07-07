package com.mediation.core.transfer;

import com.mediation.core.entities.Cdr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Handles the generation of CSV files from cleaned and mediated Call Detail Records (CDRs).
 * Transcribes internal structured Java objects into flat files for downstream consumption.
 */
public class CdrExporter {

    private static final Logger log = LoggerFactory.getLogger(CdrExporter.class);

    /**
     * Exports a list of cleaned CDRs into a newly generated CSV file.
     * Converts numeric mappings into human-readable strings (e.g., VOICE/SMS, MO/MT).
     *
     * @param cleanCdrs        The list of filtered CDRs ready for export.
     * @param outputDirPath    The local directory where the CSV file should be temporarily staged.
     * @param originalFilename The original name of the upstream CDR file.
     * @param destNodeName     The name of the target downstream node, used for file naming.
     * @return The File object pointing to the newly generated CSV file on disk.
     */
    public File exportToCsv(List<Cdr> cleanCdrs, String outputDirPath, String originalFilename, String destNodeName) {
        File outputDir = new File(outputDirPath);
        if (!outputDir.exists()) outputDir.mkdirs();

        String newFilename = "FORWARDED_" + destNodeName + "_" + originalFilename + ".csv";
        File outputFile = new File(outputDir, newFilename);

        try (FileWriter writer = new FileWriter(outputFile)) {
            writer.write("Type,Direction,A_Party,B_Party,Time,Duration,Cause\n");
            
            for (Cdr cdr : cleanCdrs) {
                String typeStr = (cdr.getRecordType() == 0) ? "VOICE" : "SMS";
                String dirStr = (cdr.getCallDirection() == 0) ? "MO" : "MT";

                writer.write(String.format("%s,%s,%s,%s,%s,%d,%d\n",
                        typeStr, dirStr, cdr.getDialA(), cdr.getDialB(), 
                        cdr.getAnswerTime(), cdr.getQuantity(), cdr.getCauseForTerm()));
            }
            log.info("  -> Exported {} records to {}", cleanCdrs.size(), newFilename);
        } catch (IOException e) {
            log.error("Failed to export CDRs to CSV.", e);
        }
        return outputFile;
    }
}