
package pt.materialgasto.app.excel

import jxl.Workbook
import jxl.WorkbookSettings
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import pt.materialgasto.app.domain.CatalogMatcher
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

object ExcelCatalogParser {

    fun parseAndAnalyze(inputStream: InputStream, fileName: String): ExcelLoadResult {
        val bytes = runCatching { inputStream.readBytes() }.getOrElse {
            return ExcelLoadResult.Error("Não foi possível ler o ficheiro selecionado.")
        }
        if (bytes.isEmpty()) {
            return ExcelLoadResult.Error("O ficheiro selecionado está vazio.")
        }

        val spreadsheet = runCatching {
            when {
                isXlsxMagic(bytes) -> parseXlsx(bytes, fileName)
                isXlsMagic(bytes) -> parseXls(bytes, fileName)
                fileName.lowercase().endsWith(".xlsx") -> parseXlsx(bytes, fileName)
                fileName.lowercase().endsWith(".xls") -> parseXls(bytes, fileName)
                else -> throw IllegalArgumentException(
                    "Formato não suportado. Selecione um ficheiro Excel (.xlsx ou .xls)."
                )
            }
        }.getOrElse { error ->
            return ExcelLoadResult.Error(
                error.message ?: "Erro ao ler o ficheiro Excel. Verifique se não está corrompido."
            )
        }

        if (spreadsheet.rows.isEmpty() || spreadsheet.maxColumns < 2) {
            return ExcelLoadResult.Error(
                "O ficheiro Excel não contém colunas suficientes (são necessárias pelo menos 2 colunas: Referência e Designação)."
            )
        }

        val firstRow = spreadsheet.rows.first()
        val (refCol, descCol) = detectHeaderColumns(firstRow)

        return if (refCol != null && descCol != null && refCol != descCol) {
            val analysis = analyzeColumns(
                spreadsheet = spreadsheet,
                refCol = refCol,
                descCol = descCol,
                hasHeaderRow = true
            )
            ExcelLoadResult.ReadyForReview(analysis)
        } else {
            ExcelLoadResult.NeedsColumnMapping(
                spreadsheet = spreadsheet,
                suggestedRefColumn = refCol,
                suggestedDescColumn = descCol
            )
        }
    }

    fun analyzeColumns(
        spreadsheet: ParsedSpreadsheet,
        refCol: Int,
        descCol: Int,
        hasHeaderRow: Boolean
    ): ExcelImportAnalysis {
        val dataRows = if (hasHeaderRow && spreadsheet.rows.isNotEmpty()) {
            spreadsheet.rows.drop(1)
        } else {
            spreadsheet.rows
        }

        val groupedByRef = linkedMapOf<String, LinkedHashMap<String, Pair<String, String>>>()
        var duplicateCount = 0
        var skippedIncompleteCount = 0

        for (row in dataRows) {
            if (row.all { it.isBlank() }) continue

            val rawRef = row.getOrNull(refCol)?.trim().orEmpty()
            val rawDesc = row.getOrNull(descCol)?.trim().orEmpty()

            if (rawRef.isBlank() && rawDesc.isBlank()) {
                continue
            }
            if (rawRef.isBlank() || rawDesc.isBlank()) {
                skippedIncompleteCount++
                continue
            }

            val normRef = CatalogMatcher.normalize(rawRef)
            val normDesc = CatalogMatcher.normalize(rawDesc)

            val descsForRef = groupedByRef.getOrPut(normRef) { linkedMapOf() }
            if (descsForRef.containsKey(normDesc)) {
                duplicateCount++
            } else {
                descsForRef[normDesc] = rawRef to rawDesc
            }
        }

        val uniqueItems = mutableListOf<Pair<String, String>>()
        val conflicts = mutableListOf<ReferenceConflict>()

        for ((normRef, descMap) in groupedByRef) {
            if (descMap.size == 1) {
                uniqueItems.add(descMap.values.first())
            } else {
                val entries = descMap.values.toList()
                conflicts.add(
                    ReferenceConflict(
                        normalizedReferencia = normRef,
                        referenciaDisplay = entries.first().first,
                        designations = entries.map { it.second }
                    )
                )
            }
        }

        return ExcelImportAnalysis(
            fileName = spreadsheet.fileName,
            uniqueItems = uniqueItems,
            conflicts = conflicts,
            duplicateCount = duplicateCount,
            skippedIncompleteCount = skippedIncompleteCount
        )
    }

