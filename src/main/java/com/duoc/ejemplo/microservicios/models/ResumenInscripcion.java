package com.duoc.ejemplo.microservicios.models;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "resumenes_inscripcion")
public class ResumenInscripcion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String email;

    @Column(nullable = false, length = 200)
    private String nombreArchivo;

    // Clave (key) del objeto dentro del bucket S3 -> resumenes/{id}/{nombreArchivo}
    @Column(nullable = false, length = 300)
    private String s3Key;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion;

    public ResumenInscripcion() {}

    public ResumenInscripcion(String email, String nombreArchivo, String s3Key, LocalDateTime fechaCreacion) {
        this.email = email;
        this.nombreArchivo = nombreArchivo;
        this.s3Key = s3Key;
        this.fechaCreacion = fechaCreacion;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNombreArchivo() { return nombreArchivo; }
    public String getS3Key() { return s3Key; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }

    public void setId(Long id) { this.id = id; }
    public void setEmail(String email) { this.email = email; }
    public void setNombreArchivo(String nombreArchivo) { this.nombreArchivo = nombreArchivo; }
    public void setS3Key(String s3Key) { this.s3Key = s3Key; }
    public void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }
}
