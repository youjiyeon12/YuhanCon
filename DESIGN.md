---
name: YuhanCon
description: 포스터가 무대를 채우는 홈과, 입장권·티켓 모양으로 통일한 공연 예매 화면
colors:
  ground: "#F2F3F5"
  paper: "#FFFFFF"
  ink: "#14161B"
  ink-2: "#4A4F5C"
  ink-3: "#636977"
  accent: "#2B3FD6"
  accent-dark: "#1F2FA8"
  accent-soft: "#E8EBFC"
  line: "#C9CDD6"
  line-soft: "#E1E4EA"
  stamp: "#D42F25"
  stamp-dark: "#A1261B"
  ok: "#1E7A45"
  ok-soft: "#E4F3EA"
typography:
  numerals: "Black Han Sans Digits (날짜 숫자·점만, 자체 호스팅)"
  body: "Pretendard, Apple SD Gothic Neo, Malgun Gothic, Noto Sans KR, system-ui"
radius:
  card: "14px"
  panel: "20px"
  pill: "999px"
---

# YuhanCon 디자인

2026-11-22 시안 선택으로 정했다. 시안 파일: `.impeccable/mocks/v2/` (홈 A·B·C, 다른 화면 D). 구현은 `static/css/app.css` 하나와 `templates/fragments.html` 조각.

## 방향

- **홈 = 무대.** 첫 화면 전체를 포스터 슬라이드가 채운다(시안 A). 포스터는 잘리지 않게 `contain`으로 두고, 같은 포스터를 흐리게(blur 40px, 밝기 0.42) 깔아 빈 곳을 채운다. 큰 날짜 숫자 + 공연명 + 예매 버튼. 5초마다 넘어가고 진행 바를 누르면 그 장면부터 5초를 다시 센다. 마우스·키보드 포커스가 올라가면 멈춘다.
- 그 아래 **이번 주 무대**(시안 B): 오늘부터 7일 칸, 공연 있는 날은 accent 테두리, 오늘은 잉크색 칸.
- **다가오는 공연**(시안 C): 분류 칩으로 바로 거르는 세로 입장권 카드, 살짝 기울여 쌓은 느낌(모바일에서는 기울이지 않음).
- **나머지 화면**(시안 D): 같은 토큰으로 둥근 카드·칩·패널·표. 공연 카드는 가로 입장권, 마이페이지 예매 내역은 절취선이 있는 티켓.

## 규칙

- 색: 배경 ground, 카드 paper, 글자 ink 3단계. 강조와 행동은 accent 하나. 빨강 계열(stamp)은 매진·종료 도장, 오류 안내, 삭제·취소 버튼, 필수 표시에만. 성공 안내만 ok(초록).
- 절취선(점선 + 양끝 반원 홈)은 입장권과 티켓에만 쓴다. 다른 구분은 여백과 옅은 선(line-soft).
- 숫자: 날짜는 Black Han Sans Digits. 본문은 시스템 한글 글꼴, `word-break: keep-all`.
- 버튼: 둥근 알약. 기본은 accent 채움, 보조는 테두리(ghost), 위험은 ghost + 빨간 글자.
- 칩: 선택되면 잉크색 채움(`aria-pressed`).
- 포스터 없는 공연: accent-dark 점무늬 바탕에 분류 이름. 상세 화면은 "포스터 준비 중" 점선 상자.
- 좌석표: STAGE 막대 아래 열 이름 + 좌석 칸. 고른 좌석은 accent 채움, 예매된 좌석은 빗금(색이 아니라 무늬로도 구분), 1인 최대 6석.
- 움직임: 슬라이드 전환·카드 hover만. `prefers-reduced-motion`이면 끈다.
- 반응형: 760px 이하에서 메뉴는 햄버거, 슬라이드는 포스터 위·글 아래, 이번 주 무대는 가로 스크롤, 티켓은 꼬리표가 아래로.
- 접근성: 건너뛰기 링크, 현재 메뉴 `aria-current`, 폼 오류 `aria-invalid` + 칸 아래 문구, 좌석 칸은 실제 체크박스(화면 밖 이름 포함).
- 보안 제약: CSP가 인라인 스타일·스크립트를 막으므로 `style=""`, `th:style`, 인라인 `<script>`를 쓰지 않는다.
