package br.com.aps.meteorologia.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DataHoraUtilTest {

    @Test
    void converteHorarioDaOpenMeteo() {
        assertThat(DataHoraUtil.converterHorarioFonte("2026-10-20T22:15"))
                .isEqualTo(LocalDateTime.of(2026, 10, 20, 22, 15));
    }

    @Test
    void horarioVazioViraNulo() {
        assertThat(DataHoraUtil.converterHorarioFonte(null)).isNull();
        assertThat(DataHoraUtil.converterHorarioFonte(" ")).isNull();
    }

    @Test
    void formatoInvalido() {
        assertThatThrownBy(() -> DataHoraUtil.converterHorarioFonte("20/10/2026 22h15"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void agoraSemNanossegundos() {
        assertThat(DataHoraUtil.agora(DataHoraUtil.FUSO_PADRAO).getNano() % 1_000_000).isZero();
    }
}
