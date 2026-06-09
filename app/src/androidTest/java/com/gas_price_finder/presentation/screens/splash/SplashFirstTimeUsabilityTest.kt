package com.gas_price_finder.presentation.screens.splash

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

/**
 * Pruebas de usabilidad para el flujo de primera configuración de perfil (SplashScreen).
 *
 * Escenario: el usuario abre la aplicación por primera vez y debe crear un perfil
 * antes de acceder al contenido principal.
 */
class SplashFirstTimeUsabilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun firstTimeSetup_showsWelcomeForm_andCreatesUserOnClick() {
        var created = false

        composeTestRule.setContent {
            MaterialTheme {
                FirstTimeSetup(
                    state = SplashState(nombre = ""),
                    onUpdateNombre = {},
                    onUpdateRadio = {},
                    onUpdateConsumo = {},
                    onUpdateCapacidadDeposito = {},
                    onUpdateTema = {},
                    onCreateUser = { created = true }
                )
            }
        }

        // Verificar que se muestra el texto de bienvenida
        composeTestRule.onNodeWithText("Bienvenido a GasPrice Finder").assertIsDisplayed()

        // Verificar que existe el campo de nombre
        composeTestRule.onNodeWithText("Tu nombre *").assertIsDisplayed()

        // Verificar que el botón de comenzar está visible
        composeTestRule.onNodeWithText("Comenzar").assertIsDisplayed()

        // Simular pulsación sobre el botón
        composeTestRule.onNodeWithText("Comenzar").performClick()

        // El callback de creación debe haberse invocado
        assertTrue(created)
    }

    @Test
    fun firstTimeSetup_allowsTypingName() {
        var typedName = ""

        composeTestRule.setContent {
            MaterialTheme {
                FirstTimeSetup(
                    state = SplashState(nombre = typedName),
                    onUpdateNombre = { typedName = it },
                    onUpdateRadio = {},
                    onUpdateConsumo = {},
                    onUpdateCapacidadDeposito = {},
                    onUpdateTema = {},
                    onCreateUser = {}
                )
            }
        }

        // Escribir en el campo de nombre
        composeTestRule.onNodeWithText("Tu nombre *").performTextInput("Emilio")
        composeTestRule.waitForIdle()

        // Verificar que el callback recibió el texto
        assertTrue(typedName == "Emilio")
    }
}
