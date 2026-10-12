package com.clone.love8.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clone.love8.BuildConfig
import com.clone.love8.data.Actions
import com.clone.love8.data.AppData
import com.clone.love8.data.DiaryEntry
import com.clone.love8.data.Profile
import com.clone.love8.data.Repository
import com.clone.love8.sync.Crypto
import com.clone.love8.sync.NtfyClient
import com.clone.love8.sync.SyncManager
import com.clone.love8.ui.components.AvatarEmojis
import com.clone.love8.ui.components.AvatarView
import com.clone.love8.ui.components.DatePickerField
import com.clone.love8.ui.components.EmojiPicker
import com.clone.love8.ui.components.LoveCard
import com.clone.love8.ui.components.MoodEmojis
import com.clone.love8.ui.components.SubPage
import com.clone.love8.ui.theme.LoveGradient
import com.clone.love8.ui.theme.LovePink
import com.clone.love8.ui.theme.LoveSubText
import com.clone.love8.ui.theme.PremiumGradient
import com.clone.love8.util.Avatars
import com.clone.love8.util.Dates
import com.clone.love8.widget.WidgetUpdater
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val diaryFmt = DateTimeFormatter.ofPattern("HH:mm · dd/MM/yyyy")
private val DangerRed = Color(0xFFE53935)

// ---------------------------------------------------------------- Nhật ký

@Composable
fun DiaryScreen(data: AppData, onBack: () -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<DiaryEntry?>(null) }
    SubPage(
        title = "Nhật ký tình yêu 📔",
        onBack = onBack,
        floatingActionButton = {
            FloatingActionButton(onClick = { showAdd = true }, containerColor = LovePink, contentColor = Color.White) {
                Icon(Icons.Filled.Edit, contentDescription = "Viết nhật ký")
            }
        },
    ) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (data.diary.isEmpty()) {
                item {
                    Column(Modifier.fillMaxWidth().padding(top = 60.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📖", fontSize = 64.sp)
                        Text("Chưa có trang nhật ký nào", fontWeight = FontWeight.Bold)
                        Text("Ghi lại những khoảnh khắc đáng nhớ của hai bạn", color = LoveSubText, textAlign = TextAlign.Center)
                    }
                }
            }
            items(data.diary, key = { it.id }) { e ->
                val mine = e.author == data.deviceId
                LoveCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.mood, fontSize = 30.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (mine) data.me.name.ifBlank { "Bạn" } else data.partner.name.ifBlank { "Người ấy" },
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                Instant.ofEpochMilli(e.ts).atZone(ZoneId.systemDefault()).format(diaryFmt),
                                style = MaterialTheme.typography.labelSmall,
                                color = LoveSubText,
                            )
                        }
                        if (mine) {
                            IconButton(onClick = { toDelete = e }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Xóa", tint = LoveSubText)
                            }
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(e.text, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }

    if (showAdd) {
        var mood by remember { mutableStateOf(MoodEmojis.first()) }
        var text by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("Viết nhật ký") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EmojiPicker(MoodEmojis, mood) { mood = it }
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(1000) },
                        placeholder = { Text("Hôm nay của hai bạn thế nào?") },
                        minLines = 4,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        Actions.addDiary(mood, text)
                        showAdd = false
                    },
                    enabled = text.isNotBlank(),
                ) { Text("Lưu") }
            },
            dismissButton = { TextButton(onClick = { showAdd = false }) { Text("Hủy") } },
        )
    }

    val del = toDelete
    if (del != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Xóa trang nhật ký?") },
            text = { Text("Trang nhật ký sẽ bị xóa trên cả hai máy.") },
            confirmButton = {
                TextButton(onClick = {
                    Actions.deleteItem(del.id)
                    toDelete = null
                }) { Text("Xóa", color = DangerRed) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Hủy") } },
        )
    }
}

// ---------------------------------------------------------------- Kết nối

