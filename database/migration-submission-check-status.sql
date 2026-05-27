use homework_system;

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
