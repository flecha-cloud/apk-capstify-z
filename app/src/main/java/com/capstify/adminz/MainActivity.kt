package com.capstify.adminz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.layout.statusBarsPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import android.content.Context
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.graphics.BitmapFactory
import android.net.Uri

private var Bg by mutableStateOf(Color(0xFF101218))
private var Panel by mutableStateOf(Color(0xFF1B1F29))
private var Panel2 by mutableStateOf(Color(0xFF232936))
private var Accent by mutableStateOf(Color(0xFF9BCB45))
private var Muted by mutableStateOf(Color(0xFFAAA8B6))
private var Danger by mutableStateOf(Color(0xFFFF7777))
private var Info by mutableStateOf(Color(0xFF83B9FF))
private var TextColor by mutableStateOf(Color.White)
private var SelectedFont by mutableStateOf("Default")
private var BackgroundImage by mutableStateOf("")
private val pages = listOf(
    "Dashboard",
    "Produk & Kode",
    "Atur Stok",
    "Pesanan",
    "Tempat Sampah",
    "Rekap Penjualan",
    "Backup Data",
    "Pengaturan Tema"
)
private fun saveTheme(context: Context) {
    val pref = context.getSharedPreferences("capstify_theme", Context.MODE_PRIVATE)
    pref.edit()
        .putLong("bg", Bg.value.toLong())
        .putLong("panel", Panel.value.toLong())
        .putLong("panel2", Panel2.value.toLong())
        .putLong("accent", Accent.value.toLong())
        .putLong("muted", Muted.value.toLong())
        .putLong("text", TextColor.value.toLong())
        .putString("font", SelectedFont)
        .putString("background_image", BackgroundImage)
        .apply()
}

private fun loadTheme(context: Context) {
    val pref = context.getSharedPreferences("capstify_theme", Context.MODE_PRIVATE)

    fun color(key: String, default: Color): Color {
        val value = pref.getLong(key, default.value.toLong())
        return Color(value.toULong())
    }

    Bg = color("bg", Color(0xFF101218))
    Panel = color("panel", Color(0xFF1B1F29))
    Panel2 = color("panel2", Color(0xFF232936))
    Accent = color("accent", Color(0xFF9BCB45))
    Muted = color("muted", Color(0xFFAAA8B6))
    TextColor = color("text", Color.White)
    SelectedFont = pref.getString("font", "Default") ?: "Default"
    BackgroundImage = pref.getString("background_image", "") ?: ""
}

private fun resetTheme(context: Context) {
    Bg = Color(0xFF101218)
    Panel = Color(0xFF1B1F29)
    Panel2 = Color(0xFF232936)
    Accent = Color(0xFF9BCB45)
    Muted = Color(0xFFAAA8B6)
    TextColor = Color.White
    SelectedFont = "Default"
    BackgroundImage = ""
    saveTheme(context)
}

private fun selectedFontFamily(): FontFamily {
    return when (SelectedFont) {
        "Sans Serif" -> FontFamily.SansSerif
        "Serif" -> FontFamily.Serif
        "Monospace" -> FontFamily.Monospace
        "Cursive" -> FontFamily.Cursive
        else -> FontFamily.Default
    }
}

data class Product(val id:Int, val sku:String, val name:String, val variant:String, val price:Double, val stock:Int)
data class OrderRow(val id:Int, val number:String, val platform:String, val total:Double, val payment:String, val status:String, val tracking:String, val count:Int, val created:String)

