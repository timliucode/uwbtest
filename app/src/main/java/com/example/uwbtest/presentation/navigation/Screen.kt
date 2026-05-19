package com.example.uwbtest.presentation.navigation

/**
 * 所有導航路由的集中定義。
 * 使用 sealed class 避免路由字串散落在各處造成打字錯誤。
 */
sealed class Screen(val route: String) {
    /** 步驟 1：檢查裝置 UWB 能力（硬體 + 軟體 + 權限） */
    data object CapabilityCheck : Screen("capability_check")

    /** 步驟 2：選擇本機角色（Controller / Controlee） */
    data object RoleSelect : Screen("role_select")

    /**
     * 步驟 3：OOB 參數交換
     * @param isController  "true" / "false"，由 RoleSelectScreen 傳入
     */
    data object OobExchange : Screen("oob_exchange/{isController}") {
        fun createRoute(isController: Boolean) = "oob_exchange/$isController"
    }

    /** 步驟 4：測距結果顯示 */
    data object Ranging : Screen("ranging")
}
