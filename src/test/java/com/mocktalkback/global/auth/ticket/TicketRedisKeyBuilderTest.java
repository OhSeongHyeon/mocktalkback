package com.mocktalkback.global.auth.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TicketRedisKeyBuilderTest {

    // ticket Redis key는 channel 규격에 맞춰 생성되어야 한다.
    @Test
    void build_returns_standardized_ticket_key() {
        // given: ticket Redis key builder
        TicketRedisKeyBuilder builder = new TicketRedisKeyBuilder();

        // when: resource view channel key를 생성하면
        String key = builder.build(TicketChannel.RESOURCE_VIEW, "fv_123");

        // then: 표준 ticket key를 반환한다.
        assertThat(key).isEqualTo("ticket:resource_view:fv_123");
    }

    // ticket id가 비어 있으면 Redis key를 만들면 안 된다.
    @Test
    void build_throws_when_ticket_id_is_blank() {
        // given: ticket Redis key builder
        TicketRedisKeyBuilder builder = new TicketRedisKeyBuilder();

        // when & then: 빈 ticket id는 예외가 발생한다.
        assertThatThrownBy(() -> builder.build(TicketChannel.REALTIME_CONNECT, " "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ticket id");
    }
}
