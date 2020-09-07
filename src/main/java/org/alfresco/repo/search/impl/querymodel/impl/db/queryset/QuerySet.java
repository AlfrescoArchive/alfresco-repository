package org.alfresco.repo.search.impl.querymodel.impl.db.queryset;

import org.alfresco.service.namespace.QName;

public interface QuerySet
{

    String getTableName();

    String getColumnName(QName qname);

}