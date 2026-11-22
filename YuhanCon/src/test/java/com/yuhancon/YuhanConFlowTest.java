package com.yuhancon;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.yuhancon.domain.Board;
import com.yuhancon.domain.Category;
import com.yuhancon.domain.Concert;
import com.yuhancon.domain.ConcertStatus;
import com.yuhancon.domain.Member;
import com.yuhancon.domain.Role;
import com.yuhancon.repository.BoardRepository;
import com.yuhancon.repository.ConcertLikeRepository;
import com.yuhancon.repository.ConcertRepository;
import com.yuhancon.repository.MemberRepository;
import com.yuhancon.repository.RecentConcertViewRepository;
import com.yuhancon.repository.ReserveRepository;
import com.yuhancon.repository.ReservedSeatRepository;
import com.yuhancon.security.CustomUserDetails;
import com.yuhancon.service.ConcertService;

// 요청서 규칙을 실제 요청으로 확인한다. 메모리 DB를 써서 H2 서버 없이 돈다.
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:flowtest;DB_CLOSE_DELAY=-1",
    "yuhancon.demo-data=false"
})
@AutoConfigureMockMvc
class YuhanConFlowTest {

    @Autowired MockMvc mvc;
    @Autowired MemberRepository memberRepository;
    @Autowired ConcertRepository concertRepository;
    @Autowired ReserveRepository reserveRepository;
    @Autowired ReservedSeatRepository seatRepository;
    @Autowired ConcertLikeRepository likeRepository;
    @Autowired RecentConcertViewRepository recentRepository;
    @Autowired BoardRepository boardRepository;
    @Autowired com.yuhancon.repository.CommentRepository commentRepository;
    @Autowired com.yuhancon.repository.BoardLikeRepository boardLikeRepository;
    @Autowired ConcertService concertService;
    @Autowired PasswordEncoder passwordEncoder;

    Member admin, stage, fan, fan2;

    @BeforeEach
    void setUp() {
        seatRepository.deleteAll();
        reserveRepository.deleteAll();
        likeRepository.deleteAll();
        recentRepository.deleteAll();
        commentRepository.deleteAll(commentRepository.findAll().stream().filter(c -> c.getParent() != null).toList());
        commentRepository.deleteAll();
        boardLikeRepository.deleteAll();
        boardRepository.deleteAll();
        concertRepository.deleteAll();
        memberRepository.deleteAll();
        admin = saveMember("admin01", Role.ADMIN);
        stage = saveMember("stage01", Role.PERFORMER);
        fan = saveMember("fan0001", Role.USER);
        fan2 = saveMember("fan0002", Role.USER);
    }

    // ---------- 회원가입·로그인 ----------

    @Test
    void signupRules() throws Exception {
        mvc.perform(get("/signup/check-id").param("loginId", "fan0001")).andExpect(jsonPath("$.available").value(false));
        mvc.perform(get("/signup/check-id").param("loginId", "abc")).andExpect(jsonPath("$.available").value(false));
        mvc.perform(get("/signup/check-id").param("loginId", "newuser1")).andExpect(jsonPath("$.available").value(true));

        // 아이디 형식(영문+숫자 6~12자)
        signup("onlyletters", "a@test.com", "Pass1234", "Pass1234", "USER")
            .andExpect(model().attributeHasFieldErrors("form", "loginId"));
        // 아이디 중복(대소문자 무시)
        signup("FAN0001", "b@test.com", "Pass1234", "Pass1234", "USER")
            .andExpect(model().attributeHasFieldErrors("form", "loginId"));
        // 비밀번호 확인
        signup("newuser1", "c@test.com", "Pass1234", "Pass9999", "USER")
            .andExpect(model().attributeHasFieldErrors("form", "passwordConfirm"));
        // 가입으로 관리자를 만들 수 없다
        signup("newuser1", "c@test.com", "Pass1234", "Pass1234", "ADMIN")
            .andExpect(model().attributeHasFieldErrors("form", "role"));
        // 공연 등록자로 가입, 비밀번호는 해시로 저장
        signup("NewUser1", "C@Test.com", "Pass1234", "Pass1234", "PERFORMER").andExpect(redirectedUrl("/login?signup"));
        Member m = memberRepository.findById("newuser1").orElseThrow();
        assertThat(m.getRole()).isEqualTo(Role.PERFORMER);
        assertThat(m.getEmail()).isEqualTo("c@test.com");
        assertThat(m.getPassword()).startsWith("$2").isNotEqualTo("Pass1234");
    }

