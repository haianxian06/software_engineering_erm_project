# 阿里云部署方案

## 推荐购买

预算为 300 元代金券时，优先选择轻量应用服务器或入门级 ECS。

建议配置：

- 2 核 2G 或 2 核 4G
- Ubuntu 22.04 / Alibaba Cloud Linux
- 带宽 3Mbps 左右即可
- 系统盘 40GB 起

## 服务器上需要安装

- JDK 17
- MySQL 8
- Nginx
- Git

## 部署关系

```text
Windows 电脑：开发、打包、上传代码
阿里云 Linux：运行前端、后端、数据库
MySQL：保存业务数据
OSS：后续保存作业文件
```

## 部署步骤概览

1. 本地执行数据库脚本，确认项目可以跑通。
2. 前端执行 `npm run build`，得到 `dist` 目录。
3. 后端执行 `mvn package`，得到 `target/*.jar`。
4. 将前端 `dist` 上传到服务器 Nginx 目录。
5. 将后端 jar 上传到服务器。
6. 服务器 MySQL 创建 `homework_system` 数据库并导入 SQL。
7. 修改后端数据库密码和上传目录。
8. 启动 Spring Boot 后端。
9. 配置 Nginx 反向代理 `/api` 到后端 8080 端口。

## 后续接入 OSS

当前代码中的 `FileStorageService` 是文件存储层。后续接入阿里云 OSS 时，可以保留 controller、service、repository 和数据库表，只替换 `FileStorageService` 的保存逻辑。
