package pt.materialgasto.app.share

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {

    fun getUriForFile(context: Context, file: File): Uri {
        val authority = "${context.packageName}.fileprovider"
        return FileProvider.getUriForFile(context, authority, file)
    }

    fun openPdf(context: Context, pdfFile: File) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "Ficheiro PDF não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }
        val uri = getUriForFile(context, pdfFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        runCatching {
            context.startActivity(Intent.createChooser(intent, "Abrir PDF"))
        }.onFailure {
            Toast.makeText(
                context,
                "Não foi encontrada nenhuma aplicação para abrir PDFs.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    fun sharePdfByEmail(
        context: Context,
        pdfFile: File,
        recipientEmail: String,
        workOrder: String,
        technicianName: String,
        dateText: String
    ) {
        if (!pdfFile.exists()) {
            Toast.makeText(context, "Ficheiro PDF não encontrado.", Toast.LENGTH_SHORT).show()
            return
        }

        val uri = getUriForFile(context, pdfFile)
        val subject = "Material Gasto - Obra: $workOrder"
        val body = buildString {
            appendLine("Segue em anexo o registo de material gasto em obra.")
            appendLine()
            appendLine("Obra: $workOrder")
            appendLine("Técnico: $technicianName")
            appendLine("Data: $dateText")
        }

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_EMAIL, arrayOf(recipientEmail.trim()))
            putExtra(Intent.EXTRA_SUBJECT, subject)
            putExtra(Intent.EXTRA_TEXT, body)
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newUri(context.contentResolver, pdfFile.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooser = Intent.createChooser(sendIntent, "Enviar PDF por Email").apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        runCatching {
            context.startActivity(chooser)
        }.onFailure {
            Toast.makeText(
                context,
                "Nenhuma aplicação de email disponível neste dispositivo.",
                Toast.LENGTH_LONG
            ).show()
        }
    }
}
