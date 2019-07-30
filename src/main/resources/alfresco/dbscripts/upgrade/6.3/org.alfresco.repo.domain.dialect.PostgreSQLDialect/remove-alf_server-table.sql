--
-- Title:      Remove alf_server table
-- Database:   PostgreSQL
-- Since:      V6.3
-- Author:     David Edwards
-- Author:     Alex Mukha
--
-- Please contact support@alfresco.com if you need assistance with the upgrade.
--

-- ASSIGN:SYSTEM_NS_ID=id
-- TODO: Find out what below line is for and how to update it for current schema (was in NodeDeleted.sql which is being used as a template) and why they are used
SELECT id FROM alf_namespace WHERE uri = 'http://www.alfresco.org/model/system/1.0';

-- ASSIGN:NEXT_ID=id
SELECT nextVal('alf_qname_seq') AS id;


INSERT INTO alf_qname (id, version, ns_id, local_name) VALUES (${NEXT_ID}, 0, ${SYSTEM_NS_ID}, 'deleted');         -- (optional)


-- ASSIGN:DELETED_TYPE_ID=id
SELECT id FROM alf_qname WHERE ns_id = ${SYSTEM_NS_ID} AND local_name = 'deleted';

-- TODO DROP the indexes

-- TODO DROP the constraints

-- TODO Rename existing alf_transaction to t_alf_transaction

-- TODO Create new alf_transaction table with new schema

-- TODO Migrate the data from t_alf_transaction to the new alf_transaction

-- TODO DROP old table

-- TODO rebuilt indexes and constraints on new table

--
-- Record script finish
--
DELETE FROM alf_applied_patch WHERE id = 'patch.db-V6.3-remove-alf_server-table';
INSERT INTO alf_applied_patch
  (id, description, fixes_from_schema, fixes_to_schema, applied_to_schema, target_schema, applied_on_date, applied_to_server, was_executed, succeeded, report)
  VALUES
  (
    'patch.db-V6.3-remove-alf_server-table', 'Remove alf_server table',
    0, 13000, -1, 13001, null, 'UNKNOWN', ${TRUE}, ${TRUE}, 'Script completed'
  );