package org.alfresco.repo.domain.node.ibatis.cqrs;

import java.util.List;

/**
 * Implements the writer in the CQRS pattern. You can define multiple writer with different kind of databases
 * behind them. Even only in memory could be possible.
 *
 * Created by mmuller on 26/03/2018.
 */
public interface CqrsWriter
{
    /**
     * Notifies the writer if new events are detected from the event source. Is a simple listener pattern.
     *
     * @param events List of new events
     */
    public void notifyWriter(List<Event> events);

    /**
     * Writer are instantiated with names
     *
     * @return the name of the writer
     */
    public String getName();
}
