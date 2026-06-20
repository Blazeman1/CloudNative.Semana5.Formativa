package com.duoc.ejemplo.microservicios.models;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cursos")
public class Curso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, length = 100)
    private String instructor;

    @Column(nullable = false)
    private Integer duracion;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal costo;

    public Curso() {}

    public Curso(String nombre, String instructor, Integer duracion, BigDecimal costo) {
        this.nombre = nombre;
        this.instructor = instructor;
        this.duracion = duracion;
        this.costo = costo;
    }

    // Getters
    public Long getId() { return id; }
    public String getNombre() { return nombre; }
    public String getInstructor() { return instructor; }
    public Integer getDuracion() { return duracion; }
    public BigDecimal getCosto() { return costo; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public void setInstructor(String instructor) { this.instructor = instructor; }
    public void setDuracion(Integer duracion) { this.duracion = duracion; }
    public void setCosto(BigDecimal costo) { this.costo = costo; }
}
