# 풋노트 (FootNote, 구 페달 가디언) — 프로젝트 인계 문서

> 현대자동차그룹 차량용 앱 개발 공모전(Pleos Playground) 출품작.
> Claude Code가 이 파일을 읽고 바로 개발을 이어갈 수 있도록 정리한 인계 문서다.
> 답변과 코드 주석은 **한국어**로 작성한다.

---

## 1. 앱 컨셉 (확정)

**운전자가 밟고 있는 페달을 화면 구석에 실시간으로 표시하고, 그 입력을 조용히 기록하는 앱.**

- 화면 구석에 **브레이크·가속 페달 아이콘 2개**를 작게 상시 표시한다.
- 밟고 있는 페달 아이콘에만 **연한 색**을 입힌다. (브레이크: 연한 빨강, 가속: 연한 초록)
- 페달 상태와 차속을 **순환 버퍼에 기록**하고, 이상 구간(예: 브레이크를 밟았는데 차속 증가)은 덮어쓰지 않게 따로 보존한다.
- 정차 중에만 기록 조회(시간축 그래프)와 설정(위치·크기·투명도)을 할 수 있다.

### 하지 않기로 결정한 것 (다시 넣지 말 것)
- **알림·경고음·팝업·음성 안내 없음.** 사용자가 명시적으로 뺐다. 표시는 수동적이어야 한다.
- 주행 점수, 운전 코칭: 블루링크 '안전운전 점수'와 겹친다.
- 펫 모드, 아이 잠 모드: 현대차 순정 기능(펫 모드, 후석 취침 모드)과 겹친다.
- 측방 카메라 녹화: 공개 SDK에 카메라 API가 없다. 향후 확장 또는 별도 작품으로 낸다.

### 안전 원칙
- 앱은 페달 반응을 **절대 제어하지 않는다. 읽기 전용이다.**
- 주행 중에는 터치할 수 있는 요소를 두지 않는다.

---

## 2. 공모전 핵심 정보

| 항목 | 내용 |
|---|---|
| 접수 마감 | **2026-11-15(일)**, 이메일 제출 |
| 접수 메일 | appdevchallenge@hyundai-partners.com |
| 메일 제목 | `[차량용 앱 개발 공모전] 팀명(개인은 성명)_대표자명` |
| 필수 제출 | 접수양식(기획서, DOCX) + 개인정보 동의서(서명) |
| 선택 제출(가산점) | 시연 영상(화면 녹화 인정), GitHub 저장소(소스+APK) |
| 1차 결과 | 11/23 |
| 2차 PT | 12/3~4 오프라인, **실제 동작하는 앱 필수**, 시연은 **에뮬레이터로만** 함(실차 시연 없음) |

### 1차 심사 기준 (배점 비공개)
차량특화성 / 창의성 / 기술 활용성 / 사업성 (타깃, 예상 MAU, Retention, 확대 계획)

### 설명회(10/28)에서 확인된 사항
- SDK를 많이 쓰는 게 목적이 아니다. **핵심 가치 하나를 실제로 동작하게 보여 주는 게 중요하다.**
- 개발 단계에서는 권한 승인이 필요 없다. **매니페스트에 퍼미션만 선언하면 된다.** (승인 심사는 App Market 배포 때)
- 아직 공개되지 않은 SDK도 공모전에 제공할 예정이라고 했다. → 가속 페달 신호 문의 필요.
- 외부 API와 인터넷 연결을 쓸 수 있다.
- 생성형 AI를 써도 되지만, **PT 기술 Q&A에서 구조와 로직을 직접 설명해야 한다.** 코드는 단순하게 짜고, 주석은 충분히 단다.
- Pleos Connect **버추얼 콘솔**로 차량 상태를 가상으로 바꿔 가며 테스트할 수 있다.

### GitHub / APK 제출 규칙
- 소스와 APK를 같은 저장소에 둔다. APK 위치는 `apk/팀명_앱이름_release.apk` (**release 빌드**)
- 용량을 넘으면 GitHub Releases에 올리고 그 주소를 제출한다.
- 비공개 저장소라면 심사용 계정을 Collaborator로 초대한다.
- README.md 필수 항목: 앱 이름과 한 줄 소개, 실행 방법, 테스트 환경과 필요한 설정, APK 위치

---

## 3. 개발 환경

- Android Studio(최신), **Kotlin**, minSdk **28 이상**, Gradle **8.0 이상**
- **Pleos Connect Emulator** (AAOS 기반, API Level 34 시스템 이미지)
  - SDK Manager → SDK Update Sites → 아래 URL 추가
    - Windows/Linux(x86_64): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-sys-img.xml`
    - Mac(arm64): `https://nexus-playground.pleos.ai/repository/raw-releases/connect-avd/2.x/crp-arm64-sys-img.xml`
    - (옛 connect.v2.0.5 주소는 404. 11/10 공식 문서로 확인)
  - SDK Platforms → Show Package Details → API 34 아래 Pleos Connect System Image 체크 → 설치 후 Android Studio 재시작
  - Device Manager → Create device → Automotive → **Pleos Connect v2.1.0** 선택, RAM 4GB
  - Graphics는 **Hardware**, Boot는 **Cold boot** (Quick boot로 하면 VHAL을 쓸 수 없다)
