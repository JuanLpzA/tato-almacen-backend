package com.tato.almacen.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "compras")
@Getter
@Setter
public class Compra {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "condicion_pago")
    private String condicionPago;

    @Column(name = "fecha_emision")
    private LocalDate fechaEmision;

    @Column(name = "fecha_vencimiento")
    private LocalDate fechaVencimiento;

    private BigDecimal igv;

    @Column(name = "incluye_igv")
    private Boolean incluyeIgv;

    private String moneda;

    @Column(name = "numero_comprobante")
    private String numeroComprobante;

    private String observacion;

    @Column(name = "razon_social_proveedor")
    private String razonSocialProveedor;

    @Column(name = "ruc_proveedor")
    private String rucProveedor;

    private String serie;
    private BigDecimal subtotal;

    @Column(name = "tipo_cambio")
    private BigDecimal tipoCambio;

    @Column(name = "tipo_documento")
    private String tipoDocumento;

    private BigDecimal total;

    @ManyToOne
    @JoinColumn(name = "sucursal_id")
    private Sucursal sucursal;

    @ManyToOne
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
