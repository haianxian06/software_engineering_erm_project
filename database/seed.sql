use homework_system;

insert ignore into class_info (id, class_name, major, course_name, grade, created_by)
values (1, '计算机科学与技术-软件工程班', '计算机科学与技术', '软件工程', '2023', 1);

insert ignore into sys_user (id, username, real_name, student_no, work_no, major, role, class_id)
values
  (1, 'T001', '李老师', null, 'T001', null, 'ADMIN', 1),
  (2, '20230001', '张三', '20230001', null, '计算机科学与技术', 'STUDENT', 1),
  (3, '20230002', '李四', '20230002', null, '计算机科学与技术', 'STUDENT', 1),
  (4, '20230003', '王五', '20230003', null, '计算机科学与技术', 'STUDENT', 1),
  (5, '20230004', '赵六', '20230004', null, '智能科学与技术', 'STUDENT', 1);

insert ignore into class_member (class_id, user_id, member_role)
values
  (1, 1, 'TEACHER'),
  (1, 2, 'STUDENT'),
  (1, 3, 'STUDENT'),
  (1, 4, 'STUDENT');

insert ignore into assignment (id, title, description, deadline, max_size_mb, class_id, created_by, status)
values
  (1, '软件工程作业 2', '提交需求分析与数据库设计文档，文件名会按学号和姓名自动归档。', '2026-12-31 23:59:59', 100, 1, 'teacher', 'PUBLISHED'),
  (2, '数据库系统设计作业', '提交 ER 图、关系模式、建表 SQL 和查询示例。', '2026-12-31 23:59:59', 100, 1, 'teacher', 'PUBLISHED');

insert ignore into assignment_file_type (assignment_id, file_ext)
values
  (1, 'zip'),
  (1, 'docx'),
  (1, 'pdf'),
  (2, 'zip'),
  (2, 'docx'),
  (2, 'pdf'),
  (2, 'sql');
