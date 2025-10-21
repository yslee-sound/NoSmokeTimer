package com.sweetapps.nosmoketimer.feature.start

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.core.content.edit
import com.sweetapps.nosmoketimer.core.ui.AppElevation
import com.sweetapps.nosmoketimer.core.ui.AppBorder
import com.sweetapps.nosmoketimer.core.ui.BaseActivity
import com.sweetapps.nosmoketimer.core.ui.StandardScreenWithBottomButton
import com.sweetapps.nosmoketimer.core.ui.components.AppUpdateDialog
import com.sweetapps.nosmoketimer.core.util.AppUpdateManager
import com.sweetapps.nosmoketimer.core.util.Constants
import com.sweetapps.nosmoketimer.feature.run.RunActivity
import com.google.android.play.core.install.model.AppUpdateType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.WindowInsetsCompat
import androidx.compose.material3.SnackbarResult
import com.sweetapps.nosmoketimer.R
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.painterResource
import android.os.Build
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.stringResource
import com.sweetapps.nosmoketimer.core.util.UpdateVersionMapper
import android.content.pm.ApplicationInfo
import android.graphics.Color as AndroidColor

class StartActivity : BaseActivity() {
    private lateinit var appUpdateManager: AppUpdateManager

    private fun isDebugBuild(): Boolean {
        return (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Constants.initializeUserSettings(this)
        Constants.ensureInstallMarkerAndResetIfReinstalled(this)

        // 내부 네비게이션(API 30-)에서 스플래시 재등장 방지: skip_splash 처리
        val skipSplash = intent?.getBooleanExtra("skip_splash", false) == true
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && skipSplash) {
            // 스플래시 레이어를 흰 배경으로 즉시 덮고, 첫 프레임 직후 제거
            window.setBackgroundDrawable(ColorDrawable(AndroidColor.WHITE))
        }

        // 첫 프레임부터 상태바 표시 및 어두운 아이콘 적용 (Splash -> 첫 화면 전환 시 깜빡임 방지)
        WindowCompat.setDecorFitsSystemWindows(window, true)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.isAppearanceLightStatusBars = true
        controller.isAppearanceLightNavigationBars = true
        controller.show(WindowInsetsCompat.Type.statusBars())
        controller.show(WindowInsetsCompat.Type.navigationBars())

        // In-App Update 초기화
        appUpdateManager = AppUpdateManager(this)

        val debugBuild = isDebugBuild()
        // 데모 인텐트 플래그 (DEBUG에서만 유효)
        val demoIntentActive = debugBuild && (intent?.getBooleanExtra("demo_update_ui", false) == true)

        setContent {
            // 첫 실행 화면에서는 edge-to-edge 비활성화하여 상태바를 OS가 분리 렌더링
            BaseScreen(applyBottomInsets = false, applySystemBars = false) {
                Box(Modifier.fillMaxSize()) {
                    StartScreenWithUpdate(
                        appUpdateManager = appUpdateManager,
                        demoIntentActive = demoIntentActive,
                        isDebug = debugBuild
                    )
                }
            }
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S && skipSplash) {
            // 첫 프레임이 렌더링된 뒤 스플래시 배경 제거(잔상/깜빡임 방지)
            window.decorView.post { window.setBackgroundDrawable(null) }
        }
    }

