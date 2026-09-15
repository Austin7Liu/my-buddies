# My Buddies 前端

当前包含 Vue 3 基础工程、手机号验证码登录、Token 保存与自动刷新、路由保护、退出登录，Category、Topic、Circle、我的兴趣和 Elasticsearch 搜索页面，以及内容管理员后台。公开内容允许游客浏览，关注、加入和创建操作会要求登录。

拥有 `CONTENT_ADMIN` 或 `SUPER_ADMIN` 有效角色的账户登录后，用户端导航会显示“管理后台”。后台地址为 `/admin`，提供 Circle/Post 审核、Meetup 终止和 ES 全量重建功能。

## 本地运行

先启动 MySQL、Redis 和 Spring Boot 后端，确认后端监听 `http://localhost:8080`。

```powershell
cd frontend
npm install
npm run dev
```

浏览器访问 `http://localhost:5173`。开发环境点击“获取验证码”后，在 Spring Boot 控制台查找 `[DEV ONLY] verification code`。Vite 会把浏览器发往 `/api` 的请求代理到后端，因此不需要额外配置 CORS。

## 构建

```powershell
npm run build
```

`src/api/http.js` 统一添加 Access Token，并在接口返回 401 时使用 Refresh Token 刷新一次后重试原请求。Token 当前保存在 localStorage，适合学习和本地演示；生产环境建议改用由后端设置的 HttpOnly、Secure Cookie 来降低 XSS 窃取风险。

后端雪花 ID 超过 JavaScript 安全整数范围，因此 Axios 使用 `json-bigint` 将 JSON 中的大整数 ID 保留为字符串。前端不得将业务 ID 转换成 JavaScript `Number`。

搜索页面依赖 Elasticsearch 的 `my-buddies-search` Alias。第一次使用前，需要由内容管理员调用后端全量重建接口初始化索引。
