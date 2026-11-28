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
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Black = Color(0xFF000000)
private val Purple = Color(0xFF760FFA)
private val White = Color(0xFFFFFFFF)
private val Grey = Color(0xFFAAA3B5)
private val DarkCard = Color(0xFF17131F)
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
            "CAPSTIFY",
            color = Purple,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Admin Z",
            color = White,
            fontSize = 36.sp,
            fontWeight = FontWeight.ExtraBold
        )

        Text(
            "Order Management",
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
                message,
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
                if (loading) "Memproses..." else "Masuk",
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
    onCompleteOrder: suspend (OrderData) -> Unit,
    onLogout: () -> Unit
) {
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("Semua") }
    var selected by remember { mutableStateOf<OrderData?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var celebration by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    var completing by remember { mutableStateOf(false) }
    var completionError by remember { mutableStateOf("") }

    if (showForm) {
        OrderEditor(
            order = selected,
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
            onComplete = { order ->
                if (!completing && !order.synced) {
                    completing = true
                    completionError = ""

                    scope.launch {
                        try {
                            // Kirim pesanan ke website terlebih dahulu.
                            onCompleteOrder(order)

                            // Tandai selesai hanya setelah website berhasil.
                            val completed = order.copy(
                                status = "Dah Kelar Bre",
                                synced = true
                            )

                            val updated = if (selected == null) {
                                orders + completed
                            } else {
                                orders.map {
                                    if (it.id == completed.id) {
                                        completed
                                    } else {
                                        it
                                    }
                                }
                            }

                            onOrdersChanged(updated)

                            showForm = false
                            selected = null
                            celebration = true

                        } catch (e: Exception) {
                            completionError = e.message
                                ?: "Gagal mengirim pesanan ke website."
                        } finally {
                            completing = false
                        }
                    }
                }
            },
            onCancel = {
                if (!completing) {
                    showForm = false
                    selected = null
                    completionError = ""
                }
            }
        )

        if (completing) {
            AlertDialog(
                onDismissRequest = {},
                containerColor = DarkCard,
                title = {
                    Text(
                        "Mengirim Pesanan",
                        color = White
                    )
                },
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Purple,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            "Menghubungkan ke website...",
                            color = Grey
                        )
                    }
                },
                confirmButton = {}
            )
        }

        if (completionError.isNotBlank()) {
            AlertDialog(
                onDismissRequest = {
                    completionError = ""
                },
                containerColor = DarkCard,
                title = {
                    Text(
                        "Gagal Mengirim Pesanan",
                        color = Red
                    )
                },
                text = {
                    Text(
                        completionError,
                        color = White
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            completionError = ""
                        }
                    ) {
                        Text("Mengerti", color = Purple)
                    }
                }
            )
        }

        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "CAPSTIFY",
                    color = Purple,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Pesanan",
                    color = White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "Halo, ${userName.ifBlank { "Admin" }}",
                    color = Grey,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onRefreshProducts) {
                Icon(
                    Icons.Default.Refresh,
                    contentDescription = "Refresh produk",
                    tint = Grey
                )
            }

            IconButton(onClick = onLogout) {
                Icon(
                    Icons.Default.Logout,
                    contentDescription = "Keluar",
                    tint = Grey
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = {
                selected = null
                completionError = ""
                showForm = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
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
            "${orders.size} pesanan tersimpan",
            color = Grey,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(8.dp))

        val filtered = orders.filter { order ->
            val matchesFilter =
                filter == "Semua" || order.status == filter

            val matchesSearch = listOf(
                order.product,
                order.orderNumber,
                order.recipient,
                order.tracking,
                order.platform
            ).any {
                it.contains(search, ignoreCase = true)
            }

            matchesFilter && matchesSearch
        }

        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = Grey,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(Modifier.height(10.dp))

                    Text(
                        "Belum ada pesanan",
                        color = Grey
                    )

                    Text(
                        "Tekan Tambah Pesanan untuk memulai.",
                        color = Grey,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.id }
                ) { order ->
                    OrderCard(
                        order = order,
                        onClick = {
                            selected = order
                            completionError = ""
                            showForm = true
                        },
                        onToggleLock = {
                            val updated = orders.map {
                                if (it.id == order.id) {
                                    it.copy(locked = !it.locked)
                                } else {
                                    it
                                }
                            }
                            onOrdersChanged(updated)
                        }
                    )
                }
            }
        }
    }

    if (celebration) {
        AlertDialog(
            onDismissRequest = {
                celebration = false
            },
            containerColor = DarkCard,
            title = {
                Text(
                    "Dah Kelar Bre! 🎉",
                    color = Purple
                )
            },
            text = {
                Text(
                    "Pesanan berhasil dikirim ke website dan stok telah diperbarui.",
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
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderEditor(
    order: OrderData?,
    products: List<ProductData>,
    loadingProducts: Boolean,
    onRefreshProducts: () -> Unit,
    onSave: (OrderData) -> Unit,
    onComplete: (OrderData) -> Unit,
    onCancel: () -> Unit
) {
    var platform by remember(order?.id) {
        mutableStateOf(order?.platform ?: "TikTok")
    }

    var productId by remember(order?.id) {
        mutableStateOf(order?.productId ?: 0L)
    }

    var quantityText by remember(order?.id) {
        mutableStateOf(order?.quantity ?: "1")
    }

    var orderNumber by remember(order?.id) {
        mutableStateOf(order?.orderNumber ?: "")
    }

    var orderDate by remember(order?.id) {
        mutableStateOf(order?.orderDate ?: "")
    }

    var courier by remember(order?.id) {
        mutableStateOf(order?.courier ?: "")
    }

    var tracking by remember(order?.id) {
        mutableStateOf(order?.tracking ?: "")
    }

    var city by remember(order?.id) {
        mutableStateOf(order?.city ?: "")
    }

    var recipient by remember(order?.id) {
        mutableStateOf(order?.recipient ?: "")
    }

    var phone by remember(order?.id) {
        mutableStateOf(order?.customerPhone ?: "")
    }

    var address by remember(order?.id) {
        mutableStateOf(order?.address ?: "")
    }

    var error by remember { mutableStateOf("") }
    var platformExpanded by remember { mutableStateOf(false) }
    var productExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateState = rememberDatePickerState()

    val selectedProduct = products.find {
        it.id == productId
    }

    val quantity = quantityText.toIntOrNull() ?: 0
    val stock = selectedProduct?.stock ?: 0

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
        "Padang",
        "Batam",
        "Cirebon",
        "Kediri",
        "Madiun",
        "Jambi",
        "Manado",
        "Samarinda",
        "Ambon",
        "Jayapura"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            if (order == null) "Pesanan Baru" else "Detail Pesanan",
            color = White,
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold
        )

        if (order?.locked == true) {
            Text(
                "Pesanan ini terkunci. Buka kunci dari daftar pesanan untuk mengedit.",
                color = Red
            )
            return@Column
        }

        if (order?.synced == true) {
            Text(
                "Pesanan ini sudah berhasil dikirim ke website.",
                color = Green,
                fontSize = 13.sp
            )
        }

        Text("Platform", color = Grey)

        ExposedDropdownMenuBox(
            expanded = platformExpanded,
            onExpandedChange = {
                platformExpanded = !platformExpanded
            }
        ) {
            OutlinedTextField(
                value = platform,
                onValueChange = {},
                readOnly = true,
                label = { Text("Pilih platform") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = platformExpanded
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = fieldColors()
            )

            ExposedDropdownMenu(
                expanded = platformExpanded,
                onDismissRequest = {
                    platformExpanded = false
                }
            ) {
                listOf("TikTok", "Shopee", "Web").forEach {
                    DropdownMenuItem(
                        text = { Text(it) },
                        onClick = {
                            platform = it
                            platformExpanded = false
                        }
                    )
                }
            }
        }

        Text("Produk", color = Grey)

        ExposedDropdownMenuBox(
            expanded = productExpanded,
            onExpandedChange = {
                productExpanded = !productExpanded
            }
        ) {
            OutlinedTextField(
                value = selectedProduct?.displayName
                    ?: "Pilih produk",
                onValueChange = {},
                readOnly = true,
                label = { Text("Produk website") },
                trailingIcon = {
                    ExposedDropdownMenuDefaults.TrailingIcon(
                        expanded = productExpanded
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor(),
                colors = fieldColors()
            )

            ExposedDropdownMenu(
                expanded = productExpanded,
                onDismissRequest = {
                    productExpanded = false
                }
            ) {
                products.forEach { product ->
                    DropdownMenuItem(
                        text = {
                            Column {
                                Text(product.displayName)
                                Text(
                                    "Stok: ${product.stock}",
                                    color = Grey,
                                    fontSize = 12.sp
                                )
                            }
                        },
                        onClick = {
                            productId = product.id
                            productExpanded = false
                        }
                    )
                }
            }
        }

        if (loadingProducts) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    color = Purple,
                    strokeWidth = 2.dp
                )
                Spacer(Modifier.width(8.dp))
                Text("Memuat produk...", color = Grey)
            }
        } else if (products.isEmpty()) {
            Text(
                "Daftar produk kosong.",
                color = Red
            )
            OutlinedButton(
                onClick = onRefreshProducts,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Muat Ulang Produk")
            }
        }

        OutlinedTextField(
            value = quantityText,
            onValueChange = {
                quantityText = it.filter { c -> c.isDigit() }
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
                "Stok tersedia: $stock",
                color = if (quantity > stock) Red else Green,
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
                IconButton(
                    onClick = { showDatePicker = true }
                ) {
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
            DatePickerDialog(
                onDismissRequest = {
                    showDatePicker = false
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            dateState.selectedDateMillis?.let {
                                orderDate = SimpleDateFormat(
                                    "dd-MM-yyyy",
                                    Locale("id", "ID")
                                ).format(Date(it))
                            }
                            showDatePicker = false
                        }
                    ) {
                        Text("Pilih", color = Purple)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDatePicker = false
                        }
                    ) {
                        Text("Batal", color = Grey)
                    }
                }
            ) {
                DatePicker(state = dateState)
            }
        }

        Divider(color = DarkCard)

        Text(
            "Informasi Pengiriman",
            color = White,
            fontSize = 19.sp,
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
            onExpandedChange = {
                cityExpanded = !cityExpanded
            }
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
                onDismissRequest = {
                    cityExpanded = false
                }
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
            minLines = 3,
            modifier = Modifier.fillMaxWidth(),
            colors = fieldColors()
        )

        if (error.isNotBlank()) {
            Text(
                error,
                color = Red
            )
        }

        Button(
            onClick = {
                error = when {
                    selectedProduct == null ->
                        "Pilih produk terlebih dahulu."

                    quantity <= 0 ->
                        "Jumlah harus lebih dari 0."

                    quantity > stock ->
                        "Stok tidak mencukupi. Tersedia: $stock."

                    orderNumber.isBlank() ->
                        "Nomor pesanan wajib diisi."

                    orderDate.isBlank() ->
                        "Tanggal pesanan wajib dipilih."

                    else -> ""
                }

                if (error.isBlank() && selectedProduct != null) {
                    onSave(
                        createOrder(
                            old = order,
                            platform = platform,
                            product = selectedProduct,
                            quantity = quantity,
                            orderNumber = orderNumber,
                            orderDate = orderDate,
                            courier = courier,
                            tracking = tracking,
                            city = city,
                            recipient = recipient,
                            phone = phone,
                            address = address
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
            Text("Simpan Pesanan", color = White)
        }

        Button(
            onClick = {
                error = when {
                    order?.synced == true ->
                        "Pesanan ini sudah pernah dikirim ke website."

                    selectedProduct == null ->
                        "Pilih produk terlebih dahulu."

                    quantity <= 0 ->
                        "Jumlah harus lebih dari 0."

                    quantity > stock ->
                        "Stok tidak mencukupi."

                    orderNumber.isBlank() ->
                        "Nomor pesanan wajib diisi."

                    orderDate.isBlank() ->
                        "Tanggal pesanan wajib dipilih."

                    courier.isBlank() ->
                        "Kurir wajib diisi."

                    tracking.isBlank() ->
                        "Nomor resi wajib diisi."

                    city.isBlank() ->
                        "Tujuan wajib diisi."

                    recipient.isBlank() ->
                        "Nama penerima wajib diisi."

                    phone.isBlank() ->
                        "Nomor HP wajib diisi."

                    address.isBlank() ->
                        "Alamat wajib diisi."

                    else -> ""
                }

                if (
                    error.isBlank() &&
                    selectedProduct != null &&
                    order?.synced != true
                ) {
                    onComplete(
                        createOrder(
                            old = order,
                            platform = platform,
                            product = selectedProduct,
                            quantity = quantity,
                            orderNumber = orderNumber,
                            orderDate = orderDate,
                            courier = courier,
                            tracking = tracking,
                            city = city,
                            recipient = recipient,
                            phone = phone,
                            address = address
                        ).copy(status = "Dah Kelar Bre")
                    )
                }
            },
            enabled = order?.synced != true,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Green,
                disabledContainerColor = DarkCard
            )
        ) {
            Text(
                if (order?.synced == true) {
                    "Sudah Dikirim ke Website"
                } else {
                    "Dah Kelar Bre"
                },
                color = if (order?.synced == true) Grey else Black
            )
        }

        OutlinedButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Kembali", color = White)
        }
    }
}
private fun createOrder(
    old: OrderData?,
    platform: String,
    product: ProductData,
    quantity: Int,
    orderNumber: String,
    orderDate: String,
    courier: String,
    tracking: String,
    city: String,
    recipient: String,
    phone: String,
    address: String
): OrderData {
    return OrderData(
        id = old?.id ?: System.currentTimeMillis(),
        platform = platform,
        product = product.displayName,
        quantity = quantity.toString(),
        price = product.price.toString(),
        orderNumber = orderNumber,
        orderDate = orderDate,
        courier = courier,
        tracking = tracking,
        city = city,
        recipient = recipient,
        address = address,
        status = old?.status ?: "Packing Dulu Bre",
        locked = old?.locked ?: false,
        productId = product.id,
        customerPhone = phone,
        synced = old?.synced ?: false
    )
}

@Composable
fun OrderCard(
    order: OrderData,
    onClick: () -> Unit,
    onToggleLock: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !order.locked) {
                onClick()
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkCard
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        order.orderNumber.ifBlank {
                            "Pesanan tanpa nomor"
                        },
                        color = White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Text(
                        "${order.platform} • ${order.orderDate}",
                        color = Grey,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = onToggleLock
                ) {
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
                        tint = if (order.locked) Red else Green
                    )
                }
            }

            Divider(color = Black)

            Text(
                order.product,
                color = White,
                fontWeight = FontWeight.Medium
            )

            Text(
                "Jumlah: ${order.quantity}",
                color = Grey,
                fontSize = 13.sp
            )

            Text(
                "Penerima: ${order.recipient.ifBlank { "-" }}",
                color = Grey,
                fontSize = 13.sp
            )

            Text(
                "Kurir: ${order.courier.ifBlank { "-" }}",
                color = Grey,
                fontSize = 13.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = when (order.status) {
                        "Dah Kelar Bre" ->
                            Green.copy(alpha = 0.15f)

                        "OTW Kirim" ->
                            Purple.copy(alpha = 0.2f)

                        else ->
                            Grey.copy(alpha = 0.15f)
                    }
                ) {
                    Text(
                        order.status,
                        color = when (order.status) {
                            "Dah Kelar Bre" -> Green
                            "OTW Kirim" -> Purple
                            else -> White
                        },
                        fontSize = 11.sp,
                        modifier = Modifier.padding(
                            horizontal = 12.dp,
                            vertical = 7.dp
                        )
                    )
                }

                if (order.locked) {
                    Text(
                        "TERKUNCI",
                        color = Red,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        "Ketuk untuk detail",
                        color = Grey,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun fieldColors() =
    OutlinedTextFieldDefaults.colors(
        focusedTextColor = White,
        unfocusedTextColor = White,
        disabledTextColor = Grey,
        focusedBorderColor = Purple,
        unfocusedBorderColor = Grey,
        focusedLabelColor = Purple,
        unfocusedLabelColor = Grey,
        cursorColor = Purple,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard,
        disabledContainerColor = DarkCard,
        focusedLeadingIconColor = Purple,
        unfocusedLeadingIconColor = Grey,
        focusedTrailingIconColor = Purple,
        unfocusedTrailingIconColor = Grey
    )
