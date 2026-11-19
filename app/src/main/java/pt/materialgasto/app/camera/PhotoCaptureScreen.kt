package pt.materialgasto.app.camera

import android.view.ViewGroup
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pt.materialgasto.app.data.storage.LocalFileStorage
import java.io.File

@Composable
fun PhotoCaptureScreen(
    fileStorage: LocalFileStorage,
    onPhotoConfirmed: (String) -> Unit,
    onCancel: () -> Unit
) {
    CameraPermissionGate(
        rationaleText = "Para tirar uma fotografia do material, autorize o acesso à câmara do telemóvel.",
        onCancel = onCancel
    ) {
        PhotoCaptureContent(
            fileStorage = fileStorage,
            onPhotoConfirmed = onPhotoConfirmed,
            onCancel = onCancel
        )
    }
}

@Composable
private fun PhotoCaptureContent(
    fileStorage: LocalFileStorage,
    onPhotoConfirmed: (String) -> Unit,
    onCancel: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember { ContextCompat.getMainExecutor(context) }
    val scope = rememberCoroutineScope()

    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var hasFlashUnit by remember { mutableStateOf(false) }
    var isCapturing by remember { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var capturedTempFile by remember { mutableStateOf<File?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(lifecycleOwner, capturedTempFile == null) {
        if (capturedTempFile != null) {
            return@DisposableEffect onDispose { }
        }
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                runCatching {
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()

                    cameraProvider.unbindAll()
                    val boundCamera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )
                    camera = boundCamera
                    imageCapture = capture
                    hasFlashUnit = boundCamera.cameraInfo.hasFlashUnit()
                    boundCamera.cameraControl.enableTorch(isTorchEnabled)
                }.onFailure { err ->
                    errorMessage = err.localizedMessage ?: "Erro ao iniciar a câmara."
                }
            },
            mainExecutor
        )

        onDispose {
            runCatching {
                if (cameraProviderFuture.isDone) {
                    cameraProviderFuture.get().unbindAll()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            // Clean up unconfirmed temp capture if user leaves
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val currentCaptured = capturedTempFile
        if (currentCaptured == null) {
            AndroidView(
                factory = { previewView },
                modifier = Modifier.fillMaxSize()
            )

            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onCancel,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.6f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar"
                    )
                }

                if (hasFlashUnit) {
                    IconButton(
                        onClick = {
                            val next = !isTorchEnabled
                            isTorchEnabled = next
                            camera?.cameraControl?.enableTorch(next)
                        },
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = if (isTorchEnabled) {
                                Color(0xFF1565C0)
                            } else {
                                Color.Black.copy(alpha = 0.6f)
                            },
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = "Flash"
                        )
                    }
                }
            }

            // Bottom shutter button
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 28.dp, start = 24.dp, end = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (errorMessage != null) {
                    Text(
                        text = errorMessage.orEmpty(),
                        color = Color(0xFFFFCDD2),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                IconButton(
                    onClick = {
                        val capture = imageCapture ?: return@IconButton
                        if (isCapturing) return@IconButton
                        isCapturing = true
                        errorMessage = null

                        val tempFile = fileStorage.createTempPhotoFile()
                        val outputOptions = ImageCapture.OutputFileOptions.Builder(tempFile).build()
                        capture.takePicture(
                            outputOptions,
                            mainExecutor,
                            object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                    isCapturing = false
                                    capturedTempFile = tempFile
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    isCapturing = false
                                    runCatching { tempFile.delete() }
                                    errorMessage = "Não foi possível tirar a fotografia. Tente novamente."
                                }
                            }
                        )
                    },
                    enabled = !isCapturing,
                    colors = IconButtonDefaults.iconButtonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF0D47A1)
                    ),
                    modifier = Modifier
                        .size(76.dp)
                        .background(Color.White, CircleShape)
                ) {
                    if (isCapturing) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            color = Color(0xFF0D47A1)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Tirar Fotografia",
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Toque para fotografar o material",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // Confirm or Retake Preview
            AsyncImage(
                model = currentCaptured,
                contentDescription = "Pré-visualização da fotografia",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 110.dp)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .navigationBarsPadding()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        runCatching { currentCaptured.delete() }
                        capturedTempFile = null
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Repetir", fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = {
                        if (isSaving) return@Button
                        isSaving = true
                        scope.launch {
                            val savedPath = withContext(Dispatchers.IO) {
                                fileStorage.saveConfirmedPhoto(currentCaptured)
                            }
                            isSaving = false
                            onPhotoConfirmed(savedPath)
                        }
                    },
                    enabled = !isSaving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1565C0),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(54.dp)
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.size(6.dp))
                        Text("Confirmar", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}
