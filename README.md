# DeckFlow AI

一个基于 Spring AI、DeepSeek V4 Flash、Vue 3 和 Redis 的多轮对话式 PPT 生成器。

## 功能

- 新建、检索并恢复历史会话
- 选择 3–30 页 PPT，并通过多轮对话补充受众、风格、内容与演讲目标
- 每轮 AI 回复后询问是否开始生成
- 生成真正可编辑、可下载的 `.pptx`
- 对已生成 PPT 继续提出修改要求，确认后生成新版本
- Redis 持久化完整对话、PPT 结构与版本信息

## 环境要求

- Java 21+
- Maven 3.9+
- Node.js 20+（或 pnpm）
- Redis 7+
- 环境变量 `DSAPIKEY`

## 启动

1. 启动 Redis：

   ```powershell
   docker compose up -d redis
   ```

2. 配置 DeepSeek API Key：

   ```powershell
   $env:DSAPIKEY="你的 DeepSeek API Key"
   ```

3. 启动后端：

   ```powershell
   cd backend
   mvn -gs .mvn/settings.xml -s .mvn/settings.xml spring-boot:run
   ```

4. 另开终端启动前端：

   ```powershell
   cd frontend
   npm install
   npm run dev
   ```

打开 `http://localhost:5173`。后端地址为 `http://localhost:8080`。

生产构建时先执行 `npm run build`，再将 `frontend/dist` 中的文件部署到静态服务器；也可通过反向代理把 `/api` 转发到后端。

## 配置

| 环境变量 | 默认值 | 说明 |
| --- | --- | --- |
| `DSAPIKEY` | 无 | DeepSeek API Key，必填 |
| `DEEPSEEK_BASE_URL` | `https://api.deepseek.com` | OpenAI 兼容接口地址 |
| `REDIS_HOST` | `localhost` | Redis 地址 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `PPT_STORAGE_PATH` | `../data/presentations` | PPT 文件保存目录 |
