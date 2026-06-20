package com.duoc.ejemplo.microservicios.controllers;

import com.duoc.ejemplo.microservicios.models.Curso;
import com.duoc.ejemplo.microservicios.services.CursoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CursoController {

    @Autowired
    private CursoService cursoService;

    // GET /api/cursos
    @GetMapping("/cursos")
    public ResponseEntity<List<Curso>> listarCursos() {
        return ResponseEntity.ok(cursoService.listarCursos());
    }

    // POST /api/cursos
    @PostMapping("/cursos")
    public ResponseEntity<Map<String, Object>> agregarCurso(@RequestBody Curso curso) {
        Curso nuevoCurso = cursoService.agregarCurso(curso);
        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Curso agregado exitosamente");
        response.put("curso", nuevoCurso);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // POST /api/inscripciones
    @PostMapping("/inscripciones")
    public ResponseEntity<Map<String, Object>> inscribir(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        List<Integer> cursosIdsRaw = (List<Integer>) request.get("cursosIds");

        List<Long> cursosIds = new ArrayList<>();
        for (Integer id : cursosIdsRaw) {
            cursosIds.add(id.longValue());
        }

        List<Curso> cursosSeleccionados = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (Long id : cursosIds) {
            Curso curso = cursoService.obtenerCursoPorId(id);
            cursosSeleccionados.add(curso);
            total = total.add(curso.getCosto());
        }

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Inscripción exitosa");
        response.put("estudiante", email);
        response.put("cursos", cursosSeleccionados);
        response.put("totalPagar", total);
        response.put("fecha", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }
}
