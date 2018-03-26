package org.alfresco.repo.domain.node.ibatis.cqrs;

import java.util.UUID;

/**
 * Class represents an event from stored in the Event Store. Typically this event needs to contain the changes
 * which leaded the state A to state B. Think of that like diff changes from commits.
 *
 * Created by mmuller on 26/03/2018.
 */
public class Event<T>
{
    private long timestamp;
    /** Every event has an id */
    private UUID id;
    private T diffObject;

    /**
     * Creates an event which can be stored in an Event Store
     *
     * @param diffObject contains the diff object (e.g. ibatis statement)
     */
    public Event(T diffObject)
    {
        this.diffObject = diffObject;
        timestamp = System.currentTimeMillis();
        id = UUID.randomUUID();
    }

    public String toString()
    {
        return "Event id: " + id + " Created: " + timestamp + " DiffObject: " + diffObject.toString();
    }

    public long getTimestamp()
    {
        return timestamp;
    }

    public UUID getId()
    {
        return id;
    }

    public T getDiffObject()
    {
        return diffObject;
    }
}
