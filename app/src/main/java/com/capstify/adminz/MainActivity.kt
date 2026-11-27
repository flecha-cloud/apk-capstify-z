package com.capstify.adminz

import android.os.Bundle
import android.content.Context
import android.net.Uri
import android.graphics.BitmapFactory
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

private var Bg by mutableStateOf(Color(0xFF101218))
private var Panel by mutableStateOf(Color(0xFF1B1F29))
private var Panel2 by mutableStateOf(Color(0xFF232936))
private var Accent by mutableStateOf(Color(0xFF9BCB45))
private var Muted by mutableStateOf(Color(0xFFAAA8B6))
private var Danger by mutableStateOf(Color(0xFFFF7777))
private var TextColor by mutableStateOf(Color.White)
private var SelectedFont by mutableStateOf("Default")
private var BackgroundImage by mutableStateOf("")

private val pages = listOf("Dashboard", "Produk & Kode", "Atur Stok", "Pesanan",
    "Tempat Sampah", "Rekap Penjualan", "Backup Data", "Pengaturan Tema")

private fun saveTheme(c: Context) {
    c.getSharedPreferences("capstify_theme", Context.MODE_PRIVATE).edit()
        .putLong("bg", Bg.value.toLong()).putLong("panel", Panel.value.toLong())
        .putLong("panel2", Panel2.value.toLong()).putLong("accent", Accent.value.toLong())
        .putLong("muted", Muted.value.toLong()).putLong("text", TextColor.value.toLong())
        .putString("font", SelectedFont).putString("background_image", BackgroundImage).apply()
}
private fun loadTheme(c: Context) {
    val p = c.getSharedPreferences("capstify_theme", Context.MODE_PRIVATE)
    fun color(k: String, d: Color) = Color(p.getLong(k, d.value.toLong()).toULong())
    Bg = color("bg", Color(0xFF101218)); Panel = color("panel", Color(0xFF1B1F29))
    Panel2 = color("panel2", Color(0xFF232936)); Accent = color("accent", Color(0xFF9BCB45))
    Muted = color("muted", Color(0xFFAAA8B6)); TextColor = color("text", Color.White)
    SelectedFont = p.getString("font", "Default") ?: "Default"
    BackgroundImage = p.getString("background_image", "") ?: ""
}
private fun resetTheme(c: Context) {
    Bg = Color(0xFF101218); Panel = Color(0xFF1B1F29); Panel2 = Color(0xFF232936)
    Accent = Color(0xFF9BCB45); Muted = Color(0xFFAAA8B6); TextColor = Color.White
    SelectedFont = "Default"; BackgroundImage = ""; saveTheme(c)
}
private fun fontFamily() = when (SelectedFont) {
    "Sans Serif" -> FontFamily.SansSerif
    "Serif" -> FontFamily.Serif
    "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
    else -> FontFamily.Default
}
private fun appTypography(): Typography {
    val base = Typography()
    val family = fontFamily()
    return Typography(
        displayLarge = base.displayLarge.copy(fontFamily = family),
        displayMedium = base.displayMedium.copy(fontFamily = family),
        displaySmall = base.displaySmall.copy(fontFamily = family),
        headlineLarge = base.headlineLarge.copy(fontFamily = family),
        headlineMedium = base.headlineMedium.copy(fontFamily = family),
        headlineSmall = base.headlineSmall.copy(fontFamily = family),
        titleLarge = base.titleLarge.copy(fontFamily = family),
        titleMedium = base.titleMedium.copy(fontFamily = family),
        titleSmall = base.titleSmall.copy(fontFamily = family),
        bodyLarge = base.bodyLarge.copy(fontFamily = family),
        bodyMedium = base.bodyMedium.copy(fontFamily = family),
        bodySmall = base.bodySmall.copy(fontFamily = family),
        labelLarge = base.labelLarge.copy(fontFamily = family),
        labelMedium = base.labelMedium.copy(fontFamily = family),
        labelSmall = base.labelSmall.copy(fontFamily = family)
    )
}

