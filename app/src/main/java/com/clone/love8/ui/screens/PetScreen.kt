package com.clone.love8.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.clone.love8.data.Actions
import com.clone.love8.data.AppData
import com.clone.love8.data.PetState
import com.clone.love8.ui.components.LoveCard
import com.clone.love8.ui.components.StatBar
import com.clone.love8.ui.theme.LovePink
import com.clone.love8.ui.theme.LovePinkLight
import com.clone.love8.ui.theme.LoveSubText
import com.clone.love8.ui.theme.SkyGradient
import com.clone.love8.ui.theme.SoftGradient
import com.clone.love8.util.Dates
import com.clone.love8.util.PetLogic
import kotlinx.coroutines.delay
import java.time.LocalDate

@Composable
fun PetScreen(data: AppData, showMessage: (String) -> Unit) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(20_000)
            now = System.currentTimeMillis()
        }
    }
    if (!data.pet.adopted) {
        AdoptPet(onAdopt = { type, name ->
            Actions.adoptPet(type, name)
            showMessage("Chào mừng $name về nhà! 🎉")
        })
        return
    }
    val pet = remember(data.pet, now) { PetLogic.tick(data.pet, now) }
    var showEdit by remember { mutableStateOf(false) }
    var hearts by remember { mutableIntStateOf(0) }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SoftGradient),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            PetStage(pet, hearts, onTap = {
                Actions.petAction(PetLogic.Action.PAT)
                hearts++
            }, onEdit = { showEdit = true })
        }
        item {
            LoveCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Cấp ${pet.level}", fontWeight = FontWeight.Bold, color = LovePink)
                    Spacer(Modifier.width(10.dp))
                    LinearProgressIndicator(
                        progress = { pet.xp.toFloat() / PetLogic.xpForLevel(pet.level) },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)),
                        color = LovePink,
                        trackColor = LovePinkLight,
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("${pet.xp}/${PetLogic.xpForLevel(pet.level)} XP", style = MaterialTheme.typography.labelMedium, color = LoveSubText)
                }
                Spacer(Modifier.height(8.dp))
                StatBar("🍖 No bụng", pet.hunger, Color(0xFFFF8A3D))
                StatBar("😊 Vui vẻ", pet.happiness, LovePink)
                StatBar("⚡ Năng lượng", pet.energy, Color(0xFFFFC107))
                StatBar("🫧 Sạch sẽ", pet.cleanliness, Color(0xFF42A5F5))
            }
        }
        item {
            LoveCard {
                Text("Chăm sóc bé", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    PetActionButton("🍖", "Cho ăn") { showMessage(Actions.petAction(PetLogic.Action.FEED)) }
                    PetActionButton("⚽", "Chơi") { showMessage(Actions.petAction(PetLogic.Action.PLAY)) }
                    PetActionButton("🛁", "Tắm") { showMessage(Actions.petAction(PetLogic.Action.BATHE)) }
                    PetActionButton(if (pet.sleeping) "☀️" else "🌙", if (pet.sleeping) "Đánh thức" else "Đi ngủ") {
                        showMessage(Actions.petAction(PetLogic.Action.SLEEP))
                    }
                }
            }
        }
        item { CoinsCard(data, showMessage) }
        item { AccessoryShop(pet, data.coins, showMessage) }
        if (pet.log.isNotEmpty()) {
            item {
                LoveCard {
                    Text("Nhật ký chăm sóc 📝", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    pet.log.take(10).forEach { l ->
                        Row(Modifier.padding(vertical = 4.dp)) {
                            Text(l.text, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                            Text(Dates.relative(l.ts, now), style = MaterialTheme.typography.labelSmall, color = LoveSubText)
                        }
                    }
                }
            }
        }
    }

    if (showEdit) {
        EditPetDialog(pet, onDismiss = { showEdit = false }) { name, type ->
            Actions.renamePet(name, type)
            showEdit = false
        }
    }
}

