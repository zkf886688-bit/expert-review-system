# 专家评审系统

基于 Vue 3、Spring Boot 3 和 MySQL 的在线评审系统，支持专家信息管理、任务分配、评分与意见提交、结果汇总，以及申报人整改和多轮复审。

## 功能

| 模块 | 功能 |
| --- | --- |
| 专家管理 | 专家档案、资质审核、材料上传、回避关系、履约记录 |
| 评审任务 | 任务发布、项目导入、手动或加权分配专家、截止时间管理、催办 |
| 在线评审 | 邀请确认、评分、评审意见、附件提交、意见模板 |
| 结果汇总 | 得分排名、标准差、多轮结果对比、Excel / Word / PDF 导出 |
| 申报与整改 | 申报进度、材料提交、整改反馈、复审 |
| 系统管理 | 角色权限、数据域、通知模板、审计日志、运行状态 |

管理员维护任务与专家库；专家处理分配的评审；申报人查看本人项目及整改要求；领导查看脱敏汇总。`REVIEW_ADMIN` 的任务访问范围受数据域限制。

前端使用 Element Plus、Pinia 和 ECharts；后端使用 Spring Security、MyBatis-Plus 和 JWT。Redis 可用于多实例验证码存储。

## 本地运行

环境：JDK 17、Maven、Node.js 20、MySQL 8。

在项目根目录打开终端，通过 MySQL 客户端创建数据库：

```text
mysql -u root -p
```

进入 MySQL 后执行：

```sql
SOURCE sql/schema.sql;
```

数据库连接配置位于 `backend/src/main/resources/application.yml`，启动前按本机环境调整。请使用独立的开发数据库：默认初始化逻辑会维护演示账号与任务，并可能重置演示账号密码。

分别在两个终端启动后端和前端：

```bash
# 项目根目录，终端一
cd backend
mvn -s ../maven-settings.xml spring-boot:run
```

```bash
# 项目根目录，终端二
cd frontend
npm ci
npm run dev
```

| 地址 | 用途 |
| --- | --- |
| <http://localhost:5173> | 前端 |
| <http://localhost:8080/swagger-ui.html> | API 文档 |
| <http://localhost:8080/api/public/health> | 健康检查 |

## 开发账号与示例数据

以下账号由开发初始化逻辑维护，仅用于本地测试。

| 账号 | 密码 | 角色 |
| --- | --- | --- |
| `admin` | `admin123` | 管理员 |
| `leader` | `leader123` | 领导 |
| `applicant` | `applicant123` | 申报人 |
| `expert1` | `expert123` | 专家，需绑定专家档案 |

默认配置不自动填充专家库。示例 Excel 的生成与导入方法见 [示例数据](demo/README.md)。登录页的演示 SSO 用于本地调试，不依赖外部身份服务。

## 测试

后端测试使用 H2 内存数据库。在项目根目录执行：

```bash
cd backend
mvn -s ../maven-settings.xml test
```

前端单元测试、构建与端到端测试，在项目根目录另开终端执行：

```bash
cd frontend
npm ci
npm test
npm run build
npx playwright install chromium
npm run test:e2e
```

端到端测试自动启动 H2 测试后端和前端预览服务，分别使用 8090、4173 端口。本地默认使用 Chrome，CI 使用 Playwright Chromium。自动检查配置见 [ci.yml](.github/workflows/ci.yml)。

## 配置

后端配置位于 `backend/src/main/resources/`；环境变量示例见 [.env.example](.env.example)。

| 配置项 | 用途 |
| --- | --- |
| `app.demo.reset-passwords` | 启动时是否重置演示账号密码 |
| `app.demo.seed-experts` / `app.demo.seed-tasks` | 演示专家与任务初始化 |
| `app.sso.mock-enabled` | 演示 SSO |
| `app.sso.oauth2.*` | 外部 OAuth2 身份服务 |
| `app.mail.enabled` / `app.sms.enabled` | 邮件与短信通知 |
| `app.captcha.redis-enabled` | 使用 Redis 存储验证码 |
| `app.pii.encryption-key` | 专家敏感字段加密密钥 |

Docker Compose 配置用于本地演示。在项目根目录复制 `.env.example` 为 `.env`，设置数据库密码和 JWT 密钥后运行：

```bash
docker compose up -d --build
```

访问 <http://localhost>。该配置保留演示账号和初始化逻辑，生产部署前需要另行配置认证、凭据、访问范围和数据库迁移。

## 目录

```text
backend/            Spring Boot 服务与测试
frontend/           Vue 页面、组件与测试
sql/                数据库结构和迁移脚本
demo/               示例数据生成说明
scripts/            迁移、备份与检查脚本
deploy/             nginx 配置示例
docker-compose.yml  本地容器配置
```

运行数据库、上传材料和日志不纳入版本管理。排查接口问题时，可通过健康检查与日志中的 `requestId` 定位请求。