class MainActivity : ComponentActivity() {
    

@Composable
private fun CapstifyApp(api: CapstifyApi) {
    val scope = rememberCoroutineScope()
    var loggedIn by remember { mutableStateOf(false) }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var page by remember { mutableStateOf("Dashboard") }
    var stats by remember { mutableStateOf(JSONObject()) }

    fun runApi(block: suspend () -> Unit) {
        scope.launch {
            busy = true; message = ""
            try { block() } catch (e: Exception) { message = e.message ?: "Terjadi kesalahan." }
            busy = false
        }
    }
    fun loadDashboard() = runApi {
        val data = withContext(Dispatchers.IO) { api.get("/api/dashboard") }
        stats = data.optJSONObject("stats") ?: JSONObject()
    }
    fun go(target:String) { page=target; message=""; if(target=="Dashboard") loadDashboard() }
    BackHandler(loggedIn && page!="Dashboard") { go("Dashboard") }

    LaunchedEffect(Unit) {
        try {
            val me = withContext(Dispatchers.IO) { api.get("/api/me") }
            userName = me.optJSONObject("user")?.optString("name").orEmpty()
            loggedIn = true
            val d = withContext(Dispatchers.IO) { api.get("/api/dashboard") }
            stats = d.optJSONObject("stats") ?: JSONObject()
        } catch (_:Exception) { loggedIn=false }
    }

    if (!loggedIn) {
        LoginScreen(username, {username=it}, password, {password=it}, busy, message) {
            runApi {
                val result = withContext(Dispatchers.IO) {
                    api.post("/api/login", JSONObject().put("username",username).put("password",password))
                }
                userName = result.optJSONObject("user")?.optString("name").orEmpty()
                password=""; loggedIn=true
                val d = withContext(Dispatchers.IO) { api.get("/api/dashboard") }
                stats = d.optJSONObject("stats") ?: JSONObject()
            }
        }
    } else {
        Column(Modifier.fillMaxSize().background(Bg).statusBarsPadding()) {
            var menu by remember { mutableStateOf(false) }
            Row(Modifier.fillMaxWidth().background(Panel).padding(horizontal=16.dp, vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(page, fontSize=21.sp, fontWeight=FontWeight.Bold)
                    Text("Capstify-Admin Z · $userName", color=Muted, fontSize=11.sp)
                }
                Box {
                    IconButton(onClick={menu=true}) { Icon(Icons.Default.Menu, "Menu") }
                    DropdownMenu(expanded=menu, onDismissRequest={menu=false}) {
                        pages.forEach { p -> DropdownMenuItem(text={Text(p)}, onClick={menu=false;go(p)}) }
                        HorizontalDivider()
                        DropdownMenuItem(text={Text("Keluar")}, leadingIcon={Icon(Icons.Default.Logout,null)}, onClick={
                            menu=false
                            runApi {
                                try { withContext(Dispatchers.IO) { api.post("/api/logout") } } catch (_:Exception) {}
                                api.clearSession(); loggedIn=false; page="Dashboard"
                            }
                        })
                    }
                }
            }
            when(page) {
    "Dashboard" -> DashboardScreen(stats, busy, message, {loadDashboard()}, {go(it)})
    "Produk & Kode" -> ProductScreen(api)
    "Atur Stok" -> StockScreen(api)
    "Pesanan" -> OrdersScreen(api, false)
    "Tempat Sampah" -> OrdersScreen(api, true)
    "Rekap Penjualan" -> ReportsScreen(api)
    "Backup Data" -> BackupScreen(api)
    "Pengaturan Tema" -> ThemeSettingsScreen()
            }
        }
    }
}

@Composable
private fun LoginScreen(username:String,onUsername:(String)->Unit,password:String,onPassword:(String)->Unit,busy:Boolean,message:String,onLogin:()->Unit) {
    Column(Modifier.fillMaxSize().background(Bg).padding(24.dp), verticalArrangement=Arrangement.Center) {
        Text("C", color=Accent, fontSize=48.sp, fontWeight=FontWeight.Black)
        Text("Capstify-Admin Z", fontSize=27.sp, fontWeight=FontWeight.Bold)
        Text("Sales Management System · Private Workspace", color=Muted, fontSize=12.sp)
        Spacer(Modifier.height(28.dp))
        OutlinedTextField(username,onUsername,label={Text("Username")},singleLine=true,modifier=Modifier.fillMaxWidth())
        Spacer(Modifier.height(10.dp))
        OutlinedTextField(password,onPassword,label={Text("Password")},singleLine=true,visualTransformation=PasswordVisualTransformation(),keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Password),modifier=Modifier.fillMaxWidth())
        if(message.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(message,color=Danger,fontSize=13.sp) }
        Spacer(Modifier.height(18.dp))
        Button(onClick=onLogin,enabled=!busy && username.isNotBlank() && password.isNotBlank(),modifier=Modifier.fillMaxWidth().height(50.dp)) {
            if(busy) CircularProgressIndicator(Modifier.size(19.dp),strokeWidth=2.dp) else Text("Masuk ke Dashboard",fontWeight=FontWeight.Bold)
        }
    }
}

@Composable
private fun DashboardScreen(stats:JSONObject,busy:Boolean,message:String,onRefresh:()->Unit,onOpen:(String)->Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Text("Ringkasan aktivitas operasional",color=Muted,modifier=Modifier.weight(1f))
            IconButton(onClick=onRefresh){Icon(Icons.Default.Refresh,"Muat ulang")}
        }
        val cards=listOf(
            Triple("Pesanan Hari Ini",stats.optString("ordersToday","—"),"Pesanan"),
            Triple("Total Produk",stats.optString("totalProducts","—"),"Produk & Kode"),
            Triple("Total Stok",stats.optString("totalStock","—"),"Atur Stok"),
            Triple("Pesanan Diproses",stats.optString("pendingShipments","—"),"Pesanan")
        )
        cards.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                pair.forEach { (label,value,target) ->
                    Card(Modifier.weight(1f).padding(vertical=5.dp).clickable{onOpen(target)},colors=CardDefaults.cardColors(containerColor=Panel),shape=RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(15.dp)) {
                            Text(label,color=Muted,fontSize=12.sp); Spacer(Modifier.height(8.dp))
                            Text(value,fontSize=24.sp,fontWeight=FontWeight.Bold); Text("Lihat detail →",color=Accent,fontSize=11.sp)
                        }
                    }
                }
            }
        }
        Card(Modifier.fillMaxWidth().padding(top=10.dp),colors=CardDefaults.cardColors(containerColor=Color(0xFF202A1A))) {
            Column(Modifier.padding(17.dp)) {
                Text("Omzet Hari Ini",color=Muted,fontSize=12.sp)
                Text("Rp${money(stats.optDouble("revenueToday",0.0))}",fontSize=24.sp,fontWeight=FontWeight.Bold)
            }
        }
        if(busy) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=12.dp))
        if(message.isNotBlank()) Text(message,color=Danger,modifier=Modifier.padding(top=10.dp))
        Spacer(Modifier.height(18.dp))
        Text("Database terhubung",color=Accent,fontWeight=FontWeight.SemiBold)
        Text("Data aplikasi menggunakan database Capstify yang sama dengan website.",color=Muted,fontSize=12.sp)
        Spacer(Modifier.height(12.dp))
        pages.drop(1).forEach { p ->
            Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{onOpen(p)},colors=CardDefaults.cardColors(containerColor=Panel)) {
                Row(Modifier.padding(15.dp),verticalAlignment=Alignment.CenterVertically) {
                    Text(p,Modifier.weight(1f),fontWeight=FontWeight.SemiBold)
                    Icon(Icons.Default.ChevronRight,null,tint=Accent)
                }
            }
        }
    }
}

