package com.tato.almacen.service;

import com.tato.almacen.dto.FotoReferenciaDTO;
import com.tato.almacen.exception.ApiException;
import com.tato.almacen.model.FotoReferenciaProducto;
import com.tato.almacen.model.Producto;
import com.tato.almacen.repository.FotoReferenciaProductoRepository;
import com.tato.almacen.repository.ProductoRepository;
import com.tato.almacen.service.cloudinary.CloudinaryService;
import com.tato.almacen.service.cloudinary.CloudinaryUploadResult;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * CRUD de fotos de referencia de un producto (almacenadas en Cloudinary,
 * carpeta tato-almacen/productos/{id}). Estas fotos son las que se usan
 * para generar (o regenerar) el perfil IA sin tener que volver a tomarlas
 * cada vez.
 */
@Service
@RequiredArgsConstructor
public class FotoReferenciaService {

    private final FotoReferenciaProductoRepository fotoReferenciaProductoRepository;
    private final ProductoRepository productoRepository;
    private final CloudinaryService cloudinaryService;
    private final RestClient restClient = RestClient.create();

    public List<FotoReferenciaDTO> subir(Long productoId, List<MultipartFile> fotos) {
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new ApiException("Producto no encontrado", HttpStatus.NOT_FOUND));

        if (fotos == null || fotos.isEmpty()) {
            throw new ApiException("Debes enviar al menos una foto");
        }

        boolean yaTieneFotos = !fotoReferenciaProductoRepository.findByProductoId(productoId).isEmpty();

        List<FotoReferenciaProducto> guardadas = new ArrayList<>();
        for (MultipartFile file : fotos) {
            CloudinaryUploadResult subida = cloudinaryService.subir(file, "productos/" + productoId);

            FotoReferenciaProducto foto = new FotoReferenciaProducto();
            foto.setProducto(producto);
            foto.setUrlFoto(subida.url());
            foto.setPublicIdCloudinary(subida.publicId());
            foto.setEsPrincipal(!yaTieneFotos && guardadas.isEmpty());
            foto.setFechaSubida(LocalDateTime.now());

            guardadas.add(fotoReferenciaProductoRepository.save(foto));
        }

        return guardadas.stream().map(this::toDTO).toList();
    }

    public List<FotoReferenciaDTO> listar(Long productoId) {
        return fotoReferenciaProductoRepository.findByProductoId(productoId)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public void eliminar(Long productoId, Long fotoId) {
        FotoReferenciaProducto foto = fotoReferenciaProductoRepository.findById(fotoId)
                .filter(f -> f.getProducto().getId().equals(productoId))
                .orElseThrow(() -> new ApiException("Foto no encontrada para este producto", HttpStatus.NOT_FOUND));

        if (foto.getPublicIdCloudinary() != null) {
            cloudinaryService.eliminar(foto.getPublicIdCloudinary());
        }
        fotoReferenciaProductoRepository.delete(foto);
    }

    /**
     * Descarga los bytes de las fotos ya guardadas para poder mandarlas a
     * Gemini sin que el usuario tenga que volver a subirlas (usado por
     * PerfilIaService cuando se pide regenerar el perfil de un producto
     * que ya tiene fotos de referencia).
     */
    public List<byte[]> descargarBytes(Long productoId) {
        List<FotoReferenciaProducto> fotos = fotoReferenciaProductoRepository.findByProductoId(productoId);
        List<byte[]> resultado = new ArrayList<>();
        for (FotoReferenciaProducto foto : fotos) {
            try {
                byte[] bytes = restClient.get().uri(foto.getUrlFoto()).retrieve().body(byte[].class);
                if (bytes != null) resultado.add(bytes);
            } catch (Exception e) {
                // si una foto puntual falla al descargar, seguimos con las demas
            }
        }
        return resultado;
    }

    private FotoReferenciaDTO toDTO(FotoReferenciaProducto f) {
        return new FotoReferenciaDTO(f.getId(), f.getUrlFoto(), f.getEsPrincipal());
    }
}
