package com.tato.almacen.service;

import com.tato.almacen.dto.ActividadDTO;
import com.tato.almacen.dto.DashboardResumenDTO;
import com.tato.almacen.repository.HistorialRepository;
import com.tato.almacen.repository.InventarioSucursalRepository;
import com.tato.almacen.repository.VentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final VentaRepository ventaRepository;
    private final InventarioSucursalRepository inventarioSucursalRepository;
    private final HistorialRepository historialRepository;

    public DashboardResumenDTO getResumen(Long sucursalId) {
        BigDecimal ventasHoy = ventaRepository.sumVentasPorFecha(sucursalId, LocalDate.now());
        BigDecimal ventasAyer = ventaRepository.sumVentasPorFecha(sucursalId, LocalDate.now().minusDays(1));

        Double variacion = calcularVariacion(ventasHoy, ventasAyer);

        Long stockCriticoLong = inventarioSucursalRepository.countStockCritico(sucursalId);
        Integer stockCritico = stockCriticoLong != null ? stockCriticoLong.intValue() : 0;

        var actividad = historialRepository.findTop10BySucursalIdOrderByFechaDesc(sucursalId)
                .stream()
                .map(h -> new ActividadDTO(h.getAccion(), h.getDescripcion(), h.getFecha()))
                .toList();

        return new DashboardResumenDTO(ventasHoy, variacion, stockCritico, actividad);
    }

    private Double calcularVariacion(BigDecimal hoy, BigDecimal ayer) {
        if (hoy == null) hoy = BigDecimal.ZERO;
        if (ayer == null || ayer.compareTo(BigDecimal.ZERO) == 0) {
            return hoy.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
        }
        BigDecimal diferencia = hoy.subtract(ayer);
        return diferencia.divide(ayer, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100))
                .doubleValue();
    }
}
