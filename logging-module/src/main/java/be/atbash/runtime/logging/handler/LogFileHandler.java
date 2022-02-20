/*
 * Copyright 2021-2022 Rudy De Busscher (https://www.atbash.be)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package be.atbash.runtime.logging.handler;

import be.atbash.runtime.core.data.RuntimeConfiguration;
import be.atbash.runtime.core.data.exception.IncorrectConfigurationException;
import be.atbash.runtime.core.data.exception.UnexpectedException;
import be.atbash.runtime.core.data.module.event.EventManager;
import be.atbash.runtime.core.data.module.event.EventPayload;
import be.atbash.runtime.core.data.module.event.ModuleEventListener;
import be.atbash.runtime.core.data.util.FileUtil;
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.logging.EnhancedLogRecord;
import be.atbash.runtime.logging.handler.formatter.JSONLogFormatter;
import be.atbash.runtime.logging.handler.formatter.ODLLogFormatter;
import be.atbash.runtime.logging.handler.formatter.UniformLogFormatter;
import be.atbash.runtime.logging.handler.rotation.RotationTimerUtil;
import be.atbash.runtime.logging.util.LogUtil;
import be.atbash.util.reflection.ClassUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Formatter;
import java.util.logging.*;

import static be.atbash.runtime.core.data.module.event.Events.LOGGING_UPDATE;

/**
 * LogFileHandler publishes formatted log Messages to a FILE.
 * Inspired by code of GlassFish
 */
public class LogFileHandler extends StreamHandler implements ModuleEventListener {
    // This class is referenced by name in a constant of be.atbash.runtime.AtbashRuntimeConstant.
    // If name or package changes, make sure the String constant is also changed.

    private static final Logger LOGGER = Logger.getLogger(LogFileHandler.class.getName());

    private static final String LOG_FILE_NAME = "runtime.log";

    private final BlockingQueue<LogRecord> pendingRecords = new ArrayBlockingQueue<>(10000);

    // This is a OutputStream to keep track of number of bytes
    // written out to the stream
    private MeteredStream meter;


    private static final String LOG_ROTATE_DATE_FORMAT = "yyyy-MM-dd'T'HH-mm-ss";
    private static final DateFormat ROTATE_FILE_DATE_FORMAT = new SimpleDateFormat(LOG_ROTATE_DATE_FORMAT);

    private static final String DEFAULT_LOG_FILE_FORMATTER_CLASS_NAME = UniformLogFormatter.class.getName();

    private Synchronizer synchronizer;

    private Thread pump;

    private int flushFrequency = 1;
    private int maxHistoryFiles = 10;

    private boolean compressionOnRotation;

    // rotation
    /**
     * Rotation can be done in 3 ways: <ol>
     * <li> Based on the Size: Rotate when some Threshold number of bytes are
     * written to server.log </li>
     * <li> Based on the Time: Rotate ever 'n' minutes, mostly 24 hrs </li>
     * <li> Rotate based on file size </li></ol>
     * <li> Rotate now (FIXME CLI command to be created)</li></ol>
     */
    private final AtomicBoolean rotationRequested = new AtomicBoolean(false);
    private boolean rotationOnDateChange;
    private int limitForFileRotation;  // file size in bytes when rotation will happen.
    private Long rotationTimeLimitValue;

    private final Object rotationLock = new Object();

    private static final int MINIMUM_ROTATION_LIMIT_VALUE = 500 * 1000;
    private static final int DEFAULT_ROTATION_LIMIT_BYTES = 2000000;
    private static final int DISABLE_LOG_FILE_ROTATION_ROTATION_LIMIT_VALUE = 0;
    private RotationTimerUtil rotationTimerUtil;


    // location of the log file
    private File absoluteFile = null;

    private final RuntimeConfiguration configuration;

    public LogFileHandler() {
        // Instantiated by Java Util LogManager and thus need to retrieve  RuntimeConfiguration through RuntimeObjectsManager.
        this.configuration = RuntimeObjectsManager.getInstance().getExposedObject(RuntimeConfiguration.class);
        postConstruct();
    }

