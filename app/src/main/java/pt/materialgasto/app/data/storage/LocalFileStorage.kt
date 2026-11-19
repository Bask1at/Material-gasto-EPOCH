package pt.materialgasto.app.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class LocalFileStorage(private val context: Context) {

    private val photosDir: File
        get() = File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }

    private val pdfsDir: File
        get() = File(context.filesDir, "pdfs").apply { if (!exists()) mkdirs() }

    fun createTempPhotoFile(): File {
        return File(context.cacheDir, "capture_${UUID.randomUUID()}.jpg")
    }

    /**
     * Copies a captured JPEG from [sourceFile] into permanent private app storage,
     * applying EXIF rotation and scaling down excessively huge bitmaps so they
     * persist reliably across app restarts and render fast in Compose & PDFs.
     */
    fun saveConfirmedPhoto(sourceFile: File): String {
        val targetFile = File(photosDir, "mat_${UUID.randomUUID()}.jpg")
        val rotatedBitmap = decodeOrientedBitmap(sourceFile, maxDimension = 1600)
        if (rotatedBitmap != null) {
            FileOutputStream(targetFile).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 88, out)
            }
            rotatedBitmap.recycle()
        } else {
            sourceFile.copyTo(targetFile, overwrite = true)
        }
        runCatching { sourceFile.delete() }
        return targetFile.absolutePath
    }

    fun deleteFileIfExists(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching {
            val file = File(path)
            if (file.exists()) file.delete()
        }
    }

    fun buildUniquePdfFile(workOrder: String, technicianName: String, dateText: String): File {
        val baseName = buildPdfBaseFileName(workOrder, technicianName, dateText)
        var candidate = File(pdfsDir, "$baseName.pdf")
        var counter = 2
        while (candidate.exists()) {
            candidate = File(pdfsDir, "${baseName}_$counter.pdf")
            counter++
        }
        return candidate
    }

    companion object {
        fun buildPdfBaseFileName(
            workOrder: String,
            technicianName: String,
            dateText: String
        ): String {
            val cleanObra = sanitizeSegment(workOrder).ifBlank { "Obra" }
            val cleanTech = sanitizeSegment(technicianName).ifBlank { "Tecnico" }
            val cleanDate = sanitizeSegment(dateText).ifBlank { "Data" }
            return "${cleanObra}_${cleanTech}_$cleanDate"
        }

        fun sanitizeSegment(raw: String): String {
            return raw.trim()
                .replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
                .replace(Regex("_+"), "_")
                .trim('_')
                .take(60)
        }

        fun decodeOrientedBitmap(file: File, maxDimension: Int): Bitmap? {
            if (!file.exists()) return null

            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)
            val width = boundsOptions.outWidth
            val height = boundsOptions.outHeight
            if (width <= 0 || height <= 0) return null

            var sampleSize = 1
            while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
                sampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
            }
            val rawBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null

            val orientation = runCatching {
                ExifInterface(file.absolutePath).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return rawBitmap
            }

            val transformed = Bitmap.createBitmap(
                rawBitmap,
                0,
                0,
                rawBitmap.width,
                rawBitmap.height,
                matrix,
                true
            )
            if (transformed != rawBitmap) {
                rawBitmap.recycle()
            }
            return transformed
        }
    }
}
