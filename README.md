# simple-tiktok

一个基于 Spring Boot + Vue 的短视频项目，包含：
- 用户端（`frontend`）
- 管理端（`admin`）
- 后端服务（`src/main/java/com/example/simpletiktok`）

## 技术栈
- 后端：Java 17、Spring Boot 3.5.x、MyBatis-Plus、Redis、RabbitMQ、MySQL、Qdrant、LangChain4j
- 用户端：Vue 3 + Vite
- 管理端：Vue 2 + Element UI

## 目录结构
- `src/main/java/com/example/simpletiktok`：后端业务代码
- `src/main/resources`：配置、SQL、静态资源
- `src/main/resources/static/simple_tiktok.sql`：数据库建表脚本
- `frontend`：用户端前端
- `admin`：管理端前端

## 拉取后本地启动（完整流程）

### 1. 环境准备
- JDK 17
- Maven 3.9+
- Node.js 18+
- MySQL 8+
- Redis 6+
- RabbitMQ 3.8+
- Qdrant 1.15+

### 2. 初始化 MySQL
先创建数据库（UTF8MB4）：

```sql
CREATE DATABASE IF NOT EXISTS simple_tiktok
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
```

然后执行脚本：
- `src/main/resources/static/simple_tiktok.sql`

说明：
- 脚本主要是建表，不会自动创建数据库。
- `admin` 表默认无账号，请自行插入一条管理员数据（当前登录逻辑为明文比对）。

示例：

```sql
INSERT INTO admin(username, password) VALUES ('admin', '123456');
```

### 3. 初始化 RabbitMQ
项目默认使用：
- `virtual-host`：`/SimpleTikTok`
- `username`：`root`
- `password`：见 `application-dev.yml`（建议改成你自己的）

需要在 RabbitMQ 管理台创建 vhost，并给用户授权。

队列/交换机说明：
- 由项目启动后自动声明（`@RabbitListener + @QueueBinding`），不需要手工创建。
- 主要名称：
  - `video.audit.queue` / `video.audit.exchange`
  - `video.like.model.queue` / `video.like.model.exchange`
  - `type.label.sync.queue` / `type.label.sync.exchange`

### 4. 初始化 Redis
配置 `host`、`port`、`password`、`database` 与本地 Redis 一致。

### 5. 初始化 Qdrant
默认集合名：`SimpleTikTok`（见 `app.qdrant.collection-name`）。

建议：
- 提前创建集合 `SimpleTikTok`，向量维度使用 `1024`（对应当前 embedding 模型配置）。
- 若不手动创建，也可在业务首次写入向量时由程序侧触发创建（取决于运行路径）。

### 6. 修改后端配置
默认启动 profile：`dev`，配置文件：
- `src/main/resources/application.yml`
- `src/main/resources/application-dev.yml`

重点修改：
- `spring.datasource`（MySQL）
- `spring.data.redis`（Redis）
- `spring.rabbitmq`（RabbitMQ）
- `app.qdrant`（Qdrant）
- `aliyun.oss`（OSS）
- `langchain4j.community.dashscope`（大模型 API Key）

建议把密钥放环境变量，不要写死在配置文件。

### 7. 启动后端
在项目根目录执行：

```bash
mvn clean package
mvn spring-boot:run
```

默认端口：`8080`

### 8. 启动用户端（frontend）

```bash
cd frontend
npm install
npm run dev
```

默认端口：`5378`，开发代理到 `http://127.0.0.1:8080`。

### 9. 启动管理端（admin）

```bash
cd admin
npm install
npm run dev
```

默认端口：`9528`，开发代理到 `http://127.0.0.1:8080`。

## 前端打包
用户端：

```bash
cd frontend
npm run build
```

管理端：

```bash
cd admin
npm run build
```
