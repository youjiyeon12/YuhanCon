# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 프로젝트

YuhanCon — 공연 예매 + 동행 커뮤니티(게시판) 웹 앱. Spring Boot 3.5 / Java 21 / Maven / Thymeleaf / Spring Security / Spring Data JPA / H2. 2인 팀 학습용 프로젝트.

실제 Maven 프로젝트는 저장소 루트가 아니라 `YuhanCon/` 하위 폴더에 있다. `YuhanCon/bin/`, `YuhanCon/.metadata/`는 Eclipse가 만든 산출물이므로 고치지 않는다 (소스는 `YuhanCon/src/`만).

## 명령어

모두 `YuhanCon/` 폴더에서 실행한다 (Windows는 `mvnw.cmd`).

```
./mvnw spring-boot:run                              # 실행 → http://localhost:8080/main
./mvnw package                                      # 빌드
./mvnw test                                         # 전체 테스트
./mvnw test -Dtest=YuhanConFlowTest                 # 기능·보안 테스트 (H2 서버 없이 메모리 DB로 실행)
./mvnw test -Dtest=YuhanConFlowTest#seatReservationRules   # 테스트 메서드 하나
```

- **H2를 서버 모드로 먼저 띄워야 한다.** datasource가 `jdbc:h2:tcp://localhost/~/yuhancon2` (sa / 빈 비밀번호)라서 H2 TCP 서버가 없으면 앱 실행도, `contextLoads` 테스트도 실패한다. `YuhanConFlowTest`만 `jdbc:h2:mem`으로 덮어써서 서버 없이 돈다. H2 서버는 원격으로 새 DB를 만들지 않으므로, DB 파일이 없으면 한 번 `org.h2.tools.Shell -url jdbc:h2:~/yuhancon2`로 만든다. 예전 DB `~/yuhancon`(회원 기본키가 숫자 id이던 시절)은 지우지 않고 남겨 두었다.
- 스키마는 `ddl-auto=update`로 엔티티에서 자동 생성된다. 마이그레이션 파일은 없다.
- 시작 데이터(`config/DataInitializer`): 관리자가 없으면 `admin01`을 만든다. 비밀번호는 환경변수 `YUHANCON_ADMIN_PASSWORD`, 없으면 무작위로 만들어 시작 로그에 한 번 찍는다. `yuhancon.demo-data=true`이고 환경변수 `YUHANCON_DEMO_PASSWORD`가 있으며 공연이 하나도 없으면 시연 데이터(공연 등록자 `stage01`, 일반 사용자 `fan0001`, 공연 17개, 글 1개)를 넣는다. 비밀번호를 소스나 문서에 적지 않는다.
- `spring-boot:run` 중에 `static/`·`templates/`를 고치면 `./mvnw -o -q process-resources`로 `target/classes`에 복사해야 화면에 반영된다. Java를 고치면 devtools가 앱을 다시 띄운다.
- 린터/포매터 설정은 없다.

## 구조 (MVC)

패키지 `com.yuhancon` 아래 `controller` / `service` / `repository` / `domain` / `web`(폼 DTO) / `security` / `config`.

- **컨트롤러는 얇게, 규칙은 서비스에.** 컨트롤러는 요청을 받아 서비스 하나를 부르고 뷰 이름이나 리다이렉트를 돌려준다. 입력 검사·권한 확인·좌석 계산은 `MemberService`, `ConcertService`, `ReserveService`, `BoardService`가 한다. 사용자에게 보여 줄 오류는 서비스가 `UserFacingException`(칸별이면 `MemberService.FieldException`)으로 던지고 컨트롤러가 잡아 flash `errorMessage`나 폼 다시 그리기로 보여 준다.
- **엔티티를 폼에 직접 바인딩하지 않는다.** 폼은 `web/`의 DTO(`SignupForm`, `MemberEditForm`, `ConcertForm`, `BoardForm`)로 받는다(요청에 `id`·`role` 등을 끼워 넣는 공격 차단). 로그인 회원은 `Principal.getName()`(=아이디)으로 `memberService.get`해서 쓴다. `@ModelAttribute`로 `Member`를 받으면 안 된다(OSIV가 켜져 있어 바인딩된 값이 저장될 수 있다).
- 모든 화면에 `me`(로그인 회원 또는 null)가 `CurrentMemberAdvice`로 들어간다. 메뉴·버튼 표시는 `me.admin`, `me.canManageConcerts()`로 나눈다.
- 템플릿은 `templates/fragments.html`의 조각(head, nav, flash, footer, 공연 카드 `ticket`, 좌석표 `seatmap`)을 `th:replace`로 쓴다. `th:replace`는 `th:each`보다 먼저 실행되므로 반복은 `<th:block th:each>`로 감싼다.
- URL은 평면 구조: `/main`, `/concertList?category=&sort=`, `/concertDetail/{id}`, `/concertWrite`, `/concert/save`, `/concertEdit/{id}`, `/concertDelete/{id}`, `/concert/{id}/like`, `/reserve`, `/reserveEdit/{id}`, `/cancelReserve/{id}`, `/boardMain`, `/boardDetail/{id}`, `/boardWrite`, `/boardEdit/{id}`, `/boardDelete/{id}`, `/mypage`, `/editMember`, `/admin`, `/admin/approve`, `/admin/slides`, `/signup/check-id`. 변경은 전부 POST.

