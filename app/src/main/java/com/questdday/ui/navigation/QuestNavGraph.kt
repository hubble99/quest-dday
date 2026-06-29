package com.questdday.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.questdday.ui.ViewModelFactory
import com.questdday.ui.screen.create.CreateQuestScreen
import com.questdday.ui.screen.masterplan.MasterPlanScreen
import com.questdday.ui.screen.settings.SettingsScreen
import com.questdday.ui.screen.today.TodayScreen
import com.questdday.ui.screen.today.TodayQuestsViewModel

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
            val context = LocalContext.current
            val application = context.applicationContext as android.app.Application
            val factory = ViewModelFactory(application)
            val viewModel: TodayQuestsViewModel = viewModel(factory = factory)
            
            TodayScreen(
                viewModel = viewModel,
                onNavigateToCreateQuest = {
                    navController.navigate(Screen.CreateQuest.route)
                }
            )
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