    override fun getScreenTitle(): String = "금연 설정"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreenWithUpdate(appUpdateManager: AppUpdateManager, demoIntentActive: Boolean, isDebug: Boolean) {
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // 업데이트 다이얼로그 및 체크 상태
    var showUpdateDialog by remember { mutableStateOf(false) }
    var updateInfo by remember { mutableStateOf<com.google.android.play.core.appupdate.AppUpdateInfo?>(null) }
    var availableVersionName by remember { mutableStateOf("") }
    var isCheckingUpdate by remember { mutableStateOf(false) }
    var showOverlay by remember { mutableStateOf(false) }

    // 데모 모드 트리거 상태(롱프레스)
    var demoLongPressTriggered by remember { mutableStateOf(false) }
    val demoActive = isDebug && (demoIntentActive || demoLongPressTriggered)

    // 앱 시작 시 업데이트 확인 (데모 모드면 실제 확인 대신 시연)
    LaunchedEffect(demoActive) {
        if (demoActive) {
            // 데모: 오버레이 600ms 노출 후 가짜 버전으로 다이얼로그 표시
            isCheckingUpdate = true
            showOverlay = false
            delay(300)
            showOverlay = true
            delay(300)
            val fakeCode = 2025101001
            availableVersionName = UpdateVersionMapper.toVersionName(fakeCode) ?: fakeCode.toString()
            showUpdateDialog = true
            isCheckingUpdate = false
            showOverlay = false
        } else {
            // 실제 업데이트 확인 (24시간 정책 준수)
            isCheckingUpdate = true
            showOverlay = false
            scope.launch {
                // 300ms 후에도 진행 중이면 오버레이 노출
                delay(300)
                if (isCheckingUpdate && !showUpdateDialog) showOverlay = true
            }
            appUpdateManager.checkForUpdate(
                forceCheck = false,
                onUpdateAvailable = { info ->
                    updateInfo = info
                    val code = info.availableVersionCode()
                    availableVersionName = UpdateVersionMapper.toVersionName(code) ?: code.toString()
                    showUpdateDialog = true
                    isCheckingUpdate = false
                    showOverlay = false
                },
                onNoUpdate = {
                    isCheckingUpdate = false
                    showOverlay = false
                }
            )
        }
    }

    // 업데이트 다운로드 완료 리스너: 사용자 액션(다시 시작) 시에만 설치 완료
    LaunchedEffect(Unit) {
        appUpdateManager.registerInstallStateListener {
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = "업데이트가 다운로드되었습니다. 다시 시작하여 설치하세요.",
                    actionLabel = "다시 시작",
                    duration = SnackbarDuration.Indefinite
                )
                if (result == SnackbarResult.ActionPerformed) {
                    appUpdateManager.completeFlexibleUpdate()
                }
            }
        }
    }
    // 화면 소멸 시 리스너 정리
    DisposableEffect(Unit) {
        onDispose { appUpdateManager.unregisterInstallStateListener() }
    }

    val blockAutoNavigation = isCheckingUpdate || showUpdateDialog

    Box(modifier = Modifier.fillMaxSize()) {
        StartScreen(
            blockAutoNavigation = blockAutoNavigation,
            onTitleLongPress = {
                if (isDebug) demoLongPressTriggered = true
            }
        )

        // 업데이트 다이얼로그
        AppUpdateDialog(
            isVisible = showUpdateDialog,
            versionName = availableVersionName,
            updateMessage = "새로운 기능과 개선사항이 포함되어 있습니다.",
            onUpdateClick = {
                if (demoActive) {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            message = "데모: 실제 업데이트는 시작하지 않습니다.",
                            duration = SnackbarDuration.Short
                        )
                    }
                } else {
                    updateInfo?.let { info ->
                        val immediateAllowed = info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE)
                        if (appUpdateManager.isMaxPostponeReached() && immediateAllowed) {
                            appUpdateManager.startImmediateUpdate(info)
                        } else {
                            appUpdateManager.startFlexibleUpdate(info)
                        }
                    }
                }
                showUpdateDialog = false
            },
            onDismiss = {
                appUpdateManager.markUserPostpone()
                showUpdateDialog = false
            },
            canDismiss = !appUpdateManager.isMaxPostponeReached() || demoActive
        )

        // 스낵바
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        // 300ms 지연 오버레이(터치 차단)
        if (showOverlay) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .semantics { testTagsAsResourceId = true }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { /* consume */ },
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    shadowElevation = 4.dp,
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Text(text = stringResource(id = R.string.checking_update))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartScreen(
    blockAutoNavigation: Boolean = false,
    onTitleLongPress: () -> Unit = {}
) {
    val context = LocalContext.current
    val sharedPref = context.getSharedPreferences("user_settings", MODE_PRIVATE)
    val startTime = sharedPref.getLong("start_time", 0L)
    val timerCompleted = sharedPref.getBoolean("timer_completed", false)

    if (!blockAutoNavigation && startTime != 0L && !timerCompleted) {
        LaunchedEffect(Unit) {
            context.startActivity(Intent(context, RunActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
        }
        return
    }

    var textFieldValue by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = "30", selection = TextRange(0, 2)))
    }
    val isValid by remember { derivedStateOf { textFieldValue.text.toFloatOrNull()?.let { it > 0 } ?: false } }
    var isTextSelected by remember { mutableStateOf(true) }
    var isFocused by remember { mutableStateOf(false) }

    LaunchedEffect(isFocused) {
        if (isFocused) {
            delay(50)
            val len = textFieldValue.text.length
            textFieldValue = textFieldValue.copy(selection = TextRange(0, len))
            isTextSelected = true
        }
    }

    StandardScreenWithBottomButton(
        topContent = {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD), // down from CARD_HIGH
                border = BorderStroke(AppBorder.Hairline, colorResource(id = R.color.color_border_light))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "목표 기간 설정",
                        style = MaterialTheme.typography.titleLarge,
                        color = colorResource(id = R.color.color_title_primary),
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(bottom = 24.dp)
                            .combinedClickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {},
                                onLongClick = onTitleLongPress
                            )
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        Card(
                            modifier = Modifier.width(100.dp).height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = colorResource(id = R.color.color_bg_card_light)),
                            elevation = CardDefaults.cardElevation(defaultElevation = AppElevation.CARD)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp), contentAlignment = Alignment.Center) {
                                BasicTextField(
                                    value = textFieldValue,
                                    onValueChange = { newValue ->
                                        val filtered = newValue.text.filter { it.isDigit() || it == '.' }
                                        val dots = filtered.count { it == '.' }
                                        val finalFiltered = if (dots <= 1) filtered else textFieldValue.text
                                        val finalText = when {
                                            finalFiltered.isEmpty() -> "0"
                                            finalFiltered.length > 1 && finalFiltered.startsWith("0") && !finalFiltered.startsWith("0.") -> finalFiltered.substring(1)
                                            else -> finalFiltered
                                        }
                                        val selection = if (isTextSelected) TextRange(finalText.length) else TextRange(finalText.length)
                                        textFieldValue = TextFieldValue(text = finalText, selection = selection)
                                        isTextSelected = false
                                    },
                                    textStyle = MaterialTheme.typography.headlineLarge.copy(
                                        color = colorResource(id = R.color.color_indicator_days),
                                        textAlign = TextAlign.Center
                                    ),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    cursorBrush = SolidColor(colorResource(id = R.color.color_indicator_days)),
                                    modifier = Modifier.fillMaxWidth().onFocusChanged { isFocused = it.isFocused }
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "일",
                            style = MaterialTheme.typography.titleLarge,
                            color = colorResource(id = R.color.color_indicator_label_gray)
                        )
                    }
                    Text(
                        text = "금연할 목표 기간을 입력해주세요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorResource(id = R.color.color_hint_gray),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // 카드 아래 여백은 backgroundDecoration에서 워터마크가 채웁니다.
            Spacer(modifier = Modifier.height(16.dp))
        },
        bottomButton = {
            Box(modifier = Modifier.size(96.dp), contentAlignment = Alignment.Center) {
                ModernStartButton(
                    isEnabled = isValid,
                    onStart = {
                        val targetTime = textFieldValue.text.toFloatOrNull() ?: 0f
                        if (targetTime > 0f) {
                            val formatted = String.format(Locale.US, "%.6f", targetTime).toFloat()
                            sharedPref.edit {
                                putFloat("target_days", formatted)
                                putLong("start_time", System.currentTimeMillis())
                                putBoolean("timer_completed", false)
                            }
                            context.startActivity(Intent(context, RunActivity::class.java))
                        }
                    }
                )
            }
        },
        imePaddingEnabled = false,
        backgroundDecoration = {
            // 워터마크: 화면 짧은 변의 70% 크기, 중앙 고정
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val base = if (maxWidth < maxHeight) maxWidth else maxHeight
                val iconSize = (base * 0.70f).coerceIn(120.dp, 320.dp)
                Image(
                    painter = painterResource(R.drawable.splash_foreground_288),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.Center).size(iconSize).alpha(0.12f)
                )
            }
        }
    )
}

@Composable
fun ModernStartButton(isEnabled: Boolean, onStart: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = { if (isEnabled) onStart() },
        modifier = modifier.size(96.dp),
        shape = CircleShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) colorResource(id = R.color.color_progress_primary) else colorResource(id = R.color.color_button_disabled)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isEnabled) AppElevation.CARD_HIGH else AppElevation.CARD)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(Icons.Default.PlayArrow, contentDescription = "시작", tint = Color.White, modifier = Modifier.size(48.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun StartScreenPreview() { StartScreen() }
