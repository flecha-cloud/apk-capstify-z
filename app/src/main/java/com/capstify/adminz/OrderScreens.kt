
package com.capstify.adminz

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
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

private val Black = Color(0xFF000000)
private val Purple = Color(0xFF760FFA)
private val White = Color(0xFFFFFFFF)
private val Grey = Color(0xFFAAA3B5)
private val Card = Color(0xFF17131F)
private val Green = Color(0xFF43D69B)
private val Red = Color(0xFFFF6262)

@Composable
fun LoginScreen(
    username: String,
    password: String,
    loading: Boolean,
    message: String,
    onUsernameChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLogin: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "CAPSTIFY",
            color = Purple,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Admin Z",
            color = White,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            text = "Order Management",
            color = Grey
        )

        Spacer(Modifier.height(32.dp))

        OutlinedTextField(
            value = username,
            onValueChange = onUsernameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            singleLine = true,
            colors = fieldColors()
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            colors = fieldColors()
        )

        if (message.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                color = Red,
                fontSize = 12.sp
            )
        }

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = onLogin,
            enabled = !loading &&
                username.isNotBlank() &&
                password.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (loading) "Memproses..." else "Masuk",
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun OrderHomeScreen(
    userName: String,
    orders: List<OrderData>,
    products: List<ProductData>,
    loadingProducts: Boolean,
    onRefreshProducts: () -> Unit,
    onOrdersChanged: (List<OrderData>) -> Unit,
    onLogout: () -> Unit
)
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    var selected by remember { mutableStateOf<OrderData?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var celebration by remember { mutableStateOf(false) }

    if (showForm) {
        OrderEditor(
    initial = selected,
    products = products,
    loadingProducts = loadingProducts,
    onRefreshProducts = onRefreshProducts,
            onSave = { order ->
                val updated = if (selected == null) {
                    orders + order
                } else {
                    orders.map {
                        if (it.id == order.id) order else it
                    }
                }

                onOrdersChanged(updated)
                showForm = false
                selected = null
            },
            onDelete = { order ->
                onOrdersChanged(
                    orders.filterNot { it.id == order.id }
                )

                showForm = false
                selected = null
            },
            onComplete = { order ->
                val updated = if (selected == null) {
                    orders + order
                } else {
                    orders.map {
                        if (it.id == order.id) order else it
                    }
                }

                onOrdersChanged(updated)
                showForm = false
                selected = null
                celebration = true
            },
            onBack = {
                showForm = false
                selected = null
            }
        )

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = "CAPSTIFY",
                    color = Purple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Pesanan",
                    color = White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Halo, ${userName.ifBlank { "Admin" }}",
                    color = Grey,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onLogout) {
                Icon(
                    imageVector = Icons.Default.Logout,
                    contentDescription = "Keluar",
                    tint = Grey
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                selected = null
                showForm = true
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Spacer(Modifier.width(6.dp))
            Text("Tambah Pesanan")
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text(
                    "Cari nama, produk, nomor pesanan...",
                    color = Grey
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = Grey
                )
            },
            singleLine = true,
            colors = fieldColors()
        )

        Spacer(Modifier.height(10.dp))

        Row(
            modifier = Modifier.horizontalScroll(
                rememberScrollState()
            ),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(
                "Semua",
                "Packing Dulu Bre",
                "OTW Kirim",
                "Dah Kelar Bre"
            ).forEach { status ->
                FilterChip(
                    selected = filter == status,
                    onClick = { filter = status },
                    label = {
                        Text(
                            when (status) {
                                "Packing Dulu Bre" -> "Packing"
                                "OTW Kirim" -> "OTW"
                                "Dah Kelar Bre" -> "Selesai"
                                else -> "Semua"
                            },
                            fontSize = 11.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Purple,
                        selectedLabelColor = White,
                        labelColor = Grey
                    )
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Text(
            text = "${orders.size} pesanan tersimpan",
            color = Grey,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(8.dp))

        val filtered = orders.filter { order ->
            val matchesFilter =
                filter == "Semua" || order.status == filter

            val searchable = listOf(
                order.product,
                order.orderNumber,
                order.recipient,
                order.tracking,
                order.platform
            ).any {
                it.contains(search, ignoreCase = true)
            }

            matchesFilter && searchable
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Grey,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        text = "Belum ada pesanan",
                        color = Grey
                    )

                    Text(
                        text = "Tekan Tambah Pesanan untuk memulai.",
                        color = Grey,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.id }
                ) { order ->
                    OrderCard(order) {
                        selected = order
                        showForm = true
                    }
                }
            }
        }
    }

    if (celebration) {
        AlertDialog(
            onDismissRequest = {
                celebration = false
            },
            containerColor = Card,
            title = {
                Text(
                    text = "Dah Kelar Bre! 🎉",
                    color = Purple
                )
            },
            text = {
                Text(
                    text = "Pesanan berhasil ditandai selesai di aplikasi.",
                    color = White
                )
            },
            confirmButton = {
                TextButton(
                    onClick = { celebration = false }
                ) {
                    Text("Mantap!", color = Purple)
                }
            }
        )
    }
}


