package com.github.mykolab31.plugincalculator.plugin

import org.junit.Assert.fail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class ZipArchiveExtractorTest {

    private fun buildZip(entries: Map<String, ByteArray>): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            entries.forEach { (name, content) ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(content)
                zos.closeEntry()
            }
        }
        return baos.toByteArray()
    }

    @Test
    fun `archive within limits extracts successfully`() {
        val zipBytes = buildZip(
            mapOf(
                "manifest.json" to "{\"id\":\"test\"}".toByteArray(),
                "main.lua" to "function execute() end".toByteArray()
            )
        )

        val result = ZipArchiveExtractor.extract(ByteArrayInputStream(zipBytes))

        assertEquals(2, result.size)
        assertEquals("{\"id\":\"test\"}", result["manifest.json"])
        assertEquals("function execute() end", result["main.lua"])
    }

    @Test
    fun `archive with too many entries is rejected`() {
        val tooManyEntries = (1..ZipArchiveExtractor.MAX_ENTRY_COUNT + 1)
            .associate { "file$it.txt" to "x".toByteArray() }
        val zipBytes = buildZip(tooManyEntries)

        try {
            ZipArchiveExtractor.extract(ByteArrayInputStream(zipBytes))
            fail("Expected SecurityException for exceeding entry count limit")
        } catch (e: SecurityException) {
            assertTrue(
                "Expected message about too many files, got: ${e.message}",
                e.message?.contains("too many files", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun `archive exceeding total uncompressed size is rejected`() {
        val entrySize = (ZipArchiveExtractor.MAX_ENTRY_SIZE_BYTES - 500_000L).toInt()
        val entries = (1..5).associate { "file$it.bin" to ByteArray(entrySize) }
        val zipBytes = buildZip(entries)

        try {
            ZipArchiveExtractor.extract(ByteArrayInputStream(zipBytes))
            fail("Expected SecurityException for exceeding total size limit")
        } catch (e: SecurityException) {
            assertTrue(
                "Expected message about total uncompressed size, got: ${e.message}",
                e.message?.contains("total uncompressed size", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun `single entry exceeding max entry size is rejected`() {
        val oversizedEntry = ByteArray((ZipArchiveExtractor.MAX_ENTRY_SIZE_BYTES + 1024L).toInt())
        val zipBytes = buildZip(mapOf("huge.bin" to oversizedEntry))

        try {
            ZipArchiveExtractor.extract(ByteArrayInputStream(zipBytes))
            fail("Expected SecurityException for exceeding single entry size limit")
        } catch (e: SecurityException) {
            assertTrue(
                "Expected message about entry size, got: ${e.message}",
                e.message?.contains("exceeds maximum allowed size", ignoreCase = true) == true
            )
        }
    }

    @Test
    fun `zip slip path traversal is rejected`() {
        val zipBytes = buildZip(mapOf("../evil.lua" to "malicious".toByteArray()))

        try {
            ZipArchiveExtractor.extract(ByteArrayInputStream(zipBytes))
            fail("Expected SecurityException for path traversal")
        } catch (e: SecurityException) {
            assertTrue(
                "Expected message about unsafe path, got: ${e.message}",
                e.message?.contains("Unsafe entry path", ignoreCase = true) == true
            )
        }
    }
}