private fun money(n: Double) = String.format(Locale("id", "ID"), "%,.0f", n)
private fun JSONObject.array(k: String) = optJSONArray(k) ?: JSONArray()
private fun JSONArray.rows() = (0 until length()).mapNotNull { optJSONObject(it) }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        loadTheme(this)
        val api = CapstifyApi(this)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Bg, surface = Panel, primary = Accent, onPrimary = Color.Black,
                onBackground = TextColor, onSurface = TextColor, secondary = Muted,
                onSecondary = TextColor, error = Danger, onError = Color.Black,
                surfaceVariant = Panel2, onSurfaceVariant = TextColor
            ), typography = appTypography()) {
                App(api)
            }
        }
    }
}

@Composable
private fun App(api: CapstifyApi) {
    var loggedIn by remember { mutableStateOf(false) }
    var user by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("Dashboard") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var stats by remember { mutableStateOf(JSONObject()) }
    val scope = rememberCoroutineScope()

    fun loadDashboard() {
        scope.launch {
            busy = true
            try { stats = withContext(Dispatchers.IO) { api.get("/api/dashboard") }.optJSONObject("stats") ?: JSONObject() }
            catch (e: Exception) { message = e.message ?: "Gagal memuat dashboard" }
            busy = false
        }
    }
    BackHandler(loggedIn && page != "Dashboard") { page = "Dashboard" }
    LaunchedEffect(Unit) {
        try {
            val me = withContext(Dispatchers.IO) { api.get("/api/me") }
            user = me.optJSONObject("user")?.optString("name").orEmpty()
            loggedIn = true
            loadDashboard()
        } catch (_: Exception) { loggedIn = false }
    }

    Box(Modifier.fillMaxSize().background(Bg)) {
        ThemeBackground()
        if (!loggedIn) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
                Text("Capstify-Admin Z", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Accent)
                Text("Sales Management System", color = Muted)
                Spacer(Modifier.height(24.dp))
                OutlinedTextField(username, { username = it }, label = { Text("Username") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth(), singleLine = true)
                if (message.isNotBlank()) Text(message, color = Danger)
                Button(onClick = {
                    scope.launch {
                        busy = true; message = ""
                        try {
                            val r = withContext(Dispatchers.IO) { api.post("/api/login", JSONObject().put("username", username).put("password", password)) }
                            user = r.optJSONObject("user")?.optString("name").orEmpty()
                            loggedIn = true; password = ""; page = "Dashboard"; loadDashboard()
                        } catch (e: Exception) { message = e.message ?: "Login gagal" }
                        busy = false
                    }
                }, enabled = !busy && username.isNotBlank() && password.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Text(if (busy) "Memproses..." else "Masuk")
                }
            }
        } else {
            Column(Modifier.fillMaxSize().statusBarsPadding()) {
                Row(Modifier.fillMaxWidth().background(Panel).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(page, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("Capstify-Admin Z · $user", color = Muted, fontSize = 11.sp)
                    }
                    var menu by remember { mutableStateOf(false) }
                    Box {
                        IconButton(onClick = { menu = true }) { Icon(Icons.Default.Menu, "Menu") }
                        DropdownMenu(menu, { menu = false }) {
                            pages.forEach { p -> DropdownMenuItem(text = { Text(p) }, onClick = { page = p; menu = false; message = "" }) }
                            HorizontalDivider()
                            DropdownMenuItem(text = { Text("Keluar") }, onClick = {
                                menu = false
                                scope.launch {
                                    try { withContext(Dispatchers.IO) { api.post("/api/logout") } } catch (_: Exception) {}
                                    api.clearSession(); loggedIn = false; page = "Dashboard"
                                }
                            })
                        }
                    }
                }
                when (page) {
                    "Dashboard" -> Dashboard(stats, busy, message, { loadDashboard() }, { page = it })
                    "Produk & Kode" -> Products(api)
                    "Atur Stok" -> Stock(api)
                    "Pesanan" -> Orders(api, false)
                    "Tempat Sampah" -> Orders(api, true)
                    "Rekap Penjualan" -> Reports(api)
                    "Backup Data" -> Backup(api)
                    "Pengaturan Tema" -> ThemeSettings()
                }
            }
        }
    }
}

