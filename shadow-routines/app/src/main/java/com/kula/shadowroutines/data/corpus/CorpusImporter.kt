package com.kula.shadowroutines.data.corpus

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

/** A discovered corpus file: its display name and full text content. */
data class CorpusFile(val name: String, val content: String)

/**
 * Discovers and reads Markdown corpus files from two sources:
 *  - the bundled `assets/quotes/` directory (the starter corpus), and
 *  - a user-picked directory via the Storage Access Framework (sideloaded `.md` files).
 *
 * Reading is one-shot: content is returned to the caller, which parses and stores it. We do
 * not hold on to the SAF uri, so no persistable permission is needed for v0.
 */
object CorpusImporter {

    private const val ASSET_DIR = "quotes"

    fun readAssets(context: Context): List<CorpusFile> {
        val assets = context.assets
        val names = assets.list(ASSET_DIR)?.filter { it.endsWith(".md", ignoreCase = true) }
            ?: emptyList()
        return names.map { name ->
            val content = assets.open("$ASSET_DIR/$name").bufferedReader().use { it.readText() }
            CorpusFile(name, content)
        }
    }

    fun readDirectory(context: Context, treeUri: Uri): List<CorpusFile> {
        val tree = DocumentFile.fromTreeUri(context, treeUri) ?: return emptyList()
        val resolver = context.contentResolver
        return tree.listFiles()
            .filter { it.isFile && (it.name?.endsWith(".md", ignoreCase = true) == true) }
            .mapNotNull { doc ->
                val name = doc.name ?: return@mapNotNull null
                val content = resolver.openInputStream(doc.uri)?.use { stream ->
                    stream.bufferedReader().readText()
                } ?: return@mapNotNull null
                CorpusFile(name, content)
            }
    }
}
