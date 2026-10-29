# 副屏应用管理 · Z Flip Cover Manager

**在 Galaxy Z Flip5 的副屏上管理 MultiStar 应用名单。** 主屏损坏后，也能为后来安装的 App 添加副屏入口。

[![License: MIT](https://img.shields.io/badge/License-MIT-8de5d6.svg)](LICENSE)
[![Release](https://img.shields.io/github/v/release/andyfanybo/zflip-cover-manager)](https://github.com/andyfanybo/zflip-cover-manager/releases/latest)

**[下载 APK](https://github.com/andyfanybo/zflip-cover-manager/releases/latest) · [安装与授权](#安装与授权) · [实现原理](docs/implementation.md)**

## 它解决什么问题？

MultiStar 可以让应用运行在 Z Flip5 的副屏，但新装 App 通常需要通过主屏进入设置，再手动添加。主屏无法显示时，这一步就很困难。

本工具提供适合副屏的应用列表。完成一次授权后，在副屏搜索、勾选即可添加应用，无需每次连接电脑。

- 搜索应用名称或包名，勾选添加，取消勾选移除。
- 安装新应用后点击“刷新”，即可在列表中找到。
- 支持查看已添加应用、撤销上一次修改。
- 支持 **ADB** 和 **Shizuku** 两种首次设置方式。
- 无需 root；App 不申请联网权限。
- 校验 MultiStar 版本与配置格式，保留其他配置字段。

## 副屏实机截图

以下为 SC-54D 副屏截图。为避免公开个人应用列表，搜索框仅筛选本工具自身；日常使用可清空搜索框查看全部应用。截图是 748×720 的矩形帧缓冲，真实副屏还包含镜头缺口和系统保留区域。

| 选择首次设置方式 | Shizuku 权限确认 | 授权完成 |
| --- | --- | --- |
| <img src="docs/images/setup-options.png" alt="首次设置菜单，可选择 Shizuku 授权或修复副屏入口" width="260"> | <img src="docs/images/shizuku-permission.png" alt="在副屏允许本工具使用 Shizuku" width="260"> | <img src="docs/images/authorized.png" alt="Shizuku 授权完成后的副屏管理界面" width="260"> |

## 兼容性

| 项目 | 已验证环境 |
| --- | --- |
| 手机 | Samsung Galaxy Z Flip5，日版 SC-54D |
| 系统 | Android 15 / One UI 7 |
| MultiStar | **9.1.09** |
| Shizuku | 真机测试 13.6.0；功能要求 v13 或更新版本 |
| App 最低系统 | Android 11（API 30）；不代表已验证所有机型 |

**目前仅允许对 MultiStar 9.1.09 写入。** 其他版本或格式不匹配时只读。Z Flip6/7、其他 One UI 版本未验证，不承诺兼容。本项目与 Samsung、Good Lock、MultiStar 或 Shizuku 没有官方关联。

## 安装与授权

两种方式安装的是同一个 APK，区别在于如何完成首次授权与建立副屏入口。

| 路径 | 适合谁 | 需要什么 |
| --- | --- | --- |
| ADB | 可使用电脑，或需要由电脑建立首次副屏入口 | 已授权的 USB / 无线 ADB |
| Shizuku | 不熟悉 ADB 命令，希望在手机上点选操作 | Shizuku 已安装并运行，且能首次打开本工具 |

### 方式一：通过 ADB 设置

从 [Releases](https://github.com/andyfanybo/zflip-cover-manager/releases/latest) 下载 APK。Windows 用户还可下载同页的 `setup.ps1`，在已安装 Android Platform Tools 的电脑运行：

```powershell
# USB 或已连接的无线 ADB：用 adb devices 查看设备标识
.\setup.ps1 -Device '设备序列号或IP:连接端口' -ApkPath '.\CoverManager-v0.2.0.apk'
```

脚本依次安装 APK、授予权限、备份当前配置、将本工具加入两份副屏名单，并校验其他字段未改变。备份保存在当前目录的 `.covermanager-backups` 下，请留在自己的电脑上。

也可以手动操作：

```sh
adb -s DEVICE install -r CoverManager-v0.2.0.apk
adb -s DEVICE shell pm grant local.covermanager android.permission.WRITE_SECURE_SETTINGS
adb -s DEVICE shell am start --display 1 -n local.covermanager/.MainActivity
```

首次打开工具后，点右上角 **“授权 / 入口” → “已有 ADB 权限：添加 / 修复入口”**。如果手机处于锁屏，需要先正常解锁。上述 `am start` 的副屏 ID `1` 已在测试机确认，其他设备应自行核实。

### 方式二：通过 Shizuku 设置

1. 从 [Shizuku 官方网站](https://shizuku.rikka.app/zh-hans/) 安装并启动 Shizuku。仅安装而未运行服务无法授权。
2. 从本仓库 Releases 下载 APK，在手机上安装并打开“副屏应用管理”。
3. 点右上角 **“授权 / 入口” → “通过 Shizuku 授权并添加入口”**。
4. 在 Shizuku 提示中选择 **“始终允许”**。
5. 看到 **“Shizuku 已完成授权”** 后，返回 MultiStar 查看入口。未刷新时，切换到其他副屏小组件再回来。

Shizuku 在这里执行一次固定的权限授予。完成后，日常增删名单由工具直接完成，无需 Shizuku 持续运行，也无需电脑持续连接。

> **首次打开的前提：** Shizuku 不能让一个从未打开过的 App 自动出现在副屏。如果主屏损坏，且没有任何现有副屏入口能够打开新 App，需要先通过可用的启动器、已有远程控制方案，或 ADB 路径完成首次打开。不要把“手机已安装 Shizuku”等同于“任何情况下都能完全绕过首次设置”。

## 日常使用

1. 从 MultiStar 打开“副屏应用管理”。
2. 新装 App 后点“刷新”，搜索应用名称或包名。
3. 勾选右侧方框，返回 MultiStar 使用。取消勾选可移除。
4. 图标未更新时，切换一次副屏小组件。

点击应用名称可尝试在当前屏幕启动该 App，是否能正常显示仍取决于系统策略和应用自身适配。工具自身的勾选被保留，避免失去管理入口。

“撤销”只恢复本工具上一次普通名单修改。如果配置已被其他程序更改，会拒绝覆盖；首次建立自身入口不会进入撤销记录。

## 为什么需要这个权限？

`WRITE_SECURE_SETTINGS` 允许修改系统安全设置，权限范围大于本工具实际需要的名单字段。请仅向你信任的构建授予此权限。

本工具实际只写 `Settings.Secure.multistar_setting_repository`，修改其中索引 20 和 25 的应用名单。Shizuku 的临时服务只接受一个固定操作：为 `local.covermanager` 授予该权限，不接收任意包名、权限或 Shell 命令。服务校验调用 UID，执行后关闭。

详见 [实现原理、数据格式与验证记录](docs/implementation.md)。

## 从源码构建

需要 Windows PowerShell、JDK 17、Android SDK Platform 35、Build Tools 35.0.0。

```powershell
.\build.ps1 -Jdk 'C:\path\to\jdk-17' -AndroidSdk 'C:\path\to\Android\Sdk'
```

脚本从 Maven Central 获取 Shizuku API 13.1.5 的四个组件，按 `dependencies.json` 校验 SHA-256，运行配置解析器测试，再构建并校验 APK。输出位于 `build/CoverManager-v0.2.0.apk`。

自行构建默认生成本地开发签名。它与正式 Release 的签名不同，无法直接覆盖已安装的 Release；请保留自己的签名文件，以便后续更新。仓库不包含签名私钥。CI 构建只用于验证，不作为稳定签名发布源。

## 问题反馈

请在 [Issues](https://github.com/andyfanybo/zflip-cover-manager/issues) 描述机型、Android / One UI / MultiStar 版本、授权方式和复现步骤。请勿公开完整配置备份、设备序列号、ADB 配对码或个人应用列表。

## 开源许可证

本项目采用 [MIT License](LICENSE)。Shizuku API 同样采用 MIT License，版权与许可证见 [第三方声明](docs/SHIZUKU-LICENSE.txt)。
