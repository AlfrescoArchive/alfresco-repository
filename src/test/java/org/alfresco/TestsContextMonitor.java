/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
 * 
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco;

import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextRefreshedEvent;

public class TestsContextMonitor implements ApplicationListener<ApplicationContextEvent>
{
    private static Log logger = LogFactory.getLog(TestsContextMonitor.class);

    @Override
    public void onApplicationEvent(ApplicationContextEvent event)
    {
        ConfigurableApplicationContext applicationContext = (ConfigurableApplicationContext) event.getApplicationContext();
        ConfigurableListableBeanFactory beanFactory = applicationContext.getBeanFactory();
        if (!(event instanceof ContextRefreshedEvent))
        {
            return;
        }

        Set<String> contextFiles = buildApplicationContextSet(applicationContext, beanFactory);

        String guessedTestSuite = "";
        StackTraceElement testSuiteInfo = guessTestSuiteStackEntry();
        if (testSuiteInfo != null)
        {
            guessedTestSuite = testSuiteInfo.getClassName();
        }
        String guessedTestClassName = "";
        String guessedTestName = "";
        String testClassParents = "";
        StackTraceElement testClassInfo = guessTestClassStackEntry();

        if (testClassInfo != null)
        {
            guessedTestClassName = testClassInfo.getClassName();
            guessedTestName = testClassInfo.getMethodName();
            Class<?> loadClass = loadTheGuessedClass(guessedTestClassName);
            testClassParents = buildTestClassParents(loadClass);

        }

        String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();

        StringBuilder sb = new StringBuilder();
        sb.append("\n\n\n----------------TestsContextMonitor: ContextRefreshedEvent---------------------------------\n");
        sb.append("Context object: " + applicationContext + "\n");
        sb.append("Event type: " + event.getClass().getName() + "\n");
        sb.append("Bean definition count: " + beanDefinitionNames.length + "\n");
        sb.append("TestSuite:" + guessedTestSuite + "\n");
        sb.append("TestClass: " + guessedTestClassName + "\n");
        sb.append("TestName: " + guessedTestName + "\n");
        sb.append("Test parent classes:" + testClassParents + "\n");
        sb.append("Context Files size: " + contextFiles.size() + "\n");
        sb.append("Context Files:\n" + contextFiles + "\n");

        sb.append("TestsContextMonitor stack:\n" + getStackTrace() + "\n");

        sb.append("----------------TestsContextMonitor: ContextRefreshedEvent done!---------------------------\n");

        logger.error(sb.toString());
    }

    private Class<?> loadTheGuessedClass(String guessedTestClassName)
    {
        try
        {
            return this.getClass().getClassLoader().loadClass(guessedTestClassName);
        }
        catch (Exception e)
        {

        }
        return null;
    }

    private String buildTestClassParents(Class class1)
    {
        if (class1 == null)
        {
            return "";
        }
        return class1.getName() + "->" + buildTestClassParents(class1.getSuperclass());
    }

    private String getStackTrace()
    {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        for (int j = 0; j < stackTrace.length; ++j)
        {
            StackTraceElement el = stackTrace[j];
            sb.append(el + "\n");
        }
        return sb.toString();
    }

    protected StackTraceElement guessTestClassStackEntry()
    {
        StackTraceElement testClassInfo = null;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int j = 0; j < stackTrace.length; ++j)
        {
            StackTraceElement el = stackTrace[j];
            String className = el.getClassName();
            if (className.endsWith("Test"))
            {
                testClassInfo = el;
                // first one from the bottom of the stack should do
                break;
            }
        }
        if (testClassInfo == null)
        {
            for (int j = 0; j < stackTrace.length; ++j)
            {
                StackTraceElement el = stackTrace[j];
                String className = el.getClassName();
                if (className.contains("Abstract") && className.endsWith("Tests"))
                {
                    testClassInfo = el;
                    // first one from the bottom of the stack should do
                    break;
                }
            }
        }
        return testClassInfo;
    }

    private StackTraceElement guessTestSuiteStackEntry()
    {
        StackTraceElement testSuiteInfo = null;
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (int j = 0; j < stackTrace.length; ++j)
        {
            StackTraceElement el = stackTrace[j];
            String methodName = el.getMethodName();
            if (methodName.equals("suite"))
            {
                testSuiteInfo = el;
                // first one from the bottom of the stack should do
                // LE this is usually not the best place
                break;
            }
        }
        return testSuiteInfo;
    }

    protected Set<String> buildApplicationContextSet(ConfigurableApplicationContext applicationContext,
            ConfigurableListableBeanFactory beanFactory)
    {
        Set<String> contextFiles = new TreeSet<String>();

        for (String beanName : applicationContext.getBeanDefinitionNames())
        {
            String resourceDescription = beanFactory.getBeanDefinition(beanName).getResourceDescription();
            if (resourceDescription != null)
            {
                contextFiles.add(resourceDescription);
            }
        }
        return contextFiles;
    }

    private String extractFileName(String resourceDescription)
    {
        int indexOfSlash = Math.max(resourceDescription.lastIndexOf("\\"), resourceDescription.lastIndexOf("/"));

        return resourceDescription.substring(indexOfSlash + 1).replaceAll("]", "");

    }
}
