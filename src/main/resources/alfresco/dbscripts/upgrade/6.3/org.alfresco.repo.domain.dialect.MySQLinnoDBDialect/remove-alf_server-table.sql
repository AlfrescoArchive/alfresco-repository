--
-- Title:      Remove alf_server table
-- Database:   MySQL
-- Since:      V6.3
-- Author:     David Edwards
-- Author:     Alex Mukha
--
-- Please contact support@alfresco.com if you need assistance with the upgrade.
--

SET FOREIGN_KEY_CHECKS=0;

alter table alf_transaction
    drop index fk_alf_txn_svr,
    drop column server_id;

drop table alf_server;

SET FOREIGN_KEY_CHECKS=1;

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