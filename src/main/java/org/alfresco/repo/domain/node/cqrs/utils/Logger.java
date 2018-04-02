/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.repo.domain.node.cqrs.utils;

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