- **CRN 주입 필수**: Pleos Playground에서 My Project를 만들고, Project Info > Test Info의 CRN을 복사해서 넣는다.
  ```bash
  adb root on
  adb shell su 0 "echo 'propId: 554696961 areaId: 0 values: {CRN}' > /data/vendor/vsomeip/vhal_fifo"
  adb reboot
  ```
- 문서
  - 개발 가이드: https://document.pleos.ai/en/docs/connect/guide/getting-started/application-development/setup-application-development-environment
  - Vehicle SDK API: https://document.pleos.ai/docs/pleos-only/connect-sdk-pleos/Vehicle/intro (JS 렌더링 페이지: 일반 fetch로는 빈 페이지, 브라우저/Playwright로 읽어야 함)
  - 튜토리얼(Vehicle SDK, Hilt 예제): https://pleos.ai/playground/resources/docs/pleos-tutorial/vehicle-sdk

> SDK 클래스명과 메서드명은 위 API 레퍼런스에서 **반드시 확인하고** 쓴다. 추측해서 쓰지 않는다.
> 차종에 따라 지원 범위가 다르므로 `checkXXXCapabilities` 계열 API로 지원 여부를 먼저 확인한다.

---

## 4. 사용할 API

| 기능 | API | 비고 |
|---|---|---|
| 브레이크 페달 상태 | Vehicle SDK – **Brake API** (연동 완료) | `ai.pleos.playground:Vehicle:2.3.2`, 저장소 `https://nexus-playground.pleos.ai/repository/maven-releases/`, 퍼미션 `pleos.car.permission.CAR_DRIVE`. `Vehicle(context).initialize()` → `getBrake()` → `checkBrakePedalStatusCapabilities` / `getCurrentBrakePedalStatus` / `registerBrakePedalStatus(BrakePedalStatusListener)` |
| 차속·기어 | AAOS Car API (`CarPropertyManager`, 예: `PERF_VEHICLE_SPEED`) | 표준 데이터는 AAOS API를 쓰는 게 Pleos 정책 |
| 가속 페달 | **미확인** | Vehicle SDK v2.3.2 리스너 목록에도 없다(11/10 확인). 기술지원 답변 전까지 UI는 브레이크 중심, 가속 아이콘은 자리만 유지 |

---

## 5. 구현 계획 (최소 구성)

```
app/
 ├─ data/VehicleRepository.kt    # Brake API + 차속 콜백 → StateFlow
 ├─ data/PedalLog.kt             # 순환 버퍼(최근 N초) + 이상 구간 파일 보존
 ├─ ui/PedalIndicator.kt         # 구석 표시(아이콘 2개, 연한 색)
 ├─ ui/HistoryScreen.kt          # 정차 중 기록 조회(시간축 그래프)
 └─ ui/SettingsScreen.kt         # 정차 중 위치·크기·투명도
```

- 화면 표시 방식: 처음에는 **앱 화면 안에 표시**하는 것으로 시작한다. 다른 앱 위에 띄우는 오버레이가 AAOS 에뮬레이터에서 되는지는 따로 확인한 뒤 적용한다.
- 이상 구간 판정(초안): 브레이크 ON인데 일정 시간 안에 차속이 증가하면 그 구간 앞뒤를 보존한다. 기준값은 상수 하나로 두고 조정할 수 있게 한다.
- 데이터 저장은 앱 내부 저장소의 파일(CSV/JSON)로 충분하다. DB나 서버는 쓰지 않는다.
- 핵심 로직(이상 구간 판정, 순환 버퍼)에는 실행 가능한 테스트를 하나씩 둔다.

---

## 6. 일정 (오늘 11/10 기준)

| 날짜 | 할 일 |
|---|---|
| 11/10(화) | Playground 가입, My Project 생성, 에뮬레이터·CRN 세팅, Brake API 첫 호출 성공 |
| 11/11(수) | 실시간 페달 표시 UI 완성 |
| 11/12(목) | 기록(순환 버퍼, 이상 구간 보존)과 조회 화면 |
| 11/13(금) | 버추얼 콘솔로 시연 영상 녹화, 접수양식 작성 |
| 11/14(토) | release APK 빌드, GitHub 정리(README, `apk/`), 최종 점검 |
| 11/15(일) | 이메일 제출 |

---

## 7. 확인 필요 사항 (기술지원 문의)

- [ ] 가속 페달 상태 데이터를 제공하는지 (공개 전 SDK 포함)
- [ ] 버추얼 콘솔에서 브레이크 페달·차속 값을 바꿀 수 있는지 (시연 영상 구성에 필요)
- [ ] 다른 앱 위에 상시 표시(오버레이)하는 방식이 에뮬레이터에서 동작하는지, App Market 배포 심사에서 허용되는지
- [ ] 브레이크·차속 데이터가 들어오는 주기 (실시간 표시 반응성에 영향)

---

## 8. 참고

- 기획서 원본: Claude 대화에서 만든 문서 「차량용 앱 공모전 기획서 – 페달 가디언」
- 제출 가이드: https://appdevchallenge.auto-hmg.io/guide/submission
- 심사·시상: https://appdevchallenge.auto-hmg.io/guide/judging-awards
- 설명회 다시보기: https://youtu.be/na6tSJ44pWc
