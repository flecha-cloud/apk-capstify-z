package com.capstify.adminz

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class CapstifyTheme(
    val background: Long = 0xFF101218,
    val panel: Long = 0xFF1B1F29,
    val panelAlt: Long = 0xFF232936,
    val text: Long = 0xFFFFFFFF,
    val muted: Long = 0xFFAAA8B6,
    val accent: Long = 0xFF9BCB45,
    val font: String = "Default",
    val backgroundImage: String = ""
)

private const val THEME_PREFS = "capstify_theme_settings"

fun loadCapstifyTheme(context: Context): CapstifyTheme {
    val p = context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE)
    return CapstifyTheme(
        background = p.getLong("background", 0xFF101218),
        panel = p.getLong("panel", 0xFF1B1F29),
        panelAlt = p.getLong("panelAlt", 0xFF232936),
        text = p.getLong("text", 0xFFFFFFFF),
        muted = p.getLong("muted", 0xFFAAA8B6),
        accent = p.getLong("accent", 0xFF9BCB45),
        font = p.getString("font", "Default") ?: "Default",
        backgroundImage = p.getString("backgroundImage", "") ?: ""
    )
}

fun saveCapstifyTheme(context: Context, theme: CapstifyTheme) {
    context.getSharedPreferences(THEME_PREFS, Context.MODE_PRIVATE).edit()
        .putLong("background", theme.background)
        .putLong("panel", theme.panel)
        .putLong("panelAlt", theme.panelAlt)
        .putLong("text", theme.text)
        .putLong("muted", theme.muted)
        .putLong("accent", theme.accent)
        .putString("font", theme.font)
        .putString("backgroundImage", theme.backgroundImage)
        .apply()
}

@Composable
fun CapstifyThemeSettings(
    theme: CapstifyTheme,
    onThemeChange: (CapstifyTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var bg by remember(theme.background) { mutableStateOf(theme.background.toHex()) }
    var panel by remember(theme.panel) { mutableStateOf(theme.panel.toHex()) }
    var panelAlt by remember(theme.panelAlt) { mutableStateOf(theme.panelAlt.toHex()) }
    var textColor by remember(theme.text) { mutableStateOf(theme.text.toHex()) }
    var muted by remember(theme.muted) { mutableStateOf(theme.muted.toHex()) }
    var accent by remember(theme.accent) { mutableStateOf(theme.accent.toHex()) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            onThemeChange(theme.copy(backgroundImage = uri.toString()))
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Pengaturan Tampilan", fontSize = 21.sp, color = Color(theme.text))
        Text("Tema tersimpan otomatis dan tetap digunakan saat aplikasi dibuka kembali.",
            fontSize = 12.sp, color = Color(theme.muted))
        ThemeColorField("Background", bg, { bg = it }) {
            bg.toColorLong()?.let { onThemeChange(theme.copy(background = it)) }
        }
        ThemeColorField("Panel / kartu", panel, { panel = it }) {
            panel.toColorLong()?.let { onThemeChange(theme.copy(panel = it)) }
        }
        ThemeColorField("Panel sekunder", panelAlt, { panelAlt = it }) {
            panelAlt.toColorLong()?.let { onThemeChange(theme.copy(panelAlt = it)) }
        }
        ThemeColorField("Warna teks", textColor, { textColor = it }) {
            textColor.toColorLong()?.let { onThemeChange(theme.copy(text = it)) }
        }
        ThemeColorField("Teks sekunder", muted, { muted = it }) {
            muted.toColorLong()?.let { onThemeChange(theme.copy(muted = it)) }
        }
        ThemeColorField("Warna aksen", accent, { accent = it }) {
            accent.toColorLong()?.let { onThemeChange(theme.copy(accent = it)) }
        }

        var fontMenu by remember { mutableStateOf(false) }
        Text("Font aplikasi", color = Color(theme.text))
        Box {
            OutlinedButton(onClick = { fontMenu = true }) {
                Text(theme.font, color = Color(theme.text))
            }
            DropdownMenu(expanded = fontMenu, onDismissRequest = { fontMenu = false }) {
                listOf("Default", "Serif", "Monospace", "Cursive").forEach { item ->
                    DropdownMenuItem(text = { Text(item) }, onClick = {
                        onThemeChange(theme.copy(font = item))
                        fontMenu = false
                    })
                }
            }
        }

        Text("Gambar latar belakang", color = Color(theme.text))
        OutlinedButton(onClick = { picker.launch(arrayOf("image/*")) }) {
            Text("Pilih gambar dari galeri")
        }
        if (theme.backgroundImage.isNotBlank()) {
            Text("Gambar latar sudah dipilih.", color = Color(theme.accent), fontSize = 12.sp)
            TextButton(onClick = { onThemeChange(theme.copy(backgroundImage = "")) }) {
                Text("Hapus gambar latar")
            }
        }
        Button(onClick = { onThemeChange(CapstifyTheme()) }) { Text("Reset default") }
        Card(colors = CardDefaults.cardColors(containerColor = Color(theme.panel)),
            shape = RoundedCornerShape(14.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Preview tampilan", color = Color(theme.text), fontSize = 18.sp)
                Text("Contoh teks sekunder", color = Color(theme.muted), fontSize = 12.sp)
                Button(onClick = {}, colors = ButtonDefaults.buttonColors(
                    containerColor = Color(theme.accent), contentColor = Color.Black
                )) { Text("Contoh tombol") }
            }
        }
    }
}

@Composable
private fun ThemeColorField(
    label: String, value: String, onValue: (String) -> Unit, onApply: () -> Unit
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(value = value, onValueChange = onValue,
            label = { Text("$label (#RRGGBB)") }, singleLine = true,
            modifier = Modifier.weight(1f))
        Button(onClick = onApply, modifier = Modifier.padding(top = 8.dp)) { Text("Pakai") }
    }
}

private fun Long.toHex(): String = "#%06X".format(this and 0xFFFFFF)
private fun String.toColorLong(): Long? {
    val raw = trim().removePrefix("#")
    if (raw.length != 6 && raw.length != 8) return null
    return try {
        val parsed = raw.toLong(16)
        if (raw.length == 6) (0xFF000000L or parsed) else parsed
    } catch (_: Exception) { null }
}
