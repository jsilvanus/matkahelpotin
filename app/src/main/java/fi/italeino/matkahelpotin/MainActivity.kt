package fi.italeino.matkahelpotin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.italeino.matkahelpotin.ui.SettingsScreen
import fi.italeino.matkahelpotin.ui.SettingsViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val application = application as MatkahelpotinApplication
        setContent {
            MaterialTheme {
                Surface { SettingsScreen(viewModel(factory = SettingsViewModel.factory(application))) }
            }
        }
    }
}
