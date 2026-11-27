
package com.capstify.adminz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val AppBlack = Color(0xFF000000)
private val AppPurple = Color(0xFF760FFA)
private val AppWhite = Color(0xFFFFFFFF)
private val AppGrey = Color(0xFFAAA3B5)
private val AppCard = Color(0xFF17131F)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val api = CapstifyApi(this)

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = AppPurple,
                    onPrimary = AppWhite,
                    background = AppBlack,
                    onBackground = AppWhite,
                    surface = AppCard,
                    onSurface = AppWhite,
                    secondary = AppGrey
                )
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = AppBlack
                ) {
                    CapstifyApp(api)
                }
            }
        }
    }
}

@Composable
private fun CapstifyApp(api: CapstifyApi) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()

    var loggedIn by remember { mutableStateOf(false) }
    var checkingSession by remember { mutableStateOf(true) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    var orders by remember {
        mutableStateOf(OrderStorage.load(context))
    }

    fun saveOrders(updated: List<OrderData>) {
        orders = updated
        OrderStorage.save(context, updated)
    }

    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) {
                api.get("/api/me")
            }

            userName = result.optJSONObject("user")
                ?.optString("name").orEmpty()

            loggedIn = true
        } catch (_: Exception) {
            loggedIn = false
        }

        checkingSession = false
    }

    if (checkingSession) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppBlack),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator(color = AppPurple)
        }
        return
    }

    if (!loggedIn) {
        LoginScreen(
            username = username,
            password = password,
            loading = loading,
            message = message,
            onUsernameChange = { username = it },
            onPasswordChange = { password = it },
            onLogin = {
                scope.launch {
                    loading = true
                    message = ""

                    try {
                        val result = withContext(Dispatchers.IO) {
                            api.post(
                                "/api/login",
                                org.json.JSONObject()
                                    .put("username", username)
                                    .put("password", password)
                            )
                        }

                        userName = result.optJSONObject("user")
                            ?.optString("name").orEmpty()

                        loggedIn = true
                        password = ""
                    } catch (e: Exception) {
                        message = e.message ?: "Login gagal."
                    }

                    loading = false
                }
            }
        )
        return
    }

    OrderHomeScreen(
        userName = userName,
        orders = orders,
        onOrdersChanged = { saveOrders(it) },
        onLogout = {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        api.post("/api/logout")
                    }
                } catch (_: Exception) {
                }

                api.clearSession()
                loggedIn = false
                username = ""
                password = ""
            }
        }
    )
}
