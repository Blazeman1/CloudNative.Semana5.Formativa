package com.duoc.ejemplo.microservicios.repositories;

import com.duoc.ejemplo.microservicios.models.Curso;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CursoRepository extends JpaRepository<Curso, Long> {
}