private fun money(value:Double)=String.format(Locale("id","ID"),"%,.0f",value)
private fun JSONObject.arr(key:String)=optJSONArray(key) ?: JSONArray()
private fun JSONArray.objects(): List<JSONObject> = (0 until length()).mapNotNull { optJSONObject(it) }

@Composable
private fun ProductScreen(api:CapstifyApi) {
    val scope= rememberCoroutineScope()
    var products by remember { mutableStateOf(listOf<Product>()) }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<Product?>(null) }
    var deleting by remember { mutableStateOf<Product?>(null) }
    var sku by remember { mutableStateOf("") }; var name by remember { mutableStateOf("") }
    var variant by remember { mutableStateOf("") }; var price by remember { mutableStateOf("") }; var stock by remember { mutableStateOf("") }

    fun load() { scope.launch { loading=true; try {
        val data=withContext(Dispatchers.IO){api.get("/api/products")}
        products=data.arr("products").objects().map { Product(it.optInt("id"),it.optString("sku"),it.optString("name"),it.optString("variant"),it.optDouble("price"),it.optInt("stock")) }
        message=""
    } catch(e:Exception){message=e.message ?: "Gagal memuat produk"}; loading=false } }
    fun form(p:Product?=null) { editing=p; sku=p?.sku.orEmpty(); name=p?.name.orEmpty(); variant=p?.variant.orEmpty(); price=p?.price?.toLong()?.toString().orEmpty(); stock=p?.stock?.toString().orEmpty(); message=""; showForm=true }
    fun save() {
        val pr=price.toDoubleOrNull(); val st=stock.toIntOrNull()
        if(sku.isBlank()||name.isBlank()){message="Kode dan nama produk wajib diisi.";return}
        if(pr==null||pr<0){message="Harga tidak valid.";return}
        if(st==null||st<0){message="Stok tidak valid.";return}
        scope.launch { loading=true; try {
            val body=JSONObject().put("sku",sku.trim()).put("name",name.trim()).put("variant",variant.trim()).put("price",pr).put("stock",st)
            withContext(Dispatchers.IO){ if(editing==null) api.post("/api/products",body) else api.put("/api/products/${editing!!.id}",body) }
            showForm=false; load()
        } catch(e:Exception){message=e.message ?: "Gagal menyimpan"}; loading=false }
    }
    fun remove(p:Product) { scope.launch { loading=true; try { withContext(Dispatchers.IO){api.delete("/api/products/${p.id}")}; deleting=null; load() } catch(e:Exception){message=e.message ?: "Gagal menghapus"}; loading=false } }
    LaunchedEffect(Unit){load()}
    val filtered=products.filter{it.sku.contains(search,true)||it.name.contains(search,true)||it.variant.contains(search,true)}
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically) {
            Column(Modifier.weight(1f)){Text("Kelola Produk",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("${products.size} produk terdaftar",color=Muted,fontSize=12.sp)}
            IconButton(onClick={load()}){Icon(Icons.Default.Refresh,"Refresh")}
        }
        Button(onClick={form()},modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Icon(Icons.Default.Add,null);Spacer(Modifier.width(6.dp));Text("Tambah Produk")}
        OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth().padding(top=8.dp),singleLine=true,label={Text("Cari kode atau nama produk")},leadingIcon={Icon(Icons.Default.Search,null)})
        if(loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=8.dp))
        if(message.isNotBlank()&&!showForm) Text(message,color=Danger,modifier=Modifier.padding(top=8.dp))
        LazyColumn(Modifier.weight(1f).padding(top=8.dp)) {
            items(filtered,key={it.id}) { p ->
                Card(Modifier.fillMaxWidth().padding(vertical=4.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
                    Column(Modifier.padding(14.dp)) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)){Text(p.sku,color=Accent,fontSize=12.sp,fontWeight=FontWeight.Bold);Text(p.name,fontWeight=FontWeight.Bold);Text(p.variant.ifBlank{"Tanpa varian"},color=Muted,fontSize=12.sp)}
                            Text("Stok ${p.stock}",color=Accent,fontWeight=FontWeight.Bold)
                        }
                        Text("Rp${money(p.price)}",modifier=Modifier.padding(top=7.dp))
                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp),modifier=Modifier.fillMaxWidth()) {
                            OutlinedButton(onClick={form(p)},modifier=Modifier.weight(1f)){Icon(Icons.Default.Edit,null);Text("Edit")}
                            OutlinedButton(onClick={deleting=p},modifier=Modifier.weight(1f),colors=ButtonDefaults.outlinedButtonColors(contentColor=Danger)){Icon(Icons.Default.Delete,null);Text("Hapus")}
                        }
                    }
                }
            }
            if(!loading&&filtered.isEmpty()) item { Text("Produk tidak ditemukan.",color=Muted,modifier=Modifier.padding(18.dp)) }
        }
    }
    if(showForm) AlertDialog(onDismissRequest={showForm=false},title={Text(if(editing==null)"Tambah Produk" else "Edit Produk")},text={
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState())) {
            Field("Kode Produk",sku,{sku=it.uppercase()}); Field("Nama Produk",name,{name=it}); Field("Varian",variant,{variant=it})
            Field("Harga",price,{price=it},true); Field("Stok",stock,{stock=it},true)
            if(message.isNotBlank()) Text(message,color=Danger,fontSize=12.sp)
        }
    },confirmButton={TextButton(onClick={save()},enabled=!loading){Text("Simpan")}},dismissButton={TextButton(onClick={showForm=false}){Text("Batal")}},containerColor=Panel)
    deleting?.let { p -> AlertDialog(onDismissRequest={deleting=null},title={Text("Hapus produk?")},text={Text("Hapus ${p.sku} — ${p.name}?")},confirmButton={TextButton(onClick={remove(p)},enabled=!loading){Text("Hapus",color=Danger)}},dismissButton={TextButton(onClick={deleting=null}){Text("Batal")}},containerColor=Panel) }
}

