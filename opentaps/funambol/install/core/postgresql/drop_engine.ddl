--
-- This script contains the ddl to drop the engine database.
--
-- @version $Id: drop_engine.ddl,v 1.4 2007/05/09 09:14:04 nichele Exp $
--
                                  
drop table fnbl_client_mapping         ;
drop table fnbl_last_sync              ;
drop table fnbl_sync_source            ;
drop table fnbl_principal              ;
drop table fnbl_user_role              ;
drop table fnbl_user                   ;
drop table fnbl_device                 ;
drop table fnbl_id                     ;
drop table fnbl_connector_source_type  ;
drop table fnbl_module_connector       ;
drop table fnbl_module_sync_source_type;
drop table fnbl_sync_source_type       ;
drop table fnbl_module                 ;
drop table fnbl_connector              ;
drop table fnbl_role                   ;
drop table fnbl_ds_cttype_rx           ;
drop table fnbl_ds_cttype_tx           ;
drop table fnbl_ds_ctcap_prop_param    ;
drop table fnbl_ds_ctcap_prop          ;
drop table fnbl_ds_ctcap               ;
drop table fnbl_ds_filter_rx           ;
drop table fnbl_ds_filter_cap          ;
drop table fnbl_ds_mem                 ;
drop table fnbl_device_ext             ;
drop table fnbl_device_datastore       ;
drop table fnbl_device_caps            ;
