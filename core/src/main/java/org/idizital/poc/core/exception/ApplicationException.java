package org.idizital.poc.core.exception;

import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.Serializable;
import java.io.StringWriter;

import org.idizital.poc.core.util.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ApplicationException extends Exception implements Serializable {

    // Attribute serialVersionID
    private static final long serialVersionUID = 1L;

    // Separator Constant for Display Purpose
    private static final String SEPERATOR = "**********************************************************************";

    // Constant for UniqueID File Name
    private static final String UNIQUE_ID_FNAME = "casa_error_unique_id.dat";

    // Unique ID Value
    static long lnUniqueId = 0;

    // Attribute errorMessage
    private String errorMessage = null;

    // Attribute displayerrorMessage
    private String displayerrorMessage = null;

    // Attribute applicationDebugMessage
    private String applicationDebugMessage = null;

    // Attribute chainedException
    private Exception chainedException = null;

    // Attribute exceptionName
    private String exceptionName = null;

    // Attribute exceptionStackTrace
    private String exceptionStackTrace = null;

    // Prevents re-wrapping the same exception
    boolean sameException = false;

    private static Logger logger = LoggerFactory.getLogger(ApplicationException.class);

    /**
     * Over-ridden Constructor
     *
     * @param errorMessage
     *            containing the Error Code for the Exception being created
     */
    public ApplicationException(String errorMessage) {
        this.errorMessage = errorMessage;
        initialise();
        printStackTrace();
    }

    /**
     * Over-ridden Constructor
     *
     * @param errorMessage
     *            containing the Error Code for the Exception being created
     * @param applicationDebugMessage
     *            containing the Application Debug Message
     */
    public ApplicationException(String errorMessage, String applicationDebugMessage) {
        this.errorMessage = errorMessage;
        this.applicationDebugMessage = applicationDebugMessage;
        initialise();
        printStackTrace();
    }

    /**
     * Over-ridden Constructor
     *
     * @param errorMessage
     *            containing the Error Code for the Exception being created
     * @param chainedException
     *            containing the Chained Exception object instance
     */
    public ApplicationException(String errorMessage, Exception chainedException) {
        processChainedException(errorMessage, null, chainedException);
        initialise();
        if (!sameException) {
            printStackTrace();
        }
    }

    /**
     * Over-ridden Constructor
     *
     * @param errorMessage
     *            containing the Error Code for the Exception being created
     * @param applicationDebugMessage
     *            containing the Application Debug Message
     * @param chainedException
     *            containing the Chained Exception object instance
     */
    public ApplicationException(String errorMessage, String applicationDebugMessage, Exception chainedException) {
        processChainedException(errorMessage, applicationDebugMessage, chainedException);
        initialise();
        if (!sameException) {
            printStackTrace();
        }
    }


    /**
     * Method to get Display Error Code
     *
     * @return String containing the value of displayerrorMessage
     */
    public String getDisplayerrorMessage() {
        return displayerrorMessage;
    }

    /**
     * Get the actual error code
     *
     * @return the actual errorMessage
     */
    public String getErrorMessage() {
        return errorMessage;
    }


    /**
     * Get the actual error code
     *
     * @return the actual errorMessage
     */
    @Override
    public String getMessage() {
        return getErrorMessage();
    }

    /**
     * Method to get Application Debug Message
     *
     * @return String containing the value of applicationDebugMessage
     */
    public String getApplicationDebugMessage() {
        return applicationDebugMessage;
    }

    /**
     * Method to get Chained Exception
     *
     * @return Exception containing the value of chainedException
     */
    public Exception getChainedException() {
        return chainedException;
    }

    /**
     * Over-ridden method to Trace the Exception
     */
    public void printStackTrace() {
        logger.error(toString());
    }

    public String getExceptionStackTrace() {
        return exceptionStackTrace;
    }

    /**
     * Over-ridden method to return formatted Exception Message
     *
     * @return String containing the representation of this instance
     */
    public String toString() {
        StringBuilder sbdBuffer = new StringBuilder();
        sbdBuffer.append("\n");
        sbdBuffer.append(SEPERATOR + "\n");
        sbdBuffer.append("Error Code            : " + errorMessage + "\n");
        sbdBuffer.append("Display Error Code    : " + getDisplayerrorMessage() + "\n");
        sbdBuffer.append("TimeStamp             : " + new java.util.Date() + "\n");

        // Add the Application Debug Message if available
        if (applicationDebugMessage != null) {
            sbdBuffer.append("Debug Message         : " + applicationDebugMessage + "\n");
        }

        // Add the Chained Exception Information if available
        if (chainedException != null) {
            sbdBuffer.append("Exception Name        : " + exceptionName + "\n");
            sbdBuffer.append("Exception Stack Trace : \n");
            sbdBuffer.append(exceptionStackTrace + "\n");
        }
        // sbdBuffer.append("Unique Id: " + sUniqueId + "\n");
        sbdBuffer.append(SEPERATOR + "\n");
        return sbdBuffer.toString();
    }

    /**
     * Method to initialize the Exception object
     */
    private void initialise() {
        setUniqueId();
        extractExceptionInfo();
        formatDisplayerrorMessage();
    }

    /**
     * Method to format the Display Error Code <<MASKED-CLUSTER-ID>>-<<UNIQUE-ID>>
     */
    private void formatDisplayerrorMessage() {
        displayerrorMessage = "CASA-" + FrameworkUtil.getInstance() + "-" + String.format("%010d", lnUniqueId);
    }

    /**
     * Method to generate the Error Unique ID from the local file
     * [UNIQUE_ID_FNAME] available in the domain directory. If the file is not
     * available, a new file will be created.
     */
    private synchronized void setUniqueId() {
        try {
            RandomAccessFile ra = new RandomAccessFile(UNIQUE_ID_FNAME, "rw");
            ra.seek(0);
            if (ra.length() != 0) {
                String sUniqueId = ra.readLine();
                lnUniqueId = Long.parseLong(sUniqueId);
            }
            lnUniqueId++;
            ra.seek(0);
            ra.writeBytes(String.valueOf(lnUniqueId));
            ra.close();
        } catch (Throwable t) {
            System.out.println("SearchException: The following exception occured in "
                    + "setUniqueId method while creating a Unique Id.");
            t.printStackTrace();
        }
    }

    /**
     * Method to extract Information from the Chained Exception object
     */
    private void extractExceptionInfo() {
        try {
            // If the exception passed is not null
            if (chainedException != null) {
                // Extract the exception class name from the chained exception
                // object
                exceptionName = chainedException.getClass().toString().substring(6);

                // Extract the exception stack trace from the chained exception
                // object
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                chainedException.printStackTrace(pw);
                exceptionStackTrace = sw.toString();
            }
        } catch (Exception expGeneral) {
            // Dont do anything. Ignore this scenario
        }
    }

    /**
     * Private Utility method to prevent Wrapper an existing
     * SearchException object
     *
     * @param errorMessage
     *            containing the Error Code for the Exception being created
     * @param applicationDebugMessage
     *            containing the Application Debug Message
     * @param chainedException
     *            containing the Chained Exception object instance
     */
    protected void processChainedException (String errorMessage, String applicationDebugMessage, Exception chainedException) {
        if (chainedException.getClass().getName().equals(ApplicationException.class.getName())) {
            sameException = true;
            ApplicationException ApplicationException = (ApplicationException) chainedException;
            this.errorMessage = ApplicationException.getErrorMessage();
            this.applicationDebugMessage = ApplicationException.getApplicationDebugMessage();
            this.chainedException = ApplicationException.getChainedException();
        } else {
            this.errorMessage = errorMessage;
            this.applicationDebugMessage = applicationDebugMessage;
            this.chainedException = chainedException;
        }
    }
}