@Composable
fun PairingScreen(data: AppData, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    var input by remember { mutableStateOf("") }
    var server by remember(data.pairing.server) { mutableStateOf(data.pairing.server) }
    var confirmUnpair by remember { mutableStateOf(false) }
    val code = data.pairing.code

    SubPage("Kết nối người ấy", onBack, snackbar) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(LoveGradient)
                        .padding(20.dp),
                ) {
                    Column {
                        Text("🔗 Ghép đôi hai điện thoại", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Sau khi kết nối, hai bạn sẽ thấy pin, vị trí, tâm trạng của nhau, gửi lời nhắn, cùng nuôi thú cưng và chia sẻ ngày đặc biệt.",
                            color = Color.White,
                        )
                    }
                }
            }
            if (code == null) {
                item {
                    LoveCard {
                        Text("Cách 1 · Tạo mã mới", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Tạo mã rồi gửi cho người ấy nhập vào app của họ.", color = LoveSubText, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = {
                                Actions.createPairing(ctx)
                                toast("Đã tạo mã. Hãy gửi cho người ấy nhé!")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = LovePink),
                        ) { Text("✨ Tạo mã kết nối") }
                    }
                }
                item {
                    LoveCard {
                        Text("Cách 2 · Nhập mã của người ấy", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it.uppercase().take(9) },
                            label = { Text("Mã 8 ký tự (VD: ABCD-2345)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(10.dp))
                        FilledTonalButton(
                            onClick = {
                                if (Actions.joinPairing(ctx, input)) toast("Đang kết nối...") else toast("Mã không hợp lệ")
                            },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("💞 Kết nối") }
                    }
                }
            } else {
                item {
                    LoveCard {
                        Text("Mã kết nối của hai bạn", color = LoveSubText)
                        Text(
                            Crypto.pretty(code),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = LovePink,
                            letterSpacing = 3.sp,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (data.partnerConnected) "🟢 Đã kết nối với ${data.partner.name.ifBlank { "người ấy" }}"
                            else "🟡 Đang chờ người ấy nhập mã...",
                            fontWeight = FontWeight.SemiBold,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = {
                                clipboard.setText(AnnotatedString(Crypto.pretty(code)))
                                toast("Đã sao chép mã")
                            }, modifier = Modifier.weight(1f)) { Text("📋 Sao chép") }
                            Button(
                                onClick = {
                                    val send = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(
                                            Intent.EXTRA_TEXT,
                                            "💗 Kết nối với mình trên Love8 Clone nhé! Mã kết nối: ${Crypto.pretty(code)}",
                                        )
                                    }
                                    ctx.startActivity(Intent.createChooser(send, "Chia sẻ mã kết nối"))
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = LovePink),
                            ) { Text("📤 Chia sẻ") }
                        }
                        TextButton(onClick = {
                            val app = ctx.applicationContext
                            scope.launch {
                                SyncManager.sendStatus(app, force = true)
                                val got = SyncManager.pull(app)
                                snackbar.showSnackbar(if (got.isEmpty()) "Đã đồng bộ" else "Đã nhận ${got.size} cập nhật mới")
                            }
                        }) { Text("🔄 Đồng bộ ngay") }
                    }
                }
                item {
                    LoveCard {
                        Text("Ngắt kết nối", fontWeight = FontWeight.Bold)
                        Text("Dữ liệu trên máy vẫn được giữ lại.", color = LoveSubText, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { confirmUnpair = true }) { Text("Ngắt kết nối", color = DangerRed) }
                    }
                }
            }
            item {
                LoveCard {
                    Text("Máy chủ đồng bộ", fontWeight = FontWeight.Bold)
                    Text(
                        "Mặc định dùng ntfy.sh miễn phí. Bạn có thể tự host ntfy và nhập địa chỉ tại đây (cả hai máy phải dùng cùng máy chủ).",
                        color = LoveSubText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(value = server, onValueChange = { server = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Row {
                        TextButton(onClick = {
                            val s = server
                            scope.launch {
                                val ok = withContext(Dispatchers.IO) { NtfyClient.ping(s) }
                                snackbar.showSnackbar(if (ok) "Máy chủ hoạt động ✅" else "Không kết nối được máy chủ ❌")
                            }
                        }) { Text("Kiểm tra") }
                        TextButton(onClick = {
                            Actions.setServer(server)
                            toast("Đã lưu máy chủ")
                        }) { Text("Lưu") }
                    }
                }
            }
            item {
                LoveCard {
                    Text("🔒 Quyền riêng tư", fontWeight = FontWeight.Bold)
                    Text(
                        "Mọi dữ liệu gửi đi đều được mã hóa đầu-cuối (AES-256-GCM) bằng khóa sinh ra từ mã kết nối. " +
                            "Máy chủ chỉ chuyển tiếp dữ liệu đã mã hóa và tự xóa sau tối đa 12 giờ. Chỉ chia sẻ mã cho người ấy.",
                        color = LoveSubText,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }

    if (confirmUnpair) {
        AlertDialog(
            onDismissRequest = { confirmUnpair = false },
            title = { Text("Ngắt kết nối?") },
            text = { Text("Hai máy sẽ không còn đồng bộ với nhau. Bạn có thể kết nối lại bằng mã mới.") },
            confirmButton = {
                TextButton(onClick = {
                    Actions.unpair()
                    confirmUnpair = false
                }) { Text("Ngắt kết nối", color = DangerRed) }
            },
            dismissButton = { TextButton(onClick = { confirmUnpair = false }) { Text("Hủy") } },
        )
    }
}

// ---------------------------------------------------------------- Cài đặt

@Composable
fun SettingsScreen(data: AppData, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val toast: (String) -> Unit = { msg -> scope.launch { snackbar.showSnackbar(msg) } }
    var meName by remember { mutableStateOf(data.me.name) }
    var partnerName by remember { mutableStateOf(data.partner.name) }
    var pickForMe by remember { mutableStateOf(true) }
    var confirmReset by remember { mutableStateOf(false) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri: Uri? ->
        if (uri != null) {
            if (Avatars.savePhoto(ctx, uri, pickForMe)) {
                Actions.photoChanged(pickForMe)
                toast("Đã cập nhật ảnh đại diện")
            } else {
                toast("Không đọc được ảnh")
            }
        }
    }
    val locationPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        toast(if (res.values.any { it }) "Đã cấp quyền vị trí" else "Chưa được cấp quyền vị trí")
    }
    val singlePerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        toast(if (granted) "Đã cấp quyền" else "Chưa được cấp quyền")
    }

    SubPage("Cài đặt", onBack, snackbar) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                ProfileEditor(
                    title = "Hồ sơ của bạn",
                    isMe = true,
                    profile = data.me,
                    stamp = data.mePhotoStamp,
                    name = meName,
                    onName = { meName = it },
                    onSaveName = {
                        Actions.updateMe(data.me.copy(name = meName.trim()))
                        toast("Đã lưu tên")
                    },
                    onPickPhoto = {
                        pickForMe = true
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemovePhoto = {
                        Avatars.deletePhoto(ctx, true)
                        Actions.photoChanged(true)
                    },
                    onEmoji = { Actions.updateMe(data.me.copy(emoji = it)) },
                    onBirthday = { Actions.updateMe(data.me.copy(birthday = it.toString())) },
                )
            }
            item {
                ProfileEditor(
                    title = "Người ấy",
                    isMe = false,
                    profile = data.partner,
                    stamp = data.partnerPhotoStamp,
                    name = partnerName,
                    onName = { partnerName = it },
                    onSaveName = {
                        Actions.updatePartner(data.partner.copy(name = partnerName.trim()))
                        toast("Đã lưu tên")
                    },
                    onPickPhoto = {
                        pickForMe = false
                        picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemovePhoto = {
                        Avatars.deletePhoto(ctx, false)
                        Actions.photoChanged(false)
                    },
                    onEmoji = { Actions.updatePartner(data.partner.copy(emoji = it)) },
                    onBirthday = { Actions.updatePartner(data.partner.copy(birthday = it.toString())) },
                )
            }
            item {
                LoveCard {
                    Text("Ngày bắt đầu yêu 💞", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    DatePickerField("Chọn ngày", Dates.parse(data.startDate), Modifier.fillMaxWidth()) {
                        Actions.setStartDate(it)
                        toast("Đã cập nhật ngày bắt đầu")
                    }
                }
            }
            item {
                LoveCard {
                    Text("Chia sẻ & thông báo", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    SettingSwitch("📍 Chia sẻ vị trí", data.settings.shareLocation) { v -> Actions.updateSettings { it.copy(shareLocation = v) } }
                    SettingSwitch("🔋 Chia sẻ mức pin", data.settings.shareBattery) { v -> Actions.updateSettings { it.copy(shareBattery = v) } }
                    SettingSwitch("💌 Thông báo lời nhắn", data.settings.notifyMessages) { v -> Actions.updateSettings { it.copy(notifyMessages = v) } }
                    SettingSwitch("🐾 Thông báo thú cưng", data.settings.notifyPet) { v -> Actions.updateSettings { it.copy(notifyPet = v) } }
                    SettingSwitch("📅 Nhắc ngày đặc biệt", data.settings.notifySpecialDays) { v -> Actions.updateSettings { it.copy(notifySpecialDays = v) } }
                }
            }
            item {
                LoveCard {
                    Text("Quyền ứng dụng", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            OutlinedButton(onClick = { singlePerm.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) {
                                Text("🔔 Cho phép thông báo")
                            }
                        }
                        OutlinedButton(onClick = {
                            locationPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }, modifier = Modifier.fillMaxWidth()) { Text("📍 Cho phép vị trí") }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            OutlinedButton(onClick = { singlePerm.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION) }, modifier = Modifier.fillMaxWidth()) {
                                Text("🛰️ Vị trí nền (luôn cho phép)")
                            }
                        }
                        OutlinedButton(onClick = {
                            val intent = Intent(
                                android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", ctx.packageName, null),
                            )
                            ctx.startActivity(intent)
                        }, modifier = Modifier.fillMaxWidth()) { Text("⚙️ Mở cài đặt ứng dụng") }
                    }
                }
            }
            item {
                LoveCard {
                    Text("Dữ liệu", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(onClick = { confirmReset = true }, modifier = Modifier.fillMaxWidth()) {
                        Text("🗑️ Xóa toàn bộ dữ liệu", color = DangerRed)
                    }
                }
            }
        }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text("Xóa toàn bộ dữ liệu?") },
            text = { Text("Mọi dữ liệu trên máy này (hồ sơ, nhật ký, thú cưng, ngày đặc biệt) sẽ bị xóa và không thể khôi phục.") },
            confirmButton = {
                TextButton(onClick = {
                    Avatars.deletePhoto(ctx, true)
                    Avatars.deletePhoto(ctx, false)
                    Repository.reset()
                    WidgetUpdater.updateAll(ctx)
                    confirmReset = false
                }) { Text("Xóa", color = DangerRed) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Hủy") } },
        )
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ProfileEditor(
    title: String,
    isMe: Boolean,
    profile: Profile,
    stamp: Long,
    name: String,
    onName: (String) -> Unit,
    onSaveName: () -> Unit,
    onPickPhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onEmoji: (String) -> Unit,
    onBirthday: (LocalDate) -> Unit,
) {
    LoveCard {
        Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarView(isMe, profile.emoji, stamp, 72.dp, Modifier.clickable(onClick = onPickPhoto))
            Spacer(Modifier.width(12.dp))
            Column {
                TextButton(onClick = onPickPhoto) { Text("📷 Chọn ảnh từ thư viện") }
                TextButton(onClick = onRemovePhoto) { Text("😊 Dùng biểu tượng") }
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = { onName(it.take(24)) },
            label = { Text("Tên") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = { TextButton(onClick = onSaveName) { Text("Lưu") } },
        )
        Spacer(Modifier.height(10.dp))
        Text("Biểu tượng", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        EmojiPicker(AvatarEmojis, profile.emoji, onEmoji)
        Spacer(Modifier.height(10.dp))
        Text("Sinh nhật", style = MaterialTheme.typography.labelLarge)
        Spacer(Modifier.height(6.dp))
        DatePickerField("Chọn ngày sinh", Dates.parse(profile.birthday), Modifier.fillMaxWidth(), onBirthday)
    }
}

// ---------------------------------------------------------------- Premium & giới thiệu

private val premiumFeatures = listOf(
    "📍" to "Chia sẻ vị trí thời gian thực, tốc độ & thời gian dừng",
    "🔋" to "Xem mức pin và trạng thái sạc của người ấy",
    "🐾" to "Nuôi thú cưng chung, mọi loài & phụ kiện",
    "📅" to "Ngày đặc biệt không giới hạn, nhắc nhở tự động",
    "🧩" to "Widget màn hình chính: ngày yêu, thú cưng, người ấy",
    "📔" to "Nhật ký tình yêu đồng bộ hai máy",
    "💌" to "Lời nhắn & \"Nhớ bạn\" không giới hạn",
    "🔒" to "Mã hóa đầu-cuối mọi dữ liệu",
)

@Composable
fun PremiumScreen(onBack: () -> Unit) {
    SubPage("Love8 Premium", onBack) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(PremiumGradient)
                        .padding(24.dp),
                ) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("👑", fontSize = 56.sp)
                        Text("Premium đã mở khóa", color = Color.White, fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                        Text("Bản clone: mọi tính năng đều miễn phí", color = Color.White.copy(alpha = 0.9f))
                    }
                }
            }
            items(premiumFeatures) { (emoji, text) ->
                LoveCard(padding = 14.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 24.sp)
                        Spacer(Modifier.width(12.dp))
                        Text(text, modifier = Modifier.weight(1f))
                        Text("✅")
                    }
                }
            }
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    SubPage("Giới thiệu", onBack) { inner ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(inner),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("💗", fontSize = 64.sp)
                    Text("Love8 Clone", fontWeight = FontWeight.ExtraBold, style = MaterialTheme.typography.headlineSmall)
                    Text("Phiên bản ${BuildConfig.VERSION_NAME}", color = LoveSubText)
                }
            }
            item {
                LoveCard {
                    Text("Thêm widget ra màn hình chính", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Nhấn giữ vào màn hình chính → Tiện ích (Widgets) → tìm \"Love8 Clone\" → chọn widget Ngày yêu nhau, Thú cưng hoặc Người ấy.",
                        color = LoveSubText,
                    )
                }
            }
            item {
                LoveCard {
                    Text("Tính năng", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "• Đếm ngày yêu nhau & mốc kỷ niệm\n• Chia sẻ vị trí, pin, tâm trạng\n• Gửi \"Nhớ bạn\" và lời nhắn\n" +
                            "• Thú cưng chung: cho ăn, chơi, tắm, ngủ, phụ kiện\n• Ngày đặc biệt & nhắc nhở\n• Nhật ký tình yêu\n• Widget màn hình chính",
                        color = LoveSubText,
                    )
                }
            }
            item {
                Text(
                    "Ứng dụng mô phỏng lại tính năng của \"Love8 - App for Couples\" cho mục đích học tập, không liên kết với nhà phát triển gốc.",
                    style = MaterialTheme.typography.bodySmall,
                    color = LoveSubText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                )
            }
        }
    }
}
