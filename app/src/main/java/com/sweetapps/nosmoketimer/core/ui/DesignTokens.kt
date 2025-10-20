package com.sweetapps.nosmoketimer.core.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * 앱 전역 디자인 토큰 (라이트 모드 고정 전제)
 * - Elevation: 단순화 (0 / 2)
 *   ZERO: 완전 평면
 *   CARD (0dp): 일반 카드 / 그룹 / 보조 영역 — 플랫 스타일
 *   CARD_HIGH (2dp): 주요 액션 / 주목도 높은 카드 (원형 시작/중지 버튼 포함)
 */
object AppAlphas {
    const val SurfaceTint: Float = 0.1f
}

object AppElevation {
    val ZERO = 0.dp
    val CARD = 0.dp
    val CARD_HIGH = 2.dp
}

// 플랫 디자인용 테두리 두께
object AppBorder {
    val Hairline = 0.5.dp
    val Subtle = 1.dp
}

/**
 * 선택된 항목 하이라이트용 소프트 그레이 배경.
 * 배경이 흰색(#FFFFFF)일 때도 충분히 식별되도록 명도 대비를 높인 톤입니다.
 */
object AppColors {
    // 기존: Color(0xFFFBFBFC) -> 거의 흰색이라 시인성이 낮았음
    // 변경: 약한 블루-그레이 톤으로 대비 강화
    val SurfaceOverlaySoft = Color(0xFFE9EEF5)
}
