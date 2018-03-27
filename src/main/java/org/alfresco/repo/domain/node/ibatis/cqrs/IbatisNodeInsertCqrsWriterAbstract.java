package org.alfresco.repo.domain.node.ibatis.cqrs;

/**
 * Created by mmuller on 27/03/2018.
 */
public abstract class IbatisNodeInsertCqrsWriterAbstract extends CqrsWriterAndReader
{
    public IbatisNodeInsertCqrsWriterAbstract(String name)
    {
        super(name);
    }
}
