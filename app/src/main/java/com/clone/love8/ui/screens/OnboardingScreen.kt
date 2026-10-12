package com.clone.love8.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clone.love8.data.Actions
import com.clone.love8.data.Profile
import com.clone.love8.ui.components.AvatarEmojis
import com.clone.love8.ui.components.BeatingHeart
import com.clone.love8.ui.components.EmojiPicker
import com.clone.love8.ui.theme.LoveGradient
import com.clone.love8.ui.theme.LovePink
import com.clone.love8.ui.theme.LoveSubText
import com.clone.love8.ui.theme.SoftGradient
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun OnboardingScreen() {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var myName by rememberSaveable { mutableStateOf("") }
    var myEmoji by rememberSaveable { mutableStateOf("😊") }
    var partnerName by rememberSaveable { mutableStateOf("") }
    var partnerEmoji by rememberSaveable { mutableStateOf("🥰") }
    val todayMillis = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val dateState = rememberDatePickerState(initialSelectedDateMillis = todayMillis)

    BackHandler(enabled = step > 0) { step-- }

    Box(Modifier.fillMaxSize().background(SoftGradient).safeDrawingPadding()) {
        AnimatedContent(targetState = step, label = "onboarding") { s ->
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                StepDots(s)
                when (s) {
                    0 -> {
                        Spacer(Modifier.height(30.dp))
                        Box(
                            Modifier.size(150.dp).clip(CircleShape).background(LoveGradient),
                            contentAlignment = Alignment.Center,
                        ) { BeatingHeart(72.sp, "💗") }
                        Text("Love8 Clone", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold, color = LovePink)
                        Text(
                            "Ứng dụng dành cho các cặp đôi\nĐếm ngày yêu · Chia sẻ vị trí · Nuôi thú cưng chung · Ngày đặc biệt",
                            textAlign = TextAlign.Center,
                            color = LoveSubText,
                        )
                        Spacer(Modifier.height(20.dp))
                        PrimaryButton("Bắt đầu 💕") { step = 1 }
                    }
                    1 -> {
                        Text("Bạn là ai?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        BigEmoji(myEmoji)
                        OutlinedTextField(
                            value = myName,
                            onValueChange = { myName = it.take(24) },
                            label = { Text("Tên của bạn") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("Chọn biểu tượng", color = LoveSubText)
                        EmojiPicker(AvatarEmojis, myEmoji) { myEmoji = it }
                        PrimaryButton("Tiếp tục", enabled = myName.isNotBlank()) { step = 2 }
                    }
                    2 -> {
                        Text("Người ấy của bạn", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        BigEmoji(partnerEmoji)
                        OutlinedTextField(
                            value = partnerName,
                            onValueChange = { partnerName = it.take(24) },
                            label = { Text("Tên người ấy") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Text("Chọn biểu tượng", color = LoveSubText)
                        EmojiPicker(AvatarEmojis, partnerEmoji) { partnerEmoji = it }
                        PrimaryButton("Tiếp tục", enabled = partnerName.isNotBlank()) { step = 3 }
                        TextButton(onClick = { step = 1 }) { Text("Quay lại") }
                    }
                    else -> {
                        Text("Hai bạn bắt đầu yêu nhau từ ngày nào?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                        DatePicker(
                            state = dateState,
                            title = null,
                            showModeToggle = true,
                            colors = DatePickerDefaults.colors(containerColor = Color.Transparent),
                            modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.White),
                        )
                        PrimaryButton("Hoàn tất 🎉", enabled = dateState.selectedDateMillis != null) {
                            val millis = dateState.selectedDateMillis ?: todayMillis
                            val start = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            Actions.finishOnboarding(
                                Profile(name = myName.trim(), emoji = myEmoji),
                                Profile(name = partnerName.trim(), emoji = partnerEmoji),
                                start,
                            )
                        }
                        TextButton(onClick = { step = 2 }) { Text("Quay lại") }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepDots(step: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(4) { i ->
            Box(
                Modifier
                    .size(width = if (i == step) 22.dp else 8.dp, height = 8.dp)
                    .clip(CircleShape)
                    .background(if (i <= step) LovePink else Color(0xFFF2C9D6)),
            )
        }
    }
}

@Composable
private fun BigEmoji(emoji: String) {
    Box(
        Modifier.size(110.dp).clip(CircleShape).background(Color.White),
        contentAlignment = Alignment.Center,
    ) { Text(emoji, fontSize = 56.sp) }
}

@Composable
private fun PrimaryButton(text: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(27.dp),
        colors = ButtonDefaults.buttonColors(containerColor = LovePink),
    ) {
        Text(text, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
    }
}
