package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.KurdPlayApp
import com.example.ui.theme.KurdPlayTheme
import com.example.ui.viewmodel.KurdPlayViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: KurdPlayViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      KurdPlayTheme {
        KurdPlayApp(viewModel = viewModel)
      }
    }
  }
}

