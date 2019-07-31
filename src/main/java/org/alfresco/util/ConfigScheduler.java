/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2019 Alfresco Software Limited
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
package org.alfresco.util;

import org.apache.commons.logging.Log;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;

import java.io.IOException;

/**
 * Used to schedule reading of config. The config is assumed to change from time to time.
 * Initially or on error the reading frequency is high but slower once no problems are reported.
 *
 * @author adavis
 */
public abstract class ConfigScheduler<Data>
{
    public static final String CONFIG_SCHEDULER = "configScheduler";

    public static class ConfigSchedulerJob implements Job
    {
        @Override
        public void execute(JobExecutionContext context) throws JobExecutionException
        {
            JobDataMap dataMap = context.getJobDetail().getJobDataMap();
            ConfigScheduler configScheduler = (ConfigScheduler)dataMap.get(CONFIG_SCHEDULER);
            configScheduler.readConfigAndReplace();
        }
    }

    private final String jobName;
    private Log log;
    private CronExpression cronExpression;
    private CronExpression initialAndOnErrorCronExpression;

    private Scheduler scheduler;
    private boolean normalCronSchedule;

    protected Data data;
    private ThreadLocal<Data> threadData = ThreadLocal.withInitial(() -> data);

    public ConfigScheduler(Object client)
    {
        jobName = client.getClass().getName()+"Job";
    }

    public abstract boolean readConfig() throws IOException;

    public abstract Data createData();

    public synchronized Data getData()
    {
        Data data = threadData.get();
        if (data == null)
        {
            data = createData();
            setData(data);
        }
        return data;
    }

    private synchronized void setData(Data data)
    {
        this.data = data;
        threadData.set(data);
    }

    private synchronized void clearData()
    {
        this.data = null;
        threadData.remove(); // we need to pick up the initial value next time (whatever the data value is at that point)
    }

    public void schedule(boolean enabled, Log log, CronExpression cronExpression, CronExpression initialAndOnErrorCronExpression)
    {
        this.log = log;
        this.cronExpression = cronExpression;
        this.initialAndOnErrorCronExpression = initialAndOnErrorCronExpression;

        try
        {
            clearData();
            if (scheduler != null)
            {
                scheduler.clear();
            }

            if (enabled && log != null && cronExpression != null && initialAndOnErrorCronExpression != null)
            {
                schedule();
            }
        }
        catch (Exception e)
        {
            log.error("Error scheduling "+e.getMessage());
        }
    }

    private synchronized void schedule()
    {
        // Don't do an initial readConfigAndReplace() as the first scheduled read can be done almost instantly and
        // there is little point doing two in the space of a few seconds. If however the scheduler is already running
        // we do need to run it (this is only from test cases).
        if (scheduler == null)
        {
            StdSchedulerFactory sf = new StdSchedulerFactory();
            try
            {
                scheduler = sf.getScheduler();

                JobDetail job = JobBuilder.newJob()
                        .withIdentity(jobName)
                        .ofType(ConfigSchedulerJob.class)
                        .build();
                JobKey key = job.getKey();
                if (scheduler.deleteJob(key))
                {
                    log.trace("Needed to kill "+jobName+" key "+key+" before starting it again");
                }
                job.getJobDataMap().put(CONFIG_SCHEDULER, this);
                CronExpression cronExpression = normalCronSchedule ? this.cronExpression : initialAndOnErrorCronExpression;
                CronTrigger trigger = TriggerBuilder.newTrigger()
                        .withIdentity(jobName+"Trigger", Scheduler.DEFAULT_GROUP)
                        .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                        .build();
                scheduler.startDelayed(0);
                scheduler.scheduleJob(job, trigger);
            }
            catch (SchedulerException e)
            {
                log.error("Failed to start "+jobName+" "+e.getMessage());
            }
        }
        else
        {
            readConfigAndReplace();
        }
    }

    private void readConfigAndReplace()
    {
        boolean successReadingConfig;
        log.debug("Config read started");
        Data data = getData();
        try
        {
            Data newData = createData();
            threadData.set(newData);
            successReadingConfig = readConfig();
            data = newData;
            log.debug("Config read finished "+data+
                    (successReadingConfig ? "" : ". Config replaced but there were problems"));
        }
        catch (Exception e)
        {
            successReadingConfig = false;
            log.error("Config read failed. "+e.getMessage(), e);
        }
        setData(data);

        // Switch schedule sequence if we were on the normal schedule and we now have problems or if
        // we are on the initial/error schedule and there were no errors.
        if ( normalCronSchedule && !successReadingConfig ||
            !normalCronSchedule && successReadingConfig)
        {
            normalCronSchedule = !normalCronSchedule;
            if (scheduler != null)
            {
                try
                {
                    scheduler.clear();
                    scheduler = null;
                    schedule();
                }
                catch (SchedulerException e)
                {
                    log.error("Problem stopping scheduler for transformer configuration "+e.getMessage());
                }
            }
            else
            {
                System.out.println("Switch schedule "+normalCronSchedule+" WITHOUT new schedule");
            }
        }
    }
}
