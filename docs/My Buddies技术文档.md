# My Buddies 技术文档

> 文档用途：项目开发记录、技术复盘、面试准备。
> 更新原则：每完成一个功能，同步更新“实现状态、数据模型、请求链路、关键设计、测试和面试题”。
> 当前版本：2026-09-02，数据库 Flyway V4。

## 1. 项目介绍

My Buddies 是一个面向真实线下活动的找搭子平台。项目不把重点放在陌生人社交，而是围绕以下可信闭环设计：

```text
手机号认证 → 实名认证 → 活动约定 → 定位签到 → 履约记录 → 评价与信用
```

目前已实现的是底层账户与内容目录能力，Meetup、帖子、评论、签到等核心业务尚未开发。

面试时可以这样介绍：

> 这是一个基于 Spring Boot 的模块化单体项目。我先完成了手机号验证码登录、JWT 会话、实名认证抽象、后台 RBAC，以及一级分类和话题目录。设计重点是状态边界、并发安全、敏感数据保护和可替换的外部服务，而不是简单堆叠 CRUD。

## 2. 技术栈

| 技术 | 版本/用途 |
|---|---|
| Java | 21 |
| Spring Boot | 4.1.1 |
| Maven | 构建和依赖管理 |
| MySQL | 业务持久化 |
| Redis | 验证码、刷新令牌、会话撤销 |
| MyBatis-Plus | Mapper、条件构造器、乐观锁 |
| Spring Security | 认证过滤链和方法级权限 |
| JWT / JJWT | Access Token、Refresh Token |
| Flyway | 数据库版本管理 |
| Jakarta Validation | HTTP 参数校验 |
| Lombok | 减少实体样板代码 |
| JUnit / MockMvc / H2 | 自动化测试 |

## 3. 架构设计

项目采用模块化单体，而不是微服务：

```text
com.austin
├── common              统一响应、异常
├── config              Jackson、时间、MyBatis-Plus
├── security            Spring Security、JWT 过滤器
└── module
    ├── account         账户生命周期
    ├── auth            短信登录和 Token 会话
    ├── identity        实名认证
    ├── admin           后台角色管理
    └── catalog         Category / Topic
```

每个业务模块内部按职责划分：

```text
controller → service → mapper → database
     ↓
 request / response
```

- Controller 只处理 HTTP、参数校验和响应转换。
- Service 负责事务、状态机和业务规则。
- Mapper 负责数据库访问。
- Domain 表达持久化实体和业务枚举。
- Request/Response 使用 record，避免 API 模型与数据库实体混用。

为什么采用模块化单体：

- 当前团队和业务规模不需要微服务复杂度。
- 本地事务能保证状态变更的一致性。
- 模块边界清楚，未来确有需要时可以拆分。
- 更适合个人项目快速验证核心业务闭环。

## 4. 通用基础能力

### 4.1 统一响应

接口统一返回 `ApiResponse<T>`：

```json
{
  "success": true,
  "data": {},
  "error": null
}
```

失败时：

```json
{
  "success": false,
  "error": {
    "code": "RESOURCE_CONFLICT",
    "message": "资源状态已发生变化"
  }
}
```

### 4.2 统一异常

`GlobalExceptionHandler` 将异常转换为明确的 HTTP 状态：

| 异常 | HTTP 状态 |
|---|---|
| 参数校验失败 | 400 |
| JSON 无法解析 | 400 |
| 未认证 | 401 |
| 无权限 | 403 |
| 资源不存在 | 404 |
| 状态冲突 | 409 |
| 请求过于频繁 | 429 |
| 非 JSON 请求体 | 415 |

这里解决过一个真实问题：缺少 `Content-Type: application/json` 时，异常曾被错误分发成 401。现在会返回准确的 415。

### 4.3 时间

- 统一时区：`Asia/Shanghai`。
- JSON 时间格式：`yyyy-MM-dd HH:mm:ss`。
- 业务代码注入 `Clock`，而不是到处直接调用系统时间，便于测试时间边界。

