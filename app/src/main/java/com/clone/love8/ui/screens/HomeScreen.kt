package com.clone.love8.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clone.love8.data.Actions
import com.clone.love8.data.AppData
import com.clone.love8.data.LoveMessage
import com.clone.love8.ui.components.AvatarView
import com.clone.love8.ui.components.BeatingHeart
import com.clone.love8.ui.components.EmojiPicker
import com.clone.love8.ui.components.LoveCard
import com.clone.love8.ui.components.MoodEmojis
import com.clone.love8.ui.theme.LoveGradient
import com.clone.love8.ui.theme.LovePink
import com.clone.love8.ui.theme.LovePinkLight
import com.clone.love8.ui.theme.LoveSubText
import com.clone.love8.ui.theme.SoftGradient
import com.clone.love8.util.BatteryHelper
import com.clone.love8.util.Dates
import com.clone.love8.util.Events
import com.clone.love8.util.LocationHelper
import com.clone.love8.util.UpcomingEvent
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val quickMessages = listOf(
    "Nhớ em/anh nhiều 💕",
    "Ăn cơm chưa? 🍚",
    "Đang làm gì đó? 👀",
    "Về đến nhà chưa? 🏠",
    "Ngủ ngon nhé 🌙",
    "Yêu nhiều 😘",
)

@Composable
fun HomeScreen(data: AppData, showMessage: (String) -> Unit, open: (SubPageId) -> Unit) {
    val ctx = LocalContext.current
    val today = LocalDate.now()
    val start = Dates.parse(data.startDate) ?: today
    var battery by remember { mutableStateOf(BatteryHelper.read(ctx)) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            battery = BatteryHelper.read(ctx)
            now = System.currentTimeMillis()
        }
    }
    val partnerName = data.partner.name.ifBlank { "Người ấy" }
    val upcoming = remember(data.startDate, data.specialDays, data.me.birthday, data.partner.birthday) {
        Events.all(data, today).filter { !it.date.isBefore(today) }.take(3)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SoftGradient),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { CounterCard(data, start, today) }
        if (!data.isPaired) {
            item { ConnectBanner { open(SubPageId.PAIRING) } }
        }
        item { StatusCard(data, battery, now) }
        item {
            MissYouCard(data) { kind, text ->
                Actions.sendMessage(kind, text)
                showMessage(
                    if (data.isPaired) "Đã gửi tới $partnerName 💗"
                    else "Đã lưu. Hãy kết nối với người ấy để gửi tin nhắn!"
                )
            }
        }
        item {
            LoveCard {
                Text("Tâm trạng của bạn hôm nay", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                EmojiPicker(MoodEmojis, data.myStatus.mood) {
                    Actions.setMood(ctx, it)
                    showMessage("Đã chia sẻ tâm trạng $it")
                }
            }
        }
        if (upcoming.isNotEmpty()) {
            item { UpcomingCard(upcoming, today) }
        }
        item { MessagesCard(data) }
    }
}

@Composable
private fun CounterCard(data: AppData, start: LocalDate, today: LocalDate) {
    val days = Dates.daysTogether(start, today)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(LoveGradient)
            .padding(20.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
                    AvatarView(true, data.me.emoji, data.mePhotoStamp, 78.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(data.me.name.ifBlank { "Bạn" }, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Spacer(Modifier.width(8.dp))
                BeatingHeart(34.sp)
                Spacer(Modifier.width(8.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
                    AvatarView(false, data.partner.emoji, data.partnerPhotoStamp, 78.dp)
                    Spacer(Modifier.height(6.dp))
                    Text(data.partner.name.ifBlank { "Người ấy" }, color = Color.White, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.height(14.dp))
            Text("Đã yêu nhau", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.titleSmall)
            Row(verticalAlignment = Alignment.Bottom) {
                Text(days.coerceAtLeast(0).toString(), fontSize = 54.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(" ngày", fontSize = 20.sp, color = Color.White, modifier = Modifier.padding(bottom = 12.dp))
            }
            Text(Dates.breakdown(start, today), color = Color.White, fontWeight = FontWeight.Medium)
            Text("Bắt đầu từ ${Dates.format(start)}", color = Color.White.copy(alpha = 0.8f), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun ConnectBanner(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFFFF0C9))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🔗", fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Kết nối với người ấy", fontWeight = FontWeight.Bold)
                Text("Chia sẻ vị trí, pin, tâm trạng và nuôi thú cưng cùng nhau", style = MaterialTheme.typography.bodySmall, color = LoveSubText)
            }
            Text("›", fontSize = 28.sp, color = LoveSubText)
        }
    }
}

@Composable
private fun StatusCard(data: AppData, battery: BatteryHelper.Battery, now: Long) {
    val ps = data.partnerStatus
    val dist = LocationHelper.distanceBetween(data.myStatus, ps)
    LoveCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            PersonStatus(data.me.name.ifBlank { "Bạn" }, battery.level.takeIf { it >= 0 }, battery.charging, data.myStatus.mood)
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                Text("📍", fontSize = 22.sp)
                Text(
                    if (dist != null) LocationHelper.formatDistance(dist) else "-- km",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    when {
                        !data.isPaired -> "chưa kết nối"
                        ps == null -> "đang chờ người ấy..."
                        else -> "cập nhật " + Dates.relative(ps.ts, now)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = LoveSubText,
                    textAlign = TextAlign.Center,
                )
            }
            PersonStatus(data.partner.name.ifBlank { "Người ấy" }, ps?.battery, ps?.charging == true, ps?.mood)
        }
    }
}

