package edu.ualberta.med.biobank.server.logging.logger;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import edu.ualberta.med.biobank.model.Log;
import edu.ualberta.med.biobank.server.logging.MessageGenerator;
import edu.ualberta.med.biobank.server.logging.user.BiobankThreadVariable;
import edu.ualberta.med.biobank.server.logging.user.UserInfo;

/**
 * This class logs the object state information.
 * 
 * Copy from CLM
 */

public abstract class BiobankObjectStateLogger {
    private static Logger logger = null;
    public static final SimpleDateFormat dateTimeFormatter = new SimpleDateFormat(
        "yyyy-MM-dd HH:mm");

    private static HashMap<String, BiobankObjectStateLogger> loggersMap = new HashMap<String, BiobankObjectStateLogger>();

    static {
        logger = Logger.getLogger("Biobank.Activity");
    }

    /**
     * This method logs the message for update operation
     * 
     * @param id -- Serializable id of the object
     * @param currentState -- current states of the object after the operation
     * @param previousState -- previous states of the object before the
     *            operation
     * @param propertyNames --names of the object states
     * @param types -- Hibernate types of the object states
     * @param action -- the name of the operation being performed
     * 
     */
    public void logMessage(Object obj, String action,
        Map<String, Object> statesMap) {
        Log log = getLogObject(obj, statesMap);
        if (log != null) {
            String message = MessageGenerator.generateStringMessage(action, log
                .getPatientNumber(), log.getInventoryId(), log
                .getLocationLabel(), log.getDetails(), log.getType());
            UserInfo userInfo = BiobankThreadVariable.get();
            if (null == userInfo)
                userInfo = new UserInfo();
            if (userInfo.getIsIntransaction() == true) {
                logToBuffer(message);
            } else {
                log(message);
            }
        }
    }

    /**
     * This method saves the message to the buffer for later use
     * 
     * @param msg -- message to be logged
     */
    public void logToBuffer(String msg) {
        UserInfo userInfo = BiobankThreadVariable.get();
        if (null == userInfo)
            userInfo = new UserInfo();
        ArrayList<String> logs = userInfo.getTransactionLogs();
        if (logs == null) {
            logs = new ArrayList<String>();
        }
        logs.add(msg);
        userInfo.setTransactionLogs(logs);
        BiobankThreadVariable.set(userInfo);
    }

    protected abstract Log getLogObject(Object obj,
        Map<String, Object> statesMap);

    /**
     * This method logs the message
     * 
     * @param message -- message to be logged
     */
    public static void log(String message) {
        Level level = Level.toLevel("INFO");
        logger.log(level, message);
    }

    public static BiobankObjectStateLogger getlogger(String entityTypeName) {
        BiobankObjectStateLogger stateLogger = loggersMap.get(entityTypeName);
        if (stateLogger == null) {
            String className = entityTypeName + "StateLogger";
            try {
                Class<?> loggerClass = Class
                    .forName(BiobankObjectStateLogger.class.getPackage()
                        .getName()
                        + "." + className);
                stateLogger = (BiobankObjectStateLogger) loggerClass
                    .newInstance();
                loggersMap.put(entityTypeName, stateLogger);
            } catch (Exception e) {
                return null;
            }
        }
        return stateLogger;
    }
}
