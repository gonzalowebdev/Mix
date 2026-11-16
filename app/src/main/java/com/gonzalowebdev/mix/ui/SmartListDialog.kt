package com.gonzalowebdev.mix.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.gonzalowebdev.mix.data.SmartList
import com.gonzalowebdev.mix.data.TypeFilter

@Composable
fun SmartListDialog(
    initial: SmartList?,
    folders: List<String>,
    onSave: (SmartList) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var type by remember { mutableStateOf(initial?.type ?: TypeFilter.ALL) }
    var folder by remember { mutableStateOf(initial?.folder) }
    var minText by remember { mutableStateOf(initial?.minMinutes?.takeIf { it > 0 }?.toString() ?: "") }
    var maxText by remember { mutableStateOf(initial?.maxMinutes?.takeIf { it > 0 }?.toString() ?: "") }
    var query by remember { mutableStateOf(initial?.query ?: "") }
    var favOnly by remember { mutableStateOf(initial?.favoritesOnly ?: false) }
    var folderMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nueva lista inteligente" else "Editar lista inteligente") },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = type == TypeFilter.ALL, onClick = { type = TypeFilter.ALL }, label = { Text("Todo") })
                    FilterChip(selected = type == TypeFilter.IMAGES, onClick = { type = TypeFilter.IMAGES }, label = { Text("Imágenes") })
                    FilterChip(selected = type == TypeFilter.VIDEOS, onClick = { type = TypeFilter.VIDEOS }, label = { Text("Videos") })
                }
                Column {
                    OutlinedButton(onClick = { folderMenu = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(folder ?: "Todas las carpetas", maxLines = 1)
                    }
                    DropdownMenu(expanded = folderMenu, onDismissRequest = { folderMenu = false }) {
                        DropdownMenuItem(
                            text = { Text("Todas las carpetas") },
                            onClick = {
                                folder = null
                                folderMenu = false
                            }
                        )
                        folders.forEach { f ->
                            DropdownMenuItem(
                                text = { Text(f) },
                                onClick = {
                                    folder = f
                                    folderMenu = false
                                }
                            )
                        }
                    }
                }
                if (type != TypeFilter.IMAGES) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minText,
                            onValueChange = { v -> minText = v.filter { it.isDigit() }.take(4) },
                            singleLine = true,
                            label = { Text("Mín. (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxText,
                            onValueChange = { v -> maxText = v.filter { it.isDigit() }.take(4) },
                            singleLine = true,
                            label = { Text("Máx. (min)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    label = { Text("El nombre contiene…") },
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Solo favoritos", modifier = Modifier.weight(1f).padding(end = 8.dp))
                    Switch(checked = favOnly, onCheckedChange = { favOnly = it })
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank(),
                onClick = {
                    onSave(
                        SmartList(
                            id = initial?.id ?: System.currentTimeMillis(),
                            name = name.trim(),
                            type = type,
                            folder = folder,
                            minMinutes = if (type == TypeFilter.IMAGES) 0 else (minText.toIntOrNull() ?: 0),
                            maxMinutes = if (type == TypeFilter.IMAGES) 0 else (maxText.toIntOrNull() ?: 0),
                            query = query.trim(),
                            favoritesOnly = favOnly
                        )
                    )
                }
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
