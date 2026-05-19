# UWB 測距示範應用 / UWB Ranging Sample App

[![Android](https://img.shields.io/badge/Platform-Android%2012%2B-green)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1.21-blueviolet)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-BOM%202026.04.01-blue)](https://developer.android.com/jetpack/compose)
[![UWB](https://img.shields.io/badge/androidx.core.uwb-1.0.0-orange)](https://developer.android.com/jetpack/androidx/releases/core-uwb)
[![License](https://img.shields.io/badge/License-Apache%202.0-lightgrey)](LICENSE)

> 以 `androidx.core.uwb:uwb:1.0.0`（2026-05-06 首個穩定版）實作的 Android UWB 學習示範專案。  
> A hands-on Android sample demonstrating UWB ranging with the first stable release of `androidx.core.uwb:1.0.0`.

---

## 目錄 / Table of Contents

- [專案概述](#專案概述--overview)
- [裝置需求](#裝置需求--device-requirements)
- [技術棧](#技術棧--tech-stack)
- [架構說明](#架構說明--architecture)
- [快速開始](#快速開始--quick-start)
- [應用程式流程](#應用程式流程--app-flow)
- [OOB 參數交換操作步驟](#oob-參數交換操作步驟--oob-exchange-walkthrough)
- [SM-N9860 相容性說明](#sm-n9860-相容性說明--n9860-compatibility-notes)
- [UWB 核心概念](#uwb-核心概念--uwb-core-concepts)
- [已知限制](#已知限制--known-limitations)
- [專案結構](#專案結構--project-structure)
- [授權](#授權--license)

---

## 專案概述 / Overview

本專案為 UWB（超寬帶，Ultra Wideband）技術的入門學習範例，涵蓋：

- ✅ UWB 硬體與軟體能力的執行期檢查
- ✅ Controller / Controlee 雙角色支援
- ✅ 手動 OOB（Out-of-Band）參數交換流程
- ✅ 即時距離（公尺）、方位角（Azimuth）、仰角（Elevation）顯示
- ✅ Android 13 byte-order debug 工具
- ✅ Samsung 中國版韌體（CHC CSC）相容性處理

This project is a learning-focused Android UWB sample covering:
capability checks, dual-role ranging (Controller/Controlee), manual OOB exchange,
and real-time distance/AoA display. Includes workarounds for Samsung CHC firmware and
Android 13 address byte-order issues.

---

## 裝置需求 / Device Requirements

| 角色 / Role | 裝置 / Device | Android | OneUI | UWB 支援 |
|---|---|---|---|---|
| Controller | SM-S9180 (Galaxy S23 Ultra) | 14+ | 8 | ✅ 確認 |
| Controlee | SM-N9860 (Galaxy Note20 Ultra) | 13 | 5.1 | ⚠️ 見下方說明 |

**最低需求 / Minimum Requirements**
- `minSdk = 31`（Android 12）— `UWB_RANGING` 執行期權限的最低版本
- 需要 UWB 硬體（`PackageManager.FEATURE_UWB`）
- 前景模式使用（Android 13 及以下不支援背景測距）

---

## 技術棧 / Tech Stack

| 類別 | 套件 | 版本 |
|---|---|---|
| 語言 | Kotlin | 2.1.21 |
| UI | Jetpack Compose BOM | 2026.04.01 |
| 導航 | Navigation Compose | 2.8.9 |
| DI | Hilt | 2.56.2 |
| 非同步 | Kotlin Coroutines + StateFlow | 1.9.0 |
| **UWB** | **androidx.core.uwb** | **1.0.0** |
| Build | Android Gradle Plugin | 8.10.1 |
| Code gen | KSP | 2.1.21-2.0.1 |

---

## 架構說明 / Architecture

本專案採用 **MVVM + Clean Architecture** 三層分離設計。

### 分層圖 / Layer Diagram

```
┌─────────────────────────────────────────────────────────┐
│  presentation/                                          │
│  ┌─────────────────────────────────────────────────┐   │
│  │  Screen (Compose)  ←→  ViewModel                │   │
│  │  只知道 domain model，不知道 data 層的存在        │   │
│  └─────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────┤
│  domain/（純 Kotlin，零 Android import）                 │
│  ┌──────────────┐  ┌─────────────┐  ┌───────────────┐  │
│  │    model/    │  │ repository/ │  │   usecase/    │  │
│  │  (data class)│  │ (interface) │  │（業務邏輯封裝）│  │
│  └──────────────┘  └─────────────┘  └───────────────┘  │
├─────────────────────────────────────────────────────────┤
│  data/（實作 domain 介面）                               │
│  ┌────────────────────┐  ┌──────────────────────────┐  │
│  │  uwb/              │  │  repository/             │  │
│  │  UwbManagerWrapper │  │  UwbRepositoryImpl       │  │
│  │  RangingResultMapper│  │  （快取 UwbScope）       │  │
│  │  ← 唯一接觸 UWB API│  │                          │  │
│  └────────────────────┘  └──────────────────────────┘  │
├─────────────────────────────────────────────────────────┤
│  di/  UwbModule（Hilt 黏合上下層）                      │
└─────────────────────────────────────────────────────────┘
```

### 為什麼這樣分層？

| 層 | 職責 | 可測試性 |
|---|---|---|
| domain | 業務規則（狀態機轉換、資料模型）| 純 JUnit，不需 Android 環境 |
| data | UWB API 呼叫、錯誤轉換、scope 管理 | Mockito mock UwbManagerWrapper |
| presentation | UI 狀態渲染、使用者互動 | ComposeTest / 快照測試 |

---

## 快速開始 / Quick Start

### 前置需求 / Prerequisites

1. Android Studio Meerkat 2024.3.x 或更新版本
2. Gradle 8.13（`gradle-wrapper.properties` 已設定，會自動下載）
3. 兩台 UWB 支援的 Android 裝置（見 [裝置需求](#裝置需求--device-requirements)）

### 建置步驟 / Build Steps

```bash
# Clone 專案
git clone <your-repo-url>
cd uwbtest

# 在 Android Studio 開啟，等待 Gradle sync 完成
# 或透過命令列建置（需要 ANDROID_HOME 環境變數）
./gradlew assembleDebug
```

### 安裝到裝置

```bash
# 安裝到 S23 Ultra（Controller）
adb -s <S23_SERIAL> install app/build/outputs/apk/debug/app-debug.apk

# 安裝到 Note20 Ultra（Controlee）
adb -s <N9860_SERIAL> install app/build/outputs/apk/debug/app-debug.apk
```

---

## 應用程式流程 / App Flow

```
[兩台裝置] → CapabilityCheck → RoleSelect → OobExchange → Ranging
                                                ↑
                              手動複製貼上交換地址 + 信道參數
```

1. **Capability Check**：兩台裝置分別確認 UWB 可用
2. **Role Select**：S23 Ultra 選 Controller；Note20 Ultra 選 Controlee
3. **OOB Exchange**：互相複製對方地址，填入 Session Key
4. **Ranging**：查看即時距離與 AoA 數據

---

## OOB 參數交換操作步驟 / OOB Exchange Walkthrough

OOB（Out-of-Band）表示 UWB 本身不處理裝置探索，需要透過其他管道（本 App 使用手動複製）交換連線參數。

### 操作流程（兩台裝置需同步操作）

| 步驟 | Controller（S23 Ultra） | Controlee（Note20 Ultra） |
|---|---|---|
| 1 | 進入 OOB 畫面，看到 My Address + Channel + Preamble | 進入 OOB 畫面，看到 My Address |
| 2 | 點擊複製圖示，複製「A1:B2  CH:9  PR:10」格式的字串 | 點擊複製圖示，複製「C3:D4」格式的地址 |
| 3 | 在「Peer Address」輸入 C3:D4（從 Note20 複製而來） | 在「Peer Address」輸入 A1:B2（從 S23 複製而來） |
| 4 | Channel/Preamble 已自動填入 | 手動輸入 Channel=9、Preamble=10（從 S23 得知） |
| 5 | 確認 Session Key 一致（預設 `0102030405060708`） | 確認 Session Key 一致 |
| 6 | 點擊「開始測距」| 點擊「開始測距」|

> 💡 **實際 App 中**，OOB 交換通常透過 BLE（藍牙低功耗）自動完成。本 App 以手動方式呈現以便學習理解。

---

## SM-N9860 相容性說明 / N9860 Compatibility Notes

### 裝置資訊

Samsung Galaxy Note20 Ultra（SM-N9860）是 Samsung 最早支援 UWB 的機型之一。
然而，**SM-N9860 存在多種韌體變體**，UWB 支援情況因韌體而異：

| CSC 代碼 | 銷售地區 | UWB 支援 |
|---|---|---|
| CHC | 中國大陸 | ⚠️ 可能因國碼政策停用 |
| TGY | 香港 | ✅ 通常可用 |
| CHT | 台灣 | ✅ 通常可用 |

### 確認方式

在 CapabilityCheckScreen 中：
- **Hardware Present = ✓ / API Available = ✗**：硬體存在但韌體層停用，可嘗試更換 CSC
- **Hardware Present = ✓ / API Available = ✓**：完全支援，可正常測距

### 已知問題

- **Android 13 byte-order**：UWB 地址可能以反向位元組順序傳遞。若 ranging 無法啟動，嘗試開啟 OOB 畫面的「Reverse Bytes」開關
- **背景測距**：Android 13 不支援背景 UWB ranging，本 App 僅支援前景使用

---

## UWB 核心概念 / UWB Core Concepts

### 什麼是 UWB？

Ultra Wideband（超寬帶）是一種短距離無線通訊技術，透過極短的脈衝訊號實現**精確的空間感知**：

- 測距精度：± 10 cm 級別（相較於 Wi-Fi/BLE 的 ±1-3 m）
- 測距距離：約 10-30 m（室內環境）
- 角度測量：Azimuth（水平方位角）+ Elevation（垂直仰角）
- 標準：IEEE 802.15.4z / FiRa Consortium

### Controller vs Controlee

| 角色 | 職責 | API |
|---|---|---|
| Controller | 發起 session，決定 UWB 信道 | `UwbManager.controllerSessionScope()` |
| Controlee | 回應 session，向 Controller 回報位置 | `UwbManager.controleeSessionScope()` |

### Ranging Session 生命週期

```
1. 申請 UWB_RANGING 執行期權限
   ↓
2. 建立 UwbScope（Controller 或 Controlee）
   → 取得 localAddress（+ channel/preamble for Controller）
   ↓
3. OOB 參數交換（本 App 使用手動複製）
   ↓
4. 建立 RangingParameters
   - uwbConfigType = CONFIG_UNICAST_DS_TWR
   - sessionKeyInfo（8 bytes，兩端一致）
   - complexChannel（來自 Controller scope）
   - peerDevices（對端地址）
   ↓
5. scope.prepareSession(params).execute() → Flow<RangingResult>
   ↓
6. 收集 Flow：
   RangingResultInitialized     → 握手成功
   RangingResultPosition        → 距離 + AoA 資料
   RangingResultPeerDisconnected → 對端離開
   RangingResultFailure          → 錯誤（含 reason code）
   ↓
7. 取消 Flow 收集 → 停止 ranging
```

### ⚠️ Scope 快取的重要性

每次呼叫 `controllerSessionScope()` 都會**產生新的地址**。
如果取地址和開始 ranging 用了不同的 scope，session 永遠無法建立。

本 App 在 `UwbRepositoryImpl` 中快取 scope，確保 OOB 顯示的地址和 ranging 使用的地址來自同一個 scope。

---

## 已知限制 / Known Limitations

| 項目 | 說明 |
|---|---|
| 背景測距 | Android 13（Note20 Ultra）不支援背景 UWB ranging |
| Static STS | Session Key 為靜態，僅適合學習用途；生產環境應使用 Dynamic STS（需 FiRa 認證）|
| OOB 機制 | 本 App 使用手動複製貼上；真實 App 應透過 BLE 或 Wi-Fi Direct 自動交換 |
| 多裝置 ranging | 目前僅支援兩裝置一對一 ranging |
| iOS 不支援 | `androidx.core.uwb` 為 Android 專屬；iOS 需使用 Apple NearbyInteraction（不同 API）|

---

## 專案結構 / Project Structure

```
app/src/main/java/com/example/uwbtest/
│
├── UwbApp.kt                     @HiltAndroidApp 入口
│
├── di/
│   └── UwbModule.kt              Hilt 依賴注入模組
│
├── domain/                       ← 純 Kotlin，零 Android import
│   ├── model/
│   │   ├── UwbCapability.kt      裝置 UWB 能力描述
│   │   ├── RangingState.kt       Ranging 狀態機（sealed interface）
│   │   ├── UwbDeviceInfo.kt      本機地址 + 角色資訊
│   │   └── OobParams.kt          OOB 交換的參數集合
│   ├── repository/
│   │   └── UwbRepository.kt      抽象介面
│   └── usecase/
│       ├── CheckUwbCapabilityUseCase.kt
│       ├── GetLocalAddressUseCase.kt
│       └── StartRangingUseCase.kt
│
├── data/                         ← 唯一接觸 androidx.core.uwb.* 的層
│   ├── uwb/
│   │   ├── UwbManagerWrapper.kt  封裝 UwbManager
│   │   └── RangingResultMapper.kt RangingResult → RangingState
│   └── repository/
│       └── UwbRepositoryImpl.kt  UwbRepository 實作（含 Scope 快取）
│
└── presentation/
    ├── MainActivity.kt
    ├── theme/Theme.kt
    ├── navigation/
    │   ├── Screen.kt             路由定義
    │   └── AppNavGraph.kt        NavHost 組合
    ├── screen/
    │   ├── capability/           Screen 1：UWB 能力檢查
    │   ├── roleselect/           Screen 2：角色選擇
    │   ├── oob/                  Screen 3：OOB 參數交換
    │   └── ranging/              Screen 4：測距結果
    └── component/
        ├── PermissionHandler.kt  可重用的權限申請元件
        └── UwbStatusBadge.kt     顏色狀態徽章
```

---

## 授權 / License

```
Copyright 2026

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

---

*本專案為學習用途。UWB 相關硬體存取需在支援裝置上測試。*  
*This project is for educational purposes. UWB hardware access must be tested on supported devices.*