### 4.4 数据库迁移

Flyway 迁移历史：

| 版本 | 内容 |
|---|---|
| V1 | `user_account` |
| V2 | `identity_verification` |
| V3 | 后台 Role、账户角色、角色审计 |
| V4 | Category、Topic、目录审计和默认分类 |

原则：已经执行的迁移文件不能直接修改；数据库变更必须新增更高版本迁移。

## 5. 账户模块

### 5.1 UserAccount

核心字段：

```text
id
phone
accountStatus
cancelRequestedAt
cancelledAt
version
createdAt
updatedAt
```

账户状态：

```text
ACTIVE → CANCEL_PENDING → CANCELLED
   └────────────────────── BANNED
```

- `ACTIVE`：正常账户。
- `CANCEL_PENDING`：注销冷静期，可以撤销注销。
- `CANCELLED`：注销完成，手机号释放并置空。
- `BANNED`：封禁状态，不能继续认证使用。

### 5.2 为什么需要 version

`version` 是乐观锁版本号，用于防止两个请求同时修改同一条数据时互相覆盖。

例如申请注销时，更新条件同时要求：

```text
id = 当前账户
account_status = ACTIVE
```

并执行：

```sql
version = version + 1
```

更新行数必须为 1：

- `1`：唯一目标账户更新成功。
- `0`：记录不存在或状态已经被其他请求修改。
- 大于 `1`：主键条件下理论上不可能，出现说明更新条件或数据约束严重异常。

### 5.3 注销设计

注销不是直接删除账户：

```text
申请注销
→ CANCEL_PENDING
→ 7 天冷静期
→ 正式注销
→ 清除手机号绑定
```

这样能支持撤销、风控记录保留和手机号重新注册。

## 6. 手机验证码与 JWT 认证

### 6.1 登录流程

```text
POST /auth/sms-codes
→ 生成 6 位随机验证码
→ Redis 保存 5 分钟
→ SmsSender 发送

POST /auth/token
→ Redis 原子校验验证码
→ 查询或创建账户
→ 签发 accessToken + refreshToken
```

开发环境的 `LocalSmsSender` 只把验证码输出到控制台；生产环境必须实现真实短信服务。

### 6.2 验证码为什么放 Redis

- 验证码是短期临时数据，不属于长期业务事实。
- Redis 原生支持 TTL。
- 可以通过 Lua 脚本原子完成次数统计、验证和删除。
- 避免在 MySQL 保存验证码敏感信息。

当前规则：

```text
有效期：5 分钟
重发冷却：60 秒
最大连续失败：5 次
失败锁定：5 分钟
成功后：立即删除，只能使用一次
```

Redis Key 不直接包含手机号，而是包含手机号的 SHA-256 摘要，降低运维侧泄露风险。

### 6.3 双 Token

| Token | 有效期 | 用途 |
|---|---:|---|
| Access Token | 30 分钟 | 访问业务接口 |
| Refresh Token | 7 天 | 换取一组新 Token |

Refresh Token 的 `jti` 保存在 Redis。刷新时通过 Lua 原子消费，旧 Refresh Token 不能重复使用。

Access 和 Refresh 共享 `session_id`。退出登录时：

- 删除 Refresh Token。
- 拉黑当前 Access Token。
- 撤销整个 session，确保刷新前签发的 Access Token 也失效。

### 6.4 JWT 请求链路

```text
Authorization: Bearer <accessToken>
→ JwtAuthenticationFilter
→ 验证签名、issuer、过期时间、token_type
→ 检查 access 黑名单和 session 撤销状态
→ 检查账户是否可用
→ 从数据库加载后台 Role
→ 写入 SecurityContext
→ Controller / @PreAuthorize
```

JWT 不保存后台角色。角色每次从数据库加载，因此撤销角色后无需等待 Token 过期。

