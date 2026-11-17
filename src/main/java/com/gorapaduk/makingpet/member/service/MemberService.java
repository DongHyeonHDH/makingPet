package com.gorapaduk.makingpet.member.service;

import java.util.Locale;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.service.InventoryService;
import com.gorapaduk.makingpet.item.service.ItemService;
import com.gorapaduk.makingpet.member.domain.Member;
import com.gorapaduk.makingpet.member.dto.LoginRequest;
import com.gorapaduk.makingpet.member.dto.MemberResponse;
import com.gorapaduk.makingpet.member.dto.NicknameUpdateRequest;
import com.gorapaduk.makingpet.member.dto.RegisterRequest;
import com.gorapaduk.makingpet.member.dto.TokenResponse;
import com.gorapaduk.makingpet.member.repository.MemberRepository;
import com.gorapaduk.makingpet.pet.dto.PetSummary;
import com.gorapaduk.makingpet.pet.service.PetService;
import com.gorapaduk.makingpet.home.service.HomeService;
import com.gorapaduk.makingpet.reward.service.RewardService;
import com.gorapaduk.makingpet.wallet.domain.TransactionReason;
import com.gorapaduk.makingpet.wallet.service.WalletService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

    /** 가입 선물 코인 (규칙 §4-1) */
    public static final long SIGNUP_GIFT_COINS = 300;

    private final MemberRepository members;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final WalletService walletService;
    private final ItemService itemService;
    private final InventoryService inventoryService;
    private final PetService petService;
    private final HomeService homeService;
    private final RewardService rewardService;

    /** 타이밍 공격 완화용: 존재하지 않는 이메일도 같은 시간만큼 해시 비교를 한다. */
    private final String dummyHash;

    public MemberService(MemberRepository members, PasswordEncoder passwordEncoder, TokenService tokenService,
                         WalletService walletService, ItemService itemService, InventoryService inventoryService,
                         PetService petService, HomeService homeService, RewardService rewardService) {
        this.members = members;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.walletService = walletService;
        this.itemService = itemService;
        this.inventoryService = inventoryService;
        this.petService = petService;
        this.homeService = homeService;
        this.rewardService = rewardService;
        this.dummyHash = passwordEncoder.encode(UUID.randomUUID().toString());
    }

    /**
     * M-1. 가입. 같은 트랜잭션에서 지갑 개설 → 가입 선물 300코인 → 기본 테마 1개 지급 → 마이 홈 생성(테마 reserve).
     * 다른 도메인은 service 로만 호출한다.
     */
    @Transactional
    public MemberResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String nickname = request.nickname().strip();
        validateNickname(nickname);
        if (members.existsByEmail(email)) throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
        if (members.existsByNickname(nickname)) throw new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);

        Member member = Member.create(email, nickname, passwordEncoder.encode(request.password()));
        try {
            member = members.saveAndFlush(member);
        } catch (DataIntegrityViolationException e) {           // 동시 가입 경합
            if (members.existsByEmail(email)) throw new BusinessException(ErrorCode.MEMBER_EMAIL_DUPLICATED);
            throw new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }

        walletService.open(member.getId());
        var wallet = walletService.credit(member.getId(), SIGNUP_GIFT_COINS, TransactionReason.SIGNUP_GIFT,
                "MEMBER", member.getId());
        Item defaultTheme = itemService.getByCode(Item.DEFAULT_THEME_CODE);
        inventoryService.grant(member.getId(), defaultTheme.getId(), 1);
        homeService.create(member.getId(), defaultTheme.getId());

        return MemberResponse.from(member, wallet.balance(), null, rewardService.today(member.getId()), 0);
    }

    /** M-2. 로그인. 이메일/비밀번호 어느 쪽이 틀렸는지 구분하지 않는다. */
    public TokenResponse login(LoginRequest request) {
        var found = members.findByEmailAndDeletedAtIsNull(normalizeEmail(request.email()));
        String hash = found.map(Member::getPasswordHash).orElse(dummyHash);
        boolean matches = passwordEncoder.matches(request.password(), hash);
        if (found.isEmpty() || !matches) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        return tokenService.issue(found.get());
    }

    /** M-5 */
    public MemberResponse getMe(UUID memberId) {
        return toResponse(getActive(memberId));
    }

    private MemberResponse toResponse(Member member) {
        UUID repId = member.getRepresentativePetId();
        PetSummary rep = repId == null ? null : petService.summary(repId, repId);
        return MemberResponse.from(member, walletService.getBalance(member.getId()).balance(), rep,
                rewardService.today(member.getId()), (int) petService.countPets(member.getId()));
    }

    /** H-5 방문용. 탈퇴·미존재는 HOME_NOT_FOUND (닉네임 존재 여부를 따로 노출하지 않는다). */
    public Member getByNickname(String nickname) {
        return members.findByNicknameAndDeletedAtIsNull(nickname)
                .orElseThrow(() -> new BusinessException(ErrorCode.HOME_NOT_FOUND));
    }

    // ----- 대표 펫 (pet 도메인이 호출; PetService 는 MemberService 를 import 하지 않는다) -----

    public UUID representativePetId(UUID memberId) {
        return getActive(memberId).getRepresentativePetId();
    }

    @Transactional
    public void changeRepresentativePet(UUID memberId, UUID petId) {
        getActive(memberId).changeRepresentativePet(petId);
    }

    /** M-6 */
    @Transactional
    public MemberResponse changeNickname(UUID memberId, NicknameUpdateRequest request) {
        Member member = getActive(memberId);
        String nickname = request.nickname().strip();
        validateNickname(nickname);
        if (!member.getNickname().equals(nickname) && members.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }
        member.changeNickname(nickname);
        try {
            members.flush();
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.MEMBER_NICKNAME_DUPLICATED);
        }
        return toResponse(member);
    }

    /** 다른 도메인이 쓰는 컨트랙트: 활성 회원 조회. 없거나 탈퇴면 INVALID_TOKEN (토큰은 있으나 주체가 사라진 경우). */
    public Member getActive(UUID memberId) {
        return members.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_TOKEN));
    }

    private static String normalizeEmail(String email) {
        return email.strip().toLowerCase(Locale.ROOT);
    }

    /** TODO(M1): 금칙어 목록을 설정 파일로 분리. */
    private static void validateNickname(String nickname) {
        String lower = nickname.toLowerCase(Locale.ROOT);
        if (lower.startsWith("deleted_") || lower.contains("admin") || lower.contains("운영자")) {
            throw new BusinessException(ErrorCode.MEMBER_NICKNAME_FORBIDDEN_WORD);
        }
    }
}