@Composable
private fun Dashboard(s: JSONObject, busy: Boolean, msg: String, refresh: () -> Unit, open: (String) -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ringkasan aktivitas operasional", Modifier.weight(1f), color = Muted)
            IconButton(onClick = refresh) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        val cards = listOf(
            Triple("Pesanan Hari Ini", s.optString("ordersToday", "—"), "Pesanan"),
            Triple("Total Produk", s.optString("totalProducts", "—"), "Produk & Kode"),
            Triple("Total Stok", s.optString("totalStock", "—"), "Atur Stok"),
            Triple("Pesanan Diproses", s.optString("pendingShipments", "—"), "Pesanan")
        )
        cards.chunked(2).forEach { group ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                group.forEach { (label, value, target) ->
                    Card(Modifier.weight(1f).padding(vertical = 4.dp).clickable { open(target) },
                        colors = CardDefaults.cardColors(containerColor = Panel)) {
                        Column(Modifier.padding(14.dp)) {
                            Text(label, color = Muted, fontSize = 12.sp)
                            Text(value, fontSize = 23.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth().padding(top = 8.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(16.dp)) {
                Text("Omzet Hari Ini", color = Muted)
                Text("Rp${money(s.optDouble("revenueToday"))}", fontSize = 24.sp, color = Accent, fontWeight = FontWeight.Bold)
            }
        }
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top = 8.dp))
        if (msg.isNotBlank()) Text(msg, color = Danger)
        Spacer(Modifier.height(14.dp))
        pages.drop(1).forEach { p ->
            Card(Modifier.fillMaxWidth().padding(vertical = 3.dp).clickable { open(p) },
                colors = CardDefaults.cardColors(containerColor = Panel)) {
                Row(Modifier.padding(15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(p, Modifier.weight(1f)); Icon(Icons.Default.ChevronRight, null, tint = Accent)
                }
            }
        }
    }
}

@Composable
private fun Products(api: CapstifyApi) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf(listOf<JSONObject>()) }
    var loading by remember { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }
    var search by remember { mutableStateOf("") }
    var sku by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
    var variant by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }
    var stock by remember { mutableStateOf("") }
    fun load() { scope.launch { loading = true; try {
        list = withContext(Dispatchers.IO) { api.get("/api/products") }.array("products").rows()
        msg = ""
    } catch (e: Exception) { msg = e.message ?: "Gagal memuat produk" }; loading = false } }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Produk & Kode", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "Muat ulang") }
        }
        OutlinedTextField(search, { search = it }, label = { Text("Cari SKU / nama") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(12.dp)) {
                Text("Tambah Produk", fontWeight = FontWeight.Bold)
                Field("SKU / Kode", sku, { sku = it })
                Field("Nama Produk", name, { name = it })
                Field("Varian", variant, { variant = it })
                Field("Harga", price, { price = it }, true)
                Field("Stok Awal", stock, { stock = it }, true)
                Button(onClick = { scope.launch {
                    try {
                        withContext(Dispatchers.IO) { api.post("/api/products", JSONObject().put("sku", sku).put("name", name).put("variant", variant).put("price", price.toDoubleOrNull() ?: 0).put("stock", stock.toIntOrNull() ?: 0)) }
                        sku = ""; name = ""; variant = ""; price = ""; stock = ""; msg = "Produk berhasil ditambahkan"; load()
                    } catch (e: Exception) { msg = e.message ?: "Gagal menyimpan" }
                } }, enabled = sku.isNotBlank() && name.isNotBlank(), modifier = Modifier.fillMaxWidth()) { Text("Simpan Produk") }
            }
        }
        if (msg.isNotBlank()) Text(msg, color = if (msg.contains("berhasil", true)) Accent else Danger)
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        LazyColumn {
            items(list.filter { it.optString("name").contains(search, true) || it.optString("sku").contains(search, true) }) { p ->
                Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(p.optString("name"), fontWeight = FontWeight.Bold)
                            Text("${p.optString("sku")} · ${p.optString("variant")}", color = Muted, fontSize = 12.sp)
                            Text("Rp${money(p.optDouble("price"))} · Stok ${p.optInt("stock")}", color = Accent)
                        }
                        IconButton(onClick = { scope.launch { try {
                            withContext(Dispatchers.IO) { api.delete("/api/products/${p.optInt("id")}") }; load()
                        } catch (e: Exception) { msg = e.message ?: "Gagal menghapus" } } }) { Icon(Icons.Default.Delete, "Hapus", tint = Danger) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Stock(api: CapstifyApi) {
    val scope = rememberCoroutineScope()
    var products by remember { mutableStateOf(listOf<JSONObject>()) }
    var selected by remember { mutableStateOf<JSONObject?>(null) }
    var mode by remember { mutableStateOf("set") }; var type by remember { mutableStateOf("in") }
    var amount by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    var msg by remember { mutableStateOf("") }; var loading by remember { mutableStateOf(false) }
    fun load() { scope.launch { try { products = withContext(Dispatchers.IO) { api.get("/api/products") }.array("products").rows() }
        catch (e: Exception) { msg = e.message ?: "Gagal memuat stok" } } }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Manajemen Stok", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        var expanded by remember { mutableStateOf(false) }
        Box {
            OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text(selected?.optString("name") ?: "Pilih Produk", Modifier.weight(1f))
                Icon(Icons.Default.ArrowDropDown, null)
            }
            DropdownMenu(expanded, { expanded = false }) {
                products.forEach { p -> DropdownMenuItem(text = { Text("${p.optString("sku")} · ${p.optString("name")} (stok ${p.optInt("stock")})") },
                    onClick = { selected = p; expanded = false }) }
            }
        }
        selected?.let { Text("Stok saat ini: ${it.optInt("stock")}", color = Accent, modifier = Modifier.padding(8.dp)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(mode == "set", { mode = "set" }, label = { Text("Atur Stok Akhir") })
            FilterChip(mode == "adjust", { mode = "adjust" }, label = { Text("Masuk / Keluar") })
        }
        if (mode == "adjust") Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(type == "in", { type = "in" }, label = { Text("Masuk") })
            FilterChip(type == "out", { type = "out" }, label = { Text("Keluar") })
        }
        Field(if (mode == "set") "Jumlah stok akhir" else "Jumlah perubahan", amount, { amount = it }, true)
        Field("Catatan", notes, { notes = it })
        Button(enabled = !loading && selected != null && amount.isNotBlank(), onClick = {
            val p = selected ?: return@Button
            val n = amount.toIntOrNull()
            if (n == null || n < 0 || (mode == "adjust" && n == 0)) { msg = "Jumlah tidak valid"; return@Button }
            scope.launch { loading = true; try {
                val body = JSONObject().put("productId", p.optInt("id")).put("notes", notes)
                val endpoint = if (mode == "set") { body.put("stock", n); "/api/stock-set" }
                    else { body.put("quantity", n).put("type", type); "/api/stock-adjustments" }
                withContext(Dispatchers.IO) { api.post(endpoint, body) }
                msg = "Stok berhasil diperbarui"; amount = ""; notes = ""; load()
            } catch (e: Exception) { msg = e.message ?: "Gagal memperbarui stok" }; loading = false }
        }, modifier = Modifier.fillMaxWidth()) { Text("Simpan Perubahan") }
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (msg.isNotBlank()) Text(msg, color = if (msg.contains("berhasil", true)) Accent else Danger)
    }
}

