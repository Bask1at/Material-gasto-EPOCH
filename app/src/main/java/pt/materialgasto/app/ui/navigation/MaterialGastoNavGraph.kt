package pt.materialgasto.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import pt.materialgasto.app.data.repository.MaterialGastoRepository
import pt.materialgasto.app.camera.BarcodeScannerScreen
import pt.materialgasto.app.camera.PhotoCaptureScreen
import pt.materialgasto.app.ui.home.HomeScreen
import pt.materialgasto.app.ui.newrecord.NewRecordScreen
import pt.materialgasto.app.ui.newrecord.NewRecordViewModel
import pt.materialgasto.app.ui.records.RecordDetailScreen
import pt.materialgasto.app.ui.records.SavedRecordsScreen
import pt.materialgasto.app.ui.records.SavedRecordsViewModel
import pt.materialgasto.app.ui.settings.SettingsScreen
import pt.materialgasto.app.ui.settings.SettingsViewModel

sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object NewRecord : Screen("new_record")
    data object BarcodeScanner : Screen("barcode_scanner")
    data object PhotoCapture : Screen("photo_capture")
    data object SavedRecords : Screen("saved_records")
    data object RecordDetail : Screen("record_detail/{recordId}") {
        fun createRoute(recordId: Long): String = "record_detail/$recordId"
    }
    data object Settings : Screen("settings")
}

@Composable
fun MaterialGastoNavGraph(
    repository: MaterialGastoRepository
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNewRecordClick = { navController.navigate(Screen.NewRecord.route) },
                onSavedRecordsClick = { navController.navigate(Screen.SavedRecords.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        composable(Screen.NewRecord.route) { backStackEntry ->
            val newRecordViewModel: NewRecordViewModel = viewModel(
                viewModelStoreOwner = backStackEntry,
                factory = NewRecordViewModel.Factory(repository)
            )
            NewRecordScreen(
                viewModel = newRecordViewModel,
                onOpenBarcodeScanner = { navController.navigate(Screen.BarcodeScanner.route) },
                onOpenPhotoCapture = { navController.navigate(Screen.PhotoCapture.route) },
                onNavigateBack = { navController.popBackStack() },
                onFinishToSavedRecords = {
                    navController.navigate(Screen.SavedRecords.route) {
                        popUpTo(Screen.Home.route)
                    }
                }
            )
        }

        composable(Screen.BarcodeScanner.route) {
            val parentEntry = navController.getBackStackEntry(Screen.NewRecord.route)
            val newRecordViewModel: NewRecordViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = NewRecordViewModel.Factory(repository)
            )
            BarcodeScannerScreen(
                onBarcodeDetected = { code ->
                    newRecordViewModel.onBarcodeScanned(code)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.PhotoCapture.route) {
            val parentEntry = navController.getBackStackEntry(Screen.NewRecord.route)
            val newRecordViewModel: NewRecordViewModel = viewModel(
                viewModelStoreOwner = parentEntry,
                factory = NewRecordViewModel.Factory(repository)
            )
            PhotoCaptureScreen(
                fileStorage = repository.fileStorage,
                onPhotoConfirmed = { photoPath ->
                    newRecordViewModel.onPhotoCaptured(photoPath)
                    navController.popBackStack()
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.SavedRecords.route) {
            val savedRecordsViewModel: SavedRecordsViewModel = viewModel(
                factory = SavedRecordsViewModel.Factory(repository)
            )
            SavedRecordsScreen(
                viewModel = savedRecordsViewModel,
                onRecordClick = { recordId ->
                    navController.navigate(Screen.RecordDetail.createRoute(recordId))
                },
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.RecordDetail.route,
            arguments = listOf(navArgument("recordId") { type = NavType.LongType })
        ) { backStackEntry ->
            val recordId = backStackEntry.arguments?.getLong("recordId") ?: 0L
            val savedRecordsViewModel: SavedRecordsViewModel = viewModel(
                factory = SavedRecordsViewModel.Factory(repository)
            )
            RecordDetailScreen(
                recordId = recordId,
                viewModel = savedRecordsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = SettingsViewModel.Factory(repository)
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