@Composable
private fun StockScreen(api:CapstifyApi) {
    val scope= rememberCoroutineScope()
    var products by remember { mutableStateOf(listOf<Product>()) }
    var movements by remember { mutableStateOf(listOf<JSONObject>()) }
    var selected by remember { mutableStateOf<Product?>(null) }
    var targetStock by remember { mutableStateOf("") }; var quantity by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("in") }; var reference by remember { mutableStateOf("") }; var notes by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("set") }; var loading by remember { mutableStateOf(false) }; var message by remember { mutableStateOf("") }

    fun load() { scope.launch { loading=true; try {
        val p=withContext(Dispatchers.IO){api.get("/api/products")}
        products=p.arr("products").objects().map{Product(it.optInt("id"),it.optString("sku"),it.optString("name"),it.optString("variant"),it.optDouble("price"),it.optInt("stock"))}
        val m=withContext(Dispatchers.IO){api.get("/api/stock-movements")}
        movements=m.arr("movements").objects()
    }catch(e:Exception){message=e.message ?: "Gagal memuat stok"};loading=false } }
    fun submit() {
        val p=selected ?: run {message="Pilih produk terlebih dahulu.";return}
        val body=JSONObject().put("productId",p.id).put("notes",notes)
        if(mode=="set") {
            val n=targetStock.toIntOrNull()
            if(n==null||n<0){message="Masukkan jumlah stok akhir yang valid.";return}
            body.put("stock",n)
        } else {
            val n=quantity.toIntOrNull()
            if(n==null||n<=0){message="Jumlah perubahan harus lebih dari 0.";return}
            body.put("quantity",n).put("type",type).put("reference",reference)
        }
        scope.launch { loading=true;try {
            withContext(Dispatchers.IO){if(mode=="set")api.post("/api/stock-set",body) else api.post("/api/stock-adjustments",body)}
            message="Stok berhasil diperbarui.";targetStock="";quantity="";notes="";reference="";load()
        }catch(e:Exception){message=e.message ?: "Gagal memperbarui stok"};loading=false }
    }
    LaunchedEffect(Unit){load()}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("Manajemen Stok",fontSize=20.sp,fontWeight=FontWeight.Bold)
        Text("Atur stok akhir atau catat stok masuk dan keluar.",color=Muted,fontSize=12.sp)
        Spacer(Modifier.height(12.dp))
        Card(colors=CardDefaults.cardColors(containerColor=Panel)) {
            Column(Modifier.padding(14.dp)) {
                Text("Pilih Produk",fontWeight=FontWeight.Bold)
                var expanded by remember{mutableStateOf(false)}
                Box {
                    OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()) { Text(selected?.let{"${it.sku} · ${it.name}"} ?: "Pilih produk",modifier=Modifier.weight(1f));Icon(Icons.Default.ArrowDropDown,null) }
                    DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}) {
                        products.forEach{p->DropdownMenuItem(text={Text("${p.sku} · ${p.name} (stok ${p.stock})")},onClick={selected=p;expanded=false})}
                    }
                }
                selected?.let{Text("Stok saat ini: ${it.stock}",color=Accent,fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=6.dp))}
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected=mode=="set",onClick={mode="set"},label={Text("Atur Stok Akhir")})
                    FilterChip(selected=mode=="adjust",onClick={mode="adjust"},label={Text("Stok Masuk/Keluar")})
                }
                if(mode=="set") Field("Jumlah stok akhir",targetStock,{targetStock=it},true)
                else {
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected=type=="in",onClick={type="in"},label={Text("Stok Masuk")})
                        FilterChip(selected=type=="out",onClick={type="out"},label={Text("Stok Keluar")})
                    }
                    Field("Jumlah",quantity,{quantity=it},true);Field("Referensi",reference,{reference=it})
                }
                Field("Catatan",notes,{notes=it})
                Button(onClick={submit()},enabled=!loading,modifier=Modifier.fillMaxWidth()){Text("Simpan Perubahan Stok")}
            }
        }
        if(message.isNotBlank()) Text(message,color=if(message.contains("berhasil"))Accent else Danger,modifier=Modifier.padding(vertical=8.dp))
        Text("Riwayat Pergerakan Stok",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=12.dp,bottom=6.dp))
        movements.take(50).forEach { m ->
            Card(Modifier.fillMaxWidth().padding(vertical=3.dp),colors=CardDefaults.cardColors(containerColor=Panel)) {
                Column(Modifier.padding(12.dp)) {
                    Text("${m.optString("sku")} · ${m.optString("name")}",fontWeight=FontWeight.SemiBold)
                    Text("${m.optString("movement_type")}  ${m.optInt("quantity")}",color=Accent)
                    Text("${m.optString("reference")} ${m.optString("notes")}",color=Muted,fontSize=12.sp)
                    Text(m.optString("created_at"),color=Muted,fontSize=10.sp)
                }
            }
        }
    }
}

