/*
 * Copyright 2021 Rudy De Busscher (https://www.atbash.be)
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
import be.atbash.runtime.core.module.RuntimeObjectsManager;
import be.atbash.runtime.logging.EnhancedLogRecord;
import be.atbash.runtime.logging.LoggingManager;
import be.atbash.runtime.logging.LoggingUtil;
import be.atbash.runtime.logging.handler.formatter.JSONLogFormatter;
import be.atbash.runtime.logging.handler.formatter.ODLLogFormatter;
import be.atbash.runtime.logging.handler.formatter.UniformLogFormatter;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.security.PrivilegedAction;
import java.text.FieldPosition;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Formatter;
import java.util.logging.*;
import java.util.zip.GZIPOutputStream;

import static java.security.AccessController.doPrivileged;

/**
 * LogFileHandler publishes formatted log Messages to a FILE.
 * Inspired by code of GlassFish
 */
public class LogFileHandler extends StreamHandler {

    private static final Logger LOGGER = Logger.getLogger(LogFileHandler.class.getName());

    private static final int DEFAULT_ROTATION_LIMIT_BYTES = 2000000;
    public static final int DISABLE_LOG_FILE_ROTATION_VALUE = 0;

    private static final String RECORD_BEGIN_MARKER = "[#|";
    private static final String RECORD_END_MARKER = "|#]";
    private static final String RECORD_FIELD_SEPARATOR = "|";
    private static final String RECORD_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSZ";
    public static final String INVALID_PROPERTY = "LOG010: An invalid value %s has been specified for the %s attribute in the logging configuration.";

    // This is a OutputStream to keep track of number of bytes
    // written out to the stream
    private MeteredStream meter;


    private static final String LOG_FILE_NAME = "runtime.log";
    private static final String GZIP_EXTENSION = ".gz";

    private String absoluteServerLogName = null;
    private File absoluteFile = null;
    private int flushFrequency = 1;
    private int maxHistoryFiles = 10;
    private boolean rotationOnDateChange;
    private String excludeFields;
    private Integer rotationLimitAttrValue;
    private Long rotationTimeLimitValue;
    private boolean compressionOnRotation;
    private boolean multiLineMode;
    private String fileHandlerFormatter = "";
    private String currentFileHandlerFormatter = "";
    private boolean logStandardStreams;

    private LoggingOutputStream stdoutOutputStream = null;
    private LoggingOutputStream stderrOutputStream = null;

    /**
     * Initially the LogRotation will be off until the domain.xml value is read.
     */
    private int limitForFileRotation = 0;

    private BlockingQueue<LogRecord> pendingRecords = new ArrayBlockingQueue<>(10000);

    /**
     * Rotation can be done in 3 ways: <ol>
     * <li> Based on the Size: Rotate when some Threshold number of bytes are
     * written to server.log </li>
     * <li> Based on the Time: Rotate ever 'n' minutes, mostly 24 hrs </li>
     * <li> Rotate now </li></ol>
     * For mechanisms 2 and 3 we will use this flag. The rotate() will always
     * be fired from the publish( ) method for consistency
     */
    private AtomicBoolean rotationRequested = new AtomicBoolean(false);

    private final Object rotationLock = new Object();

    private static final String LOG_ROTATE_DATE_FORMAT =
            "yyyy-MM-dd'T'HH-mm-ss";

    private static final String DEFAULT_LOG_FILE_FORMATTER_CLASS_NAME = UniformLogFormatter.class.getName();

    public static final int MINIMUM_ROTATION_LIMIT_VALUE = 500 * 1000;

    private Synchronizer synchronizer;

    private boolean dayBasedFileRotation = false;

    //private List<LogEventListener> logEventListeners = new ArrayList<>();

    private Thread pump;

    protected String logFileProperty = "";
    private final LogManager manager = LogManager.getLogManager();
    private final String className = getClass().getName();

