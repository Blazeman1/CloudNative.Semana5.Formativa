package com.duoc.ejemplo.microservicios.controllers;

import com.duoc.ejemplo.microservicios.models.Curso;
import com.duoc.ejemplo.microservicios.services.CursoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api")
public class CursoController {

    @Autowired
    private CursoService cursoService;

    /**
     * GET /api/cursos
     * Lista todos los cursos. Requiere JWT válido (cualquier rol).
     */
    @GetMapping("/cursos")
    public ResponseEntity<List<Curso>> listarCursos() {
        return ResponseEntity.ok(cursoService.listarCursos());
    }

    /**
     * GET /api/cursos/consulta
     * Lista cursos disponibles con información del usuario autenticado.
     * Requiere JWT válido + custom claim extension_consultaRole = "consulta".
     * → 401 si no hay token, 403 si el rol no es "consulta".
     *
     * Semana 6: endpoint protegido por custom claim de Azure AD B2C.
     */
    @GetMapping("/cursos/consulta")
    public ResponseEntity<Map<String, Object>> consultarCursos(
            @AuthenticationPrincipal Jwt jwt) {

        List<Curso> cursos = cursoService.listarCursos();

        // Extraemos info del token para mostrar en la respuesta
        String usuario   = jwt.getClaimAsString("name");
        String email     = jwt.getClaimAsString("emails") != null
                ? jwt.getClaimAsString("emails") : jwt.getSubject();
        String rol       = jwt.getClaimAsString("extension_consultaRole");

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Acceso autorizado con rol: " + rol);
        response.put("usuario", usuario);
        response.put("email", email);
        response.put("rol", rol);
        response.put("totalCursos", cursos.size());
        response.put("cursos", cursos);
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/cursos
     * Crea un nuevo curso. Requiere JWT válido + custom claim
     * extension_consultaRole = "admin".
     * → 401 si no hay token, 403 si el rol no es "admin".
     *
     * Semana 6: acción privilegiada protegida por custom claim de Azure AD B2C.
     */
    @PostMapping("/cursos")
    public ResponseEntity<Map<String, Object>> agregarCurso(
            @RequestBody Curso curso,
            @AuthenticationPrincipal Jwt jwt) {

        Curso nuevoCurso = cursoService.agregarCurso(curso);
        String rol = jwt.getClaimAsString("extension_consultaRole");

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Curso agregado exitosamente por usuario con rol: " + rol);
        response.put("curso", nuevoCurso);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * POST /api/inscripciones
     * Gestiona la inscripción de un estudiante en cursos.
     * Requiere JWT válido (cualquier rol).
     */
    @PostMapping("/inscripciones")
    public ResponseEntity<Map<String, Object>> inscribir(
            @RequestBody Map<String, Object> request) {

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
