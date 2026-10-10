package dz.locationauto.data

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/** Offline bilingual (FR/AR labels) rental-contract PDF generator. No network needed. */
object ContractPdfGenerator {

    fun generate(
        context: Context,
        contract: Contract,
        vehicle: Vehicle,
        client: Client,
        agencyName: String = "LocationAutoDZ"
    ): File {
        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 pts
        val page = doc.startPage(pageInfo)
        val c = page.canvas
        val title = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val head = Paint().apply { textSize = 14f; isFakeBoldText = true }
        val body = Paint().apply { textSize = 12f }
        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
        var y = 60f
        fun line(p: Paint, s: String) { c.drawText(s, 40f, y, p); y += 24f }

        line(title, "$agencyName — Contrat de location N ${contract.id}")
        line(body, "Vehicle/Vehicule: ${vehicle.brand} ${vehicle.model} (${vehicle.plate})")
        line(body, "Client: ${client.fullName} — Tel: ${client.phone}")
        line(body, "Permis: ${client.licenseNumber}  NIN: ${client.nationalId}")
        line(body, "Periode: ${fmt.format(Date(contract.startDateEpoch))} -> ${fmt.format(Date(contract.endDateEpoch))}  (${contract.days()} jour(s))")
        line(body, "Tarif: ${contract.dailyRateDzd} DZD/jour  Caution: ${contract.depositDzd} DZD")
        line(body, "Depart: ${contract.startKm} km, carburant ${contract.startFuelPercent}%")
        y += 8f
        line(head, "Retour / Return")
        line(body, "Arrivee: ${contract.endKm?.let { "$it km" } ?: "—"}  carburant: ${contract.endFuelPercent?.let { "$it%" } ?: "—"}")
        line(body, "Degats: ${contract.damageNotes.ifBlank { "—" }}")
        line(body, "Frais supp: ${contract.extraChargesDzd} DZD")
        y += 8f
        line(head, "Total: ${contract.totalDzd()} DZD / \u0627\u0644\u0645\u062c\u0645\u0648\u0639: ${contract.totalDzd()} \u062f\u062c")
        y += 24f
        line(body, "Signature agence: ....................   Signature client: ....................")

        doc.finishPage(page)
        val dir = File(context.getExternalFilesDir(null), "contracts").apply { mkdirs() }
        val file = File(dir, "contrat-${contract.id}.pdf")
        file.outputStream().use { doc.writeTo(it) }
        doc.close()
        return file
    }
}