@Composable
private fun Orders(api: CapstifyApi, trash: Boolean) {
    val scope = rememberCoroutineScope()
    var list by remember { mutableStateOf(listOf<JSONObject>()) }; var msg by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }; var search by remember { mutableStateOf("") }
    fun load() { scope.launch { loading = true; try {
        val endpoint = if (trash) "/api/orders/trash" else "/api/orders"
        list = withContext(Dispatchers.IO) { api.get(endpoint) }.array("orders").rows(); msg = ""
    } catch (e: Exception) { msg = e.message ?: "Gagal memuat pesanan" }; loading = false } }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().padding(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(if (trash) "Tempat Sampah" else "Pesanan", Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
            IconButton(onClick = { load() }) { Icon(Icons.Default.Refresh, "Refresh") }
        }
        OutlinedTextField(search, { search = it }, label = { Text("Cari nomor pesanan") }, modifier = Modifier.fillMaxWidth())
        if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
        if (msg.isNotBlank()) Text(msg, color = Danger)
        LazyColumn {
            items(list.filter { it.optString("order_number").contains(search, true) }) { o ->
                Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(o.optString("order_number"), fontWeight = FontWeight.Bold)
                        Text("${o.optString("platform")} · ${o.optString("payment_status")}", color = Muted)
                        Text("Rp${money(o.optDouble("total_amount"))} · ${o.optString("order_status")}", color = Accent)
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            if (trash) {
                                TextButton(onClick = { scope.launch { try {
                                    withContext(Dispatchers.IO) { api.post("/api/orders/${o.optInt("id")}/restore") }; load()
                                } catch (e: Exception) { msg = e.message ?: "Gagal memulihkan" } } }) { Text("Pulihkan") }
                                TextButton(onClick = { scope.launch { try {
                                    withContext(Dispatchers.IO) { api.delete("/api/orders/${o.optInt("id")}/permanent") }; load()
                                } catch (e: Exception) { msg = e.message ?: "Gagal menghapus" } } }) { Text("Hapus Permanen", color = Danger) }
                            } else {
                                TextButton(onClick = { scope.launch { try {
                                    withContext(Dispatchers.IO) { api.post("/api/orders/${o.optInt("id")}/trash") }; load()
                                } catch (e: Exception) { msg = e.message ?: "Gagal memindahkan" } } }) { Text("Ke Sampah", color = Danger) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Reports(api: CapstifyApi) {
    var list by remember { mutableStateOf(listOf<JSONObject>()) }; var msg by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { try { list = withContext(Dispatchers.IO) { api.get("/api/orders") }.array("orders").rows() }
        catch (e: Exception) { msg = e.message ?: "Gagal memuat laporan" } }
    val paid = list.filter { it.optString("payment_status") == "paid" }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Rekap Penjualan", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Jumlah pesanan: ${list.size}", color = Muted)
        Text("Pesanan dibayar: ${paid.size}", color = Muted)
        Text("Total pendapatan: Rp${money(paid.sumOf { it.optDouble("total_amount") })}", color = Accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        if (msg.isNotBlank()) Text(msg, color = Danger)
    }
}

@Composable
private fun Backup(api: CapstifyApi) {
    val scope = rememberCoroutineScope()
    var msg by remember { mutableStateOf("") }; var fileId by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }; var confirmation by remember { mutableStateOf("") }
    var show by remember { mutableStateOf(false) }; var loading by remember { mutableStateOf(false) }
    var jobs by remember { mutableStateOf(listOf<JSONObject>()) }
    fun load() { scope.launch { try { jobs = withContext(Dispatchers.IO) { api.get("/api/backups") }.array("jobs").rows() }
        catch (e: Exception) { msg = e.message ?: "Gagal memuat backup" } } }
    LaunchedEffect(Unit) { load() }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Backup Data", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Button(enabled = !loading, onClick = { scope.launch { loading = true; try {
            val r = withContext(Dispatchers.IO) { api.post("/api/backups/request") }
            msg = r.optString("message").ifBlank { "Permintaan backup berhasil dibuat" }; load()
        } catch (e: Exception) { msg = e.message ?: "Gagal membuat backup" }; loading = false } }, modifier = Modifier.fillMaxWidth()) { Text("Buat Backup Sekarang") }
        OutlinedButton(onClick = { show = true }, modifier = Modifier.fillMaxWidth()) { Text("Pulihkan Database") }
        if (msg.isNotBlank()) Text(msg, color = Accent)
        Text("Riwayat Backup", fontWeight = FontWeight.Bold)
        jobs.forEach { j -> Card(Modifier.fillMaxWidth().padding(vertical = 3.dp), colors = CardDefaults.cardColors(containerColor = Panel)) {
            Column(Modifier.padding(12.dp)) {
                Text(j.optString("file_name").ifBlank { "Backup ${j.optString("id")}" })
                Text("Status: ${j.optString("status")}", color = Muted)
                Text("ID: ${j.optString("id")}", color = Muted, fontSize = 11.sp)
            }
        } }
    }
    if (show) AlertDialog(onDismissRequest = { show = false }, title = { Text("Pulihkan Database") },
        text = { Column {
            Field("File ID", fileId, { fileId = it })
            OutlinedTextField(password, { password = it }, label = { Text("Password Admin") },
                visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth())
            Field("Ketik PULIHKAN DATA", confirmation, { confirmation = it })
            Text("Pemulihan akan mengganti data tersimpan.", color = Danger, fontSize = 11.sp)
        } },
        confirmButton = { TextButton(enabled = !loading, onClick = { scope.launch { loading = true; try {
            withContext(Dispatchers.IO) { api.post("/api/backups/restore", JSONObject().put("fileId", fileId).put("password", password).put("confirmation", confirmation)) }
            msg = "Permintaan pemulihan berhasil dikirim"; show = false
        } catch (e: Exception) { msg = e.message ?: "Gagal memulihkan" }; loading = false } }) { Text("Pulihkan") } },
        dismissButton = { TextButton(onClick = { show = false }) { Text("Batal") } },
        containerColor = Panel)
}

