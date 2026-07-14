package com.github.mykolab31.plugincalculator.plugin

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

object ZipArchiveExtractor {

    internal const val MAX_ENTRY_SIZE_BYTES = 5 * 1024 * 1024L
    internal const val MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES = 20 * 1024 * 1024L
    internal const val MAX_ENTRY_COUNT = 50

    fun extract(inputStream: InputStream): Map<String, String> {
        val entries = mutableMapOf<String, String>()
        var totalUncompressedSize = 0L
        var entryCount = 0

        ZipInputStream(inputStream).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    entryCount++
                    if (entryCount > MAX_ENTRY_COUNT) {
                        throw SecurityException(
                            "Archive contains too many files (limit: $MAX_ENTRY_COUNT)"
                        )
                    }

                    val entryName = entry.name
                    if (entryName.contains("..") || entryName.startsWith("/")) {
                        throw SecurityException("Unsafe entry path detected: $entryName")
                    }

                    val outputStream = ByteArrayOutputStream()
                    val buffer = ByteArray(8192)
                    var entryBytes = 0L
                    var bytesRead = zip.read(buffer)

                    while (bytesRead != -1) {
                        entryBytes += bytesRead
                        totalUncompressedSize += bytesRead

                        if (entryBytes > MAX_ENTRY_SIZE_BYTES) {
                            throw SecurityException(
                                "Entry '${entry.name}' exceeds maximum allowed size"
                            )
                        }
                        if (totalUncompressedSize > MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES) {
                            throw SecurityException(
                                "Archive exceeds maximum total uncompressed size ($MAX_TOTAL_UNCOMPRESSED_SIZE_BYTES bytes"
                            )
                        }

                        outputStream.write(buffer, 0, bytesRead)
                        bytesRead = zip.read(buffer)
                    }

                    entries[entryName] = outputStream.toString(Charsets.UTF_8.name())
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }

        return entries
    }
}