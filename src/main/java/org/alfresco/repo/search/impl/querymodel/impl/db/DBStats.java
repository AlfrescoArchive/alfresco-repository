package org.alfresco.repo.search.impl.querymodel.impl.db;

import org.springframework.util.StopWatch;

public class DBStats
{
    public static final ThreadLocal<StopWatch> SW = new ThreadLocal<StopWatch>();
    
    private DBStats() {}
    
    public static StopWatch resetStopwatch() {
        StopWatch sw = new StopWatch();
        SW.set(sw);
        return sw;
    }
    
    public static StopWatch stopwatch() {
        return SW.get();
    }
    
}
