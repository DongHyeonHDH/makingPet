package com.gorapaduk.makingpet.home.domain;

import java.util.UUID;

import com.gorapaduk.makingpet.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 마이 홈 (회원당 1개). 가구 배치는 HomeFurniture. */
@Entity
@Table(name = "homes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Home extends BaseEntity {

    public enum Visibility { PUBLIC, PRIVATE }

    public static final int FURNITURE_LIMIT = 20;

    @Id
    @Column(name = "member_id")
    private UUID memberId;

    @Column(name = "theme_item_id", nullable = false)
    private UUID themeItemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Visibility visibility;

    @Column(nullable = false)
    private int likeCount;

    public static Home create(UUID memberId, UUID themeItemId) {
        Home h = new Home();
        h.memberId = memberId;
        h.themeItemId = themeItemId;
        h.visibility = Visibility.PUBLIC;
        h.likeCount = 0;
        return h;
    }

    public void changeTheme(UUID themeItemId) { this.themeItemId = themeItemId; }

    public void changeVisibility(Visibility v) { this.visibility = v; }

    public void like() { likeCount++; }

    public boolean isPublic() { return visibility == Visibility.PUBLIC; }
}