    private static final String STDOUT_LOGGER_NAME = "javax.enterprise.logging.stdout";

    private static final String STDERR_LOGGER_NAME = "javax.enterprise.logging.stderr";

    public static final Logger STDOUT_LOGGER = Logger.getLogger(STDOUT_LOGGER_NAME);

    public static final Logger STDERR_LOGGER = Logger.getLogger(STDERR_LOGGER_NAME);


    private RuntimeConfiguration configuration;

    public LogFileHandler() {

        this.configuration = RuntimeObjectsManager.getInstance().getExposedObject(RuntimeConfiguration.class);
        postConstruct();
    }

    //private static final String GF_FILE_HANDLER = GFFileHandler.class.getCanonicalName() ;
    //private LogRecord logRecord = new LogRecord(Level.INFO, LogFacade.GF_VERSION_INFO);

    public void postConstruct() {

        String filename = evaluateFileName();

        File logFile = new File(filename);
        absoluteServerLogName = filename;
        if (!logFile.isAbsolute()) {
            logFile = new File(configuration.getLoggingDirectory(), filename);
            absoluteServerLogName = logFile.getAbsolutePath();
        }
        changeFileName(logFile);

        // Reading just few lines of log file to get the log formatter used.
        String strLine;
        int odlFormatter = 0;
        int uniformLogFormatter = 0;
        int jsonLogFormatter = 0;
        int otherFormatter = 0;
        boolean mustRotate = false;

        // FIXME
        /*
        try (BufferedReader br = new BufferedReader(new FileReader(logFile))) {
            while ((strLine = br.readLine()) != null) {
                strLine = strLine.trim();
                if (!strLine.equals("")) {
                    if (LogFormatHelper.isUniformFormatLogHeader(strLine)) {  // for ufl formatter
                        uniformLogFormatter++;
                    } else if (LogFormatHelper.isODLFormatLogHeader(strLine)) {
                        // for ODL formatter
                        odlFormatter++;
                    } else if (LogFormatHelper.isJSONFormatLogHeader(strLine)) {
                        //for JSON Log format
                        jsonLogFormatter++;
                    } else {
                        otherFormatter++;  // for other formatter
                    }

                    // Rotate on startup for custom log files
                    if (otherFormatter > 0) {
                        mustRotate = true;
                    }
                    // Read only first log record line and break out of the loop
                    break;
                }
            }
        } catch (Exception e) {
            ErrorManager em = getErrorManager();
            if (em != null) {
                em.error(e.getMessage(), e, ErrorManager.GENERIC_FAILURE);
            }
        }

        if (odlFormatter > 0) {
            currentFileHandlerFormatter = "com.sun.enterprise.server.logging.ODLLogFormatter";
        } else if (uniformLogFormatter > 0) {
            currentFileHandlerFormatter = "com.sun.enterprise.server.logging.UniformLogFormatter";
        } else if (jsonLogFormatter > 0) {
            currentFileHandlerFormatter = "fish.payara.enterprise.server.logging.JSONLogFormatter";
        }

         */
        initializePump();
        // FIXME Here the version number of the runtime would be logged

        String propertyValue = manager.getProperty(className + ".rotationOnDateChange");
        timeBasedRotation(propertyValue);

        propertyValue = manager.getProperty(className + ".rotationLimitInBytes");
        rotationOnFileSizeLimit(propertyValue);

        //setLevel(Level.ALL);
        propertyValue = manager.getProperty(className + ".flushFrequency");
        if (propertyValue != null) {
            try {
                flushFrequency = Integer.parseInt(propertyValue);
            } catch (NumberFormatException e) {
                LOGGER.warning(String.format(INVALID_PROPERTY, propertyValue, "flushFrequency"));
            }
        }
        if (flushFrequency <= 0) {
            flushFrequency = 1;
        }

        propertyValue = manager.getProperty(className + ".maxHistoryFiles");
        try {
            if (propertyValue != null) {
                maxHistoryFiles = Integer.parseInt(propertyValue);
            }
        } catch (NumberFormatException e) {
            LOGGER.warning(String.format(INVALID_PROPERTY, propertyValue, "maxHistoryFiles"));
        }

        if (maxHistoryFiles < 0) {
            maxHistoryFiles = 10;
        }

        propertyValue = manager.getProperty(className + ".compressOnRotation");
        compressionOnRotation = false;
        if (propertyValue != null) {
            compressionOnRotation = Boolean.parseBoolean(propertyValue);
        }

        propertyValue = manager.getProperty(className + ".logStandardStreams");
        if (propertyValue != null) {
            logStandardStreams = Boolean.parseBoolean(propertyValue);
            if (logStandardStreams) {
                logStandardStreams();
            }
        }

        String formatterName = manager.getProperty(className + ".formatter");
        formatterName = (formatterName == null) ? DEFAULT_LOG_FILE_FORMATTER_CLASS_NAME : formatterName;

        // Below snapshot of the code is used to rotate server.log file on startup. It is used to avoid different format
        // log messages logged under same server.log file.
        fileHandlerFormatter = formatterName;
        if (mustRotate) {  // FIXME  is always false at this point
            rotate();
        } else if (fileHandlerFormatter != null
                && !fileHandlerFormatter.equals(currentFileHandlerFormatter)) {
            rotate();
        }
        // FIXME
        //excludeFields = manager.getProperty(LogManagerService.EXCLUDE_FIELDS_PROPERTY);
        //multiLineMode = Boolean.parseBoolean(manager.getProperty(LogManagerService.MULTI_LINE_MODE_PROPERTY));
        excludeFields = "";
        multiLineMode = false;
        configureLogFormatter(formatterName, excludeFields, multiLineMode);

    }

