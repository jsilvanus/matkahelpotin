package fi.italeino.matkahelpotin

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.*
import androidx.lifecycle.viewmodel.compose.viewModel
import fi.italeino.matkahelpotin.ui.CommuteScreen
import fi.italeino.matkahelpotin.ui.CommuteViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MatkahelpotinApplication
        setContent {
            MaterialTheme {
                Surface {
                    CommuteScreen(viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        @Suppress("UNCHECKED_CAST")
                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                            CommuteViewModel(app.commuteProfileRepository, app.commuteRecordRepository) as T
                    }))
                }
            }
        }
    }
}