@Composable
private fun OrdersScreen(api:CapstifyApi,trash:Boolean) {
    val scope= rememberCoroutineScope()
    var orders by remember { mutableStateOf(listOf<OrderRow>()) }
    var loading by remember { mutableStateOf(false) };var message by remember { mutableStateOf("") };var search by remember{mutableStateOf("")}
    var detail by remember{mutableStateOf<OrderRow?>(null)}
    fun load(){scope.launch{loading=true;try{
        val endpoint=if(trash)"/api/orders/trash" else "/api/orders"
        val data=withContext(Dispatchers.IO){api.get(endpoint)}
        orders=data.arr("orders").objects().map{OrderRow(it.optInt("id"),it.optString("order_number"),it.optString("platform"),it.optDouble("total_amount"),it.optString("payment_status"),it.optString("order_status"),it.optString("tracking_number"),it.optInt("item_count"),it.optString("created_at"))}
        message=""
    }catch(e:Exception){message=e.message ?: "Gagal memuat pesanan"};loading=false}}
    fun action(order:OrderRow,action:String,status:String?=null){scope.launch{loading=true;try{
        withContext(Dispatchers.IO){
            when(action){
                "status"->api.post("/api/orders/${order.id}/status",JSONObject().put("status",status))
                "trash"->api.post("/api/orders/${order.id}/trash")
                "restore"->api.post("/api/orders/${order.id}/restore")
                "permanent"->api.post("/api/orders/${order.id}/permanent")
            }
        }
        detail=null;load()
    }catch(e:Exception){message=e.message ?: "Aksi gagal"};loading=false}}
    LaunchedEffect(trash){load()}
    val filtered=orders.filter{it.number.contains(search,true)||it.platform.contains(search,true)||it.tracking.contains(search,true)}
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(if(trash)"Tempat Sampah Pesanan" else "Kelola Pesanan",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("${orders.size} pesanan",color=Muted,fontSize=12.sp)};IconButton(onClick={load()}){Icon(Icons.Default.Refresh,"Refresh")}}
        OutlinedTextField(search,{search=it},modifier=Modifier.fillMaxWidth(),singleLine=true,label={Text("Cari nomor pesanan, platform, resi")},leadingIcon={Icon(Icons.Default.Search,null)})
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth().padding(top=6.dp))
        if(message.isNotBlank())Text(message,color=Danger,modifier=Modifier.padding(vertical=6.dp))
        LazyColumn(Modifier.weight(1f).padding(top=6.dp)) {
            items(filtered,key={it.id}){o->
                Card(Modifier.fillMaxWidth().padding(vertical=4.dp).clickable{detail=o},colors=CardDefaults.cardColors(containerColor=Panel)){
                    Column(Modifier.padding(14.dp)){
                        Row(verticalAlignment=Alignment.CenterVertically){Text(o.number.ifBlank{"Pesanan #${o.id}"},Modifier.weight(1f),fontWeight=FontWeight.Bold);Text(o.status,color=Accent,fontSize=11.sp)}
                        Text("${o.platform} · ${o.count} item · ${o.payment}",color=Muted,fontSize=12.sp)
                        Text("Rp${money(o.total)}",fontWeight=FontWeight.SemiBold,modifier=Modifier.padding(top=5.dp))
                        if(o.tracking.isNotBlank())Text("Resi: ${o.tracking}",color=Muted,fontSize=11.sp)
                        Text(o.created,color=Muted,fontSize=10.sp)
                    }
                }
            }
            if(!loading&&filtered.isEmpty())item{Text("Belum ada pesanan.",color=Muted,modifier=Modifier.padding(16.dp))}
        }
    }
    detail?.let{o->
        var expanded by remember{mutableStateOf(false)}
        AlertDialog(onDismissRequest={detail=null},title={Text(o.number.ifBlank{"Pesanan #${o.id}"})},text={
            Column(Modifier.verticalScroll(rememberScrollState())){
                Text("Platform: ${o.platform}");Text("Pembayaran: ${o.payment}");Text("Status: ${o.status}");Text("Total: Rp${money(o.total)}");Text("Nomor resi: ${o.tracking.ifBlank{"—"}}")
                Spacer(Modifier.height(10.dp))
                if(trash){
                    Button(onClick={action(o,"restore")},modifier=Modifier.fillMaxWidth()){Text("Pulihkan Pesanan")}
                    OutlinedButton(onClick={action(o,"permanent")},modifier=Modifier.fillMaxWidth(),colors=ButtonDefaults.outlinedButtonColors(contentColor=Danger)){Text("Hapus Permanen")}
                }else{
                    Text("Ubah Status",fontWeight=FontWeight.Bold)
                    listOf("processing" to "Diproses","shipped" to "Dikirim","delivered" to "Selesai","cancelled" to "Dibatalkan").forEach{(s,label)->
                        OutlinedButton(onClick={action(o,"status",s)},modifier=Modifier.fillMaxWidth().padding(vertical=2.dp)){Text(label)}
                    }
                    TextButton(onClick={action(o,"trash")}){Text("Pindahkan ke Tempat Sampah",color=Danger)}
                }
            }
        },confirmButton={TextButton(onClick={detail=null}){Text("Tutup")}},containerColor=Panel)
    }
}

