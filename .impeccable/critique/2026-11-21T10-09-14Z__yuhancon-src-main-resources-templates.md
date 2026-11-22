---
target: YuhanCon 전체 화면
total_score: 17
max_score: 40
na_heuristics: 
p0_count: 0
p1_count: 3
target_identity: "file:D:\\portfolio\\YuhanCon\\YuhanCon\\src\\main\\resources\\templates"
timestamp: 2026-11-21T10-09-14Z
slug: yuhancon-src-main-resources-templates
---
# YuhanCon templates critique (dual-agent)

Nielsen: 1 status 1 | 2 real-world 2 | 3 control 2 | 4 consistency 1 | 5 error prevention 2 | 6 recognition 2 | 7 flexibility 1 | 8 minimalist 2 | 9 error recovery 3 | 10 help 1 => 17/40 Poor

Specificity: generic CRUD look (#333 nav, centered h1, gray table, gray buttons); only poster slider is concert-specific; #3f0549 only on auth pages.
Detector: CLI 6 (3 real: login.html red 4.0:1, mypage .btn-cancel 4.0:1; false: broken-image x2 th:src, flat-type x2 th:href css unread). Browser: overused-font Arial (concertList, boardMain, mypage), low-contrast concertDetail .meta-info 3.3:1, nested-cards mypage x2. Measured: concertList overflow 447px @390, boardMain title vertical, nav tap targets 17-21px, signup outline:none.

Priority issues:
- [P1] No reservation success feedback / no cancel confirm -> harden
- [P1] Mobile breakage: concertList overflow, nav wrap, main.html slide-container </div> misplaced so arrows over preview -> adapt
- [P1] Split visual system: 3 fonts, 2 accents, nav copied in 12 css -> typeset, polish
- [P2] concertDetail hierarchy: reserve not primary, 250px poster, unlabeled seat input -> layout
- [P2] Concert status (past/sold out/empty lists) not shown -> harden

Personas: Jordan (no hero CTA, card -> login with no way back), Casey (3-line nav, horizontal scroll, 20px taps), Sam (unlabeled seat input, 6 empty links in mypage, alt "공연1", outline none, no landmarks).
