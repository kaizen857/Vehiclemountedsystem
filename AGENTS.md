# 仓库协作说明

面向用户的回答统一使用中文。本文件只记录未来 OpenCode 代理容易猜错的仓库事实；更完整的产品范围看 `DESIGN.md`，IMU 漂移细节看 `IMU_DRIFT_MITIGATION.md`。当前没有 `README*`、CI workflow、`opencode.json` 或 `.opencode/` 配置；`.omo/boulder.json` 是本地任务追踪，不要随意删除 `.omo/`。

## 项目边界

- 单模块 Android 项目，只有 `:app`；包名、namespace、applicationId 都是 `com.example.vehicle_mountedsystem`。
- 技术栈是 Java + XML View + AppCompat/Material/ConstraintLayout；不要假设已有 Compose、Fragment、Navigation、ViewModel、Room 或多模块结构。
- 入口是 `MainActivity`，只加载 `activity_main.xml`、处理 EdgeToEdge insets，并创建/销毁 `MainShellController`。
- `DESIGN.md` 早期段落仍把仓库描述成模板工程，已过时；以当前 Gradle/Manifest/Java/XML 代码为准。

## 构建与验证

- 必须使用仓库 wrapper：`./gradlew`。不要改用系统 Gradle。
- Toolchain：Gradle wrapper 9.4.1，`gradle/gradle-daemon-jvm.properties` 指定 daemon JDK 21，AGP 9.2.1，Java 11。
- Android 配置在 `app/build.gradle.kts`：`compileSdk = release(36) { minorApiLevel = 1 }`，`targetSdk = 36`，`minSdk = 24`。
- 依赖通过 `gradle/libs.versions.toml` 管理；`settings.gradle.kts` 使用 `RepositoriesMode.FAIL_ON_PROJECT_REPOS`，不要在模块里临时加仓库。
- 常用命令：
  - 调试构建：`./gradlew :app:assembleDebug`
  - 全部本地单测：`./gradlew :app:testDebugUnitTest`
  - 单个 JUnit：`./gradlew :app:testDebugUnitTest --tests "com.example.vehicle_mountedsystem.HvacRepositoryTest"`
  - Android Lint：`./gradlew :app:lintDebug`
  - Instrumentation：`./gradlew :app:connectedDebugAndroidTest`，需要连接设备或启动模拟器。
- `local.properties` 存本机 SDK 路径且被 `.gitignore` 忽略；不要把其中路径写进共享配置或文档。

## 运行架构

- 当前是单 Activity + 页面控制器，不是 Fragment：`MainActivity` -> `MainShellController` -> `TabController` + 8 个 `ui/pages/*PageController`。
- 页面切换由 `MainShellController` 执行：先 `stopCurrentPage()`，再 `pageHostView.removeAllViews()` + `addView(createPageView(tab))`。
- 每个页面控制器通过 `createView(ViewGroup)` inflate 自己的 XML；传感器页缓存 view 并用 provider listener 节流刷新，媒体页进入时注册 snapshot listener、离开时清理。
- `MainShellController` 构造时启动共享 `MotionSensorProvider`，并用主线程 `Handler` 周期刷新总览/简洁/导航页；`destroy()` 会移除回调、停止当前页并停止传感器。
- `ui/` 除 shell/tab 外还有自定义 View：`AttitudeIndicatorView` 和 `GForceIndicatorView`。

## 数据与降级模式

- 数据提供者要保持可注入：Android 便捷构造函数 + 接口构造函数，便于本地单测注入 fake。
- 已有模式：`MotionSensorProvider.MotionSensorSource`、`HvacStorage`、`BatteryStatusProvider.BatteryDataSource`、`SystemMediaController.SessionGateway/MediaKeyGateway`。
- UI 层不要收到 `null` 状态；用 `AvailabilityStatus.available/unavailable(message, timestamp)` 表达可用性和中文降级文案。
- `ImuSpeedEstimator` 是纯 Java 类；速度单位是 m/s，UI 文案必须是“IMU 估算速度”或“短时估算速度”，不要写“真实车速”。
- 空调基础版只维护本地状态机并用 `SharedPreferences` 持久化；不要提前加入 `ConsumerIrManager`、红外权限或红外码库。
- 控制页车窗/后视镜/座椅是本地演示状态，不代表真实车辆控制。

## 权限与外部能力

- Manifest 当前只声明 `HIGH_SAMPLING_RATE_SENSORS` 和 `MediaNotificationListenerService`；不要为基础功能添加 `INTERNET`、定位权限或 `ACCESS_NETWORK_STATE`。
- Activity 强制横屏：`android:screenOrientation="landscape"`，并声明 `configChanges="orientation|screenSize|keyboardHidden"`；XML 布局按横屏车机界面设计。
- 导航页是演示导航，不接地图 SDK、`MapView`、`SupportMapFragment`、GPS 或网络；保留“演示导航 · 无地图依赖”“演示模式”等文案。
- 不接入 `android.car`、Vehicle HAL、CAN、OBD 或真实车辆控制。
- 媒体页通过 `MediaSessionManager`/`MediaController` 读取活动会话；普通应用需要用户手动开启通知监听权限。未授权或无会话时，`SystemMediaController` 降级发送系统媒体键。
- `MediaNotificationListenerService` 当前仍是空 `NotificationListenerService` 壳；媒体逻辑主要在 `SystemMediaController` 和 `MediaPageController`。

## UI 约束

- 主题是 Material 3：`Theme.Material3.DayNight.NoActionBar`，冷色科技风暗色主题。
- 关键颜色：`@color/bg_dark` `#0B0F19`，`@color/accent_cyan` `#06B6D4`，`@color/accent_blue` `#38BDF8`。
- 不要在 UI 文件里引入 Material 2 主题或浅色默认风格；新增页面优先复用现有颜色、dimens、卡片/标签文案模式。

## 测试约定

- 新增纯 Java 业务逻辑时补本地 JUnit，优先 fake 注入而不是 Android 依赖。
- 参考测试：`HvacRepositoryTest`、`MotionSensorProviderTest`、`BatteryStatusProviderTest`、`SystemMediaControllerTest`、`ImuSpeedEstimatorTest`、`VehicleModelTest`、`UnitFormatterTest`。
- 页面切换、横屏 shell、持久化 UI 状态用 instrumentation，参考 `MainShellSmokeTest` 和 `PageSwitchingTest`。
- `ExampleUnitTest` 与 `ExampleInstrumentedTest` 仍存在但只是模板，不要把它们当作业务覆盖依据。
