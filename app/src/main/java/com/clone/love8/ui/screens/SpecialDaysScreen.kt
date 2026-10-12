package com.clone.love8.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.clone.love8.data.Actions
import com.clone.love8.data.AppData
import com.clone.love8.ui.components.DatePickerField
import com.clone.love8.ui.components.EmojiPicker
import com.clone.love8.ui.components.LoveCard
import com.clone.love8.ui.theme.LoveGradient
import com.clone.love8.ui.theme.LovePink
import com.clone.love8.ui.theme.LoveSubText
import com.clone.love8.ui.theme.SoftGradient
import com.clone.love8.util.Dates
import com.clone.love8.util.Events
import com.clone.love8.util.UpcomingEvent
import java.time.LocalDate

private val dayEmojis = listOf("💝", "🎂", "🎉", "💍", "🌹", "✈️", "🍰", "🎁", "🏖️", "🎄", "🎬", "☕")

@Composable
fun SpecialDaysScreen(data: AppData, showMessage: (String) -> Unit) {
    val today = LocalDate.now()
    val start = Dates.parse(data.startDate) ?: today
    var showAdd by remember { mutableStateOf(false) }
    var toDelete by remember { mutableStateOf<UpcomingEvent?>(null) }
    val events = remember(data.startDate, data.specialDays, data.me.birthday, data.partner.birthday, data.me.name, data.partner.name) {
        Events.all(data, today, milestoneCount = 6)
    }

    Box(Modifier.fillMaxSize().background(SoftGradient)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(LoveGradient)
                        .padding(18.dp),
                ) {
                    Column {
                        Text("Ngày đặc biệt 📅", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Hôm nay là ngày thứ ${Dates.daysTogether(start, today)} hai bạn bên nhau",
                            color = Color.White,
                        )
                        Text("Kỷ niệm, sinh nhật và những ngày đáng nhớ", color = Color.White.copy(alpha = 0.85f), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            items(events, key = { it.key }) { ev ->
                LoveCard(padding = 10.dp) {
                    EventRow(ev, today) {
                        if (ev.custom != null) {
                            IconButton(onClick = { toDelete = ev }) {
                                Icon(Icons.Filled.Delete, contentDescription = "Xóa", tint = LoveSubText)
                            }
                        }
                    }
                }
            }
        }
        FloatingActionButton(
            onClick = { showAdd = true },
            containerColor = LovePink,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp),
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Thêm ngày đặc biệt")
        }
    }

    if (showAdd) {
        AddSpecialDayDialog(onDismiss = { showAdd = false }) { title, date, emoji, yearly, remind ->
            Actions.addSpecialDay(title, date, emoji, yearly, remind)
            showAdd = false
            showMessage("Đã thêm \"$title\" $emoji")
        }
    }

    val del = toDelete?.custom
    if (del != null) {
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("Xóa ngày đặc biệt?") },
            text = { Text("\"${del.title}\" sẽ bị xóa trên cả hai máy.") },
            confirmButton = {
                TextButton(onClick = {
                    Actions.deleteItem(del.id)
                    toDelete = null
                }) { Text("Xóa", color = Color(0xFFE53935)) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("Hủy") } },
        )
    }
}

@Composable
private fun AddSpecialDayDialog(
    onDismiss: () -> Unit,
    onSave: (String, LocalDate, String, Boolean, Boolean) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(null) }
    var emoji by remember { mutableStateOf(dayEmojis.first()) }
    var yearly by remember { mutableStateOf(true) }
    var remind by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Thêm ngày đặc biệt") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it.take(60) },
                    label = { Text("Tên sự kiện") },
                    placeholder = { Text("VD: Lần đầu hẹn hò") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                DatePickerField("Chọn ngày", date, Modifier.fillMaxWidth()) { date = it }
                EmojiPicker(dayEmojis, emoji) { emoji = it }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Lặp lại hằng năm", modifier = Modifier.weight(1f))
                    Switch(checked = yearly, onCheckedChange = { yearly = it })
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Nhắc nhở", modifier = Modifier.weight(1f))
                    Switch(checked = remind, onCheckedChange = { remind = it })
                }
            }
        },
        confirmButton = {
            val d = date
            TextButton(
                onClick = { if (d != null) onSave(title.trim(), d, emoji, yearly, remind) },
                enabled = title.isNotBlank() && d != null,
            ) { Text("Lưu") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}
