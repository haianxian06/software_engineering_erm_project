use homework_system;

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