## 7. 实名认证模块

### 7.1 状态

```text
UNVERIFIED → VERIFYING → VERIFIED
                       └→ FAILED → 可重新提交
```

`UNVERIFIED` 是没有认证记录时的逻辑状态，不需要为了每个新用户额外插入空记录。

### 7.2 隐私设计

请求中的姓名和身份证号只在当前请求内存中传递给认证提供方。数据库不保存：

- 真实姓名；
- 身份证号码明文；
- 身份证照片；
- 人脸照片；
- 照片地址。

数据库保存必要结果：

```text
status
subjectFingerprint
birthDate
gender
provider
providerReference
failureCode
submittedAt / verifiedAt
```

`subjectFingerprint` 是使用服务端密钥对身份证号码计算的 HMAC-SHA256。它不可逆，但相同证件会得到相同结果，因此可通过唯一索引阻止同一实名主体绑定多个账户。

普通 SHA-256 不够安全，因为身份证号码空间有限，攻击者可以预计算；HMAC 还需要服务端秘密密钥。

### 7.3 为什么不保存 age 和 adult

年龄会变化，因此只保存稳定事实 `birthDate`：

```java
Period.between(birthDate, LocalDate.now(clock)).getYears() >= 18
```

`adult` 是响应时动态计算的值，不是数据库字段。这样不会在用户生日后产生过期数据。

### 7.4 当前模拟认证的限制

`LocalIdentityVerificationProvider` 只能检查：

- 姓名格式；
- 身份证18位格式和校验位；
- 出生日期和性别解析。

它不能证明“姓名确实属于该身份证号码”。正式环境必须实现真实的 `IdentityVerificationProvider`，并根据需要接入二要素认证、活体检测和人脸比对。

## 8. 后台 Role 权限管理

### 8.1 角色

```text
CONTENT_ADMIN
RISK_REVIEWER
SECURITY_REVIEWER
SUPER_ADMIN
```

Role 只表示后台管理维度，不用于替代账户状态、实名状态、风险等级或业务限制。

数据关系：

```text
user_account
    │
    └── account_admin_role ── admin_role
                 │
                 └── granted_by / granted_at
```

角色授予与撤销写入 `admin_role_audit_log`。系统禁止撤销最后一个超级管理员，避免后台彻底失去管理入口。

只有 `SUPER_ADMIN` 可以查询、授予和撤销其他账户的后台角色。超级管理员的有效权限会展开为全部四种角色。

### 8.2 Spring Security

数据库角色转换成：

```text
CONTENT_ADMIN → ROLE_CONTENT_ADMIN
```

接口使用方法级权限：

```java
@PreAuthorize("hasRole('CONTENT_ADMIN')")
```

普通用户即使知道管理接口地址，也会得到 403。

## 9. Category 与 Topic

### 9.1 模型

第一版只有一级 Category，因此 Category 表故意不设计 `parent_id`：

```text
Category 1 ── N Topic
```

Category 和 Topic 都包含：

```text
code
name
description
sortOrder
enabled
version
createdAt
updatedAt
```

- `code` 是稳定的程序标识，创建后不可修改。
- `name` 是用户看到的名称，可以修改。
- `sortOrder` 控制展示顺序。
- `enabled` 实现软停用，不做物理删除。
- `version` 防止并发编辑覆盖。

约束：

- Category code、name 唯一。
- Topic code 全局唯一。
- 同一 Category 下 Topic name 唯一。
- Topic 必须属于存在的 Category。

### 9.2 为什么停用而不是删除

未来 Post、Circle、Meetup 都可能引用 Topic。物理删除会破坏历史业务记录，因此后台只提供启停：

- 停用 Category 后，它和其下 Topic 都不会公开展示。
- Topic 自身可以单独停用。
- Category 停用时禁止启用其 Topic。

### 9.3 权限和接口

游客可以读取启用的目录：

