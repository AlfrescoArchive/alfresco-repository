package org.alfresco.repo.domain.node.ibatis.cqrs;

import java.util.List;

/**
 * Implements the same context of Viewer and Reader in the CQRS pattern. For this example they both can have names.
 * And they are listen to the Event Source with EventListener.
 *
 * As well the CQRS pattern allows us that we can share storage ressources (e.g. in-memory) across Reader and Writer.
 *
 * Created by mmuller on 26/03/2018.
 */
public abstract class CqrsWriterAndReader implements EventListener
{
    private String name;

    public CqrsWriterAndReader(String name)
    {
        this.name = name;
    }

    /**
     * Returns the name of the Writer or Reader
     *
     * @return
     */
    public String getName()
    {
        return name;
    }

    /**
     * Returns the store which was used for the writes or readers.
     *
     * @return the used store as object. Can be null if no store was used.
     */
    public abstract List<Object> getUsedStores();
}
