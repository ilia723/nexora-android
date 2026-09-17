package top.nexora.app

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val API_URL = "https://admintel.s14.telviprobot.top/bot/android_api_production.php"
private val JSON = "application/json; charset=utf-8".toMediaType()
private val client = OkHttpClient()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexoraApp(this) }
    }
}

@Composable
private fun NexoraApp(context: Context) {
    val prefs = context.getSharedPreferences("nexora", Context.MODE_PRIVATE)
    var token by remember { mutableStateOf(prefs.getString("access_token", null)) }
    if (token.isNullOrBlank()) {
        LoginScreen(onLogin = {
            prefs.edit().putString("access_token", it).apply()
            token = it
        })
    } else {
        DashboardScreen(token!!, onLogout = {
            prefs.edit().remove("access_token").apply()
            token = null
        })
    }
}

@Composable
private fun LoginScreen(onLogin: (String) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("شماره موبایل خود را وارد کنید") }
    val scope = rememberCoroutineScope()

    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
        Text("NEXORA", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(8.dp))
        Text("ورود با شماره موبایل")
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(phone, { phone = it }, Modifier.fillMaxWidth(), label = { Text("شماره موبایل") })
        Spacer(Modifier.height(12.dp))
        Button(enabled = !busy, onClick = {
            busy = true
            scope.launch {
                val r = api("send_otp", mapOf("phone" to phone))
                busy = false
                message = r.optString("message", "خطا")
                if (r.optBoolean("ok")) sent = true
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("ارسال کد تأیید") }
        if (sent) {
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(code, { code = it }, Modifier.fillMaxWidth(), label = { Text("کد پیامک") })
            Spacer(Modifier.height(12.dp))
            Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    val r = api("verify_otp", mapOf("phone" to phone, "code" to code))
                    busy = false
                    message = r.optString("message", "خطا")
                    if (r.optBoolean("ok")) onLogin(r.optString("access_token"))
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("تأیید و ورود") }
        }
        Spacer(Modifier.height(16.dp))
        Text(message)
    }
}

@Composable
private fun DashboardScreen(token: String, onLogout: () -> Unit) {
    var user by remember { mutableStateOf<JSONObject?>(null) }
    var message by remember { mutableStateOf("در حال دریافت اطلاعات...") }
    var wallet by remember { mutableStateOf("") }
    var walletInput by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var exchangeAmount by remember { mutableStateOf("") }
    var exchangeRequest by remember { mutableStateOf("") }
    var referral by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    fun refresh() = scope.launch {
        val r = api("me", emptyMap(), token)
        if (r.optBoolean("ok")) user = r.optJSONObject("user") else message = r.optString("message")
        val w = api("wallet", emptyMap(), token)
        if (w.optBoolean("ok")) wallet = w.optString("address", "none")
    }
    LaunchedEffect(Unit) { refresh() }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
        Text("NEXORA", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(12.dp))
        user?.let { u ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("حساب کاربری")
                    Spacer(Modifier.height(8.dp))
                    Text("موجودی: ${u.optString("coin")} TON")
                    Text("سپرده: ${u.optString("stake")} TON")
                    Text("زیرمجموعه: ${u.optString("invite")}")
                    Text("موبایل: ${u.optString("phone")}")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(message)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(modifier = Modifier.weight(1f), onClick = { scope.launch { message = api("daily_bonus", emptyMap(), token).optString("message", ""); refresh() } }) { Text("پاداش روزانه") }
            Button(modifier = Modifier.weight(1f), onClick = { scope.launch { message = api("stake_profit", emptyMap(), token).optString("message", ""); refresh() } }) { Text("سود روزانه") }
        }
        Spacer(Modifier.height(8.dp))
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            scope.launch {
                val r = api("referral", emptyMap(), token)
                referral = r.optString("link", "")
                message = r.optString("text", r.optString("message", ""))
            }
        }) { Text("سیستم دعوت") }
        if (referral.isNotBlank()) Text(referral)
        Spacer(Modifier.height(16.dp))
        Text("کیف پول", style = MaterialTheme.typography.titleMedium)
        Text(if (wallet.isBlank() || wallet == "none") "ثبت نشده" else wallet)
        OutlinedTextField(walletInput, { walletInput = it }, Modifier.fillMaxWidth(), label = { Text("آدرس کیف پول") })
        Button(modifier = Modifier.fillMaxWidth(), onClick = { scope.launch { message = api("set_wallet", mapOf("address" to walletInput), token).optString("message", ""); refresh() } }) { Text("ذخیره کیف پول") }
        Spacer(Modifier.height(16.dp))
        Text("تبدیل موجودی به سپرده", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(amount, { amount = it }, Modifier.fillMaxWidth(), label = { Text("مقدار TON") })
        Button(modifier = Modifier.fillMaxWidth(), onClick = { scope.launch { message = api("convert", mapOf("amount" to amount), token).optString("message", ""); refresh() } }) { Text("تبدیل") }
        Spacer(Modifier.height(16.dp))
        Text("صرافی", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(exchangeAmount, { exchangeAmount = it }, Modifier.fillMaxWidth(), label = { Text("مبلغ تومان") })
        Button(modifier = Modifier.fillMaxWidth(), onClick = {
            scope.launch {
                val r = api("exchange_start", mapOf("toman" to exchangeAmount), token)
                if (r.optBoolean("ok")) exchangeRequest = r.optString("request_id") + "\nکارت مقصد: " + r.optString("recipient_card") + "\nمقدار: " + r.optString("coin")
                message = r.optString("message", "")
            }
        }) { Text("ایجاد درخواست صرافی") }
        if (exchangeRequest.isNotBlank()) Text(exchangeRequest)
        Spacer(Modifier.height(20.dp))
        OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { scope.launch { api("logout", emptyMap(), token); onLogout() } }) { Text("خروج از حساب") }
    }
}

private suspend fun api(action: String, values: Map<String, String>, token: String? = null): JSONObject = withContext(Dispatchers.IO) {
    val obj = JSONObject().put("action", action)
    values.forEach { (k, v) -> obj.put(k, v) }
    val req = Request.Builder().url(API_URL)
        .post(obj.toString().toRequestBody(JSON))
        .apply { if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token") }
        .build()
    try {
        client.newCall(req).execute().use { JSONObject(it.body?.string().orEmpty()) }
    } catch (e: Exception) {
        JSONObject().put("ok", false).put("message", "خطا در ارتباط با سرور: ${e.message ?: "نامشخص"}")
    }
}
