package org.alfresco.heartbeat;

import org.alfresco.repo.lock.JobLockService;
import org.quartz.Job;
import org.quartz.JobDataMap;


/**
 *
 * The scheduler is responsible for the scheduling and unscheduling of locking jobs {@link LockingJob}.
 * Only one repository node in a cluster will collect data for collectors with this type of job.
 */
public class LockingJobScheduler extends QuartzJobScheduler
{

    /** Services needed to schedule and execute this job */
    private JobLockService jobLockService;

    public void setJobLockService(JobLockService jobLockService)
    {
        this.jobLockService = jobLockService;
    }

    @Override
    protected JobDataMap getJobDetailMap(HBBaseDataCollector collector)
    {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(LockingJob.COLLECTOR_KEY, collector);
        jobDataMap.put(LockingJob.DATA_SENDER_SERVICE_KEY, hbDataSenderService);
        jobDataMap.put(LockingJob.JOB_LOCK_SERVICE_KEY, jobLockService);
        return jobDataMap;
    }

    @Override
    protected Class<? extends Job> getHeartBeatJobClass()
    {
        return LockingJob.class;
    }
}
