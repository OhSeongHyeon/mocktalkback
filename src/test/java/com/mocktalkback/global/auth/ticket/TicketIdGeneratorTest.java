package com.mocktalkback.global.auth.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class TicketIdGeneratorTest {

    // ticket id 생성기는 prefix를 유지한 채 UUID 기반 식별자를 붙여야 한다.
    @Test
    void generate_returns_ticket_id_with_prefix() {
        // given: ticket id 생성기
        TicketIdGenerator generator = new TicketIdGenerator();

        // when: file view prefix로 ticket id를 생성하면
        String ticketId = generator.generate("fv_");

        // then: prefix가 유지된 ticket id가 생성된다.
        assertThat(ticketId).startsWith("fv_");
        assertThat(ticketId.length()).isGreaterThan("fv_".length());
    }

    // prefix가 비어 있으면 ticket id를 만들면 안 된다.
    @Test
    void generate_throws_when_prefix_is_blank() {
        // given: ticket id 생성기
        TicketIdGenerator generator = new TicketIdGenerator();

        // when & then: 빈 prefix는 예외가 발생한다.
        assertThatThrownBy(() -> generator.generate(" "))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ticket prefix");
    }
}
