# FakeHachimi v2

> **这不是官方 Hachimi 客户端。**
> 本仓库是一个被判定为**仿冒（fake / clone）**的「Hachimi 2.0」jar 的反编译复原源码，仅用于技术分析与存档。

## 结论先行

这个 jar 自称 `Hachimi 2.0`，作者署名 `haodzzzzzzz`，官网指向 `hachimiclient.pw`。但它**不是**官方 Hachimi 的构建产物，而是一个 UI 与功能都拙劣模仿的第三方仿冒版本。

社区反馈（B 站视频评论区，@yumu-w9I，1 个月前）：

> The real 2.0 UI doesn't look like this, this is obviously a clumsy copy of Coastline, and it doesn't have so few features.
>
> （真正的 2.0 界面不是这样的，这明显是对 Coastline 的拙劣抄写，而且功能也没有这么少。）

## 判定依据

### 1. 构建信息是伪造的 / 缺失的

`src/main/java/net/hachimi/client/BuildConfig.java`：

```java
public static final String HASH          = "null";              // ← git hash 是字面量字符串 "null"
public static final String VERSION        = "2.0";
public static final String BUILD_NUMBER   = "2.0";
public static final String BUILD_IDENTIFIER = "\"release\"";
public static final String BUILD_TIME     = "07/14/2026 02:26";
```

`Hachimi.java` 启动日志：

```java
info("This build of Hachimi is on Git hash {} and was compiled on {}", "null", "07/14/2026 02:26");
```

- **`HASH` 是字符串 `"null"`**，不是 40 位 commit SHA。官方出包必然带真实 git hash；这说明它是手工拼出来的、没有走正常 CI 的构建。
- `BUILD_IDENTIFIER` 的值里带着多余的转义引号（`"\"release\""`），也是手工硬编码的痕迹。
- `BUILD_TIME` 为 **2026-07-14**。而官方 Hachimi 在 2026 年 7 月**尚未发布 v2**，时间线对不上。

### 2. UI 是抄 Coastline 的，不是 Hachimi 的

ClickGUI 那套结构与配色（`impl/gui/click/` 下的 `CategoryFrame` / `ModuleButton` / `SliderButton` / `DropdownButton` / `ColorButton` 布局）和官方 Hachimi 2.0 的实际界面完全不同，而是照着 Coastline 的形态拼的。社区用户一眼就认出来了。

### 3. 功能规模对不上

官方 2.0 的功能列表远多于这个包；这个包虽然也有 170 个模块文件、145 个 Mixin，但缺了官方大量核心模块，属于「形似而神不似」。

### 4. 贴图资源被剥离

原始 jar 里 `assets/hachimiclient/textures/` 是**空目录**——作者自己删掉了贴图，但没删引用它们的代码，导致 UI 一跑就是纯色/无纹理。这也是仿冒包常见的「压包交付」特征（本仓库已把相关引用代码一并清理，见下文）。

## 与官方 Hachimi 的关系

| 项目 | 官方 Hachimi 2.0 | 本仓库（仿冒） |
|---|---|---|
| Git hash | 真实 commit SHA | 字符串 `"null"` |
| 发布日期 | 2026 年 7 月后 | 声称 2026-07-14 |
| UI 风格 | 自有设计 | 抄 Coastline |
| 功能数量 | 完整 | 明显缺失 |
| 贴图资源 | 完整 | 被删除（空目录） |
| 署名 | haodzzzzzzz | 同样冒用 haodzzzzzzz |

**冒用原作者署名**是最恶劣的一点：这个包让使用者以为自己在用 haodzzzzzzz 的作品。

## 源码是怎么来的

1. 原始 jar 是 **intermediary 命名**（629 个类引用 `net/minecraft/class_*`），无法直接阅读。
2. 用 tiny-remapper + Yarn `1.21.1+build.3` 映射（含 MixinExtension）重映射为可读命名。
3. Vineflower 反编译 → 783 个 `.java` 文件。
4. 手工修复 **86 个反编译误差**（泛型擦除、Mixin `(Xxx)(Object)this` 强转、变量重名、record 推断等），从 0 错误编译。
5. `hachimi.accesswidener` 用自写工具从 intermediary 翻成 named 供 Loom 使用；打包时 Loom 自动回转，产物与原 jar 的 AW **逐字节一致**。

## 构建

### 环境要求

- **JDK 21**
- 不需要单独安装 Gradle —— 自带 Wrapper

### 命令

