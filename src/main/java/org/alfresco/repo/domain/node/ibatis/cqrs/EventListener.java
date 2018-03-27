package org.alfresco.repo.domain.node.ibatis.cqrs;

import java.util.List;

/**
 * Listener interface for Writer and Reader
 *
 * Created by mmuller on 27/03/2018.
 */
public interface EventListener
{
    void onUpdate(List<Event> events);

    void onCreate(List<Event> events);

    void onDelete(List<Event> events);
}
