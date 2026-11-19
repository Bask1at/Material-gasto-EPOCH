package pt.materialgasto.app.ui.records

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import pt.materialgasto.app.data.local.entity.MaterialItemEntity
import pt.materialgasto.app.pdf.PdfGenerator
import pt.materialgasto.app.share.ShareHelper
import pt.materialgasto.app.ui.theme.BlueDark
import pt.materialgasto.app.ui.theme.BluePrimary
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordDetailScreen(
    recordId: Long,
    viewModel: SavedRecordsViewModel,
    onNavigateBack: () -> Unit
) {
    val recordFlow = remember(recordId) { viewModel.observeRecord(recordId) }
    val recordWithMaterials by recordFlow.collectAsStateWithLifecycle(initialValue = null)
    val destinationEmail by viewModel.destinationEmail.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showDeleteConfirm by remember { mutableStateOf(false) }
    var fullScreenPhotoPath by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Detalhe do Registo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    if (recordWithMaterials != null) {
                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar registo"
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BluePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { innerPadding ->
        val current = recordWithMaterials
        if (current == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "A carregar registo...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val sortedMaterials = current.materials.sortedBy { it.sortOrder }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Details Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = current.record.workOrder,
                            style = MaterialTheme.typography.titleLarge,
                            color = BlueDark
                        )
                        Text(
                            text = "Técnico: ${current.record.technicianName}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "Data: ${current.record.dateText}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        val pdfName = current.record.pdfFilePath?.let { File(it).name }
                        if (!pdfName.isNullOrBlank()) {
                            Text(
                                text = "PDF: $pdfName",
                                style = MaterialTheme.typography.bodySmall,
                                color = BluePrimary
                            )
                        }
                    }
                }

                // PDF Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                val pdfFile = viewModel.getOrRegeneratePdf(current)
                                ShareHelper.openPdf(context, pdfFile)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Abrir PDF")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                val pdfFile = viewModel.getOrRegeneratePdf(current)
                                ShareHelper.sharePdfByEmail(
                                    context = context,
                                    pdfFile = pdfFile,
                                    recipientEmail = destinationEmail,
                                    workOrder = current.record.workOrder,
                                    technicianName = current.record.technicianName,
                                    dateText = current.record.dateText
                                )
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Icon(Icons.Default.Email, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Enviar Email")
                    }
                }

                // Materials List + Photos
                Text(
                    text = "Materiais Registados (${sortedMaterials.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = BlueDark
                )

                sortedMaterials.forEachIndexed { idx, item ->
                    DetailMaterialCard(
                        index = idx + 1,
                        item = item,
                        onPhotoClick = { path -> fullScreenPhotoPath = path }
                    )
                }
            }
        }
    }

    if (showDeleteConfirm && recordWithMaterials != null) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Eliminar Registo") },
            text = {
                Text("Tem a certeza que pretende eliminar este registo, as respetivas fotografias e o ficheiro PDF?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        val target = recordWithMaterials ?: return@Button
                        showDeleteConfirm = false
                        viewModel.deleteRecord(target) {
                            onNavigateBack()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    val photoToView = fullScreenPhotoPath
    if (!photoToView.isNullOrBlank()) {
        Dialog(
            onDismissRequest = { fullScreenPhotoPath = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    AsyncImage(
                        model = File(photoToView),
                        contentDescription = "Fotografia do material",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    IconButton(
                        onClick = { fullScreenPhotoPath = null },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailMaterialCard(
    index: Int,
    item: MaterialItemEntity,
    onPhotoClick: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "$index. ${item.referencia.ifBlank { "(Sem referência)" }}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = BlueDark
            )
            if (item.designacao.isNotBlank()) {
                Text(
                    text = item.designacao,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = "Quantidade: ${PdfGenerator.formatQuantity(item.quantity)}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold
            )
            if (item.barcode.isNotBlank()) {
                Text(
                    text = "Código de barras: ${item.barcode}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            val photoPath = item.photoPath
            if (!photoPath.isNullOrBlank() && File(photoPath).exists()) {
                Spacer(modifier = Modifier.height(6.dp))
                AsyncImage(
                    model = File(photoPath),
                    contentDescription = "Fotografia de ${item.designacao}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onPhotoClick(photoPath) }
                )
                Text(
                    text = "Toque na fotografia para ampliar",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
