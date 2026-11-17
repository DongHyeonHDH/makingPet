package com.gorapaduk.makingpet.reward.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.gorapaduk.makingpet.reward.domain.SlotMachine;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;
import jakarta.validation.constraints.NotNull;

public final class RewardDtos {

    private RewardDtos() {}

    /** R-1 */
    public record Today(LocalDate date, Attendance attendance, Rps rps, Slot slot, LocalDateTime resetsAt) {
        public record Attendance(boolean claimed, int streak, int nextBonusIn) {}
        public record Rps(boolean claimed) {}
        public record Slot(int used, int limit, int remaining, int entryFee) {}
    }

    /** R-2 */
    public record AttendanceResponse(LocalDate date, int streak, int coins, int bonusCoins, WalletResponse wallet) {}

    /** R-3 */
    public record AttendanceCalendar(String month, List<LocalDate> days, int streak, int totalThisMonth) {}

    /** R-4 */
    public enum Hand { ROCK, PAPER, SCISSORS }

    public record RpsRequest(@NotNull Hand hand) {}

    public enum RpsResult { WIN, DRAW, LOSE }

    public record RpsResponse(Hand myHand, Hand opponentHand, RpsResult result, int coins, WalletResponse wallet) {}

    /** R-5 */
    public record SlotResponse(List<SlotMachine.Symbol> reels, SlotMachine.Tier tier, int entryFee, int payout, int net,
                               int remaining, WalletResponse wallet) {}
}
