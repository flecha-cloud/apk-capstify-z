
package com.capstify.adminz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
private val Accent = Color(0xFF9BCB45)
private val Muted = Color(0xFFAAA8B6)

private val pages = listOf(
    "Dashboard",
    "Produk & Kode",
    "Atur Stok",
    "Pesanan",
    "Tempat Sampah",
    "Rekap Penjualan",
    "Backup Data"
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val api = CapstifyApi(this)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Bg,
                    surface = Panel,
                    primary = Accent,
                    onPrimary = Bg,
                    onBackground = Color.White,
                    onSurface = Color.White,
                    secondary = Muted
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
            message = ""
            try {
                val result = withContext(Dispatchers.IO) {
                    api.get("/api/dashboard")
                }
                stats = result.optJSONObject("stats") ?: JSONObject()
            } catch (e: Exception) {
                message = e.message ?: "Gagal memuat dashboard."
            }
            busy = false
        }
    }

    fun navigate(target: String) {
        page = target
        message = ""
        if (target == "Dashboard") loadDashboard()
    }

    BackHandler(enabled = loggedIn && page != "Dashboard") {
        navigate("Dashboard")
    }

    LaunchedEffect(Unit) {
        try {
            val me = withContext(Dispatchers.IO) {
                api.get("/api/me")
            }
            userName = me.optJSONObject("user")
                ?.optString("name").orEmpty()
            loggedIn = true
            loadDashboard()
        } catch (_: Exception) {
            loggedIn = false
        }
    }

    if (!loggedIn) {
        LoginScreen(
            username = username,
            onUsername = { username = it },
            password = password,
            onPassword = { password = it },
            busy = busy,
            message = message,
            onLogin = {
                scope.launch {
                    busy = true
                    message = ""
                    try {
                        val result = withContext(Dispatchers.IO) {
                            api.post(
                                "/api/login",
                                JSONObject()
                                    .put("username", username)
                                    .put("password", password)
                            )
                        }
                        userName = result.optJSONObject("user")
                            ?.optString("name").orEmpty()
                        loggedIn = true
                        password = ""
                        loadDashboard()
                    } catch (e: Exception) {
                        message = e.message ?: "Login gagal."
                    }
                    busy = false
                }
            }
        )
    } else {
        Column(
            Modifier
                .fillMaxSize()
                .background(Bg)
        ) {
            var menu by remember { mutableStateOf(false) }

            Row(
                Modifier
                    .fillMaxWidth()
                    .background(Panel)
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        page,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Capstify-Admin Z · $userName",
                        color = Muted,
                        fontSize = 12.sp
                    )
                }

                Box {
                    IconButton(onClick = { menu = true }) {
                        Icon(Icons.Default.Menu, "Buka menu")
                    }

                    DropdownMenu(
                        expanded = menu,
                        onDismissRequest = { menu = false }
                    ) {
                        pages.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item) },
                                leadingIcon = {
                                    Icon(
                                        when (item) {
                                            "Dashboard" -> Icons.Default.Home
                                            "Produk & Kode" -> Icons.Default.Inventory
                                            "Atur Stok" -> Icons.Default.Warehouse
                                            "Pesanan" -> Icons.Default.ShoppingCart
                                            "Tempat Sampah" -> Icons.Default.Delete
                                            "Rekap Penjualan" -> Icons.Default.BarChart
                                            else -> Icons.Default.Backup
                                        },
                                        contentDescription = null
                                    )
                                },
                                onClick = {
                                    menu = false
                                    navigate(item)
                                }
                            )
                        }

                        HorizontalDivider()

                        DropdownMenuItem(
                            text = { Text("Keluar") },
                            leadingIcon = {
                                Icon(Icons.Default.Logout, null)
                            },
                            onClick = {
                                menu = false
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            api.post("/api/logout", JSONObject())
                                        }
                                    } catch (_: Exception) {
                                    }
                                    api.clearSession()
                                    loggedIn = false
                                    page = "Dashboard"
                                }
                            }
                        )
                    }
                }
            }

            when (page) {
                "Dashboard" -> DashboardScreen(
                    stats = stats,
                    busy = busy,
                    message = message,
                    onRefresh = { loadDashboard() },
                    onOpenPage = { navigate(it) }
                )

                else -> PlaceholderScreen(
                    page = page,
                    onBack = { navigate("Dashboard") }
                )
            }
        }
    }
}

