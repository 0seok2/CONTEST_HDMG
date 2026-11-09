# 페달 가디언 (Pedal Guardian)

운전자가 밟고 있는 페달을 화면 구석에 조용히 표시하고, 그 입력을 기록하는 차량용 앱.
현대자동차그룹 차량용 앱 개발 공모전(Pleos Playground) 출품작.

## 실행 방법

1. Android Studio(최신)로 이 저장소를 연다.
2. Pleos Connect Emulator(AAOS, API 34)를 **Cold boot**로 켜고 CRN을 주입한다. (자세한 절차: `CLAUDE.md` 3장)
3. `app` 실행 구성으로 설치·실행한다. 명령줄: `./gradlew installDebug`
4. 단위 테스트: `./gradlew testDebugUnitTest`

## 테스트 환경

- Pleos Connect Emulator v2.0 (AAOS, API Level 34), Graphics: Hardware
- 버추얼 콘솔로 차속·브레이크 값을 바꿔 가며 확인

## 구조

| 파일 | 역할 |
|---|---|
| `data/VehicleRepository.kt` | 차속(AAOS `CarPropertyManager`)·브레이크 신호 → `StateFlow`, 이상 구간 CSV 저장 |
| `data/PedalLog.kt` | 순환 버퍼 + 이상 구간 판정 (순수 Kotlin, 단위 테스트 있음) |
| `ui/PedalIndicator.kt` | 구석 페달 표시 (브레이크: 연한 빨강, 가속: 연한 초록) |
| `ui/HistoryScreen.kt` | 정차 중 기록 조회: 최근 30초·보존된 이상 구간을 시간축 그래프로 |
| `ui/SettingsScreen.kt` | 정차 중 표시 위치·크기·투명도 설정 |

앱은 차량을 제어하지 않는다. 읽기 전용이다.

## APK 위치

`apk/팀명_앱이름_release.apk` (제출 전 release 빌드로 채운다)