@Composable
private fun ReportsScreen(api:CapstifyApi) {
    val scope= rememberCoroutineScope()
    var orders by remember{mutableStateOf(listOf<OrderRow>())};var loading by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")}
    fun load(){scope.launch{loading=true;try{
        val d=withContext(Dispatchers.IO){api.get("/api/orders")}
        orders=d.arr("orders").objects().map{OrderRow(it.optInt("id"),it.optString("order_number"),it.optString("platform"),it.optDouble("total_amount"),it.optString("payment_status"),it.optString("order_status"),it.optString("tracking_number"),it.optInt("item_count"),it.optString("created_at"))}
    }catch(e:Exception){message=e.message ?: "Gagal memuat rekap"};loading=false}}
    LaunchedEffect(Unit){load()}
    val completed=orders.filter{it.status.equals("delivered",true)||it.status.equals("completed",true)}
    val paid=orders.filter{it.payment.equals("paid",true)}
    val revenue=paid.sumOf{it.total}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Rekap Penjualan",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Ringkasan berdasarkan data pesanan",color=Muted,fontSize=12.sp)};IconButton(onClick={load()}){Icon(Icons.Default.Refresh,"Refresh")}}
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(message.isNotBlank())Text(message,color=Danger)
        ReportCard("Total Pesanan",orders.size.toString())
        ReportCard("Pesanan Selesai",completed.size.toString())
        ReportCard("Pesanan Dibayar",paid.size.toString())
        ReportCard("Total Nilai Pesanan Dibayar","Rp${money(revenue)}")
        Text("Ringkasan per Platform",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=14.dp,bottom=6.dp))
        (orders.map{it.platform}.filter{it.isNotBlank()}.distinct().ifEmpty{listOf("Website","TikTok Shop","Shopee")}).forEach{platform->
            val list=orders.filter{it.platform.equals(platform,true)}
            Card(Modifier.fillMaxWidth().padding(vertical=3.dp),colors=CardDefaults.cardColors(containerColor=Panel)){
                Row(Modifier.padding(14.dp),verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text(platform,fontWeight=FontWeight.SemiBold);Text("${list.size} pesanan",color=Muted,fontSize=12.sp)};Text("Rp${money(list.filter{it.payment=="paid"}.sumOf{it.total})}",color=Accent,fontWeight=FontWeight.Bold)}
            }
        }
        Text("Catatan: rekap dihitung dari data pesanan yang dikembalikan server; total pendapatan memakai pesanan berstatus pembayaran paid.",color=Muted,fontSize=11.sp,modifier=Modifier.padding(top=12.dp))
    }
}

