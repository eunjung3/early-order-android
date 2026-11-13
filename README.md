# 얼리오더 (EarlyOrder)

카페 메뉴를 살펴보고 앱에서 주문 및 결제 절차를 진행하는 Android 사전 주문 앱입니다.

## 프로젝트 소개

- 학교 캡스톤 프로젝트
- 개발 기간: 2024.09 ~ 2024.12
- 팀 구성: 3인
- 담당: Android 앱 전체 개발 (Android Studio, Kotlin)
- 구성: Android 앱, 관리자 웹(HTML/CSS/JavaScript), 서버(FastAPI, MariaDB, kt cloud Ubuntu, Toss Payments)

서버(FastAPI)와 관리자 웹은 팀원이 개발했으며 이 저장소에는 포함하지 않았다. 실행하려면 별도 서버가 필요하다.

## 사용 기술

- Kotlin, Android SDK, Gradle Kotlin DSL
- AndroidX (Core, AppCompat, Activity, Fragment, ConstraintLayout, Lifecycle), Material Components
- Retrofit 2, Gson Converter, OkHttp Logging Interceptor
- Glide, ViewPager2, Kotlin Coroutines
- 테스트 의존성: JUnit 4, AndroidX Test, Espresso

## 주요 기능

- 회원가입 및 로그인
- 메뉴 목록 조회와 메뉴 옵션 선택
- 장바구니 관리
- 즐겨찾기 메뉴 조회 및 관리
- 주문 생성 및 결제 준비/승인 처리 (Toss Payments 결제 화면은 WebView로 표시)
- 주문 내역 조회
- 사용자 이름, 이메일, 비밀번호 변경

## 프로젝트 구조

```text
.
├── README.md
├── .gitignore
├── docs/
│   └── screenshots/              # 앱 화면 캡처
└── MyApplication2/
    ├── app/
    │   ├── src/main/java/        # Kotlin 앱 코드
    │   ├── src/main/res/         # Android 리소스
    │   ├── src/test/             # 단위 테스트
    │   ├── src/androidTest/      # 계측 테스트
    │   └── build.gradle.kts       # 앱 의존성 및 빌드 설정
    ├── gradle/                   # Gradle Wrapper 및 버전 카탈로그
    ├── build.gradle.kts
    ├── settings.gradle.kts
    └── gradlew(.bat)
```

## 실행 준비

1. Android Studio에서 `MyApplication2` 폴더를 엽니다.
2. Android SDK 설정이 들어 있는 `MyApplication2/local.properties`에 아래 항목을 추가합니다. 값은 사용 가능한 서버 주소로 바꾸고 URL 끝에 `/`를 포함해야 합니다.

   ```properties
   APP_URL=https://your-api.example.com/
   ```

3. Gradle 동기화 후 앱을 실행합니다. 앱의 API 요청과 결제 준비에는 별도 서버가 필요합니다. HTTP 주소를 설정하면 앱의 cleartext 통신이 허용되므로, 가능하면 HTTPS 주소를 사용하세요.

## 스크린샷

화면 캡처를 `docs/screenshots/`에 추가한 뒤 아래에 정리할 예정입니다.

<!-- 예: ![메뉴 화면](docs/screenshots/menu.png) -->

## 참고

2024년에 진행한 프로젝트를 2026.11에 포트폴리오용으로 정리해 업로드했다.

## 추가 작성 예정

- 시연 영상 및 화면 설명: 추후 작성
- 테스트 및 배포 정보: 추후 작성
