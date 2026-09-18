package fi.italeino.matkahelpotin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.italeino.matkahelpotin.ui.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MatkahelpotinApplication
        setContent {
            MaterialTheme {
                var selectedTab by remember { mutableIntStateOf(0) }
                Scaffold(
                    bottomBar = {
                        NavigationBar {
                            NavigationBarItem(selectedTab == 0, { selectedTab = 0 }, icon = {}, label = { Text("Commute") })
                            NavigationBarItem(selectedTab == 1, { selectedTab = 1 }, icon = {}, label = { Text("Business") })
                            NavigationBarItem(selectedTab == 2, { selectedTab = 2 }, icon = {}, label = { Text("Settings") })
                            NavigationBarItem(selectedTab == 3, { selectedTab = 3 }, icon = {}, label = { Text("Export") })
                        }
                    },
                ) { padding ->
                    Surface(Modifier.fillMaxSize()) {
                        when (selectedTab) {
                            0 -> CommuteScreen(viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                @Suppress("UNCHECKED_CAST")
                                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                                    CommuteViewModel(app.commuteProfileRepository, app.commuteRecordRepository, app.mileagePolicyRepository) as T
                            }))
                            1 -> BusinessScreen(viewModel(factory = BusinessViewModel.factory(app)))
                            2 -> SettingsScreen(viewModel(factory = SettingsViewModel.factory(app)))
                            else -> ExportScreen(\n                                viewModel(factory = fi.italeino.matkahelpotin.export.ExportViewModel.factory(app)),\n                                viewModel(factory = fi.italeino.matkahelpotin.export.ImportViewModel.factory(app))\n                            )
                        }
                    }
                }
            }
        }
    }
}
