use homework_system;

alter table class_info
  add column course_name varchar(100) null after major,
  add column created_by bigint null after grade;

alter table sys_user
  add column work_no varchar(40) null after student_no,
  add column major varchar(100) null after work_no;

alter table sys_user
  add unique key uk_user_work_no (work_no);

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

update class_info
set course_name = coalesce(course_name, '软件工程'),
    created_by = coalesce(created_by, 1)
where id = 1;

update sys_user u
left join class_info c on c.id = u.class_id
set u.major = coalesce(u.major, c.major)
where u.role = 'STUDENT';

update sys_user
set work_no = coalesce(work_no, 'T001'),
    username = 'T001'
where id = 1;

insert ignore into class_member (class_id, user_id, member_role)
select class_id, id,
       case when role = 'ADMIN' then 'TEACHER' else 'STUDENT' end
from sys_user
where class_id is not null;
