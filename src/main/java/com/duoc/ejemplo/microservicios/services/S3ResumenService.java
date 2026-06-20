package com.duoc.ejemplo.microservicios.services;

import com.duoc.ejemplo.microservicios.models.ResumenInscripcion;
import com.duoc.ejemplo.microservicios.repositories.ResumenInscripcionRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.time.LocalDateTime;

@Service
public class S3ResumenService {

    private final S3Client s3Client;
    private final ResumenInscripcionRepository resumenRepository;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3ResumenService(S3Client s3Client, ResumenInscripcionRepository resumenRepository) {
        this.s3Client = s3Client;
        this.resumenRepository = resumenRepository;
    }

    /**
     * Sube el PDF a S3. Cada resumen se guarda en una carpeta cuyo nombre
     * corresponde al ID (número) del resumen: resumenes/{id}/{nombreArchivo}
     */
    public ResumenInscripcion subirResumen(String email, byte[] pdfBytes) {
        // 1) Persistimos primero el registro para obtener el ID (número del resumen)
        ResumenInscripcion resumen = new ResumenInscripcion();
        resumen.setEmail(email);
        resumen.setFechaCreacion(LocalDateTime.now());
        resumen.setNombreArchivo("resumen.pdf");
        resumen.setS3Key("pendiente"); // placeholder, se actualiza luego del save
        resumen = resumenRepository.save(resumen);

        String s3Key = "resumenes/" + resumen.getId() + "/" + resumen.getNombreArchivo();
        resumen.setS3Key(s3Key);
        resumen = resumenRepository.save(resumen);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(pdfBytes));

        return resumen;
    }

    /**
     * Reemplaza (modifica) el archivo PDF de un resumen existente.
     */
    public ResumenInscripcion modificarResumen(Long id, byte[] nuevoPdfBytes) {
        ResumenInscripcion resumen = obtenerResumen(id);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(resumen.getS3Key())
                .contentType("application/pdf")
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(nuevoPdfBytes));

        resumen.setFechaCreacion(LocalDateTime.now());
        return resumenRepository.save(resumen);
    }

    /**
     * Descarga el PDF desde S3 como arreglo de bytes.
     */
    public byte[] descargarResumen(Long id) {
        ResumenInscripcion resumen = obtenerResumen(id);

        GetObjectRequest getRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(resumen.getS3Key())
                .build();

        ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObject(getRequest);
        return objectBytes.asByteArray();
    }

    /**
     * Borra el resumen de S3 y su registro de la base de datos.
     */
    public void borrarResumen(Long id) {
        ResumenInscripcion resumen = obtenerResumen(id);

        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(resumen.getS3Key())
                .build();

        s3Client.deleteObject(deleteRequest);
        resumenRepository.delete(resumen);
    }

    public ResumenInscripcion obtenerResumen(Long id) {
        return resumenRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Resumen no encontrado con ID: " + id));
    }
}
