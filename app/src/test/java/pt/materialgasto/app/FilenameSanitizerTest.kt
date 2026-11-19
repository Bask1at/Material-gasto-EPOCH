package pt.materialgasto.app

import org.junit.Assert.assertEquals
import org.junit.Test
import pt.materialgasto.app.data.storage.LocalFileStorage

class FilenameSanitizerTest {

    @Test
    fun buildsPdfFilenameRemovingInvalidCharacters() {
        val name = LocalFileStorage.buildPdfBaseFileName(
            workOrder = "Obra 2026/04: Rua*Nova",
            technicianName = "João Silva",
            dateText = "19/11/2026"
        )
        assertEquals("Obra_2026_04_Rua_Nova_João_Silva_19_11_2026", name)
    }
}
