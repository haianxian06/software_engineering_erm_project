# 第四范式检查

## 实际数据库表

当前项目使用 `homework_system` 数据库。规范化后包含 7 张主要业务表：

- `class_info`：班级表。
- `sys_user`：用户表，保存学生和管理员。
- `class_member`：班级成员表，保存用户属于哪些班级以及班级内权限。
- `assignment`：作业任务表。
- `assignment_file_type`：作业允许文件类型表。
- `submission`：提交记录表。
- `file_record`：文件记录表。

## 第四范式判断方法

第四范式关注的是多值依赖。简单理解：

如果一张表里某个主键对应多组彼此独立的值，就不应该把这些值挤在同一张表或同一个字段里。

## 检查结果

| 表 | 检查结果 |
|---|---|
| `class_info` | 一个班级一行，不存在独立多值属性，符合第四范式 |
| `sys_user` | 一个用户一行，学生保存学号和专业，老师保存学工号，符合第四范式 |
| `class_member` | 一个班级成员关系一行，解决一个学生属于多个班级的问题，符合第四范式 |
| `assignment` | 原来 `file_types` 存 `zip,docx,pdf`，一个字段保存多个值，不符合规范 |
| `assignment_file_type` | 拆分后一个作业类型一行，符合第四范式 |
| `submission` | 一次提交一行，不混存文件列表，符合第四范式 |
| `file_record` | 一个文件一行，多个文件用多行表示，符合第四范式 |

## 拆分前

```text
assignment
id | title | file_types
1  | 软件工程作业 2 | zip,docx,pdf
```

问题：`file_types` 里有多个值。

## 拆分后

```text
assignment
id | title
1  | 软件工程作业 2

assignment_file_type
id | assignment_id | file_ext
1  | 1             | zip
2  | 1             | docx
3  | 1             | pdf
```

这样每一列都是单值，每个多值关系都单独成表。

## 结论

拆分后，当前数据库设计满足本项目范围内的第四范式要求。
