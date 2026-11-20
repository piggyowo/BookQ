# BookQ 0.1.4

这是 BookQ 的第一个 iOS + Android 同步版本。

## iOS

- 修复从「文件」App / iCloud 导入 DOCX 时没有回调的问题。
- 导入选择器、导入确认页、题库详情页统一由一个 sheet 管理，避免弹窗互相抢占。
- **Bundle ID 更新为 `com.boocool.craftquiz`**，不再使用公司拼音。
- 版本升级为 0.1.4（build 4）。

> 如果你安装过旧 Bundle ID 的 0.1.3，新版本不会覆盖旧 App，题库数据也不会自动迁移。请先卸载旧版，再安装这个 0.1.4 IPA。

## Android

- 新增 Android 版：Kotlin + Jetpack Compose，自用安装包，不上架应用商店。
- 底栏为「主页 / 题库 / 设置」；开始练习只在主页，导入按钮只在题库页。
- 支持导入 DOCX、TXT、MD、JSON；一份文件就是一个题库，分类名默认取文件名。
- 支持练习模式 / 考试模式、顺序 / 随机、题目数量设置。
- 没有答案的题，选择后提示「题库中无答案，本题不计分」，不计分、不进错题本。
- 支持错题本、答题卡、得分环、分类掌握度、答题回顾、备份恢复。
- 包名统一为 `com.boocool.bookq`。
- 版本 0.1.4（versionCode 14），已使用固定签名，后续同签名版本可以直接覆盖升级并保留数据。

## 安装说明

### iOS

这是未签名 IPA，需要 AltStore / Sideloadly 用 Apple ID 自签安装；免费账号签名通常 7 天有效。

### Android

直接下载 `BookQ-Android-0.1.4.apk` 安装即可，支持 Android 8.0 及以上。首次安装需要在系统设置里允许“安装未知来源应用”；如果 Play Protect 提示风险，选择“仍要安装”。