    fun detectHeaderColumns(headerRow: List<String>): Pair<Int?, Int?> {
        var refCol: Int? = null
        var descCol: Int? = null

        headerRow.forEachIndexed { index, cell ->
            val norm = CatalogMatcher.normalize(cell)
            if (norm.isBlank()) return@forEachIndexed

            val isRef = norm == "referencia" ||
                norm == "ref" ||
                norm == "ref." ||
                norm == "reference" ||
                norm == "codigo" ||
                norm == "cod" ||
                norm == "cod." ||
                norm == "codigo artigo" ||
                norm == "referencia artigo"

            val isDesc = norm == "designacao" ||
                norm == "descricao" ||
                norm == "description" ||
                norm == "artigo" ||
                norm == "material" ||
                norm == "nome" ||
                norm == "designacao artigo" ||
                norm == "descricao artigo"

            if (isRef && refCol == null) refCol = index
            if (isDesc && descCol == null) descCol = index
        }

        return refCol to descCol
    }

    private fun isXlsxMagic(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 0x50.toByte() &&
            bytes[1] == 0x4B.toByte()
    }

    private fun isXlsMagic(bytes: ByteArray): Boolean {
        return bytes.size >= 4 &&
            bytes[0] == 0xD0.toByte() &&
            bytes[1] == 0xCF.toByte() &&
            bytes[2] == 0x11.toByte() &&
            bytes[3] == 0xE0.toByte()
    }

    private fun parseXls(bytes: ByteArray, fileName: String): ParsedSpreadsheet {
        val settings = WorkbookSettings().apply {
            encoding = "Cp1252"
            setSuppressWarnings(true)
        }
        val workbook = Workbook.getWorkbook(ByteArrayInputStream(bytes), settings)
        return try {
            val sheet = workbook.sheets.firstOrNull()
                ?: throw IllegalStateException("O ficheiro Excel não contém nenhuma folha de cálculo.")
            val rows = mutableListOf<List<String>>()
            for (r in 0 until sheet.rows) {
                val rowList = mutableListOf<String>()
                for (c in 0 until sheet.columns) {
                    rowList.add(sheet.getCell(c, r).contents?.trim().orEmpty())
                }
                if (rowList.any { it.isNotBlank() }) {
                    rows.add(rowList)
                }
            }
            ParsedSpreadsheet(fileName = fileName, rows = rows)
        } finally {
            workbook.close()
        }
    }

    internal fun parseXlsx(bytes: ByteArray, fileName: String): ParsedSpreadsheet {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entries[entry.name.lowercase()] = zis.readBytes()
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }

        val sharedStrings = entries["xl/sharedstrings.xml"]?.let {
            parseSharedStrings(it)
        } ?: emptyList()

        val firstSheetEntryName = resolveFirstWorksheetEntry(entries)
            ?: throw IllegalStateException("Não foi encontrada nenhuma folha de cálculo no ficheiro .xlsx.")

        val sheetBytes = entries[firstSheetEntryName]
            ?: throw IllegalStateException("Não foi possível abrir a folha principal do ficheiro .xlsx.")

