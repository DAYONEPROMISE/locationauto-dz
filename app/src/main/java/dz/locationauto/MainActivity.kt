package dz.locationauto

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dz.locationauto.data.DatabaseProvider
import dz.locationauto.ui.AppRoot
import dz.locationauto.ui.LocationAutoTheme
import dz.locationauto.ui.MainViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = DatabaseProvider.get(this)
        val vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MainViewModel(db) as T
        })[MainViewModel::class.java]
        setContent {
            var dark by remember { mutableStateOf(false) }
            LocationAutoTheme(darkTheme = dark) {
                Surface(Modifier.fillMaxSize().safeDrawingPadding()) { AppRoot(vm, dark = dark, onToggleDark = { dark = !dark }) }
            }
        }
    }
}
