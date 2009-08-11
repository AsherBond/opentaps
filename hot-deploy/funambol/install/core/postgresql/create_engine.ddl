--
-- This script contains the ddl to create the engine database.
--
-- @version $Id: create_engine.ddl,v 1.13 2007/05/09 09:14:04 nichele Exp $
--

create table fnbl_user (
  username   varchar(255) not null,
  password   varchar(255) not null,
  email      varchar(255),
  first_name varchar(255),
  last_name  varchar(255),
  
  constraint pk_user primary key (username)
);

create table fnbl_device (
  id                   varchar(128) not null,
  description          varchar(255),
  type                 varchar(255),
  client_nonce         varchar(255),
  server_nonce         varchar(255),
  server_password      varchar(255),
  timezone             varchar(32) ,
  convert_date         char(1)     ,
  charset              varchar(16) ,
  address              varchar(50) ,
  msisdn               varchar(50) ,
  notification_builder varchar(255),
  notification_sender  varchar(255),
  id_caps              bigint      ,
  
  constraint pk_device primary key (id)
);

create table fnbl_principal (
  username varchar(255) not null,
  device   varchar(128) not null,
  id       bigint       not null,
  
  constraint pk_principal primary key (id),
  
  constraint fk_device foreign key (device)
  references fnbl_device (id) on delete cascade on update cascade  
);

create table fnbl_sync_source (
  uri        varchar(128) not null,
  config     varchar(255) not null,
  name       varchar(200) not null,
  sourcetype varchar(128) not null,
  
  constraint pk_sync_source primary key (uri)
);

create table fnbl_last_sync (
  principal          bigint       not null,
  sync_source        varchar(128) not null,
  sync_type          integer      not null,
  status             integer    ,  
  last_anchor_server varchar(20),
  last_anchor_client varchar(20),
  start_sync         bigint     ,
  end_sync           bigint     ,
  
  constraint pk_last_sync primary key (principal, sync_source),
  
  constraint fk_principal foreign key (principal) 
  references fnbl_principal (id) on delete cascade on update cascade,

  constraint fk_source foreign key (sync_source) 
  references fnbl_sync_source (uri) on delete cascade on update cascade
);

create table fnbl_client_mapping (
  principal   bigint       not null,
  sync_source varchar(128) not null,
  luid        varchar(200) not null,
  guid        varchar(200) not null,
  last_anchor varchar(20),    
  
  constraint pk_clientmapping primary key (principal, sync_source, luid, guid),
  
  constraint fk_principal_cm foreign key (principal) 
  references fnbl_principal (id) on delete cascade on update cascade,

  constraint fk_source_cm foreign key (sync_source) 
  references fnbl_sync_source (uri) on delete cascade on update cascade
);

create table fnbl_id (
  idspace      varchar(30) not null,
  counter      bigint      not null,
  increment_by int         default 100,
  
  constraint pk_id primary key (idspace)
);

create table fnbl_module (
  id          varchar(128) not null,
  name        varchar(200) not null,
  description varchar(200),
  
  constraint pk_module primary key (id)
);

create table fnbl_sync_source_type (
  id          varchar(128) not null,
  description varchar(200),
  class       varchar(255) not null,
  admin_class varchar(255),
  
  constraint pk_sst primary key (id)
);

create table fnbl_module_sync_source_type (
  module     varchar(128) not null,
  sourcetype varchar(128) not null,
  
  constraint pk_module_sst primary key (module, sourcetype)
);

create table fnbl_connector (
  id          varchar(128) not null,
  name        varchar(200) not null,
  description varchar(200),
  admin_class varchar(255),
    
  constraint pk_connector primary key (id)
);

create table fnbl_module_connector (
  module    varchar(128) not null,
  connector varchar(128) not null,
    
  constraint pk_mod_connector primary key (module, connector)
);

create table fnbl_connector_source_type (
  connector  varchar(128) not null,
  sourcetype varchar(128) not null,
  
  constraint pk_connector_sst primary key (connector, sourcetype)
);

create table fnbl_role (
  role        varchar(128) not null,
  description varchar(200) not null,
 
  constraint pk_role primary key (role)
);

create table fnbl_user_role (
  username varchar(255) not null,
  role     varchar(128) not null,
 
  constraint pk_user_role primary key (username,role),
  
  constraint fk_userrole foreign key (username)
  references fnbl_user (username) on delete cascade on update cascade
);


create table fnbl_device_caps (
  id      bigint      not null,
  version varchar(16) not null,
  man     varchar(100),
  model   varchar(100),
  fwv     varchar(100),
  swv     varchar(100),
  hwv     varchar(100),
  utc     char(1)     not null,
  lo      char(1)     not null,
  noc     char(1)     not null,
  
  constraint pk_device_caps primary key (id)
);


