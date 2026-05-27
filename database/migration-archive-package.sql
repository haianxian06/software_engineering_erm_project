use homework_system;

create table if not exists archive_package (
  id bigint primary key auto_increment,
  assignment_id bigint not null,
  zip_name varchar(255) not null,
  zip_path varchar(500) not null,
  package_size bigint not null,
  token_hash varchar(64) not null,
  expires_at datetime not null,
  created_at datetime not null default current_timestamp,
  unique key uk_archive_package_token_hash (token_hash),
  index idx_archive_package_assignment (assignment_id),
  constraint fk_archive_package_assignment foreign key (assignment_id) references assignment(id)
) engine = InnoDB;
