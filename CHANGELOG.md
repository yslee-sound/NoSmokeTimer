# Changelog

모든 눈에 띄는 변경 사항은 이 파일에 기록됩니다.
형식: [Keep a Changelog](https://keepachangelog.com/ko/1.1.0/) & Semantic Versioning.

## [Unreleased]
### Added
- (예정) Crashlytics / Analytics 도입
- (예정) 날짜/통계 계산 단위 테스트 확대
- 문서: `docs/MODAL_BARRIER_GUIDE.md` 추가(드로어/다이얼로그 "완전한 모달 배리어" 가이드 및 QA 체크리스트)

### Changed
- (예정) 접근성 개선 (터치 타겟/콘트라스트)
- 다이얼로그 정책: `AppUpdateDialog`에 `DialogProperties(dismissOnBackPress, dismissOnClickOutside)`를 `canDismiss`와 동기화하여 명시화

### Fixed
- 상세 화면: 기록 삭제가 되지 않던 버그(JSON 키 불일치 start_time/end_time vs startTime/endTime) 수정
- 드로어 배경 상호작용 누수: `BaseActivity`에 입력 가드 오버레이(pointerInput로 모든 포인터 이벤트 소비 + clearAndSetSemantics) 적용, 닫힘 직후 200ms 그레이스 포함
- (예정) 통계 경계 케이스 보정

## [1.0.1] - 2025-10-21
### Changed
- 하단 Safe Area/여백 정책 정리: Records/AllRecords/Level/Settings 화면의 하단 여백을 navBarBottom 사용에서 상단과 동일한 16dp로 통일.
- 수평 마진 일관화: 여러 화면의 좌우 마진을 `LayoutConstants.SCREEN_HORIZONTAL_PADDING`(16dp)로 통일.
- 문서 업데이트: `docs/INSETS_AND_IME_GUIDE.md` v1.2.1로 갱신(정책 변경 사항, QA 체크리스트 보강).

### Notes
- 예외: 하단 고정 버튼 레이아웃(`StandardScreenWithBottomButton`) 및 CTA가 중요한 세부 화면(`DetailActivity`)은 IME/네비 인셋을 고려한 가변 하단 여백 유지.
- 검증: `:app:compileDebugKotlin` 성공(타입/문법 오류 없음).

## [1.0.0] - 2025-10-05
### Added
- 금연(금연 시작~현재) 기록 생성/조회 기본 플로우
- 주/월/년/전체 통계 화면(성공률, 목표 진행률 등)
- 목표 진행률/성공률 계산 로직(기초)
- 기본 SplashScreen 적용
- Jetpack Compose UI 구조(Material3)

### Changed
- Release 빌드: R8 minify / resource shrink 활성화
- Gradle: configuration cache, build cache, parallel build 활성화

### Security
- 서명정보 환경변수 기반 로딩(로컬 미설정 시 unsigned 빌드 허용)

## 형식 가이드
버전 태그 예: v1.0.0
- Added: 새 기능
- Changed: 기존 기능 변경(비 호환 여부 PR에서 명시)
- Deprecated: 곧 제거될 항목
- Removed: 제거된 항목
- Fixed: 버그 수정
- Security: 취약점 관련 또는 보안 영향 변경

[Unreleased]: https://example.com/compare/v1.0.1...HEAD
[1.0.1]: https://example.com/releases/v1.0.1
[1.0.0]: https://example.com/releases/v1.0.0