### 회원·권한 (`config/SecurityConfig`, `security/`)

- 회원 기본키는 **아이디**(`Member.loginId`, 영문+숫자 섞어 6~12자, 소문자로 저장)다. `Member`는 `Persistable`이라 `save`가 같은 아이디를 덮어쓰지 않고 새 행만 넣는다.
- 권한 `Role`: `USER`(일반 사용자, 예매), `PERFORMER`(공연 등록자, 등록+예매), `ADMIN`(관리자). 가입 때 USER/PERFORMER 중 고르고, ADMIN은 가입으로 만들 수 없다.
- 공개: `/`, `/main`, `/signup`, `/signup/check-id`, `/login`, 정적 파일, `/uploaded-images/**`, `/error`, GET `/concertList`·`/concertDetail/**`. `/admin/**`은 ADMIN, 공연 등록·수정·삭제 URL은 PERFORMER·ADMIN. 나머지는 로그인 필요. 서비스에서도 본인 여부를 다시 확인한다(`ConcertService.canEdit`, `BoardService.canEdit`, `ReserveService.getOwn`).
- 폼 로그인(`usernameParameter("loginId")`). 실패하면 입력한 아이디를 세션(`SecurityConfig.LAST_LOGIN_ID`)에 잠깐 담아 로그인 화면에 다시 채운다(비밀번호는 남기지 않음). "로그인 상태 유지"는 remember-me(14일, 키는 `YUHANCON_REMEMBER_KEY`, 없으면 실행마다 새로 만들어 재시작하면 풀림).
- 로그인 5번 연속 실패하면 10분 잠금(`Member.failedLoginCount`·`lockedUntil`, `MemberService.recordLoginFailure`). 잠긴 계정은 `CustomUserDetails.isAccountNonLocked`가 false라 비밀번호 확인 전에 막히고 `/login?locked`로 간다. 성공하면 횟수 초기화. 없는 아이디는 세지 않고 일반 실패 안내만 보여 준다.
- 비밀번호는 BCrypt. CSRF 켜짐(폼은 `th:action` 필수). CSP는 `script-src 'self'; style-src 'self'`이므로 **인라인 `<script>`·`style=""`를 쓰지 않는다**(JS는 `static/js/app.js` 하나, 스타일은 `static/css/app.css` 하나). `frame-ancestors 'none'`.

### 공연·예매 규칙

