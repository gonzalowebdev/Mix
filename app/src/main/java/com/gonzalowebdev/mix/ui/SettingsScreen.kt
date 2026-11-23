package com.gonzalowebdev.mix.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    vm: MixViewModel,
    onBack: () -> Unit,
    onCleanup: () -> Unit,
    onTrash: () -> Unit
) {
    val context = LocalContext.current
    val prefs = vm.prefs
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var muted by remember { mutableStateOf(prefs.muted) }
    var feedMode by remember { mutableStateOf(prefs.feedMode) }
    var autoUpdate by remember { mutableStateOf(prefs.autoUpdateCheck) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            vm.exportTo(uri, includePrefs = true) { ok ->
                scope.launch { snackbar.showSnackbar(if (ok) "Respaldo exportado" else "No se pudo exportar") }
            }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            vm.importFrom(uri) { n ->
                scope.launch { snackbar.showSnackbar(if (n >= 0) "Respaldo importado" else "El archivo no es válido") }
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text("Ajustes", style = MaterialTheme.typography.headlineSmall) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(MixIcons.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            SettingsHeader("Reproductor")
            ListItem(
                headlineContent = { Text("Videos sin sonido por defecto") },
                supportingContent = { Text("Podés activarlo en cada video desde el reproductor.") },
                trailingContent = {
                    Switch(checked = muted, onCheckedChange = {
                        muted = it
                        prefs.muted = it
                    })
                }
            )
            ListItem(
                headlineContent = { Text("Modo feed por defecto") },
                supportingContent = { Text("Sin avance automático; los videos se repiten en bucle.") },
                trailingContent = {
                    Switch(checked = feedMode, onCheckedChange = {
                        feedMode = it
                        prefs.feedMode = it
                    })
                }
            )
            ListItem(
                headlineContent = { Text("Mostrar de nuevo los consejos de gestos") },
                modifier = Modifier.clickable {
                    prefs.tipsSeen = false
                    scope.launch { snackbar.showSnackbar("Los vas a ver la próxima vez que abras el reproductor") }
                }
            )

            SettingsHeader("Biblioteca")
            ListItem(
                headlineContent = { Text("Limpieza") },
                supportingContent = { Text("Duplicados, capturas, fotos borrosas y videos pesados") },
                leadingContent = { Icon(MixIcons.Delete, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onCleanup)
            )
            ListItem(
                headlineContent = { Text("Papelera") },
                supportingContent = { Text("Restaurar o eliminar definitivamente") },
                leadingContent = { Icon(MixIcons.Folder, contentDescription = null) },
                modifier = Modifier.clickable(onClick = onTrash)
            )
            ListItem(
                headlineContent = { Text("Recuerdos ocultos") },
                supportingContent = {
                    Text(
                        if (vm.hiddenMemories.isEmpty()) "No ocultaste ninguno"
                        else "${vm.hiddenMemories.size} ocultos · tocá para volver a mostrarlos"
                    )
                },
                leadingContent = { Icon(MixIcons.AutoAwesome, contentDescription = null) },
                modifier = Modifier.clickable(enabled = vm.hiddenMemories.isNotEmpty()) {
                    vm.unhideAllMemories()
                    scope.launch { snackbar.showSnackbar("Se vuelven a mostrar los recuerdos") }
                }
            )

            SettingsHeader("Respaldo")
            ListItem(
                headlineContent = { Text("Exportar respaldo") },
                supportingContent = { Text("Listas, favoritos, estilos y ajustes en un archivo") },
                modifier = Modifier.clickable { exportLauncher.launch("mix-respaldo.json") }
            )
            ListItem(
                headlineContent = { Text("Importar respaldo") },
                supportingContent = { Text("Restaura los ajustes y agrega las listas del archivo") },
                modifier = Modifier.clickable { importLauncher.launch(arrayOf("*/*")) }
            )

            SettingsHeader("Actualizaciones")
            ListItem(
                headlineContent = { Text("Buscar actualizaciones") },
                supportingContent = {
                    Text(vm.updateStatus.ifEmpty { "Versión instalada: ${vm.currentBuild()}" })
                },
                trailingContent = {
                    if (vm.checkingUpdate) CircularProgressIndicator(Modifier.padding(4.dp), strokeWidth = 2.dp)
                },
                modifier = Modifier.clickable(enabled = !vm.checkingUpdate) { vm.checkUpdate(true) }
            )
            vm.pendingUpdate?.let { info ->
                ListItem(
                    headlineContent = { Text("Descargar ${info.tag}") },
                    supportingContent = { Text("Se abre en el navegador; después instalás el archivo") },
                    modifier = Modifier.clickable {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.url)))
                    }
                )
            }
            ListItem(
                headlineContent = { Text("Avisarme de nuevas versiones") },
                supportingContent = {
                    Text("Consulta GitHub una vez por día. Es la única conexión a internet de la app.")
                },
                trailingContent = {
                    Switch(checked = autoUpdate, onCheckedChange = {
                        autoUpdate = it
                        prefs.autoUpdateCheck = it
                    })
                }
            )

            SettingsHeader("Acerca de")
            ListItem(
                headlineContent = { Text("Mix") },
                supportingContent = { Text("Versión 1.${vm.currentBuild()} · compilación ${vm.currentBuild()}") }
            )
        }
    }
}

@Composable
private fun SettingsHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MixColors.Pink,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 4.dp)
    )
}
