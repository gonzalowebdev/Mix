package com.gonzalowebdev.mix.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.gonzalowebdev.mix.data.PlaylistWithCount

fun formatDuration(ms: Long): String {
    val total = ms / 1000
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

@Composable
fun TextInputDialog(
    title: String,
    confirmLabel: String,
    initial: String = "",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                label = { Text("Nombre") }
            )
        },
        confirmButton = {
            TextButton(enabled = text.isNotBlank(), onClick = { onConfirm(text.trim()) }) {
                Text(confirmLabel)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@Composable
fun AddToPlaylistDialog(
    playlists: List<PlaylistWithCount>,
    onPick: (PlaylistWithCount) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newName by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Agregar a lista") },
        text = {
            Column {
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    singleLine = true,
                    label = { Text("Nueva lista") },
                    modifier = Modifier.fillMaxWidth()
                )
                if (newName.isNotBlank()) {
                    TextButton(onClick = { onCreate(newName.trim()) }) { Text("Crear y agregar") }
                }
                if (playlists.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text("O elegí una existente", style = MaterialTheme.typography.labelMedium)
                    LazyColumn(Modifier.heightIn(max = 240.dp)) {
                        items(playlists, key = { it.playlist.id }) { p ->
                            ListItem(
                                headlineContent = { Text(p.playlist.name) },
                                supportingContent = { Text("${p.itemCount} elementos") },
                                colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                                modifier = Modifier.clickable { onPick(p) }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

fun Context.findActivity(): Activity? {
    var c: Context = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}

fun shareUris(context: Context, uris: List<Uri>, allImages: Boolean, allVideos: Boolean) {
    if (uris.isEmpty()) return
    val intent = if (uris.size == 1) {
        Intent(Intent.ACTION_SEND).apply { putExtra(Intent.EXTRA_STREAM, uris[0]) }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
        }
    }
    intent.type = when {
        allImages -> "image/*"
        allVideos -> "video/*"
        else -> "*/*"
    }
    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(Intent.createChooser(intent, "Compartir"))
}

/**
 * Diálogo de eliminación: la papelera es la opción principal (se puede recuperar);
 * eliminar definitivamente queda como segunda opción.
 */
@Composable
fun DeleteDialog(
    count: Int,
    name: String?,
    onTrash: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val what = if (count == 1 && name != null) "\"$name\"" else "$count archivos"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (count == 1) "Eliminar archivo" else "Eliminar archivos") },
        text = {
            Column {
                Text(
                    "¿Qué querés hacer con $what?\n\n" +
                        "En la papelera se conserva unos 30 días y podés recuperarlo desde la Galería o el " +
                        "administrador de archivos.",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(onClick = onDelete) {
                    Text("Eliminar definitivamente", color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = { TextButton(onClick = onTrash) { Text("Mover a la papelera") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

enum class FileAction { TRASH, DELETE, RESTORE }

/**
 * Devuelve una función (uris, acción) que le pide al sistema mover a la papelera, eliminar
 * definitivamente o restaurar. Devuelve false si ninguno de los archivos se puede tocar desde Mix.
 */
@Composable
fun rememberFileAction(onResult: (List<Uri>, Boolean) -> Unit): (List<Uri>, FileAction) -> Boolean {
    val context = LocalContext.current
    val callback by rememberUpdatedState(onResult)
    var pending by remember { mutableStateOf<List<Uri>>(emptyList()) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        val uris = pending
        pending = emptyList()
        callback(uris, result.resultCode == Activity.RESULT_OK)
    }
    return remember(launcher) {
        { uris: List<Uri>, action: FileAction ->
            val media = uris.filter { it.authority == MediaStore.AUTHORITY }
            if (media.isEmpty()) {
                false
            } else {
                if (Build.VERSION.SDK_INT >= 30) {
                    pending = media
                    val sender = when (action) {
                        FileAction.DELETE -> MediaStore.createDeleteRequest(context.contentResolver, media).intentSender
                        FileAction.TRASH -> MediaStore.createTrashRequest(context.contentResolver, media, true).intentSender
                        FileAction.RESTORE -> MediaStore.createTrashRequest(context.contentResolver, media, false).intentSender
                    }
                    launcher.launch(IntentSenderRequest.Builder(sender).build())
                } else {
                    var ok = true
                    if (action != FileAction.RESTORE) {
                        for (u in media) {
                            try {
                                context.contentResolver.delete(u, null, null)
                            } catch (e: Exception) {
                                ok = false
                            }
                        }
                    }
                    callback(media, ok)
                }
                true
            }
        }
    }
}

/** Atajo: papelera o eliminación definitiva. */
@Composable
fun rememberFileRemover(onResult: (List<Uri>, Boolean) -> Unit): (List<Uri>, Boolean) -> Boolean {
    val act = rememberFileAction(onResult)
    return remember(act) {
        { uris: List<Uri>, permanent: Boolean ->
            act(uris, if (permanent) FileAction.DELETE else FileAction.TRASH)
        }
    }
}
