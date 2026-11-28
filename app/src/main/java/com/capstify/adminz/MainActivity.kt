
package com.capstify.adminz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    var products by remember {
        mutableStateOf<List<ProductData>>(emptyList())
    }

    var loadingProducts by remember {
        mutableStateOf(false)
    }

    fun saveOrders(updated: List<OrderData>) {
        orders = updated
        OrderStorage.save(context, updated)
    }

    suspend fun loadProducts() {
        loadingProducts = true

        try {
            val result = withContext(Dispatchers.IO) {
                api.get("/api/products")
            }

            val array = result.optJSONArray("products")
            val loaded = mutableListOf<ProductData>()

            if (array != null) {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue

                    loaded.add(
                        ProductData(
                            id = item.optLong("id"),
                            sku = item.optString("sku"),
                            name = item.optString("name"),
                            variant = item.optString("variant"),
                            price = item.optDouble("price"),
                            stock = item.optInt("stock")
                        )
                    )
                }
            }

            products = loaded

        } catch (e: Exception) {
            message = e.message ?: "Gagal mengambil daftar produk."
        }

        loadingProducts = false
    }

    LaunchedEffect(Unit) {
        try {
            val result = withContext(Dispatchers.IO) {
                api.get("/api/me")
            }

            userName = result.optJSONObject("user")
                ?.optString("name")
                .orEmpty()

            loggedIn = true
            loadProducts()

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
            contentAlignment = Alignment.Center
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

                        userName = result
                            .optJSONObject("user")
                            ?.optString("name")
                            .orEmpty()

                        loggedIn = true
                        password = ""
                        loadProducts()

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
        products = products,
        loadingProducts = loadingProducts,
        onRefreshProducts = {
            scope.launch { loadProducts() }
        },
        onOrdersChanged = { updated ->
            saveOrders(updated)
        },
        onCompleteOrder = { order ->
    withContext(Dispatchers.IO) {
        api.post(
            "/api/apk/complete-order",
            org.json.JSONObject()
                .put("orderNumber", order.orderNumber)
                .put("platform", order.platform)
                .put("productId", order.productId)
                .put("quantity", order.quantity.toInt())
                .put("customerName", order.recipient)
                .put("customerPhone", order.customerPhone)
                .put(
                    "address",
                    "${order.address}, ${order.city}".trim(' ', ',')
                )
                .put("courier", order.courier)
                .put("trackingNumber", order.tracking)
                .put("paymentStatus", "unpaid")
        )
    }

    loadProducts()
},
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
                userName = ""
                products = emptyList()
            }
        }
    )
}
