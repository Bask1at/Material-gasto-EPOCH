package pt.materialgasto.app.excel

data class ParsedSpreadsheet(
    val fileName: String,
    val rows: List<List<String>>
) {
    val maxColumns: Int
        get() = rows.maxOfOrNull { it.size } ?: 0
}

data class ReferenceConflict(
    val normalizedReferencia: String,
    val referenciaDisplay: String,
    val designations: List<String>
)

data class ExcelImportAnalysis(
    val fileName: String,
    val uniqueItems: List<Pair<String, String>>,
    val conflicts: List<ReferenceConflict>,
    val duplicateCount: Int,
    val skippedIncompleteCount: Int
)

sealed interface ExcelLoadResult {
    data class NeedsColumnMapping(
        val spreadsheet: ParsedSpreadsheet,
        val suggestedRefColumn: Int?,
        val suggestedDescColumn: Int?
    ) : ExcelLoadResult

    data class ReadyForReview(
        val analysis: ExcelImportAnalysis
    ) : ExcelLoadResult

    data class Error(val message: String) : ExcelLoadResult
}
