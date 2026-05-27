# 作业收集管理系统

这是一个适合期末作业展示的 Web 项目框架，技术路线为：

```text
Vue 3 网页端 + Spring Boot 后端 + MySQL 关系型数据库
```

## 目录结构

```text
homework-system
  frontend    Vue 网页端
  backend     Spring Boot 后端
  database    MySQL 建表、测试数据和 ER 图
  docs        技术路线和阿里云部署说明
```

数据库已按第四范式调整，说明见：

```text
docs/normalization-4nf.md
```

## 本地运行

### 1. 初始化数据库

先确保本地 MySQL 已启动，然后执行：

```powershell
mysql -uroot -p < D:\ruanjiangongcheng\homework-system\database\schema.sql
mysql -uroot -p < D:\ruanjiangongcheng\homework-system\database\seed.sql
```

### 2. 启动后端

```powershell
cd D:\ruanjiangongcheng\homework-system\backend
$env:DB_PASSWORD="你的MySQL密码"
mvn spring-boot:run
```

后端地址：

```text
http://localhost:8080
```

### 3. 启动前端

```powershell
cd D:\ruanjiangongcheng\homework-system\frontend
npm install
npm run dev
```

前端地址：

```text
http://localhost:5173
```

## 演示账号

管理员：

```text
身份：管理员
姓名：李老师
账号：teacher
```

学生：

```text
身份：学生
姓名：张三
学号：20230001
```

## 核心功能

- 学生注册：学号、姓名、专业。
- 老师注册：学工号、姓名。
- 老师创建班级，系统按专业自动加入学生。
- 老师给班级内学生设置管理员权限。
- 管理员发布作业。
- 发布作业时选择目标班级。
- 学生上传作业文件。
- 系统保存提交记录。
- 文件自动按学号、姓名、作业名重命名。
- 管理员查看已交名单、未交名单和提交率。
