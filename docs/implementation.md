# 实现原理

工具读写 `Settings.Secure.multistar_setting_repository`，需要通过 ADB 或 Shizuku 授予 `WRITE_SECURE_SETTINGS` 权限。这个权限的范围大于应用名单，本工具只修改下面两个字段。

## 配置

配置包含 27 个以 `/` 分隔的字段，末尾保留 `/`。以 0 起始：

| 索引 | 内容 | 示例 |
| --- | --- | --- |
| 20 | 包名名单 | `com.example.a;com.example.b` |
| 25 | 包名与用户 ID | `com.example.a,0;com.example.b,0` |

空名单为 `0`。写入前检查字段数、包名和用户 ID，其余字段保留原文。移除当前用户的应用时，会保留其他用户的同包名条目。

不检查 MultiStar 版本号。配置格式不匹配时停止写入。图标列表可能延迟刷新，切换一次副屏小组件即可重载。

## Shizuku

使用 Shizuku API 13.1.5。用户允许后，临时 UserService 以 Shell 身份执行固定的 `pm grant`，为本工具授予权限。服务检查调用 UID，不接受任意包名、权限或命令，完成后退出。

日常名单修改由 App 直接完成。卸载重装或权限被撤销后，需要重新授权。

## 检查

配置解析测试覆盖字段保留、重复添加、增删与撤销、跨用户条目保留、异常格式拒绝。真机测试使用 SC-54D / Android 15 / One UI 7 / MultiStar 9.1.09，已验证 ADB、Shizuku 授权和中英文切换。
