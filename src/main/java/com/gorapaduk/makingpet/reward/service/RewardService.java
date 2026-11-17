package com.gorapaduk.makingpet.reward.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.common.exception.RetryAfterException;
import com.gorapaduk.makingpet.common.util.KstTime;
import com.gorapaduk.makingpet.reward.domain.DailyCounter;
import com.gorapaduk.makingpet.reward.domain.RewardClaim;
import com.gorapaduk.makingpet.reward.domain.SlotMachine;
import com.gorapaduk.makingpet.reward.dto.RewardDtos;
import com.gorapaduk.makingpet.reward.repository.DailyCounterRepository;
import com.gorapaduk.makingpet.reward.repository.RewardClaimRepository;
import com.gorapaduk.makingpet.wallet.domain.TransactionReason;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;
import com.gorapaduk.makingpet.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 규칙 §5. 모든 결과는 서버가 정하고, 보상 지급은 기록과 같은 트랜잭션. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RewardService {

    public static final int ATTENDANCE_COINS = 30;
    public static final int ATTENDANCE_BONUS_COINS = 100;
    public static final int ATTENDANCE_BONUS_DAY = 7;
    public static final int RPS_WIN = 50, RPS_DRAW = 20, RPS_LOSE = 10;
    private static final String SLOT_COUNTER = "SLOT";

    private final RewardClaimRepository claims;
    private final DailyCounterRepository counters;
    private final WalletService walletService;
    private final SlotMachine slotMachine = new SlotMachine();
    private final SecureRandom random = new SecureRandom();

    /** R-1 (M-5 의 today 에도 쓰인다) */
    public RewardDtos.Today today(UUID memberId) {
        LocalDate today = KstTime.today();
        var attendance = claims.findByMemberIdAndTypeAndRewardDate(memberId, RewardClaim.Type.ATTENDANCE, today);
        int streak = attendance.map(RewardClaim::getStreak).orElseGet(() -> currentStreak(memberId, today));
        boolean rps = claims.existsByMemberIdAndTypeAndRewardDate(memberId, RewardClaim.Type.RPS, today);
        int used = counters.findById(new DailyCounter.Key(memberId, today, SLOT_COUNTER)).map(DailyCounter::getCount).orElse(0);
        int nextBonusIn = ATTENDANCE_BONUS_DAY - (streak % ATTENDANCE_BONUS_DAY);   // 7일째 당일이면 7
        return new RewardDtos.Today(today,
                new RewardDtos.Today.Attendance(attendance.isPresent(), streak, nextBonusIn),
                new RewardDtos.Today.Rps(rps),
                new RewardDtos.Today.Slot(used, SlotMachine.DAILY_LIMIT, Math.max(0, SlotMachine.DAILY_LIMIT - used), SlotMachine.ENTRY_FEE),
                KstTime.nextMidnight().toLocalDateTime());
    }

    /** R-2 출석. 기록을 먼저 넣고(유니크로 중복 차단) 코인을 준다. */
    @Transactional
    public RewardDtos.AttendanceResponse attend(UUID memberId) {
        LocalDate today = KstTime.today();
        // 이미 받았으면 INSERT 를 시도하지 않는다. 유니크 위반으로 flush 가 실패하면 영속성 컨텍스트가 오염되어
        // 같은 트랜잭션의 다음 flush 가 깨진다. 아래 유니크 제약 catch 는 동시 요청 경합용 안전망으로만 남긴다.
        if (claims.existsByMemberIdAndTypeAndRewardDate(memberId, RewardClaim.Type.ATTENDANCE, today)) {
            throw new BusinessException(ErrorCode.REWARD_ALREADY_CLAIMED);
        }
        int streak = currentStreak(memberId, today) + 1;            // 어제 출석했으면 이어가고, 아니면 1
        int bonus = streak % ATTENDANCE_BONUS_DAY == 0 ? ATTENDANCE_BONUS_COINS : 0;
        int coins = ATTENDANCE_COINS + bonus;
        RewardClaim claim = RewardClaim.of(memberId, RewardClaim.Type.ATTENDANCE, today, 0, streak, null, coins);
        try {
            claims.saveAndFlush(claim);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.REWARD_ALREADY_CLAIMED);
        }
        WalletResponse wallet = walletService.credit(memberId, coins, TransactionReason.ATTENDANCE, "REWARD_CLAIM", claim.getId());
        return new RewardDtos.AttendanceResponse(today, streak, ATTENDANCE_COINS, bonus, wallet);
    }

    /** R-3 */
    public RewardDtos.AttendanceCalendar calendar(UUID memberId, YearMonth month) {
        List<LocalDate> days = claims.findByMemberIdAndTypeAndRewardDateBetweenOrderByRewardDateAsc(
                        memberId, RewardClaim.Type.ATTENDANCE, month.atDay(1), month.atEndOfMonth())
                .stream().map(RewardClaim::getRewardDate).toList();
        LocalDate today = KstTime.today();
        int streak = claims.findByMemberIdAndTypeAndRewardDate(memberId, RewardClaim.Type.ATTENDANCE, today)
                .map(RewardClaim::getStreak).orElseGet(() -> currentStreak(memberId, today));
        return new RewardDtos.AttendanceCalendar(month.toString(), days, streak, days.size());
    }

    /** R-4 가위바위보 */
    @Transactional
    public RewardDtos.RpsResponse rps(UUID memberId, RewardDtos.Hand hand) {
        LocalDate today = KstTime.today();
        // 이미 받았으면 INSERT 를 시도하지 않는다. 유니크 위반으로 flush 가 실패하면 영속성 컨텍스트가 오염되어
        // 같은 트랜잭션의 다음 flush 가 깨진다. 아래 유니크 제약 catch 는 동시 요청 경합용 안전망으로만 남긴다.
        if (claims.existsByMemberIdAndTypeAndRewardDate(memberId, RewardClaim.Type.RPS, today)) {
            throw new BusinessException(ErrorCode.REWARD_ALREADY_CLAIMED);
        }
        RewardDtos.Hand opponent = RewardDtos.Hand.values()[random.nextInt(3)];
        RewardDtos.RpsResult result = judge(hand, opponent);
        int coins = switch (result) { case WIN -> RPS_WIN; case DRAW -> RPS_DRAW; case LOSE -> RPS_LOSE; };
        RewardClaim claim = RewardClaim.of(memberId, RewardClaim.Type.RPS, today, 0, 0, hand + ">" + opponent + ":" + result, coins);
        try {
            claims.saveAndFlush(claim);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.REWARD_ALREADY_CLAIMED);
        }
        WalletResponse wallet = walletService.credit(memberId, coins, TransactionReason.RPS, "REWARD_CLAIM", claim.getId());
        return new RewardDtos.RpsResponse(hand, opponent, result, coins, wallet);
    }

    static RewardDtos.RpsResult judge(RewardDtos.Hand me, RewardDtos.Hand other) {
        if (me == other) return RewardDtos.RpsResult.DRAW;
        boolean win = (me == RewardDtos.Hand.ROCK && other == RewardDtos.Hand.SCISSORS)
                || (me == RewardDtos.Hand.PAPER && other == RewardDtos.Hand.ROCK)
                || (me == RewardDtos.Hand.SCISSORS && other == RewardDtos.Hand.PAPER);
        return win ? RewardDtos.RpsResult.WIN : RewardDtos.RpsResult.LOSE;
    }

    /** R-5 슬롯. 카운터 행 잠금·증가 → 참가비 → 추첨 → 배당 → 기록. 한 트랜잭션. */
    @Transactional
    public RewardDtos.SlotResponse slot(UUID memberId) {
        LocalDate today = KstTime.today();
        DailyCounter counter = counters.findForUpdate(memberId, today, SLOT_COUNTER)
                .orElseGet(() -> counters.saveAndFlush(DailyCounter.zero(memberId, today, SLOT_COUNTER)));
        if (counter.getCount() >= SlotMachine.DAILY_LIMIT) {
            throw new RetryAfterException(ErrorCode.REWARD_DAILY_LIMIT_EXCEEDED, KstTime.secondsUntilMidnight(),
                    "오늘 슬롯 횟수를 모두 사용했습니다. 자정에 다시 열려요.");
        }
        counter.increment();
        UUID claimId = UUID.randomUUID();
        WalletResponse wallet = walletService.debit(memberId, SlotMachine.ENTRY_FEE, TransactionReason.SLOT_ENTRY, "REWARD_CLAIM", claimId);
        SlotMachine.Spin spin = slotMachine.spin();
        int payout = spin.tier().getPayout();
        if (payout > 0) {
            wallet = walletService.credit(memberId, payout, TransactionReason.SLOT_PAYOUT, "REWARD_CLAIM", claimId);
        }
        String detail = String.join(",", spin.reels().stream().map(Enum::name).toList()) + ":" + spin.tier();
        claims.save(RewardClaim.of(memberId, RewardClaim.Type.SLOT, today, counter.getCount(), 0, detail, payout - SlotMachine.ENTRY_FEE));
        return new RewardDtos.SlotResponse(spin.reels(), spin.tier(), SlotMachine.ENTRY_FEE, payout,
                payout - SlotMachine.ENTRY_FEE, SlotMachine.DAILY_LIMIT - counter.getCount(), wallet);
    }

    /** 오늘 출석 전 기준의 연속일: 어제 출석 기록의 streak (7일째였다면 0 으로 순환). */
    private int currentStreak(UUID memberId, LocalDate today) {
        return claims.findByMemberIdAndTypeAndRewardDate(memberId, RewardClaim.Type.ATTENDANCE, today.minusDays(1))
                .map(c -> c.getStreak() % ATTENDANCE_BONUS_DAY == 0 ? 0 : c.getStreak())
                .orElse(0);
    }
}
