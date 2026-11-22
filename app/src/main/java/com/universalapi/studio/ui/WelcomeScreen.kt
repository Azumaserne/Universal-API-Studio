package com.universalapi.studio.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.universalapi.studio.i18n.AppLanguage
import com.universalapi.studio.i18n.AppStrings
import com.universalapi.studio.ui.theme.Brand
import com.universalapi.studio.ui.theme.GradientButton
import com.universalapi.studio.ui.theme.glassCard
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeScreen(
    initialLanguage: AppLanguage,
    onLanguagePreview: (AppLanguage) -> Unit,
    onContinue: (AppLanguage) -> Unit,
) {
    var selectedLanguage by remember { mutableStateOf(initialLanguage) }
    var userManuallyPicked by remember { mutableStateOf(false) }

    val allLanguages = remember { AppLanguage.entries }
    var cycleIndex by remember {
        mutableIntStateOf(allLanguages.indexOf(initialLanguage).coerceAtLeast(0))
    }

    // 首次未手动点击前，像 Apple 初次激活界面一样循环跳动展示多语言“欢迎”
    LaunchedEffect(userManuallyPicked) {
        if (!userManuallyPicked) {
            while (true) {
                delay(2200)
                cycleIndex = (cycleIndex + 1) % allLanguages.size
            }
        }
    }

    val displayedGreetingLang = if (userManuallyPicked) selectedLanguage else allLanguages[cycleIndex]
    val activeStrings = remember(selectedLanguage) { AppStrings.forLanguage(selectedLanguage) }

    val infiniteTransition = rememberInfiniteTransition(label = "welcome_glow")
    val glowScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_scale",
    )
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.50f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glow_alpha",
    )

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(Brand.PageBackground),
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                // 上半区：跳动的多语言「欢迎」主视觉（仅使用系统内置无版权风险字体族）
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 56.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 18.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(190.dp)
                                .scale(glowScale)
                                .alpha(glowAlpha)
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Brand.Accent.copy(alpha = 0.65f),
                                            Brand.Cyan.copy(alpha = 0.25f),
                                            Color.Transparent,
                                        ),
                                    ),
                                    CircleShape,
                                ),
                        )

                        AnimatedContent(
                            targetState = displayedGreetingLang,
                            transitionSpec = {
                                (fadeIn(tween(420)) +
                                    slideInVertically(
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow,
                                        ),
                                        initialOffsetY = { it / 2 },
                                    ) +
                                    scaleIn(initialScale = 0.90f, animationSpec = tween(420)))
                                    .togetherWith(
                                        fadeOut(tween(260)) +
                                            slideOutVertically(
                                                animationSpec = tween(260),
                                                targetOffsetY = { -it / 3 },
                                            ),
                                    )
                            },
                            label = "hello_bounce",
                        ) { lang ->
                            Text(
                                text = lang.welcomeWord,
                                style = TextStyle(
                                    fontFamily = FontFamily.SansSerif,
                                    fontSize = 42.sp,
                                    lineHeight = 50.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    brush = Brush.linearGradient(
                                        colors = listOf(
                                            Color.White,
                                            Brand.AccentSoft,
                                            Brand.Cyan,
                                        ),
                                    ),
                                ),
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Universal API Studio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Brand.TextPrimary,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = activeStrings.welcomeSubtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = Brand.TextSecondary,
                        textAlign = TextAlign.Center,
                    )
                }

                // 中下半区居中：语言选择卡片 + 继续按钮
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(22)
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = activeStrings.chooseLanguagePrompt,
                            style = MaterialTheme.typography.labelMedium,
                            color = Brand.TextSecondary,
                            textAlign = TextAlign.Center,
                        )

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            allLanguages.forEach { lang ->
                                val isSelected = lang == selectedLanguage
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) Brand.Accent.copy(alpha = 0.22f)
                                            else Brand.Input.copy(alpha = 0.72f),
                                        )
                                        .border(
                                            width = if (isSelected) 1.5.dp else 1.dp,
                                            color = if (isSelected) Brand.Accent else Brand.Border,
                                            shape = RoundedCornerShape(12.dp),
                                        )
                                        .clickable {
                                            selectedLanguage = lang
                                            userManuallyPicked = true
                                            onLanguagePreview(lang)
                                        }
                                        .padding(horizontal = 14.dp, vertical = 9.dp),
                                ) {
                                    Text(
                                        text = lang.nativeName,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Brand.TextSecondary,
                                        maxLines = 1,
                                    )
                                }
                            }
                        }

                        GradientButton(
                            onClick = { onContinue(selectedLanguage) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = activeStrings.continueBtn,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                    }

                    Text(
                        text = "Designed & Authored by @Azumaserne",
                        style = MaterialTheme.typography.labelSmall,
                        color = Brand.TextTertiary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
            }
        }
    }
}