@Composable
private fun LoginScreen(
    username: String,
    onUsername: (String) -> Unit,
    password: String,
    onPassword: (String) -> Unit,
    busy: Boolean,
    message: String,
    onLogin: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(Bg)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text("🧢", fontSize = 42.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            "Capstify-Admin Z",
            fontSize = 27.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Sales Management System · Private Workspace",
            color = Muted,
            fontSize = 13.sp
        )
        Spacer(Modifier.height(28.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsername,
            label = { Text("Username") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPassword,
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(message, color = Color(0xFFFF7777), fontSize = 13.sp)
        }

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = onLogin,
            enabled = !busy &&
                username.isNotBlank() &&
                password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(13.dp)
        ) {
            if (busy) {
                CircularProgressIndicator(
                    Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else {
                Text(
                    "Masuk ke Dashboard",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun DashboardScreen(
    stats: JSONObject,
    busy: Boolean,
    message: String,
    onRefresh: () -> Unit,
    onOpenPage: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Ringkasan aktivitas operasional",
                Modifier.weight(1f),
                color = Muted,
                fontSize = 13.sp
            )
            IconButton(onClick = onRefresh) {
                Icon(Icons.Default.Refresh, "Muat ulang")
            }
        }

        val cards = listOf(
            Triple(
                "Pesanan Hari Ini",
                stats.optString("ordersToday", "—"),
                "Pesanan"
            ),
            Triple(
                "Total Produk",
                stats.optString("totalProducts", "—"),
                "Produk & Kode"
            ),
            Triple(
                "Total Stok",
                stats.optString("totalStock", "—"),
                "Atur Stok"
            ),
            Triple(
                "Pesanan Diproses",
                stats.optString("pendingShipments", "—"),
                "Pesanan"
            )
        )

        cards.chunked(2).forEach { pair ->
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { (label, value, target) ->
                    Card(
                        Modifier
                            .weight(1f)
                            .padding(vertical = 5.dp)
                            .clickable { onOpenPage(target) },
                        colors = CardDefaults.cardColors(
                            containerColor = Panel
                        ),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(label, color = Muted, fontSize = 12.sp)
                            Spacer(Modifier.height(9.dp))
                            Text(
                                value,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Lihat detail →",
                                color = Accent,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        Card(
            Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF202A1A)
            ),
            shape = RoundedCornerShape(17.dp)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Omzet Hari Ini", color = Muted, fontSize = 12.sp)
                Text(
                    "Rp${"%,.0f".format(stats.optDouble("revenueToday", 0.0))}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (busy) {
            Spacer(Modifier.height(15.dp))
            LinearProgressIndicator(Modifier.fillMaxWidth())
        }

        if (message.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Text(message, color = Color(0xFFFF8888))
        }

        Spacer(Modifier.height(18.dp))
        Text(
            "Database terhubung",
            color = Accent,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            "Data aplikasi ini menggunakan database Capstify yang sama dengan website.",
            color = Muted,
            fontSize = 12.sp
        )
    }
}

@Composable
private fun PlaceholderScreen(
    page: String,
    onBack: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Construction,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(48.dp)
        )

        Spacer(Modifier.height(16.dp))

        Text(
            page,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        Text(
            "Navigasi berhasil dibuka. Fitur halaman ini belum diimplementasikan.",
            color = Muted
        )

        Spacer(Modifier.height(22.dp))

        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.ArrowBack, null)
            Spacer(Modifier.width(8.dp))
            Text("Kembali ke Dashboard")
        }
    }
}
