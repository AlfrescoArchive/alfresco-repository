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

package org.alfresco.repo.event2;

import java.util.Arrays;
import java.util.List;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.event.v1.model.NodeResource;
import org.alfresco.repo.event.v1.model.RepoEvent;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.util.GUID;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.context.annotation.Description;

/**
 * @author Adina Ababei
 */
public class ChildAssociationRepoEventIT extends AbstractContextAwareRepoEvent
{
    private RepoEventContainer repoEventsContainer;

    @Before
    public void initContainer()
    {
        repoEventsContainer = getRepoEventsContainer();
        repoEventsContainer.reset();
    }

    @Test
    public void testAddChildAssociation()
    {
        final NodeRef parentNodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT);

        checkNumOfEvents(2);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        retryingTransactionHelper.doInTransaction(() ->
                nodeService.addChild(parentNodeRef, childNodeRef, ContentModel.ASSOC_CONTAINS,
                QName.createQName(TEST_NAMESPACE, GUID.generate())));

        List<ChildAssociationRef> childAssociationRefs = retryingTransactionHelper.doInTransaction(() ->
                nodeService.getChildAssocs(parentNodeRef));

        assertEquals(1, childAssociationRefs.size());
        assertFalse(childAssociationRefs.get(0).isPrimary());
    }

    @Test
    public void testRemoveChildAssociation()
    {
        final NodeRef parentNodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT);

        checkNumOfEvents(2);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        ChildAssociationRef childAssociationRef = retryingTransactionHelper.doInTransaction(() ->
                nodeService.addChild(parentNodeRef, childNodeRef, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(TEST_NAMESPACE, GUID.generate())));

        List<ChildAssociationRef> childAssociationRefs = retryingTransactionHelper.doInTransaction(() ->
                nodeService.getChildAssocs(parentNodeRef));

        assertEquals(1, childAssociationRefs.size());
        assertFalse(childAssociationRefs.get(0).isPrimary());

        retryingTransactionHelper.doInTransaction(() ->
                nodeService.removeChildAssociation(childAssociationRef));

        childAssociationRefs = retryingTransactionHelper.doInTransaction(() ->
                nodeService.getChildAssocs(parentNodeRef));

        assertEquals(0, childAssociationRefs.size());
    }

    @Test
    public void testPrimaryAssociation()
    {
        final NodeRef parentNodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT, parentNodeRef);

        checkNumOfEvents(2);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        List<ChildAssociationRef> childAssociationRefs = retryingTransactionHelper.doInTransaction(() ->
                nodeService.getChildAssocs(parentNodeRef));

        assertEquals(1, childAssociationRefs.size());
        assertTrue(childAssociationRefs.get(0).isPrimary());

        deleteNode(childNodeRef);
        deleteNode(parentNodeRef);
    }

    @Test
    @Ignore
    public void testOneChildOneParentMultipleAssociations()
    {
        final NodeRef parentNodeRef = createNode(ContentModel.TYPE_CONTAINER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT);

        checkNumOfEvents(2);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        retryingTransactionHelper.doInTransaction(() ->
                nodeService.addChild(parentNodeRef, childNodeRef, ContentModel.ASSOC_CHILDREN,
                        QName.createQName(TEST_NAMESPACE, GUID.generate())));

        retryingTransactionHelper.doInTransaction(() ->
                nodeService.addChild(parentNodeRef, childNodeRef, ContentModel.ASSOC_ORIGINAL,
                        QName.createQName(TEST_NAMESPACE, GUID.generate())));

        List<ChildAssociationRef> childAssociationRefs = retryingTransactionHelper.doInTransaction(() ->
                nodeService.getChildAssocs(parentNodeRef));

        assertEquals(1, childAssociationRefs.size());
        assertFalse(childAssociationRefs.get(0).isPrimary());
    }

    @Test
    @Description("")
    public void testOneChildListOfParentsAssociations()
    {
        final NodeRef parent1NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent2NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent3NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT);

        List<NodeRef> parents = Arrays.asList(parent1NodeRef, parent2NodeRef, parent3NodeRef);

        checkNumOfEvents(4);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(3);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(4);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        retryingTransactionHelper.doInTransaction(() ->
                nodeService.addChild(parents, childNodeRef, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(TEST_NAMESPACE, GUID.generate())));

        retryingTransactionHelper.doInTransaction(() -> {
            List<ChildAssociationRef> childAssocParent1 = nodeService.getChildAssocs(parent1NodeRef);
            List<ChildAssociationRef> childAssocParent2 = nodeService.getChildAssocs(parent2NodeRef);
            List<ChildAssociationRef> childAssocParent3 = nodeService.getChildAssocs(parent3NodeRef);

            assertEquals(1, childAssocParent1.size());
            assertEquals(1, childAssocParent2.size());
            assertEquals(1, childAssocParent3.size());
            return null;
        });
    }

    @Test
    public void testOneChildMultipleParentsSameTransaction()
    {
        final NodeRef parent1NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent2NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent3NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT);

        List<NodeRef> parents = Arrays.asList(parent1NodeRef, parent2NodeRef, parent3NodeRef);
        checkNumOfEvents(4);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(3);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(4);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        retryingTransactionHelper.doInTransaction(() -> {
            for (NodeRef parent : parents)
            {
                nodeService.addChild(parent, childNodeRef, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(TEST_NAMESPACE, GUID.generate()));
            }

            return null;
        });

        retryingTransactionHelper.doInTransaction(() -> {
            List<ChildAssociationRef> childAssocParent1 = nodeService.getChildAssocs(parent1NodeRef);
            List<ChildAssociationRef> childAssocParent2 = nodeService.getChildAssocs(parent2NodeRef);
            List<ChildAssociationRef> childAssocParent3 = nodeService.getChildAssocs(parent3NodeRef);

            assertEquals(1, childAssocParent1.size());
            assertEquals(1, childAssocParent2.size());
            assertEquals(1, childAssocParent3.size());
            return null;
        });
    }

    @Test
    public void testOneChildMultipleParentsDifferentTransaction()
    {
        final NodeRef parent1NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent2NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent3NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT);

        List<NodeRef> parents = Arrays.asList(parent1NodeRef, parent2NodeRef, parent3NodeRef);

        checkNumOfEvents(4);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(3);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(4);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        for (NodeRef parent : parents)
        {
            retryingTransactionHelper.doInTransaction(() ->
                    nodeService.addChild(parent, childNodeRef, ContentModel.ASSOC_CONTAINS,
                            QName.createQName(TEST_NAMESPACE, GUID.generate())));
        }

        retryingTransactionHelper.doInTransaction(() -> {
            List<ChildAssociationRef> childAssocParent1 = nodeService.getChildAssocs(parent1NodeRef);
            List<ChildAssociationRef> childAssocParent2 = nodeService.getChildAssocs(parent2NodeRef);
            List<ChildAssociationRef> childAssocParent3 = nodeService.getChildAssocs(parent3NodeRef);

            assertEquals(1, childAssocParent1.size());
            assertEquals(1, childAssocParent2.size());
            assertEquals(1, childAssocParent3.size());
            return null;
        });
    }

    @Test
    public void testOneParentMultipleChildrenSameTransaction()
    {
        final NodeRef parentNodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef child1NodeRef = createNode(ContentModel.TYPE_CONTENT);
        final NodeRef child2NodeRef = createNode(ContentModel.TYPE_CONTENT);
        final NodeRef child3NodeRef = createNode(ContentModel.TYPE_CONTENT);

        List<NodeRef> children = Arrays.asList(child1NodeRef, child2NodeRef, child3NodeRef);

        checkNumOfEvents(4);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(3);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(4);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        retryingTransactionHelper.doInTransaction(() -> {
            for (NodeRef child : children)
            {
                nodeService.addChild(parentNodeRef, child, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(TEST_NAMESPACE, GUID.generate()));
            }

           return null;
        });

        retryingTransactionHelper.doInTransaction(() -> {
            List<ChildAssociationRef> childAssocParent = nodeService.getChildAssocs(parentNodeRef);

            assertEquals(3, childAssocParent.size());
            return null;
        });
    }

    @Test
    public void testOneParentMultipleChildrenDifferentTransaction()
    {
        final NodeRef parentNodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef child1NodeRef = createNode(ContentModel.TYPE_CONTENT);
        final NodeRef child2NodeRef = createNode(ContentModel.TYPE_CONTENT);
        final NodeRef child3NodeRef = createNode(ContentModel.TYPE_CONTENT);

        List<NodeRef> children = Arrays.asList(child1NodeRef, child2NodeRef, child3NodeRef);

        checkNumOfEvents(4);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(3);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(4);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        for (NodeRef child : children)
        {
            retryingTransactionHelper.doInTransaction(() ->
                    nodeService.addChild(parentNodeRef, child, ContentModel.ASSOC_CONTAINS,
                            QName.createQName(TEST_NAMESPACE, GUID.generate())));
        }

        retryingTransactionHelper.doInTransaction(() -> {
            List<ChildAssociationRef> childAssocParent = nodeService.getChildAssocs(parentNodeRef);

            assertEquals(3, childAssocParent.size());
            return null;
        });
    }

    @Test
    public void testDeleteAssociationsOneChildMultipleParentsSameTransaction()
    {
        final NodeRef parent1NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent2NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent3NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT);

        List<NodeRef> parents = Arrays.asList(parent1NodeRef, parent2NodeRef, parent3NodeRef);

        checkNumOfEvents(4);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(3);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(4);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        retryingTransactionHelper.doInTransaction(() ->
                nodeService.addChild(parents, childNodeRef, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(TEST_NAMESPACE, GUID.generate())));

        List<ChildAssociationRef> listChildAssociationRefs = retryingTransactionHelper.doInTransaction(() -> {
            List<ChildAssociationRef> childAssocParent1 = nodeService.getChildAssocs(parent1NodeRef);
            List<ChildAssociationRef> childAssocParent2 = nodeService.getChildAssocs(parent2NodeRef);
            List<ChildAssociationRef> childAssocParent3 = nodeService.getChildAssocs(parent3NodeRef);

            assertEquals(1, childAssocParent1.size());
            assertEquals(1, childAssocParent2.size());
            assertEquals(1, childAssocParent3.size());

            return Arrays.asList(childAssocParent1.get(0), childAssocParent2.get(0), childAssocParent3.get(0));
        });

        retryingTransactionHelper.doInTransaction(() -> {
            for (ChildAssociationRef childAssociationRef : listChildAssociationRefs)
            {
                nodeService.removeChildAssociation(childAssociationRef);
            }
            return null;
        });

        repoEventsContainer.reset();
    }

    @Test
    public void testDeleteAssociationOneParentMultipleChildrenDifferentTransactions()
    {
        final NodeRef parent1NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent2NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent3NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT);

        List<NodeRef> parents = Arrays.asList(parent1NodeRef, parent2NodeRef, parent3NodeRef);

        checkNumOfEvents(4);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(3);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(4);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        retryingTransactionHelper.doInTransaction(() ->
                nodeService.addChild(parents, childNodeRef, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(TEST_NAMESPACE, GUID.generate())));

        List<ChildAssociationRef> listChildAssociationRefs = retryingTransactionHelper.doInTransaction(() -> {
            List<ChildAssociationRef> childAssocParent1 = nodeService.getChildAssocs(parent1NodeRef);
            List<ChildAssociationRef> childAssocParent2 = nodeService.getChildAssocs(parent2NodeRef);
            List<ChildAssociationRef> childAssocParent3 = nodeService.getChildAssocs(parent3NodeRef);

            assertEquals(1, childAssocParent1.size());
            assertEquals(1, childAssocParent2.size());
            assertEquals(1, childAssocParent3.size());

            return Arrays.asList(childAssocParent1.get(0), childAssocParent2.get(0), childAssocParent3.get(0));
        });

        for (ChildAssociationRef childAssociationRef : listChildAssociationRefs)
        {
            retryingTransactionHelper.doInTransaction(() ->
                nodeService.removeChildAssociation(childAssociationRef));
        }
    }

    @Test
    public void testDeleteParentWithMultipleChildAssociations()
    {
        final NodeRef parentNodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef child1NodeRef = createNode(ContentModel.TYPE_CONTENT);
        final NodeRef child2NodeRef = createNode(ContentModel.TYPE_CONTENT);
        final NodeRef child3NodeRef = createNode(ContentModel.TYPE_CONTENT);

        List<NodeRef> children = Arrays.asList(child1NodeRef, child2NodeRef, child3NodeRef);

        checkNumOfEvents(4);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(3);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(4);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        retryingTransactionHelper.doInTransaction(() -> {
            for (NodeRef child : children)
            {
                nodeService.addChild(parentNodeRef, child, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(TEST_NAMESPACE, GUID.generate()));
            }

            return null;
        });

        retryingTransactionHelper.doInTransaction(() -> {
            List<ChildAssociationRef> childAssocParent = nodeService.getChildAssocs(parentNodeRef);

            assertEquals(3, childAssocParent.size());
            return null;
        });

        deleteNode(parentNodeRef);
    }

    @Test
    public void testDeleteChildWithMultipleParentAssociations()
    {
        final NodeRef parent1NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent2NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent3NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT);

        List<NodeRef> parents = Arrays.asList(parent1NodeRef, parent2NodeRef, parent3NodeRef);

        checkNumOfEvents(4);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(3);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(4);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        retryingTransactionHelper.doInTransaction(() ->
                nodeService.addChild(parents, childNodeRef, ContentModel.ASSOC_CONTAINS,
                        QName.createQName(TEST_NAMESPACE, GUID.generate())));

        retryingTransactionHelper.doInTransaction(() -> {
            List<ChildAssociationRef> childAssocParent1 = nodeService.getChildAssocs(parent1NodeRef);
            List<ChildAssociationRef> childAssocParent2 = nodeService.getChildAssocs(parent2NodeRef);
            List<ChildAssociationRef> childAssocParent3 = nodeService.getChildAssocs(parent3NodeRef);

            assertEquals(1, childAssocParent1.size());
            assertEquals(1, childAssocParent2.size());
            assertEquals(1, childAssocParent3.size());
            return null;
        });

        deleteNode(childNodeRef);
    }

    @Test
    @Ignore
    public void testMoveNodeWithChildAssociation()
    {
        final NodeRef parent1NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef parent2NodeRef = createNode(ContentModel.TYPE_FOLDER);
        final NodeRef childNodeRef = createNode(ContentModel.TYPE_CONTENT);

        checkNumOfEvents(3);

        RepoEvent<NodeResource> resultRepoEvent = repoEventsContainer.getEvent(1);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(2);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        resultRepoEvent = repoEventsContainer.getEvent(3);
        assertEquals("Wrong repo event type.", EventType.NODE_CREATED.getType(), resultRepoEvent.getType());

        QName test = QName.createQName(TEST_NAMESPACE, GUID.generate());

        retryingTransactionHelper.doInTransaction(() ->
                nodeService.addChild(parent1NodeRef, childNodeRef, ContentModel.ASSOC_CONTAINS,
                        test));

        List<ChildAssociationRef> childAssociationRefs = retryingTransactionHelper.doInTransaction(() ->
                nodeService.getChildAssocs(parent1NodeRef));

        assertEquals(1, childAssociationRefs.size());
        assertFalse(childAssociationRefs.get(0).isPrimary());

        ChildAssociationRef childAssociationRef = retryingTransactionHelper.doInTransaction(() ->
            nodeService.moveNode(
                    childNodeRef,
                    parent2NodeRef,
                    ContentModel.ASSOC_CONTAINS,
                    test));

        childAssociationRefs = retryingTransactionHelper.doInTransaction(() ->
                nodeService.getChildAssocs(parent2NodeRef));

        assertEquals(1, childAssociationRefs.size());
        assertTrue(childAssociationRefs.get(0).isPrimary());
        assertFalse(childAssociationRef.isPrimary());
    }
}
