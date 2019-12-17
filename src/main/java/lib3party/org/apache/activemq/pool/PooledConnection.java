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
package lib3party.org.apache.activemq.pool;

import javax.jms.JMSException;
import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.EnhancedConnection;
import org.apache.activemq.advisory.DestinationSource;
import org.apache.activemq.jms.pool.ConnectionPool;

public class PooledConnection extends org.apache.activemq.jms.pool.PooledConnection implements EnhancedConnection {
    public PooledConnection(ConnectionPool connection) {
        super(connection);
    }

    @Override
    public DestinationSource getDestinationSource() throws JMSException {
        return ((ActiveMQConnection)getConnection()).getDestinationSource();
    }
}

