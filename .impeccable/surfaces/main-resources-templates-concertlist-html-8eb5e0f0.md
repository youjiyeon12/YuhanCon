---
version: 1
slug: "main-resources-templates-concertlist-html-8eb5e0f0"
primary_target: "YuhanCon/src/main/resources/templates/concertList.html"
related_targets: ["YuhanCon/src/main/resources/templates/concertDetail.html","YuhanCon/src/main/resources/templates/main.html","YuhanCon/src/main/resources/templates/concertWrite.html","YuhanCon/src/main/resources/templates/concertEdit.html"]
---

# 공연 화면 (메인 · 공연 목록 · 상세 · 등록 · 수정)

범위: Operate(앱 화면). 메인은 첫 화면이라 공연 사진이 이끈다. 팀원 담당 화면(로그인·회원가입, 게시판, 마이페이지)은 이 방향을 적용하지 않는다.
사용자와 할 일: 포트폴리오 심사자가 공연을 고르고, 예매하고, 상태(남은 좌석·매진·종료·내 예매)를 바로 읽는다.
제약: URL·기능·메뉴 구성·메인 슬라이드 사진 유지. 가격·시간·좌석 배치는 없으므로 암시하지 않는다.

## Direction contract

THESIS: 공연 한 편을 인쇄 입장권 한 장으로 보여 준다. 포스터와 공연 정보가 몸통이고, 절취선 너머 스텁이 날짜·좌석·예매 행동을 맡는다. 표로 나열하는 관리자식 목록을 거부한다.

OWN-WORLD: 차가운 티켓 용지 바탕 #F2F3F5, 잉크 #14161B, 울트라마린 #2B3FD6(주요 버튼·현재 메뉴·링크), 절취선·가는 선 #C9CDD6, 도장 빨강 #E0352B(매진·종료만). 상태는 채움으로도 구분: 예매 가능 흰 스텁, 매진 빗금, 종료 점 채움. 숫자는 모두 tabular-nums. 포스터는 모두 3:4, 같은 기준선.

STORY: 심사자는 목록에서 날짜와 남은 좌석을 한눈에 비교하고, 상세에서 절취선 오른쪽 스텁으로 바로 예매한 뒤, 완료 안내를 본다.

FIRST VIEWPORT: 공연 목록. 상단 내비(YuhanCon, 홈/공연정보/게시판/마이페이지, 로그아웃). 제목 "공연 목록"과 오른쪽 "공연 등록" 버튼. 그 아래 2열 입장권 카드: 왼쪽 3:4 포스터와 제목·장소, 점선 절취선과 위아래 반원 노치, 오른쪽 스텁에 큰 날짜(12.20 토)와 남은 좌석. 상세에서는 같은 절취선이 포스터·정보와 예매 스텁을 나눈다.

FORM: 종이 입장권, 근거 목록 4번째(배정). seed:5b4dd13d60b29cdb. 받은 규율: 탭 숫자 정밀도(데이터 바코드), 채움으로 상태 구분(무용 기보), 같은 비율·기준선(식물 도판), 절취선 하나가 정보|행동을 나누는 유일한 구분선(안내 지도).

FINISH: unreviewed and undocumented is unfinished; this build ends with the finish review, the verdict, DESIGN.md, and every shipping raster carrying its provenance
