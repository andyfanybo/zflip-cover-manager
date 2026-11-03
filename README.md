# 副屏应用管理 · Z Flip Cover Manager

简体中文 · [English](README.en.md)

在 Galaxy Z Flip5 副屏管理 MultiStar 应用名单。授权一次后，新装的 App 也能直接从副屏添加，无需打开主屏幕。

**[下载 APK](https://github.com/andyfanybo/zflip-cover-manager/releases/latest)** · [实现原理](docs/implementation.md) · [MIT License](LICENSE)

## 界面

| 新版主界面 | 一键全启用确认 | 撤销操作 |
| --- | --- | --- |
| <img src="docs/images/main-v040.png" width="240" alt="v0.4.0 主界面"> | <img src="docs/images/bulk-enable-confirm-v040.png" width="240" alt="批量启用确认，显示操作范围与应用数量"> | <img src="docs/images/undo-example.png" width="240" alt="撤销名单修改"> |

<details>
<summary>首次授权与英文界面</summary>

| 首次设置 | Shizuku 授权 | 英文界面 |
| --- | --- | --- |
| <img src="docs/images/setup-options.png" width="240" alt="首次设置"> | <img src="docs/images/shizuku-permission.png" width="240" alt="Shizuku 授权"> | <img src="docs/images/english.png" width="240" alt="英文界面"> |

</details>

截图来自开发和实测过程，部分为早期界面。个人应用列表已避开；应用全部启用后，对应按钮会变灰。

## 安装

### Shizuku

1. 安装并启动 [Shizuku](https://shizuku.rikka.app/zh-hans/)（v13 及以上）。
2. 安装 APK，打开“副屏应用管理”。
3. 点“授权 / 入口”→“通过 Shizuku 授权并添加入口”，在提示中允许。

完成后无需 Shizuku 持续运行。主屏损坏时，仍需有办法首次打开本工具；如果现有副屏启动器无法打开它，使用下面的 ADB 方式。

### ADB

从下载页获取 APK 和 `setup.ps1`，在装有 Android Platform Tools 的 Windows 电脑运行：

```powershell
.\setup.ps1 -Device '设备序列号或IP:端口' -ApkPath '.\CoverManager-v0.4.0.apk'
```

脚本会安装、授权、备份配置并添加副屏入口。备份保存在当前目录的 `.covermanager-backups`。

也可以手动安装和授权：

```sh
adb -s DEVICE install -r CoverManager-v0.4.0.apk
adb -s DEVICE shell pm grant local.covermanager android.permission.WRITE_SECURE_SETTINGS
adb -s DEVICE shell am start --display 1 -n local.covermanager/.MainActivity
```

打开后点“授权 / 入口”→“已有 ADB 权限：添加 / 修复入口”。手机需先解锁；测试机的副屏 ID 为 `1`。

## 使用

| 操作 | 范围 |
| --- | --- |
| 一键全启用 | 所有可启动应用，忽略搜索和筛选 |
| 已启用 → 全部取消 | 当前筛选和搜索结果，保留管理器入口 |
| 未启用 → 全部启用 | 当前筛选和搜索结果 |
| 撤销 | 恢复上一次修改，支持整批撤销 |

“启用”表示加入 MultiStar 副屏名单，不是应用正在运行。也可以逐个勾选或取消；新装 App 后点“刷新”。图标未更新时，切换一次副屏小组件。

字体跟随系统，支持中文和英文。不限制 MultiStar 版本；配置格式无法识别时只读。实测 Z Flip5 SC-54D / Android 15 / One UI 7，最低系统为 Android 11。

## 构建

需要 JDK 17、Android SDK Platform 35、Build Tools 35.0.0：

```powershell
.\build.ps1 -Jdk 'C:\path\to\jdk-17' -AndroidSdk 'C:\path\to\Android\Sdk'
```

输出为 `build/CoverManager-v0.4.0.apk`。脚本会下载并校验 Shizuku 依赖，运行测试，生成开发签名。自行构建的签名与 Release 不同，请保留自己的签名文件供后续更新。

问题反馈见 [Issues](https://github.com/andyfanybo/zflip-cover-manager/issues)，请注明机型和系统、MultiStar 版本。不要附上完整配置备份或个人应用列表。

本项目采用 [MIT License](LICENSE)，Shizuku 版权声明见 [第三方许可证](docs/SHIZUKU-LICENSE.txt)。
