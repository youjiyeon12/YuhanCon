package com.yuhancon.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import com.yuhancon.domain.Category;
import com.yuhancon.domain.Concert;
import com.yuhancon.domain.ConcertLike;
import com.yuhancon.domain.ConcertStatus;
import com.yuhancon.domain.Member;
import com.yuhancon.domain.RecentConcertView;
import com.yuhancon.repository.ConcertLikeRepository;
import com.yuhancon.repository.ConcertRepository;
import com.yuhancon.repository.RecentConcertViewRepository;
import com.yuhancon.repository.ReserveRepository;
import com.yuhancon.repository.ReservedSeatRepository;
import com.yuhancon.web.ConcertForm;

import com.yuhancon.security.SecurityLog;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ConcertService {

    public static final int MAX_TOTAL_SEATS = 100000;
    public static final int HOME_LIMIT = 4;   // 홈 '다가오는 공연'은 분류마다 최대 4개
    public static final int LIST_LIMIT = 8;   // 공연 정보 목록은 분류·정렬마다 최대 8개
    public static final int MAX_SLIDES = 6;
    public static final int MIN_SLIDES = 4;
    public static final int MAX_ROWS = 26;
    public static final int MAX_SEATS_PER_ROW = 50;

    private final ConcertRepository concertRepository;
    private final ReserveRepository reserveRepository;
    private final ReservedSeatRepository reservedSeatRepository;
    private final ConcertLikeRepository likeRepository;
    private final RecentConcertViewRepository recentRepository;
    private final ImageStorage imageStorage;

    // ---------- 조회 ----------

    public List<Concert> upcoming(Category category, String sort) {
        return upcoming(category, sort, LIST_LIMIT);
    }

    public List<Concert> upcoming(Category category, String sort, int limit) {
        return concertRepository.findUpcoming(LocalDate.now(), category, sortOf(sort), org.springframework.data.domain.Limit.of(limit));
    }

    // 홈: 전체와 분류마다 가까운 공연 최대 4개씩(분류 칩을 누르면 그 묶음을 보여 준다). 키는 "ALL" = 전체, 나머지는 분류 이름.
    public Map<String, List<Concert>> homeGroups() {
        Map<String, List<Concert>> groups = new LinkedHashMap<>();
        groups.put("ALL", upcoming(null, "date", HOME_LIMIT));
        for (Category cat : Category.values()) groups.put(cat.name(), upcoming(cat, "date", HOME_LIMIT));
        return groups;
    }

    static Sort sortOf(String sort) {
        if (sort == null) sort = "date";
        return switch (sort) {
            case "latest" -> Sort.by(Sort.Direction.DESC, "createdAt");
            case "oldest" -> Sort.by(Sort.Direction.ASC, "createdAt");
            case "likes" -> Sort.by(Sort.Order.desc("likeCount"), Sort.Order.asc("date"));
            default -> Sort.by(Sort.Order.asc("date"), Sort.Order.asc("id"));
        };
    }

    public List<Concert> pastTop8() {
        return concertRepository.findTop8ByStatusAndDateLessThanOrderByDateDesc(ConcertStatus.APPROVED, LocalDate.now());
    }

    // 오늘부터 7일간 날짜별 공연(달력 주로 자르면 일요일엔 하루만 남아서 오늘 기준으로 센다)
    public Map<LocalDate, List<Concert>> thisWeek() {
        LocalDate start = LocalDate.now();
        Map<LocalDate, List<Concert>> week = new LinkedHashMap<>();
        for (int i = 0; i < 7; i++) week.put(start.plusDays(i), new ArrayList<>());
        concertRepository.findByStatusAndDateBetweenOrderByDateAsc(ConcertStatus.APPROVED, start, start.plusDays(6))
            .forEach(c -> week.get(c.getDate()).add(c));
        return week;
    }

    // 홈 슬라이드: 관리자가 고른 공연 + 4개가 안 되면 가장 가까운 공연 포스터로 채운다(끝난 공연은 자동으로 빠진다)
    public List<Concert> slides() {
        LocalDate today = LocalDate.now();
        List<Concert> slides = new ArrayList<>(concertRepository
            .findByStatusAndDateGreaterThanEqualAndSlideOrderIsNotNullAndImageIsNotNullOrderBySlideOrderAsc(ConcertStatus.APPROVED, today));
        if (slides.size() < MIN_SLIDES) {
            for (Concert c : concertRepository.findByStatusAndDateGreaterThanEqualAndImageIsNotNullOrderByDateAsc(ConcertStatus.APPROVED, today)) {
                if (slides.size() >= MIN_SLIDES) break;
                if (!slides.contains(c)) slides.add(c);
            }
        }
        return slides;
    }

    public List<Concert> slideCandidates() {
        return concertRepository.findByStatusAndDateGreaterThanEqualAndImageIsNotNullOrderByDateAsc(ConcertStatus.APPROVED, LocalDate.now());
    }

    public List<Concert> pending() {
        return concertRepository.findByStatusOrderByCreatedAtAsc(ConcertStatus.PENDING);
    }

    public List<Concert> all() {
        return concertRepository.findAllByOrderByDateDesc();
    }

    public List<Concert> mine(Member member) {
        return concertRepository.findByMemberOrderByCreatedAtDesc(member);
    }

    public Concert get(Long id) {
        return concertRepository.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    // 승인 전 공연은 등록자와 관리자만 볼 수 있다. 다른 사람에게는 '없는 공연'으로 보인다.
    public Concert getVisible(Long id, Member viewer) {
        Concert c = get(id);
        if (!c.isApproved() && !(viewer != null && (viewer.isAdmin() || isOwner(c, viewer)))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return c;
    }

    public static boolean isOwner(Concert c, Member m) {
        return m != null && c.getMember().getLoginId().equals(m.getLoginId());
    }

    public static boolean canEdit(Concert c, Member m) {
        return m != null && (m.isAdmin() || (m.canManageConcerts() && isOwner(c, m)));
    }

    public Concert getEditable(Long id, Member m) {
        return checkEditable(get(id), m);
    }

    private Concert checkEditable(Concert c, Member m) {
        if (!canEdit(c, m)) throw new AccessDeniedException("본인이 등록한 공연만 고칠 수 있습니다.");
        return c;
    }

    public int reservedSeats(Long concertId) {
        return reserveRepository.sumSeatsByConcert(concertId);
    }

    public Set<Long> likedIds(Member member) {
        return member == null ? Set.of() : likeRepository.findConcertIdsByMember(member);
    }

    public List<ConcertLike> likes(Member member) {
        return likeRepository.findByMemberWithConcert(member);
    }

    public List<RecentConcertView> recent(Member member) {
        return recentRepository.findRecentWithConcert(member, PageRequest.of(0, 5));
    }

    // ---------- 변경 ----------

    @Transactional
    public void recordView(Concert concert, Member member) {
        recentRepository.deleteByMemberAndConcert(member, concert);
        RecentConcertView v = new RecentConcertView();
        v.setMember(member);
        v.setConcert(concert);
        v.setViewedAt(LocalDateTime.now());
        recentRepository.save(v);
    }

    @Transactional
    public Concert create(ConcertForm form, MultipartFile image, Member member) {
        if (!member.canManageConcerts()) throw new AccessDeniedException("공연 등록자만 공연을 등록할 수 있습니다.");
        Concert c = new Concert();
        apply(form, c, null, 0);
        c.setAvailableSeats(c.getTotalSeats());
        c.setMember(member);
        c.setCreatedAt(LocalDateTime.now());
        // 관리자가 직접 올린 공연은 바로 공개, 공연 등록자가 올리면 승인 대기
        c.setStatus(member.isAdmin() ? ConcertStatus.APPROVED : ConcertStatus.PENDING);
        String saved = imageStorage.save(image);
        c.setImage(saved);
        try {
            return concertRepository.saveAndFlush(c);
        } catch (RuntimeException e) {
            imageStorage.delete(saved); // DB 저장에 실패하면 방금 올린 파일도 지운다
            throw e;
        }
    }

    @Transactional
    public Concert update(Long id, ConcertForm form, MultipartFile image, Member member) {
        // 포스터 파일(최대 10MB)은 잠그기 전에 저장한다. 잠근 채 오래 걸리면 그동안 들어온 예매가 잠금 대기 시간을 넘겨 실패한다.
        String saved = imageStorage.save(image);
        try {
            // 행을 잠근 뒤 예매 합계를 세야 그 사이 들어온 예매가 남은 좌석 계산에서 빠지지 않는다.
            // 주의: 이 요청 안에서 잠그기 전에 같은 공연을 먼저 읽으면(getEditable 등) 영속성 컨텍스트의 옛 객체가 돌아와 잠금 효과가 없다.
            Concert c = checkEditable(concertRepository.findForUpdate(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), member);
            int reserved = reserveRepository.sumSeatsByConcert(id);
            String oldLayout = c.getSeatLayout();
            apply(form, c, c.getDate(), reserved);
            // 이미 팔린 좌석 이름이 사라지면 안 되므로, 예매가 있으면 좌석 배치를 바꾸지 못한다
            if (reserved > 0 && !java.util.Objects.equals(oldLayout, c.getSeatLayout())) {
                throw new UserFacingException("예매가 있는 공연은 좌석 배치를 바꿀 수 없습니다.");
            }
            c.setAvailableSeats(c.getTotalSeats() - reserved);
            if (saved != null) {
                imageStorage.delete(c.getImage());
                c.setImage(saved);
            }
            return c;
        } catch (RuntimeException e) {
            imageStorage.delete(saved); // 수정이 실패하면 방금 올린 파일도 지운다
            throw e;
        }
    }

    // 등록자는 예매가 없을 때만 삭제, 관리자는 예매가 있어도 삭제(예매·좌석·좋아요 기록도 함께 지운다)
    @Transactional
    public void delete(Long id, Member member) {
        Concert c = getEditable(id, member);
        if (!member.isAdmin() && reserveRepository.sumSeatsByConcert(id) > 0) {
            throw new UserFacingException("예매한 사람이 있는 공연은 삭제할 수 없습니다. 관리자에게 문의하세요.");
        }
        reservedSeatRepository.deleteByConcertId(id);
        reserveRepository.deleteByConcertId(id);
        likeRepository.deleteByConcertId(id);
        recentRepository.deleteByConcertId(id);
        concertRepository.delete(c);
        imageStorage.delete(c.getImage());
        if (member.isAdmin()) SecurityLog.LOG.info("ADMIN_CONCERT_DELETE by={} concert={} title={}", member.getLoginId(), id, SecurityLog.clean(c.getTitle()));
    }

    @Transactional
    public int approve(Collection<Long> ids, Member admin) {
        requireAdmin(admin);
        if (ids == null || ids.isEmpty()) throw new UserFacingException("승인할 공연을 고르세요.");
        int n = concertRepository.approveAll(ids);
        SecurityLog.LOG.info("ADMIN_APPROVE by={} concerts={} approved={}", admin.getLoginId(), ids, n);
        return n;
    }

    @Transactional
    public void setSlides(List<Long> ids, Member admin) {
        requireAdmin(admin);
        List<Long> picked = ids == null ? List.of() : ids.stream().distinct().toList();
        if (picked.size() > MAX_SLIDES) throw new UserFacingException("슬라이드는 최대 " + MAX_SLIDES + "개까지 고를 수 있습니다.");
        Set<Long> allowed = new HashSet<>();
        slideCandidates().forEach(c -> allowed.add(c.getId()));
        if (!allowed.containsAll(picked)) throw new UserFacingException("포스터가 있고 끝나지 않은 승인된 공연만 고를 수 있습니다.");
        concertRepository.clearSlides();
        for (int i = 0; i < picked.size(); i++) get(picked.get(i)).setSlideOrder(i + 1);
        SecurityLog.LOG.info("ADMIN_SLIDES by={} order={}", admin.getLoginId(), picked);
    }

    // 좋아요를 누르면 켜고, 다시 누르면 끈다. 켜졌으면 true
    @Transactional
    public boolean toggleLike(Long id, Member member) {
        Concert c = getVisible(id, member);
        if (likeRepository.deleteByMemberAndConcert(member, c) > 0) {
            concertRepository.addLikeCount(id, -1);
            return false;
        }
        ConcertLike like = new ConcertLike();
        like.setMember(member);
        like.setConcert(c);
        like.setLikedAt(LocalDateTime.now());
        likeRepository.saveAndFlush(like);
        concertRepository.addLikeCount(id, 1);
        return true;
    }

    private void requireAdmin(Member m) {
        if (m == null || !m.isAdmin()) throw new AccessDeniedException("관리자만 할 수 있습니다.");
    }

    // 폼 값을 검사해 공연에 옮긴다. unchangedDate와 같은 날짜면 지난 날짜여도 통과(지난 공연 수정용)
    private void apply(ConcertForm f, Concert c, LocalDate unchangedDate, int reserved) {
        String title = strip(f.getTitle()), place = strip(f.getPlace()), content = f.getContent() == null ? "" : f.getContent().replace("\r\n", "\n").strip();
        if (title.isEmpty()) throw new UserFacingException("공연 제목을 입력하세요.");
        if (title.length() > 100) throw new UserFacingException("공연 제목은 100자 이하로 입력하세요.");
        if (place.isEmpty()) throw new UserFacingException("공연 장소를 입력하세요.");
        if (place.length() > 100) throw new UserFacingException("공연 장소는 100자 이하로 입력하세요.");
        if (content.isEmpty()) throw new UserFacingException("공연 소개를 입력하세요.");
        if (content.length() > 5000) throw new UserFacingException("공연 소개는 5000자 이하로 입력하세요.");
        if (f.getDate() == null) throw new UserFacingException("공연 날짜를 입력하세요.");
        if (f.getDate().isBefore(LocalDate.now()) && !f.getDate().equals(unchangedDate)) throw new UserFacingException("공연 날짜는 오늘 이후로 입력하세요.");
        if (f.getCategory() == null) throw new UserFacingException("공연 분류를 고르세요.");
        String etc = strip(f.getCategoryEtc());
        if (f.getCategory() == Category.ETC && etc.isEmpty()) throw new UserFacingException("분류가 '기타'면 분류 이름을 입력하세요.");
        if (etc.length() > 30) throw new UserFacingException("분류 이름은 30자 이하로 입력하세요.");

        String layout = null;
        int total;
        if (f.isSeated()) {
            layout = buildLayout(f.getRowNames(), f.getRowCounts());
            total = java.util.Arrays.stream(layout.split(",")).mapToInt(p -> Integer.parseInt(p.split(":")[1])).sum();
        } else {
            total = f.getTotalSeats() == null ? 0 : f.getTotalSeats();
            if (total < 1 || total > MAX_TOTAL_SEATS) throw new UserFacingException("총 인원은 1~" + MAX_TOTAL_SEATS + "명으로 입력하세요.");
        }
        if (total < reserved) throw new UserFacingException("이미 예매된 좌석(" + reserved + "석)보다 적게 줄일 수 없습니다.");

        c.setTitle(title);
        c.setPlace(place);
        c.setContent(content);
        c.setDate(f.getDate());
        c.setCategory(f.getCategory());
        c.setCategoryEtc(f.getCategory() == Category.ETC ? etc : null);
        c.setSeatLayout(layout);
        c.setTotalSeats(total);
    }

    // 열 이름(영문·숫자 1~3자, 중복 불가)과 좌석 수(1~50)를 "A:10,B:20"으로 만든다
    static String buildLayout(List<String> names, List<Integer> counts) {
        List<String> parts = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        int n = Math.min(names == null ? 0 : names.size(), counts == null ? 0 : counts.size());
        for (int i = 0; i < n; i++) {
            String name = strip(names.get(i)).toUpperCase();
            Integer count = counts.get(i);
            if (name.isEmpty() && count == null) continue; // 비워 둔 줄은 건너뛴다
            if (!name.matches("^[A-Z0-9]{1,3}$")) throw new UserFacingException("좌석 열 이름은 영문·숫자 1~3자로 입력하세요. (예: A, B, VIP)");
            if (!seen.add(name)) throw new UserFacingException("좌석 열 이름 '" + name + "'이 겹칩니다.");
            if (count == null || count < 1 || count > MAX_SEATS_PER_ROW) throw new UserFacingException("한 열의 좌석 수는 1~" + MAX_SEATS_PER_ROW + "석으로 입력하세요.");
            parts.add(name + ":" + count);
        }
        if (parts.isEmpty()) throw new UserFacingException("좌석 지정 공연이면 좌석 열을 하나 이상 입력하세요.");
        if (parts.size() > MAX_ROWS) throw new UserFacingException("좌석 열은 최대 " + MAX_ROWS + "개까지 입력할 수 있습니다.");
        return String.join(",", parts);
    }

    private static String strip(String s) {
        return s == null ? "" : s.strip();
    }
}
