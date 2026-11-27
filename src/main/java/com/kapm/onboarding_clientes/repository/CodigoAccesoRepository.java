package com.kapm.onboarding_clientes.repository;

import com.kapm.onboarding_clientes.entity.CodigoAcceso;
import com.kapm.onboarding_clientes.entity.PropositoCodigoAcceso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface CodigoAccesoRepository extends JpaRepository<CodigoAcceso, Long> {

    Optional<CodigoAcceso> findTopByUsernameAndPropositoOrderByCreadoEnDesc(String username, PropositoCodigoAcceso proposito);

    List<CodigoAcceso> findByUsernameAndPropositoAndUsadoFalse(String username, PropositoCodigoAcceso proposito);

    long countByUsernameAndPropositoAndCreadoEnAfter(String username, PropositoCodigoAcceso proposito, LocalDateTime desde);
}
