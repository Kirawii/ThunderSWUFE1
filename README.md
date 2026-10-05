# ThunderSWUFE

面向西南财经大学宿舍用户的 Android 用电记录与分析应用。项目使用 Jetpack Compose、Room、WorkManager 和 Retrofit，实现按宿舍查询、历史趋势、低余额提醒、CSV 导入、异常分析与每日用电预测。

## 安全说明

- 仓库不包含会话 Token、Cookie、签名密钥、APK 或个人用电 CSV。
- 会话 Token 由用户在设置页输入，使用 Android Keystore 支持的 AES-GCM 加密后保存在本机。
- 应用关闭系统备份，并明确排除数据库和偏好设置。
- 请勿把真实 Token、宿舍号或个人 CSV 加入 Git。曾经提交过的凭据必须在服务端撤销，并从 Git 历史中清除。

## 本地构建

1. 安装 JDK 17 与 Android SDK 35。
2. 在 `local.properties` 中设置 `sdk.dir`，或配置 `ANDROID_HOME`。
3. 运行：

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

首次启动后，在设置页填写房间号、楼栋、区域和自己的会话 Token。后台任务每 6 小时检查一次余额；手动刷新会显示 WorkManager 的真实完成状态。

校方接口没有公开 `Sign` 生成规范，公开资料也未找到可验证算法。本项目不再使用抓包得到的静态 `Time/Sign/Cookie`，而是发送当前时间、用户自己的 Authorization 与对应 `etToken` Cookie。接口升级或强制签名时，应通过官方授权流程适配，不能重新硬编码他人的会话。网络查询必须在自己的真实账号和宿舍上进行设备联调。

## CSV 导入

设置页可通过系统文件选择器导入 CSV，不再从 APK 内置个人数据。格式为：

```csv
Timestamp,Balance,Change
2025-01-01 12:00:00,100.0,1.5
```

## 界面与曲线图

界面使用绿色 Material 3 配色并适配深色模式。首页按日、月或小时汇总历史数据；余额使用每个时间段的最新记录，用电量排除充值造成的负变化。图表支持点击查看日期和读数，历史曲线使用淡色面积填充，预测曲线使用虚线并最多展示 30 天。所有图表统一以度（kWh）标注，缺少数据时显示明确的空状态。

## 预测方法

App 默认沿用最近完整日的用电量，并提供 α=0.8 指数平滑，递归估算余额耗尽时间。两种方法都在设备本地运行；可靠度来自用户本机最近历史的一步回测，不是准确率保证。

严格滚动验证表明 Ridge、ElasticNet 和树模型都未能稳定超过简单基线，所以没有把未达标的训练参数带入上线包。完整实验协议、指标和停止结论见 [MODEL_CARD.md](MODEL_CARD.md)。

使用私有 CSV 重新训练：

```bash
python app/src/main/python/experiment_models.py private.csv \
  --output experiments/forecast_results.json
```

训练 CSV 属于私有输入，不应提交。

## 工程结构

- `network/`：服务接口与脱敏网络客户端
- `data/database/`：Room 数据库、迁移和按房间索引
- `work/`：周期检查与预测任务
- `ml/`：设备本地的昨日值/指数平滑预测与动态回测可靠度
- `ui/`：Compose 页面与 ViewModel
- `.github/workflows/android.yml`：测试、Lint 与构建质量门禁
