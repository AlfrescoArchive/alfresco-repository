package org.alfresco.repo.domain.node.ibatis.cqrs;

import java.util.List;

/**
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

    public String getName();
}
