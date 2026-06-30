package com.questdday.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.questdday.ui.ViewModelFactory
import com.questdday.ui.screen.create.CreateQuestScreen
import com.questdday.ui.screen.create.CreateQuestViewModel
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
        composable(
            route = "create_quest?parentId={parentId}&parentEndDate={parentEndDate}&parentTitle={parentTitle}",
            arguments = listOf(
                navArgument("parentId") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("parentEndDate") {
                    type = NavType.StringType
                    nullable = true
                },
                navArgument("parentTitle") {
                    type = NavType.StringType
                    nullable = true
                }
            )
        ) { backStackEntry ->
            val context = LocalContext.current
            val application = context.applicationContext as android.app.Application
            val factory = ViewModelFactory(application)
            val viewModel: CreateQuestViewModel = viewModel(factory = factory)

            val parentId = backStackEntry.arguments?.getString("parentId")?.toLongOrNull()
            val parentEndDate = backStackEntry.arguments?.getString("parentEndDate")
            val parentTitle = backStackEntry.arguments?.getString("parentTitle")
            if (parentId != null) {
                viewModel.setParentQuestId(parentId, parentEndDate, parentTitle)
            }

            CreateQuestScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
