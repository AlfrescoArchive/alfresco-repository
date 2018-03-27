package org.alfresco.repo.domain.node.ibatis.cqrs;

/**
 * Created by mmuller on 27/03/2018.
 */
public abstract class IbatisNodeInsertCqrsReaderAbstract extends CqrsWriterAndReader
{
    public IbatisNodeInsertCqrsReaderAbstract(String name)
    {
        super(name);
    }

    public abstract String getValue(String col, Object node);
}
