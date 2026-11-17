package com.gorapaduk.makingpet.gacha.service;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.constants.Rarity;
import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.common.util.PageRequests;
import com.gorapaduk.makingpet.gacha.domain.GachaDraw;
import com.gorapaduk.makingpet.gacha.domain.GachaPityCounter;
import com.gorapaduk.makingpet.gacha.domain.GachaPool;
import com.gorapaduk.makingpet.gacha.domain.GachaPoolEntry;
import com.gorapaduk.makingpet.gacha.dto.GachaDtos;
import com.gorapaduk.makingpet.gacha.repository.GachaDrawRepository;
import com.gorapaduk.makingpet.gacha.repository.GachaPityCounterRepository;
import com.gorapaduk.makingpet.gacha.repository.GachaPoolRepository;
import com.gorapaduk.makingpet.member.service.MemberService;
import com.gorapaduk.makingpet.pet.domain.PetSpecies;
import com.gorapaduk.makingpet.pet.domain.UserPet;
import com.gorapaduk.makingpet.pet.dto.SpeciesSummary;
import com.gorapaduk.makingpet.pet.service.PetService;
import com.gorapaduk.makingpet.pet.service.PetSpeciesService;
import com.gorapaduk.makingpet.wallet.domain.TransactionReason;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;
import com.gorapaduk.makingpet.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 규칙 §1. 서버가 결과를 정한다(SecureRandom). 한 트랜잭션:
 * 코인 차감 → count 회 추첨(천장·10연 보장) → 회차마다 새 펫 또는 환급 → 기록·천장 갱신.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GachaService {

    public static final Rarity MULTI_GUARANTEE = Rarity.RARE;
    public static final Rarity PITY_GUARANTEE = Rarity.EPIC;

    private final GachaPoolRepository pools;
    private final GachaDrawRepository draws;
    private final GachaPityCounterRepository pityCounters;
    private final PetSpeciesService speciesService;
    private final PetService petService;
    private final WalletService walletService;
    private final MemberService memberService;
    private final SecureRandom random = new SecureRandom();

    // ----- G-1 -----

    public List<GachaDtos.PoolResponse> listOpenPools() {
        LocalDateTime now = LocalDateTime.now();
        return pools.findByActiveTrueOrderByCreatedAtAsc().stream()
                .filter(p -> p.isOpenAt(now))
                .map(this::toPoolResponse)
                .toList();
    }

    public GachaDtos.PoolResponse toPoolResponse(GachaPool pool) {
        Map<UUID, PetSpecies> species = speciesMap(pool);
        List<GachaDtos.RarityRate> rates = new ArrayList<>();
        for (Rarity r : Rarity.values()) {
            List<GachaDtos.SpeciesRate> list = pool.getEntries().stream()
                    .filter(e -> species.get(e.getSpeciesId()).getRarity() == r)
                    .map(e -> new GachaDtos.SpeciesRate(e.getSpeciesId(), species.get(e.getSpeciesId()).getCode(),
                            species.get(e.getSpeciesId()).getName(), rate(e.getWeight())))
                    .toList();
            double sum = list.stream().mapToDouble(GachaDtos.SpeciesRate::rate).sum();
            rates.add(new GachaDtos.RarityRate(r, Math.round(sum * 10000) / 10000.0, list));
        }
        return new GachaDtos.PoolResponse(pool.getId(), pool.getName(), pool.getType().name(), pool.getSinglePrice(),
                pool.getMultiPrice(), MULTI_GUARANTEE, pool.getPityThreshold(), PITY_GUARANTEE,
                pool.getStartsAt(), pool.getEndsAt(), rates);
    }

    // ----- G-2 -----

    @Transactional
    public GachaDtos.DrawResponse draw(UUID memberId, UUID poolId, int count) {
        if (count != 1 && count != 10) throw new BusinessException(ErrorCode.VALIDATION_FAILED, "count 는 1 또는 10");
        GachaPool pool = pools.findById(poolId).orElseThrow(() -> new BusinessException(ErrorCode.GACHA_POOL_NOT_FOUND));
        if (!pool.isOpenAt(LocalDateTime.now())) throw new BusinessException(ErrorCode.GACHA_POOL_INACTIVE);

        UUID batchId = UUID.randomUUID();
        int cost = pool.priceFor(count);
        WalletResponse wallet = walletService.debit(memberId, cost, TransactionReason.GACHA, "GACHA_BATCH", batchId);

        GachaPityCounter pity = pityCounters.findForUpdate(memberId, poolId)
                .orElseGet(() -> pityCounters.saveAndFlush(GachaPityCounter.zero(memberId, poolId)));
        Map<UUID, PetSpecies> species = speciesMap(pool);
        Set<UUID> owned = new HashSet<>(petService.ownedSpeciesIds(memberId));
        boolean hadNoPet = owned.isEmpty();

        List<GachaDtos.DrawResult> results = new ArrayList<>();
        int newPets = 0, refunds = 0, refundCoins = 0;
        boolean rareSeen = false;
        int perDrawCost = cost / count;

        for (int order = 1; order <= count; order++) {
            boolean pityHit = pity.getCount() >= pool.getPityThreshold();
            boolean guarantee = count == 10 && order == 10 && !rareSeen;
            Rarity floor = pityHit ? PITY_GUARANTEE : guarantee ? MULTI_GUARANTEE : null;
            GachaPoolEntry picked = pick(pool, species, floor);
            PetSpecies s = species.get(picked.getSpeciesId());
            if (s.getRarity().compareTo(MULTI_GUARANTEE) >= 0) rareSeen = true;
            if (s.getRarity().compareTo(PITY_GUARANTEE) >= 0) pity.reset(); else pity.increment();

            GachaDraw.ResultType type;
            UserPet pet = null;
            int refund = 0;
            if (owned.contains(s.getId())) {
                type = GachaDraw.ResultType.REFUND;
                refund = s.getRarity().getRefundCoins();
                wallet = walletService.credit(memberId, refund, TransactionReason.GACHA_REFUND, "GACHA_BATCH", batchId);
                refunds++;
                refundCoins += refund;
            } else {
                type = GachaDraw.ResultType.NEW_PET;
                pet = petService.create(memberId, s, "GACHA");
                owned.add(s.getId());
                newPets++;
            }
            draws.save(GachaDraw.of(memberId, poolId, batchId, order, s.getId(), s.getRarity(), pityHit, guarantee,
                    type, pet == null ? null : pet.getId(), refund, perDrawCost));

            UUID repId = memberService.representativePetId(memberId);
            if (pet != null && hadNoPet && repId == null) {           // 첫 펫은 자동 대표
                memberService.changeRepresentativePet(memberId, pet.getId());
                repId = pet.getId();
            }
            results.add(new GachaDtos.DrawResult(order, type, SpeciesSummary.from(s),
                    pet == null ? null : petService.summary(pet.getId(), repId), refund, pityHit, guarantee));
        }

        return new GachaDtos.DrawResponse(batchId, poolId, cost, results,
                new GachaDtos.Summary(newPets, refunds, refundCoins),
                new GachaDtos.Pity(pity.getCount(), pool.getPityThreshold()), wallet);
    }

    /** floor 가 있으면 그 희귀도 이상 항목만 후보로 두고 가중치 추첨. */
    private GachaPoolEntry pick(GachaPool pool, Map<UUID, PetSpecies> species, Rarity floor) {
        List<GachaPoolEntry> candidates = pool.getEntries().stream()
                .filter(e -> floor == null || species.get(e.getSpeciesId()).getRarity().compareTo(floor) >= 0)
                .toList();
        if (candidates.isEmpty()) candidates = pool.getEntries();              // 보장 불가능한 풀(AD-6 에서 막지만 방어)
        int total = candidates.stream().mapToInt(GachaPoolEntry::getWeight).sum();
        int roll = random.nextInt(total);
        int acc = 0;
        for (GachaPoolEntry e : candidates) {
            acc += e.getWeight();
            if (roll < acc) return e;
        }
        return candidates.get(candidates.size() - 1);
    }

    // ----- G-3, G-4 -----

    public PageResponse<GachaDtos.DrawRecord> history(UUID memberId, UUID poolId, int page, int size) {
        var pageable = PageRequests.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = poolId == null ? draws.findByMemberId(memberId, pageable) : draws.findByMemberIdAndPoolId(memberId, poolId, pageable);
        Map<UUID, PetSpecies> species = speciesService.getMap(result.getContent().stream().map(GachaDraw::getSpeciesId).toList());
        Map<UUID, String> poolNames = pools.findAllById(result.getContent().stream().map(GachaDraw::getPoolId).collect(Collectors.toSet()))
                .stream().collect(Collectors.toMap(GachaPool::getId, GachaPool::getName));
        return PageResponse.of(result, d -> new GachaDtos.DrawRecord(d.getId(), d.getBatchId(), poolNames.get(d.getPoolId()),
                SpeciesSummary.from(species.get(d.getSpeciesId())), d.getResultType(), d.getRefundCoins(), d.isPityTriggered(), d.getCreatedAt()));
    }

    public GachaDtos.PityResponse pity(UUID memberId, UUID poolId) {
        GachaPool pool = pools.findById(poolId).orElseThrow(() -> new BusinessException(ErrorCode.GACHA_POOL_NOT_FOUND));
        int count = pityCounters.findById(new GachaPityCounter.Key(memberId, poolId)).map(GachaPityCounter::getCount).orElse(0);
        return new GachaDtos.PityResponse(poolId, count, pool.getPityThreshold(), PITY_GUARANTEE);
    }

    // ----- AD-6, AD-7 -----

    @Transactional
    public GachaPool createPool(String name, GachaPool.Type type, int single, int multi, int pityThreshold,
                                LocalDateTime startsAt, LocalDateTime endsAt, Map<UUID, Integer> weights) {
        int sum = weights.values().stream().mapToInt(Integer::intValue).sum();
        if (sum != GachaPool.WEIGHT_TOTAL) throw new BusinessException(ErrorCode.GACHA_WEIGHT_SUM_INVALID);
        Map<UUID, PetSpecies> species = speciesService.getMap(weights.keySet());
        if (species.size() != weights.size()) throw new BusinessException(ErrorCode.PET_SPECIES_NOT_FOUND);
        if (species.values().stream().anyMatch(s -> !s.isActive())) throw new BusinessException(ErrorCode.PET_SPECIES_INACTIVE);
        boolean hasRare = species.values().stream().anyMatch(s -> s.getRarity().compareTo(MULTI_GUARANTEE) >= 0);
        boolean hasEpic = species.values().stream().anyMatch(s -> s.getRarity().compareTo(PITY_GUARANTEE) >= 0);
        if (!hasRare || !hasEpic) throw new BusinessException(ErrorCode.GACHA_POOL_GUARANTEE_IMPOSSIBLE);

        GachaPool pool = GachaPool.create(name, type, single, multi, pityThreshold, startsAt, endsAt);
        weights.forEach(pool::addEntry);
        return pools.save(pool);
    }

    @Transactional
    public GachaPool updatePool(UUID poolId, String name, Boolean active, LocalDateTime startsAt, LocalDateTime endsAt) {
        GachaPool pool = pools.findById(poolId).orElseThrow(() -> new BusinessException(ErrorCode.GACHA_POOL_NOT_FOUND));
        pool.update(name, active, startsAt, endsAt);
        return pool;
    }

    public boolean existsPoolNamed(String name) {
        return pools.findByName(name).isPresent();
    }

    // ----- 내부 -----

    private Map<UUID, PetSpecies> speciesMap(GachaPool pool) {
        return speciesService.getMap(pool.getEntries().stream().map(GachaPoolEntry::getSpeciesId).toList());
    }

    private static double rate(int weight) {
        return Math.round(weight / (double) GachaPool.WEIGHT_TOTAL * 10000) / 10000.0;
    }
}
