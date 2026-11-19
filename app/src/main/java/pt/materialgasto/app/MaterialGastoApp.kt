package pt.materialgasto.app

import android.app.Application
import pt.materialgasto.app.data.local.AppDatabase
import pt.materialgasto.app.data.repository.MaterialGastoRepository
import pt.materialgasto.app.data.storage.LocalFileStorage
import pt.materialgasto.app.pdf.PdfGenerator

class MaterialGastoApp : Application() {

    lateinit var repository: MaterialGastoRepository
        private set

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getInstance(this)
        val fileStorage = LocalFileStorage(this)
        val pdfGenerator = PdfGenerator()
        repository = MaterialGastoRepository(
            database = database,
            fileStorage = fileStorage,
            pdfGenerator = pdfGenerator
        )
    }
}
