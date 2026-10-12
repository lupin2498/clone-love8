package com.clone.love8.ui.screens

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clone.love8.data.AppData
import com.clone.love8.ui.components.AvatarView
import com.clone.love8.ui.components.BeatingHeart
import com.clone.love8.ui.components.LoveCard
import com.clone.love8.ui.theme.LovePinkLight
import com.clone.love8.ui.theme.LoveSubText
import com.clone.love8.ui.theme.PremiumGradient
import com.clone.love8.ui.theme.SoftGradient
import com.clone.love8.util.Dates
import java.time.LocalDate

@Composable
fun MoreScreen(data: AppData, open: (SubPageId) -> Unit) {
    val start = Dates.parse(data.startDate) ?: LocalDate.now()
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SoftGradient),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            LoveCard {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    AvatarView(true, data.me.emoji, data.mePhotoStamp, 56.dp)
                    Spacer(Modifier.width(6.dp))
                    BeatingHeart(22.sp)
                    Spacer(Modifier.width(6.dp))
                    AvatarView(false, data.partner.emoji, data.partnerPhotoStamp, 56.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "${data.me.name.ifBlank { "Bạn" }} & ${data.partner.name.ifBlank { "Người ấy" }}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text("${Dates.daysTogether(start)} ngày bên nhau", color = LoveSubText)
                        Text(
                            if (data.partnerConnected) "🟢 Đã kết nối" else if (data.isPaired) "🟡 Đang chờ người ấy" else "⚪ Chưa kết nối",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        }
        item {
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(PremiumGradient)
                    .clickable { open(SubPageId.PREMIUM) }
                    .padding(18.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("👑", fontSize = 30.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Love8 Premium", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Đã mở khóa toàn bộ tính năng", color = Color.White.copy(alpha = 0.9f), style = MaterialTheme.typography.bodySmall)
                    }
                    Text("›", color = Color.White, fontSize = 28.sp)
                }
            }
        }
        item {
            LoveCard(padding = 4.dp) {
                MenuRow("🔗", "Kết nối người ấy", if (data.isPaired) "Mã: đã thiết lập" else "Ghép đôi hai điện thoại") { open(SubPageId.PAIRING) }
                HorizontalDivider(color = Color(0xFFF6E6EB))
                MenuRow("📔", "Nhật ký tình yêu", "${data.diary.size} trang nhật ký") { open(SubPageId.DIARY) }
                HorizontalDivider(color = Color(0xFFF6E6EB))
                MenuRow("⚙️", "Cài đặt", "Hồ sơ, ngày bắt đầu, thông báo, quyền riêng tư") { open(SubPageId.SETTINGS) }
                HorizontalDivider(color = Color(0xFFF6E6EB))
                MenuRow("ℹ️", "Giới thiệu", "Phiên bản & hướng dẫn widget") { open(SubPageId.ABOUT) }
            }
        }
    }
}

@Composable
fun MenuRow(emoji: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(42.dp).clip(CircleShape).background(LovePinkLight), contentAlignment = Alignment.Center) {
            Text(emoji, fontSize = 20.sp)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = LoveSubText)
        }
        Text("›", fontSize = 24.sp, color = LoveSubText)
    }
}
