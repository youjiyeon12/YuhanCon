package com.yuhancon.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import com.yuhancon.domain.Concert;
import com.yuhancon.domain.Member;
import com.yuhancon.domain.Reserve;
import com.yuhancon.domain.ReservedSeat;
import com.yuhancon.repository.ConcertRepository;
import com.yuhancon.repository.ReserveRepository;
import com.yuhancon.repository.ReservedSeatRepository;

import lombok.RequiredArgsConstructor;

// 예매 규칙: 한 공연에 한 사람당 예매 하나, 최대 6석. 좌석 지정 공연은 자리를 골라서, 아니면 매수만 고른다.
// 같은 자리·같은 회원의 중복 예매는 DB 유일 제약이 막는다(DataIntegrityViolationException → 컨트롤러가 안내).
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReserveService {

    public static final int MAX_SEATS_PER_MEMBER = 6;

    private final ReserveRepository reserveRepository;
    private final ReservedSeatRepository seatRepository;
    private final ConcertRepository concertRepository;
    private final ConcertService concertService;

    public Optional<Reserve> find(Member member, Concert concert) {
        return member == null ? Optional.empty() : reserveRepository.findByMemberAndConcert(member, concert);
    }

    public List<Reserve> mine(Member member) {
        return reserveRepository.findByMemberWithConcert(member);
    }

    public Set<String> takenSeats(Long concertId) {
        return new HashSet<>(seatRepository.findLabelsByConcert(concertId));
    }

    public Reserve getOwn(Long id, Member member) {
        return checkOwn(reserveRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), member);
    }

    // 주의: 이 요청 안에서 잠그기 전에 같은 예매를 먼저 읽으면(getOwn 등) 영속성 컨텍스트의 옛 객체가 돌아와 잠금 효과가 없다
    private Reserve lockOwn(Long id, Member member) {
        return checkOwn(reserveRepository.findForUpdate(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND)), member);
    }

    private Reserve checkOwn(Reserve r, Member member) {
        if (!r.getMember().getLoginId().equals(member.getLoginId())) throw new AccessDeniedException("본인 예매만 볼 수 있습니다.");
        return r;
    }

    @Transactional
    public Reserve reserve(Long concertId, int seatCount, List<String> seats, Member member) {
        Concert c = concertService.getVisible(concertId, member);
        checkOpen(c);
        if (reserveRepository.findByMemberAndConcert(member, c).isPresent()) {
            throw new UserFacingException("이미 예매한 공연입니다. 마이페이지에서 예매를 수정하거나 취소해 주세요.");
        }
        List<String> picked = normalize(c, seatCount, seats);
        if (concertRepository.decreaseAvailableSeats(c.getId(), picked.size() == 0 ? seatCount : picked.size()) == 0) {
            throw new UserFacingException("남은 좌석이 부족합니다.");
        }
        Reserve r = new Reserve();
        r.setMember(member);
        r.setConcert(c);
        r.setSeatCount(c.isSeated() ? picked.size() : seatCount);
        r.setSeats(c.isSeated() ? String.join(",", picked) : null);
        r.setReservedAt(LocalDateTime.now());
        reserveRepository.save(r);
        saveSeats(c, r, picked);
        reserveRepository.flush();
        return r;
    }

    @Transactional
    public Reserve update(Long reserveId, int seatCount, List<String> seats, Member member) {
        Reserve r = lockOwn(reserveId, member);
        Concert c = r.getConcert();
        checkOpen(c);
        if (c.isSoldOut()) throw new UserFacingException("남은 좌석이 없어 예매를 수정할 수 없습니다. 취소만 할 수 있습니다.");
        List<String> picked = normalize(c, seatCount, seats);
        int newCount = c.isSeated() ? picked.size() : seatCount;
        int diff = newCount - r.getSeatCount();
        if (diff > 0 && concertRepository.decreaseAvailableSeats(c.getId(), diff) == 0) {
            throw new UserFacingException("남은 좌석이 부족합니다.");
        }
        if (diff < 0) concertRepository.increaseAvailableSeats(c.getId(), -diff);
        seatRepository.deleteByReserveId(r.getId());
        seatRepository.flush();
        r.setSeatCount(newCount);
        r.setSeats(c.isSeated() ? String.join(",", picked) : null);
        saveSeats(c, r, picked);
        reserveRepository.flush();
        return r;
    }

    @Transactional
    public Concert cancel(Long reserveId, Member member) {
        // 탭 두 개에서 거의 동시에 취소하면 두 번째는 행이 이미 없다. 오류 화면 대신 안내로 보낸다.
        Reserve r = checkOwn(reserveRepository.findForUpdate(reserveId)
            .orElseThrow(() -> new UserFacingException("이미 취소된 예매입니다.")), member);
        Concert c = r.getConcert();
        if (c.isPast()) throw new UserFacingException("이미 끝난 공연의 예매는 취소할 수 없습니다.");
        seatRepository.deleteByReserveId(r.getId());
        // 실제로 지웠을 때만 좌석을 되돌린다(동시에 두 번 취소해도 한 번만)
        if (reserveRepository.deleteReserveById(r.getId()) == 1) {
            concertRepository.increaseAvailableSeats(c.getId(), r.getSeatCount());
        }
        return c;
    }

    private void checkOpen(Concert c) {
        if (!c.isApproved()) throw new UserFacingException("승인 전 공연은 예매할 수 없습니다.");
        if (c.isPast()) throw new UserFacingException("이미 끝난 공연은 예매할 수 없습니다.");
    }

    // 좌석 지정 공연이면 고른 좌석 목록(검사 후), 아니면 빈 목록. 화면 제한은 우회될 수 있으니 서버에서 다시 본다.
    private List<String> normalize(Concert c, int seatCount, List<String> seats) {
        if (!c.isSeated()) {
            if (seatCount < 1 || seatCount > MAX_SEATS_PER_MEMBER) {
                throw new UserFacingException("예매 매수는 1~" + MAX_SEATS_PER_MEMBER + "석입니다.");
            }
            return List.of();
        }
        Set<String> picked = new LinkedHashSet<>();
        if (seats != null) seats.forEach(s -> picked.add(s.strip().toUpperCase()));
        if (picked.isEmpty()) throw new UserFacingException("좌석을 골라 주세요.");
        if (picked.size() > MAX_SEATS_PER_MEMBER) throw new UserFacingException("한 사람당 최대 " + MAX_SEATS_PER_MEMBER + "석까지 고를 수 있습니다.");
        if (!new HashSet<>(c.getSeatLabels()).containsAll(picked)) throw new UserFacingException("없는 좌석이 포함되어 있습니다.");
        return new ArrayList<>(picked);
    }

    private void saveSeats(Concert c, Reserve r, List<String> labels) {
        for (String label : labels) {
            ReservedSeat s = new ReservedSeat();
            s.setConcert(c);
            s.setReserve(r);
            s.setLabel(label);
            seatRepository.save(s);
        }
    }
}
