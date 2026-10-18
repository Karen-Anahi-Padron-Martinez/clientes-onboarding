package com.kapm.onboarding_clientes.dto.response;

import lombok.*;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PageResponse<T> {

    private List<T> contenido;
    private int paginaActual;
    private int totalPaginas;
    private long totalElementos;
    private int tamanoPagina;
    private boolean primera;
    private boolean ultima;

    public static <T> PageResponse<T> from(Page<T> page) {
        return PageResponse.<T>builder()
                .contenido(page.getContent())
                .paginaActual(page.getNumber())
                .totalPaginas(page.getTotalPages())
                .totalElementos(page.getTotalElements())
                .tamanoPagina(page.getSize())
                .primera(page.isFirst())
                .ultima(page.isLast())
                .build();
    }
}