    public void postConstruct() {

        File logFile = determineLogFileName();

        changeFileName(logFile);

        initializePump();

        initializeTimeBasedRotation();

        initializeRotationOnFileSizeLimit();

        initializeFlushFrequency();

        initializeMaxHistoryFiles();

        initializeCompressionOnRotation();

        // Always rotate at startup or when configuration changes!
        rotate();

        Optional<String> formatterName = LogUtil.getStringProperty(LogUtil.getLogPropertyKey("formatter"));
        String fileHandlerFormatter = formatterName.orElse(DEFAULT_LOG_FILE_FORMATTER_CLASS_NAME);
        configureLogFormatter(fileHandlerFormatter);

        EventManager.getInstance().registerListener(this);
    }

    private File determineLogFileName() {
        String filename = getLogFileName();

        // determine absolute name
        File logFile = new File(filename);
        if (!logFile.isAbsolute()) {
            logFile = new File(configuration.getLoggingDirectory(), filename);
        }
        return logFile;
    }

    private String getLogFileName() {
        Optional<String> fileName = LogUtil.getStringProperty(LogUtil.getLogPropertyKey("file"));
        return fileName.orElseGet(() -> configuration.getLoggingDirectory() + File.separator + LOG_FILE_NAME);
    }

    private void configureLogFormatter(String formatterName) {

        String excludeFields = LogUtil.getStringProperty(LogUtil.getLogPropertyKey("excludeFields")).orElse("");

        Formatter formatter;

        // We do check based on the names since it has the excludeFields fields support.
        // We might look for a better solution.
        if (UniformLogFormatter.class.getName().equals(formatterName)) {
            formatter = new UniformLogFormatter(excludeFields);
        } else if (ODLLogFormatter.class.getName().equals(formatterName)) {
            formatter = new ODLLogFormatter(excludeFields);
        } else if (JSONLogFormatter.class.getName().equals(formatterName)) {
            formatter = new JSONLogFormatter(excludeFields);

        } else {
            formatter = findFormatter(formatterName);

        }
        setFormatter(formatter);
        formatterName = formatter.getClass().getName();
        LOGGER.warning(String.format("Server log file is using Formatter class '%s'", formatterName));
    }

    private void initializeTimeBasedRotation() {
        rotationOnDateChange = LogUtil.getBooleanProperty(LogUtil.getLogPropertyKey("rotationOnDateChange"), false);

        if (rotationOnDateChange) {
            rotationOnDateChange();
        } else {
            rotationTimeLimitValue = LogUtil.getLongProperty(LogUtil.getLogPropertyKey("rotationTimelimitInMinutes"), 0L);
            rotationOnTimeLimit();
        }
    }


    private void rotationOnDateChange() {
        rotationTimeLimitValue = 0L;

        // calculate number of minutes until end of day.
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endOfDay = LocalDate.now().atStartOfDay().plusDays(1);
        long minutes = ChronoUnit.MINUTES.between(now, endOfDay);

        cancelOldTimer();
        rotationTimerUtil = new RotationTimerUtil(this::rotate, minutes);

    }

    private void rotationOnTimeLimit() {
        if (rotationTimeLimitValue > 0) {
            cancelOldTimer();
            rotationTimerUtil = new RotationTimerUtil(this::rotate, rotationTimeLimitValue);
        }
    }

    private void cancelOldTimer() {
        if (rotationTimerUtil != null) {
            rotationTimerUtil.cancelExecutor();
        }
    }

    private void initializeRotationOnFileSizeLimit() {
        int rotationLimitAttrValue = LogUtil.getIntProperty(LogUtil.getLogPropertyKey("rotationLimitInBytes"), DEFAULT_ROTATION_LIMIT_BYTES);

        // We set the LogRotation limit here. The rotation limit is the
        // Threshold for the number of bytes in the log file after which
        // it will be rotated.
        if (rotationLimitAttrValue >= MINIMUM_ROTATION_LIMIT_VALUE || rotationLimitAttrValue == DISABLE_LOG_FILE_ROTATION_ROTATION_LIMIT_VALUE) {
            setLimitForRotation(rotationLimitAttrValue);
        } else {
            LOGGER.warning(String.format("LOG-103: Invalid value for the `rotationLimitInBytes` option : '%s", rotationLimitAttrValue));
        }
    }

