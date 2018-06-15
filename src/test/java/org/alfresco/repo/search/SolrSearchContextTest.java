package org.alfresco.repo.search;

import org.alfresco.util.ApplicationContextHelper;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;


public class SolrSearchContextTest
{


    @Test
    public void testSearchContextStartup()
    {
        ApplicationContext searchContext =
                new ClassPathXmlApplicationContext(new String[] {
                        "classpath:org/alfresco/repo/search/impl/test-solr-search-context.xml",
                        "alfresco/subsystems/Search/solr6/common-search-scheduler-context.xml"},
                        ApplicationContextHelper.getApplicationContext());




    }
}
