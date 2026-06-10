# 仓库协作说明

- 本仓库目前是 Android Studio 刚生成的单模块 Android 项目，尚未编写业务代码或项目文档；后续文档与面向用户的回答统一使用中文。
- 只有 `:app` 模块：入口 Activity 是 `app/src/main/java/com/example/vehicle_mountedsystem/MainActivity.java`，当前使用 XML 布局 `app/src/main/res/layout/activity_main.xml`，不要假设已有 Compose 或多模块结构。
- 构建配置使用 Kotlin DSL 与版本目录：根配置在 `settings.gradle.kts`、`build.gradle.kts`，依赖与 Android Gradle Plugin 版本在 `gradle/libs.versions.toml`，模块配置在 `app/build.gradle.kts`。
- 当前 Android 配置为 `namespace/applicationId = com.example.vehicle_mountedsystem`、`compileSdk = release(36) { minorApiLevel = 1 }`、`targetSdk = 36`、`minSdk = 24`；Java 编译目标是 11。
- Gradle wrapper 为 9.4.1，`gradle/gradle-daemon-jvm.properties` 指定 Daemon toolchain 为 JDK 21；优先使用 `./gradlew`，不要改用系统 Gradle。
- `local.properties` 包含本机 SDK 路径且已被 `.gitignore` 忽略；不要把其中路径写入共享配置或文档。

## 常用验证命令

- 调试构建：`./gradlew :app:assembleDebug`
- 本地单元测试：`./gradlew :app:testDebugUnitTest`
- 单个 JUnit 测试示例：`./gradlew :app:testDebugUnitTest --tests "com.example.vehicle_mountedsystem.ExampleUnitTest"`
- Android Lint：`./gradlew :app:lintDebug`
- 设备/模拟器上的 instrumentation 测试：`./gradlew :app:connectedDebugAndroidTest`，需要已连接设备；可用 `--serial <deviceSerial>` 指定设备。

## 当前测试现状

- `app/src/test/java/com/example/vehicle_mountedsystem/ExampleUnitTest.java` 与 `app/src/androidTest/java/com/example/vehicle_mountedsystem/ExampleInstrumentedTest.java` 仍是模板测试；新增业务逻辑时应补充对应的本地或 instrumentation 测试。
