package com.kapm.onboarding_clientes.repository.specification;

import com.kapm.onboarding_clientes.dto.request.BuscarClienteRequest;
import com.kapm.onboarding_clientes.entity.Cliente;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class ClienteSpecification {

    public static Specification<Cliente> conFiltros(BuscarClienteRequest request) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (request.getCurp() != null && !request.getCurp().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("curp")), request.getCurp().trim().toUpperCase()));
            }

            if (request.getRfc() != null && !request.getRfc().isBlank()) {
                predicates.add(cb.equal(cb.upper(root.get("rfc")), request.getRfc().trim().toUpperCase()));
            }

            if (request.getCorreo() != null && !request.getCorreo().isBlank()) {
                predicates.add(cb.equal(cb.lower(root.get("correo")), request.getCorreo().trim().toLowerCase()));
            }

            if (request.getNumeroCuenta() != null && !request.getNumeroCuenta().isBlank()) {
                predicates.add(cb.equal(root.get("cuenta").get("numeroCuenta"), request.getNumeroCuenta().trim()));
            }

            if (request.getNombre() != null && !request.getNombre().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("nombre")), "%" + request.getNombre().trim().toLowerCase() + "%"));
            }

            if (request.getApellidoPaterno() != null && !request.getApellidoPaterno().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("apellidoPaterno")), "%" + request.getApellidoPaterno().trim().toLowerCase() + "%"));
            }

            if (request.getApellidoMaterno() != null && !request.getApellidoMaterno().isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("apellidoMaterno")), "%" + request.getApellidoMaterno().trim().toLowerCase() + "%"));
            }

            if (request.getActivo() != null) {
                predicates.add(cb.equal(root.get("activo"), request.getActivo()));
            }

            if (request.getFechaInicio() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaRegistro"), request.getFechaInicio().atStartOfDay()));
            }

            if (request.getFechaFin() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaRegistro"), request.getFechaFin().atTime(LocalTime.MAX)));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
