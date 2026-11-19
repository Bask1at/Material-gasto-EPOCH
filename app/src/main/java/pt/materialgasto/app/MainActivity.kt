package pt.materialgasto.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import pt.materialgasto.app.ui.navigation.MaterialGastoNavGraph
import pt.materialgasto.app.ui.theme.MaterialGastoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as MaterialGastoApp
        setContent {
            MaterialGastoTheme {
                MaterialGastoNavGraph(repository = app.repository)
            }
        }
    }
}
