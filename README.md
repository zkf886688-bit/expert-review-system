# 外审专家管理系统

面向评审业务流程的 Web 应用，覆盖专家库维护、任务分配、在线评分与意见提交、汇总导出、申报人整改及多轮复审。

本公开仓库提供前后端源码、数据库结构与演示数据生成代码，不包含运行数据库、上传材料、真实专家资料、日志或本地密钥。

技术栈：**Vue 3 + Spring Boot 3 + MySQL**。本地开发前后端分离，提供 Docker Compose 本地演示配置。

---

## 怎么跑起来

**环境**：JDK 17、Node 18+、MySQL 8、Maven。

Windows 可以直接用根目录脚本：

```powershell
# 第一次先建库
mysql -u root -p < sql/schema.sql

# 两个终端分别执行
cd <你的项目目录>
.\start-backend.ps1
cd <你的项目目录>
.\start-frontend.ps1
```

手动启动也行：

```bash
cd backend && mvn -s ../maven-settings.xml spring-boot:run
cd frontend && npm install && npm run dev
```

| 地址 | 说明 |
|------|------|
| http://localhost:5173 | 前端 |
| http://localhost:8080/swagger-ui.html | 接口文档 |
| http://localhost:8080/api/public/health | 健康检查 |

数据库如果是老库、缺字段，跑 `scripts/migrate-db.ps1`，或者重启后端（会自动补一部分表结构）。更稳妥是执行 `sql/migration_all.sql`。

---

## 测试账号

| 账号 | 密码 | 能干什么 |
|------|------|----------|
| admin | admin123 | 管理员，基本都能点 |
| leader | leader123 | 领导，只看脱敏汇总 |
| applicant | applicant123 | 申报人，看自己的项目和整改 |
| expert1 | expert123 | 专家，登录后进「我的评审」（库内另有 9 名演示专家无账号） |

当前默认配置不自动填充专家库。可按 `demo/README.md` 生成并导入合成专家示例；`expert1` 的可用性取决于专家档案绑定。示例中的姓名、单位与联系方式仅用于演示。

登录页有个 **演示 SSO**，本地开发用的，不用配真实 IdP。

---

## 系统大概有哪些东西

**管理员 / 评审管理员**

- 专家库维护、资质材料上传、回避关系配置、**待审核专家**通过/驳回
- 随机分配专家时按**负载与履约**加权优选（非纯随机）
- 评审任务：建任务（7 种评审类型）、发布、导项目、随机/手动分专家、催办、改截止时间
- 汇总页按得分排名，含标准差；导出 Excel / Word / PDF（PDF 带水印和签章区）
- 多轮复审、要求申报人整改
- 审计日志、用户管理、通知模板

**专家**

- 我的评审（待接受 / 待提交 / 快截止几个 Tab）
- 按任务类型动态结论选项、常用意见模板、在线评分表、补充附件上传

**申报人**

- 我的申报：看进度、传材料、整改

**领导**

- 只看汇总，专家姓名会脱敏

**其它**

- 消息中心、统计大屏（ECharts，含专家标签/状态分布）
- 登录验证码（生产默认开启，测试 profile 关闭；多实例可开 Redis 存储 `CAPTCHA_REDIS_ENABLED=true`）
- 登录失败锁定（默认 5 次失败锁 15 分钟）、生产环境关闭 Swagger
- 材料下载走 ID 鉴权，专家身份证/银行账号加密存储
- 邮件/短信默认关着，开了但没配 SMTP 会在后端日志里 `[邮件模拟]`
- Prometheus：`/actuator/prometheus`，日志里带 `requestId`

角色多了个 **数据域**：`REVIEW_ADMIN` 只能看自己域里的任务，全站管理员不受影响。

---

## 答辩 / 录屏怎么演示

不用全点一遍，按这个顺序够用了：

1. 登录（或点演示 SSO）
2. 进度看板 → 评审任务 → 进某个任务详情（分配、催办）
3. 换专家账号 → 我的评审 → 提交一条意见
4. 回到 admin → 汇总 → 多轮对比 Tab → 导 PDF
5. 系统设置里瞄一眼健康检查

截图的话：登录页、看板、任务详情、专家工作台、汇总多轮对比、设置页，可覆盖主要业务流程。

---

## 跑测试

```bash
# 后端，使用 H2 内存数据库运行测试
cd backend
mvn -s ../maven-settings.xml test

# 前端 E2E（会占 8090 端口，别冲突）
cd frontend
npm ci && npm test && npm run build && npm run test:e2e
```

根目录还有 `scripts/run-all-tests.ps1` 一次跑完。CI 配置在 `.github/workflows/ci.yml`。

---

## 部署

**Docker（本地演示）**

```bash
cp .env.example .env   # 改 JWT 和数据库密码，别留 change-me
docker compose up -d --build
```

浏览器访问 http://localhost ，API 走 nginx 反代。

**裸机**

```bash
SPRING_PROFILES_ACTIVE=prod \
JWT_SECRET=一串足够长的随机字符 \
DB_PASSWORD=你的库密码 \
mvn -s ../maven-settings.xml spring-boot:run
```

上线前跑 `scripts/check-deploy.ps1` 自检。HTTPS 参考 `deploy/nginx-https.conf.example`。

生产环境记得改：

- `.env` 里的 `JWT_SECRET`、`MYSQL_ROOT_PASSWORD`
- `CORS_ALLOWED_ORIGIN_PATTERNS` 设成真实前端域名
- 需要通知时再开 `MAIL_*` / `SMS_*`（变量说明见 `.env.example`）

---

## 目录结构

```
expert-review-system/
├── backend/          Java 后端
├── frontend/         Vue 前端
├── sql/              建表和 migration
├── scripts/          迁移、自检、测试脚本
├── deploy/           nginx 配置示例
├── docker-compose.yml
├── .env.example
└── maven-settings.xml
```

---

## 配置入口

常用项在 `backend/src/main/resources/application.yml`：

- `app.mail.enabled` / `app.sms.enabled` — 通知开关
- `app.sso.mock-enabled` — 演示 SSO
- `app.demo.reset-passwords` — 开发环境每次启动重置演示账号密码（生产 profile 里是 false）
- `app.expert.inactive-years` — 超过该年数未参与评审的专家，每日定时任务自动标记为「已停用」（默认 3 年）

生产用 `application-prod.yml`，敏感信息走环境变量。OAuth2 真 SSO 配 `app.sso.oauth2.*`，示例在 yml 注释里。

接口细节可通过本地 Swagger 文档查看。

---

## 说明

项目按阶段迭代开发，功能清单比较长，这里就不逐条列 Phase 了。缺什么表结构先看 `sql/` 目录，后端启动时 `SchemaAutoFix` 也会尽量自动补。

有问题先看 `/api/public/health` 和后台日志里的 `requestId`。

## 公开版本说明

本项目仍保留开发演示账号、演示 SSO 与初始化逻辑，不能直接作为生产部署。请在隔离的本地演示数据库运行，不要连接真实业务库。生产使用前需单独审查初始化行为、凭据配置、认证与权限策略。

`demo/expert_import_10.xlsx` 不随源码发布，可由仓库内的合成数据生成器重新生成。前端与后端测试结果见仓库 Actions。