create table fnbl_device_ext (
  id     bigint  not null,
  caps   bigint,
  xname  varchar(255),
  xvalue varchar(255),
  
  constraint pk_dev_ext primary key (id),
  
  constraint fk_dev_ext foreign key (caps)
  references fnbl_device_caps (id) on delete cascade on update cascade
);


create table fnbl_device_datastore (
  id          bigint       not null,
  caps        bigint      ,
  sourceref   varchar(128) not null,
  label       varchar(128),
  maxguidsize integer     ,
  dsmem       char(1)      not null,
  shs         char(1)      not null,
  synccap     varchar(32)  not null,
  
  constraint pk_dev_datastore primary key (id),
  
  constraint fk_dev_datastore foreign key (caps)
  references fnbl_device_caps (id) on delete cascade on update cascade
);

create table fnbl_ds_cttype_rx (
  datastore bigint      not null,
  type      varchar(64) not null,
  version   varchar(16) not null,
  preferred char(1)     not null,
  
  constraint pk_ds_cttype_rx primary key (type,version,datastore),
  
  constraint fk_ds_cttype_rx foreign key (datastore) 
  references fnbl_device_datastore (id) on delete cascade on update cascade
);

create table fnbl_ds_cttype_tx (
  datastore bigint      not null,
  type      varchar(64) not null,
  version   varchar(16) not null,
  preferred char(1)     not null,
  
  constraint pk_ds_cttype_tx primary key (type,version,datastore),
  
  constraint fk_ds_cttype_tx foreign key (datastore) 
  references fnbl_device_datastore (id) on delete cascade on update cascade
);

create table fnbl_ds_ctcap (
  id        bigint      not null,
  datastore bigint      not null,
  type      varchar(64) not null,
  version   varchar(16) not null,
  field     char(1)     not null,
  
  constraint pk_ds_ctcap primary key (id),
  
  constraint fk_ds_ctcap foreign key (datastore) 
  references fnbl_device_datastore (id) on delete cascade on update cascade
);

create table fnbl_ds_ctcap_prop (
  id        bigint      not null,
  ctcap     bigint      not null,
  name      varchar(64) not null,
  label     varchar(128),
  type      varchar(32) ,
  maxoccur  integer     ,
  maxsize   integer     ,
  truncated char(1)     not null,
  valenum   varchar(255),
  
  constraint pk_ds_ctcap_prop primary key (id),

  constraint fk_ds_ctcap_prop foreign key (ctcap) 
  references fnbl_ds_ctcap (id) on delete cascade on update cascade
);

create table fnbl_ds_ctcap_prop_param (
  property bigint      not null,
  name     varchar(64) not null,
  label    varchar(128),
  type     varchar(32) ,
  valenum  varchar(255),
    
  constraint fk_ctcap_propparam foreign key (property) 
  references fnbl_ds_ctcap_prop (id) on delete cascade on update cascade
);

create table fnbl_ds_filter_rx (
  datastore bigint      not null,
  type      varchar(64) not null,
  version   varchar(16) not null,
  
  constraint pk_ds_filter_rx primary key (type,version,datastore),

  constraint fk_ds_filter_rx foreign key (datastore) 
  references fnbl_device_datastore (id) on delete cascade on update cascade
);

create table fnbl_ds_filter_cap (
  datastore  bigint      not null,
  type       varchar(64) not null,
  version    varchar(16) not null,
  keywords   varchar(255),
  properties varchar(255),
  
  constraint pk_ds_filter_cap primary key (type,version,datastore),

  constraint fk_ds_filter_cap foreign key (datastore) 
  references fnbl_device_datastore (id) on delete cascade on update cascade
);

create table fnbl_ds_mem (
  datastore bigint ,
  shared    char(1) not null,
  maxmem    integer,
  maxid     integer,
   
  constraint fk_ds_mem foreign key (datastore) 
  references fnbl_device_datastore (id) on delete cascade on update cascade
);

create index ind_user                         on fnbl_user         (username, password  );
create index ind_principal                    on fnbl_principal    (username, device    );

create index ind_device_ext     on fnbl_device_ext          (caps     );
create index ind_datastore      on fnbl_device_datastore    (caps     );
create index ind_cttype_rx      on fnbl_ds_cttype_rx        (datastore);
create index ind_cttype_tx      on fnbl_ds_cttype_tx        (datastore);
create index ind_ctcap          on fnbl_ds_ctcap            (datastore);
create index ind_ctcap_prop     on fnbl_ds_ctcap_prop       (ctcap    );
create index ind_ctcappropparam on fnbl_ds_ctcap_prop_param (property );
create index ind_filter_rx      on fnbl_ds_filter_rx        (datastore);
create index ind_filter_cap     on fnbl_ds_filter_cap       (datastore);
create index ind_mem            on fnbl_ds_mem              (datastore);
