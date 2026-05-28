create database if not exists homework_system
  default character set utf8mb4
  collate utf8mb4_0900_ai_ci;

use homework_system;

create table if not exists class_info (
  id bigint primary key auto_increment,
  class_name varchar(100) not null,
  major varchar(100) not null,
  course_name varchar(100) null,
  grade varchar(20) not null default '',
  created_by bigint null,
  created_at datetime not null default current_timestamp
) engine = InnoDB;

create table if not exists sys_user (
  id bigint primary key auto_increment,
  username varchar(80) not null,
  real_name varchar(80) not null,
  student_no varchar(40) null,
  work_no varchar(40) null,
  major varchar(100) null,
  role varchar(20) not null,
  class_id bigint null,
  created_at datetime not null default current_timestamp,
  unique key uk_user_username (username),
  unique key uk_user_student_no (student_no),
  unique key uk_user_work_no (work_no),
  constraint fk_user_class foreign key (class_id) references class_info(id)
) engine = InnoDB;

create table if not exists class_member (
  id bigint primary key auto_increment,
  class_id bigint not null,
  user_id bigint not null,
  member_role varchar(20) not null default 'STUDENT',
  created_at datetime not null default current_timestamp,
  unique key uk_class_member (class_id, user_id),
  index idx_class_member_user (user_id),
  constraint fk_class_member_class foreign key (class_id) references class_info(id),
  constraint fk_class_member_user foreign key (user_id) references sys_user(id)
) engine = InnoDB;

create table if not exists assignment (
  id bigint primary key auto_increment,
  title varchar(160) not null,
  description text null,
  deadline datetime not null,
  max_size_mb int not null default 100,
  rename_pattern varchar(120) not null default '学号_姓名_作业名',
  class_id bigint not null,
  created_by varchar(80) not null,
  status varchar(20) not null default 'PUBLISHED',
  created_at datetime not null default current_timestamp,
  index idx_assignment_class_id (class_id),
  constraint fk_assignment_class foreign key (class_id) references class_info(id)
) engine = InnoDB;

create table if not exists assignment_file_type (
  id bigint primary key auto_increment,
  assignment_id bigint not null,
  file_ext varchar(20) not null,
  unique key uk_assignment_file_type (assignment_id, file_ext),
  index idx_assignment_file_type_assignment (assignment_id),
  constraint fk_assignment_file_type_assignment foreign key (assignment_id) references assignment(id)
) engine = InnoDB;

create table if not exists submission (
  id bigint primary key auto_increment,
  assignment_id bigint not null,
  student_id bigint not null,
  submit_time datetime not null default current_timestamp,
  status varchar(20) not null default 'SUBMITTED',
  version_no int not null default 1,
  is_final tinyint(1) not null default 1,
  index idx_submission_assignment (assignment_id),
  index idx_submission_student (student_id),
  constraint fk_submission_assignment foreign key (assignment_id) references assignment(id),
  constraint fk_submission_student foreign key (student_id) references sys_user(id)
) engine = InnoDB;

create table if not exists file_record (
  id bigint primary key auto_increment,
  submission_id bigint not null,
  original_name varchar(255) not null,
  stored_name varchar(255) not null,
  storage_key varchar(500) not null,
  file_size bigint not null,
  mime_type varchar(120) null,
  processed_name varchar(255) null,
  processed_storage_key varchar(500) null,
  processed_type varchar(40) null,
  upload_time datetime not null default current_timestamp,
  index idx_file_submission (submission_id),
  constraint fk_file_submission foreign key (submission_id) references submission(id)
) engine = InnoDB;

create table if not exists submission_review (
  id bigint primary key auto_increment,
  submission_id bigint not null,
  score decimal(5,2) not null,
  comment text null,
  reviewed_by bigint null,
  reviewed_at datetime not null default current_timestamp,
  created_at datetime not null default current_timestamp,
  unique key uk_submission_review_submission (submission_id),
  index idx_submission_review_reviewer (reviewed_by),
  constraint fk_submission_review_submission foreign key (submission_id) references submission(id),
  constraint fk_submission_review_user foreign key (reviewed_by) references sys_user(id)
) engine = InnoDB;

create table if not exists submission_check_status (
  id bigint primary key auto_increment,
  assignment_id bigint not null,
  class_id bigint not null,
  student_id bigint not null,
  has_submitted tinyint(1) not null default 0,
  final_submission_id bigint null,
  checked_at datetime not null default current_timestamp,
  unique key uk_submission_check_assignment_student (assignment_id, student_id),
  index idx_submission_check_assignment (assignment_id),
  index idx_submission_check_class (class_id),
  index idx_submission_check_student (student_id),
  constraint fk_submission_check_assignment foreign key (assignment_id) references assignment(id),
  constraint fk_submission_check_class foreign key (class_id) references class_info(id),
  constraint fk_submission_check_student foreign key (student_id) references sys_user(id),
  constraint fk_submission_check_submission foreign key (final_submission_id) references submission(id)
) engine = InnoDB;

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
