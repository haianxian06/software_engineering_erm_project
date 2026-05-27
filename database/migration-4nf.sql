use homework_system;

create table if not exists assignment_file_type (
  id bigint primary key auto_increment,
  assignment_id bigint not null,
  file_ext varchar(20) not null,
  unique key uk_assignment_file_type (assignment_id, file_ext),
  index idx_assignment_file_type_assignment (assignment_id),
  constraint fk_assignment_file_type_assignment foreign key (assignment_id) references assignment(id)
) engine = InnoDB;

insert ignore into assignment_file_type (assignment_id, file_ext)
select id, trim(substring_index(substring_index(file_types, ',', numbers.n), ',', -1)) as file_ext
from assignment
join (
  select 1 as n union all select 2 union all select 3 union all select 4 union all select 5
  union all select 6 union all select 7 union all select 8 union all select 9 union all select 10
) numbers on char_length(file_types) - char_length(replace(file_types, ',', '')) >= numbers.n - 1
where file_types is not null
  and trim(substring_index(substring_index(file_types, ',', numbers.n), ',', -1)) <> '';

alter table assignment
  drop column file_types;
