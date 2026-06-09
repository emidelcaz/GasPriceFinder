package com.gas_price_finder.presentation.screens.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.assertTrue

/**
 * Pruebas de usabilidad para la pantalla de selección de combustible
 * y el diálogo de confirmación de AdBlue.
 *
 * Escenario: el usuario cambia su combustible preferido a uno de gasóleo
 * y el sistema solicita confirmación sobre el uso de AdBlue.
 */
class SettingsFuelUsabilityTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun combustibleListItem_showsSelectionAndHandlesClick() {
        var clicked = false

        composeTestRule.setContent {
            MaterialTheme {
                CombustibleListItem(
                    nombre = "Gasoleo A",
                    isSelected = true,
                    onClick = { clicked = true }
                )
            }
        }

        // Verificar que el nombre del combustible se muestra
        composeTestRule.onNodeWithText("Gasoleo A").assertIsDisplayed()

        // Verificar que aparece el indicador de selección
        composeTestRule.onNodeWithContentDescription("Seleccionado").assertIsDisplayed()

        // Simular pulsación
        composeTestRule.onNodeWithText("Gasoleo A").performClick()

        assertTrue(clicked)
    }

    @Test
    fun adBlueConfirmDialog_showsQuestionAndHandlesYesResponse() {
        var result: Boolean? = null

        composeTestRule.setContent {
            MaterialTheme {
                AdBlueConfirmDialog(
                    onConfirm = { result = it },
                    onDismiss = { result = false }
                )
            }
        }

        // Verificar título y descripción del diálogo
        composeTestRule.onNodeWithText("AdBlue").assertIsDisplayed()
        composeTestRule.onNodeWithText(
            "¿Tu vehículo utiliza AdBlue? Se añadirá la configuración del depósito de AdBlue más abajo."
        ).assertIsDisplayed()

        // Pulsar "Sí"
        composeTestRule.onNodeWithText("Sí").performClick()

        assertTrue(result == true)
    }

    @Test
    fun adBlueConfirmDialog_handlesNoResponse() {
        var result: Boolean? = null

        composeTestRule.setContent {
            MaterialTheme {
                AdBlueConfirmDialog(
                    onConfirm = { result = it },
                    onDismiss = { result = false }
                )
            }
        }

        // Pulsar "No"
        composeTestRule.onNodeWithText("No").performClick()

        assertTrue(result == false)
    }
}
