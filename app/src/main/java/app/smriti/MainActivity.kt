package app.smriti

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import app.smriti.ui.GameViewModel
import app.smriti.ui.SmritiRoot

class MainActivity : ComponentActivity() {

    private val graph: AppGraph get() = (application as SmritiApp).graph

    private val viewModel: GameViewModel by viewModels { factory(graph) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            // Pause music and stop speech when SMRITI leaves the screen (not on rotation).
            if (event == Lifecycle.Event.ON_STOP && !isChangingConfigurations) viewModel.onAppBackgrounded()
        })
        setContent {
            SmritiRoot(viewModel, graph.layouts, graph.mediaStorage)
        }
    }

    companion object {
        fun factory(graph: AppGraph): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                GameViewModel(graph.save, graph.createVoice(), graph.createRadioPlayer(), graph.strings, graph.mediaImporter, graph.layouts)
            }
        }
    }
}
