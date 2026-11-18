# 얼리오더 (EarlyOrder)

교내 카페의 메뉴를 살펴보고 앱에서 주문 및 결제를 진행하는 Android 주문 앱입니다.

## 프로젝트 소개

- 학교 캡스톤 프로젝트
- 개발 기간: 2024.09 ~ 2024.12
- 팀 구성: 3인
- 담당: Android 앱 전체 개발 (Android Studio, Kotlin)
- 구성: Android 앱, 관리자 웹(HTML/CSS/JavaScript), 서버(FastAPI, MariaDB, kt cloud Ubuntu, Toss Payments)

서버(FastAPI)와 관리자 웹은 팀원이 개발했으며 이 저장소에는 포함하지 않았습니다.
현재 운영 서버는 종료된 상태이므로 실제 로그인, 메뉴 조회, 장바구니 및 결제 기능은 확인할 수 없습니다.

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
└── MyApplication2/
    ├── app/
    │   ├── src/main/java/        # Kotlin 앱 코드
    │   │   └── com/example/
    │   │       └── myapplication/
    │   ├── src/main/res/         # Android 리소스
    │   │   ├── layout/           # Activity 및 Fragment 레이아웃
    │   │   ├── drawable/         # 이미지 및 상태 Drawable
    │   │   ├── menu/             # 내비게이션 메뉴
    │   │   └── values/           # 문자열, 색상, 테마
    │   ├── src/test/             # 단위 테스트
    │   ├── src/androidTest/      # 계측 테스트
    │   └── build.gradle.kts      # 앱 의존성 및 빌드 설정
    ├── gradle/
    │   ├── libs.versions.toml    # 공통 라이브러리 버전 카탈로그
    │   └── wrapper/              # Gradle Wrapper 설정
    ├── build.gradle.kts          # 프로젝트 공통 플러그인 설정
    ├── settings.gradle.kts       # 모듈 및 저장소 설정
    ├── gradle.properties
    └── gradlew(.bat)
```

## 참고

2024년에 진행한 프로젝트를 2026.11에 포트폴리오용으로 정리해 업로드했습니다.