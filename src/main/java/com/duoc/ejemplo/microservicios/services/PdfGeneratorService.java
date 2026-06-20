package com.duoc.ejemplo.microservicios.services;

import com.duoc.ejemplo.microservicios.models.Curso;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGeneratorService {

    /**
     * Genera el PDF del resumen de inscripción en memoria y retorna los bytes.
     */
    public byte[] generarResumenInscripcion(String email, List<Curso> cursos, BigDecimal total, LocalDateTime fecha) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            document.add(new Paragraph("Resumen de Inscripción")
                    .setBold().setFontSize(18));

            document.add(new Paragraph("Estudiante: " + email));
            document.add(new Paragraph("Fecha: " + fecha.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))));
            document.add(new Paragraph(" "));

            Table table = new Table(UnitValue.createPercentArray(new float[]{4, 3, 2, 2}))
                    .useAllAvailableWidth();
            table.addHeaderCell(new Cell().add(new Paragraph("Curso")));
            table.addHeaderCell(new Cell().add(new Paragraph("Instructor")));
            table.addHeaderCell(new Cell().add(new Paragraph("Duración (hrs)")));
            table.addHeaderCell(new Cell().add(new Paragraph("Costo")));

            for (Curso curso : cursos) {
                table.addCell(new Cell().add(new Paragraph(curso.getNombre())));
                table.addCell(new Cell().add(new Paragraph(curso.getInstructor())));
                table.addCell(new Cell().add(new Paragraph(String.valueOf(curso.getDuracion()))));
                table.addCell(new Cell().add(new Paragraph("$" + curso.getCosto().toString())));
            }

            document.add(table);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Total a pagar: $" + total.toString())
                    .setBold().setFontSize(14));

            document.close();
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Error generando el PDF del resumen de inscripción", e);
        }
    }
}
