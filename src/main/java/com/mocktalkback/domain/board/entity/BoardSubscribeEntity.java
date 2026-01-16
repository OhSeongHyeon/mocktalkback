package com.mocktalkback.domain.board.entity;

import com.mocktalkback.global.common.entity.BaseTimeEntity;
import com.mocktalkback.domain.user.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
    name = "tb_board_subscribes",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_tb_board_subscribes_user_id_board_id",
            columnNames = {"user_id", "board_id"}
        )
    },
    indexes = {
        @Index(name = "ix_tb_board_subscribes_board_id", columnList = "board_id")
    }
)
public class BoardSubscribeEntity extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "board_subscribe_id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "user_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_board_subscribes_user_id__tb_users")
    )
    private UserEntity user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "board_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_tb_board_subscribes_board_id__tb_boards")
    )
    private BoardEntity board;

    @Builder
    private BoardSubscribeEntity(UserEntity user, BoardEntity board) {
        this.user = user;
        this.board = board;
    }
}
