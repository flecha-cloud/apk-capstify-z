package com.capstify.adminz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private val Bg = Color(0xFF101218)
private val Panel = Color(0xFF1B1F29)
private val Line = Color(0xFF343C4C)
private val Accent = Color(0xFF9BCB45)
private val Muted = Color(0xFFAAA8B6)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val api = CapstifyApi(this)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Bg, surface = Panel, primary = Accent,
                    onPrimary = Color(0xFF101218), onBackground = Color.White,
                    onSurface = Color.White, secondary = Muted
                )
            ) {
                CapstifyApp(api)
            }
        }
    }
}

@Composable
private fun CapstifyApp(api: CapstifyApi) {
    val scope = rememberCoroutineScope()
    var loggedIn by remember { mutableStateOf(false) }
    var userName by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("Dashboard") }
    var stats by remember { mutableStateOf(JSONObject()) }

    fun loadDashboard() {
        scope.launch {
            busy = true
            try {
                val result = withContext(Dispatchers.IO) { api.get("/api/dashboard") }
                stats = result.optJSONObject("stats") ?: JSONObject()
            } catch (e: Exception) { message = e.message ?: "Gagal memuat dashboard." }
            busy = false
        }
    }

    LaunchedEffect(Unit) {
        try {
            val me = withContext(Dispatchers.IO) { api.get("/api/me") }
            userName = me.optJSONObject("user")?.optString("name").orEmpty()
            loggedIn = true
            loadDashboard()
        } catch (_: Exception) { loggedIn = false }
    }

    if (!loggedIn) {
        LoginScreen(
            username = username, onUsername = { username = it },
            password = password, onPassword = { password = it },
            busy = busy, message = message,
            onLogin = {
                scope.launch {
                    busy = true; message = ""
                    try {
                        val result = withContext(Dispatchers.IO) {
                            api.post("/api/login", JSONObject().put("username", username).put("password", password))
                        }
                        userName = result.optJSONObject("user")?.optString("name").orEmpty()
                        loggedIn = true
                        password = ""
                        loadDashboard()
                    } catch (e: Exception) { message = e.message ?: "Login gagal." }
                    busy = false
                }
            }
        )
    } else {
        Column(Modifier.fillMaxSize().background(Bg)) {
            Row(
                Modifier.fillMaxWidth().background(Panel).padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(page, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Capstify-Admin Z · $userName", color = Muted, fontSize = 12.sp)
                }
                var menu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { menu = true }) { Icon(Icons.Default.Menu, "Menu") }
                    DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                        listOf("Dashboard", "Produk & Kode", "Atur Stok", "Pesanan", "Tempat Sampah", "Rekap Penjualan", "Backup Data").forEach {
                            DropdownMenuItem(text = { Text(it) }, onClick = { page = it; menu = false; if (it == "Dashboard") loadDashboard() })
                        }
                        DropdownMenuItem(text = { Text("Keluar") }, onClick = {
                            menu = false
                            scope.launch {
                                withContext(Dispatchers.IO) { api.post("/api/logout", JSONObject()) }
                                api.clearSession(); loggedIn = false
                            }
                        })
                    }
                }
            }
            when (page) {
                "Dashboard" -> DashboardScreen(stats, busy, message, onRefresh = { loadDashboard() })
                else -> PlaceholderScreen(page)
            }
        }
    }
}

@Composable
private fun LoginScreen(
    username: String, onUsername: (String) -> Unit,
    password: String, onPassword: (String) -> Unit,
    busy: Boolean, message: String, onLogin: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Bg).padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("🧢", fontSize = 42.sp)
        Spacer(Modifier.height(12.dp))
        Text("Capstify-Admin Z", fontSize = 27.sp, fontWeight = FontWeight.Bold)
        Text("Sales Management System · Private Workspace", color = Muted, fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(username, onUsername, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            password, onPassword, label = { Text("Password") }, singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        if (message.isNotBlank()) {
            Spacer(Modifier.height(10.dp)); Text(message, color = Color(0xFFFF7777), fontSize = 13.sp)
        }
        Spacer(Modifier.height(18.dp))
        Button(onClick = onLogin, enabled = !busy && username.isNotBlank() && password.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(13.dp)) {
            if (busy) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
            else Text("Masuk ke Dashboard", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun DashboardScreen(stats: JSONObject, busy: Boolean, message: String, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Ringkasan aktivitas operasional", Modifier.weight(1f), color = Muted, fontSize = 13.sp)
            IconButton(onClick = onRefresh) { Icon(Icons.Default.Refresh, "Muat ulang") }
        }
        val cards = listOf(
            "Pesanan Hari Ini" to stats.optString("ordersToday", "—"),
            "Total Produk" to stats.optString("totalProducts", "—"),
            "Total Stok" to stats.optString("totalStock", "—"),
            "Pesanan Diproses" to stats.optString("pendingShipments", "—")
        )
        cards.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { (label, value) ->
                    Card(Modifier.weight(1f).padding(vertical = 5.dp), colors = CardDefaults.cardColors(containerColor = Panel),
                        shape = RoundedCornerShape(17.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            Text(label, color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(9.dp))
                            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth().padding(top = 10.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF202A1A)),
            shape = RoundedCornerShape(17.dp)) {
            Column(Modifier.padding(18.dp)) {
                Text("Omzet Hari Ini", color = Muted, fontSize = 12.sp)
                Text("Rp${"%,.0f".format(stats.optDouble("revenueToday", 0.0))}", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        if (busy) { Spacer(Modifier.height(15.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
        if (message.isNotBlank()) { Spacer(Modifier.height(12.dp)); Text(message, color = Color(0xFFFF8888)) }
        Spacer(Modifier.height(18.dp))
        Text("Database terhubung", color = Accent, fontWeight = FontWeight.SemiBold)
        Text("Data aplikasi ini menggunakan database Capstify yang sama dengan website.", color = Muted, fontSize = 12.sp)
    }
}

@Composable
private fun PlaceholderScreen(page: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(page, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Modul ini akan ditambahkan pada tahap berikutnya.", color = Muted)
    }
}
