use homework_system;

set @ddl = if(
  (select count(*) from information_schema.columns
   where table_schema = database() and table_name = 'assignment' and column_name = 'rename_pattern') = 0,
  'alter table assignment add column rename_pattern varchar(120) not null default ''学号_姓名_作业名'' after max_size_mb',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns
   where table_schema = database() and table_name = 'file_record' and column_name = 'processed_name') = 0,
  'alter table file_record add column processed_name varchar(255) null after mime_type',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns
   where table_schema = database() and table_name = 'file_record' and column_name = 'processed_storage_key') = 0,
  'alter table file_record add column processed_storage_key varchar(500) null after processed_name',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;

set @ddl = if(
  (select count(*) from information_schema.columns
   where table_schema = database() and table_name = 'file_record' and column_name = 'processed_type') = 0,
  'alter table file_record add column processed_type varchar(40) null after processed_storage_key',
  'do 0'
);
prepare stmt from @ddl;
execute stmt;
deallocate prepare stmt;
