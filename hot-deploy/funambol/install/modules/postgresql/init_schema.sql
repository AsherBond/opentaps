--
-- Copyright (c) 2006 - 2007 Open Source Strategies, Inc.
-- 
-- This program is free software; you can redistribute it and/or modify
-- it under the terms of the Honest Public License.
-- 
-- This program is distributed in the hope that it will be useful,
-- but WITHOUT ANY WARRANTY; without even the implied warranty of
-- MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-- Honest Public License for more details.
-- 
-- You should have received a copy of the Honest Public License
-- along with this program; if not, write to Funambol,
-- 643 Bair Island Road, Suite 305 - Redwood City, CA 94063, USA
--
-- Initialization data for the OT Sync Module
-- Cameron Smith - Database, Lda - www.database.co.mz
--

-- 1. Register our module TODO: what conventions do we have here?
delete from fnbl_module where id='opentaps';
insert into fnbl_module (id, name, description) values('opentaps','opentaps','OpenTAPS CRM');

-- 2.1. Register our SyncSource type
delete from fnbl_sync_source_type where id='opentaps';
insert into fnbl_sync_source_type(id, description, class, admin_class) values('opentaps','OpenTAPs SyncSource','org.opentaps.funambol.sync.EntitySyncSource','org.opentaps.funambol.sync.EntitySyncSourceConfigPanel');

-- 2.2. Register our connector
delete from fnbl_connector where id='opentaps';
insert into fnbl_connector(id, name, description, admin_class) values('opentaps','OpenTAPS','OpenTAPS CRM Connector','');

-- 3. Relate our connector and source type with our module
delete from fnbl_connector_source_type where connector='opentaps' and sourcetype='opentaps';
insert into fnbl_connector_source_type(connector, sourcetype) values('opentaps','opentaps');

delete from fnbl_module_connector where module='opentaps' and connector='opentaps';
insert into fnbl_module_connector(module, connector) values('opentaps','opentaps');
  
  