@Composable
private fun BackupScreen(api:CapstifyApi) {
    val scope= rememberCoroutineScope()
    var jobs by remember{mutableStateOf(listOf<JSONObject>())};var loading by remember{mutableStateOf(false)};var message by remember{mutableStateOf("")}
    var showRestore by remember{mutableStateOf(false)}
    var fileId by remember{mutableStateOf("")};var restorePassword by remember{mutableStateOf("")};var confirmation by remember{mutableStateOf("")}
    fun load(){scope.launch{loading=true;try{val d=withContext(Dispatchers.IO){api.get("/api/backups")};jobs=d.arr("jobs").objects();message=""}catch(e:Exception){message=e.message ?: "Gagal memuat backup"};loading=false}}
    fun request(){scope.launch{loading=true;try{val d=withContext(Dispatchers.IO){api.post("/api/backups/request")};message=d.optString("message").ifBlank{"Permintaan backup berhasil dibuat."};load()}catch(e:Exception){message=e.message ?: "Gagal meminta backup"};loading=false}}
    fun restore(){if(fileId.isBlank()||restorePassword.isBlank()||confirmation!="PULIHKAN DATA"){message="Isi File ID, password, dan konfirmasi persis PULIHKAN DATA.";return}
        scope.launch{loading=true;try{val body=JSONObject().put("fileId",fileId.trim()).put("password",restorePassword).put("confirmation",confirmation);val d=withContext(Dispatchers.IO){api.post("/api/backups/restore",body)};message=d.optString("message").ifBlank{"Permintaan pemulihan dikirim."};showRestore=false;load()}catch(e:Exception){message=e.message ?: "Pemulihan gagal"};loading=false}}
    LaunchedEffect(Unit){load()}
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)){
        Row(verticalAlignment=Alignment.CenterVertically){Column(Modifier.weight(1f)){Text("Backup Data",fontSize=20.sp,fontWeight=FontWeight.Bold);Text("Cadangkan dan pulihkan database",color=Muted,fontSize=12.sp)};IconButton(onClick={load()}){Icon(Icons.Default.Refresh,"Refresh")}}
        Card(colors=CardDefaults.cardColors(containerColor=Panel)){
            Column(Modifier.padding(15.dp)){
                Text("Buat Backup",fontWeight=FontWeight.Bold)
                Text("Minta server membuat salinan database terbaru.",color=Muted,fontSize=12.sp)
                Button(onClick={request()},enabled=!loading,modifier=Modifier.fillMaxWidth().padding(top=10.dp)){Icon(Icons.Default.CloudUpload,null);Text("Buat Backup Sekarang")}
            }
        }
        Card(Modifier.padding(top=10.dp),colors=CardDefaults.cardColors(containerColor=Panel)){
            Column(Modifier.padding(15.dp)){
                Text("Pulihkan Data",fontWeight=FontWeight.Bold)
                Text("Pemulihan akan mengganti data database. Pastikan file dan password benar.",color=Danger,fontSize=12.sp)
                OutlinedButton(onClick={showRestore=true},modifier=Modifier.fillMaxWidth().padding(top=8.dp)){Icon(Icons.Default.Restore,null);Text("Buka Pemulihan")}
            }
        }
        if(message.isNotBlank())Text(message,color=if(message.contains("berhasil",true))Accent else Danger,modifier=Modifier.padding(vertical=10.dp))
        Text("Riwayat Backup",fontWeight=FontWeight.Bold,modifier=Modifier.padding(top=14.dp,bottom=6.dp))
        if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
        jobs.forEach{j->
            Card(Modifier.fillMaxWidth().padding(vertical=3.dp),colors=CardDefaults.cardColors(containerColor=Panel)){
                Column(Modifier.padding(13.dp)){
                    Text(j.optString("file_name").ifBlank{j.optString("filename").ifBlank{"Backup ${j.optString("id")}" }},fontWeight=FontWeight.SemiBold)
                    Text("Status: ${j.optString("status","—")}",color=Accent,fontSize=12.sp)
                    Text(j.optString("created_at"),color=Muted,fontSize=11.sp)
                    if(j.optString("id").isNotBlank())Text("File ID: ${j.optString("id")}",color=Muted,fontSize=10.sp)
                }
            }
        }
        if(!loading&&jobs.isEmpty())Text("Belum ada riwayat backup yang tersedia.",color=Muted)
    }
    if(showRestore)AlertDialog(onDismissRequest={showRestore=false},title={Text("Pulihkan Database")},text={
        Column(Modifier.verticalScroll(rememberScrollState())){
            Field("File ID",fileId,{fileId=it})
            OutlinedTextField(restorePassword,{restorePassword=it},label={Text("Password Admin")},visualTransformation=PasswordVisualTransformation(),singleLine=true,modifier=Modifier.fillMaxWidth())
            Field("Ketik PULIHKAN DATA",confirmation,{confirmation=it})
            Text("Tindakan ini berisiko mengganti data yang sedang tersimpan.",color=Danger,fontSize=11.sp)
        }
    },confirmButton={TextButton(onClick={restore()},enabled=!loading){Text("Pulihkan",color=Danger)}},dismissButton={TextButton(onClick={showRestore=false}){Text("Batal")}},containerColor=Panel)
}