```bash
# Linux / macOS 桌面环境
./gradlew build

# Windows
gradlew.bat build

# Android / Termux（外置存储不支持可执行位，chmod 无效，必须用 bash 调用）
bash ./gradlew build
```

首次构建会从腾讯云镜像自动下载 Gradle 9.7.1 及依赖。产物：`build/libs/Hachimi-2.0.jar`。

> ⚠️ `build/libs/` 里还有个 `Hachimi-2.0-sources.jar`，那是源码包，**不要**放进 `mods/`。

### 依赖版本

| 组件 | 版本 |
|---|---|
| Minecraft | 1.21.1 |
| Fabric Loader | 0.16.14 |
| Fabric API | 0.116.12+1.21.1 |
| Yarn Mappings | 1.21.1+build.3 |
| Fabric Loom | 1.17.20 |
| MixinExtras | 0.4.1（已内嵌） |
| Gradle | 9.7.1 |

> Loom 1.17.20 要求 **Gradle ≥ 9.5**。降级到 Gradle 8.x 需同时把 loom 降到 1.9-SNAPSHOT 或更早。

## 目录结构

```
Hachimi/
├── build.gradle / settings.gradle / gradle.properties
├── gradlew / gradlew.bat / gradle/wrapper/
└── src/main/
    ├── java/                        # 783 个源文件
    │   ├── net/hachimi/client/
    │   │   ├── api/                 # 配置 / 渲染 / 账号 / 命令基础层
    │   │   ├── impl/                # 170 个模块类 + 事件 + 管理器 + ClickGUI
    │   │   ├── mixin/               # 145 个 Mixin
    │   │   └── util/
    │   ├── net/hachimi/eventbus/    # 自研事件总线
    │   └── meteordevelopment/discordipc/
    └── resources/
        ├── assets/hachimiclient/    # 字体 / 着色器 / 音效（贴图已被原作者删除）
        ├── fabric.mod.json
        ├── mixins.hachimi.json
        ├── hachimi.accesswidener
        └── Hachimi-refmap.json
```

## 为能编译所做的改动

原作者删掉了 `assets/hachimiclient/textures/` 下所有贴图但保留了引用代码，直接编译虽能过、运行会缺资源。本仓库已把这些引用**连代码一起删除**：

| 文件 | 删除内容 | 降级方案 |
|---|---|---|
| `MixinLoadingOverlay` | `textures/gui/loading/frame_%03d.png` 48 帧启动动画 | 黑底 + 进度条 |
| `MixinTitleScreen` | `background.png` 兜底背景、`purple.png` 标题 logo | 纯色渐变 |
| `ColorButton` | `textures/sync_clickgui.png` 取色器图标 | 不绘制 |
| `HUDModule` | `textures/white.png` HUD logo | 不绘制 |

产物中已确认**不含任何** `hachimiclient/textures` 字符串。编译与运行不受影响。

## 安全审计（简要）

对全部 783 个源文件做过定向扫描，结论：**未发现后门、木马或系统/账号窃取行为**。网络出口仅限 Microsoft OAuth（`login.live.com` / `*.auth.xboxlive.com` / `api.minecraftservices.com`）、`laby.net`、`api.2b2t.vc`、`s.optifine.net`、`minotar.net` 与本地 Discord IPC。无远程代码加载、无 webhook、无硬编码 IP、无浏览器/Discord 数据读取。

但存在以下实质风险，**建议不要在本机使用**：

1. **账号密码明文存盘** —— `AccountFile` 会把微软账号 `email` + `password` 明文写入 `.minecraft/hachimi/accounts.json`；`EncryptedAccountFile` 的 `save()` / `load()` 是空方法，「加密存储」根本没实现，是个空壳。
2. **OAuth 回调监听 `0.0.0.0:6969`** —— 绑定所有网卡而非 `127.0.0.1`，同局域网可在登录窗口期抢先注入伪造 code（PKCE 拦住了实际危害，但仍是配置缺陷）。
3. **冒用他人署名对外分发** —— 这个包被冠以 haodzzzzzzz 的名义传播，本身即是对原作者权益的侵害。

## 免责声明

本仓库仅供**技术分析与安全研究**。源码源自对第三方 jar 的逆向复原，不代表官方 Hachimi，也不代表原作者立场。

- 请勿将本构建当作官方客户端分发或售卖。
- 请勿用于任何违反 Minecraft EULA 或服务器规则的用途。
- 若你是在寻找真正的 Hachimi，请前往官方渠道获取，不要使用此类仿冒包。

原客户端作者：**haodzzzzzzz**。本仓库的逆向复原与文档整理不代表其授权。
