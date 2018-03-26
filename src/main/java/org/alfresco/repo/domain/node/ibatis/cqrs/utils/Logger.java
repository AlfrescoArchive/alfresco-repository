package org.alfresco.repo.domain.node.ibatis.cqrs.utils;

/**
 * Created by mmuller on 26/03/2018.
 */
public class Logger
{
    public static int LEVEL_OFF = 0;
    public static int LEVEL_ERROR = 1;
    public static int LEVEL_WARN = 2;
    public static int LEVEL_INFO = 3;
    public static int LEVEL_DEBUG = 4;

    private static int InternalLogLevel = LEVEL_DEBUG;

    static
    {
        try
        {
            String level = System.getenv("LOG_LEVEL");
            if (level != null && !level.isEmpty())
            {
                if (level.equalsIgnoreCase("ERROR"))
                {
                    InternalLogLevel = LEVEL_ERROR;
                }
                else if (level.equalsIgnoreCase("WARN"))
                {
                    InternalLogLevel = LEVEL_WARN;
                }
                else if (level.equalsIgnoreCase("DEBUG"))
                {
                    InternalLogLevel = LEVEL_DEBUG;
                }
                else
                {
                    InternalLogLevel = LEVEL_INFO;
                }
            }
        }
        catch (Exception e)
        {
            InternalLogLevel = LEVEL_INFO;
        }
    }

    public static int getLogLevel()
    {
        return InternalLogLevel;
    }

    public static void setLogLevel(int logLevel)
    {
        InternalLogLevel = logLevel;
    }

    public static void logInfo(String message, Context context)
    {
        if (context != null && InternalLogLevel >= LEVEL_INFO)
        {
            context.getLogger().log("INFO " + message);
        }
    }

    public static void logWarn(String message, Context context)
    {
        if (context != null && InternalLogLevel >= LEVEL_WARN)
        {
            context.getLogger().log("WARN " + message);
        }
    }

    public static void logDebug(String message, Context context)
    {
        if (context != null && InternalLogLevel >= LEVEL_DEBUG)
        {
            context.getLogger().log("DEBUG " + message);
        }
    }

    public static void logError(String message, Context context)
    {
        if (context != null && InternalLogLevel >= LEVEL_ERROR)
        {
            context.getLogger().log("ERROR " + message);
        }
    }

    public static void logError(Throwable error, Context context)
    {
        if (error != null)
        {
            logError(error.getMessage(), context);
        }
    }



}
