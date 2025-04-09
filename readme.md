Lotting Back-End Application
이 프로젝트는 Spring Boot 기반의 백엔드 애플리케이션으로, 고객 관리, 입금 내역 및 할부(차수) 관리, 파일 업로드/다운로드(엑셀 처리), 환불 처리, 공지사항 관리 등 다양한 기능을 제공합니다. JWT 인증, SSE(서버 전송 이벤트)를 활용한 실시간 진행상황 전달, Apache POI를 이용한 엑셀 파일 처리 등 최신 기술을 적용하여 모듈화된 구조로 구현되었습니다.

주요 기능
인증 및 사용자 관리

관리자 회원가입 및 로그인 (JWT 기반 인증)

AuthController 및 UserDetailsServiceImpl 등 관련 컴포넌트 제공

고객 및 입금 내역 관리

고객 생성, 조회, 수정, 삭제

고객의 각 차수(Phase)에 따른 입금액 배분 및 재계산 로직 구현

입금 내역(DepositHistory) CRUD 처리

엑셀 파일 업로드 및 다운로드

입금 내역, 환불, 일반 신청서 등 템플릿 기반 엑셀 파일 자동 채움 및 다운로드(SSE를 통한 진행상황 전달)

DepositExcelController, FileController, ExcelService 등 사용

수수료 및 환불 관리

Fee, FeePerPhase 관리

해약 고객 환불 내역 관리 및 엑셀 변환

공지사항 및 기타 관리

공지사항 생성/수정/삭제 및 검색 기능

연체료 계산 및 통계 제공

기술 스택
Language: Java 11 이상

Framework: Spring Boot, Spring Data JPA, Spring Security

Database: JPA/Hibernate (RDBMS 연동 – MySQL, PostgreSQL 등)

빌드 도구: Maven (또는 Gradle)

기타 라이브러리:

Lombok (간편한 VO/DTO, Model 생성)

Apache POI (엑셀 파일 처리)

JSON Web Token (JWT)

Spring Web (REST API, SSE 등)

Jakarta EE (javax → jakarta 패키지 사용)

프로젝트 구조
plaintext
복사
.
├── LottingBeApplication.java           # Spring Boot 메인 애플리케이션
├── config
│   └── SecurityConfig.java             # 스프링 시큐리티 설정 (CORS, JWT 필터, AuthenticationManager 등)
├── controller                          # REST API 컨트롤러 계층
│   ├── AuthController.java             # 인증 관련 엔드포인트 (로그인, 회원가입)
│   ├── CustomerController.java         # 고객 관리 API
│   ├── DepositExcelController.java     # 입금 내역 엑셀 업로드/다운로드 API (SSE 기반 진행상황 전송)
│   ├── DepositHistoryController.java   # 입금 내역 CRUD API
│   ├── DepositListController.java      # 입금 내역 리스트 조회 API
│   ├── FeeController.java              # 수수료 관리 API
│   ├── FileController.java             # 파일 업로드/다운로드 API (엑셀 신청서 등)
│   ├── LateFeesController.java         # 연체료 정보 조회 API
│   ├── NoticeController.java           # 공지사항 관리 API
│   ├── PhaseController.java            # 각 차수(Phase) 관리 API
│   └── RefundController.java           # 환불 엑셀 업로드/다운로드 및 환불 기록 API
├── model                               # 도메인 모델 및 엔티티 계층
│   ├── customer                       # 고객 관련 엔티티 (Customer, DepositHistory, Phase, Status 등) 및 부가 모델(minor) 포함
│   ├── Fee                            # 수수료 관련 엔티티 (Fee, FeePerPhase)
│   ├── manager                        # 관리자 엔티티 (Manager)
│   ├── notice                         # 공지사항 엔티티 (Notice)
│   └── refund                         # 해약/환불 엔티티 (CancelledCustomerRefund)
├── payload                             # 요청(Request) 및 응답(Response) DTO 계층
│   ├── request                        # 로그인, 회원가입, 차수 수정 관련 DTO
│   └── response                       # JWT 응답, 메시지, 연체료 정보, 고객 입금 내역 DTO
├── repository                          # Spring Data JPA Repository 계층 (DB 접근)
├── security                            # JWT, 인증 필터, 사용자 상세정보 서비스 등 보안 관련 컴포넌트
├── service                             # 비즈니스 로직 계층 (고객, 입금, 엑셀, 수수료, 환불 등 서비스)
└── util                                # 유틸리티 클래스 (FileCache 등)
Getting Started
Prerequisites
Java: Java 11 이상 설치

Maven: Maven 3.x 이상 (빌드 및 의존성 관리를 위해)

Database: MySQL, PostgreSQL 또는 원하는 RDBMS (application.properties 파일에서 DB 설정 필수)

