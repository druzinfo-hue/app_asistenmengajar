package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfExtractorHelper {

    private const val TAG = "PdfExtractorHelper"

    /**
     * Extracts text from a PDF file by rendering each page to a Bitmap
     * and performing MLKit OCR on each page.
     *
     * @param context Android context
     * @param uri Uri of the PDF file
     * @param maxPages Maximum number of pages to extract (default 15)
     * @param onProgress Callback with (currentPage, totalPages)
     * @return Extracted full text
     */
    suspend fun extractTextFromPdfUri(
        context: Context,
        uri: Uri,
        maxPages: Int = 15,
        onProgress: ((Int, Int) -> Unit)? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var tempFile: File? = null

        try {
            // First attempt to open descriptor directly, or copy to temp file
            pfd = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (e: Exception) {
                null
            }

            if (pfd == null) {
                // Copy stream to temp cache file
                tempFile = File.createTempFile("temp_pdf_extract", ".pdf", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }

            if (pfd == null) {
                return@withContext Result.failure(Exception("Tidak dapat membuka file PDF"))
            }

            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            val pagesToProcess = minOf(totalPages, maxPages)

            if (pagesToProcess <= 0) {
                return@withContext Result.failure(Exception("File PDF tidak memiliki halaman"))
            }

            val fullTextBuilder = StringBuilder()

            for (i in 0 until pagesToProcess) {
                onProgress?.invoke(i + 1, pagesToProcess)
                val page = renderer.openPage(i)

                // Scale up slightly for better OCR accuracy (1.5x)
                val width = (page.width * 1.5).toInt().coerceIn(300, 2400)
                val height = (page.height * 1.5).toInt().coerceIn(400, 3200)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                try {
                    val pageText = OcrHelper.recognizeTextFromBitmap(bitmap)
                    if (pageText.isNotBlank()) {
                        fullTextBuilder.append("=== Halaman ${i + 1} ===\n")
                        fullTextBuilder.append(pageText.trim())
                        fullTextBuilder.append("\n\n")
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal OCR halaman $i: ${e.message}")
                } finally {
                    bitmap.recycle()
                }
            }

            val resultText = fullTextBuilder.toString().trim()
            if (resultText.isBlank()) {
                Result.failure(Exception("Tidak ada teks yang dapat dideteksi dari PDF ini"))
            } else {
                Result.success(resultText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saat membaca PDF: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                renderer?.close()
                pfd?.close()
                tempFile?.delete()
            } catch (ignored: Exception) {}
        }
    }

    suspend fun extractPagesFromPdfUri(
        context: Context,
        uri: Uri,
        maxPages: Int = 30,
        onProgress: ((Int, Int) -> Unit)? = null
    ): Result<List<Pair<Int, String>>> = withContext(Dispatchers.IO) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var tempFile: File? = null

        try {
            pfd = try {
                context.contentResolver.openFileDescriptor(uri, "r")
            } catch (e: Exception) {
                null
            }

            if (pfd == null) {
                tempFile = File.createTempFile("temp_pdf_pages", ".pdf", context.cacheDir)
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
                pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            }

            if (pfd == null) {
                return@withContext Result.failure(Exception("Tidak dapat membuka file PDF"))
            }

            renderer = PdfRenderer(pfd)
            val totalPages = renderer.pageCount
            val pagesToProcess = minOf(totalPages, maxPages)

            if (pagesToProcess <= 0) {
                return@withContext Result.failure(Exception("File PDF tidak memiliki halaman"))
            }

            val pageList = mutableListOf<Pair<Int, String>>()

            for (i in 0 until pagesToProcess) {
                onProgress?.invoke(i + 1, pagesToProcess)
                val page = renderer.openPage(i)

                val width = (page.width * 1.5).toInt().coerceIn(300, 2400)
                val height = (page.height * 1.5).toInt().coerceIn(400, 3200)

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                try {
                    val pageText = OcrHelper.recognizeTextFromBitmap(bitmap)
                    if (pageText.isNotBlank()) {
                        pageList.add(Pair(i + 1, pageText.trim()))
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Gagal OCR halaman $i: ${e.message}")
                } finally {
                    bitmap.recycle()
                }
            }

            if (pageList.isEmpty()) {
                Result.failure(Exception("Tidak ada teks yang dapat dideteksi dari PDF ini"))
            } else {
                Result.success(pageList)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error saat membaca halaman PDF: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                renderer?.close()
                pfd?.close()
                tempFile?.delete()
            } catch (ignored: Exception) {}
        }
    }
}