    private void initializeFlushFrequency() {
        flushFrequency = LogUtil.getIntProperty(LogUtil.getLogPropertyKey("flushFrequency"), 1);
        if (flushFrequency <= 0) {
            LOGGER.warning(String.format("LOG-104: Invalid value for `flushFrequency` (should be larger than 0). Default value 1 is used: '%s'", flushFrequency));
            flushFrequency = 1;
        }
    }

    private void initializeMaxHistoryFiles() {
        maxHistoryFiles = LogUtil.getIntProperty(LogUtil.getLogPropertyKey("maxHistoryFiles"), 10);

        if (maxHistoryFiles < 0) {
            LOGGER.warning(String.format("LOG-105: Invalid value for `maxHistoryFiles` (should be larger than 0). Default value 10 is used:: '%s'", maxHistoryFiles));
            maxHistoryFiles = 10;
        }
    }

    private void initializeCompressionOnRotation() {
        compressionOnRotation = LogUtil.getBooleanProperty(LogUtil.getLogPropertyKey("compressOnRotation"), false);
    }

    private Formatter findFormatter(String formatterName) {
        Formatter result;
        if (!ClassUtils.isAvailable(formatterName)) {
            throw new IncorrectConfigurationException(IncorrectConfigurationException.IncorrectConfigurationCode.CON_010
                    , String.format("Unable to instantiate the Log Formatter '%s'", formatterName));

        }

        Object instance = ClassUtils.newInstance(formatterName);
        if (!(instance instanceof Formatter)) {
            throw new IncorrectConfigurationException(IncorrectConfigurationException.IncorrectConfigurationCode.CON_011
                    , String.format("Configured Log Formatter '%s' is not an instance of java.util.logging.Formatter", formatterName));

        }
        result = (Formatter) instance;
        return result;
    }

    void initializePump() {
        pump = new Thread(() -> {
            synchronizer = new Synchronizer();
            while (!synchronizer.isSignalled()) {
                try {
                    log();
                } catch (Exception e) {
                    // Continue the loop without exiting
                }
            }
            synchronizer.release();
        });
        pump.setName("LogFileHandler log pump");
        pump.setDaemon(true);
        pump.start();
    }

    public void preDestroy() {
        // stop the Queue consumer thread.
        reset();
    }

    private void drainAllPendingRecords() {
        drainPendingRecords(0);
    }

    /**
     * Drains the amount of {@link LogRecord}s in the pending records queue.
     * If passed in the amount <= 0 all of the records get drained.
     *
     * @param flushAmount number of records to drain from the queue of pending records.
     */
    private void drainPendingRecords(int flushAmount) {
        if (!pendingRecords.isEmpty()) {
            Collection<LogRecord> records;
            if (flushAmount > 0) {
                records = new ArrayList<>(flushAmount);
                pendingRecords.drainTo(records, flushAmount);
            } else {
                records = new ArrayList<>(pendingRecords.size());
                pendingRecords.drainTo(records);
            }
            for (LogRecord record : records) {
                super.publish(record);
            }
        }
    }

    /**
     * This method is invoked from LogManager.reInitializeLoggers() to
     * change the location of the file.
     */
    void changeFileName(File file) {
        // If the file name is same as the current file name, there
        // is no need to change the filename
        if (file.equals(absoluteFile)) {
            return;
        }
        synchronized (rotationLock) {
            super.close();  // performs also flush
            try {
                openFile(file);
                absoluteFile = file;
            } catch (IOException ix) {
                new ErrorManager().error(
                        "FATAL ERROR: COULD NOT OPEN LOG FILE. " +
                                "Please Check to make sure that the directory for " +
                                "Logfile exists. Currently reverting back to use the " +
                                " default server.log", ix, ErrorManager.OPEN_FAILURE);
                try {
                    // Reverting back to the old server.log
                    openFile(absoluteFile);
                } catch (Exception e) {
                    new ErrorManager().error(
                            "FATAL ERROR: COULD NOT RE-OPEN SERVER LOG FILE. ", e,
                            ErrorManager.OPEN_FAILURE);
                }
            }
        }
    }

