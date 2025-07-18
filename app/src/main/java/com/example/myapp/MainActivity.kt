package com.example.myapp

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.*
import android.provider.MediaStore
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No UI adicional; solo procesar el intent de compartir
        if (intent?.action == Intent.ACTION_SEND && intent.type?.startsWith("video/") == true) {
            if (hasRequiredPermissions()) {
                processIncomingIntent(intent)
            } else {
                requestRequiredPermissions()
            }
        } else {
            // Cerrar si no se invoca por intent de compartir
            Toast.makeText(this, "Esta app solo funciona compartiendo videos", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun processIncomingIntent(intent: Intent) {
        val videoUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
        if (videoUri != null) {
            saveVideoFile(videoUri)
        } else {
            showError("No se recibió ningún video")
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestRequiredPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_VIDEO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        ActivityCompat.requestPermissions(this, permissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                processIncomingIntent(intent)
            } else {
                showError("Permiso denegado. No se puede guardar el video.")
                finish()
            }
        }
    }

    private fun saveVideoFile(sourceUri: Uri) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Para API >= 29 usar MediaStore API
                val filename = "video_${System.currentTimeMillis()}.mp4"
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/MyApp")
                }
                
                val contentResolver = contentResolver
                val targetUri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                
                if (targetUri == null) {
                    showError("Error al crear archivo de destino")
                    return
                }
                
                contentResolver.openOutputStream(targetUri).use { outputStream ->
                    copyContent(sourceUri, outputStream)
                }
            } else {
                // Para API < 29, usar API de archivos del almacenamiento externo
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val targetDir = File(downloadsDir, "MyApp")
                
                if (!targetDir.exists()) {
                    targetDir.mkdirs()
                }
                
                val filename = "video_${System.currentTimeMillis()}.mp4"
                val targetFile = File(targetDir, filename)
                
                FileOutputStream(targetFile).use { outputStream ->
                    copyContent(sourceUri, outputStream)
                }
            }
            
            Toast.makeText(this, "Video guardado en Downloads/MyApp/", Toast.LENGTH_LONG).show()
            
        } catch (e: Exception) {
            e.printStackTrace()
            showError("Error al guardar el video: ${e.localizedMessage}")
        } finally {
            finish() // Cerrar actividad después del procesamiento
        }
    }

    private fun copyContent(sourceUri: Uri, outputStream: OutputStream?) {
        if (outputStream == null) throw IOException("No se pudo abrir el stream de salida")
        
        contentResolver.openInputStream(sourceUri)?.use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
        } ?: throw IOException("No se pudo abrir el stream de entrada")
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}
