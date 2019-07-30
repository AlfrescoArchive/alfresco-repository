--
-- Title:      Remove alf_server table
-- Database:   PostgreSQL
-- Since:      V6.3
-- Author:     David Edwards
--
-- Please contact support@alfresco.com if you need assistance with the upgrade.
--

-- ASSIGN:SYSTEM_NS_ID=id
SELECT id FROM alf_namespace WHERE uri = 'http://www.alfresco.org/model/system/1.0';

-- ASSIGN:NEXT_ID=id
SELECT nextVal('alf_qname_seq') AS id;


INSERT INTO alf_qname (id, version, ns_id, local_name) VALUES (${NEXT_ID}, 0, ${SYSTEM_NS_ID}, 'deleted');         -- (optional)


-- ASSIGN:DELETED_TYPE_ID=id
SELECT id FROM alf_qname WHERE ns_id = ${SYSTEM_NS_ID} AND local_name = 'deleted';

-- DROP the indexes
DROP INDEX fk_alf_txn_svr;
DROP INDEX idx_alf_txn_ctms;

-- DROP the constraints alf_transaction
ALTER TABLE alf_transaction DROP CONSTRAINT fk_alf_txn_svr;

-- Rename existing alf_transaction to t_alf_transaction
ALTER TABLE alf_transaction RENAME TO t_alf_transaction;

-- Create new alf_transaction table with new schema
CREATE TABLE alf_transaction
(
    id INT8 NOT NULL,
    version INT8 NOT NULL,
    change_txn_id VARCHAR(56) NOT NULL,
    commit_time_ms INT8,
    PRIMARY KEY (id)
);
CREATE INDEX idx_alf_txn_ctms ON alf_transaction (commit_time_ms, id);

--  Migrate the data from t_alf_transaction to the new alf_transaction
--FOREACH t_alf_node.id system.upgrade.alf_node_deleted_type.batchsize
INSERT INTO alf_transaction
(id, version, change_txn_id, commit_time_ms)
(
    SELECT
       id, version, (CASE WHEN node_deleted THEN ${DELETED_TYPE_ID} ELSE server_id END), change_txn_id, commit_time_ms
    FROM
       t_alf_transaction
    where
       id >= ${LOWERBOUND} AND id <= ${UPPERBOUND}
);
-- DROP old table t_alf_transaction
DROP TABLE t_alf_transaction;

-- DROP alf_server table
DROP TABLE alf_server;

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