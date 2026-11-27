package com.bolaodosvei.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bolaodosvei.app.model.League
import com.bolaodosvei.app.model.UserAccount
import com.bolaodosvei.app.ui.components.BolaoLogoBadge
import com.bolaodosvei.app.ui.theme.*
import kotlin.math.sqrt

@Composable
fun LoginScreen(
    onLogin: (String, String) -> Result<UserAccount>,
    onRegister: (String, String, String, Long, League) -> Result<UserAccount>
) {
    var isRegisterMode by remember { mutableStateOf(true) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var selectedColor by remember { mutableLongStateOf(0xFF00C853L) }
    var selectedLeague by remember { mutableStateOf(League.BRASILEIRAO) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val badgeColors = listOf(
        0xFF00C853L,
        0xFFFFD600L,
        0xFF3B82F6L,
        0xFFEF4444L,
        0xFFA855F7L,
        0xFFF97316L
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF09090B))
    ) {
        // Templens radial dot-matrix aura
        Canvas(modifier = Modifier.fillMaxSize()) {
            val step = 28f
            val centerX = size.width * 0.5f
            val centerY = size.height * 0.16f
            val maxRadius = size.width * 0.85f
            var y = step * 0.5f
            while (y < size.height * 0.72f) {
                var x = step * 0.5f
                while (x < size.width) {
                    val dx = x - centerX
                    val dy = y - centerY
                    val dist = sqrt(dx * dx + dy * dy)
                    val falloff = (1f - (dist / maxRadius)).coerceIn(0f, 1f)
                    if (falloff > 0.02f) {
                        drawCircle(
                            color = Color.White.copy(alpha = 0.09f * falloff * falloff),
                            radius = 1.6f,
                            center = Offset(x, y)
                        )
                    }
                    x += step
                }
                y += step
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BolaoLogoBadge(size = 96.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "BOLÃO DOS VÉI",
                style = TextStyle(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.White, Color(0xFFA1A1AA))
                    ),
                    fontFamily = BolaoDisplayFontFamily,
                    fontSize = 30.sp,
                    letterSpacing = (-0.3).sp
                )
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Brasileirão • Premier League • La Liga • Champions",
                color = Color(0xFFA1A1AA),
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Double-nested Bento Auth Shell
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF111114),
                border = BorderStroke(1.dp, Color(0xFF222226)),
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // OriginKit Segmented Switch inside inset stage
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF09090B),
                        border = BorderStroke(1.dp, Color(0xFF1F1F23)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isRegisterMode) Color.White else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isRegisterMode = true; errorMessage = null }
                            ) {
                                Text(
                                    text = "Criar Conta",
                                    color = if (isRegisterMode) Color(0xFF09090B) else Color(0xFFA1A1AA),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (!isRegisterMode) Color.White else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { isRegisterMode = false; errorMessage = null }
                            ) {
                                Text(
                                    text = "Entrar",
                                    color = if (!isRegisterMode) Color(0xFF09090B) else Color(0xFFA1A1AA),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(vertical = 10.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Seu nome ou apelido") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Cor do seu escudo de perfil",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            badgeColors.forEach { hex ->
                                val chosen = hex == selectedColor
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(hex))
                                        .border(
                                            width = if (chosen) 2.5.dp else 1.dp,
                                            color = if (chosen) Color.White else Color(0xFF27272A),
                                            shape = CircleShape
                                        )
                                        .clickable { selectedColor = hex }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Liga favorita",
                            color = Color(0xFFA1A1AA),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(League.entries) { league ->
                                FilterChip(
                                    selected = selectedLeague == league,
                                    onClick = { selectedLeague = league },
                                    label = { Text(league.shortName, fontSize = 11.sp, maxLines = 1) }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("E-mail") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Senha") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = errorMessage!!,
                            color = LiveRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Button(
                        onClick = {
                            val res = if (isRegisterMode) {
                                onRegister(name, email, password, selectedColor, selectedLeague)
                            } else {
                                onLogin(email, password)
                            }
                            errorMessage = res.exceptionOrNull()?.message
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF09090B)
                        ),
                        shape = CircleShape,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = if (isRegisterMode) "Criar Conta e Entrar" else "Entrar no Bolão",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Crie sua conta real para salvar seus palpites e sincronizar com seus amigos na nuvem.",
                color = Color(0xFF71717A),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
