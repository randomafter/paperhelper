# 历史特色创作平台

汉/唐/宋/明 历史特色素材库、AI 风格化创作、可视化展示与架空创作工作台。

## 技术栈

- **后端**: Spring Boot 3 + MyBatis-Plus + Spring Security + JWT + MySQL + Redis + Apache POI
- **前端**: Vue 3 + Vue Router + Pinia + Vue-Quill-Editor + D3.js + ECharts
- **爬虫/清洗**: Python (Requests + BeautifulSoup)

## 快速开始

### 1. 数据库

创建库并执行建表脚本：

```bash
mysql -u root -p -e "CREATE DATABASE history_creation DEFAULT CHARACTER SET utf8mb4;"
mysql -u root -p history_creation < docs/schema.sql
```

修改 `backend/src/main/resources/application.yml` 中的数据库与 Redis 连接信息。

### 2. 后端

```bash
cd backend
mvn spring-boot:run
```

默认端口 8080，接口前缀 `/api`。若未安装/启动 Redis，检索仍可用，仅缓存不生效。

### 3. 前端

```bash
cd frontend
npm install
npm run dev
```

访问 http://localhost:5173 ，注册/登录后使用。

### 4. 素材数据（可选）

生成 1000+ 条示例素材并导入 MySQL：

```bash
cd scripts
pip install -r requirements.txt
python crawler/crawl_sample.py
python clean_import/clean.py
# 修改 clean_import/import_mysql.py 中 DB_CONFIG 后：
python clean_import/import_mysql.py
```

## 功能概览

| 模块 | 功能 |
|------|------|
| 用户 | 注册/登录(JWT)、角色(管理员/创作者)、个人中心 |
| 素材库 | 多维度检索(关键词/朝代/分类/标签)、CRUD与批量(管理员)、素材详情 |
| AI 创作 | 讯飞星火对接、历史风格提示词模板、素材关联生成、Word/TXT 导出 |
| 工作台 | 在线创作、每 5 分钟自动保存、最近 5 版回溯、素材检索面板、风格切换 |
| 可视化 | 朝代-分类-素材 D3 图谱、ECharts 分布图、节点跳转素材详情 |

**表附件：**

### Phase 1 – 基础与用户

- MySQL 建表: `docs/schema.sql`（`role`、`user`、`verification_code`）
- Spring Boot: 项目骨架、MyBatis-Plus、Spring Security、JWT 认证
- 注册/登录/个人中心: BCrypt 密码、JWT 签发、`/auth/register`、`/auth/login`、`/users/me`
- Vue: 登录/注册页、主布局、路由守卫、Pinia 登录态、axios 请求封装

### Phase 2 – 素材库

- 表: `material`、`tag`、`material_tag`（`docs/schema.sql`）
- CRUD 与批量: MyBatis-Plus，REST `/materials`，管理员专用批量删除/更新分类/标签
- 多维度检索: `MaterialMapper.searchPage`（keyword/dynasty/category/tagIds），Redis 缓存（有 try-catch，Redis 不可用时仅跳过缓存）
- Python: `scripts/crawler/crawl_sample.py` 生成示例数据，`scripts/clean_import/clean.py` 清洗，`scripts/clean_import/import_mysql.py` 导入 MySQL
- 前端: 素材列表、筛选、分页、详情页、管理员新增/编辑/删除/批量删除

### Phase 3 – AI 模块

- 讯飞星火: `SparkClient` HTTP 封装（未配置时返回占位文案）
- 提示词模板: 表 `prompt_template`，CRUD，变量 `{material_context}` / `{user_input}` 替换
- 生成: `AiGenerateService` 拼装 prompt 后调用 Spark
- 导出: `ExportService` POI 生成 Word、TXT；接口 `/ai/export/word`、`/ai/export/txt`
- 前端: 工作台「AI 风格生成」：模板选择、素材上下文、用户输入、生成、Quill 编辑、导出 Word/TXT

### Phase 4 – 工作台

- 表: `creation_work`、`creation_version`（`docs/schema.sql`）
- 作品与版本: 创建/更新作品、保存版本、仅保留最近 5 个版本
- 接口: `/works` 列表/创建/更新/删除，`/works/{id}/version` 保存版本，`/works/{id}/versions` 版本列表
- 前端: 「在线创作」：作品列表、新建、Quill 编辑、约 5 分钟自动保存、版本列表与恢复、素材检索面板（关键词检索 +「加入素材上下文」）

### Phase 5 – 可视化

- 接口: `/visual/stats` 返回 byDynasty、byCategory、graphNodes、graphLinks
- 前端: Visual.vue — ECharts 柱状图（朝代）、饼图（分类），D3 力导向图（朝代→分类→素材），素材节点点击跳转 `/materials/:id`





## 配置说明

- **讯飞星火**: 在 `application.yml` 中配置 `spark.app-id`、`spark.api-key`、`spark.api-secret` 后可使用真实 AI 生成；未配置时返回占位文案。
- **短信/邮件**: 验证码表已预留，接口可 Mock 或对接第三方。

## 项目结构

```
backend/          # Spring Boot
frontend/         # Vue 3 SPA
scripts/          # Python 爬虫与清洗、MySQL 导入
docs/             # schema.sql
data/             # 可选 SQLite 等
```