    private void configureLogFormatter(String formatterName, String excludeFields, boolean multiLineMode) {
        if (UniformLogFormatter.class.getName().equals(formatterName)) {
            configureUniformLogFormatter(excludeFields, multiLineMode);
        } else if (ODLLogFormatter.class.getName().equals(formatterName)) {
            configureODLFormatter(excludeFields, multiLineMode);
        } else if (JSONLogFormatter.class.getName().equals(formatterName)) {
            configureJSONFormatter(excludeFields);
        } else {
            // Custom formatter is configured in logging.properties
            // Check if the user specified formatter is in play else
            // log an error message
            Formatter currentFormatter = this.getFormatter();
            if (currentFormatter == null || !currentFormatter.getClass().getName().equals(formatterName)) {
                Formatter formatter = findFormatterService(formatterName);
                if (formatter == null) {
                    LOGGER.warning(String.format("The formatter class %s could not be instantiated.", formatterName));
                    // Fall back to the GlassFish default
                    configureDefaultFormatter(excludeFields, multiLineMode);
                } else {
                    setFormatter(formatter);
                }
            }
        }

        formatterName = this.getFormatter().getClass().getName();
        LOGGER.warning(String.format("Server log file is using Formatter class: %s", formatterName));
    }

    private void timeBasedRotation(String propertyValue) {
        rotationOnDateChange = false;

        if (propertyValue != null) {
            rotationOnDateChange = Boolean.parseBoolean(propertyValue);
        }

        if (rotationOnDateChange) {
            rotationOnDateChange();
        } else {
            rotationTimeLimitValue = 0L;
            try {
                propertyValue = manager.getProperty(className + ".rotationTimelimitInMinutes");
                if (propertyValue != null) {
                    rotationTimeLimitValue = Long.parseLong(propertyValue);
                }
            } catch (NumberFormatException e) {
                LOGGER.warning(String.format(INVALID_PROPERTY, propertyValue, "rotationTimelimitInMinutes"));
            }
            rotationOnTimeLimit();
        }
    }

