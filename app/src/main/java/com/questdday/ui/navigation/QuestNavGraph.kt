package com.questdday.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.questdday.ui.screen.create.CreateQuestScreen
import com.questdday.ui.screen.masterplan.MasterPlanScreen
import com.questdday.ui.screen.settings.SettingsScreen
import com.questdday.ui.screen.today.TodayScreen

@Composable
fun QuestNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Today.route,
        modifier = modifier
    ) {
        composable(Screen.Today.route) {
            TodayScreen()
        }
        composable(Screen.MasterPlan.route) {
            MasterPlanScreen()
        }
        composable(Screen.CreateQuest.route) {
            CreateQuestScreen()
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
