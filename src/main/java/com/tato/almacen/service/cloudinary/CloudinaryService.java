package com.tato.almacen.service.cloudinary;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.tato.almacen.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public CloudinaryService(@Value("${cloudinary.cloud_name}") String cloudName,
                              @Value("${cloudinary.api_key}") String apiKey,
                              @Value("${cloudinary.api_secret}") String apiSecret) {
        this.cloudinary = new Cloudinary(ObjectUtils.asMap(
                "cloud_name", cloudName,
                "api_key", apiKey,
                "api_secret", apiSecret
        ));
    }

    public CloudinaryUploadResult subir(MultipartFile file, String carpeta) {
        try {
            Map<?, ?> resultado = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", "tato-almacen/" + carpeta,
                    "resource_type", "image"
            ));
            return new CloudinaryUploadResult(
                    resultado.get("secure_url").toString(),
                    resultado.get("public_id").toString()
            );
        } catch (IOException e) {
            throw new ApiException("Error subiendo imagen a Cloudinary: " + e.getMessage());
        }
    }

    public void eliminar(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            throw new ApiException("Error eliminando imagen de Cloudinary: " + e.getMessage());
        }
    }
}