    private void rotationOnDateChange() {
        dayBasedFileRotation = true;
        rotationTimeLimitValue = 0L;

        int millisecondsInDay = 1000 * 60 * 60 * 24;
        Date date = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy");

        long systemTime = System.currentTimeMillis();
        String nextDate = dateFormat.format(date.getTime() + millisecondsInDay);
        Date nextDay;

        try {
            nextDay = dateFormat.parse(nextDate);
        } catch (ParseException e) {
            nextDay = new Date();

            LOGGER.warning(String.format("Failed to parse the date: %s", nextDate));
        }

        long nextDaySystemTime = nextDay.getTime();
        rotationTimeLimitValue = nextDaySystemTime - systemTime;

        // FIXME
         /*
        Runnable rotationTask = this::rotate;

        if (className.equals(GF_FILE_HANDLER)) {
            LogRotationTimer.getInstance().startTimer(
                    new LogRotationTimerTask(rotationTask,
                            rotationTimeLimitValue / 60000));
        } else {
            PayaraNotificationLogRotationTimer.getInstance().startTimer(
                    new LogRotationTimerTask(rotationTask,
                            rotationTimeLimitValue / 60000));
        }

          */
    }

    private void rotationOnTimeLimit() {
        if (rotationTimeLimitValue > 0) {
            Runnable rotationTask = () -> {
                rotate();
            };

            // FIXME
            /*
            if (className.equals(GF_FILE_HANDLER)) {
                LogRotationTimer.getInstance().startTimer(
                        new LogRotationTimerTask(rotationTask,
                                rotationTimeLimitValue));
            } else {
                PayaraNotificationLogRotationTimer.getInstance().startTimer(
                        new LogRotationTimerTask(rotationTask,
                                rotationTimeLimitValue));
            }

             */
        }
    }

    private void rotationOnFileSizeLimit(String propertyValue) {
        try {
            if (propertyValue != null) {
                rotationLimitAttrValue = Integer.parseInt(propertyValue);
            } else {
                rotationLimitAttrValue = DEFAULT_ROTATION_LIMIT_BYTES;
            }
        } catch (NumberFormatException e) {
            LOGGER.warning(String.format(INVALID_PROPERTY, propertyValue, "rotationLimitInBytes"));
        }
        // We set the LogRotation limit here. The rotation limit is the
        // Threshold for the number of bytes in the log file after which
        // it will be rotated.
        if (rotationLimitAttrValue >= MINIMUM_ROTATION_LIMIT_VALUE || rotationLimitAttrValue == DISABLE_LOG_FILE_ROTATION_VALUE) {
            setLimitForRotation(rotationLimitAttrValue);
        }
    }

    protected String evaluateFileName() {
        String cname = getClass().getName();
        LogManager manager = LogManager.getLogManager();

        logFileProperty = manager.getProperty(cname + ".file");
        if (logFileProperty == null || logFileProperty.trim().equals("")) {
            logFileProperty = configuration.getLoggingDirectory() + File.separator +
                    LOG_FILE_NAME;
        }

        return logFileProperty;
    }

    Formatter findFormatterService(String formatterName) {
        Formatter result;
        try {
            Object instance = Class.forName(formatterName).getDeclaredConstructor().newInstance();
            if (!(instance instanceof Formatter)) {
                throw new IncorrectConfigurationException(IncorrectConfigurationException.IncorrectConfigurationCode.CON_011
                        , String.format("Configured Log Formatter %s is not an instance of java.util.logging.Formatter", formatterName));

            }
            result = (Formatter) instance;
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException | ClassNotFoundException e) {
            throw new IncorrectConfigurationException(IncorrectConfigurationException.IncorrectConfigurationCode.CON_010
                    , String.format("Unable to instantiate the Log Formatter %s. Exception %s", formatterName, e.getMessage()));

        }

        return result;
    }

