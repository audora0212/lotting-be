# Lotting Backend 애플리케이션

### 해당 프로젝트는 '도심역 민간임대주택 창립준비위원회' 전산화 과정의 '외주 프로젝트' 입니다.
Spring Boot 기반의 Lotting Backend 애플리케이션은 고객 등록, 입금내역 관리, 단계별 납입 처리, 수수료 및 환불 관리 등 다양한 행정 업무를 지원하는 백엔드 솔루션입니다.

- 최영찬 - 백엔드 개발
- 이승준 - 프론트엔드 개발

## [사용 설명서 (PDF)](./사용설명서.pdf)
## [전산화 작업 설명 및 요약 (PDF)](./전산화%20작업%20설명%20및%20요약.pdf)



## 목차

- [개요](#개요)
- [주요 기능](#주요-기능)
- [아키텍처](#아키텍처)
- [사용 기술](#사용-기술)
- [빌드 파일 (build.gradle)](#빌드-파일-buildgradle)
- [설치 및 실행](#설치-및-실행)
- [설정](#설정)
- [API 문서](#api-문서)
- [프로젝트 구조](#프로젝트-구조)

---

## 개요

**Lotting Backend 애플리케이션**은 Spring Boot와 관련 기술들을 활용하여 고객 데이터 관리, 다중 단계 입금 분배, 환불 처리 등을 체계적으로 처리합니다. RESTful API를 제공하여 프론트엔드와 연동하며, JWT 기반 인증, 파일 업로드/다운로드, SSE(Server-Sent Events)를 통한 진행률 알림 등 다양한 기능을 갖추고 있습니다.

---

## 주요 기능

- **사용자 인증:** JWT를 이용한 안전한 로그인 및 회원가입 기능
- **고객 관리:** 고객 정보의 생성, 조회, 수정, 삭제(CRUD) 및 단계별 입금 배분
- **입금/납입 처리:** 일반 입금 및 대출/자납 입금의 자동 분배 및 재계산 처리
- **환불 관리:** Excel 기반 환불 내역 업로드/다운로드 및 진행 상황 SSE 제공
- **파일 처리:** 문서 및 Excel 템플릿 파일의 업로드/다운로드 기능
- **REST API:** 인증, 고객, 입금내역, 수수료, 공지사항, 단계, 환불 관련 API 제공
- **보안:** Spring Security 및 CORS 설정을 통한 안전한 API 보호

---

## 아키텍처

애플리케이션은 아래와 같이 계층별로 명확히 구분되어 있습니다.

- **Controller 계층:** 클라이언트와의 REST API 통신 담당  
  (예: `AuthController`, `CustomerController`, `DepositHistoryController`, `RefundController` 등)
- **Service 계층:** 핵심 비즈니스 로직 처리 담당  
  (예: 고객 데이터 처리, 입금/납입 분배, 환불 처리, Excel 파일 작업 등)
- **Repository 계층:** Spring Data JPA를 통한 데이터 영속화 및 쿼리 실행
- **도메인 모델:** 고객, 입금내역, 단계(Phase), 수수료, 공지사항, 관리자, 환불 등 비즈니스 엔티티 관리
- **보안:** JWT 기반 인증/인가 및 Spring Security 설정
- **유틸리티:** FileCache 등 헬퍼 클래스 제공

---

## 사용 기술

- **Java 17 이상**
- **Spring Boot**
- **Spring Data JPA**
- **Spring Security**
- **JWT (JSON Web Token)**
- **Apache POI** – Excel 파일 처리
- **Lombok** – 코드 간소화 (getter/setter, equals/hashCode 등)
- **MySQL** – 데이터베이스 (Connector/J 사용)
- **Guava** – 추가 유틸리티 지원
- **Maven/Gradle** – 빌드 및 의존성 관리 (아래 Gradle 스크립트 참조)

---

## 빌드 파일 (build.gradle)

다음은 프로젝트에서 사용하는 Gradle 빌드 스크립트 예시입니다.

```gradle
plugins {
	id 'java'
	id 'org.springframework.boot' version '3.3.5'
	id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.audora'
version = '0.0.1-SNAPSHOT'

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation 'org.springframework.boot:spring-boot-starter-web'
	implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
	runtimeOnly 'com.mysql:mysql-connector-j'
	compileOnly 'org.projectlombok:lombok'
	annotationProcessor 'org.projectlombok:lombok'
	implementation 'org.springframework.boot:spring-boot-starter-security'
	testImplementation 'org.springframework.boot:spring-boot-starter-test'

	implementation 'com.google.guava:guava:31.1-jre'
	implementation 'io.jsonwebtoken:jjwt-api:0.11.5'
	runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.11.5'
	runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.11.5'

	testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
	developmentOnly 'org.springframework.boot:spring-boot-devtools'

	implementation 'org.apache.poi:poi:5.2.2'
	implementation 'org.apache.poi:poi-ooxml:5.2.2'
}

tasks.named('test') {
	useJUnitPlatform()
}
```
## 설치 및 실행 저장소 클론:
```
git clone https://github.com/your-org/lotting_backend.git
cd lotting_backend
```
## 프로젝트 빌드:

JDK 17 이상과 Gradle이 설치되어 있어야 합니다. 다음 명령어로 빌드를 진행합니다.
```
./gradlew clean build
```
## 애플리케이션 실행:

Gradle로 실행하거나 빌드된 jar 파일을 실행합니다.

```
./gradlew bootRun
```
또는

```
java -jar build/libs/lotting_backend-0.0.1-SNAPSHOT.jar
```
## 설정

애플리케이션 설정은 application.properties 또는 application.yml 파일로 관리됩니다. 주요 설정 항목은 다음과 같습니다.

- CORS 설정: allowed.origins 값 설정 (SecurityConfig에서 주입)

- JWT 설정: jwt.secret (최소 32자 이상) 및 jwt.expirationMs 설정

- 데이터베이스: MySQL 연결 정보

- 파일 업로드 경로: file.upload-dir 설정

- 참고: JWT secret은 보안을 위해 최소 32자 이상이어야 합니다.

## API 문서
API는 아래와 같은 URI로 제공됩니다.

- 인증: /api/auth

  - 로그인: POST /api/auth/signin

  - 회원가입: POST /api/auth/signup

- 고객 관리: /customers

    - 고객 생성, 조회, 수정, 삭제 및 단계 관련 엔드포인트 제공

- 입금내역: /deposit 및 /depositlist

    - 입금내역 관련 CRUD 작업

- Excel 파일 처리:

    - 입금 Excel 업로드/다운로드 진행: /api/deposithistory/excel

  - 고객 등록 Excel 생성: /files/format1/{id}, /files/format2/{id}

- 수수료: /fees

- 공지사항: /api/notices

- 환불: /api/refunds


## 프로젝트 구조
```├── src
│   ├── main
│   │   ├── java
│   │   │   └── com
│   │   │       └── audora
│   │   │           └── lotting_be
│   │   │               ├── config            // 보안 및 애플리케이션 설정
│   │   │               ├── controller        // REST API 컨트롤러
│   │   │               ├── model             // 도메인 모델 및 임베디드 엔티티
│   │   │               ├── payload           // 요청/응답 DTO
│   │   │               ├── repository        // JPA 리포지토리
│   │   │               ├── security          // JWT 및 사용자 상세 정보 서비스
│   │   │               ├── service           // 핵심 비즈니스 로직 서비스
│   │   │               └── util              // 유틸리티 클래스 (FileCache 등)
│   │   └── resources
│   │       ├── application.properties        // 애플리케이션 설정
│   │       └── excel_templates               // Excel 템플릿 파일
└── README.md
```
