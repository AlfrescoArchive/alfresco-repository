package org.alfresco.heartbeat;

import org.alfresco.heartbeat.datasender.HBData;
import org.alfresco.repo.scheduler.AlfrescoSchedulerFactory;
import org.junit.Before;
import org.junit.Test;
import org.quartz.*;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuartzJobSchedulerTest
{

    private Scheduler scheduler;
    QuartzJobScheduler hbJobScheduler ;

    @Before
    public void setUp() throws Exception
    {
        // Create scheduler
        SchedulerFactoryBean sfb = new SchedulerFactoryBean();
        sfb.setSchedulerFactoryClass(AlfrescoSchedulerFactory.class);
        sfb.setAutoStartup(false);
        sfb.afterPropertiesSet();
        scheduler = sfb.getScheduler();
        hbJobScheduler = createSimpleJobScheduler();
        hbJobScheduler.setScheduler(scheduler);

    }

    /**
     *
     * Jobs are scheduled with cron expressions provided by collectors
     *
     */
    @Test
    public void testJobsScheduledWithDifferentCronExpressions() throws Exception
    {

        final String cron1 = "0 0/1 * * * ?";
        final String cron2 = "0 0/2 * * * ?";
        final String cron3 = "0 0/3 * * * ?";

        final HBBaseDataCollector c1 = new SimpleHBDataCollector("c1", cron1);
        final HBBaseDataCollector c2 = new SimpleHBDataCollector("c2", cron2);
        final HBBaseDataCollector c3 = new SimpleHBDataCollector("c3", cron3);

        final String triggerName1 = "heartbeat-" + c1.getCollectorId() + "-Trigger";
        final String triggerName2 = "heartbeat-" + c2.getCollectorId() + "-Trigger";
        final String triggerName3 = "heartbeat-" + c3.getCollectorId() + "-Trigger";

        // Register 3 collectors with 3 different cron expressions
        hbJobScheduler.scheduleJob(c1);
        hbJobScheduler.scheduleJob(c2);
        hbJobScheduler.scheduleJob(c3);

        String testCron1 = ((CronTrigger) scheduler.getTrigger(new TriggerKey(triggerName1, Scheduler.DEFAULT_GROUP))).getCronExpression();
        String testCron2 = ((CronTrigger) scheduler.getTrigger(new TriggerKey(triggerName2, Scheduler.DEFAULT_GROUP))).getCronExpression();
        String testCron3 = ((CronTrigger) scheduler.getTrigger(new TriggerKey(triggerName3, Scheduler.DEFAULT_GROUP))).getCronExpression();

        assertEquals("Cron expression doesn't match", cron1, testCron1);
        assertEquals("Cron expression doesn't match", cron2, testCron2);
        assertEquals("Cron expression doesn't match", cron3, testCron3);
    }

    @Test
    public void testUnscheduling() throws Exception
    {
        final String cron1 = "0 0/1 * * * ?";
        final String cron2 = "0 0/2 * * * ?";
        final String cron3 = "0 0/3 * * * ?";

        final HBBaseDataCollector c1 = new SimpleHBDataCollector("c1", cron1);
        final HBBaseDataCollector c2 = new SimpleHBDataCollector("c2", cron2);
        final HBBaseDataCollector c3 = new SimpleHBDataCollector("c3", cron3);

        final String triggerName3 = "heartbeat-" + c3.getCollectorId() + "-Trigger";

        // Register 3 collectors with 3 different cron expressions
        hbJobScheduler.scheduleJob(c1);
        hbJobScheduler.scheduleJob(c2);
        hbJobScheduler.scheduleJob(c3);

        // Unschedule 2
        hbJobScheduler.unscheduleJob(c1);
        hbJobScheduler.unscheduleJob(c2);

        // 1 & 2 gone, 3 is still there
        assertFalse(isJobScheduledForCollector(c1.getCollectorId(),scheduler));
        assertFalse(isJobScheduledForCollector(c2.getCollectorId(),scheduler));
        assertTrue(isJobScheduledForCollector(c3.getCollectorId(),scheduler));
    }

    @Test(expected=RuntimeException.class)
    public void testInvalidCronExpression() throws Exception
    {

        // Register collector with invalid cron expression
        SimpleHBDataCollector c2 = new SimpleHBDataCollector("c2", "Ivalidcron");
        hbJobScheduler.scheduleJob(c2);
    }

    private class SimpleHBDataCollector extends HBBaseDataCollector
    {

        public SimpleHBDataCollector(String collectorId, String cron)
        {
            super(collectorId,"1.0",cron);
        }

        public List<HBData> collectData()
        {
            List<HBData> result = new LinkedList<>();
            result.add(new HBData("systemId2", this.getCollectorId(), "1", new Date()));
            return result;
        }
    }

    private boolean isJobScheduledForCollector(String collectorId, Scheduler scheduler) throws Exception
    {
        String jobName = "heartbeat-" + collectorId;
        String triggerName = jobName + "-Trigger";
        return scheduler.checkExists(new JobKey(jobName, Scheduler.DEFAULT_GROUP))
                && scheduler.checkExists(new TriggerKey(triggerName, Scheduler.DEFAULT_GROUP));
    }

    private QuartzJobScheduler createSimpleJobScheduler()
    {
        return new QuartzJobScheduler()
        {

            @Override
            protected JobDataMap getJobDetailMap(HBBaseDataCollector collector)
            {
                return new JobDataMap();
            }

            @Override
            protected Class<? extends Job> getHeartBeatJobClass()
            {
                return SimpleJob.class;
            }
        };
    }

    private class SimpleJob implements Job
    {
        @Override
        public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException
        {

        }
    }
}
