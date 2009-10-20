--
-- Initialization data for the Funambol Data Synchronization Server
--
-- @version $Id: init_engine.sql,v 1.6 2007/03/28 09:44:29 luigiafassina Exp $
--

--
-- FNBL_USER
--
insert into fnbl_user (username, password, email, first_name, last_name)
  values('admin', 'sa', 'admin@funambol.com', 'admin', 'admin');

insert into fnbl_user (username, password, email, first_name, last_name)
  values('guest', 'guest', 'guest@funambol.com', 'guest', 'guest');

--
-- FNBL_ID
--
insert into fnbl_id values('device', 0, 100);

insert into fnbl_id values('principal', 0, 100);

insert into fnbl_id values('guid', 3, 100);

insert into fnbl_id values('datastore', 0, 100);

insert into fnbl_id values('capability', 0, 100);

insert into fnbl_id values('ext', 0, 100);

insert into fnbl_id values('ctcap', 0, 100);

insert into fnbl_id values('ctcap_property', 0, 100);

--
-- FNBL_ROLE
--
insert into fnbl_role values('sync_user','User');

insert into fnbl_role values('sync_administrator','Administrator');

--
-- FNBL_USER_ROLE
--
insert into fnbl_user_role values('admin','sync_administrator');

insert into fnbl_user_role values('guest','sync_user');
