package com.gorapaduk.makingpet.shop.service;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import com.gorapaduk.makingpet.common.constants.ErrorCode;
import com.gorapaduk.makingpet.common.dto.PageResponse;
import com.gorapaduk.makingpet.common.exception.BusinessException;
import com.gorapaduk.makingpet.common.util.KstTime;
import com.gorapaduk.makingpet.common.util.PageRequests;
import com.gorapaduk.makingpet.item.domain.InventoryItem;
import com.gorapaduk.makingpet.item.domain.Item;
import com.gorapaduk.makingpet.item.domain.ItemCategory;
import com.gorapaduk.makingpet.item.dto.ItemSummary;
import com.gorapaduk.makingpet.item.service.InventoryService;
import com.gorapaduk.makingpet.item.service.ItemService;
import com.gorapaduk.makingpet.shop.domain.ShopProduct;
import com.gorapaduk.makingpet.shop.domain.ShopPurchase;
import com.gorapaduk.makingpet.shop.dto.ShopDtos;
import com.gorapaduk.makingpet.shop.repository.ShopProductRepository;
import com.gorapaduk.makingpet.shop.repository.ShopPurchaseRepository;
import com.gorapaduk.makingpet.wallet.domain.TransactionReason;
import com.gorapaduk.makingpet.wallet.dto.WalletResponse;
import com.gorapaduk.makingpet.wallet.service.WalletService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상점 (규칙 §6). S-1 ~ S-3 + 관리자 AD-8 · AD-9 (admin 도메인이 호출).
 * 판매 중 = 상품 visible · 기간 안(KST) · 아이템 active.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ShopService {

    public static final String REF_TYPE = "SHOP_PURCHASE";

    private final ShopProductRepository products;
    private final ShopPurchaseRepository purchases;
    private final ItemService itemService;
    private final InventoryService inventoryService;
    private final WalletService walletService;

    /** S-1. viewerId 가 null 이면 owned = null. */
    public PageResponse<ShopDtos.Product> list(ItemCategory category, int page, int size, UUID viewerId) {
        var pageable = PageRequests.of(page, size, Sort.by("sortOrder"));
        Page<ShopProduct> result = products.searchOnSale(KstTime.now(), category, pageable);
        var itemIds = result.getContent().stream().map(ShopProduct::getItemId).toList();
        Map<UUID, Item> items = itemService.getMap(itemIds);
        Map<UUID, Integer> owned = viewerId == null ? Map.of() : inventoryService.quantities(viewerId, itemIds);
        return PageResponse.of(result, p -> new ShopDtos.Product(p.getId(), ItemSummary.from(items.get(p.getItemId())),
                p.getPrice(), p.getSaleEndsAt(), viewerId == null ? null : owned.getOrDefault(p.getItemId(), 0)));
    }

    /** S-2. 판매 중이 아닌 상품은 익명 조회에서도 숨긴다(404). */
    public ShopDtos.Product detail(UUID productId, UUID viewerId) {
        ShopProduct product = getById(productId);
        Item item = itemService.getById(product.getItemId());
        if (!onSale(product, item, KstTime.now())) throw new BusinessException(ErrorCode.SHOP_PRODUCT_NOT_FOUND);
        Integer owned = viewerId == null ? null : inventoryService.quantities(viewerId, java.util.List.of(item.getId()))
                .getOrDefault(item.getId(), 0);
        return new ShopDtos.Product(product.getId(), ItemSummary.detail(item), product.getPrice(), product.getSaleEndsAt(), owned);
    }

    /**
     * S-3. 한 트랜잭션: 판매 중 확인 → 코인 차감(조건부 UPDATE) → 아이템 지급(행 잠금) → 구매 기록.
     * 코인이 부족하면 아무것도 바뀌지 않는다.
     */
    @Transactional
    public ShopDtos.PurchaseResponse purchase(UUID memberId, UUID productId, int quantity) {
        if (quantity < 1 || quantity > 10) throw new BusinessException(ErrorCode.VALIDATION_FAILED, "quantity 는 1~10 이어야 합니다.");
        ShopProduct product = getById(productId);
        Item item = itemService.getById(product.getItemId());
        if (!onSale(product, item, KstTime.now())) throw new BusinessException(ErrorCode.SHOP_PRODUCT_NOT_ON_SALE);

        UUID purchaseId = UUID.randomUUID();
        int total = product.getPrice() * quantity;
        WalletResponse wallet = walletService.debit(memberId, total, TransactionReason.SHOP_PURCHASE, REF_TYPE, purchaseId,
                item.getName() + " x" + quantity);
        InventoryItem row = inventoryService.grant(memberId, item.getId(), quantity);
        purchases.save(ShopPurchase.of(purchaseId, memberId, product, quantity));
        return new ShopDtos.PurchaseResponse(purchaseId, ItemSummary.from(item), quantity, product.getPrice(), total,
                new ShopDtos.InventorySnapshot(row.getQuantity(), row.getAvailable()), wallet);
    }

    // ----- Admin (AD-8 · AD-9) -----

    /** AD-8. MOTION(친밀도 보상)은 상점에 올릴 수 없다. */
    @Transactional
    public ShopDtos.AdminProduct create(ShopDtos.CreateRequest req) {
        Item item = itemService.getById(req.itemId());
        if (item.getCategory() == ItemCategory.MOTION) throw new BusinessException(ErrorCode.ITEM_NOT_TRADABLE);
        LocalDateTime starts = toKst(req.saleStartsAt());
        LocalDateTime ends = toKst(req.saleEndsAt());
        validatePeriod(starts, ends);
        ShopProduct product = products.save(ShopProduct.create(item.getId(), req.price(), starts, ends,
                req.visible() == null || req.visible(), req.sortOrder() == null ? 0 : req.sortOrder()));
        return toAdmin(product, item);
    }

    /** AD-9 */
    @Transactional
    public ShopDtos.AdminProduct update(UUID productId, ShopDtos.UpdateRequest req) {
        ShopProduct product = getById(productId);
        product.update(req.price(), toKst(req.saleStartsAt()), toKst(req.saleEndsAt()), req.visible(), req.sortOrder());
        validatePeriod(product.getSaleStartsAt(), product.getSaleEndsAt());
        return toAdmin(product, itemService.getById(product.getItemId()));
    }

    /** 시드용: 같은 아이템 상품이 이미 있는가 */
    public boolean existsForItem(UUID itemId) {
        return products.existsByItemId(itemId);
    }

    // ----- 내부 -----

    private ShopProduct getById(UUID productId) {
        return products.findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SHOP_PRODUCT_NOT_FOUND));
    }

    private static boolean onSale(ShopProduct product, Item item, LocalDateTime now) {
        return item.isActive() && product.isOnSale(now);
    }

    private ShopDtos.AdminProduct toAdmin(ShopProduct p, Item item) {
        return new ShopDtos.AdminProduct(p.getId(), ItemSummary.from(item), p.getPrice(), p.getSaleStartsAt(),
                p.getSaleEndsAt(), p.isVisible(), p.getSortOrder(), onSale(p, item, KstTime.now()));
    }

    private static void validatePeriod(LocalDateTime starts, LocalDateTime ends) {
        if (starts != null && ends != null && !starts.isBefore(ends)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "saleStartsAt 은 saleEndsAt 보다 앞이어야 합니다.");
        }
    }

    /** 요청 시각(오프셋 포함)을 KST LocalDateTime 으로. 서버 기본 시간대와 무관하게 비교하기 위함. */
    static LocalDateTime toKst(OffsetDateTime t) {
        return t == null ? null : t.atZoneSameInstant(KstTime.ZONE).toLocalDateTime();
    }
}
