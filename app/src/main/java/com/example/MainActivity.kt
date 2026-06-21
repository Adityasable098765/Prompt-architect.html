package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.example.data.AppDatabase
import com.example.data.PromptRepository
import com.example.ui.PromptApp
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.PromptViewModel
import com.example.viewmodel.PromptViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize local Room Database and Repository
        val database = AppDatabase.getDatabase(applicationContext)
        val repository = PromptRepository(database.promptDao())
        
        // Instantiate ViewModel
        val factory = PromptViewModelFactory(repository, applicationContext)
        val viewModel = ViewModelProvider(this, factory)[PromptViewModel::class.java]

        setContent {
            MyApplicationTheme(dynamicColor = false) {
                PromptApp(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