IDE (IntelliJ IDEA, Eclipse, VSCode 등)

설치 및 설정
Repository 클론

bash
복사
git clone https://your-repository-url.git
cd your-repository
환경 설정 파일 수정

src/main/resources/application.properties 또는 application.yml 파일에서 다음 항목들을 설정하세요.

properties
복사
# 데이터베이스 설정 (예: MySQL)
spring.datasource.url=jdbc:mysql://localhost:3306/your_database?useSSL=false&serverTimezone=UTC
spring.datasource.username=your_username
spring.datasource.password=your_password

# JPA/Hibernate 설정
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# CORS, allowed origins – config에서 사용
allowed.origins=http://localhost:3000

# JWT 설정 – secret은 32자 이상 필요합니다.\n
jwt.secret=Your_32+Character_JWT_Secret_Key
jwt.expirationMs=86400000

# 파일 업로드 디렉토리\n
file.upload-dir=/path/to/upload-dir
빌드

Maven을 사용하는 경우 다음 명령어 실행:

bash
복사
mvn clean install
애플리케이션 실행
빌드가 완료되면 애플리케이션을 실행합니다.

bash
복사
mvn spring-boot:run
또는 생성된 JAR 파일을 실행합니다.

bash
복사
java -jar target/your-artifact-name.jar
API Endpoints 개요
프로젝트는 다양한 REST API 엔드포인트를 제공합니다. 주요 엔드포인트는 다음과 같습니다.

인증 (Auth)

POST /api/auth/signin – 로그인 (JWT 토큰 반환)

POST /api/auth/signup – 신규 관리자 회원가입

고객 관리

GET /customers/nextId – 다음 고객 ID 조회

POST /customers – 신규 고객 생성 (입금 내역 및 차수 초기화 포함)

GET /customers/{id} – 고객 상세 조회

PUT /customers/{id} – 고객 정보 업데이트 및 전체 재계산

DELETE /customers/{id} – 고객 삭제

GET /customers/search – 이름/번호 기반 고객 검색

입금 내역

GET /deposit/customer/{userId} – 특정 고객 입금 내역 조회

POST /deposit – 입금 내역 생성

PUT /deposit/{id} – 입금 내역 수정

DELETE /deposit/{id} – 입금 내역 삭제

GET /deposit/phase-summary – 각 차수별 통계 조회

엑셀 업로드/다운로드

POST /api/deposithistory/excel/upload – 입금 내역 엑셀 업로드 (SSE 진행 상황 전송)

GET /api/deposithistory/excel/download/progress – 입금 내역 엑셀 다운로드 진행 상황

GET /api/deposithistory/excel/download/file – 고유 fileId 기반 엑셀 파일 다운로드

GET /files/format1/{id} – 일반 신청서(포맷1) 엑셀 파일 생성 및 다운로드

GET /files/format2/{id} – 일반 부속 서류(포맷2) 엑셀 파일 생성 및 다운로드

POST /files/uploadExcelWithProgress – 파일 업로드 및 SSE 진행상황 전송

GET /files/regfiledownload/progress – 고객 목록 기반 파일 생성 및 진행 상황 전송\n - GET /files/regfiledownload/file – 생성된 파일 다운로드

공지사항

POST /api/notices – 공지사항 등록

PUT /api/notices/{id} – 공지사항 수정

DELETE /api/notices/{id} – 공지사항 삭제

GET /api/notices/{id} – 공지사항 단건 조회

GET /api/notices/search – 공지사항 검색

GET /api/notices – 전체 공지사항 목록 조회

환불 처리

POST /api/refunds/excel/upload – 환불 엑셀 업로드 (SSE 진행 상황 전송)

GET /api/refunds/excel/download/progress – 환불 엑셀 다운로드 진행 상황

GET /api/refunds/excel/download/file – 환불 엑셀 파일 다운로드

테스트
프로젝트에 포함된 단위 테스트/통합 테스트를 실행하여 기능을 검증할 수 있습니다.

bash
복사
mvn test
테스트 커버리지가 충분한지 확인하고, 필요 시 CI/CD 파이프라인에 통합하여 릴리즈 전 자동화 테스트를 수행하세요.

배포
빌드된 JAR 파일을 생성하고, 클라우드 환경(예: AWS, Azure, Google Cloud) 또는 온프레미스 서버에 배포합니다.

Dockerfile 작성 후 컨테이너화할 수도 있습니다.

기여하기
이 Repository를 Fork합니다.

새로운 Feature/버그픽스 브랜치를 생성합니다.

코드를 수정하고 commit한 뒤, Pull Request를 생성합니다.

코드 리뷰 후 병합합니다.

라이선스
이 프로젝트는 MIT License 하에 배포됩니다.