- 공연 등록자가 올린 공연은 `PENDING`(승인 대기)이고 관리자가 `/admin`에서 골라(전체 선택 가능) 승인하면 `APPROVED`로 공개된다. 관리자가 올리면 바로 공개. 승인 전 공연은 등록자·관리자 외에는 404.
- 분류 `Category`(콘서트·학교공연·버스킹·기타, 기타면 `categoryEtc`). 목록은 분류 필터 + 정렬(최신 등록순(기본)/오래된 등록순/좋아요 순. "공연 가까운 순"은 기준이 모호하고 주소를 받지 않아 뺐다)로 다가오는 공연 최대 8개(`LIST_LIMIT`), 지난 공연은 최근 8개만. 홈 "다가오는 공연"은 전체·분류마다 공연 날짜순 최대 4개(`HOME_LIMIT`, `homeGroups`)를 미리 받아 칩으로 바꿔 보여 준다.
- 좌석: `Concert.seatLayout`이 `"A:10,B:20"`이면 좌석 지정 공연(관객이 자리를 고름), null이면 자유석(매수만). 고른 좌석은 `ReservedSeat` 행으로 저장하고 (공연, 좌석) 유일 제약이 동시 선점을 막는다. 예매가 있으면 좌석 배치를 바꿀 수 없다.
- 예매(`ReserveService`): 한 회원은 한 공연에 예매 하나((회원, 공연) 유일 제약), 최대 6석. 다시 예매하려면 수정하거나 취소한다. 매진이면 수정 불가(취소만). 지난 공연은 예매·수정·취소 불가. `availableSeats`는 조건부 UPDATE로 빼고 더한다(영속성 컨텍스트를 거치지 않으므로 이미 읽은 `Concert`의 값은 갱신되지 않는다). 공연 수정과 예매 수정·취소는 행을 잠그고 읽는다(`findForUpdate`). `Concert`는 `@DynamicUpdate`이고 `likeCount`는 `updatable = false`라, 엔티티 저장이 남은 좌석·좋아요 수를 옛 값으로 덮지 않는다.
- 삭제: 등록자는 예매가 없을 때만, 관리자는 예매가 있어도 삭제(좌석·예매·좋아요·최근 본 기록·포스터 파일까지 지운다). `Concert`를 참조하는 엔티티를 추가하면 `ConcertService.delete`도 고친다.
- 좋아요: `ConcertLike`((회원, 공연) 유일) + `Concert.likeCount`(UPDATE로 증감). 마이페이지에 좋아요한 공연 목록.
- 홈 슬라이드: 관리자가 포스터 있는 다가오는 공연 중 최대 6개를 골라 순서를 정한다(`slideOrder`, 관리자 화면의 "홈에 보일 순서"에서 위·아래 버튼, 스크립트가 고른 순서대로 `ids`를 보낸다). 4개보다 적거나 끝난 공연이 빠지면 가장 가까운 공연 포스터로 4개를 채운다. 공연이 하나도 없으면 기본 사진.
- 게시판: 로그인 회원 누구나 쓰고 본인 글만 수정·삭제, 관리자는 모든 글 수정·삭제. 글은 `th:text`로만 출력해 스크립트가 실행되지 않는다.
- 댓글(`Comment`): 대댓글은 `parent`가 있는 댓글이고 한 단계만(답글의 답글은 같은 댓글 아래로). 본인 댓글만 삭제(관리자는 전부). 답글이 남은 댓글은 `deleted`로 "삭제된 댓글입니다"만 남기고, 마지막 답글이 지워지면 함께 지운다. 게시글 좋아요는 `BoardLike`((회원, 글) 유일) + `Board.likeCount`(전용 UPDATE, `updatable = false`). 글을 지우면 답글 → 댓글 → 좋아요 → 글 순으로 지운다(`BoardService.delete`).
- 컬럼 기본값은 `@ColumnDefault`로 준다. `columnDefinition`에 default를 넣으면 Hibernate가 JPQL 산술식의 cast에 그 문구를 넣어 SQL 오류가 난다.

### 이미지 업로드

`service/ImageStorage`가 `user.dir/uploaded-images`에 저장하고 `WebConfig`가 `/uploaded-images/**`로 서빙한다(실행 위치 기준). 한도 10MB, 확장자 jpg·jpeg·png·gif·webp만, 파일 앞부분(매직 바이트)도 확장자와 맞아야 한다. 시연 데이터의 포스터는 `static/images/`를 가리키며 삭제 대상이 아니다.

### 디자인

규칙은 루트 `DESIGN.md`. 시안은 `.impeccable/mocks/v2/`. 홈은 풀스크린 포스터 무대(5초 자동 진행, 진행 바를 누르면 초기화) + 이번 주 무대(오늘부터 7일) + 분류 칩으로 거르는 입장권 카드. 나머지 화면은 둥근 카드·칩·패널·표·티켓형 예매 내역. 날짜 숫자만 `static/fonts/black-han-sans-digits.woff2`(OFL).
