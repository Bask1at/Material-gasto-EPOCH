package pt.materialgasto.app.pdf

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import pt.materialgasto.app.data.local.entity.MaterialItemEntity
import pt.materialgasto.app.data.local.entity.WorkRecordWithMaterials
import pt.materialgasto.app.data.storage.LocalFileStorage
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class PdfGenerator {

    private val pageWidth = 595
    private val pageHeight = 842
    private val margin = 36f
    private val contentWidth = pageWidth - margin * 2

    private val headerHeight = 112f
    private val footerHeight = 32f
    private val contentTop = margin + headerHeight
    private val contentBottom = pageHeight - margin - footerHeight

    // Table column proportions totaling contentWidth (523pt)
    private val colRefWidth = 100f
    private val colDescWidth = 223f
    private val colQtyWidth = 75f
    private val colBarcodeWidth = contentWidth - colRefWidth - colDescWidth - colQtyWidth

    private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D47A1")
        textSize = 15f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val metaLabelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#37474F")
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val metaValuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#111827")
        textSize = 10.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val headerLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        strokeWidth = 1.5f
    }

    private val tableHeaderBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1565C0")
        style = Paint.Style.FILL
    }

    private val tableAltRowBgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#F5F8FC")
        style = Paint.Style.FILL
    }

    private val tableBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#CBD5E1")
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
    }

    private val tableHeaderTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val cellTextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1F2937")
        textSize = 9.5f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private val sectionTitlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0D47A1")
        textSize = 12f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val photoCaptionPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#1E293B")
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    private val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#64748B")
        textSize = 9f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
    }

    private sealed interface PageElement {
        data object TableHeader : PageElement
        data class TableRow(
            val refLayout: StaticLayout,
            val descLayout: StaticLayout,
            val qtyLayout: StaticLayout,
            val barcodeLayout: StaticLayout,
            val rowHeight: Float,
            val isAlternate: Boolean
        ) : PageElement

        data class PhotosSectionHeader(val height: Float) : PageElement
        data class PhotoBlock(
            val captionLayout: StaticLayout,
            val photoFile: File,
            val imageDrawWidth: Float,
            val imageDrawHeight: Float,
            val totalHeight: Float
        ) : PageElement
    }

    fun generate(recordWithMaterials: WorkRecordWithMaterials, outputFile: File) {
        val sortedMaterials = recordWithMaterials.materials.sortedBy { it.sortOrder }
        val pages = buildPagePlan(sortedMaterials)
        val totalPages = pages.size.coerceAtLeast(1)

        val document = PdfDocument()
        try {
            pages.forEachIndexed { index, elements ->
                val pageNumber = index + 1
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                drawPageHeader(canvas, recordWithMaterials)
                drawPageElements(canvas, elements)
                drawPageFooter(canvas, pageNumber, totalPages)

                document.finishPage(page)
            }

            outputFile.parentFile?.mkdirs()
            FileOutputStream(outputFile).use { fos ->
                document.writeTo(fos)
            }
        } finally {
            document.close()
        }
    }

    private fun buildPagePlan(materials: List<MaterialItemEntity>): List<List<PageElement>> {
        val pages = mutableListOf<MutableList<PageElement>>()
        var currentPage = mutableListOf<PageElement>()
        pages.add(currentPage)

        var cursorY = contentTop
        val tableHeaderHeight = 26f

        fun startNewPage(includeTableHeader: Boolean) {
            currentPage = mutableListOf()
            pages.add(currentPage)
            cursorY = contentTop
            if (includeTableHeader) {
                currentPage.add(PageElement.TableHeader)
                cursorY += tableHeaderHeight
            }
        }

        // Start materials table
        currentPage.add(PageElement.TableHeader)
        cursorY += tableHeaderHeight

        val cellPadH = 6f
        val cellPadV = 6f

        materials.forEachIndexed { index, item ->
            val refText = item.referencia.ifBlank { "-" }
            val descText = item.designacao.ifBlank { "-" }
            val qtyText = formatQuantity(item.quantity)
            val barcodeText = item.barcode.ifBlank { "-" }

            val refLayout = createStaticLayout(refText, cellTextPaint, (colRefWidth - cellPadH * 2).toInt())
            val descLayout = createStaticLayout(descText, cellTextPaint, (colDescWidth - cellPadH * 2).toInt())
            val qtyLayout = createStaticLayout(
                qtyText,
                cellTextPaint,
                (colQtyWidth - cellPadH * 2).toInt(),
                Layout.Alignment.ALIGN_OPPOSITE
            )
            val barcodeLayout = createStaticLayout(barcodeText, cellTextPaint, (colBarcodeWidth - cellPadH * 2).toInt())

            val maxTextHeight = maxOf(
                refLayout.height,
                descLayout.height,
                qtyLayout.height,
                barcodeLayout.height
            ).toFloat()
            val rowHeight = (maxTextHeight + cellPadV * 2).coerceAtLeast(24f)

            if (cursorY + rowHeight > contentBottom) {
                startNewPage(includeTableHeader = true)
            }

            currentPage.add(
                PageElement.TableRow(
                    refLayout = refLayout,
                    descLayout = descLayout,
                    qtyLayout = qtyLayout,
                    barcodeLayout = barcodeLayout,
                    rowHeight = rowHeight,
                    isAlternate = index % 2 == 1
                )
            )
            cursorY += rowHeight
        }

        // Photos section (only if at least one material has an existing photo)
        val photoMaterials = materials.mapNotNull { item ->
            val path = item.photoPath
            if (!path.isNullOrBlank()) {
                val file = File(path)
                if (file.exists() && file.length() > 0L) item to file else null
            } else {
                null
            }
        }

        if (photoMaterials.isNotEmpty()) {
            val sectionHeaderHeight = 32f
            val minNeededForHeaderAndFirstPhoto = sectionHeaderHeight + 170f
            if (cursorY + minNeededForHeaderAndFirstPhoto > contentBottom) {
                startNewPage(includeTableHeader = false)
            } else {
                cursorY += 16f
            }

            currentPage.add(PageElement.PhotosSectionHeader(sectionHeaderHeight))
            cursorY += sectionHeaderHeight

            val maxPhotoWidth = contentWidth - 16f
            val maxPhotoHeight = 250f

            for ((item, file) in photoMaterials) {
                val label = buildPhotoLabel(item)
                val captionLayout = createStaticLayout(label, photoCaptionPaint, (contentWidth - 16f).toInt())
                val (imgW, imgH) = measurePhotoDrawSize(file, maxPhotoWidth, maxPhotoHeight)
                val blockHeight = captionLayout.height.toFloat() + 8f + imgH + 20f

                if (cursorY + blockHeight > contentBottom) {
                    startNewPage(includeTableHeader = false)
                }

                currentPage.add(
                    PageElement.PhotoBlock(
                        captionLayout = captionLayout,
                        photoFile = file,
                        imageDrawWidth = imgW,
                        imageDrawHeight = imgH,
                        totalHeight = blockHeight
                    )
                )
                cursorY += blockHeight
            }
        }

        return pages
    }

    private fun drawPageHeader(canvas: Canvas, recordWithMaterials: WorkRecordWithMaterials) {
        val record = recordWithMaterials.record
        var y = margin + 16f
        canvas.drawText("REGISTO DE MATERIAL GASTO EM OBRA", margin, y, titlePaint)

        y += 22f
        canvas.drawText("Número/Nome da Obra:", margin, y, metaLabelPaint)
        canvas.drawText(record.workOrder, margin + 135f, y, metaValuePaint)

        y += 18f
        canvas.drawText("Técnico:", margin, y, metaLabelPaint)
        canvas.drawText(record.technicianName, margin + 55f, y, metaValuePaint)

        canvas.drawText("Data:", margin + 320f, y, metaLabelPaint)
        canvas.drawText(record.dateText, margin + 355f, y, metaValuePaint)

        y += 14f
        canvas.drawLine(margin, y, pageWidth - margin, y, headerLinePaint)
    }

    private fun drawPageElements(canvas: Canvas, elements: List<PageElement>) {
        var y = contentTop
        val tableHeaderHeight = 26f
        val cellPadH = 6f
        val cellPadV = 6f

        for (element in elements) {
            when (element) {
                is PageElement.TableHeader -> {
                    canvas.drawRect(margin, y, pageWidth - margin, y + tableHeaderHeight, tableHeaderBgPaint)
                    drawCellText(canvas, "Referência", margin + cellPadH, y + 7f, tableHeaderTextPaint, colRefWidth - cellPadH * 2)
                    drawCellText(canvas, "Designação", margin + colRefWidth + cellPadH, y + 7f, tableHeaderTextPaint, colDescWidth - cellPadH * 2)
                    drawCellText(
                        canvas,
                        "Qtd.",
                        margin + colRefWidth + colDescWidth + cellPadH,
                        y + 7f,
                        tableHeaderTextPaint,
                        colQtyWidth - cellPadH * 2,
                        Layout.Alignment.ALIGN_OPPOSITE
                    )
                    drawCellText(
                        canvas,
                        "Código de barras",
                        margin + colRefWidth + colDescWidth + colQtyWidth + cellPadH,
                        y + 7f,
                        tableHeaderTextPaint,
                        colBarcodeWidth - cellPadH * 2
                    )
                    y += tableHeaderHeight
                }

                is PageElement.TableRow -> {
                    if (element.isAlternate) {
                        canvas.drawRect(margin, y, pageWidth - margin, y + element.rowHeight, tableAltRowBgPaint)
                    }
                    canvas.drawRect(margin, y, pageWidth - margin, y + element.rowHeight, tableBorderPaint)

                    // Column dividers
                    val x1 = margin + colRefWidth
                    val x2 = x1 + colDescWidth
                    val x3 = x2 + colQtyWidth
                    canvas.drawLine(x1, y, x1, y + element.rowHeight, tableBorderPaint)
                    canvas.drawLine(x2, y, x2, y + element.rowHeight, tableBorderPaint)
                    canvas.drawLine(x3, y, x3, y + element.rowHeight, tableBorderPaint)

                    drawLayout(canvas, element.refLayout, margin + cellPadH, y + cellPadV)
                    drawLayout(canvas, element.descLayout, x1 + cellPadH, y + cellPadV)
                    drawLayout(canvas, element.qtyLayout, x2 + cellPadH, y + cellPadV)
                    drawLayout(canvas, element.barcodeLayout, x3 + cellPadH, y + cellPadV)

                    y += element.rowHeight
                }

                is PageElement.PhotosSectionHeader -> {
                    if (y > contentTop) {
                        y += 16f
                    }
                    canvas.drawText("FOTOGRAFIAS DOS MATERIAIS", margin, y + 18f, sectionTitlePaint)
                    canvas.drawLine(margin, y + 24f, pageWidth - margin, y + 24f, headerLinePaint)
                    y += element.height
                }

                is PageElement.PhotoBlock -> {
                    val cardTop = y
                    val cardBottom = y + element.totalHeight - 10f
                    canvas.drawRect(margin, cardTop, pageWidth - margin, cardBottom, tableBorderPaint)

                    drawLayout(canvas, element.captionLayout, margin + 8f, cardTop + 6f)

                    val imgTop = cardTop + element.captionLayout.height.toFloat() + 12f
                    val imgLeft = margin + (contentWidth - element.imageDrawWidth) / 2f
                    val bitmap = LocalFileStorage.decodeOrientedBitmap(element.photoFile, maxDimension = 1200)
                    if (bitmap != null) {
                        val destRect = RectF(
                            imgLeft,
                            imgTop,
                            imgLeft + element.imageDrawWidth,
                            imgTop + element.imageDrawHeight
                        )
                        canvas.drawBitmap(bitmap, null, destRect, null)
                        bitmap.recycle()
                    }
                    y += element.totalHeight
                }
            }
        }
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, totalPages: Int) {
        val footerY = pageHeight - margin + 10f
        canvas.drawText("Material Gasto", margin, footerY, footerPaint)
        val pageLabel = "Página $pageNumber de $totalPages"
        val labelWidth = footerPaint.measureText(pageLabel)
        canvas.drawText(pageLabel, pageWidth - margin - labelWidth, footerY, footerPaint)
    }

    private fun drawCellText(
        canvas: Canvas,
        text: String,
        x: Float,
        y: Float,
        paint: TextPaint,
        width: Float,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ) {
        val layout = createStaticLayout(text, paint, width.toInt().coerceAtLeast(1), alignment)
        drawLayout(canvas, layout, x, y)
    }

    private fun drawLayout(canvas: Canvas, layout: StaticLayout, x: Float, y: Float) {
        canvas.save()
        canvas.translate(x, y)
        layout.draw(canvas)
        canvas.restore()
    }

    private fun createStaticLayout(
        text: String,
        paint: TextPaint,
        width: Int,
        alignment: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): StaticLayout {
        return StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.coerceAtLeast(1))
            .setAlignment(alignment)
            .setLineSpacing(0f, 1.05f)
            .setIncludePad(false)
            .build()
    }

    private fun measurePhotoDrawSize(
        file: File,
        maxWidth: Float,
        maxHeight: Float
    ): Pair<Float, Float> {
        val bitmap: Bitmap? = LocalFileStorage.decodeOrientedBitmap(file, maxDimension = 800)
        val rawW = bitmap?.width?.toFloat() ?: 400f
        val rawH = bitmap?.height?.toFloat() ?: 300f
        bitmap?.recycle()

        val scale = minOf(maxWidth / rawW, maxHeight / rawH, 1f)
        return (rawW * scale) to (rawH * scale)
    }

    private fun buildPhotoLabel(item: MaterialItemEntity): String {
        val ref = item.referencia.trim()
        val desc = item.designacao.trim()
        return when {
            ref.isNotEmpty() && desc.isNotEmpty() -> "$ref — $desc"
            ref.isNotEmpty() -> ref
            desc.isNotEmpty() -> desc
            else -> "Material"
        }
    }

    companion object {
        fun formatQuantity(value: Double): String {
            val symbols = DecimalFormatSymbols(Locale("pt", "PT")).apply {
                decimalSeparator = ','
            }
            val df = DecimalFormat("#,##0.###", symbols)
            return df.format(value)
        }
    }
}
