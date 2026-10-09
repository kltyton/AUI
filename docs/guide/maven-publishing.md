# Maven Central 发布

九个 Loader / Minecraft 目标保持独立 Maven 坐标，版本来自根目录 `gradle.properties`。`scripts/publishing/prepare-central.sh` 使用每个目标自己的 Gradle Wrapper，将完整目标矩阵整理为一个 Central Portal 上传 ZIP；脚本只生成本地文件，不执行上传。

在 Git Bash 中设置 `JAVA_HOME_17`、`JAVA_HOME_21`、`JAVA_HOME_25`，分别指向现有 JDK。目标使用 `ci.properties` 中的 `ci.java` 选择运行 Gradle 的 JVM，编译工具链仍由各目标配置控制。

尚未配置签名密钥时，可先准备全部附件及校验文件：

```bash
bash scripts/publishing/prepare-central.sh unsigned
```

每个目标输出到 `targets/<target>/build/central/repository/`，包含运行 JAR、源码 JAR、Javadoc JAR、POM 和校验文件。未签名模式不生成上传 ZIP。

正式签名使用 Gradle 自带 Signing 插件调用本机 GnuPG。通过个人 Gradle 配置或以下非敏感参数指定已有密钥；私钥和口令不得放进项目或命令行。口令由 GPG Agent / Pinentry 接收。

```bash
bash scripts/publishing/prepare-central.sh signed \
  -Psigning.gnupg.executable=gpg \
  -Psigning.gnupg.homeDir=/path/to/your/gnupg-home \
  -Psigning.gnupg.keyName=YOUR_PUBLIC_KEY_FINGERPRINT
```

公钥须按 [Sonatype 的 GPG 要求](https://central.sonatype.org/publish/requirements/gpg/)公开。签名成功后生成 `build/central/KltytonUI-<version>.zip`，只包含九个组件的当前版本目录，不包括 `maven-metadata.xml`、其他版本或本地代理规则。Forge JAR 在完成重映射后签名；Fabric 使用 Loom 提供的发行附件。

使用 Windows Git 自带 GnuPG 时，通过 `GNUPGHOME` 指定密钥目录并省略 `signing.gnupg.homeDir` 参数。脚本将目录转换为 Git Bash 路径，保留该写法传给 Gradle 子进程，避免 GPG Agent 的 socket 名称包含盘符冒号。`signing.gnupg.executable` 可以指定实际 `gpg.exe` 的完整路径。

按 [Central Portal 上传流程](https://central.sonatype.org/publish/publish-portal-upload/)提交 ZIP。当前发布额度应以组织的 Usage Center 为准；本地生成成功不代表平台已批准额度或组件已公开。

公开发布并验证坐标后，下游 Gradle 项目可使用 `mavenCentral()` 和对应目标的依赖。例如 `io.github.kltyton.kltytonui:KltytonUI-neoforge-26.2:<已发布版本>`。运行环境仍需安装该目标元数据声明的 Rhino 和 Loader 依赖。
