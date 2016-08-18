package com.saic.uicds.core.infrastructure.util;

import java.io.File;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.saic.uicds.core.xmldb.WorkProductXMLDBDAO;

public class TempCleaner
    extends TimerTask {

    private Logger log = LoggerFactory.getLogger(WorkProductXMLDBDAO.class);
    private String dirName = System.getProperty("java.io.tmpdir");
    private int ageInMinutes = 5;
    private File directory = null;

    /**
     * @return the dirName
     */
    public String getDirName() {

        return dirName;
    }

    /**
     * @param dirName the dirName to set
     */
    public void setDirName(String dirName) {

        this.dirName = dirName;
    }

    /**
     * @return the ageInMinutes
     */
    public int getAgeInMinutes() {

        return ageInMinutes;
    }

    /**
     * @param ageInMinutes the ageInMinutes to set
     */
    public void setAgeInMinutes(int ageInMinutes) {

        this.ageInMinutes = ageInMinutes;
    }

    @Override
    public void run() {

        // get temp dir from environment
        log.debug("Performing temp cleanup on " + dirName);
        directory = new File(dirName);

        if (directory.exists()) {
            // get list of files
            File[] files = directory.listFiles();

            // delete them if they are past the configured age in minutes
            long purgeTime = System.currentTimeMillis() - (ageInMinutes * 60 * 1000);
            for (File file : files) {
                if (file.lastModified() < purgeTime) {
                    if (!file.delete()) {
                        // Failed to delete file
                        log.debug("File " + file + " may be locked - will try again next pass.");
                    }
                }
            }
        } else {
            log.error("Directory configured for the temp file cleaner does not exist: " + dirName);
        }
    }
}