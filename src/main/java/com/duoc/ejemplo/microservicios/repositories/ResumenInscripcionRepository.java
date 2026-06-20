package com.duoc.ejemplo.microservicios.repositories;

import com.duoc.ejemplo.microservicios.models.ResumenInscripcion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ResumenInscripcionRepository extends JpaRepository<ResumenInscripcion, Long> {
}
