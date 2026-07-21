package com.pocketwin.launcher.engine

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.compressors.CompressorStreamFactory
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

class ChecksumMismatchException(expected: String, actual: String) :
    Exception("Downloaded file sha256 mismatch: expected $expected but got $actual. Refusing to install — the file may be corrupt or tampered with.")

/** Downloads and verifies [EngineComponent] payloads, then extracts them into place. */
class ComponentDownloader {

    /**
     * Streams [component]'s archive to [destArchiveFile], reporting progress, then verifies
     * its sha256 against [EngineComponent.sha256] before returning. Throws
     * [ChecksumMismatchException] (and deletes the partial file) on mismatch — a corrupted or
     * tampered download must never be extracted and run.
     */
    suspend fun download(
        component: EngineComponent,
        destArchiveFile: File,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> },
    ): File = withContext(Dispatchers.IO) {
        destArchiveFile.parentFile?.mkdirs()
        val digest = MessageDigest.getInstance("SHA-256")
        val connection = (URL(component.downloadUrl).openConnection() as HttpURLConnection).apply {
            instanceFollowRedirects = true
            connectTimeout = 15_000
            readTimeout = 30_000
        }
        try {
            connection.connect()
            check(connection.responseCode in 200..299) {
                "Download failed for ${component.id}: HTTP ${connection.responseCode}"
            }
            val totalBytes = component.sizeBytes.takeIf { it > 0 } ?: connection.contentLengthLong
            var bytesRead = 0L
            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(destArchiveFile).use { output ->
                    val buffer = ByteArray(1 shl 16)
                    while (true) {
                        val n = input.read(buffer)
                        if (n < 0) break
                        output.write(buffer, 0, n)
                        digest.update(buffer, 0, n)
                        bytesRead += n
                        onProgress(bytesRead, totalBytes)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }

        val actualSha256 = digest.digest().joinToString("") { "%02x".format(it) }
        if (!actualSha256.equals(component.sha256, ignoreCase = true)) {
            destArchiveFile.delete()
            throw ChecksumMismatchException(component.sha256, actualSha256)
        }
        destArchiveFile
    }

    /**
     * Extracts a tar archive (optionally gzip/xz compressed) into [destDir]. Handles the
     * rootfs and Wine build formats these components are typically distributed as.
     */
    suspend fun extract(archiveFile: File, destDir: File) = withContext(Dispatchers.IO) {
        destDir.mkdirs()
        BufferedInputStream(archiveFile.inputStream()).use { rawInput ->
            val decompressed = runCatching {
                CompressorStreamFactory().createCompressorInputStream(rawInput)
            }.getOrElse { rawInput } // already an uncompressed tar

            ArchiveStreamFactory().createArchiveInputStream(BufferedInputStream(decompressed)).use { archive ->
                var entry = archive.nextEntry
                while (entry != null) {
                    val outFile = File(destDir, entry.name).also { target ->
                        check(target.canonicalPath.startsWith(destDir.canonicalPath + File.separator)) {
                            "Archive entry escapes destination directory: ${entry?.name}"
                        }
                    }
                    if (entry.isDirectory) {
                        outFile.mkdirs()
                    } else {
                        outFile.parentFile?.mkdirs()
                        FileOutputStream(outFile).use { out -> archive.copyTo(out) }
                    }
                    entry = archive.nextEntry
                }
            }
        }
    }
}
