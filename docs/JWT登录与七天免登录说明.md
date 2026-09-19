# JWT 登录与七天免登录说明

日期：2026-09-19　　实现：陈志鹏（241616118）

登录方式已从原来的 Session 改成 JWT（JSON Web Token），并实现了七天免登录。
本文说明 JWT 是什么、为什么这么用、代码在哪、怎么验证。

---

## 一、JWT 是什么

JWT 全称 JSON Web Token，可以理解成一张**带防伪签名的电子通行证**：
登录成功后由服务器签发，交给浏览器保存，之后每次访问都带着它，服务器不用记"谁登录过"就能认出来。

一个 JWT 长这样，**三段用点分隔**：

```
eyJhbGciOiJIUzI1NiJ9 . eyJzdWIiOiIxNSIsInVzZXJuYW1lIjoiMjQxNjE2MTE4In0 . qNrdx9R5PqMc0V...
      头部 Header              载荷 Payload                          签名 Signature
```

| 段 | 内容示例 | 作用 |
| --- | --- | --- |
| 头部 | `{"alg":"HS256","typ":"JWT"}` | 说明用哪种算法签名 |
| 载荷 | `{"sub":"15","username":"241616118","iat":...,"exp":...}` | 装身份信息：`sub` 是用户 ID，`exp` 是过期时间 |
| 签名 | `HMACSHA256(头部.载荷, 密钥)` | 用服务器密钥算出的防伪码，**改一个字都会对不上** |

三点必须记住：

1. **载荷是 Base64 编码，不是加密**，谁拿到都能解开看，所以不能放密码等敏感信息。我们的 token 只放用户 ID 和登录名。
2. **签名保证的是"没被改过"**，不是"内容保密"。别人改了载荷但算不出新签名，服务器一验就发现。
3. **密钥只在服务器**，泄露了别人就能伪造通行证。

---

## 二、JWT 的作用

1. **服务器不用保存会话**：原来是 Session 方案，服务器要记住每个登录的人；JWT 方案下服务器只验签名，天然无状态。
2. **校验成本低**：一次验签就能确认身份，不用查会话表。
3. **自带过期时间**：`exp` 到点自动失效，不需要服务器清理。
4. **方便前后端分离和分布式**：多个服务用同一个密钥就能互认，这也是它比 Session 更适合现代架构的原因。

---

## 三、和 Session 的区别（我们改了什么）

| 对比项 | 原来的 Session | 现在的 JWT |
| --- | --- | --- |
| 登录状态存哪 | 服务器内存 | 浏览器 Cookie（服务器只给签名） |
| 服务器要不要记 | 要，每个用户一份 | 不用 |
| 关浏览器后 | 会话没了，要重新登录 | Cookie 还在，七天免登录 |
| 怎么失效 | 删除会话即立刻失效 | 只能等过期；要立刻作废需要额外做黑名单 |
| 多服务共享 | 要共享会话（Redis 等） | 同一密钥即可互认 |

---

## 四、本项目怎么用

| 环节 | 做法 | 代码位置 |
| --- | --- | --- |
| 登录 | 校验 BCrypt 密码 → 签发 token → 放进 HttpOnly Cookie 返回 | `AuthController.login` |
| 签发/校验 | 生成三段式令牌、验签、取载荷 | `config/JwtUtil.java` |
| 访问拦截 | 先看会话，会话里没有就验 Cookie 里的 token，验过就恢复登录 | `config/LoginInterceptor.java` |
| 退出 | 销毁会话 + 把 Cookie 置为过期 | `AuthController.logout` |
| 配置 | 密钥与有效期（天） | `application.yml` 里的 `app.jwt` |

关键代码片段（签发）：

```java
Jwts.builder()
    .subject(String.valueOf(userId))      // 谁
    .claim("username", username)
    .issuedAt(now)                        // 什么时候签的
    .expiration(new Date(now.getTime() + expire.toMillis()))  // 什么时候过期
    .signWith(key)                        // 用密钥签名
    .compact();
```

登录成功后的 Cookie：

```java
Cookie cookie = new Cookie("DS_TOKEN", token);
cookie.setHttpOnly(true);      // 前端 JS 读不到，防止被脚本偷走
cookie.setPath("/");
cookie.setMaxAge(7 * 24 * 60 * 60);      // 浏览器保存 7 天
cookie.setAttribute("SameSite", "Lax");  // 跨站请求不带，缓解 CSRF
```