@Composable
private fun PetStage(pet: PetState, hearts: Int, onTap: () -> Unit, onEdit: () -> Unit) {
    val t = rememberInfiniteTransition(label = "pet")
    val bounce by t.animateFloat(
        initialValue = 0f,
        targetValue = if (pet.sleeping) -4f else -14f,
        animationSpec = infiniteRepeatable(tween(if (pet.sleeping) 1600 else 700), RepeatMode.Reverse),
        label = "bounce",
    )
    var showHeart by remember { mutableStateOf(false) }
    val heartAlpha by animateFloatAsState(if (showHeart) 1f else 0f, tween(250), label = "heartAlpha")
    val heartOffset by animateFloatAsState(if (showHeart) -36f else 10f, tween(600), label = "heartOffset")
    LaunchedEffect(hearts) {
        if (hearts > 0) {
            showHeart = true
            delay(700)
            showHeart = false
        }
    }
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(SkyGradient)
            .padding(20.dp),
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(pet.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Text("✏️", modifier = Modifier.clickable(onClick = onEdit).padding(4.dp))
            }
            Text(PetLogic.mood(pet), color = LoveSubText)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.size(190.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .size(width = 130.dp, height = 18.dp)
                        .clip(CircleShape)
                        .background(Color(0x22000000)),
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .graphicsLayer { translationY = bounce * density }
                        .clickable(onClick = onTap),
                ) {
                    val acc = PetLogic.accessoryEmoji(pet.accessory)
                    if (acc != null) Text(acc, fontSize = 40.sp, modifier = Modifier.offset(y = 22.dp))
                    Text(PetLogic.emoji(pet.type), fontSize = 110.sp)
                }
                if (pet.sleeping) {
                    Text("💤", fontSize = 36.sp, modifier = Modifier.align(Alignment.TopEnd))
                }
                Text(
                    "💕",
                    fontSize = 40.sp,
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .graphicsLayer {
                            alpha = heartAlpha
                            translationY = heartOffset * density
                        },
                )
            }
            Text("Chạm vào bé để vuốt ve 💕", style = MaterialTheme.typography.labelMedium, color = LoveSubText)
        }
    }
}

@Composable
private fun PetActionButton(emoji: String, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier
                .size(62.dp)
                .clip(CircleShape)
                .background(LovePinkLight)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) { Text(emoji, fontSize = 28.sp) }
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CoinsCard(data: AppData, showMessage: (String) -> Unit) {
    val checkedIn = data.settings.lastCheckInDay == LocalDate.now().toString()
    LoveCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🪙", fontSize = 30.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text("${data.coins} xu", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Chăm bé và điểm danh mỗi ngày để nhận xu", style = MaterialTheme.typography.bodySmall, color = LoveSubText)
            }
            Button(
                onClick = { if (Actions.dailyCheckIn()) showMessage("Điểm danh thành công +20 xu 🪙") },
                enabled = !checkedIn,
                colors = ButtonDefaults.buttonColors(containerColor = LovePink),
            ) { Text(if (checkedIn) "Đã nhận" else "Điểm danh") }
        }
    }
}

@Composable
private fun AccessoryShop(pet: PetState, coins: Int, showMessage: (String) -> Unit) {
    LoveCard {
        Text("Cửa hàng phụ kiện 🛍️", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PetLogic.accessories.forEach { a ->
                val owned = a.id in pet.ownedAccessories
                val wearing = pet.accessory == a.id
                Column(
                    Modifier
                        .width(92.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (wearing) LovePinkLight else Color(0xFFFAF4F6))
                        .border(if (wearing) 2.dp else 0.dp, if (wearing) LovePink else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable { showMessage(Actions.accessory(a.id)) }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(a.emoji, fontSize = 30.sp)
                    Text(a.label, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center, maxLines = 1)
                    Text(
                        when {
                            wearing -> "Đang đeo"
                            owned -> "Đeo"
                            else -> "🪙 ${a.price}"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = if (!owned && coins < a.price) Color(0xFFB0A0A6) else LovePink,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun AdoptPet(onAdopt: (String, String) -> Unit) {
    var type by remember { mutableStateOf(PetLogic.types.first().id) }
    var name by remember { mutableStateOf("") }
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(SoftGradient),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        item { Text("🥚", fontSize = 80.sp) }
        item {
            Text("Nhận nuôi thú cưng chung", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
        item {
            Text(
                "Hai bạn cùng chăm sóc một bé thú cưng: cho ăn, chơi đùa, tắm rửa. Bé sẽ nhớ và gửi thông báo nếu bị bỏ rơi lâu!",
                textAlign = TextAlign.Center,
                color = LoveSubText,
            )
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PetLogic.types.forEach { t ->
                    val sel = t.id == type
                    Column(
                        Modifier
                            .width(76.dp)
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (sel) LovePinkLight else Color.White)
                            .border(if (sel) 2.dp else 1.dp, if (sel) LovePink else Color(0xFFF0D5DE), RoundedCornerShape(18.dp))
                            .clickable { type = t.id }
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(t.emoji, fontSize = 34.sp)
                        Text(t.label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it.take(20) },
                label = { Text("Đặt tên cho bé") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            Button(
                onClick = { onAdopt(type, name.trim().ifBlank { "Mochi" }) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LovePink),
            ) { Text("🐾 Nhận nuôi") }
        }
    }
}

@Composable
private fun EditPetDialog(pet: PetState, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf(pet.name) }
    var type by remember { mutableStateOf(pet.type) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Đổi tên / loài") },
        text = {
            Column {
                OutlinedTextField(value = name, onValueChange = { name = it.take(20) }, label = { Text("Tên") }, singleLine = true)
                Spacer(Modifier.height(10.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PetLogic.types.forEach { t ->
                        FilledTonalButton(
                            onClick = { type = t.id },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = if (type == t.id) LovePinkLight else Color(0xFFF5F0F2),
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) { Text(t.emoji, fontSize = 22.sp) }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name.trim(), type) }) { Text("Lưu") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Hủy") } },
    )
}
