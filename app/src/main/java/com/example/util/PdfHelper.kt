package com.example.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

object PdfHelper {

    private const val TAG = "PdfHelper"

    suspend fun saveTextAsPdf(
        context: Context,
        title: String,
        bodyText: String,
        fileNamePrefix: String
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val pdfDoc = PdfDocument()

            // Standard A4 dimensions in points (72 points per inch): 595 x 842
            val pageWidth = 595
            val pageHeight = 842
            val margin = 48
            val printableWidth = pageWidth - (margin * 2)

            val titlePaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 16f
                isFakeBoldText = true
                color = Color.rgb(46, 125, 50) // GreenPrimary
            }

            val bodyPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 10.5f
                color = Color.rgb(33, 33, 33)
            }

            val footerPaint = TextPaint().apply {
                isAntiAlias = true
                textSize = 8.5f
                color = Color.rgb(128, 128, 128)
            }

            // Split body text into paragraphs/lines
            val lines = bodyText.lines()
            val textBlocks = mutableListOf<String>()
            var currentBlock = StringBuilder()

            for (line in lines) {
                currentBlock.append(line).append("\n")
                if (currentBlock.length > 2500) {
                    textBlocks.add(currentBlock.toString())
                    currentBlock = StringBuilder()
                }
            }
            if (currentBlock.isNotEmpty()) {
                textBlocks.add(currentBlock.toString())
            }

            var pageNumber = 1
            val totalBlocks = textBlocks.size

            for ((index, block) in textBlocks.withIndex()) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                // Draw header on first page
                var yPos = margin.toFloat()
                if (pageNumber == 1) {
                    canvas.drawText(title, margin.toFloat(), yPos + 14f, titlePaint)
                    yPos += 28f
                    val linePaint = Paint().apply {
                        color = Color.rgb(200, 200, 200)
                        strokeWidth = 1f
                    }
                    canvas.drawLine(margin.toFloat(), yPos, (pageWidth - margin).toFloat(), yPos, linePaint)
                    yPos += 14f
                }

                // Draw text using StaticLayout for automatic line wrapping
                canvas.save()
                canvas.translate(margin.toFloat(), yPos)

                val staticLayout = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    android.text.StaticLayout.Builder.obtain(
                        block, 0, block.length, bodyPaint, printableWidth
                    ).setAlignment(Layout.Alignment.ALIGN_NORMAL)
                     .setLineSpacing(2f, 1.15f)
                     .build()
                } else {
                    @Suppress("DEPRECATION")
                    StaticLayout(
                        block, bodyPaint, printableWidth,
                        Layout.Alignment.ALIGN_NORMAL, 1.15f, 2f, false
                    )
                }

                staticLayout.draw(canvas)
                canvas.restore()

                // Draw footer
                val footerText = "Asisten Mengajar SD • Halaman $pageNumber dari $totalBlocks"
                canvas.drawText(footerText, margin.toFloat(), (pageHeight - 24).toFloat(), footerPaint)

                pdfDoc.finishPage(page)
                pageNumber++
            }

            val sanitizedTitle = fileNamePrefix.replace(Regex("[^a-zA-Z0-9_]"), "_").take(40)
            val fileName = "${sanitizedTitle}_${System.currentTimeMillis()}.pdf"

            var savedPath = ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/AsistenMengajar")
                }
                val uri: Uri? = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        pdfDoc.writeTo(out)
                    }
                    savedPath = "Unduhan/AsistenMengajar/$fileName"
                }
            } else {
                @Suppress("DEPRECATION")
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "AsistenMengajar")
                if (!dir.exists()) dir.mkdirs()
                val file = File(dir, fileName)
                FileOutputStream(file).use { out ->
                    pdfDoc.writeTo(out)
                }
                savedPath = file.absolutePath
            }

            pdfDoc.close()
            if (savedPath.isNotEmpty()) {
                Result.success(savedPath)
            } else {
                // Fallback to internal storage
                val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
                FileOutputStream(file).use { out ->
                    pdfDoc.writeTo(out)
                }
                pdfDoc.close()
                Result.success(file.absolutePath)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating PDF: ${e.message}", e)
            Result.failure(e)
        }
    }
}
