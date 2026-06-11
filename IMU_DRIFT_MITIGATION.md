# IMU 速度积分漂移抑制改进方案

## 目标

解决 `ImuSpeedEstimator` 长期运行后速度估算漂移不可控的问题。当前状态消息为
"IMU 短时估算可用，存在积分漂移"，需要将漂移降低到实用水平。

## 当前算法概要

**文件**：`app/src/main/java/com/example/vehicle_mountedsystem/data/speed/ImuSpeedEstimator.java`（145 行）

```
校准阶段（前 5 样本）:
  bias_x = mean(x_samples)        → 一次性，永不更新
  仅 X 轴校准

积分阶段:
  corrected = x - bias_x
  v += corrected * dt
  if |corrected| < 0.05 × 5 样本 AND v < 0.2 → v = 0   （ZUPT）
  clamp v >= 0

数据源:
  MotionSensorProvider.getLinearAccelerationReading()     → TYPE_LINEAR_ACCELERATION
  MainShellController.refreshDashboardData() 每 200ms 轮询
```

## 漂移根因及贡献占比

| 排名 | 误差源 | 贡献估算 | 机制 |
|------|--------|---------|------|
| 1 | **手机倾斜 → 重力泄漏进 X 轴** | ~60% | 积分只用 X 轴，不做坐标系对齐。手机安装无法保证绝对水平，哪怕 1° 倾角每秒泄漏 ~0.17 m/s² → 10 秒漂 1.7 m/s |
| 2 | **ZUPT 静止漏检** | ~25% | 单阈值 `|a| < 0.05`，不平順路面静止时线性加速度波动超阈值 → 该清零时没清零 |
| 3 | **bias 温度漂移** | ~10% | 首次 5 样本校准后 `biasMps2` 永不变。MEMS 传感器 bias 随温度缓慢漂移 |
| 4 | **积分算法固有随机游走** | ~5% | 每次 `v += a*dt` 的测量噪声累积，无 Kalman 则无法区分噪声和真信号 |

## 改进路线（按投入产出比排序）

### 第一层：坐标系对齐（解决 ~60% 漂移）

**目标**：解除单轴假设，将线性加速度从设备坐标系旋转到世界坐标系后再积分。

**当前代码位置**：`ImuSpeedEstimator.java` 第 78 行
```java
double correctedAcceleration = xMps2 - biasMps2;  // 只积分 X 轴
```

**改为**：
```java
// 用旋转向量获取设备→世界旋转矩阵
float[] R = new float[9];
SensorManager.getRotationMatrixFromVector(R, rotationVector);
// 将线性加速度从设备坐标系转到世界坐标系
float[] worldAccel = new float[3];
// worldAccel = R * [x, y, z]
// 取 Y 轴（世界前向）做速度积分
// Z 轴做重力方向校验（可用于 ZUPT 辅助判断）
```

**所需数据**：`TYPE_GAME_ROTATION_VECTOR`（已在 `MotionSensorProvider.java` 第 24/31 行注册，数据随时可用）

**不改积分算法结构**——仅改变入参，把 `xMps2` 替换为世界前向分量。

**为什么不用 Kalman 就已大幅改善**：60% 的漂移来自重力泄漏。把这个去掉，剩下的 40% 即使按原算法也勉强可用。

**Android 官方提醒**（Developer Docs）：
> TYPE_LINEAR_ACCELERATION always has an offset, which you need to remove.
> The simplest way is to build a calibration step into your application.

我们的校准只做了 X 轴，Y/Z 未处理。坐标系对齐后应该三轴分别校准。

---

### 第二层：改进 ZUPT 静止检测（解决 ~25% 漂移）

**当前代码位置**：`ImuSpeedEstimator.java` 第 115-124 行
```java
if (Math.abs(correctedAcceleration) <= 0.05) stillSamples++;
```

**改为联合检测**：滑动窗口内**加速度方差** + **陀螺能量**同时低于阈值才判定静止。

```
加速度方差 < 0.004 (m/s²)²   （不是幅值，是方差——有偏时幅值不归零但方差归零）
陀螺能量   < 0.06 rad/s       （角运动极少 = 设备被固定）
窗口大小   = 16 样本
确认样本   = 6 连续帧
```

**参考实现**：Spresense IMU 项目已验证的方差阈值，适配大多数消费级 MEMS 芯片。

**陀螺数据**：已在 `MotionSensorProvider` 中注册（TYPE_GYROSCOPE，第 22/29 行），数据随时可用。

---

### 第三层：在线 bias 估计（解决 ~10% 漂移）

**当前代码位置**：`ImuSpeedEstimator.java` 第 103 行校准完成后 `biasMps2` 永不变。

**改为**：在 ZUPT 判定静止期间，用指数移动平均缓慢更新三轴 bias：
```java
biasX = 0.999 * biasX + 0.001 * currentAccelX;   // 时间常数 ~ 200 秒
biasY = 0.999 * biasY + 0.001 * currentAccelY;
biasZ = 0.999 * biasZ + 0.001 * currentAccelZ;
```

