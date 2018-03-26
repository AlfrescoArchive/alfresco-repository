package org.alfresco.repo.domain.node.ibatis.cqrs.utils;

/**
 * Currently only used for logging. Describes the context in which the service was called.
 *
 * Created by mmuller on 26/03/2018.
 */
public class CqrsContext implements Context
{
    public SimpleCqrsLogger getLogger()
    {
        return new SimpleCqrsLogger();
    }

    private class SimpleCqrsLogger implements CqrsLogger
    {
        public void log(String string)
        {
            System.out.println(string);
        }
    }
}
