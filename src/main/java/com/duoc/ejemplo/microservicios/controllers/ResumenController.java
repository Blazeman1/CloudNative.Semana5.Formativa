package com.duoc.ejemplo.microservicios.controllers;

import com.duoc.ejemplo.microservicios.models.Curso;
import com.duoc.ejemplo.microservicios.models.ResumenInscripcion;
import com.duoc.ejemplo.microservicios.services.CursoService;
import com.duoc.ejemplo.microservicios.services.PdfGeneratorService;
import com.duoc.ejemplo.microservicios.services.S3ResumenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/resumenes")
public class ResumenController {

    @Autowired
    private CursoService cursoService;

    @Autowired
    private PdfGeneratorService pdfGeneratorService;

    @Autowired
    private S3ResumenService s3ResumenService;

    /**
     * Genera el archivo PDF del resumen de inscripción (semana 1) y lo
     * devuelve como archivo descargable para guardar en el computador
     * del usuario.
     *
     * POST /api/resumenes/generar
     * body: { "email": "...", "cursosIds": [1,2,3] }
     */
    @PostMapping("/generar")
    public ResponseEntity<byte[]> generarResumen(@RequestBody Map<String, Object> request) {
        byte[] pdfBytes = construirPdfDesdeRequest(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "resumen_inscripcion.pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    /**
     * Genera el resumen de inscripción y lo sube a un bucket de AWS S3.
     * Cada resumen se guarda en una carpeta cuyo nombre corresponde al
     * número (ID) del resumen: resumenes/{id}/resumen.pdf
     *
     * POST /api/resumenes/subir
     * body: { "email": "...", "cursosIds": [1,2,3] }
     */
    @PostMapping("/subir")
    public ResponseEntity<Map<String, Object>> subirResumen(@RequestBody Map<String, Object> request) {
        String email = (String) request.get("email");
        byte[] pdfBytes = construirPdfDesdeRequest(request);

        ResumenInscripcion resumen = s3ResumenService.subirResumen(email, pdfBytes);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Resumen subido exitosamente a S3");
        response.put("idResumen", resumen.getId());
        response.put("s3Key", resumen.getS3Key());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Modifica (reemplaza) el archivo de un resumen existente en S3.
     * Útil para corregir errores en la inscripción.
     *
     * PUT /api/resumenes/{id}
     * body: { "email": "...", "cursosIds": [1,2,3] }
     */
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> modificarResumen(@PathVariable Long id,
                                                                  @RequestBody Map<String, Object> request) {
        byte[] pdfBytes = construirPdfDesdeRequest(request);
        ResumenInscripcion resumen = s3ResumenService.modificarResumen(id, pdfBytes);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Resumen modificado exitosamente");
        response.put("idResumen", resumen.getId());
        response.put("s3Key", resumen.getS3Key());
        return ResponseEntity.ok(response);
    }

    /**
     * Descarga el archivo del resumen de inscripción almacenado en S3.
     *
     * GET /api/resumenes/{id}/descargar
     */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargarResumen(@PathVariable Long id) {
        byte[] pdfBytes = s3ResumenService.descargarResumen(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "resumen_" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    /**
     * Borra el archivo del resumen de inscripción desde S3 y su registro.
     *
     * DELETE /api/resumenes/{id}
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> borrarResumen(@PathVariable Long id) {
        s3ResumenService.borrarResumen(id);

        Map<String, Object> response = new HashMap<>();
        response.put("mensaje", "Resumen eliminado exitosamente");
        response.put("idResumen", id);
        return ResponseEntity.ok(response);
    }

    @SuppressWarnings("unchecked")
    private byte[] construirPdfDesdeRequest(Map<String, Object> request) {
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

        return pdfGeneratorService.generarResumenInscripcion(email, cursosSeleccionados, total, LocalDateTime.now());
    }
}
