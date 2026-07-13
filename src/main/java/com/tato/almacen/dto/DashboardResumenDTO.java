package com.tato.almacen.dto;

import java.math.BigDecimal;
import java.util.List;

public record DashboardResumenDTO(
        BigDecimal ventasDelDia,
        Double porcentajeVariacion,
        Integer stockCritico,
        List<ActividadDTO> actividadReciente
) {}
