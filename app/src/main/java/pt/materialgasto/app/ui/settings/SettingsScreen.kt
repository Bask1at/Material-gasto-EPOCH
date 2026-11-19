package pt.materialgasto.app.ui.settings

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pt.materialgasto.app.BuildConfig
import pt.materialgasto.app.data.local.entity.TechnicianEntity
import pt.materialgasto.app.ui.theme.BlueContainer
import pt.materialgasto.app.ui.theme.BlueDark
import pt.materialgasto.app.ui.theme.BluePrimary
import pt.materialgasto.app.ui.theme.ErrorContainerLight
import pt.materialgasto.app.ui.theme.SuccessContainerLight
import pt.materialgasto.app.ui.theme.SuccessGreen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val technicians by viewModel.technicians.collectAsStateWithLifecycle()
    val liveCatalogCount by viewModel.catalogCount.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showAddTechDialog by remember { mutableStateOf(false) }
    var techToEdit by remember { mutableStateOf<TechnicianEntity?>(null) }
    var techToDelete by remember { mutableStateOf<TechnicianEntity?>(null) }

    val excelPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.onExcelFilePicked(context, uri)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text("Definições") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BluePrimary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Destination Email Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Email, contentDescription = null, tint = BluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Email de Destino",
                            style = MaterialTheme.typography.titleMedium,
                            color = BlueDark
                        )
                    }

                    OutlinedTextField(
                        value = uiState.emailInput,
                        onValueChange = viewModel::onEmailChanged,
                        label = { Text("Endereço de email para envio dos PDFs") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (uiState.emailStatusMessage != null) {
                        Surface(
                            color = if (uiState.emailIsError) ErrorContainerLight else SuccessContainerLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.emailStatusMessage.orEmpty(),
                                color = if (uiState.emailIsError) MaterialTheme.colorScheme.error else SuccessGreen,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = viewModel::saveDestinationEmail,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Guardar Email")
                    }
                }
            }

            // 2. Technicians Management Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = BluePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Gestão de Técnicos",
                                style = MaterialTheme.typography.titleMedium,
                                color = BlueDark
                            )
                        }

                        OutlinedButton(onClick = { showAddTechDialog = true }) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Adicionar")
                        }
                    }

                    if (technicians.isEmpty()) {
                        Text(
                            text = "Nenhum técnico configurado.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        technicians.forEachIndexed { idx, tech ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tech.name,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = { techToEdit = tech }) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar técnico",
                                        tint = BluePrimary
                                    )
                                }
                                IconButton(onClick = { techToDelete = tech }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Eliminar técnico",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            if (idx < technicians.lastIndex) {
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }

            // 3. Materials Catalog (Excel Import) Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, tint = BluePrimary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Catálogo de Materiais",
                            style = MaterialTheme.typography.titleMedium,
                            color = BlueDark
                        )
                    }

                    Text(
                        text = "Importe um ficheiro Excel (.xlsx ou .xls) com as colunas Referência e Designação para preenchimento automático offline.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Surface(
                        color = BlueContainer.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BluePrimary.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val hasCatalog = liveCatalogCount > 0 || uiState.catalogLastImportedMillis != null
                            Text(
                                text = if (hasCatalog) "Estado: Catálogo importado" else "Estado: Sem catálogo importado",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = BlueDark
                            )
                            Text(
                                text = "Artigos disponíveis: $liveCatalogCount",
                                style = MaterialTheme.typography.bodySmall
                            )
                            val lastMillis = uiState.catalogLastImportedMillis
                            if (lastMillis != null) {
                                val formattedDate = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "PT"))
                                    .format(Date(lastMillis))
                                Text(
                                    text = "Última importação: $formattedDate",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                            if (!uiState.catalogFileName.isNullOrBlank()) {
                                Text(
                                    text = "Ficheiro: ${uiState.catalogFileName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    if (uiState.importFeedbackMessage != null) {
                        Surface(
                            color = if (uiState.importFeedbackIsError) ErrorContainerLight else SuccessContainerLight,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = uiState.importFeedbackMessage.orEmpty(),
                                color = if (uiState.importFeedbackIsError) MaterialTheme.colorScheme.error else SuccessGreen,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }

                    Button(
                        onClick = {
                            excelPickerLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (liveCatalogCount > 0) "Atualizar Catálogo Excel" else "Importar Ficheiro Excel"
                        )
                    }
                }
            }

            // 4. App Version Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = BluePrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Versão da Aplicação",
                            style = MaterialTheme.typography.titleSmall,
                            color = BlueDark
                        )
                        Text(
                            text = "Material Gasto v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Add Technician Dialog
    if (showAddTechDialog) {
        TechnicianNameDialog(
            title = "Adicionar Técnico",
            initialName = "",
            confirmLabel = "Adicionar",
            onConfirm = { name ->
                viewModel.addTechnician(name)
                showAddTechDialog = false
            },
            onDismiss = { showAddTechDialog = false }
        )
    }

    // Edit Technician Dialog
    val editingTech = techToEdit
    if (editingTech != null) {
        TechnicianNameDialog(
            title = "Editar Técnico",
            initialName = editingTech.name,
            confirmLabel = "Guardar",
            onConfirm = { newName ->
                viewModel.updateTechnician(editingTech, newName)
                techToEdit = null
            },
            onDismiss = { techToEdit = null }
        )
    }

    // Delete Technician Confirmation Dialog
    val deletingTech = techToDelete
    if (deletingTech != null) {
        AlertDialog(
            onDismissRequest = { techToDelete = null },
            title = { Text("Eliminar Técnico") },
            text = { Text("Pretende eliminar \"${deletingTech.name}\" da lista de técnicos?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTechnician(deletingTech)
                        techToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Eliminar")
                }
            },
            dismissButton = {
                TextButton(onClick = { techToDelete = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Excel Import Workflow Dialogs
    when (val dialogState = uiState.importDialogState) {
        ExcelImportDialogState.Hidden -> Unit

        ExcelImportDialogState.Loading -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text("A processar Excel") },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                        Text("A ler e validar o ficheiro selecionado...")
                    }
                },
                confirmButton = {}
            )
        }

        is ExcelImportDialogState.ColumnMapping -> {
            ExcelColumnMappingDialog(
                state = dialogState,
                onUpdate = viewModel::updateColumnMapping,
                onConfirm = viewModel::confirmColumnMapping,
                onDismiss = viewModel::dismissImportDialog
            )
        }

        is ExcelImportDialogState.ConflictResolution -> {
            ExcelConflictResolutionDialog(
                state = dialogState,
                onSelectDesignation = viewModel::selectConflictResolution,
                onConfirm = viewModel::confirmConflictsResolved,
                onDismiss = viewModel::dismissImportDialog
            )
        }

        is ExcelImportDialogState.Confirmation -> {
            AlertDialog(
                onDismissRequest = viewModel::dismissImportDialog,
                title = { Text("Confirmar Importação") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Ficheiro: ${dialogState.fileName}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Serão importados ${dialogState.finalItems.size} artigo(s) para o catálogo local.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        if (dialogState.duplicateCount > 0) {
                            Text(
                                text = "• ${dialogState.duplicateCount} linha(s) duplicada(s) ignorada(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (dialogState.skippedIncompleteCount > 0) {
                            Text(
                                text = "• ${dialogState.skippedIncompleteCount} linha(s) incompleta(s) ignorada(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (dialogState.resolvedConflictCount > 0) {
                            Text(
                                text = "• ${dialogState.resolvedConflictCount} conflito(s) resolvido(s)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Os registos de obras já guardados não serão alterados.",
                            style = MaterialTheme.typography.bodySmall,
                            color = BlueDark
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = viewModel::commitCatalogImport,
                        colors = ButtonDefaults.buttonColors(containerColor = BluePrimary)
                    ) {
                        Text("Importar Catálogo")
                    }
                },
                dismissButton = {
                    TextButton(onClick = viewModel::dismissImportDialog) {
                        Text("Cancelar")
                    }
                }
            )
        }
    }
}

@Composable
private fun TechnicianNameDialog(
    title: String,
    initialName: String,
    confirmLabel: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember(initialName) { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nome do Técnico") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank()
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExcelColumnMappingDialog(
    state: ExcelImportDialogState.ColumnMapping,
    onUpdate: (refCol: Int, descCol: Int, hasHeaderRow: Boolean) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val colCount = state.spreadsheet.maxColumns
    val firstRow = state.spreadsheet.rows.firstOrNull().orEmpty()
    val sampleRow = state.spreadsheet.rows.getOrNull(1).orEmpty()

    fun columnLabel(index: Int): String {
        val letter = ('A' + index).toString()
        val preview = firstRow.getOrNull(index).orEmpty().ifBlank {
            sampleRow.getOrNull(index).orEmpty()
        }
        return if (preview.isNotBlank()) "Coluna $letter ($preview)" else "Coluna $letter"
    }

    var refExpanded by remember { mutableStateOf(false) }
    var descExpanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Selecionar Colunas do Excel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Os cabeçalhos \"Referência\" e \"Designação\" não foram reconhecidos automaticamente. Indique qual coluna corresponde a cada campo:",
                    style = MaterialTheme.typography.bodySmall
                )

                ExposedDropdownMenuBox(
                    expanded = refExpanded,
                    onExpandedChange = { refExpanded = it }
                ) {
                    OutlinedTextField(
                        value = columnLabel(state.selectedRefColumn),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Coluna de Referência") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = refExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = refExpanded,
                        onDismissRequest = { refExpanded = false }
                    ) {
                        for (c in 0 until colCount) {
                            DropdownMenuItem(
                                text = { Text(columnLabel(c)) },
                                onClick = {
                                    onUpdate(c, state.selectedDescColumn, state.hasHeaderRow)
                                    refExpanded = false
                                }
                            )
                        }
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = descExpanded,
                    onExpandedChange = { descExpanded = it }
                ) {
                    OutlinedTextField(
                        value = columnLabel(state.selectedDescColumn),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Coluna de Designação") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = descExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true)
                    )
                    ExposedDropdownMenu(
                        expanded = descExpanded,
                        onDismissRequest = { descExpanded = false }
                    ) {
                        for (c in 0 until colCount) {
                            DropdownMenuItem(
                                text = { Text(columnLabel(c)) },
                                onClick = {
                                    onUpdate(state.selectedRefColumn, c, state.hasHeaderRow)
                                    descExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable {
                        onUpdate(state.selectedRefColumn, state.selectedDescColumn, !state.hasHeaderRow)
                    }
                ) {
                    Checkbox(
                        checked = state.hasHeaderRow,
                        onCheckedChange = { checked ->
                            onUpdate(state.selectedRefColumn, state.selectedDescColumn, checked)
                        }
                    )
                    Text(
                        text = "A primeira linha é cabeçalho",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = state.selectedRefColumn != state.selectedDescColumn
            ) {
                Text("Continuar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ExcelConflictResolutionDialog(
    state: ExcelImportDialogState.ConflictResolution,
    onSelectDesignation: (String, String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Referências Repetidas Detetadas") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Foram encontradas referências repetidas com designações diferentes. Escolha qual designação deve ser mantida para cada referência:",
                    style = MaterialTheme.typography.bodySmall
                )
                state.analysis.conflicts.forEach { conflict ->
                    val chosen = state.selectedDesignations[conflict.normalizedReferencia]
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "Referência: ${conflict.referenciaDisplay}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = BlueDark
                            )
                            conflict.designations.forEach { option ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            onSelectDesignation(conflict.normalizedReferencia, option)
                                        }
                                ) {
                                    RadioButton(
                                        selected = chosen == option,
                                        onClick = {
                                            onSelectDesignation(conflict.normalizedReferencia, option)
                                        }
                                    )
                                    Text(
                                        text = option,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Confirmar Escolhas")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
