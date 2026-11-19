package pt.materialgasto.app.ui.newrecord

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardHide
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import pt.materialgasto.app.data.local.entity.CatalogItemEntity
import pt.materialgasto.app.pdf.PdfGenerator
import pt.materialgasto.app.share.ShareHelper
import pt.materialgasto.app.ui.theme.BlueContainer
import pt.materialgasto.app.ui.theme.BlueDark
import pt.materialgasto.app.ui.theme.BluePrimary
import pt.materialgasto.app.ui.theme.ErrorContainerLight
import pt.materialgasto.app.ui.theme.SuccessContainerLight
import pt.materialgasto.app.ui.theme.SuccessGreen
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewRecordScreen(
    viewModel: NewRecordViewModel,
    onOpenBarcodeScanner: () -> Unit,
    onOpenPhotoCapture: () -> Unit,
    onNavigateBack: () -> Unit,
    onFinishToSavedRecords: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val technicians by viewModel.technicians.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()

    var showTechnicianMenu by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Novo Registo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardHide,
                            contentDescription = "Esconder teclado"
                        )
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Work Order Identification Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Identificação da Obra",
                        style = MaterialTheme.typography.titleMedium,
                        color = BlueDark
                    )

                    OutlinedTextField(
                        value = uiState.workOrder,
                        onValueChange = viewModel::onWorkOrderChanged,
                        label = { Text("Número / Nome da Obra *") },
                        placeholder = { Text("Ex.: Obra 2026-045 / Rua da Escola") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    ExposedDropdownMenuBox(
                        expanded = showTechnicianMenu,
                        onExpandedChange = { showTechnicianMenu = it }
                    ) {
                        OutlinedTextField(
                            value = uiState.technicianName,
                            onValueChange = viewModel::onTechnicianNameChanged,
                            label = { Text("Nome do Técnico *") },
                            placeholder = { Text("Selecione ou escreva o nome") },
                            singleLine = true,
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showTechnicianMenu)
                            },
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.Words,
                                imeAction = ImeAction.Next
                            ),
                            keyboardActions = KeyboardActions(
                                onNext = {
                                    showTechnicianMenu = false
                                    focusManager.moveFocus(FocusDirection.Down)
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(MenuAnchorType.PrimaryEditable, enabled = true)
                        )

                        if (technicians.isNotEmpty()) {
                            ExposedDropdownMenu(
                                expanded = showTechnicianMenu,
                                onDismissRequest = { showTechnicianMenu = false }
                            ) {
                                technicians.forEach { tech ->
                                    DropdownMenuItem(
                                        text = { Text(tech.name) },
                                        onClick = {
                                            viewModel.onTechnicianNameChanged(tech.name)
                                            showTechnicianMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = uiState.dateText,
                        onValueChange = viewModel::onDateTextChanged,
                        label = { Text("Data *") },
                        placeholder = { Text("dd/mm/aaaa") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(onClick = { showDatePicker = true }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarToday,
                                    contentDescription = "Escolher data"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // 2. Material Entry Card (5 Independent Fields)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (uiState.editingMaterialLocalId != null) {
                            "Editar Material"
                        } else {
                            "Adicionar Material"
                        },
                        style = MaterialTheme.typography.titleMedium,
                        color = BlueDark
                    )

                    // Field 1: Referência
                    OutlinedTextField(
                        value = uiState.draftReferencia,
                        onValueChange = viewModel::onReferenciaChanged,
                        label = { Text("1. Referência") },
                        placeholder = { Text("Ex.: ABC123") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                viewModel.dismissSuggestions()
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.referenciaSuggestions.isNotEmpty()) {
                        CatalogSuggestionsCard(
                            suggestions = uiState.referenciaSuggestions,
                            onSelect = { item ->
                                viewModel.onSelectCatalogItem(item)
                                focusManager.clearFocus()
                            }
                        )
                    }

                    // Field 2: Designação
                    OutlinedTextField(
                        value = uiState.draftDesignacao,
                        onValueChange = viewModel::onDesignacaoChanged,
                        label = { Text("2. Designação") },
                        placeholder = { Text("Ex.: Válvula de segurança") },
                        maxLines = 3,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Sentences,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = {
                                viewModel.dismissSuggestions()
                                focusManager.moveFocus(FocusDirection.Down)
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.designacaoSuggestions.isNotEmpty()) {
                        CatalogSuggestionsCard(
                            suggestions = uiState.designacaoSuggestions,
                            onSelect = { item ->
                                viewModel.onSelectCatalogItem(item)
                                focusManager.clearFocus()
                            }
                        )
                    }

                    // Field 3: Quantidade
                    OutlinedTextField(
                        value = uiState.draftQuantity,
                        onValueChange = viewModel::onQuantityChanged,
                        label = { Text("3. Quantidade *") },
                        placeholder = { Text("Ex.: 1 ou 2,5") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Field 4: Código de barras (independent from Referência!)
                    OutlinedTextField(
                        value = uiState.draftBarcode,
                        onValueChange = viewModel::onBarcodeChanged,
                        label = { Text("4. Código de barras") },
                        placeholder = { Text("Introduza ou leia com a câmara") },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    onOpenBarcodeScanner()
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = "Ler Código de Barras",
                                    tint = BluePrimary
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    FilledTonalButton(
                        onClick = {
                            keyboardController?.hide()
                            onOpenBarcodeScanner()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ler Código de Barras")
                    }

                    // Field 5: Fotografia do material (opcional e independente)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "5. Fotografia do material (opcional)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    val draftPhoto = uiState.draftPhotoPath
                    if (!draftPhoto.isNullOrBlank() && File(draftPhoto).exists()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                AsyncImage(
                                    model = File(draftPhoto),
                                    contentDescription = "Fotografia do material",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            keyboardController?.hide()
                                            onOpenPhotoCapture()
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Substituir")
                                    }
                                    OutlinedButton(
                                        onClick = viewModel::onRemoveDraftPhoto,
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Eliminar")
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                keyboardController?.hide()
                                onOpenPhotoCapture()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Tirar Fotografia")
                        }
                    }

                    if (uiState.materialError != null) {
                        Surface(
                            color = ErrorContainerLight,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.materialError.orEmpty(),
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (uiState.editingMaterialLocalId != null) {
                            OutlinedButton(
                                onClick = viewModel::cancelEditMaterial,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Cancelar")
                            }
                        }
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.saveDraftMaterial()
                            },
                            modifier = Modifier
                                .weight(1.4f)
                                .height(52.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BluePrimary,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (uiState.editingMaterialLocalId != null) {
                                    "Atualizar Material"
                                } else {
                                    "Guardar Material"
                                },
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // 3. Added Materials List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Materiais Adicionados (${uiState.addedMaterials.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = BlueDark
                    )

                    if (uiState.addedMaterials.isEmpty()) {
                        Text(
                            text = "Ainda não adicionou nenhum material a este registo.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.addedMaterials.forEachIndexed { index, item ->
                            AddedMaterialCard(
                                index = index + 1,
                                item = item,
                                onEdit = { viewModel.editMaterial(item) },
                                onDelete = { viewModel.deleteMaterial(item) }
                            )
                        }
                    }
                }
            }

            if (uiState.formError != null) {
                Surface(
                    color = ErrorContainerLight,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = uiState.formError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            // 4. Finalize & Generate PDF Button
            Button(
                onClick = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    viewModel.finalizeAndGeneratePdf()
                },
                enabled = !uiState.isSavingRecord,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueDark,
                    contentColor = Color.White
                )
            ) {
                if (uiState.isSavingRecord) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("A gravar e gerar PDF...")
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Gravar e Gerar PDF",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date Picker Dialog
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        val selectedMillis = datePickerState.selectedDateMillis
                        if (selectedMillis != null) {
                            val formatter = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "PT")).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            viewModel.onDateTextChanged(formatter.format(Date(selectedMillis)))
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Selecionar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Ambiguous designation conflict selector (multiple catalog references share same designation)
    if (uiState.ambiguousDesignacaoMatches.isNotEmpty()) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAmbiguousDialog,
            title = { Text("Várias Referências Encontradas") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Existem várias referências para esta designação. Selecione o artigo correto:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    uiState.ambiguousDesignacaoMatches.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = BlueContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.onSelectCatalogItem(item) }
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = item.referencia,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = BlueDark
                                )
                                Text(
                                    text = item.designacao,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::dismissAmbiguousDialog) {
                    Text("Manter manual")
                }
            }
        )
    }

    // Record Saved & PDF Ready Dialog
    val completed = uiState.completedRecord
    if (completed != null) {
        val pdfFile = completed.record.pdfFilePath?.let { File(it) }
        AlertDialog(
            onDismissRequest = {},
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = SuccessGreen,
                    modifier = Modifier.size(44.dp)
                )
            },
            title = { Text("Registo Guardado e PDF Criado") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "O registo da obra \"${completed.record.workOrder}\" foi guardado no dispositivo e o PDF foi gerado com sucesso.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (pdfFile != null) {
                        Surface(
                            color = SuccessContainerLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Ficheiro: ${pdfFile.name}\nDestinatário configurado: ${uiState.destinationEmail}",
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (pdfFile != null) {
                        Button(
                            onClick = {
                                ShareHelper.sharePdfByEmail(
                                    context = context,
                                    pdfFile = pdfFile,
                                    recipientEmail = uiState.destinationEmail,
                                    workOrder = completed.record.workOrder,
                                    technicianName = completed.record.technicianName,
                                    dateText = completed.record.dateText
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                        ) {
                            Icon(Icons.Default.Email, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Enviar por Email")
                        }

                        FilledTonalButton(
                            onClick = { ShareHelper.openPdf(context, pdfFile) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir PDF")
                        }
                    }

                    OutlinedButton(
                        onClick = onFinishToSavedRecords,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Concluir")
                    }
                }
            }
        )
    }
}

@Composable
private fun CatalogSuggestionsCard(
    suggestions: List<CatalogItemEntity>,
    onSelect: (CatalogItemEntity) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.35f)),
        color = BlueContainer.copy(alpha = 0.45f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Text(
                text = "Sugestões do catálogo (toque para preencher):",
                style = MaterialTheme.typography.labelSmall,
                color = BlueDark,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
            HorizontalDivider()
            suggestions.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(item) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.referencia,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = BlueDark
                        )
                        Text(
                            text = item.designacao,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
                if (idx < suggestions.lastIndex) {
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun AddedMaterialCard(
    index: Int,
    item: DraftMaterialRow,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
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
                    fontWeight = FontWeight.Medium
                )
                if (item.barcode.isNotBlank()) {
                    Text(
                        text = "Código de barras: ${item.barcode}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!item.photoPath.isNullOrBlank() && File(item.photoPath).exists()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 2.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Photo,
                            contentDescription = null,
                            tint = BluePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Com fotografia associada",
                            style = MaterialTheme.typography.labelSmall,
                            color = BluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Column {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Editar material",
                        tint = BluePrimary
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar material",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
