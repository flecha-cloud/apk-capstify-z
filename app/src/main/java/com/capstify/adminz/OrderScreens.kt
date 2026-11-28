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
) {
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
fun OrderEditor(
    order: OrderData?,
    products: List<ProductData>,
    onSave: (OrderData) -> Unit,
    onCancel: () -> Unit
) {
    var platform by remember {
        mutableStateOf(order?.platform ?: "TikTok")
    }
    var productName by remember {
        mutableStateOf(order?.product ?: "")
    }
    var selectedProductId by remember {
        mutableStateOf(order?.productId ?: 0)
    }
    var quantityText by remember {
        mutableStateOf(order?.quantity?.toString() ?: "1")
    }
    var orderNumber by remember {
        mutableStateOf(order?.orderNumber ?: "")
    }
    var orderDate by remember {
        mutableStateOf(order?.orderDate ?: "")
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var expandedPlatform by remember { mutableStateOf(false) }
    var expandedProduct by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val selectedProduct = products.find {
        it.id == selectedProductId
    } ?: products.find {
        it.name == productName
    }

    val quantity = quantityText.toIntOrNull() ?: 0
    val availableStock = selectedProduct?.stock ?: 0

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = if (order == null) "Pesanan Baru" else "Edit Pesanan",
            color = White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        Text("Platform", color = Grey)

        ExposedDropdownMenuBox(
            expanded = expandedPlatform,
            onExpandedChange = { expandedPlatform = !expandedPlatform }
        ) {
            OutlinedTextField(
                value = platform,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pilih platform") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expandedPlatform
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = fieldColors()
            )

            ExposedDropdownMenu(
                expanded = expandedPlatform,
                onDismissRequest = { expandedPlatform = false }
            ) {
                listOf("TikTok", "Shopee", "Web").forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            platform = it
                            expandedPlatform = false
                        }
                    )
                }
            }
        }

        Text("Produk", color = Grey)

        ExposedDropdownMenuBox(
            expanded = expandedProduct,
            onExpandedChange = { expandedProduct = !expandedProduct }
        ) {
            OutlinedTextField(
                value = selectedProduct?.let {
                    "${it.name} - Stok: ${it.stock}"
                } ?: productName,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pilih produk") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = expandedProduct
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = fieldColors()
            )

            ExposedDropdownMenu(
                expanded = expandedProduct,
                onDismissRequest = { expandedProduct = false }
            ) {
                products.forEach { product ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(product.name)
                                Text(
                                    "Stok tersedia: ${product.stock}",
                                    color = Grey,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        onClick = {
                            productName = product.name
                            selectedProductId = product.id
                            expandedProduct = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = quantityText,
            onValueChange = {
                quantityText = it.filter { char -> char.isDigit() }
            },
            label = { Text("Jumlah") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        if (selectedProduct != null) {
            Text(
                text = "Stok tersedia: ${selectedProduct.stock}",
                color = if (quantity > availableStock) Red else Green,
                fontSize = 13.sp
            )
        }

        OutlinedTextField(
            value = orderNumber,
            onValueChange = { orderNumber = it },
            label = { Text("Nomor pesanan") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        OutlinedTextField(
            value = orderDate,
            onValueChange = {},
            readOnly = true,
            label = { Text("Tanggal pesanan") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        Icons.Default.CalendarMonth,
                        contentDescription = "Pilih tanggal",
                        tint = Purple
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        if (showDatePicker) {
            val datePickerState = rememberDatePickerState()

            DatePickerDialog(
                onDismissRequest = { showDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let {
                                val date = java.text.SimpleDateFormat(
                                    "dd-MM-yyyy",
                                    java.util.Locale("id", "ID")
                                ).format(java.util.Date(it))
                                orderDate = date
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("Pilih", color = Purple)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showDatePicker = false }
                    ) {
                        Text("Batal", color = Grey)
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        if (errorMessage.isNotBlank()) {
            Text(errorMessage, color = Red)
        }

        Button(
            onClick = {
                errorMessage = when {
                    selectedProduct == null ->
                        "Pilih produk terlebih dahulu."

                    quantity <= 0 ->
                        "Jumlah pesanan harus lebih dari 0."

                    quantity > availableStock ->
                        "Stok tidak mencukupi. Stok tersedia: $availableStock."

                    orderNumber.isBlank() ->
                        "Nomor pesanan wajib diisi."

                    orderDate.isBlank() ->
                        "Tanggal pesanan wajib dipilih."

                    else -> ""
                }

                if (errorMessage.isBlank() && selectedProduct != null) {
                    val savedOrder = buildOrder(
                        oldOrder = order,
                        platform = platform,
                        product = selectedProduct,
                        quantity = quantity,
                        orderNumber = orderNumber,
                        orderDate = orderDate
                    )
                    onSave(savedOrder)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple
            )
        ) {
            Text("Simpan Pesanan", color = White)
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Batal", color = White)
        }
    }
}

private fun buildOrder(
    oldOrder: OrderData?,
    platform: String,
    product: ProductData,
    quantity: Int,
    orderNumber: String,
    orderDate: String
): OrderData {
    return OrderData(
        id = oldOrder?.id ?: java.util.UUID.randomUUID().toString(),
        platform = platform,
        product = product.name,
        quantity = quantity,
        price = product.price,
        orderNumber = orderNumber,
        orderDate = orderDate,
        courier = oldOrder?.courier ?: "",
        tracking = oldOrder?.tracking ?: "",
        city = oldOrder?.city ?: "",
        recipient = oldOrder?.recipient ?: "",
        address = oldOrder?.address ?: "",
        status = oldOrder?.status ?: "Pesanan Baru",
        locked = oldOrder?.locked ?: false,
        productId = product.id,
        customerPhone = oldOrder?.customerPhone ?: "",
        synced = oldOrder?.synced ?: false
    )
}
@Composable
fun OrderCard(
    order: OrderData,
    onClick: () -> Unit,
    onToggleLock: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    order.orderNumber,
                    color = White,
                    fontWeight = FontWeight.Bold
                )

                IconButton(onClick = onToggleLock) {
                    Icon(
                        imageVector = if (order.locked) {
                            Icons.Default.Lock
                        } else {
                            Icons.Default.LockOpen
                        },
                        contentDescription = if (order.locked) {
                            "Buka kunci pesanan"
                        } else {
                            "Kunci pesanan"
                        },
                        tint = if (order.locked) Purple else Grey
                    )
                }
            }

            Text(
                "${order.platform} • ${order.product}",
                color = White
            )

            Text(
                "Jumlah: ${order.quantity}",
                color = Grey
            )

            Text(
                "Status: ${order.status}",
                color = Purple
            )

            if (order.locked) {
                Text(
                    "Pesanan terkunci. Tekan ikon gembok untuk membuka.",
                    color = Grey,
                    fontSize = 12.sp
                )
            } else {
                Button(
                    onClick = onClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Purple
                    )
                ) {
                    Text("Buka Pesanan", color = White)
                }
            }
        }
    }
}

@Composable
fun ShippingEditor(
    order: OrderData,
    onSave: (OrderData) -> Unit,
    onCancel: () -> Unit
) {
    var courier by remember { mutableStateOf(order.courier) }
    var tracking by remember { mutableStateOf(order.tracking) }
    var city by remember { mutableStateOf(order.city) }
    var recipient by remember { mutableStateOf(order.recipient) }
    var phone by remember { mutableStateOf(order.customerPhone) }
    var address by remember { mutableStateOf(order.address) }
    var error by remember { mutableStateOf("") }

    val cities = listOf(
        "Jakarta",
        "Bandung",
        "Surabaya",
        "Semarang",
        "Yogyakarta",
        "Malang",
        "Bekasi",
        "Depok",
        "Tangerang",
        "Bogor",
        "Medan",
        "Palembang",
        "Makassar",
        "Denpasar",
        "Solo",
        "Balikpapan",
        "Pontianak",
        "Banjarmasin",
        "Pekanbaru",
        "Padang"
    )

    var cityExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            "Informasi Pengiriman",
            color = White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = courier,
            onValueChange = { courier = it },
            label = { Text("Kurir") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        OutlinedTextField(
            value = tracking,
            onValueChange = { tracking = it },
            label = { Text("Nomor resi") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        ExposedDropdownMenuBox(
            expanded = cityExpanded,
            onExpandedChange = { cityExpanded = !cityExpanded }
        ) {
            OutlinedTextField(
                value = city,
                onValueChange = {
                    city = it
                    cityExpanded = true
                },
                label = { Text("Tujuan / Kota") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = cityExpanded
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = fieldColors()
            )

            ExposedDropdownMenu(
                expanded = cityExpanded,
                onDismissRequest = { cityExpanded = false }
            ) {
                cities.filter {
                    it.contains(city, ignoreCase = true)
                }.forEach { item ->
                    DropdownMenuItem(
                        text = { Text(item) },
                        onClick = {
                            city = item
                            cityExpanded = false
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = recipient,
            onValueChange = { recipient = it },
            label = { Text("Nama penerima") },
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Nomor HP penerima") },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone
            ),
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        OutlinedTextField(
            value = address,
            onValueChange = { address = it },
            label = { Text("Alamat lengkap") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            colors = fieldColors()
        )

        if (error.isNotBlank()) {
            Text(error, color = Red)
        }

        Button(
            onClick = {
                error = when {
                    courier.isBlank() -> "Nama kurir wajib diisi."
                    tracking.isBlank() -> "Nomor resi wajib diisi."
                    city.isBlank() -> "Tujuan wajib diisi."
                    recipient.isBlank() -> "Nama penerima wajib diisi."
                    phone.isBlank() -> "Nomor HP wajib diisi."
                    address.isBlank() -> "Alamat lengkap wajib diisi."
                    else -> ""
                }

                if (error.isBlank()) {
                    onSave(
                        order.copy(
                            courier = courier,
                            tracking = tracking,
                            city = city,
                            recipient = recipient,
                            customerPhone = phone,
                            address = address,
                            status = "OTW"
                        )
                    )
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple
            )
        ) {
            Text("OTW Bre", color = White)
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Batal", color = White)
        }
    }
}

@Composable
fun CompletionScreen(
    order: OrderData,
    onComplete: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "🎉",
            fontSize = 76.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "MINUM DULU BRE!",
            color = Purple,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "Keren lu. Pesanan ${order.orderNumber} selesai!",
            color = White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(30.dp))

        Button(
            onClick = onComplete,
            colors = ButtonDefaults.buttonColors(
                containerColor = Purple
            )
        ) {
            Text("Kembali ke Pesanan", color = White)
        }
    }
}

@Composable
private fun fieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = White,
    unfocusedTextColor = White,
    focusedBorderColor = Purple,
    unfocusedBorderColor = Grey,
    focusedLabelColor = Purple,
    unfocusedLabelColor = Grey,
    cursorColor = Purple
)