@Composable
private fun ThemeSettings() {
    val context = LocalContext.current
    var bg by remember { mutableStateOf("#101218") }; var panel by remember { mutableStateOf("#1B1F29") }
    var text by remember { mutableStateOf("#FFFFFF") }; var accent by remember { mutableStateOf("#9BCB45") }
    var fontMenu by remember { mutableStateOf(false) }
    fun apply(raw: String, set: (Color) -> Unit) {
        val h = raw.removePrefix("#")
        try { if (h.length == 6) set(Color((0xFF000000L or h.toLong(16)).toULong()))
            else if (h.length == 8) set(Color(h.toLong(16).toULong())); saveTheme(context) } catch (_: Exception) {}
    }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Pengaturan Tema", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Tema tersimpan otomatis.", color = Muted)
        Field("Background (#RRGGBB)", bg, { bg = it })
        Button(onClick = { apply(bg) { Bg = it } }, modifier = Modifier.fillMaxWidth()) { Text("Terapkan Background") }
        Field("Panel (#RRGGBB)", panel, { panel = it })
        Button(onClick = { apply(panel) { Panel = it } }, modifier = Modifier.fillMaxWidth()) { Text("Terapkan Panel") }
        Field("Warna Teks (#RRGGBB)", text, { text = it })
        Button(onClick = { apply(text) { TextColor = it } }, modifier = Modifier.fillMaxWidth()) { Text("Terapkan Teks") }
        Field("Warna Aksen (#RRGGBB)", accent, { accent = it })
        Button(onClick = { apply(accent) { Accent = it } }, modifier = Modifier.fillMaxWidth()) { Text("Terapkan Aksen") }
        OutlinedButton(onClick = { fontMenu = true }) { Text("Font: $SelectedFont") }
        DropdownMenu(fontMenu, { fontMenu = false }) {
            listOf("Default", "Sans Serif", "Serif", "Monospace", "Cursive").forEach { f ->
                DropdownMenuItem(text = { Text(f) }, onClick = { SelectedFont = f; fontMenu = false; saveTheme(context) })
            }
        }
        val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            if (uri != null) { try { context.contentResolver.takePersistableUriPermission(uri, android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}; BackgroundImage = uri.toString(); saveTheme(context) }
        }
        Button(onClick = { picker.launch(arrayOf("image/*")) }, modifier = Modifier.fillMaxWidth()) { Text("Pilih Gambar Background") }
        if (BackgroundImage.isNotBlank()) TextButton(onClick = { BackgroundImage = ""; saveTheme(context) }) { Text("Hapus Gambar") }
        OutlinedButton(onClick = { resetTheme(context); bg = "#101218"; panel = "#1B1F29"; text = "#FFFFFF"; accent = "#9BCB45" },
            modifier = Modifier.fillMaxWidth()) { Text("Reset Tema Default") }
    }
}

@Composable
private fun Field(label: String, value: String, onValue: (String) -> Unit, numeric: Boolean = false) {
    OutlinedTextField(value, onValue, label = { Text(label) }, singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = if (numeric) KeyboardType.Number else KeyboardType.Text),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp))
}

@Composable
private fun ThemeBackground() {
    val context = LocalContext.current
    val bitmap by produceState<android.graphics.Bitmap?>(null, BackgroundImage) {
        value = if (BackgroundImage.isBlank()) null else withContext(Dispatchers.IO) {
            try { context.contentResolver.openInputStream(Uri.parse(BackgroundImage))?.use { BitmapFactory.decodeStream(it) } }
            catch (_: Exception) { null }
        }
    }
    if (bitmap != null) Image(bitmap!!.asImageBitmap(), null, Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop, alpha = 0.22f)
}
