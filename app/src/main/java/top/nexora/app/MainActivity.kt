package top.nexora.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject

private const val API = "https://admintel.s14.telviprobot.top/bot/android_api_production.php"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { NexoraApp() }
    }
}

@Composable
fun NexoraApp() {
    var loggedIn by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var sent by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("ورود با شماره موبایل یا تلگرام") }
    var user by remember { mutableStateOf<JSONObject?>(null) }

    fun apiCall(action: String, data: JSONObject, done: (JSONObject?) -> Unit) {
        busy = true
        Thread {
            try {
                val payload = JSONObject(data.toString()).put("action", action)
                val body = payload.toString().toRequestBody("application/json".toMediaType())
                val req = Request.Builder().url(API).post(body).addHeader("Accept", "application/json").build()
                val res = OkHttpClient().newCall(req).execute()
                val text = res.body?.string() ?: "{\"ok\":false,\"message\":\"پاسخ خالی از سرور\"}"
                val obj = JSONObject(text)
                runOnUiThread { busy = false; done(obj) }
            } catch (_: Exception) {
                runOnUiThread { busy = false; message = "ارتباط با سرور برقرار نشد."; done(null) }
            }
        }.start()
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Color(0xFF18A5EE), background = Color(0xFF050B12), surface = Color(0xFF071321))) {
        Surface(Modifier.fillMaxSize(), color = Color(0xFF050B12)) {
            if (!loggedIn) LoginScreen(phone, { phone = it }, code, { code = it }, sent, busy, message,
                { message = "ورود با تلگرام در مرحله بعدی تکمیل می‌شود." },
                { apiCall("send_otp", JSONObject().put("phone", phone)) { if (it?.optBoolean("ok") == true) { sent = true; message = it.optString("message", "کد ارسال شد.") } else message = it?.optString("message", "ارسال کد انجام نشد.") ?: "ارسال کد انجام نشد." } },
                { apiCall("verify_otp", JSONObject().put("phone", phone).put("code", code)) { if (it?.optBoolean("ok") == true) { user = it.optJSONObject("user"); loggedIn = true; message = "ورود موفق بود." } else message = it?.optString("message", "کد صحیح نیست.") ?: "کد صحیح نیست." } })
            else Dashboard(user) { loggedIn = false; sent = false; code = ""; message = "از حساب خارج شدید." }
        }
    }
}

@Composable
private fun LoginScreen(phone: String, onPhone: (String) -> Unit, code: String, onCode: (String) -> Unit,
                        sent: Boolean, busy: Boolean, message: String, onTelegram: () -> Unit, onSend: () -> Unit, onVerify: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Text("NEXORA", color = Color.White, fontSize = 30.sp)
        Text("ورود امن", color = Color(0xFF8FA7BF), fontSize = 13.sp)
        Spacer(Modifier.height(28.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF071321)), shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) {
                Text("ورود با شماره موبایل", color = Color.White, fontSize = 19.sp)
                Spacer(Modifier.height(14.dp))
                OutlinedTextField(phone, onPhone, Modifier.fillMaxWidth(), label = { Text("شماره موبایل") }, placeholder = { Text("09123456789") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone), singleLine = true)
                Button(onSend, enabled = !busy && phone.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text(if (busy) "در حال ارسال..." else "ارسال کد تأیید") }
                if (sent) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(code, onCode, Modifier.fillMaxWidth(), label = { Text("کد تأیید") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    Button(onVerify, enabled = !busy && code.isNotBlank(), modifier = Modifier.fillMaxWidth().padding(top = 12.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF078B69))) { Text("تأیید و ورود") }
                }
                OutlinedButton(onTelegram, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) { Text("ورود با تلگرام") }
                Spacer(Modifier.height(10.dp))
                Text(message, color = Color(0xFF9EB5CD), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun Dashboard(user: JSONObject?, logout: () -> Unit) {
    val coin = user?.optString("coin", "0") ?: "0"
    val stake = user?.optString("stake", "0") ?: "0"
    val invite = user?.optString("invite", "0") ?: "0"
    Column(Modifier.fillMaxSize().padding(20.dp)) {
        Text("NEXORA", color = Color.White, fontSize = 28.sp)
        Text("داشبورد حساب کاربری", color = Color(0xFF8FA7BF), fontSize = 13.sp)
        Spacer(Modifier.height(18.dp))
        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF071321)), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(20.dp)) { Text("موجودی اصلی", color = Color(0xFF8FA7BF)); Text("$coin TON", color = Color.White, fontSize = 27.sp); Spacer(Modifier.height(12.dp)); Text("موجودی سپرده: $stake TON", color = Color.White); Text("زیرمجموعه‌ها: $invite نفر", color = Color.White) }
        }
        Spacer(Modifier.height(16.dp)); Text("بخش‌های برنامه", color = Color.White, fontSize = 17.sp)
        listOf("💵 سرمایه‌گذاری", "📥 برداشت", "🎁 پاداش روزانه", "👥 زیرمجموعه‌گیری", "🏦 صرافی", "💳 کیف پول", "☎️ پشتیبانی").forEach { OutlinedButton({}, Modifier.fillMaxWidth().padding(top = 8.dp)) { Text(it) } }
        Button(logout, Modifier.fillMaxWidth().padding(top = 18.dp)) { Text("خروج از حساب") }
    }
}
