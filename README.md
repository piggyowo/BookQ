# BookQ · iOS + Android

「织物靛蓝」题库 App 的**用户导入版**：App 本身不内置任何题目，首次打开是空题库。用户在「题库」页导入 DOCX / TXT / MD / JSON，解析确认后再保存；主页负责开始练习，设置页管理默认偏好与备份。

同一套设计、题库格式和交互规则现在同时提供 **iOS（SwiftUI）** 与 **Android（Kotlin + Jetpack Compose）** 版本。Android 版为自用安装包，不上架应用商店，使用固定签名，后续版本可直接覆盖升级。

## 直接运行

1. 需要 macOS + Xcode 16（最低部署版本 iOS 16.0）。
2. 双击 `QuizApp.xcodeproj`，等待 Swift Package 拉取 ZIPFoundation。
3. 选中 `QuizApp` Target → Signing & Capabilities → Team 选择你的 Apple ID（免费账号即可）。
4. 选 iPhone 模拟器或真机，Run。

## GitHub Actions 出 IPA + APK

仓库已带 `.github/workflows/build-ipa.yml`：

```bash
git tag v0.1.4
git push origin v0.1.4
```

推送 tag 后，Actions 会同时：

- 在 macOS `macos-15-intel` runner 上编译 iOS，Artifacts 留下 `BookQ-unsigned-ipa`；
- 在 Ubuntu runner 上编译 Android，Artifacts 留下 `BookQ-android-apk`；
- 两个产物都挂到同一个 GitHub Release：`BookQ-unsigned.ipa` + `BookQ-Android-0.1.4.apk`。

也可以在 Actions 页面手动 `Run workflow`，只取 Artifacts。

## Android 构建

```bash
cd BookQAndroid
gradle :app:assembleRelease
```

- 需要 JDK 17、Android SDK 35、Gradle 8.9。
- 输出：`BookQAndroid/app/build/outputs/apk/release/app-release.apk`。
- 包名 `com.boocool.bookq`，最低 Android 8.0（API 26）。
- 签名文件在 `BookQAndroid/keystore/bookq-release.jks`，固定使用，保证后续版本可覆盖安装并保留题库数据。

### 关于签名

工作流产出的是**未签名 IPA**，不能直接点安装：

- **AltStore / Sideloadly**：用自己的 Apple ID 在电脑或手机上重签后再装。免费 Apple ID 签名 7 天有效，到期需重签；最多同时装 3 个自签 App。
- **Xcode 直接装**：连线 iPhone，选择自己的 Team 后 Run，最省事。
- **长期/直接分发**：需要付费 Apple Developer 账号（$99/年），用证书 + 描述文件签名；上架 App Store 还需隐私清单、截图等材料。

如果只改 `DEVELOPMENT_TEAM` 和 `PRODUCT_BUNDLE_IDENTIFIER`，就能在自己的账号下正常签名。

## 功能

- 底栏 3 项：**主页 / 题库 / 设置**。App 启动时题库为空，不打包任何题目。
- **主页**：空状态引导去题库页导入；有题库后显示「今天，练 N 题」、最近题库进度、我的题库、本周答题统计、薄弱环节。开始练习只在主页触发，弹出统一设置面板。
- **开始设置**：范围（全部题库 / 单个题库）、模式（练习 / 考试）、题量（10 / 20 / 50 / 全部）、顺序（顺序 / 随机）。
- **题库页**：导入按钮只在这里；搜索、筛选、题库卡详情、重命名、删除。题库页没有开始按钮。
- **导入**：DOCX / TXT / MD / JSON。DOCX 直接读 `word/document.xml`，不依赖系统 Word；解析后先显示题库名、题数、无答案数、异常提示和前 3 题样例，确认后才保存。
- **无答案题**：题库答案为空（选项可以存在）时，用户选择后提示「题库中无答案，本题不计分」，不计分、不进错题本、不加掌握度。只有确实没有选项的题才显示「本题未提供选项」。
- **答题**：题号水印、题库 pill、计时、进度条、A–D 选项、练习即时对错反馈、考试可改选。
- **答题卡**：5 列网格、未答 / 已答 / 答对 / 答错 / 无答案 / 当前五态，点格跳题。
- **结果**：得分环、正确/错误/正确率/错题本统计、题库掌握度、错题回顾（可展开）、再来一轮。
- **错题本**：答错自动收集，答对自动移出，支持移除/清空/重练。
- **设置**：默认模式、默认题量、默认顺序、触感反馈、导出备份、从备份恢复、清空答题记录。
- 全部记录和题库存在本机 `Application Support/CraftQuiz/state.json`，离线可用，不上传文档。

