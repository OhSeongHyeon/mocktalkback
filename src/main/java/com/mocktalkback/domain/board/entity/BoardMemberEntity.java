package com.mocktalkback.domain.board.entity;

import com.mocktalkback.global.common.entity.BaseTimeEntity;
import com.mocktalkback.domain.board.type.BoardRole;
import com.mocktalkback.domain.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(
    name = "tb_board_members",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_board_members_user_id_board_id",
            columnNames = {"user_id", "board_id"}
        )
    },
    indexes = {
        @Index(name = "ix_tb_board_members_board_id", columnList = "board_id")
    }
)
public class BoardMemberEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_manager_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_board_members_user_id__tb_users")
    )
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "board_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_board_members_board_id__tb_boards")
    )
    private BoardEntity board;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "granted_by_user_id",
        foreignKey = @ForeignKey(name = "fk_tb_board_members_granted_by_user_id__tb_users")
    )
    private UserEntity grantedByUser;

    @Enumerated(EnumType.STRING)
    @Column(name = "board_role", nullable = false, length = 24)
    private BoardRole boardRole;

    @Builder
    private BoardMemberEntity(
        UserEntity user,
        BoardEntity board,
        UserEntity grantedByUser,
        BoardRole boardRole
    ) {
        this.user = user;
        this.board = board;
        this.grantedByUser = grantedByUser;
        this.boardRole = boardRole;
    }

    // 가입 승인 처리(역할 변경 + 승인자 기록)
    public void approve(UserEntity approver) {
        this.boardRole = BoardRole.MEMBER;
        this.grantedByUser = approver;
    }

    public void changeRole(BoardRole boardRole, UserEntity grantedByUser) {
        this.boardRole = boardRole;
        this.grantedByUser = grantedByUser;
    }
}
