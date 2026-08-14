package com.lissafi.app.service

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import com.lissafi.app.data.entity.SaleItem
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

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
        val isCredit: Boolean,
        val footerMessage: String = ""
    )

    data class ReceiptItem(
        val name: String,
        val quantity: Double,
        val price: Int
    )

    /**
     * Format du reçu — 32 colonnes (imprimante thermique 58 mm), refondu pour un
     * rendu propre et aligné. N'utilise QUE des caractères Latin-1 sûrs ("=", "-",
     * accents français) : aucun caractère Unicode rare qui s'imprimerait mal ou
     * décalerait les colonnes (les accents é/è/à/ç sont codés sur un octet).
     */
    fun formatReceipt(data: ReceiptData): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.FRENCH)
        val sb = StringBuilder()

        // ── En-tête : boutique centrée ──
        sb.appendLine(sep('='))
        sb.appendLine(center(data.shopName.ifBlank { "LISSAFI" }))
        if (data.shopPhone.isNotBlank()) sb.appendLine(center(data.shopPhone))
        sb.appendLine(center(sdf.format(Date(data.date))))
        sb.appendLine(sep('-'))

        // ── Tableau des articles ── largeurs 4+1+14+1+12 = 32 = RECEIPT_WIDTH
        // pile poil : un caractère de plus fait déborder la ligne physique et
        // l'imprimante coupe le mot au milieu ("TOTAL" -> "TOTA"/"L").
        sb.appendLine("${"Qté".padStart(4)} ${"ARTICLE".padEnd(14)} ${"TOTAL".padStart(12)}")
        sb.appendLine(sep('-'))
        for (item in data.items) {
            val name = truncate(item.name, 14)
            val qty = qtyText(item.quantity).padStart(4)
            val total = grouped((item.price * item.quantity).roundToInt())
            sb.appendLine("$qty ${name.padEnd(14)} ${total.padStart(12)}")
        }
        sb.appendLine(sep('-'))

        // ── Totaux ──
        if (data.isCredit) {
            sb.appendLine(center("VENTE À CRÉDIT"))
            sb.appendLine(sep('-'))
        }
        sb.appendLine(kvRow("TOTAL", FormatUtils.formatFCFA(data.total)))
        if (!data.isCredit) {
            sb.appendLine(kvRow("PAYÉ", FormatUtils.formatFCFA(data.amountPaid)))
            sb.appendLine(kvRow("MONNAIE", FormatUtils.formatFCFA(data.changeGiven)))
        }

        // ── Pied de page ──
        sb.appendLine(sep('='))
        sb.appendLine(center(data.footerMessage.ifBlank { "Merci pour votre achat !" }))
        sb.appendLine(center("Lissafi - Ton commerce, maîtrise."))
        sb.appendLine(sep('='))
        sb.appendLine()

        return sb.toString()
    }

    private const val RECEIPT_WIDTH = 32

    private fun sep(c: Char = '=') = c.toString().repeat(RECEIPT_WIDTH)

    /** Centre un texte sur la largeur du reçu (remplit gauche/droite). */
    private fun center(text: String, width: Int = RECEIPT_WIDTH): String {
        val t = if (text.length >= width) text.take(width) else text
        val pad = width - t.length
        val left = pad / 2
        return " ".repeat(left) + t + " ".repeat(pad - left)
    }

    /** Ligne label → valeur alignée à droite sur toute la largeur. */
    private fun kvRow(label: String, value: String, width: Int = RECEIPT_WIDTH): String {
        val gap = maxOf(1, width - label.length - value.length)
        return label + " ".repeat(gap) + value
    }

    private fun truncate(text: String, n: Int): String =
        if (text.length > n) text.take(n - 1) + ".." else text

    /** Quantité : "1" si entière, "1,5" sinon. */
    private fun qtyText(q: Double): String =
        if (q % 1.0 == 0.0) q.toInt().toString() else q.toString().replace('.', ',')

    /** Nombre groupé (espace ASCII) sans devise — pour les lignes du tableau. */
    private fun grouped(amount: Int): String {
        val sign = if (amount < 0) "-" else ""
        return sign + kotlin.math.abs(amount).toString().reversed().chunked(3).joinToString(" ").reversed()
    }

    private val ACCENT_TO_ASCII = mapOf(
        'é' to 'e', 'è' to 'e', 'ê' to 'e', 'ë' to 'e',
        'à' to 'a', 'â' to 'a', 'ä' to 'a',
        'î' to 'i', 'ï' to 'i',
        'ô' to 'o', 'ö' to 'o',
        'ù' to 'u', 'û' to 'u', 'ü' to 'u',
        'ç' to 'c',
        'É' to 'E', 'È' to 'E', 'Ê' to 'E', 'Ë' to 'E',
        'À' to 'A', 'Â' to 'A', 'Ä' to 'A',
        'Î' to 'I', 'Ï' to 'I',
        'Ô' to 'O', 'Ö' to 'O',
        'Ù' to 'U', 'Û' to 'U', 'Ü' to 'U',
        'Ç' to 'C'
    )

    /**
     * Translitère les accents en ASCII pur. De nombreuses imprimantes ESC/POS
     * bon marché (clones chinois génériques du POS-58, très répandues) n'ont
     * pas la table Latin-1 attendue en table par défaut : un octet Latin-1
     * pour "é" ou "î" s'affiche comme un caractère illisible et peut même
     * décaler les colonnes si l'imprimante l'interprète comme le premier
     * octet d'une séquence multi-octets. L'ASCII pur (0x00-0x7F) s'imprime
     * correctement quelle que soit la table de caractères de l'imprimante.
     */
    private fun toPrinterSafeAscii(text: String): String =
        text.map { ACCENT_TO_ASCII[it] ?: it }.joinToString("")

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
            } else {
                android.widget.Toast.makeText(
                    context,
                    "WhatsApp n'est pas installé sur cet appareil.",
                    android.widget.Toast.LENGTH_LONG
                ).show()
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

                // cancelDiscovery exige BLUETOOTH_SCAN sur Android 12+ ; s'il manque,
                // on l'ignore — pas indispensable pour une connexion SPP sortante.
                try {
                    @Suppress("MissingPermission")
                    adapter.cancelDiscovery()
                } catch (_: Exception) {}

                socket.connect()
                outputStream = socket.outputStream

                // ESC/POS commands for basic formatting
                val esc = 0x1B.toByte()
                val init = byteArrayOf(esc, '@'.code.toByte()) // Initialize printer
                val alignLeft = byteArrayOf(esc, 'a'.code.toByte(), 0x00) // Left align
                val cutPaper = byteArrayOf(0x1D.toByte(), 'V'.code.toByte(), 0x01) // Cut paper

                outputStream.write(init)

                // ASCII pur (accents translitérés) : voir toPrinterSafeAscii — le
                // Latin-1 seul ne suffit pas, beaucoup de clones ESC/POS bon
                // marché n'ont pas cette table par défaut et affichent un
                // caractère illisible à la place des accents.
                val lines = receiptText.split("\n")
                for (line in lines) {
                    outputStream.write(alignLeft)
                    outputStream.write("${toPrinterSafeAscii(line)}\n".toByteArray(Charsets.US_ASCII))
                }

                outputStream.write(cutPaper)
                outputStream.flush()

                postResult(onResult, true, "Reçu imprimé avec succès")
            } catch (e: Exception) {
                postResult(onResult, false, "Erreur d'impression : ${e.message ?: "inconnue"}")
            } finally {
                try { outputStream?.close() } catch (_: Exception) {}
                try { socket?.close() } catch (_: Exception) {}
            }
        }.start()
    }

    /** Renvoie le résultat d'impression sur le thread principal (état Compose). */
    private fun postResult(onResult: (Boolean, String) -> Unit, ok: Boolean, msg: String) {
        Handler(Looper.getMainLooper()).post { onResult(ok, msg) }
    }
}