    private void configureDefaultFormatter(String excludeFields,
                                           boolean multiLineMode) {
        configureUniformLogFormatter(excludeFields, multiLineMode);
    }

    private void configureODLFormatter(String excludeFields, boolean multiLineMode) {
        // this loop is used for ODL formatter
        ODLLogFormatter formatterClass;
        // set the formatter

        formatterClass = new ODLLogFormatter(excludeFields);
        setFormatter(formatterClass);

        formatterClass.setMultiLineMode(multiLineMode);
        formatterClass.noAnsi();

    }

    private void configureUniformLogFormatter(String excludeFields, boolean multiLineMode) {
        LogManager manager = LogManager.getLogManager();
        String cname = getClass().getName();
        // this loop is used for UFL formatter
        UniformLogFormatter formatterClass;
        // set the formatter

        formatterClass = new UniformLogFormatter(excludeFields);

        formatterClass.setMultiLineMode(multiLineMode);

        formatterClass.noAnsi();
        String recordBeginMarker = manager.getProperty(cname + ".logFormatBeginMarker");
        if (recordBeginMarker == null || ("").equals(recordBeginMarker)) {
            recordBeginMarker = RECORD_BEGIN_MARKER;
        }

        String recordEndMarker = manager.getProperty(cname + ".logFormatEndMarker");
        if (recordEndMarker == null || ("").equals(recordEndMarker)) {
            recordEndMarker = RECORD_END_MARKER;
        }

        String recordFieldSeparator = manager.getProperty(cname + ".logFormatFieldSeparator");
        if (recordFieldSeparator == null || ("").equals(recordFieldSeparator) || recordFieldSeparator.length() > 1) {
            recordFieldSeparator = RECORD_FIELD_SEPARATOR;
        }

        String recordDateFormat = manager.getProperty(cname + ".logFormatDateFormat");
        if (recordDateFormat != null && !("").equals(recordDateFormat)) {
            SimpleDateFormat sdf = new SimpleDateFormat(recordDateFormat);
            try {
                sdf.format(new Date());
            } catch (Exception e) {
                recordDateFormat = RECORD_DATE_FORMAT;
            }
        } else {
            recordDateFormat = RECORD_DATE_FORMAT;
        }

        formatterClass.setRecordBeginMarker(recordBeginMarker);
        formatterClass.setRecordEndMarker(recordEndMarker);
        formatterClass.setRecordDateFormat(recordDateFormat);
        formatterClass.setRecordFieldSeparator(recordFieldSeparator);
    }

    void initializePump() {
        pump = new Thread() {
            @Override
            public void run() {
                synchronizer = new Synchronizer();
                while (!synchronizer.isSignalled()) {
                    try {
                        log();
                    } catch (Exception e) {
                        // GLASSFISH-19125
                        // Continue the loop without exiting
                    }
                }
                synchronizer.release();
            }
        };
        pump.setName("LogFileHandler log pump");
        pump.setDaemon(true);
        pump.start();
    }