        val rows = parseWorksheetXml(sheetBytes, sharedStrings)
        return ParsedSpreadsheet(fileName = fileName, rows = rows)
    }

    private fun resolveFirstWorksheetEntry(entries: Map<String, ByteArray>): String? {
        if (entries.containsKey("xl/worksheets/sheet1.xml")) {
            return "xl/worksheets/sheet1.xml"
        }
        return entries.keys
            .filter { it.startsWith("xl/worksheets/sheet") && it.endsWith(".xml") }
            .sorted()
            .firstOrNull()
    }

    private fun parseSharedStrings(bytes: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        val saxParser = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
        }.newSAXParser()

        val handler = object : DefaultHandler() {
            private var inSi = false
            private var inText = false
            private val currentBuilder = StringBuilder()

            override fun startElement(
                uri: String?,
                localName: String?,
                qName: String?,
                attributes: Attributes?
            ) {
                when (localTagName(qName ?: localName)) {
                    "si" -> {
                        inSi = true
                        currentBuilder.clear()
                    }
                    "t" -> if (inSi) inText = true
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inSi && inText) {
                    currentBuilder.appendRange(ch, start, start + length)
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                when (localTagName(qName ?: localName)) {
                    "t" -> inText = false
                    "si" -> {
                        strings.add(currentBuilder.toString())
                        inSi = false
                    }
                }
            }
        }

        saxParser.parse(ByteArrayInputStream(bytes), handler)
        return strings
    }

    private fun parseWorksheetXml(
        bytes: ByteArray,
        sharedStrings: List<String>
    ): List<List<String>> {
        val allRows = mutableListOf<List<String>>()
        val saxParser = SAXParserFactory.newInstance().apply {
            isNamespaceAware = false
        }.newSAXParser()

        val handler = object : DefaultHandler() {
            private var currentRowCells = mutableMapOf<Int, String>()
            private var currentCellCol = -1
            private var currentCellType = ""
            private var inValueTag = false
            private var inInlineTextTag = false
            private val valueBuilder = StringBuilder()

            override fun startElement(
                uri: String?,
                localName: String?,
                qName: String?,
                attributes: Attributes?
            ) {
                when (localTagName(qName ?: localName)) {
                    "row" -> currentRowCells = mutableMapOf()
                    "c" -> {
                        val ref = attributes?.getValue("r")
                        currentCellCol = if (!ref.isNullOrBlank()) {
                            columnIndexFromCellRef(ref)
                        } else {
                            (currentRowCells.keys.maxOrNull() ?: -1) + 1
                        }
                        currentCellType = attributes?.getValue("t").orEmpty()
                        valueBuilder.clear()
                    }
                    "v" -> inValueTag = true
                    "t" -> inInlineTextTag = true
                }
            }

            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inValueTag || inInlineTextTag) {
                    valueBuilder.appendRange(ch, start, start + length)
                }
            }

            override fun endElement(uri: String?, localName: String?, qName: String?) {
                when (localTagName(qName ?: localName)) {
                    "v" -> inValueTag = false
                    "t" -> inInlineTextTag = false
                    "c" -> {
                        if (currentCellCol >= 0) {
                            val raw = valueBuilder.toString().trim()
                            val resolved = resolveCellValue(raw, currentCellType, sharedStrings)
                            if (resolved.isNotBlank()) {
                                currentRowCells[currentCellCol] = resolved
                            }
                        }
                        currentCellCol = -1
                        currentCellType = ""
                    }
                    "row" -> {
                        if (currentRowCells.isNotEmpty()) {
                            val maxCol = currentRowCells.keys.maxOrNull() ?: 0
                            val rowList = MutableList(maxCol + 1) { col ->
                                currentRowCells[col].orEmpty()
                            }
                            if (rowList.any { it.isNotBlank() }) {
                                allRows.add(rowList)
                            }
                        }
                    }
                }
            }
        }

        saxParser.parse(ByteArrayInputStream(bytes), handler)
        return allRows
    }

    private fun resolveCellValue(
        raw: String,
        cellType: String,
        sharedStrings: List<String>
    ): String {
        return when (cellType) {
            "s" -> {
                val idx = raw.toIntOrNull()
                if (idx != null && idx in sharedStrings.indices) sharedStrings[idx].trim() else ""
            }
            "inlineStr", "str" -> raw
            "b" -> if (raw == "1") "TRUE" else "FALSE"
            else -> {
                if (raw.endsWith(".0") && raw.dropLast(2).all { it.isDigit() || it == '-' }) {
                    raw.dropLast(2)
                } else {
                    raw
                }
            }
        }
    }

    private fun localTagName(name: String?): String {
        if (name == null) return ""
        val colon = name.indexOf(':')
        return if (colon >= 0) name.substring(colon + 1) else name
    }

    internal fun columnIndexFromCellRef(cellRef: String): Int {
        var col = 0
        for (ch in cellRef) {
            if (ch.isLetter()) {
                col = col * 26 + (ch.uppercaseChar() - 'A' + 1)
            } else {
                break
            }
        }
        return (col - 1).coerceAtLeast(0)
    }
}