```text
GET /api/v1/categories
GET /api/v1/categories/{categoryId}/topics
GET /api/v1/topics/{topicId}
```

`CONTENT_ADMIN` 可以创建、编辑、启停分类和话题；管理接口能够看到停用数据。所有维护动作写入 `catalog_admin_audit_log`。

V4 预置10个一级分类，Topic 暂不预置。

## 10. 测试策略

测试分层：

- 领域/工具单元测试：身份证校验等纯逻辑。
- Service 测试：事务、状态机和冲突处理。
- MockMvc 测试：认证、授权、参数校验、JSON 和 HTTP 状态。
- H2 测试：每次从 V1 执行到最新 Flyway 版本。
- MySQL 启动验证：确认真实数据库语法、外键和检查约束。

当前结果：

```text
Tests run: 32
Failures: 0
Errors: 0
Flyway: V4
```

重要测试场景包括：

- 匿名访问受保护接口返回 401。
- 登录用户访问其他账户返回 403。
- 验证码错误次数和一次性消费。
- Refresh Token 轮换和重放失败。
- 同一实名主体不能绑定两个账户。
- 最后一个超级管理员不能被撤销。
- 普通用户不能调用内容管理接口。
- 停用分类对游客隐藏、对管理员可见。
- 缺少 JSON Content-Type 返回 415，而不是误报 401。

## 11. 本地运行

依赖：

```text
Java 21
MySQL 8
Redis
数据库：my_buddies
```

启动：

```powershell
.\mvnw.cmd spring-boot:run
```

测试与打包：

```powershell
.\mvnw.cmd package
```

生产环境必须通过环境变量覆盖数据库密码、JWT 密钥和实名指纹密钥。技术文档、Git 和日志中不能保存真实密钥。

## 12. 当前未实现

- 用户公开资料和官方头像；
- Circle、Post、Comment；
- Meetup 和参与者状态机；
- 定位签到；
- 履约、评价和信用；
- 风险限制、举报和处罚；
- 内容审核任务；
- 真实短信和真实实名认证厂商；
- 管理后台前端。

## 13. 面试高频问题

### 为什么 JWT 还要 Redis？

JWT 本身无状态，但退出登录、Refresh Token 一次性轮换和会话主动撤销都需要服务端状态。Redis 保存短期会话状态，同时保留 Access Token 验证的高性能。

### 为什么角色不直接写进 JWT？

写入 JWT 后，撤销角色要等 Token 过期才能生效。当前系统每次从数据库加载角色，权限变更立即生效。后期可增加短 TTL 缓存并在角色变化时失效。

### 乐观锁和悲观锁怎么选择？

普通账户、Category、Topic 更新冲突概率低，使用 version 乐观锁。撤销最后一个超级管理员属于关键全局约束，修改时锁定角色记录，使并发撤销串行化。

### 为什么实名认证不保存身份证号？

业务只需要确认真实性、唯一主体、出生日期和性别。保存原文会增加泄露影响和合规成本，因此只保存不可逆主体指纹及必要结果。

### 为什么 Category 没有 parentId？

V1 明确只有一级分类。提前加入无限树结构会增加查询、排序和约束复杂度；等真实需求出现后再通过新迁移扩展。

### 为什么不直接删除分类和话题？

它们会被未来的内容和活动引用。删除会破坏历史数据，停用可以阻止新使用，同时保留历史关系。

## 14. 后续每个功能的文档同步模板

每次实现新功能后，在本文档增加或更新：

1. 功能目标与业务边界。
2. 数据表、实体和关键字段。
3. 状态机及非法状态转换。
4. Controller → Service → Mapper 请求链路。
5. Spring Security 权限要求。
6. 并发、事务和幂等设计。
7. 隐私与安全考虑。
8. API 和 Postman 示例。
9. 自动化测试与真实环境验证。
10. 面试可能追问、当前限制和后续改进。

这份文档描述的是当前真实代码，不记录尚未实现的能力为“已完成”。
