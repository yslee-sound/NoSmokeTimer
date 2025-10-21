package com.sweetapps.nosmoketimer.core.util

/**
 * In-App Update에서 제공하는 availableVersionCode(Int)를 사용자 친화적인 versionName(String)으로 매핑합니다.
 * 릴리스마다 최신 쌍을 추가하세요.
 */
object UpdateVersionMapper {
    private val map: Map<Int, String> = mapOf(
        // 예시 매핑: 실제 릴리스마다 최신 값을 추가하세요.
        2025101001 to "1.0.7"
    )

    fun toVersionName(code: Int): String? = map[code]
}

