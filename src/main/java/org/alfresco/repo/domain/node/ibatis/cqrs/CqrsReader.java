package org.alfresco.repo.domain.node.ibatis.cqrs;

import java.util.List;

/**
 * Implements the writer in the CQRS pattern. You can define multiple reader with different kind of databases
 * behind them. Even only in memory could be possible.
 *
 * Created by mmuller on 26/03/2018.
 */
public interface CqrsReader
{
    /**
     * Notifies the reader if new events are detected from the event source. Is a simple listener pattern.
     *
     * @param events List of new events
     */
    public void notifyReader(List<Event> events);

    /**
     * Returns a value from a simple table
     *
     * @param colName attribute of the returned object
     * @param objectSelection which object shall be returned
     */
    public String getValue(String colName, Object objectSelection);

    /**
     * Writer are instantiated with names
     *
     * @return the name of the reader
     */
    public String getName();
}
