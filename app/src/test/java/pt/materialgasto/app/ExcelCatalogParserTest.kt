package pt.materialgasto.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.materialgasto.app.excel.ExcelCatalogParser
import pt.materialgasto.app.excel.ExcelLoadResult
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ExcelCatalogParserTest {

    @Test
    fun parsesValidXlsxWithDuplicatesAndConflicts() {
        val xlsxBytes = buildMinimalXlsx(
            sharedStrings = listOf(
                "Referência",
                "Designação",
                "ABC123",
                "Válvula de segurança",
                "DEF456",
                "Tubo de cobre",
                "CONFL1",
                "Filtro A",
                "Filtro B"
            ),
            sheetRowsXml = """
                <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row>
                <row r="2"><c r="A2" t="s"><v>2</v></c><c r="B2" t="s"><v>3</v></c></row>
                <row r="3"><c r="A3" t="s"><v>2</v></c><c r="B3" t="s"><v>3</v></c></row>
                <row r="4"><c r="A4" t="s"><v>4</v></c><c r="B4" t="s"><v>5</v></c></row>
                <row r="5"><c r="A5" t="s"><v>6</v></c><c r="B5" t="s"><v>7</v></c></row>
                <row r="6"><c r="A6" t="s"><v>6</v></c><c r="B6" t="s"><v>8</v></c></row>
            """.trimIndent()
        )

        val result = ExcelCatalogParser.parseAndAnalyze(
            ByteArrayInputStream(xlsxBytes),
            "materiais.xlsx"
        )

        assertTrue(result is ExcelLoadResult.ReadyForReview)
        val analysis = (result as ExcelLoadResult.ReadyForReview).analysis
        assertEquals(2, analysis.uniqueItems.size)
        assertEquals(1, analysis.duplicateCount)
        assertEquals(1, analysis.conflicts.size)
        assertEquals("CONFL1", analysis.conflicts.first().referenciaDisplay)
        assertEquals(listOf("Filtro A", "Filtro B"), analysis.conflicts.first().designations)
    }

    @Test
    fun requestsColumnMappingWhenHeadersAreUnrecognized() {
        val xlsxBytes = buildMinimalXlsx(
            sharedStrings = listOf("Coluna1", "Coluna2", "X10", "Parafuso M6"),
            sheetRowsXml = """
                <row r="1"><c r="A1" t="s"><v>0</v></c><c r="B1" t="s"><v>1</v></c></row>
                <row r="2"><c r="A2" t="s"><v>2</v></c><c r="B2" t="s"><v>3</v></c></row>
            """.trimIndent()
        )

        val result = ExcelCatalogParser.parseAndAnalyze(
            ByteArrayInputStream(xlsxBytes),
            "outro.xlsx"
        )

        assertTrue(result is ExcelLoadResult.NeedsColumnMapping)
    }

    private fun buildMinimalXlsx(sharedStrings: List<String>, sheetRowsXml: String): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val ssXml = buildString {
                append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>""")
                append("""<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">""")
                sharedStrings.forEach { s ->
                    append("<si><t>$s</t></si>")
                }
                append("</sst>")
            }
            zos.putNextEntry(ZipEntry("xl/sharedStrings.xml"))
            zos.write(ssXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            val sheetXml = """
                <?xml version="1.0" encoding="UTF-8" standalone="yes"?>
                <worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
                    <sheetData>
                        $sheetRowsXml
                    </sheetData>
                </worksheet>
            """.trimIndent()
            zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
            zos.write(sheetXml.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
        }
        return baos.toByteArray()
    }
}