## 导入格式

### DOCX / TXT / MD

```text
布艺打印
1. 题干（B）
A. 选项一  B. 选项二  C. 选项三  D. 选项四
2. 另一题（ ）
A. 选项一  B. 选项二  C. 选项三  D. 选项四
```

- 一份文件 = 一个题库分类，分类名取文件名（导入确认页可改）；文档内部的「布艺打印」这类小标题只作为分隔，不参与分类。
- 答案支持 `（A）`、`(A)`、行尾缺右括号 `（A`；空括号 `（）` 视为题库无答案。
- 选项支持 A–D + 冒号/顿号/点/逗号；选项可以和题干在同一行。

### JSON

```json
[
  {
    "num": "1",
    "stem": "格雷特机器的生产属性是（　）",
    "options": [["A", "热转印"], ["B", "直喷"], ["C", "喷绘"], ["D", "UV"]],
    "answer": "B"
  }
]
```

也支持 `{"name": "题库名", "questions": [...]}`。`answer` 可以是 `null` 或缺省。

## 目录

```
QuizApp.xcodeproj/
QuizApp/
  QuizApp.swift      入口、底栏容器、页面切换
  Models.swift       题目/题库/模式/结果/导入模型
  Store.swift        题库导入管理、答题状态、计分、错题本、持久化
  ImportService.swift DOCX/TXT/MD/JSON 解析
  Theme.swift        配色、字体、背景、底栏、通用组件
  HomeView.swift     主页 + 空状态
  StartQuizSheet.swift 开始练习设置面板
  LibraryView.swift  题库页 + 导入确认 + 题库详情
  SettingsView.swift 设置 + 备份恢复
  QuizView.swift     答题页 + 答题卡
  ResultView.swift   结果页 + 错题本
  Assets.xcassets    AppIcon
BookQAndroid/
  app/src/main/java/com/boocool/bookq/
    MainActivity.kt   入口、底栏、文件选择、BottomSheet
    Models.kt         题目/题库/模式/结果模型
    Store.kt          题库管理、答题状态、计分、错题本、持久化
    ImportService.kt  DOCX/TXT/MD/JSON 解析
    Theme.kt          织物靛蓝、纸纹、底栏与通用组件
    HomeScreen.kt     主页
    LibraryScreen.kt  题库页 + 导入卡
    SettingsScreen.kt 设置 + 备份恢复
    Sheets.kt         开始练习 / 导入确认 / 题库详情
    QuizScreen.kt     答题页 + 答题卡
    ResultScreen.kt   结果页 + 错题本
  keystore/bookq-release.jks  固定 Android 签名
.github/workflows/build-ipa.yml
```

工程里不再保留内置题库文件，首次启动题库为空；旧版 `questions.json` 已移除。

## 备注

- Bundle ID 默认 `com.boocool.craftquiz`，若与已有 App 冲突，改 Target 的 `PRODUCT_BUNDLE_IDENTIFIER`。
- App 显示名是「BookQ」，在 `QuizApp/Info.plist` 的 `CFBundleDisplayName` 修改。
- DOCX 解析使用 ZIPFoundation 0.9.19（SPM），首次打开 Xcode 需要联网拉取依赖；GitHub Actions 也会自动解析。
- 本工程未包含 App Store 上架所需的隐私清单、审核素材和正式签名配置。
