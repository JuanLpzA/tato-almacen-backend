package com.tato.almacen.controller;

import com.tato.almacen.dto.DashboardResumenDTO;
import com.tato.almacen.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/resumen")
    public DashboardResumenDTO getResumen(HttpServletRequest request) {
        Long sucursalId = (Long) request.getAttribute("sucursalId");
        return dashboardService.getResumen(sucursalId);
    }
}
