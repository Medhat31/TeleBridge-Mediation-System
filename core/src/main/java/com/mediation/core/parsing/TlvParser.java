package com.mediation.core.parsing;

import com.mediation.core.entities.Cdr;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses binary Call Detail Record (CDR) files formatted in Tag-Length-Value (TLV) hexadecimal encoding.
 * Converts raw machine data into structured Java objects.
 */
public class TlvParser {

    private static final Logger log = LoggerFactory.getLogger(TlvParser.class);

    /**
     * Reads a CDR file from the file system and decodes it line-by-line.
     * Skips empty or malformed lines without crashing the parser.
     *
     * @param file The file object pointing to the raw CDR data.
     * @return A list of successfully parsed and structured Cdr objects.
     */
    public List<Cdr> parseFile(File file) {
        List<Cdr> parsedRecords = new ArrayList<>();
        log.info("=================== CDR PARSER ==================");
        log.info("Opening file: {}", file.getName());

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                
                if (line.trim().isEmpty()) continue;
                
                Cdr cdr = decodeHexStream(line.trim());
                if (cdr != null) {
                    parsedRecords.add(cdr);
                }
            }
            log.info("  -> Successfully parsed {} records.", parsedRecords.size());
            
        } catch (Exception e) {
            log.error("[CRITICAL] Failed to parse file: {}", file.getName(), e);
        }

        return parsedRecords;
    }

    /**
     * State machine that slices a single continuous Hex TLV string into a Java Cdr Object.
     * Validates the master tag and iterates through the payload based on the data dictionary.
     *
     * @param hexStream The continuous string of hexadecimal characters representing a single record.
     * @return A populated Cdr object, or null if the string is invalid or malformed.
     */
    Cdr decodeHexStream(String hexStream) {
        Cdr cdr = new Cdr();
        int index = 0;

        try {
            String masterTag = hexStream.substring(index, index + 2);
            if (!masterTag.equals("A0")) {
                log.error("Invalid record found. Missing A0 Master Tag.");
                return null;
            }
            index += 2; 

            index += 2; 

            while (index < hexStream.length()) {
                
                String tag = hexStream.substring(index, index + 2);
                index += 2;

                int lengthInBytes = Integer.parseInt(hexStream.substring(index, index + 2), 16);
                int lengthInChars = lengthInBytes * 2;
                index += 2;

                String hexValue = hexStream.substring(index, index + lengthInChars);
                index += lengthInChars;

                switch (tag) {
                    case "01": cdr.setRecordId(hexToInt(hexValue)); break;
                    case "02": cdr.setRecordType(hexToInt(hexValue)); break;
                    case "03": cdr.setDialA(hexToAscii(hexValue)); break;
                    case "04": cdr.setDialB(hexToAscii(hexValue)); break;
                    case "05": cdr.setAnswerTime(hexToAscii(hexValue)); break;
                    case "06": cdr.setQuantity(hexToInt(hexValue)); break;
                    case "07": cdr.setCauseForTerm(hexToInt(hexValue)); break;
                    case "08": cdr.setCallDirection(hexToInt(hexValue)); break;
                    default: 
                        log.warn("  -> Unknown Tag found: {}. Skipping.", tag);
                        break;
                }
            }
            return cdr;
            
        } catch (Exception e) {
            log.error("Error decoding hex stream: {}", hexStream, e);
            return null;
        }
    }

    /**
     * Converts a hexadecimal string representing a number into a Java integer.
     * Uses Long.parseLong internally to prevent overflow errors.
     *
     * @param hex The hexadecimal string to convert.
     * @return The corresponding integer value.
     */
    private int hexToInt(String hex) {
        return (int) Long.parseLong(hex, 16);
    }

    /**
     * Converts a hexadecimal string representing ASCII text into a standard Java String.
     *
     * @param hex The hexadecimal string to convert.
     * @return The decoded ASCII text string.
     */
    private String hexToAscii(String hex) {
        StringBuilder ascii = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            ascii.append((char) Integer.parseInt(str, 16));
        }
        return ascii.toString();
    }
}