    @Override
    public void onEvent(EventPayload eventPayload) {
        if (LOGGING_UPDATE.equals(eventPayload.getEventCode())) {
            updateForLoggingConfigurationChanges();
        }
    }

    private void updateForLoggingConfigurationChanges() {
        reset();

        try {
            LogManager.getLogManager().readConfiguration();
        } catch (IOException e) {
            // FIXME What happens with the Runtime process. Is this captured and logged somewhere?
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, e);
        }

        // As a new instance of LogFileHandler will be cretaed and that one needs to be registered as listener at that point.
        EventManager.getInstance().unregisterListener(this);
    }

    private void reset() {
        // drain and return all
        drainAllPendingRecords();

        if (pump != null) {
            synchronizer.raiseSignal(1, TimeUnit.SECONDS);  // Wait at max 1 sec
        }

        flush();
    }

    /**
     * A synchronized method to set the limit for File Rotation.
     */
    private synchronized void setLimitForRotation(int rotationLimitInBytes) {
        limitForFileRotation = rotationLimitInBytes;
    }


    private synchronized void determineRotationOnFileSizeLimit() {
        if (limitForFileRotation > 0 && (meter.getBytesWritten() >= limitForFileRotation)) {
            rotationRequested.set(true);
        }
    }

    /**
     * Creates the file and initialized MeteredStream and passes it on to
     * Superclass (java.util.logging.StreamHandler).
     */
    private void openFile(File file) throws IOException {
        // check that the parent directory exists.
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IOException("parent.dir.create.failed" +
                    String.format("Failed to create the parent dir %s", parent.getAbsolutePath()));
        }
        FileOutputStream fout = new FileOutputStream(file, true);
        BufferedOutputStream bout = new BufferedOutputStream(fout);
        meter = new MeteredStream(bout, file.length());
        setOutputStream(meter);
    }

    /**
     * cleanup the history log file based on attributes set under logging.properties file".
     */
    public void cleanUpHistoryLogFiles() {
        if (maxHistoryFiles == 0)
            return;

        synchronized (rotationLock) {
            File dir = absoluteFile.getParentFile();
            if (dir == null) {
                return;
            }

            // Get a list of all files in the directory.
            String logFileName = absoluteFile.getName();
            File[] allFiles = dir.listFiles();
            List<String> logFiles = new ArrayList<>();
            for (int i = 0; allFiles != null && i < allFiles.length; i++) {
                if (!logFileName.equals(allFiles[i].getName())
                        && allFiles[i].isFile()
                        && allFiles[i].getName().startsWith(logFileName)) {
                    logFiles.add(allFiles[i].getAbsolutePath());
                }
            }
            if (logFiles.size() <= maxHistoryFiles) {
                return;
            }

            Collections.sort(logFiles);
            try {
                for (int i = 0; i < logFiles.size() - maxHistoryFiles; i++) {
                    File logFile = new File(logFiles.get(i));
                    boolean deleted = logFile.delete();
                    if (!deleted) {
                        LOGGER.warning(String.format("LOG-004: Unable to delete log file '%s'", logFile.getAbsolutePath()));
                    }
                }
            } catch (Exception e) {
                new ErrorManager().error(
                        "FATAL ERROR: COULD NOT DELETE LOG FILE.", e,
                        ErrorManager.GENERIC_FAILURE);
            }
        }
    }


    /**
     * A Simple rotate method to close the old file and start the new one
     * when the limit is reached.
     */
    public void rotate() {

        synchronized (rotationLock) {
            if (meter != null && meter.getBytesWritten() <= 0) {
                // Rotation requested with empty file -> ignore rotate
                return;
            }

            // We could also do meter.close  but since the meteredStream just forward, we don't need to close it explicitly
            // and close() handles the Exceptions for us.
            close();  // Performs a flush
            try {

                File oldFile = absoluteFile;
                String renamedFileName = defineRenamedFileName();
                File rotatedFile = new File(renamedFileName);
                boolean renameSuccess = oldFile.renameTo(rotatedFile);

                if (!renameSuccess) {
                    // If we don't succeed with file rename which
                    // most likely can happen on Windows because
                    // of multiple file handles opened. We go through
                    // Plan B to copy bytes explicitly to a renamed
                    // file.
                    Files.copy(absoluteFile.toPath(), rotatedFile.toPath(), StandardCopyOption.COPY_ATTRIBUTES);
                }

                // This clears out the server log file
                FileOutputStream oldFileFO = new FileOutputStream(oldFile);
                oldFileFO.close();

                // Open the log file again (with a MeteredStream)
                openFile(absoluteFile);

                // This will ensure that the log rotation timer
                // will be restarted if there is a value set
                // for time based log rotation
                restartTimeBasedLogRotation();

                compressRotatedFile(rotatedFile);

                cleanUpHistoryLogFiles();

            } catch (IOException ix) {
                new ErrorManager().error("Error, could not rotate log file", ix, ErrorManager.GENERIC_FAILURE);
            }

        }

    }

    private void compressRotatedFile(File rotatedFile) throws IOException {
        if (compressionOnRotation) {
            boolean compressed = FileUtil.gzipFile(rotatedFile);
            if (compressed) {
                boolean deleted = rotatedFile.delete();
                if (!deleted) {
                    // FIXME should we throw exception? a bit harsh.
                    throw new IOException("Could not delete uncompressed log file: " + rotatedFile.getAbsolutePath());
                }
            } else {
                // FIXME should we throw exception? a bit harsh.
                throw new IOException("Could not compress log file: " + rotatedFile.getAbsolutePath());
            }
        }
    }

    private String defineRenamedFileName() {
        // This method is called within a Synchronized block on ROTATION_LOCK, so it is safe to use a static SimpleDateFormat instance.
        return absoluteFile + "_" + ROTATE_FILE_DATE_FORMAT.format(new Date());
    }

    private void restartTimeBasedLogRotation() {
        if (rotationTimerUtil == null) {
            // No time based rotation in effect, no restart required.
            return;
        }
        if (rotationOnDateChange) {
            rotationTimerUtil.restartTimerForDayBasedRotation();
        } else {
            rotationTimerUtil.restartTimer();
        }
    }

    /**
     * 5005
     * Retrieves the LogRecord from our Queue and store them in the file
     */
    public void log() {

        LogRecord record;

        // take is blocking so we take one record off the queue
        try {
            record = pendingRecords.take();
            super.publish(record);
        } catch (InterruptedException e) {
            return;
        }

        if (flushFrequency > 1) {
            // now try to read more.  we end up blocking on the above take call if nothing is in the queue
            drainPendingRecords(flushFrequency - 1);
        }

        flush();
        determineRotationOnFileSizeLimit();
        if (rotationRequested.get()) {
            // If we have written more than the limit set for the
            // file, or rotation requested from the Timer Task
            // start fresh with a new file after renaming the old file.
            synchronized (rotationLock) {
                rotate();
                rotationRequested.set(false);
            }
        }

    }

    /**
     * Publishes the logrecord storing it in our queue
     */
    @Override
    public void publish(LogRecord record) {

        // the queue has shutdown, we are not processing any more records
        if (synchronizer.isSignalled()) {
            return;
        }

        // JUL LogRecord does not capture thread-name. Create a wrapper to
        // capture the name of the logging thread so that a formatter can
        // output correct thread-name if done asynchronously. Note that
        // this fix is limited to records published through this handler only.
        EnhancedLogRecord wrappedRecord = EnhancedLogRecord.wrap(record, true);


        try {
            pendingRecords.add(wrappedRecord);
        } catch (IllegalStateException e) {
            // queue is full, start waiting.
            new ErrorManager().error("LogFileHandler: Queue full. Waiting to submit.", e, ErrorManager.GENERIC_FAILURE);
            try {
                pendingRecords.put(wrappedRecord);
            } catch (InterruptedException e1) {
                // too bad, record is lost...
                new ErrorManager().error("LogFileHandler: Waiting was interrupted. Log record lost.", e1, ErrorManager.GENERIC_FAILURE);
            }
        }


    }

}
