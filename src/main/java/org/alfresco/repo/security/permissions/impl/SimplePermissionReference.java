/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package org.alfresco.repo.security.permissions.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.Pair;

/**
 * A simple permission reference.
 * 
 * @author andyh
 */
public final class SimplePermissionReference extends AbstractPermissionReference {
    private static final long serialVersionUID = 637302438293417818L;

    //Use thread-safe map initiallized with a slightly larger capacity to reduce the posibility of two or more threads attempting to resize at the same time
    private static ConcurrentMap<Pair<QName, String>, Future<SimplePermissionReference>> instances = new ConcurrentHashMap<>(100);

    /**
     * Factory method to create simple permission references
     * 
     * @return a simple permission reference
     */
    public static SimplePermissionReference getPermissionReference(QName qName, String name) {
        //loop if thread is interrupted and should be run again
        while (true)
        {
            Pair<QName, String> key = new Pair<>(qName, name);
            Future<SimplePermissionReference> instance = instances.get(key);
            if (instance == null) {
                //Set up task to be completed if the thread is waiting
                Callable<SimplePermissionReference> callableReference = new Callable<SimplePermissionReference>() {
                    public SimplePermissionReference call() throws InterruptedException {
                        return new SimplePermissionReference(qName, name);
                    }
                };

                FutureTask<SimplePermissionReference> instanceTask = new FutureTask<>(callableReference);
                instance = instances.putIfAbsent(key, instanceTask);
                if (instance == null) {
                    instance = instanceTask;
                    instanceTask.run();
                }

                try
                {
                    return instance.get();
                } 
                catch (CancellationException e)
                {
                    instances.remove(key, instance);
                }
                catch (InterruptedException e)
                {
                    Thread.currentThread().interrupt();
                } 
                catch (ExecutionException e)
                {
                    throw new AlfrescoRuntimeException(e.getMessage());
                }
            }
        }
    }
    
    /*
     * The type
     */
    private QName qName;
    
    /*
     * The name of the permission
     */
    private String name;
    
    
    protected SimplePermissionReference(QName qName, String name)
    {
        super();
        this.qName = qName;
        this.name = name;
    }

    public QName getQName()
    {
        return qName;
    }

    public String getName()
    {
        return name;
    }

}