**参考**：ROS rtabmap `ComplementaryFilter.cpp` 经验值 `bias_alpha = 0.001`。

---

### 第四层：EKF-ZUPT 框架（解决 ~5% + 精细化前三层）

前三层做完后如果仍不满意，用 Kalman 替代当前积分循环。

**核心差异**：
- 不只积分速度，同时估计**速度误差、姿态误差、偏置误差**
- ZUPT 触发时不只清零速度，还通过协方差矩阵**逆向修正位置漂移**
- 9 状态（位置/速度/姿态误差）或 15 状态（以上 + accel_bias + gyro_bias）

**参考实现**（开源可翻译为 Java）：

| 项目 | 语言 | 规模 |
|------|------|------|
| [pajaraca/IEZ](https://github.com/pajaraca/IEZ) | Python/C++ | 15 状态 EKF，代码结构清晰 |
| [phonefusion_nav](https://github.com/PratyushPro2001/phonefusion_nav) | Python | 手机 IMU 专用，ZUPT 参数经验值已标定 |
| [hcarlsso/ZUPT-aided-INS](https://github.com/hcarlsso/ZUPT-aided-INS) | MATLAB | 学术界参考实现，文献最全 |

**注意**：Kalman 不能替代坐标系对齐——重力泄漏进积分是确定性误差，不是随机噪声，EKF 无法区分"重力泄漏假加速度"和"真加速度"。所以无论如何必须先做第一层。

---

## 实施顺序与依赖

```
第一层：坐标系对齐 ─────────────────────────────┐
  ├── 依赖：TYPE_GAME_ROTATION_VECTOR（已有）      │
  └── 影响：ImuSpeedEstimator.java（改输入）        │
                                                   │
第二层：改进 ZUPT ──────────────────────────────┤ 可并行
  ├── 依赖：TYPE_GYROSCOPE（已有）                  │
  └── 影响：ImuSpeedEstimator.java（改检测逻辑）    │
                                                   │
第三层：在线 bias ──────────────────────────────┘
  ├── 依赖：第二层（需要准确 ZUPT 判断静止窗口）
  └── 影响：ImuSpeedEstimator.java（新增状态变量）

第四层：EKF-ZUPT（前三层效果不够时再上）
  ├── 依赖：前三层的 ZUPT 信号 + 世界坐标系加速度
  └── 影响：新建文件，替换 ImuSpeedEstimator 算法部分
```

**第二层和第三层可同时实施**（独立改动，不冲突）。

## 涉及文件清单

| 文件 | 改动层 | 改动内容 |
|------|--------|---------|
| `data/speed/ImuSpeedEstimator.java` | 第一、二、三层 | 坐标系转换、方差检测、在线 bias |
| `data/sensor/MotionSensorProvider.java` | 第一、二层 | 暴露旋转向量和陀螺读值给估算器（当前已有 getter，可能需要加同步方法） |
| `ui/MainShellController.java` | 第一层 | 可能需要调整传感器数据传递方式（从 200ms 轮询改为推送） |
| `test/.../ImuSpeedEstimatorTest.java` | 全部 | 新用例：倾斜静止不积分、ZUPT 方差检测、bias EMA 更新 |

## 验证标准

| 层级 | 通过条件 |
|------|---------|
| 第一层 | 手机在桌面静止 60 秒，速度始终 < 0.05 m/s |
| 第二层 | 放在不平整表面（如手掌悬空不动），ZUPT 仍能正确触发 |
| 第三层 | 运行 10 分钟后重新静止，速度能归零（bias 已自动修正） |
| 第四层 | 步行 50 米往返，总位移误差 < 5 米 |

## 不纳入范围

- **GPS / 轮速 / 外部参考**：项目约束无定位权限、无 CAN 总线
- **Madgwick / Mahony 自写姿态解算**：Android `ROTATION_VECTOR` 已是硬件级 Kalman 融合，自写反而更差
- **深度学习（LSTM/CNN）**：投入产出比远低于前四层，仅在其他手段耗尽时考虑
- **红外 / 外部传感器**：项目约束排除

## 参考阅读

- **AGENTS.md**：项目架构和约束总览
- **DESIGN.md**：权威设计文档，速度估算章节
- [OpenShoe: ZUPT-aided INS](https://github.com/hcarlsso/ZUPT-aided-INS) — ZUPT 检测器参考
- [phonefusion_nav](https://github.com/PratyushPro2001/phonefusion_nav) — 手机 IMU ZUPT 参数
- [Spresense IMU 方差阈值](https://github.com/Taylorwang0614/cxd5602pwbimu_localizer_arduino) — 消费级 MEMS 方差经验值
- [rtabmap ComplementaryFilter](https://docs.ros.org/en/api/rtabmap/html/ComplementaryFilter_8cpp_source.html) — 在线 bias 估计参考
