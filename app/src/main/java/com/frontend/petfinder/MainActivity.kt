package com.frontend.petfinder

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.frontend.petfinder.core.navigation.PetFinderNavGraph
import com.frontend.petfinder.core.theme.PetFinderTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PetFinderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Inicializamos el controlador de navegación oficial
                    val navController = rememberNavController()

                    // Llamamos a nuestro enrutador central
                    PetFinderNavGraph(navController = navController)
                }
            }
        }
    }
}