@Composable
private fun Field(label:String,value:String,onValue:(String)->Unit,numeric:Boolean=false){
    OutlinedTextField(value,onValue,label={Text(label)},singleLine=true,keyboardOptions=if(numeric)KeyboardOptions(keyboardType=KeyboardType.Number) else KeyboardOptions.Default,modifier=Modifier.fillMaxWidth().padding(vertical=3.dp))
}
@Composable
private fun ReportCard(label:String,value:String){
    Card(Modifier.fillMaxWidth().padding(vertical=4.dp),colors=CardDefaults.cardColors(containerColor=Panel)){
        Column(Modifier.padding(15.dp)){Text(label,color=Muted,fontSize=12.sp);Text(value,fontSize=22.sp,fontWeight=FontWeight.Bold)}
    }
}

@Composable
private fun ThemeSettingsScreen() {
    val context = LocalContext.current

    var bgHex by remember { mutableStateOf("#101218") }
    var panelHex by remember { mutableStateOf("#1B1F29") }
    var accentHex by remember { mutableStateOf("#9BCB45") }
    var textHex by remember { mutableStateOf("#FFFFFF") }
    var fontMenu by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}

            BackgroundImage = uri.toString()
            saveTheme(context)
        }
    }

    fun updateColor(value: String, target: String) {
        try {
            val parsed = Color(android.graphics.Color.parseColor(value))
            when (target) {
                "bg" -> Bg = parsed
                "panel" -> Panel = parsed
                "accent" -> Accent = parsed
                "text" -> TextColor = parsed
            }
            saveTheme(context)
        } catch (_: Exception) {}
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(
            "Pengaturan Tema",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "Sesuaikan tampilan Capstify-Admin Z.",
            color = Muted,
            fontSize = 12.sp
        )

        Spacer(Modifier.height(18.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Panel
            )
        ) {
            Column(Modifier.padding(15.dp)) {
                Text("Warna Tampilan", fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = bgHex,
                    onValueChange = {
                        bgHex = it
                        updateColor(it, "bg")
                    },
                    label = { Text("Warna Background") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = panelHex,
                    onValueChange = {
                        panelHex = it
                        updateColor(it, "panel")
                    },
                    label = { Text("Warna Panel / Card") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = accentHex,
                    onValueChange = {
                        accentHex = it
                        updateColor(it, "accent")
                    },
                    label = { Text("Warna Aksen") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = textHex,
                    onValueChange = {
                        textHex = it
                        updateColor(it, "text")
                    },
                    label = { Text("Warna Teks") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Gunakan format HEX, contoh: #FFFFFF",
                    color = Muted,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(top = 5.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Panel
            )
        ) {
            Column(Modifier.padding(15.dp)) {
                Text("Pilihan Font", fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(8.dp))

                Box {
                    OutlinedButton(
                        onClick = { fontMenu = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            SelectedFont,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, null)
                    }

                    DropdownMenu(
                        expanded = fontMenu,
                        onDismissRequest = { fontMenu = false }
                    ) {
                        listOf(
                            "Default",
                            "Sans Serif",
                            "Serif",
                            "Monospace",
                            "Cursive"
                        ).forEach { font ->
                            DropdownMenuItem(
                                text = { Text(font) },
                                onClick = {
                                    SelectedFont = font
                                    fontMenu = false
                                    saveTheme(context)
                                }
                            )
                        }
                    }
                }

                Text(
                    "Contoh tampilan teks Capstify-Admin Z",
                    fontFamily = selectedFontFamily(),
                    color = TextColor,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Panel
            )
        ) {
            Column(Modifier.padding(15.dp)) {
                Text("Background Gambar", fontWeight = FontWeight.Bold)

                Text(
                    "Pilih gambar dari galeri perangkat.",
                    color = Muted,
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(10.dp))

                Button(
                    onClick = {
                        imagePicker.launch(arrayOf("image/*"))
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Image, null)
                    Spacer(Modifier.width(7.dp))
                    Text("Pilih Gambar dari Galeri")
                }

                if (BackgroundImage.isNotBlank()) {
                    Text(
                        "Gambar latar sudah dipilih.",
                        color = Accent,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    TextButton(
                        onClick = {
                            BackgroundImage = ""
                            saveTheme(context)
                        }
                    ) {
                        Text("Hapus Pilihan Gambar")
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        Card(
            colors = CardDefaults.cardColors(
                containerColor = Panel2
            )
        ) {
            Column(Modifier.padding(15.dp)) {
                Text("Preview Tema", fontWeight = FontWeight.Bold)

                Spacer(Modifier.height(10.dp))

                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Bg
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Capstify-Admin Z",
                            color = TextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        Text(
                            "Contoh teks sekunder",
                            color = Muted,
                            fontSize = 12.sp
                        )

                        Spacer(Modifier.height(10.dp))

                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Accent
                            )
                        ) {
                            Text("Contoh Tombol")
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        OutlinedButton(
            onClick = {
                resetTheme(context)

                bgHex = "#101218"
                panelHex = "#1B1F29"
                accentHex = "#9BCB45"
                textHex = "#FFFFFF"
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.RestartAlt, null)
            Spacer(Modifier.width(7.dp))
            Text("Reset ke Tema Default")
        }

        Spacer(Modifier.height(20.dp))
    }
}
