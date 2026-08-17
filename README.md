# 智能车载系统（Android）

运行在 Android 手机上的车机模拟程序：利用手机自带传感器与系统服务，模拟车载仪表盘、导航、空调、多媒体等功能的完整交互体验。

## 功能

8 个页面模块：

- 仪表盘总览
- 驾驶专注视图
- 演示导航
- 传感器数据诊断
- 空调状态管理
- 多媒体播放控制
- 车身控制模拟
- 系统能力诊断

核心原则：**只做手机能做的事**——不依赖 CAN 总线、红外发射、地图 SDK 等外部硬件，全部以手机传感器、系统服务与本地状态机实现，并保证任何数据源不可用时的优雅降级。

## 技术要点

- **IMU 速度估算**：陀螺仪 + 加速度计积分实时估算速度，不依赖 GPS；漂移抑制方案（坐标系对齐 / ZUPT / 在线偏置估计）详见 `IMU_DRIFT_MITIGATION.md`
- **手写页面控制器框架**：单 Activity + ViewGroup 切换 8 个页面，无 Fragment / Navigation 依赖，`MainShellController` 集中管理页面生命周期与数据分发
- **强制可用性降级**：`AvailabilityStatus` 值对象在构造时强传可用性，不可用数据编译期报错；统一处理缺陀螺仪、读不到电池电流、三星旋转向量 3/4 元素差异等硬件差异
- **七层单向依赖分层**：入口 / UI 控制 / UI 页面 / 自定义视图 / 数据 / 模型 / 工具，无循环引用
- **自定义视图**：Canvas 绘制姿态仪表（`AttitudeIndicatorView`）与 G 力球（`GForceIndicatorView`）

## 构建

```bash
./gradlew :app:assembleDebug               # 调试构建
./gradlew :app:testDebugUnitTest           # 本地单元测试
./gradlew :app:lintDebug                   # Lint
./gradlew :app:connectedDebugAndroidTest   # 设备/模拟器仪器测试
```

环境要求：Gradle wrapper 9.4.1（daemon JDK 21）、AGP 9.2.1；`compileSdk 36` / `minSdk 24`，Java + XML View 技术栈。

## 架构

`MainActivity` → `MainShellController`（中央调度器）→ `TabController` + 8 个页面控制器；共享数据提供者（传感器 / 电池 / 媒体 / 空调）只初始化一次，经**推送**（主线程 200ms 定时刷新）与**监听**（页面进入注册、离开注销）双路径分发数据。

## 文档

- `DESIGN.md` — 设计决策与方案论证
- `AGENTS.md` — 仓库协作约定（面向 AI 代理与协作者，含构建/测试/架构约束）
- `IMU_DRIFT_MITIGATION.md` — IMU 速度估算漂移抑制的完整方案
