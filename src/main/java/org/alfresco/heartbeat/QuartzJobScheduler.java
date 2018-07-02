package org.alfresco.heartbeat;

import org.alfresco.heartbeat.datasender.HBDataSenderService;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.quartz.*;

public abstract class QuartzJobScheduler implements HeartBeatJobScheduler
{

    /** The logger. */
    private static final Log logger = LogFactory.getLog(LockingJob.class);

    /** schedule set for all jobs scheduled with this scheduler if testMode is on */
    protected boolean testMode = false;
    protected final String testCronExpression = "0 0/1 * * * ?";

    protected HBDataSenderService hbDataSenderService;
    protected Scheduler scheduler;

    public void setScheduler(Scheduler scheduler)
    {
        this.scheduler = scheduler;
    }

    public void setHbDataSenderService(HBDataSenderService hbDataSenderService)
    {
        this.hbDataSenderService = hbDataSenderService;
    }

    public void setTestMode(boolean testMode)
    {
        this.testMode = testMode;
    }

    public String getJobName(String collectorId)
    {
        return "heartbeat-" + collectorId;
    }

    public String getTriggerName(String collectorId)
    {
        return getJobName(collectorId) + "-Trigger";
    }

    protected abstract JobDataMap getJobDetailMap(HBBaseDataCollector collector);

    protected abstract Class<? extends Job> getHeartBeatJobClass();

    @Override
    public void scheduleJob(HBBaseDataCollector collector)
    {

        final JobDetail jobDetail = JobBuilder.newJob()
                .withIdentity(getJobName(collector.getCollectorId()))
                .usingJobData(getJobDetailMap(collector))
                .ofType(getHeartBeatJobClass())
                .build();

        final String cronExpression = testMode ? testCronExpression : collector.getCronExpression();
        // Schedule job
        final CronTrigger cronTrigger = TriggerBuilder.newTrigger()
                .withIdentity(getTriggerName(collector.getCollectorId()))
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();

        try{
            // Ensure the job wasn't already scheduled in an earlier retry of this transaction
            scheduler.unscheduleJob(cronTrigger.getKey());
            scheduler.scheduleJob(jobDetail, cronTrigger);

            if (logger.isDebugEnabled())
            {
                logger.debug("HeartBeat job scheduled for collector: " +
                        collector.getCollectorId());
            }
        }
        catch (SchedulerException e)
        {
            throw new RuntimeException("Heartbeat failed to schedule job for collector: "
                    + collector.getCollectorId(), e);
        }
    }

    @Override
    public void unscheduleJob(final HBBaseDataCollector collector)
    {
        try
        {
            scheduler.unscheduleJob(new TriggerKey(getTriggerName(collector.getCollectorId())));

            if (logger.isDebugEnabled())
            {
                logger.debug("HeartBeat unscheduled job for collector: " +
                        collector.getCollectorId());
            }
        }
        catch (SchedulerException e)
        {
            throw new RuntimeException("Heartbeat failed to unschedule job for collector: "
                    + collector.getCollectorId(), e);
        }
    }
}