    @Test
    void loginFailureKeepsIdAndRememberMeWorks() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mvc.perform(post("/login").session(session).with(csrf()).param("loginId", "fan0001").param("password", "wrong"))
            .andExpect(redirectedUrl("/login?error"));
        mvc.perform(get("/login").session(session)).andExpect(model().attribute("lastLoginId", "fan0001"));

        mvc.perform(post("/login").with(csrf()).param("loginId", "FAN0001").param("password", "Test1234").param("remember", "true"))
            .andExpect(redirectedUrl("/main"))
            .andExpect(header().stringValues("Set-Cookie", org.hamcrest.Matchers.hasItem(containsString("remember-me="))));
    }

    // ---------- 권한·승인 ----------

    @Test
    void onlyPerformerOrAdminCanRegisterAndAdminApproves() throws Exception {
        mvc.perform(get("/concertWrite").with(as(fan))).andExpect(status().isForbidden());

        mvc.perform(concertForm().with(as(stage))).andExpect(status().is3xxRedirection());
        Concert c = concertRepository.findAll().get(0);
        assertThat(c.getStatus()).isEqualTo(ConcertStatus.PENDING);
        assertThat(c.getTotalSeats()).isEqualTo(25); // A:10 + B:15

        // 승인 전에는 다른 사람에게 '없는 공연', 목록에도 없다
        mvc.perform(get("/concertDetail/" + c.getId()).with(as(fan))).andExpect(status().isNotFound());
        mvc.perform(get("/concertDetail/" + c.getId()).with(as(stage))).andExpect(status().isOk());
        mvc.perform(get("/concertList")).andExpect(content().string(not(containsString("정기공연"))));
        // 승인 전 예매 불가, 관리자 화면은 관리자만
        mvc.perform(post("/reserve").with(csrf()).with(as(fan)).param("concertId", c.getId().toString()).param("seats", "A1"))
            .andExpect(status().isNotFound()); // 승인 전 공연은 다른 사람에게 보이지 않는다
        mvc.perform(post("/reserve").with(csrf()).with(as(stage)).param("concertId", c.getId().toString()).param("seats", "A1"))
            .andExpect(flash().attribute("errorMessage", "승인 전 공연은 예매할 수 없습니다."));
        assertThat(reserveRepository.count()).isZero();
        mvc.perform(post("/admin/approve").with(csrf()).with(as(stage)).param("ids", c.getId().toString())).andExpect(status().isForbidden());

        Concert c2 = saveConcert("두번째", ConcertStatus.PENDING, null, 10, null);
        mvc.perform(post("/admin/approve").with(csrf()).with(as(admin)).param("ids", c.getId().toString(), c2.getId().toString()))
            .andExpect(flash().attribute("successMessage", "공연 2개를 승인했습니다."));
        mvc.perform(get("/concertDetail/" + c.getId()).with(as(fan))).andExpect(status().isOk());
        mvc.perform(get("/concertList")).andExpect(content().string(containsString("정기공연")));
    }

    @Test
    void performerCannotDeleteWithReservationsButAdminCan() throws Exception {
        Concert c = saveConcert("공연", ConcertStatus.APPROVED, null, 10, null);
        reserve(fan, c, "2", null).andExpect(redirectedUrl("/mypage"));
        mvc.perform(post("/concertDelete/" + c.getId()).with(csrf()).with(as(stage)))
            .andExpect(flash().attributeExists("errorMessage"));
        mvc.perform(post("/concertDelete/" + c.getId()).with(csrf()).with(as(fan))).andExpect(status().isForbidden());
        mvc.perform(post("/concertDelete/" + c.getId()).with(csrf()).with(as(admin)).param("back", "/admin"))
            .andExpect(redirectedUrl("/admin"));
        assertThat(concertRepository.count()).isZero();
        assertThat(reserveRepository.count()).isZero();
    }

    // ---------- 예매 ----------

    @Test
    void seatReservationRules() throws Exception {
        Concert c = saveConcert("좌석 공연", ConcertStatus.APPROVED, "A:5,B:5", 0, null);
        reserve(fan, c, null, "A1", "A2").andExpect(redirectedUrl("/mypage"));
        assertThat(concertRepository.findById(c.getId()).orElseThrow().getAvailableSeats()).isEqualTo(8);

        // 같은 공연 중복 예매 불가
        reserve(fan, c, null, "B1").andExpect(flash().attribute("errorMessage", containsString("이미 예매한 공연")));
        // 남이 잡은 좌석 불가, 없는 좌석 불가, 6석 초과 불가
        reserve(fan2, c, null, "A2").andExpect(flash().attributeExists("errorMessage"));
        reserve(fan2, c, null, "Z9").andExpect(flash().attribute("errorMessage", "없는 좌석이 포함되어 있습니다."));
        reserve(fan2, c, null, "B1", "B2", "B3", "B4", "B5", "A3", "A4").andExpect(flash().attributeExists("errorMessage"));
        assertThat(reserveRepository.count()).isEqualTo(1);

        // 예매 수정: A1,A2 → A3 하나. 풀린 좌석은 남이 잡을 수 있다
        Long rid = reserveRepository.findAll().get(0).getId();
        mvc.perform(post("/reserveEdit/" + rid).with(csrf()).with(as(fan)).param("seats", "A3")).andExpect(redirectedUrl("/mypage"));
        assertThat(concertRepository.findById(c.getId()).orElseThrow().getAvailableSeats()).isEqualTo(9);
        reserve(fan2, c, null, "A1").andExpect(redirectedUrl("/mypage"));
        // 남의 예매는 고칠 수 없다
        mvc.perform(post("/reserveEdit/" + rid).with(csrf()).with(as(fan2)).param("seats", "B5")).andExpect(status().isForbidden());

        // 취소하면 좌석이 돌아온다
        mvc.perform(post("/cancelReserve/" + rid).with(csrf()).with(as(fan))).andExpect(flash().attributeExists("successMessage"));
        assertThat(concertRepository.findById(c.getId()).orElseThrow().getAvailableSeats()).isEqualTo(9);
        assertThat(seatRepository.findLabelsByConcert(c.getId())).containsExactly("A1");
    }

    @Test
    void freeSeatingAndSoldOutEditBlocked() throws Exception {
        Concert c = saveConcert("자유석", ConcertStatus.APPROVED, null, 3, null);
        reserve(fan, c, "7", null).andExpect(flash().attributeExists("errorMessage"));
        reserve(fan, c, "2", null).andExpect(redirectedUrl("/mypage"));
        reserve(fan2, c, "1", null).andExpect(redirectedUrl("/mypage"));
        // 매진되면 수정 불가(취소만)
        Long rid = reserveRepository.findByMemberAndConcert(fan, c).orElseThrow().getId();
        mvc.perform(post("/reserveEdit/" + rid).with(csrf()).with(as(fan)).param("seatCount", "1"))
            .andExpect(flash().attribute("errorMessage", containsString("수정할 수 없습니다")));
    }

    // ---------- 좋아요·목록·슬라이드 ----------

    @Test
    void likesFilterSortAndPastLimit() throws Exception {
        Concert a = saveConcert("버스킹 공연", ConcertStatus.APPROVED, null, 10, null);
        a.setCategory(Category.BUSKING);
        concertRepository.save(a);
        Concert b = saveConcert("콘서트 공연", ConcertStatus.APPROVED, null, 10, null);

        mvc.perform(post("/concert/" + b.getId() + "/like").with(csrf()).with(as(fan)));
        assertThat(concertRepository.findById(b.getId()).orElseThrow().getLikeCount()).isEqualTo(1);
        mvc.perform(get("/mypage").with(as(fan))).andExpect(content().string(containsString("콘서트 공연")));
        mvc.perform(post("/concert/" + b.getId() + "/like").with(csrf()).with(as(fan)));
        assertThat(concertRepository.findById(b.getId()).orElseThrow().getLikeCount()).isZero();

        mvc.perform(get("/concertList").param("category", "BUSKING"))
            .andExpect(content().string(containsString("버스킹 공연")))
            .andExpect(content().string(not(containsString("콘서트 공연"))));
        mvc.perform(get("/concertList").param("sort", "likes")).andExpect(status().isOk());

        for (int i = 1; i <= 10; i++) {
            Concert p = saveConcert("지난" + i, ConcertStatus.APPROVED, null, 10, null);
            p.setDate(LocalDate.now().minusDays(i));
            concertRepository.save(p);
        }
        assertThat(concertService.pastTop8()).hasSize(8).first().extracting(Concert::getTitle).isEqualTo("지난1");
    }

    @Test
    void slidesFillToFourAndMaxSix() throws Exception {
        Concert[] cs = new Concert[7];
        for (int i = 0; i < 7; i++) cs[i] = saveConcert("포스터" + i, ConcertStatus.APPROVED, null, 10, "/images/concert.jpg");
        // 아무것도 안 고르면 가까운 공연 4개
        assertThat(concertService.slides()).hasSize(4);
        // 하나만 고르면 그 공연이 먼저, 나머지는 가까운 공연으로 채움
        mvc.perform(post("/admin/slides").with(csrf()).with(as(admin)).param("ids", cs[6].getId().toString()));
        assertThat(concertService.slides()).hasSize(4).first().extracting(Concert::getId).isEqualTo(cs[6].getId());
        // 7개는 거절
        String[] all = java.util.Arrays.stream(cs).map(c -> c.getId().toString()).toArray(String[]::new);
        mvc.perform(post("/admin/slides").with(csrf()).with(as(admin)).param("ids", all)).andExpect(flash().attributeExists("errorMessage"));
        // 끝난 공연은 슬라이드에서 빠진다(다시 읽어서 날짜만 바꾼다. 예전 객체를 저장하면 slideOrder도 지워져 날짜 필터를 확인하지 못한다)
        Concert ended = concertRepository.findById(cs[6].getId()).orElseThrow();
        assertThat(ended.getSlideOrder()).isEqualTo(1);
        ended.setDate(LocalDate.now().minusDays(1));
        concertRepository.save(ended);
        assertThat(concertService.slides()).extracting(Concert::getId).doesNotContain(cs[6].getId());
    }

    // 결함 1 회귀: 고른 순서대로 보이고, 같은 선택을 다시 저장하거나 순서를 바꿔도 빠지지 않는다
    @Test
    void slidesKeepChosenOrderOnResave() throws Exception {
        Concert[] cs = new Concert[5];
        for (int i = 0; i < 5; i++) cs[i] = saveConcert("포스터" + i, ConcertStatus.APPROVED, null, 10, "/images/concert.jpg");
        String a = cs[3].getId().toString(), b = cs[1].getId().toString();
        mvc.perform(post("/admin/slides").with(csrf()).with(as(admin)).param("ids", a, b));
        assertThat(concertService.slides()).extracting(Concert::getId).startsWith(cs[3].getId(), cs[1].getId());
        mvc.perform(post("/admin/slides").with(csrf()).with(as(admin)).param("ids", a, b)); // 그대로 다시 저장
        assertThat(concertService.slides()).extracting(Concert::getId).startsWith(cs[3].getId(), cs[1].getId());
        mvc.perform(post("/admin/slides").with(csrf()).with(as(admin)).param("ids", b, a, cs[4].getId().toString())); // 순서 바꾸고 하나 추가
        assertThat(concertService.slides()).extracting(Concert::getId).startsWith(cs[1].getId(), cs[3].getId(), cs[4].getId());
    }

    // 결함 3 회귀: 공연을 고쳐도 좋아요 수와 예매로 줄어든 좌석이 옛 값으로 돌아가지 않는다
    @Test
    void concertEditKeepsLikesAndReservedSeats() throws Exception {
        Concert c = saveConcert("자유석", ConcertStatus.APPROVED, null, 10, null);
        reserve(fan, c, "2", null).andExpect(redirectedUrl("/mypage"));
        mvc.perform(post("/concert/" + c.getId() + "/like").with(csrf()).with(as(fan2)));
        mvc.perform(multipart("/concertEdit/" + c.getId()).with(csrf()).with(as(stage))
                .param("title", "고친 제목").param("place", "대강당").param("content", "소개")
                .param("date", c.getDate().toString()).param("category", "CONCERT").param("seated", "false").param("totalSeats", "12"))
            .andExpect(redirectedUrl("/concertDetail/" + c.getId()));
        Concert after = concertRepository.findById(c.getId()).orElseThrow();
        assertThat(after.getTitle()).isEqualTo("고친 제목");
        assertThat(after.getLikeCount()).isEqualTo(1);
        assertThat(after.getAvailableSeats()).isEqualTo(10); // 12석 - 예매 2석
    }

    // 결함 4 회귀: 같은 예매 수정을 동시에 두 번 보내도 좌석 수가 예매와 맞는다. 두 번째 취소는 안내로 끝난다.
    @Test
    void concurrentReserveEditKeepsSeatsAndDoubleCancelIsHandled() throws Exception {
        Concert c = saveConcert("자유석", ConcertStatus.APPROVED, null, 10, null);
        reserve(fan, c, "2", null).andExpect(redirectedUrl("/mypage"));
        Long rid = reserveRepository.findByMemberAndConcert(fan, c).orElseThrow().getId();

        java.util.concurrent.ExecutorService pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start = new java.util.concurrent.CountDownLatch(1);
        java.util.List<java.util.concurrent.Future<?>> jobs = new java.util.ArrayList<>();
        for (int i = 0; i < 2; i++) {
            jobs.add(pool.submit(() -> {
                start.await();
                return mvc.perform(post("/reserveEdit/" + rid).with(csrf()).with(as(fan)).param("seatCount", "4")).andReturn();
            }));
        }
        start.countDown();
        for (var j : jobs) j.get(30, java.util.concurrent.TimeUnit.SECONDS);
        pool.shutdown();
        int reserved = reserveRepository.findById(rid).orElseThrow().getSeatCount();
        assertThat(reserved).isEqualTo(4);
        assertThat(concertRepository.findById(c.getId()).orElseThrow().getAvailableSeats()).isEqualTo(10 - reserved);

        mvc.perform(post("/cancelReserve/" + rid).with(csrf()).with(as(fan))).andExpect(flash().attributeExists("successMessage"));
        mvc.perform(post("/cancelReserve/" + rid).with(csrf()).with(as(fan)))
            .andExpect(flash().attribute("errorMessage", "이미 취소된 예매입니다."));
        assertThat(concertRepository.findById(c.getId()).orElseThrow().getAvailableSeats()).isEqualTo(10);
    }

    // 로그인 5번 연속 실패하면 10분 잠금, 잠긴 동안은 맞는 비밀번호도 거절. 성공하면 실패 횟수 초기화.
    @Test
    void loginLocksAfterFiveFailures() throws Exception {
        for (int i = 1; i <= 4; i++) {
            mvc.perform(post("/login").with(csrf()).param("loginId", "fan0001").param("password", "wrong" + i))
                .andExpect(redirectedUrl("/login?error"));
        }
        mvc.perform(post("/login").with(csrf()).param("loginId", "fan0001").param("password", "Test1234"))
            .andExpect(redirectedUrl("/main")); // 4번 틀린 뒤 성공 → 초기화
        assertThat(memberRepository.findById("fan0001").orElseThrow().getFailedLoginCount()).isZero();

        for (int i = 1; i <= 4; i++) {
            mvc.perform(post("/login").with(csrf()).param("loginId", "fan0001").param("password", "wrong" + i))
                .andExpect(redirectedUrl("/login?error"));
        }
        mvc.perform(post("/login").with(csrf()).param("loginId", "fan0001").param("password", "wrong5"))
            .andExpect(redirectedUrl("/login?locked"));
        mvc.perform(post("/login").with(csrf()).param("loginId", "fan0001").param("password", "Test1234"))
            .andExpect(redirectedUrl("/login?locked"));
        assertThat(memberRepository.findById("fan0001").orElseThrow().isLoginLocked()).isTrue();
        // 없는 아이디는 몇 번 틀려도 같은 안내(아이디가 있는지 드러내지 않는다)
        mvc.perform(post("/login").with(csrf()).param("loginId", "nobody99").param("password", "x"))
            .andExpect(redirectedUrl("/login?error"));
    }

    // ---------- 게시판·보안 ----------

    @Test
    void boardEscapesScriptAndOwnershipRules() throws Exception {
        mvc.perform(post("/boardWrite").with(csrf()).with(as(fan)).param("title", "<b>제목</b>").param("content", "<script>alert(1)</script>"))
            .andExpect(status().is3xxRedirection());
        Board b = boardRepository.findAll().get(0);
        mvc.perform(get("/boardDetail/" + b.getId()).with(as(fan2)))
            .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
            .andExpect(content().string(not(containsString("<script>alert(1)"))));
        mvc.perform(post("/boardEdit/" + b.getId()).with(csrf()).with(as(fan2)).param("title", "x").param("content", "y"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/boardDelete/" + b.getId()).with(csrf()).with(as(admin))).andExpect(redirectedUrl("/boardMain"));
        assertThat(boardRepository.count()).isZero();
    }

    @Test
    void securityHeadersUploadCheckAndCsrf() throws Exception {
        mvc.perform(get("/main"))
            .andExpect(header().string("Content-Security-Policy", containsString("script-src 'self'")))
            .andExpect(header().string("X-Frame-Options", "DENY"));
        // 확장자만 이미지인 가짜 파일은 거절
        MockMultipartFile fake = new MockMultipartFile("imageFile", "poster.png", "image/png", "<svg onload=alert(1)>".getBytes());
        mvc.perform(concertForm().file(fake).with(as(stage)))
            .andExpect(model().attribute("errorMessage", containsString("이미지 파일")));
        assertThat(concertRepository.count()).isZero();
        // CSRF 토큰 없는 요청은 막는다
        mvc.perform(post("/boardWrite").with(as(fan)).param("title", "t").param("content", "c")).andExpect(status().isForbidden());
        // 없는 id는 404
        mvc.perform(get("/concertDetail/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/concertDetail/abc")).andExpect(status().isNotFound());
    }

    // 홈은 분류마다 최대 4개, 공연 정보 목록은 분류·정렬마다 최대 8개
    @Test
    void homeShowsFourAndListShowsEight() throws Exception {
        for (int i = 0; i < 10; i++) saveConcert("공연" + i, ConcertStatus.APPROVED, null, 10, null);
        var groups = concertService.homeGroups();
        assertThat(groups.get("ALL")).hasSize(4);
        assertThat(groups.get("CONCERT")).hasSize(4);
        assertThat(groups.get("BUSKING")).isEmpty();
        assertThat(concertService.upcoming(null, "latest")).hasSize(8);
        String html = mvc.perform(get("/concertList").param("sort", "likes")).andReturn().getResponse().getContentAsString();
        assertThat(html.split("class=\"tk-link\"", -1).length - 1).isEqualTo(8);
    }

    // 댓글·대댓글: 답글의 답글은 같은 댓글 아래로, 답글이 남은 댓글은 "삭제된 댓글"로 표시, 마지막 답글이 지워지면 함께 지움
    @Test
    void boardCommentsAndReplies() throws Exception {
        Board b = new Board();
        b.setTitle("글"); b.setContent("내용"); b.setMember(fan); b.setBoardAt(LocalDate.now());
        b = boardRepository.save(b);
        String url = "/boardDetail/" + b.getId() + "/comment";
        mvc.perform(post(url).with(csrf()).with(as(fan2)).param("content", "<script>alert(1)</script>"));
        var top = commentRepository.findAll().get(0);
        mvc.perform(post(url).with(csrf()).with(as(fan)).param("parentId", top.getId().toString()).param("content", "답글1"));
        var reply1 = commentRepository.findAll().stream().filter(c -> "답글1".equals(c.getContent())).findFirst().orElseThrow();
        mvc.perform(post(url).with(csrf()).with(as(fan2)).param("parentId", reply1.getId().toString()).param("content", "답글2"));
        var reply2 = commentRepository.findAll().stream().filter(c -> "답글2".equals(c.getContent())).findFirst().orElseThrow();
        assertThat(reply2.getParent().getId()).isEqualTo(reply1.getId()); // 답글의 답글(여러 단계)
        mvc.perform(post(url).with(csrf()).with(as(fan)).param("content", "   ")).andExpect(flash().attribute("errorMessage", "댓글 내용을 입력하세요."));
        mvc.perform(get("/boardDetail/" + b.getId()).with(as(fan)))
            .andExpect(content().string(containsString("&lt;script&gt;alert(1)&lt;/script&gt;")))
            .andExpect(content().string(containsString("답글2")));

        // 남의 댓글은 지울 수 없다
        mvc.perform(post("/comment/" + top.getId() + "/delete").with(csrf()).with(as(fan))).andExpect(status().isForbidden());
        // 답글이 있으면 삭제 표시만
        mvc.perform(post("/comment/" + top.getId() + "/delete").with(csrf()).with(as(fan2))).andExpect(status().is3xxRedirection());
        assertThat(commentRepository.findById(top.getId()).orElseThrow().isDeleted()).isTrue();
        mvc.perform(get("/boardDetail/" + b.getId()).with(as(fan))).andExpect(content().string(containsString("삭제된 댓글입니다.")));
        // 삭제된 댓글에는 답글 불가
        mvc.perform(post(url).with(csrf()).with(as(fan)).param("parentId", top.getId().toString()).param("content", "x"))
            .andExpect(flash().attribute("errorMessage", "삭제된 댓글에는 답글을 달 수 없습니다."));
        // 관리자는 남의 답글도 지울 수 있다. 답글이 달린 답글도 삭제 표시만 하고,
        // 맨 아래 답글이 지워지면 삭제 표시된 조상들이 위로 올라가며 함께 사라진다
        mvc.perform(post("/comment/" + reply1.getId() + "/delete").with(csrf()).with(as(admin)));
        assertThat(commentRepository.findById(reply1.getId()).orElseThrow().isDeleted()).isTrue();
        assertThat(commentRepository.findById(top.getId())).isPresent();
        mvc.perform(post("/comment/" + reply2.getId() + "/delete").with(csrf()).with(as(fan2)));
        assertThat(commentRepository.count()).isZero();
    }

    // 게시글 좋아요: 누르면 켜지고 다시 누르면 꺼진다. 댓글·좋아요가 있는 글도 지울 수 있다.
    @Test
    void boardLikeAndDeleteWithComments() throws Exception {
        Board b = new Board();
        b.setTitle("글"); b.setContent("내용"); b.setMember(fan); b.setBoardAt(LocalDate.now());
        b = boardRepository.save(b);
        mvc.perform(post("/board/" + b.getId() + "/like").with(csrf()).with(as(fan2)))
            .andExpect(flash().attribute("successMessage", "이 글에 좋아요를 눌렀습니다."));
        assertThat(boardRepository.findById(b.getId()).orElseThrow().getLikeCount()).isEqualTo(1);
        mvc.perform(post("/board/" + b.getId() + "/like").with(csrf()).with(as(fan2)));
        assertThat(boardRepository.findById(b.getId()).orElseThrow().getLikeCount()).isZero();
        mvc.perform(post("/board/" + b.getId() + "/like").with(csrf()).with(as(fan2)));
        // 글을 고쳐도 좋아요 수가 옛 값으로 돌아가지 않는다
        mvc.perform(post("/boardEdit/" + b.getId()).with(csrf()).with(as(fan)).param("title", "고친 글").param("content", "내용"));
        assertThat(boardRepository.findById(b.getId()).orElseThrow().getLikeCount()).isEqualTo(1);

        String url = "/boardDetail/" + b.getId() + "/comment";
        mvc.perform(post(url).with(csrf()).with(as(fan2)).param("content", "댓글"));
        var top = commentRepository.findAll().get(0);
        mvc.perform(post(url).with(csrf()).with(as(fan)).param("parentId", top.getId().toString()).param("content", "답글"));
        var reply = commentRepository.findAll().stream().filter(c -> "답글".equals(c.getContent())).findFirst().orElseThrow();
        mvc.perform(post(url).with(csrf()).with(as(fan2)).param("parentId", reply.getId().toString()).param("content", "답글의 답글"));
        mvc.perform(get("/boardMain").with(as(fan))).andExpect(content().string(containsString("aria-label=\"댓글 3\"")));
        mvc.perform(post("/boardDelete/" + b.getId()).with(csrf()).with(as(fan))).andExpect(redirectedUrl("/boardMain"));
        assertThat(boardRepository.count()).isZero();
        assertThat(commentRepository.count()).isZero();
        assertThat(boardLikeRepository.count()).isZero();
    }

    // ---------- 도우미 ----------

    private RequestPostProcessor as(Member m) {
        return user(new CustomUserDetails(m));
    }

    private Member saveMember(String id, Role role) {
        Member m = new Member();
        m.setLoginId(id);
        m.setEmail(id + "@test.com");
        m.setName("테스트");
        m.setPhone("01012345678");
        m.setRole(role);
        m.setPassword(passwordEncoder.encode("Test1234"));
        return memberRepository.save(m);
    }

    private Concert saveConcert(String title, ConcertStatus status, String layout, int free, String image) {
        Concert c = new Concert();
        c.setTitle(title);
        c.setContent("소개");
        c.setPlace("유한대");
        c.setDate(LocalDate.now().plusDays(3));
        c.setSeatLayout(layout);
        c.setTotalSeats(layout == null ? free : c.getSeatLabels().size());
        c.setAvailableSeats(c.getTotalSeats());
        c.setStatus(status);
        c.setImage(image);
        c.setMember(stage);
        c.setCreatedAt(LocalDateTime.now());
        return concertRepository.save(c);
    }

    private org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder concertForm() {
        return (org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder) multipart("/concert/save")
            .with(csrf())
            .param("title", "밴드부 정기공연").param("place", "대강당").param("content", "소개")
            .param("date", LocalDate.now().plusDays(5).toString()).param("category", "SCHOOL")
            .param("seated", "true").param("rowNames", "A", "B").param("rowCounts", "10", "15");
    }

    private org.springframework.test.web.servlet.ResultActions signup(String id, String email, String pw, String pw2, String role) throws Exception {
        return mvc.perform(post("/signup").with(csrf()).param("loginId", id).param("email", email).param("password", pw)
            .param("passwordConfirm", pw2).param("name", "새회원").param("phone", "01099998888").param("role", role));
    }

    private org.springframework.test.web.servlet.ResultActions reserve(Member m, Concert c, String count, String... seats) throws Exception {
        var req = post("/reserve").with(csrf()).with(as(m)).param("concertId", c.getId().toString());
        if (count != null) req.param("seatCount", count);
        if (seats != null) req.param("seats", seats);
        return mvc.perform(req);
    }

    @Test
    void securityLogCleansUserInput() {
        // 아이디 칸에 줄바꿈을 넣어 가짜 기록 줄을 만드는 공격을 막는다
        assertThat(com.yuhancon.security.SecurityLog.clean("evil\nLOGIN_SUCCESS id=admin01")).doesNotContain("\n");
        assertThat(com.yuhancon.security.SecurityLog.clean("x".repeat(100))).hasSize(41);
        assertThat(com.yuhancon.security.SecurityLog.clean(null)).isEqualTo("-");
    }
}