@Composable
private fun PersonStatus(name: String, battery: Int?, charging: Boolean, mood: String?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(92.dp)) {
        Text(name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.height(4.dp))
        val low = battery != null && battery <= 20
        Text(
            "🔋 ${battery?.toString() ?: "--"}%" + if (charging) " ⚡" else "",
            color = if (low) Color(0xFFE53935) else LoveSubText,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(2.dp))
        Text(mood ?: "😶", fontSize = 24.sp)
    }
}

@Composable
private fun MissYouCard(data: AppData, send: (String, String) -> Unit) {
    var text by remember { mutableStateOf("") }
    val startOfDay = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val missToday = data.messages.count { it.sender == data.deviceId && it.kind == "miss_you" && it.ts >= startOfDay }
    LoveCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Nhớ người ấy?", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Hôm nay bạn đã nhớ $missToday lần 💕", style = MaterialTheme.typography.bodySmall, color = LoveSubText)
            }
            Button(
                onClick = { send("miss_you", "Nhớ bạn!") },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = LovePink),
            ) { Text("💗 Nhớ bạn") }
        }
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            quickMessages.forEach { q ->
                AssistChip(onClick = { send("text", q) }, label = { Text(q) })
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.take(300) },
                placeholder = { Text("Nhắn gì đó cho người ấy...") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                maxLines = 3,
            )
            IconButton(onClick = {
                if (text.isNotBlank()) {
                    send("text", text.trim())
                    text = ""
                }
            }) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Gửi", tint = LovePink)
            }
        }
    }
}

@Composable
private fun UpcomingCard(events: List<UpcomingEvent>, today: LocalDate) {
    LoveCard {
        Text("Sắp tới 🎉", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        events.forEach { ev -> EventRow(ev, today) }
    }
}

@Composable
fun EventRow(ev: UpcomingEvent, today: LocalDate, trailing: @Composable () -> Unit = {}) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(LovePinkLight), contentAlignment = Alignment.Center) {
            Text(ev.emoji, fontSize = 22.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(ev.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(Dates.format(ev.date), style = MaterialTheme.typography.bodySmall, color = LoveSubText)
        }
        val diff = Dates.daysUntil(ev.date, today)
        Box(
            Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(if (diff == 0L) LovePink else LovePinkLight)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                Dates.ddayLabel(ev.date, today),
                color = if (diff == 0L) Color.White else LovePink,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.labelLarge,
            )
        }
        trailing()
    }
}

private val timeFmt = DateTimeFormatter.ofPattern("HH:mm dd/MM")

@Composable
private fun MessagesCard(data: AppData) {
    val recent = data.messages.takeLast(15).reversed()
    LoveCard {
        Text("Lời nhắn gần đây 💌", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        if (recent.isEmpty()) {
            Text("Chưa có lời nhắn nào. Hãy gửi một lời yêu thương nhé!", color = LoveSubText, style = MaterialTheme.typography.bodySmall)
        }
        recent.forEach { m -> MessageBubble(m, mine = m.sender == data.deviceId) }
    }
}

@Composable
private fun MessageBubble(m: LoveMessage, mine: Boolean) {
    val text = when (m.kind) {
        "miss_you" -> "💗 Nhớ bạn!"
        "heart" -> "💖"
        else -> m.text
    }
    val time = Instant.ofEpochMilli(m.ts).atZone(ZoneId.systemDefault()).format(timeFmt)
    Row(
        Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start,
    ) {
        Column(horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
            Box(
                Modifier
                    .widthIn(max = 260.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(if (mine) LovePink else Color(0xFFF3ECEF))
                    .padding(horizontal = 14.dp, vertical = 8.dp),
            ) {
                Text(text, color = if (mine) Color.White else Color(0xFF3D1A26))
            }
            Text(time, style = MaterialTheme.typography.labelSmall, color = LoveSubText)
        }
    }
}
