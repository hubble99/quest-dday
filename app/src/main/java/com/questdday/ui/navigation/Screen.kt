package com.questdday.ui.navigation

sealed class Screen(val route: String) {
    object Today : Screen("today")
    object MasterPlan : Screen("master_plan")
    object CreateQuest : Screen("create_quest")
    object Settings : Screen("settings")
}
