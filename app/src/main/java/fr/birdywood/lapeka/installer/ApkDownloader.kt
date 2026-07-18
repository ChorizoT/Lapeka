package fr.birdywood.lapeka.installer

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

class ApkDownloader(
    private val context: Context,
    private val client: OkHttpClient = OkHttpClient()
) {

    sealed class Result {
        data class Success(val file: File) : Result()
        data class Failure(val message: String) : Result()
    }

    suspend fun download(url: String, fileName: String, expectedSha256: String?): Result =
        withContext(Dispatchers.IO) {
            try {
                val cacheDir = File(context.cacheDir, "apks").apply { mkdirs() }
                val outFile = File(cacheDir, fileName)

                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        return@withContext Result.Failure("HTTP ${response.code}")
                    }
                    val body = response.body ?: return@withContext Result.Failure("Empty response body")
                    FileOutputStream(outFile).use { output ->
                        body.byteStream().copyTo(output)
                    }
                }

                if (expectedSha256 != null) {
                    val actual = sha256Of(outFile)
                    if (!actual.equals(expectedSha256, ignoreCase = true)) {
                        outFile.delete()
                        return@withContext Result.Failure(
                            "Checksum mismatch (expected $expectedSha256, got $actual)"
                        )
                    }
                }

                Result.Success(outFile)
            } catch (e: Exception) {
                Result.Failure(e.message ?: "Unknown download error")
            }
        }

    private fun sha256Of(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}
