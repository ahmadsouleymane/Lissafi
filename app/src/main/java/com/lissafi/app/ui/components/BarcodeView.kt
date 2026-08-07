package com.lissafi.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lissafi.app.ui.theme.LissafiWhite

/**
 * Vrai code-barres RECTANGULAIRE (et non carré) dessiné pour un produit.
 * Génère une séquence de barres déterministe (style Code-128) à partir du code,
 * avec zone de silence, barres de garde, et le numéro lisible dessous.
 */
@Composable
fun BarcodeView(
    barcode: String,
    modifier: Modifier = Modifier,
    barHeight: Int = 64
) {
    if (barcode.isBlank()) return
    val modules = remember(barcode) { generateModules(barcode) }

    Column(
        modifier = modifier
            .background(LissafiWhite, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Canvas(
            modifier = Modifier
                .width(220.dp)
                .height(barHeight.dp)
        ) {
            val barW = size.width / modules.size
            for (i in modules.indices) {
                if (modules[i]) {
                    drawRect(
                        color = Color.Black,
                        topLeft = Offset(i * barW, 0f),
                        size = Size(barW, size.height)
                    )
                }
            }
        }
        Text(
            text = barcode,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = Color.Black,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Génère une séquence de modules barre/blanc déterministe (95 modules comme un
 * Code-128), avec les barres de garde 101 en début et fin.
 */
private fun generateModules(barcode: String): List<Boolean> {
    var state = barcode.hashCode().let { if (it == 0) 0x12345678 else it }
    fun nextBit(): Boolean {
        state = state * 1664525 + 1013904223
        return ((state ushr 31) and 1) == 1
    }
    val modules = BooleanArray(95)
    // Barres de garde (101)
    modules[0] = true
    modules[1] = false
    modules[2] = true
    for (i in 3 until 92) modules[i] = nextBit()
    // Barres de garde de fin (101)
    modules[92] = true
    modules[93] = false
    modules[94] = true
    return modules.toList()
}
