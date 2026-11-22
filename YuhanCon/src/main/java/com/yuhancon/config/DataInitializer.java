package com.yuhancon.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.yuhancon.domain.Board;
import com.yuhancon.domain.Category;
import com.yuhancon.domain.Concert;
import com.yuhancon.domain.ConcertStatus;
import com.yuhancon.domain.Member;
import com.yuhancon.domain.Role;
import com.yuhancon.repository.BoardRepository;
import com.yuhancon.repository.ConcertRepository;
import com.yuhancon.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

// 앱 시작 시 1) 관리자 계정이 없으면 만든다 2) yuhancon.demo-data=true이고 공연이 하나도 없으면 시연용 데이터를 넣는다.
// 관리자 비밀번호는 환경변수 YUHANCON_ADMIN_PASSWORD로 정한다. 없으면 무작위로 만들어 로그에 한 번 보여 준다.
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    public static final String ADMIN_ID = "admin01";

    private final MemberRepository memberRepository;
    private final ConcertRepository concertRepository;
    private final BoardRepository boardRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${YUHANCON_ADMIN_PASSWORD:}")
    private String adminPassword;

    @Value("${yuhancon.demo-data:false}")
    private boolean demoData;

    @Value("${yuhancon.demo-password:}")
    private String demoPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!memberRepository.existsByRole(Role.ADMIN)) {
            String pw = adminPassword.isBlank() ? UUID.randomUUID().toString().substring(0, 12) + "a1" : adminPassword;
            memberRepository.save(member(ADMIN_ID, "관리자", "admin@yuhancon.local", pw, Role.ADMIN));
            if (adminPassword.isBlank()) {
                log.warn("관리자 계정을 만들었습니다. 아이디: {} / 임시 비밀번호: {} (로그인 후 회원정보 수정에서 바꾸세요)", ADMIN_ID, pw);
            }
        }
        if (demoData && !demoPassword.isBlank() && concertRepository.count() == 0) seedDemo();
    }

    private Member member(String id, String name, String email, String rawPw, Role role) {
        Member m = new Member();
        m.setLoginId(id);
        m.setName(name);
        m.setEmail(email);
        m.setPhone("01000000000");
        m.setRole(role);
        m.setPassword(passwordEncoder.encode(rawPw));
        return m;
    }

    private void seedDemo() {
        Member stage = memberRepository.save(member("stage01", "유한무대", "stage01@yuhancon.local", demoPassword, Role.PERFORMER));
        Member fan = memberRepository.save(member("fan0001", "공연팬", "fan0001@yuhancon.local", demoPassword, Role.USER));
        LocalDate t = LocalDate.now();
        concert(stage, "aespa LIVE TOUR – SYNK", "KSPO DOME", t.plusDays(3), Category.CONCERT, null, "A:12,B:12,C:12,D:12,E:12", 0, "/images/aespa.jpg", ConcertStatus.APPROVED, 128);
        concert(stage, "BLACKPINK WORLD TOUR", "고척스카이돔", t.plusDays(5), Category.CONCERT, null, null, 300, "/images/blackpink.jpg", ConcertStatus.APPROVED, 342);
        concert(stage, "유한대 밴드부 정기공연", "유한대학교 대강당", t.plusDays(1), Category.SCHOOL, null, "A:10,B:10,C:10", 0, "/images/concert.jpg", ConcertStatus.APPROVED, 41);
        concert(stage, "싸이 흠뻑쇼 연말 스페셜", "잠실종합운동장", t.plusDays(12), Category.CONCERT, null, null, 1000, "/images/psy.jpg", ConcertStatus.APPROVED, 205);
        concert(stage, "홍대 걷고싶은거리 버스킹", "홍대 걷고싶은거리", t.plusDays(2), Category.BUSKING, null, null, 50, null, ConcertStatus.APPROVED, 19);
        concert(stage, "재즈 동아리 야간 라이브", "유한대 학생회관 1층", t.plusDays(9), Category.ETC, "재즈", null, 40, null, ConcertStatus.APPROVED, 7);
        concert(stage, "어쿠스틱 동아리 첫 공연", "유한대 소극장", t.plusDays(20), Category.SCHOOL, null, "A:8,B:8", 0, null, ConcertStatus.PENDING, 0);
        concert(stage, "신촌 연말 버스킹 페스타", "신촌 스타광장", t.plusDays(25), Category.BUSKING, null, null, 80, null, ConcertStatus.PENDING, 0);
        for (int i = 1; i <= 9; i++) {
            concert(stage, "지난 무대 " + i + "회", "유한대학교 대강당", t.minusDays(i * 7L), i % 2 == 0 ? Category.SCHOOL : Category.BUSKING, null, null, 30, null, ConcertStatus.APPROVED, i);
        }
        Board b = new Board();
        b.setTitle("aespa 공연 같이 가실 분 구해요");
        b.setContent("KSPO DOME 공연 같이 가실 분 찾습니다.\n댓글 대신 마이페이지 연락처로 연락 주세요.");
        b.setMember(fan);
        b.setBoardAt(t);
        boardRepository.save(b);
        log.info("시연용 데이터를 넣었습니다. 공연 등록자 stage01, 일반 사용자 fan0001");
    }

    private void concert(Member owner, String title, String place, LocalDate date, Category cat, String etc,
                         String layout, int freeSeats, String image, ConcertStatus status, int likes) {
        Concert c = new Concert();
        c.setTitle(title);
        c.setPlace(place);
        c.setContent(title + " 공연입니다.\n공연 시작 30분 전부터 입장할 수 있습니다.");
        c.setDate(date);
        c.setCategory(cat);
        c.setCategoryEtc(etc);
        c.setSeatLayout(layout);
        c.setTotalSeats(layout == null ? freeSeats : c.getSeatLabels().size());
        c.setAvailableSeats(c.getTotalSeats());
        c.setImage(image);
        c.setStatus(status);
        c.setLikeCount(likes);
        c.setMember(owner);
        c.setCreatedAt(LocalDateTime.now().minusDays(likes % 10));
        concertRepository.save(c);
    }
}
