package com.tato.almacen.controller;

import com.tato.almacen.dto.AsistenteConsultaRequest;
import com.tato.almacen.dto.AsistenteRespuestaDTO;
import com.tato.almacen.service.AsistenteVozService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asistente")
@RequiredArgsConstructor
public class AsistenteVozController {

    private final AsistenteVozService asistenteVozService;

    /**
     * Recibe TEXTO ya transcrito por el SpeechRecognizer del celular
     * (la app Android hace voz->texto de forma nativa, aca solo se
     * interpreta la intencion y se responde con producto+ubicacion).
     */
    @PostMapping("/consultar")
    public AsistenteRespuestaDTO consultar(@Valid @RequestBody AsistenteConsultaRequest request, HttpServletRequest httpRequest) {
        Long sucursalId = (Long) httpRequest.getAttribute("sucursalId");
        return asistenteVozService.consultar(request.texto(), sucursalId);
    }
}