    public void preDestroy() {
        // FIXME  When shutting down, make sure everything is cleanly closed
        // stop the Queue consumer thread.
        /*
        if (LogFacade.LOGGING_LOGGER.isLoggable(Level.FINE)) {
            LogFacade.LOGGING_LOGGER.fine("Logger handler killed");
        }

         */

        System.setOut(LoggingUtil.oStdOutBackup);
        System.setErr(LoggingUtil.oStdErrBackup);

        try {
            if (stdoutOutputStream != null) {
                stdoutOutputStream.close();
            }

            if (stderrOutputStream != null) {
                stderrOutputStream.close();
            }
        } catch (IOException ex) {
            throw new UnexpectedException(UnexpectedException.UnexpectedExceptionCode.UE001, ex);
        }

        synchronizer.raiseSignal(1, TimeUnit.SECONDS);  // Wait at max 1 sec

        if (pump != null) {
            pump.interrupt();
        }

        // drain and return all
        drainAllPendingRecords();
        flush();
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
            super.flush();
            super.close();
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


    /**
     * A simple getter to access the current log file written by
     * this FileHandler.
     */
    public File getCurrentLogFile() {
        return absoluteFile;
    }

    /**
     * A package private method to set the limit for File Rotation.
     */
    private synchronized void setLimitForRotation(int rotationLimitInBytes) {
        limitForFileRotation = rotationLimitInBytes;
    }

    private void configureJSONFormatter(String excludeFields) {
        // this loop is used for JSON formatter
        JSONLogFormatter formatterClass;
        // set the formatter

        formatterClass = new JSONLogFormatter(excludeFields);
        setFormatter(formatterClass);
    }

    /**
     * NOTE: This private class is copied from java.util.logging.FileHandler
     * A metered stream is a subclass of OutputStream that
     * (a) forwards all its output to a target stream
     * (b) keeps track of how many bytes have been written
     */
    private static final class MeteredStream extends OutputStream {

        private volatile boolean isOpen;

        OutputStream out;
        long written;

        MeteredStream(OutputStream out, long written) {
            this.out = out;
            this.written = written;
            isOpen = true;
        }

        @Override
        public void write(int b) throws IOException {
            out.write(b);
            written++;
        }

        @Override
        public void write(byte[] buff) throws IOException {
            out.write(buff);
            written += buff.length;
        }

        @Override
        public void write(byte[] buff, int off, int len) throws IOException {
            out.write(buff, off, len);
            written += len;
        }

        @Override
        public void flush() throws IOException {
            out.flush();
        }

        @Override
        public void close() throws IOException {
            if (isOpen) {
                isOpen = false;
                flush();
                out.close();
            }

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
     * Request Rotation called from Rotation Timer Task or LogMBean
     */
    void requestRotation() {
        rotationRequested.set(true);
    }

    /**
     * cleanup the history log file based on attributes set under logging.properties file".
     * <p/>
     * If it is defined with valid number, we only keep that number of history logfiles;
     * If "max_history_files" is defined without value, then default that number to be 10;
     * If "max_history_files" is defined with value 0, any number of history files are kept.
     */
    public void cleanUpHistoryLogFiles() {
        if (maxHistoryFiles == 0)
            return;

        synchronized (rotationLock) {
            File dir = absoluteFile.getParentFile();
            String logFileName = absoluteFile.getName();
            if (dir == null)
                return;
            File[] fset = dir.listFiles();
            List<String> candidates = new ArrayList<>();
            for (int i = 0; fset != null && i < fset.length; i++) {
                if (!logFileName.equals(fset[i].getName()) && fset[i].isFile()
                        && fset[i].getName().startsWith(logFileName)) {
                    candidates.add(fset[i].getAbsolutePath());
                }
            }
            if (candidates.size() <= maxHistoryFiles)
                return;
            Object[] paths = candidates.toArray();
            Arrays.sort(paths);
            try {
                for (int i = 0; i < paths.length - maxHistoryFiles; i++) {
                    File logFile = new File((String) paths[i]);
                    boolean delFile = logFile.delete();
                    if (!delFile) {
                        throw new IOException("Could not delete log file: "
                                + logFile.getAbsolutePath());
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
        final LogFileHandler thisInstance = this;
        doPrivileged((PrivilegedAction<Object>) () -> {
                    synchronized (thisInstance.rotationLock) {
                        if (thisInstance.meter != null
                                && thisInstance.meter.written <= 0) {
                            return null;
                        }
                        thisInstance.flush();
                        thisInstance.close();
                        try {
                            if (!absoluteFile.exists()) {
                                File creatingDeletedLogFile = new File(
                                        absoluteFile.getAbsolutePath());
                                if (creatingDeletedLogFile.createNewFile()) {
                                    absoluteFile = creatingDeletedLogFile;
                                }
                            } else {
                                File oldFile = absoluteFile;
                                StringBuffer renamedFileName = new StringBuffer(
                                        absoluteFile + "_");
                                new SimpleDateFormat(LOG_ROTATE_DATE_FORMAT)
                                        .format(new Date(), renamedFileName, new FieldPosition(0));
                                File rotatedFile = new File(renamedFileName.toString());
                                boolean renameSuccess = oldFile.renameTo(rotatedFile);
                                if (!renameSuccess) {
                                    // If we don't succeed with file rename which
                                    // most likely can happen on Windows because
                                    // of multiple file handles opened. We go through
                                    // Plan B to copy bytes explicitly to a renamed
                                    // file.
                                    // FIXME
                                    //FileUtils.copy(absoluteFile, rotatedFile);
                                    File freshServerLogFile = getLogFileName();
                                    // We do this to make sure that server.log
                                    // contents are flushed out to start from a
                                    // clean file again after the rename..
                                    FileOutputStream fo = new FileOutputStream(
                                            freshServerLogFile);
                                    fo.close();
                                }
                                FileOutputStream oldFileFO = new FileOutputStream(oldFile);
                                oldFileFO.close();
                                openFile(getLogFileName());
                                absoluteFile = getLogFileName();
                                // This will ensure that the log rotation timer
                                // will be restarted if there is a value set
                                // for time based log rotation
                                restartTimeBasedLogRotation();
                                if (compressionOnRotation) {
                                    boolean compressed = gzipFile(rotatedFile);
                                    if (compressed) {
                                        boolean deleted = rotatedFile.delete();
                                        if (!deleted) {
                                            throw new IOException("Could not delete uncompressed log file: "
                                                    + rotatedFile.getAbsolutePath());
                                        }
                                    } else {
                                        throw new IOException("Could not compress log file: "
                                                + rotatedFile.getAbsolutePath());
                                    }
                                }

                                cleanUpHistoryLogFiles();
                            }
                        } catch (IOException ix) {
                            new ErrorManager().error("Error, could not rotate log file", ix, ErrorManager.GENERIC_FAILURE);
                        }
                        return null;
                    }
                }
        );
    }

    private void restartTimeBasedLogRotation() {
        // FIXME rotation
        /*
        if (dayBasedFileRotation) {
            if (className.equals(GF_FILE_HANDLER)) {
                LogRotationTimer.getInstance()
                        .restartTimerForDayBasedRotation();
            } else {
                PayaraNotificationLogRotationTimer.getInstance()
                        .restartTimerForDayBasedRotation();
            }
        } else {
            if (className.equals(GF_FILE_HANDLER)) {
                LogRotationTimer.getInstance()
                        .restartTimer();
            } else {
                PayaraNotificationLogRotationTimer.getInstance()
                        .restartTimer();
            }
        }


         */
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
        if ((rotationRequested.get())
                || ((limitForFileRotation > 0)
                && (meter.written >= limitForFileRotation))) {
            // If we have written more than the limit set for the
            // file, or rotation requested from the Timer Task or LogMBean
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
        // ***
        // PAYARA-406 Check if the LogRecord passed in is already a GFLogRecord,
        // and just cast the passed record if it is
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

    protected File getLogFileName() {
        return new File(absoluteServerLogName);

    }

    private boolean gzipFile(File infile) {

        boolean status = false;

        try (
                FileInputStream fis = new FileInputStream(infile);
                FileOutputStream fos = new FileOutputStream(infile.getCanonicalPath() + GZIP_EXTENSION);
                GZIPOutputStream gzos = new GZIPOutputStream(fos)
        ) {
            byte[] buffer = new byte[1024];
            int len;
            while ((len = fis.read(buffer)) != -1) {
                gzos.write(buffer, 0, len);
            }
            gzos.finish();

            status = true;

        } catch (IOException ix) {
            new ErrorManager().error("Error gzipping log file", ix, ErrorManager.GENERIC_FAILURE);
        }

        return status;
    }

    private void logStandardStreams() {

        // redirect stderr and stdout, a better way to do this
        //http://blogs.sun.com/nickstephen/entry/java_redirecting_system_out_and

        Logger _ologger = STDOUT_LOGGER;
        stdoutOutputStream = new LoggingOutputStream(_ologger, Level.INFO);
        LoggingOutputStream.LoggingPrintStream pout = stdoutOutputStream.new LoggingPrintStream(stdoutOutputStream);
        System.setOut(pout);

        Logger _elogger = STDERR_LOGGER;
        stderrOutputStream = new LoggingOutputStream(_elogger, Level.SEVERE);
        LoggingOutputStream.LoggingPrintStream perr = stderrOutputStream.new LoggingPrintStream(stderrOutputStream);
        System.setErr(perr);
    }

    public synchronized void setLogFile(String fileName) {
        String logFileName = fileName;
        File logFile = new File(logFileName);
        if (!logFile.isAbsolute()) {
            logFile = new File(configuration.getLoggingDirectory(), logFileName);
        }
        changeFileName(logFile);
        absoluteServerLogName = logFileName;
    }

    public synchronized void setRotationOnDateChange(boolean rotationOnDateChange) {
        this.rotationOnDateChange = rotationOnDateChange;
        restartTimeBasedLogRotation();
        rotationOnDateChange();
    }

    public synchronized void setMultiLineMode(boolean multiLineMode) {
        this.multiLineMode = multiLineMode;
        // Rotate log file to avoid different log format to be displayed in same log file
        rotate();
        configureLogFormatter(fileHandlerFormatter, excludeFields, multiLineMode);
    }

    public synchronized void setFileHandlerFormatter(String fileHandlerFormatter) {
        this.fileHandlerFormatter = fileHandlerFormatter;
        // Rotate log file to avoid different log format to be displayed in same log file
        rotate();
        configureLogFormatter(fileHandlerFormatter, excludeFields, multiLineMode);
    }

    public synchronized void setExcludeFields(String excludeFields) {
        this.excludeFields = excludeFields;
        // Rotate log file to avoid different log format to be displayed in same log file
        rotate();
        configureLogFormatter(fileHandlerFormatter, excludeFields, multiLineMode);
    }

    public synchronized void setRotationLimitAttrValue(Integer rotationLimitAttrValue) {
        this.rotationLimitAttrValue = rotationLimitAttrValue;
        // We set the LogRotation limit here. The rotation limit is the
        // Threshold for the number of bytes in the log file after which
        // it will be rotated.
        if (rotationLimitAttrValue >= MINIMUM_ROTATION_LIMIT_VALUE || rotationLimitAttrValue == DISABLE_LOG_FILE_ROTATION_VALUE) {
            setLimitForRotation(rotationLimitAttrValue);
        }
    }

    public synchronized void setRotationTimeLimitValue(Long rotationTimeLimitValue) {
        this.rotationTimeLimitValue = rotationTimeLimitValue;
        restartTimeBasedLogRotation();
        rotationOnTimeLimit();
    }

    public synchronized void setMaxHistoryFiles(int maxHistoryFiles) {
        this.maxHistoryFiles = maxHistoryFiles;
    }

    public synchronized void setFlushFrequency(int flushFrequency) {
        this.flushFrequency = flushFrequency;
    }

    public synchronized void setCompressionOnRotation(boolean compressionOnRotation) {
        this.compressionOnRotation = compressionOnRotation;
    }

    synchronized void setLogStandardStreams(boolean logStandardStreams) {
        this.logStandardStreams = logStandardStreams;

        if (logStandardStreams) {
            logStandardStreams();
        } else {
            System.setOut(LoggingUtil.oStdOutBackup);
            System.setErr(LoggingUtil.oStdErrBackup);
        }
    }
}