---

## 五、七天免登录的原理

**光有 token 有效期还不够**，要三个条件同时满足：

1. **令牌本身有效期 7 天**：`app.jwt.expire-days: 7`，过期后验签失败。
2. **Cookie 的 Max-Age 也是 7 天**：否则浏览器一关就把 Cookie 删了，下次没东西可带。
3. **拦截器会自动恢复登录**：重开浏览器后服务器会话已丢失，但 Cookie 里的令牌还在，
   拦截器验签通过 → 查出账号和角色 → 自动恢复登录状态，用户感觉"没登录就进来了"。

第 3 点是关键：如果拦截器只认会话、不去读 Cookie，那么每次关浏览器都要重新登录。

> 另外，拦截器恢复登录时**会重新查一次数据库里的角色**，所以管理员改了某人的角色后，对方下次访问就能生效，不会因为令牌是七天前签的就一直用旧权限。

---

## 六、安全设计要点

| 风险 | 我们的做法 |
| --- | --- |
| 脚本偷令牌（XSS） | token 放 **HttpOnly** Cookie，前端 JavaScript 读不到 |
| 令牌被篡改 | 签名校验，改一个字符就失败（已实测） |
| 令牌被伪造 | 密钥只在服务器；换密钥验不过（已有测试） |
| 跨站请求伪造（CSRF） | Cookie 设 `SameSite=Lax`，跨站 POST 不带 Cookie |
| 令牌泄露后被长期使用 | 有效期 7 天自动失效；退出登录清理浏览器 Cookie |
| 密钥泄露 | 配置支持用环境变量 `JWT_SECRET` 覆盖；不要把密钥提交到仓库 |

**要如实说明的局限**：JWT 是无状态的，服务器不保存已签发的令牌，
所以"退出登录"只清掉了浏览器这一份，如果令牌在退出前已经被复制走，它在有效期内仍然可用。
要做到"退出即刻失效"，需要额外维护黑名单（把退出的令牌记下来并在校验时拒绝）或者改用短有效期 + 刷新令牌。
本项目是课程作业，采用"七天有效期 + 退出清 Cookie"的简化方案。

---

## 七、怎么验证

### 1. 单元测试（4 个）

```powershell
mvn test -Dtest=JwtUtilTest
```

| 用例 | 验证什么 |
| --- | --- |
| 签发与解析 | 令牌是三段结构，能取回用户 ID 和登录名，过期时间在一周后 |
| 过期令牌 | 有效期设为负数后解析抛异常，说明到点自动失效 |
| 篡改载荷 | 改掉载荷后签名对不上，解析失败 |
| 换密钥 | 用另一把密钥验不过，说明无法伪造 |

### 2. 实际抓包与免登录验证（已实测）

| 检查项 | 实测结果 |
| --- | --- |
| 登录响应 | 返回 `Set-Cookie: DS_TOKEN=...` |
| Cookie 属性 | `HttpOnly`、`Max-Age=604800`（正好 7 天）、`SameSite=Lax` |
| 重新打开浏览器 | **新建会话、只带这一个 Cookie 访问首页，返回 200 且显示已登录** |
| 不带 Cookie | 302 跳回登录页 |
| 篡改令牌 | 302 被拒绝，当作未登录 |

---

## 八、文件索引

| 文件 | 说明 |
| --- | --- |
| `config/JwtUtil.java` | JWT 签发与校验工具 |
| `config/LoginInterceptor.java` | 登录拦截器，负责用 Cookie 恢复登录 |
| `config/WebConfig.java` | 注册拦截器、放行登录注册和静态资源 |
| `controller/AuthController.java` | 登录签发令牌、退出清除令牌 |
| `application.yml` | `app.jwt.secret` 密钥、`app.jwt.expire-days` 有效期 |
| `src/test/java/.../JwtUtilTest.java` | JWT 单元测试 |

> 按协作约定，认证权限属于作业 03 的范围。本实现已经接通并验证，刘梓烨做作业 03 时可以直接沿用，
> 或按需要把它搬到 Service 层并补充 CSRF 令牌、Redis 会话缓存等增强项。
