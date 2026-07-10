package com.kula.stylusnotes.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.kula.stylusnotes.core.color.resolve
import com.kula.stylusnotes.core.model.CanvasBackground
import com.kula.stylusnotes.core.model.Stroke
import com.kula.stylusnotes.ink.StrokeRenderer
import java.io.File
import java.io.FileOutputStream

/**
 * Renders a note for handwriting-to-text recognition by an LLM: always onto a plain white
 * background at a fixed high resolution, regardless of the on-screen editing theme (Adaptive
 * ink automatically resolves to black on white here, same as it would on-screen against white).
 */
object NoteExporter {

    private const val EXPORT_SCALE = 2f

    fun renderToBitmap(strokes: List<Stroke>, widthPx: Int, heightPx: Int): Bitmap {
        val scaledWidth = (widthPx * EXPORT_SCALE).toInt().coerceAtLeast(1)
        val scaledHeight = (heightPx * EXPORT_SCALE).toInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(CanvasBackground.WHITE.argb)
        canvas.scale(EXPORT_SCALE, EXPORT_SCALE)

        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val path = Path()
        for (stroke in strokes) {
            StrokeRenderer.draw(
                canvas = canvas,
                paint = paint,
                scratchPath = path,
                color = stroke.color.resolve(CanvasBackground.WHITE),
                baseWidthPx = stroke.baseWidthPx,
                points = stroke.points
            )
        }
        return bitmap
    }

    fun exportPng(context: Context, title: String, strokes: List<Stroke>, widthPx: Int, heightPx: Int): Uri {
        val bitmap = renderToBitmap(strokes, widthPx, heightPx)
        val file = exportFile(context, title, "png")
        FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
        return uriFor(context, file)
    }

    fun exportPdf(context: Context, title: String, strokes: List<Stroke>, widthPx: Int, heightPx: Int): Uri {
        val bitmap = renderToBitmap(strokes, widthPx, heightPx)
        val document = PdfDocument()
        // Page matches the bitmap's full (supersampled) resolution rather than the original
        // on-screen size, so the exported PDF keeps the same detail as the PNG export.
        val page = document.startPage(PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create())
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        document.finishPage(page)

        val file = exportFile(context, title, "pdf")
        FileOutputStream(file).use { out -> document.writeTo(out) }
        document.close()
        return uriFor(context, file)
    }

    fun buildShareIntent(uri: Uri, mimeType: String): Intent {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        return Intent.createChooser(send, null)
    }

    private fun uriFor(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)

    private fun exportFile(context: Context, title: String, extension: String): File {
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val safeName = title.ifBlank { "note" }.replace(Regex("[^A-Za-z0-9-_ ]"), "_")
        return File(dir, "$safeName-${System.currentTimeMillis()}.$extension")
    }
}
