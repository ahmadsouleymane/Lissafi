package com.lissafi.app.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import com.lissafi.app.data.entity.SaleItem
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Service de génération et d'impression de reçus.
 *
 * Supporte :
 * - Génération texte formaté pour ticket
 * - Impression Bluetooth (ESC/POS simplifié)
 * - Partage WhatsApp
 */
object ReceiptService {

    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    // ==================== FORMAT RECU ====================

    data class ReceiptData(
        val shopName: String,
        val shopPhone: String,
        val date: Long,
        val items: List<ReceiptItem>,
        val total: Int,
        val amountPaid: Int,
        val changeGiven: Int,
        val isCredit: Boolean
    )

    data class ReceiptItem(
        val name: String,
        val quantity: Double,
        val price: Int
    )

    fun formatReceipt(data: ReceiptData): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
        val sb = StringBuilder()

        sb.appendLine("======================================")
        sb.appendLine("      ${data.shopName.ifBlank { "LISSAFI" }}")
        sb.appendLine("      ${data.shopPhone.ifBlank { "" }}")
        sb.appendLine("--------------------------------------")
        sb.appendLine("      ${sdf.format(Date(data.date))}")
        sb.appendLine("--------------------------------------")
        sb.appendLine("QTÉ   ARTICLE           PRIX   TOTAL")
        sb.appendLine("--------------------------------------")

        for (item in data.items) {
            val name = if (item.name.length > 16) item.name.take(14) + ".." else item.name.padEnd(16)
            val qty = item.quantity.toInt().toString().padStart(3)
            val price = FormatUtils.formatFCFA(item.price).padEnd(7)
            val lineTotal = FormatUtils.formatFCFA((item.price * item.quantity).toInt())
            sb.appendLine("$qty  $name $price $lineTotal")
        }

        sb.appendLine("--------------------------------------")
        sb.appendLine("TOTAL :           ${FormatUtils.formatFCFA(data.total)}")

        if (data.isCredit) {
            sb.appendLine("TYPE :            VENTE À CRÉDIT")
        } else {
            sb.appendLine("PAYÉ :            ${FormatUtils.formatFCFA(data.amountPaid)}")
            sb.appendLine("MONNAIE :         ${FormatUtils.formatFCFA(data.changeGiven)}")
        }

        sb.appendLine("======================================")
        sb.appendLine("      Merci pour votre achat !")
        sb.appendLine("      Lissafi — Ton commerce, maîtrisé.")
        sb.appendLine("======================================")
        sb.appendLine()

        return sb.toString()
    }

    // ==================== PARTAGE WHATSAPP ====================

    fun shareViaWhatsApp(context: Context, receiptText: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, receiptText)
                setPackage("com.whatsapp")
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
            }
        } catch (_: Exception) {}
    }

    fun shareText(context: Context, receiptText: String) {
        try {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, receiptText)
            }
            context.startActivity(Intent.createChooser(intent, "Partager le reçu"))
        } catch (_: Exception) {}
    }

    // ==================== IMPRESSION BLUETOOTH ====================

    data class PrinterDevice(
        val name: String,
        val address: String
    )

    fun getPairedPrinters(): List<PrinterDevice> {
        return try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return emptyList()
            @Suppress("MissingPermission")
            adapter.bondedDevices.map { PrinterDevice(it.name ?: "Imprimante", it.address) }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun printReceipt(address: String, receiptText: String, onResult: (Boolean, String) -> Unit) {
        Thread {
            var socket: BluetoothSocket? = null
            var outputStream: OutputStream? = null
            try {
                val adapter = BluetoothAdapter.getDefaultAdapter()
                    ?: throw Exception("Bluetooth non disponible")

                val device: BluetoothDevice = adapter.getRemoteDevice(address)
                @Suppress("MissingPermission")
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)

                @Suppress("MissingPermission")
                adapter.cancelDiscovery()

                socket.connect()
                outputStream = socket.outputStream

                // ESC/POS commands for basic formatting
                val esc = 0x1B.toByte()
                val init = byteArrayOf(esc, '@'.code.toByte()) // Initialize printer
                val alignCenter = byteArrayOf(esc, 'a'.code.toByte(), 0x01) // Center align
                val alignLeft = byteArrayOf(esc, 'a'.code.toByte(), 0x00) // Left align
                val boldOn = byteArrayOf(esc, 'E'.code.toByte(), 0x01) // Bold on
                val boldOff = byteArrayOf(esc, 'E'.code.toByte(), 0x00) // Bold off
                val cutPaper = byteArrayOf(0x1D.toByte(), 'V'.code.toByte(), 0x01) // Cut paper

                outputStream.write(init)
                outputStream.write(alignCenter)
                outputStream.write(boldOn)
                outputStream.write("LISSAFI\n\n".toByteArray(Charsets.UTF_8))
                outputStream.write(boldOff)

                // Print the receipt text
                val lines = receiptText.split("\n")
                for (line in lines) {
                    outputStream.write(alignLeft)
                    outputStream.write("$line\n".toByteArray(Charsets.UTF_8))
                }

                outputStream.write(cutPaper)
                outputStream.flush()

                onResult(true, "Reçu imprimé avec succès")
            } catch (e: Exception) {
                onResult(false, "Erreur d'impression : ${e.message ?: "inconnue"}")
            } finally {
                try { outputStream?.close() } catch (_: Exception) {}
                try { socket?.close() } catch (_: Exception) {}
            }
        }.start()
    }
}
