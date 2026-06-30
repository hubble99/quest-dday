package com.questdday.ui.screen.create

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.questdday.ui.theme.Spacing
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateQuestScreen(
    viewModel: CreateQuestViewModel,
    onNavigateBack: () -> Unit
) {
    val state by viewModel.formState.collectAsState()
    val attributesList by viewModel.attributes.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.isContainer) "Buat Epic Quest" else "Buat Quest Baru",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md)
        ) {
            // Parenting context banner
            if ((state.isAwaitingFirstSubQuest || state.parentQuestId != null) && state.parentQuestTitle != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.md)
                ) {
                    Text(
                        text = "Menambah sub-quest untuk: ${state.parentQuestTitle}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(Spacing.md)
                    )
                }
            }

            // Section 1 - Dasar
            Text(
                text = "Informasi Dasar",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )

            // Title
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.updateTitle(it) },
                label = { Text("Judul") },
                isError = state.errors.containsKey("title"),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            if (state.errors.containsKey("title")) {
                Text(
                    text = state.errors["title"] ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs, bottom = Spacing.sm)
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            // Description
            OutlinedTextField(
                value = state.description,
                onValueChange = { viewModel.updateDescription(it) },
                label = { Text("Deskripsi (Opsional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            Spacer(modifier = Modifier.height(Spacing.sm))

            // Attribute Selection
            var dropdownExpanded by remember { mutableStateOf(false) }
            val selectedAttribute = attributesList.find { it.id == state.selectedAttributeId }

            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedAttribute?.let { "${it.icon ?: ""} ${it.displayName}" } ?: "Pilih Atribut",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Atribut") },
                    isError = state.errors.containsKey("selectedAttributeId"),
                    trailingIcon = {
                        IconButton(onClick = { dropdownExpanded = true }) {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Pilih Atribut")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { dropdownExpanded = true }
                )
                DropdownMenu(
                    expanded = dropdownExpanded,
                    onDismissRequest = { dropdownExpanded = false },
                    modifier = Modifier.fillMaxWidth(0.9f)
                ) {
                    attributesList.forEach { attr ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    text = "${attr.icon ?: ""} ${attr.displayName}",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            onClick = {
                                viewModel.selectAttribute(attr.id)
                                dropdownExpanded = false
                            }
                        )
                    }
                }
            }
            if (state.errors.containsKey("selectedAttributeId")) {
                Text(
                    text = state.errors["selectedAttributeId"] ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs, bottom = Spacing.sm)
                )
            } else {
                Spacer(modifier = Modifier.height(Spacing.sm))
            }

            // Epic toggle
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.sm)
            ) {
                Text(
                    text = "Epic Quest dengan sub-quest?",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Switch(
                    checked = state.isContainer,
                    onCheckedChange = { viewModel.setIsContainer(it) },
                    enabled = !state.isAwaitingFirstSubQuest && state.parentQuestId == null
                )
            }

            // Section 2 - Mode Penyelesaian (Hidden for Epic Containers)
            AnimatedVisibility(
                visible = !state.isContainer,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
                    Text(
                        text = "Mode Penyelesaian",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = state.completionMode == "instant",
                            onClick = { viewModel.setCompletionMode("instant") }
                        )
                        Text(
                            text = "Instant Complete",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = Spacing.md)
                        )
                        
                        RadioButton(
                            selected = state.completionMode == "timer",
                            onClick = { viewModel.setCompletionMode("timer") }
                        )
                        Text(
                            text = "Timer",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (state.errors.containsKey("completionMode")) {
                        Text(
                            text = state.errors["completionMode"] ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
                        )
                    }

                    AnimatedVisibility(visible = state.completionMode == "timer") {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                            OutlinedTextField(
                                value = (state.targetDurationSeconds?.let { it / 60 } ?: "").toString(),
                                onValueChange = { 
                                    val minutes = it.toIntOrNull() ?: 0
                                    viewModel.setTargetDuration(minutes * 60)
                                },
                                label = { Text("Durasi Target (menit)") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                isError = state.errors.containsKey("targetDurationSeconds"),
                                modifier = Modifier.fillMaxWidth()
                            )
                            if (state.errors.containsKey("targetDurationSeconds")) {
                                Text(
                                    text = state.errors["targetDurationSeconds"] ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
                                )
                            }
                        }
                    }
                }
            }

            // Section 3 - Jadwal (Hidden for Epic Containers)
            AnimatedVisibility(
                visible = !state.isContainer,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
                    Text(
                        text = "Jadwal",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = state.scheduleType == "daily",
                            onClick = { viewModel.setScheduleType("daily") }
                        )
                        Text(
                            text = "Setiap Hari",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = Spacing.md)
                        )
                        
                        RadioButton(
                            selected = state.scheduleType == "custom_days",
                            onClick = { viewModel.setScheduleType("custom_days") }
                        )
                        Text(
                            text = "Hari Tertentu",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    AnimatedVisibility(visible = state.scheduleType == "custom_days") {
                        Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                            Text(
                                text = "Pilih Hari:",
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(bottom = Spacing.sm)
                            )
                            val dayNames = listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min")
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                for (i in 1..7) {
                                    val isSelected = state.scheduleDays.contains(i)
                                    val containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                    
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(containerColor)
                                            .clickable { viewModel.toggleScheduleDay(i) }
                                    ) {
                                        Text(
                                            text = dayNames[i - 1],
                                            color = contentColor,
                                            style = MaterialTheme.typography.bodyMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (state.errors.containsKey("scheduleDays")) {
                                Text(
                                    text = state.errors["scheduleDays"] ?: "",
                                    color = MaterialTheme.colorScheme.error,
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
                                )
                            }
                        }
                    }
                }
            }

            // Section 4 - Masa Aktif (Always visible)
            HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
            Text(
                text = "Masa Aktif",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = Spacing.sm)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start,
                modifier = Modifier.fillMaxWidth()
            ) {
                RadioButton(
                    selected = state.durationType == "endless",
                    onClick = { viewModel.setDurationType("endless") }
                )
                Text(
                    text = "Tanpa Batas (Endless)",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(end = Spacing.md)
                )
                
                RadioButton(
                    selected = state.durationType == "time_bound",
                    onClick = { viewModel.setDurationType("time_bound") }
                )
                Text(
                    text = "Berjangka",
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (state.errors.containsKey("durationType")) {
                Text(
                    text = state.errors["durationType"] ?: "",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
                )
            }

            AnimatedVisibility(visible = state.durationType == "time_bound") {
                Column(modifier = Modifier.fillMaxWidth().padding(top = Spacing.sm)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = state.durationInputType == "days",
                            onClick = { viewModel.setDurationInputType("days") }
                        )
                        Text(
                            text = "Jumlah Hari",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = Spacing.md)
                        )
                        
                        RadioButton(
                            selected = state.durationInputType == "date",
                            onClick = { viewModel.setDurationInputType("date") }
                        )
                        Text(
                            text = "Input Tanggal",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (state.parentEndDate != null) {
                        Text(
                            text = "Batas maksimal: ${state.parentEndDate}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(vertical = Spacing.xs)
                        )
                    }

                    if (state.durationInputType == "days") {
                        OutlinedTextField(
                            value = state.targetDays?.toString() ?: "",
                            onValueChange = { 
                                viewModel.setTargetDays(it.toIntOrNull() ?: 0)
                            },
                            label = { Text("Jumlah Hari") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = state.errors.containsKey("targetDays"),
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.errors.containsKey("targetDays")) {
                            Text(
                                text = state.errors["targetDays"] ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
                            )
                        }
                    } else {
                        var showDatePicker by remember { mutableStateOf(false) }
                        
                        OutlinedTextField(
                            value = state.endDate ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Tanggal Akhir (YYYY-MM-DD)") },
                            isError = state.errors.containsKey("endDate"),
                            trailingIcon = {
                                IconButton(onClick = { showDatePicker = true }) {
                                    Icon(imageVector = Icons.Default.DateRange, contentDescription = "Calendar")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        if (state.errors.containsKey("endDate")) {
                            Text(
                                text = state.errors["endDate"] ?: "",
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
                            )
                        }

                        if (showDatePicker) {
                            val today = java.util.Calendar.getInstance()
                            val dialog = android.app.DatePickerDialog(
                                context,
                                { _, year, month, dayOfMonth ->
                                    val formattedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    viewModel.setEndDate(formattedDate)
                                    showDatePicker = false
                                },
                                today.get(java.util.Calendar.YEAR),
                                today.get(java.util.Calendar.MONTH),
                                today.get(java.util.Calendar.DAY_OF_MONTH)
                            )
                            
                            state.parentEndDate?.let { parentEndStr ->
                                try {
                                    val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
                                    val parentLocalDate = java.time.LocalDate.parse(parentEndStr, formatter)
                                    val zoneId = java.time.ZoneId.systemDefault()
                                    val epochMillis = parentLocalDate.atStartOfDay(zoneId).toInstant().toEpochMilli()
                                    dialog.datePicker.maxDate = epochMillis
                                } catch (e: Exception) {
                                    // Ignore parsing issue
                                }
                            }
                            
                            dialog.setOnDismissListener { showDatePicker = false }
                            dialog.show()
                        }
                    }
                }
            }

            // Section 5 - Mode Bolos (Hidden if duration type is endless or quest is container)
            AnimatedVisibility(
                visible = state.durationType == "time_bound" && !state.isContainer,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = Spacing.md))
                    Text(
                        text = "Mode Bolos (Absence Mode)",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = Spacing.sm)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        RadioButton(
                            selected = state.absenceMode == "shift",
                            onClick = { viewModel.setAbsenceMode("shift") }
                        )
                        Text(
                            text = "Geser Jadwal (Shift)",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(end = Spacing.md)
                        )
                        
                        RadioButton(
                            selected = state.absenceMode == "stack",
                            onClick = { viewModel.setAbsenceMode("stack") }
                        )
                        Text(
                            text = "Tumpuk Tugas (Stack)",
                            style = MaterialTheme.typography.bodyLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (state.errors.containsKey("absenceMode")) {
                        Text(
                            text = state.errors["absenceMode"] ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = Spacing.xs, top = Spacing.xs)
                        )
                    }

                    Text(
                        text = when (state.absenceMode) {
                            "shift" -> "Mode Shift: Jika sesi terlewat, tanggal akhir quest digeser ke sesi terjadwal berikutnya."
                            "stack" -> "Mode Stack: Jika sesi terlewat, sisa durasi akan ditumpuk ke sesi berikutnya."
                            else -> "Pilih mode penyelesaian sesi bolos."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = Spacing.sm, start = Spacing.xs)
                    )
                }
            }

            // Save / Submit Button
            val isButtonEnabled = state.errors.isEmpty() && state.title.isNotBlank() && state.selectedAttributeId != null
            Button(
                onClick = {
                    scope.launch {
                        val success = viewModel.validateAndSave()
                        if (success) {
                            onNavigateBack()
                        }
                    }
                },
                enabled = isButtonEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = Spacing.lg)
            ) {
                Text(
                    text = if (state.isContainer) "Lanjut Tambah Sub-Quest" else "Simpan Quest",
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
