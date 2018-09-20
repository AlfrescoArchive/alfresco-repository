/*
 * #%L
 * Alfresco Repository
 * %%
 * Copyright (C) 2005 - 2018 Alfresco Software Limited
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
package org.alfresco.repo.rendition2;

import java.util.Objects;
import org.alfresco.repo.transaction.AlfrescoTransactionSupport;
import org.alfresco.repo.transaction.RetryingTransactionHelper;
import org.alfresco.service.transaction.TransactionService;
import org.alfresco.util.GUID;
import org.alfresco.util.transaction.TransactionListenerAdapter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Schedules a rendition (callback) to a post-commit phase.
 *
 * @author alex.mukha
 */
public class RenditionRequestScheduler
{
    private static Log logger = LogFactory.getLog(RenditionRequestScheduler.class);

    private TransactionService transactionService;

    public void setTransactionService(TransactionService transactionService)
    {
        this.transactionService = transactionService;
    }

    void scheduleRendition(RetryingTransactionHelper.RetryingTransactionCallback callback)
    {
        AlfrescoTransactionSupport.bindListener(new Rendition2TransactionListener(callback));
    }

    private class Rendition2TransactionListener extends TransactionListenerAdapter
    {
        private final RetryingTransactionHelper.RetryingTransactionCallback callback;
        private final String id;

        Rendition2TransactionListener(RetryingTransactionHelper.RetryingTransactionCallback callback)
        {
            this.callback = callback;
            this.id = Rendition2TransactionListener.class.getSimpleName() + "-" + GUID.generate();
            logger.debug("Created lister with id = " + id);
        }

        @Override
        public void afterCommit()
        {
            try
            {
                transactionService.getRetryingTransactionHelper().doInTransaction(callback);
            }
            catch (Exception e)
            {
                logger.debug(e.getMessage());
                // consume exception as we need to move on to the next transform
            }
        }

        @Override
        public boolean equals(Object o)
        {
            if (this == o)
            {
                return true;
            }
            if (o == null || getClass() != o.getClass())
            {
                return false;
            }
            Rendition2TransactionListener that = (Rendition2TransactionListener) o;
            return Objects.equals(callback, that.callback) &&
                    Objects.equals(id, that.id);
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(callback, id);
        }
    }
}