@Composable
private fun OrderCard(
    order: OrderData,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Card
        ),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = order.product,
                    color = White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                if (order.locked) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Terkunci",
                        tint = Grey,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.height(5.dp))

            Text(
                "No. Pesanan: ${order.orderNumber}",
                color = Grey,
                fontSize = 12.sp
            )

            Text(
                "${order.platform} · ${order.quantity} produk",
                color = Grey,
                fontSize = 12.sp
            )

            if (order.recipient.isNotBlank()) {
                Text(
                    "Penerima: ${order.recipient}",
                    color = Grey,
                    fontSize = 12.sp
                )
            }

            if (order.tracking.isNotBlank()) {
                Text(
                    "Resi: ${order.tracking}",
                    color = Grey,
                    fontSize = 12.sp
                )
            }

            Spacer(Modifier.height(10.dp))

            val statusColor = when (order.status) {
                "Dah Kelar Bre" -> Green
                "OTW Kirim" -> Purple
                else -> Color(0xFFFFC15A)
            }

            Surface(
                color = statusColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = order.status,
                    color = statusColor,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(
                        horizontal = 10.dp,
                        vertical = 6.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun OrderEditor(
    initial: OrderData?,
    onSave: (OrderData) -> Unit,
    onDelete: (OrderData) -> Unit,
    onComplete: (OrderData) -> Unit,
    onBack: () -> Unit
) {
    var platform by remember {
        mutableStateOf(initial?.platform ?: "Shopee")
    }
    var product by remember {
        mutableStateOf(initial?.product ?: "")
    }
    var quantity by remember {
        mutableStateOf(initial?.quantity ?: "1")
    }
    var price by remember {
        mutableStateOf(initial?.price ?: "")
    }
    var orderNumber by remember {
        mutableStateOf(initial?.orderNumber ?: "")
    }
    var orderDate by remember {
        mutableStateOf(initial?.orderDate ?: "")
    }
    var courier by remember {
        mutableStateOf(initial?.courier ?: "")
    }
    var tracking by remember {
        mutableStateOf(initial?.tracking ?: "")
    }
    var city by remember {
        mutableStateOf(initial?.city ?: "")
    }
    var recipient by remember {
        mutableStateOf(initial?.recipient ?: "")
    }
    var address by remember {
        mutableStateOf(initial?.address ?: "")
    }
    var locked by remember {
        mutableStateOf(initial?.locked ?: false)
    }
    var error by remember {
        mutableStateOf("")
    }

    val id = initial?.id ?: System.currentTimeMillis()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "Kembali",
                    tint = White
                )
            }

            Text(
                text = if (initial == null)
                    "Tambah Pesanan"
                else
                    "Detail Pesanan",
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }

        if (locked) {
            Text(
                "Pesanan terkunci. Buka kunci untuk mengedit.",
                color = Color(0xFFFFC15A),
                fontSize = 12.sp
            )

            Button(
                onClick = { locked = false },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                )
            ) {
                Text("Buka Kunci")
            }
        }

        Text(
            "INFORMASI PESANAN",
            color = Purple,
            fontWeight = FontWeight.Bold
        )

        FormField(
            "Platform",
            platform,
            { platform = it },
            !locked
        )

        FormField(
            "Nama Produk",
            product,
            { product = it },
            !locked
        )

        FormField(
            "Jumlah Produk",
            quantity,
            { quantity = it },
            !locked,
            numeric = true
        )

        FormField(
            "Harga",
            price,
            { price = it },
            !locked,
            numeric = true
        )

        FormField(
            "Nomor Pesanan",
            orderNumber,
            { orderNumber = it },
            !locked
        )

        FormField(
            "Tanggal Pesanan",
            orderDate,
            { orderDate = it },
            !locked
        )

        HorizontalDivider(color = Card)

        Text(
            "INFORMASI PENGIRIMAN",
            color = Purple,
            fontWeight = FontWeight.Bold
        )

        FormField(
            "Kurir / Ekspedisi",
            courier,
            { courier = it },
            !locked
        )

        FormField(
            "Nomor Resi",
            tracking,
            { tracking = it },
            !locked
        )

        FormField(
            "Kota",
            city,
            { city = it },
            !locked
        )

        FormField(
            "Nama Penerima",
            recipient,
            { recipient = it },
            !locked
        )

        FormField(
            "Alamat Lengkap",
            address,
            { address = it },
            !locked,
            multiline = true
        )

        if (!locked) {
            Button(
                onClick = {
                    if (
                        product.isBlank() ||
                        orderNumber.isBlank()
                    ) {
                        error = "Nama produk dan nomor pesanan wajib diisi."
                    } else {
                        onSave(
                            buildOrder(
                                id, platform, product, quantity,
                                price, orderNumber, orderDate,
                                courier, tracking, city,
                                recipient, address,
                                "Packing Dulu Bre", locked
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Purple
                )
            ) {
                Text("Packing Dulu Bre")
            }

            Button(
                onClick = {
                    if (
                        product.isBlank() ||
                        orderNumber.isBlank() ||
                        courier.isBlank() ||
                        tracking.isBlank() ||
                        recipient.isBlank() ||
                        address.isBlank()
                    ) {
                        error = "Lengkapi informasi pesanan dan pengiriman."
                    } else {
                        onSave(
                            buildOrder(
                                id, platform, product, quantity,
                                price, orderNumber, orderDate,
                                courier, tracking, city,
                                recipient, address,
                                "OTW Kirim", locked
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Card
                )
            ) {
                Text("OTW Kirim")
            }

            Button(
                onClick = {
                    if (
                        tracking.isBlank() ||
                        recipient.isBlank() ||
                        address.isBlank()
                    ) {
                        error = "Lengkapi data pengiriman terlebih dahulu."
                    } else {
                        onComplete(
                            buildOrder(
                                id, platform, product, quantity,
                                price, orderNumber, orderDate,
                                courier, tracking, city,
                                recipient, address,
                                "Dah Kelar Bre", locked
                            )
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Green,
                    contentColor = Black
                )
            ) {
                Text(
                    "Dah Kelar Bre",
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedButton(
                onClick = { locked = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kunci Pesanan")
            }
        }

        if (initial != null) {
            OutlinedButton(
                onClick = { onDelete(initial) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Red
                )
            ) {
                Text("Hapus Pesanan")
            }
        }

        if (error.isNotBlank()) {
            Text(
                error,
                color = Red,
                fontSize = 12.sp
            )
        }
    }
}

private fun buildOrder(
    id: Long,
    platform: String,
    product: String,
    quantity: String,
    price: String,
    orderNumber: String,
    orderDate: String,
    courier: String,
    tracking: String,
    city: String,
    recipient: String,
    address: String,
    status: String,
    locked: Boolean
): OrderData {
    return OrderData(
        id = id,
        platform = platform,
        product = product,
        quantity = quantity,
        price = price,
        orderNumber = orderNumber,
        orderDate = orderDate,
        courier = courier,
        tracking = tracking,
        city = city,
        recipient = recipient,
        address = address,
        status = status,
        locked = locked
    )
}

@Composable
private fun FormField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    enabled: Boolean = true,
    numeric: Boolean = false,
    multiline: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValue,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        singleLine = !multiline,
        keyboardOptions = KeyboardOptions(
            keyboardType = if (numeric)
                KeyboardType.Number
            else
                KeyboardType.Text
        ),
        shape = RoundedCornerShape(11.dp),
        colors = fieldColors()
    )
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White,
    unfocusedTextColor = White,
    disabledTextColor = Grey,
    focusedBorderColor = Purple,
    unfocusedBorderColor = Card,
    focusedLabelColor = Purple,
    unfocusedLabelColor = Grey,
    cursorColor = Purple,
    focusedContainerColor = Card,
    unfocusedContainerColor = Card,
    disabledContainerColor = Card
)
