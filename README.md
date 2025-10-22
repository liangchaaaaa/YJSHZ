# 雅鉴生活志 (YJSHZ) - 本地生活服务平台

## 项目简介

雅鉴生活志是一个集本地生活服务、社交分享和AI智能咨询于一体的综合性平台。项目采用前后端分离架构，结合现代Web技术和人工智能能力，为用户提供优质的本地生活体验。

## 🚀 技术架构

### 前端架构
- **技术栈**: Vue.js + Element UI + Axios
- **服务器**: Nginx (端口8083)
- **部署方式**: 静态资源部署

### 后端架构
- **核心模块** (core): Spring Boot 2.7.18 + JDK 8
- **AI咨询模块** (consultant): Spring Boot 2.7.18 + JDK 17
- **数据库**: MySQL + Redis
- **AI服务**: 阿里云通义千问大模型

### 服务端口配置
| 服务 | 端口 | 说明 |
|------|------|------|
| 前端Nginx | 8083 | 静态资源服务和API代理 |
| 核心服务 | 8081 | 业务逻辑处理 |
| AI咨询服务 | 8084 | AI对话功能 |

## 📁 项目结构

```
YJSHZ/
├── backend/                    # 后端服务
│   ├── core/                  # 核心业务模块 (JDK 8)
│   │   ├── src/main/java/com/yjshz/
│   │   │   ├── controller/    # 控制器层
│   │   │   ├── service/       # 业务逻辑层
│   │   │   ├── entity/        # 实体类
│   │   │   └── utils/         # 工具类
│   │   └── src/main/resources/
│   ├── consultant/           # AI咨询模块 (JDK 17)
│   │   ├── src/main/java/com/yjshz/consultant/
│   │   │   ├── controller/    # AI控制器
│   │   │   └── aiservice/     # AI服务实现
│   │   └── src/main/resources/static/
│   │       └── index.html     # AI咨询前端页面
│   └── pom.xml               # Maven父POM
└── frontend/                  # 前端资源
    ├── html/yjshz/           # 前端页面
    │   ├── index.html        # 首页
    │   ├── shop-list.html    # 商铺列表
    │   ├── shop-detail.html  # 商铺详情
    │   ├── blog-detail.html  # 博客详情
    │   └── login.html        # 登录页面
    ├── conf/nginx.conf       # Nginx配置
    └── nginx.exe             # Nginx服务器
```

## 🛠️ 快速开始

### 环境要求
- JDK 8 和 JDK 17 (consultant模块需要JDK 17)
- MySQL 5.7+
- Redis 6.0+
- Maven 3.6+

### 数据库初始化
1. 创建数据库 `redis_project`
2. 执行SQL脚本: `backend/core/src/main/resources/db/yjshz.sql`
3. 配置数据库连接信息

### 启动步骤

#### 1. 启动Redis服务
```bash
cd backend
./start_redis.bat  # Windows系统
# 或使用Docker: docker-compose up -d redis
```

#### 2. 启动后端服务
```bash
# 启动核心服务 (端口8081)
cd backend/core
mvn spring-boot:run

# 启动AI咨询服务 (端口8084，需要新终端)
cd backend/consultant
mvn spring-boot:run -Dmaven.compiler.source=17 -Dmaven.compiler.target=17
```

#### 3. 启动前端服务
```bash
cd frontend
./nginx.exe  # Windows系统
# 或使用命令: nginx -c conf/nginx.conf
```

### 4. 访问应用
- **主应用**: http://localhost:8083
- **AI咨询页面**: http://localhost:8084
- **API文档**: 可通过Swagger访问相关接口

## 🔌 API接口说明

### 核心业务接口
| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 用户登录 | POST | `/user/login` | 用户登录 |
| 商铺列表 | GET | `/shop/**` | 商铺相关接口 |
| 博客管理 | GET/POST | `/blog/**` | 博客内容管理 |
| 优惠券 | GET/POST | `/voucher/**` | 优惠券功能 |

### AI咨询接口
| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| AI对话 | GET | `/ai/chat` | 与AI助手对话 |
| 服务状态 | GET | `/ai/status` | 检查AI服务状态 |

### 数据流示例
```
用户请求 → Nginx(8083) → 核心服务(8081) → AI服务(8084) → 阿里云API
```

## 🤖 AI智能咨询功能

### 功能特点
- **流式对话**: 支持实时流式响应
- **记忆管理**: 基于memoryId的对话上下文管理
- **多用户支持**: 支持多用户并发对话
- **优雅UI**: 现代化的聊天界面设计

### 技术实现
- **LangChain4J**: AI服务框架集成
- **WebFlux**: 响应式流式处理
- **Redis**: 对话记忆存储
- **通义千问**: 大语言模型支持

## ⚙️ 配置说明

### 核心服务配置 (application.yaml)
```yaml
server:
  port: 8081
spring:
  datasource:
    url: jdbc:mysql://127.0.0.1:3306/redis_project
    username: root
    password: 15915255868
  redis:
    host: 127.0.0.1
    port: 6379
ai:
  consultant:
    url: http://localhost:8084
```

### AI服务配置 (application.yml)
```yaml
server:
  port: 8084
langchain4j:
  open-ai:
    chat-model:
      base-url: https://dashscope.aliyuncs.com/compatible-mode/v1
      api-key: your-api-key
      model-name: qwen-plus
```

### Nginx配置 (nginx.conf)
```nginx
server {
    listen 8083;
    location / {
        root html/yjshz;
        index index.html;
    }
    location /api {
        proxy_pass http://127.0.0.1:8081;
    }
}
```

## 🐛 常见问题解决

### 1. 端口冲突
- 检查8081、8083、8084端口是否被占用
- 修改对应配置文件中的端口号

### 2. 数据库连接失败
- 确认MySQL服务已启动
- 检查数据库连接配置
- 验证数据库用户权限

### 3. AI服务调用失败
- 检查consultant服务是否正常启动
- 验证API密钥配置
- 查看服务日志排查问题

### 4. 前端资源加载失败
- 确认Nginx配置正确
- 检查静态资源路径
- 查看浏览器控制台错误信息

## 📊 功能模块

### 用户模块
- 用户注册登录
- 个人信息管理
- 权限控制

### 商铺模块
- 商铺分类浏览
- 商铺详情查看
- 地理位置服务

### 内容模块
- 博客发布浏览
- 内容点赞评论
- 热门内容推荐

### AI咨询模块
- 智能对话咨询
- 个性化推荐
- 多轮对话支持

## 🔒 安全特性

- JWT Token认证
- Redis会话管理
- 接口权限控制
- SQL注入防护
- XSS攻击防护

## 📈 性能优化

- Redis缓存优化
- 数据库连接池
- 静态资源CDN
- 接口响应式处理
- AI服务流式响应

## 🤝 开发贡献

### 代码规范
- 遵循阿里巴巴Java开发规范
- 使用Lombok减少样板代码
- 统一的异常处理机制

### 提交规范
- feat: 新功能
- fix: 修复bug
- docs: 文档更新
- style: 代码格式调整
- refactor: 代码重构

## 📄 许可证

本项目基于MIT许可证开源。

## 📞 技术支持

如有问题请联系开发团队或提交Issue。

---

**雅鉴生活志** - 让本地生活更智能、更便捷！