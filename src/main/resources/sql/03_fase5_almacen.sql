-- =====================================================================
-- Ajuste para Fase 5 (modulo Almacen + notificaciones push).
-- Ejecutar UNA VEZ sobre motorepuestos_tato (despues de 01 y 02).
-- =====================================================================

USE motorepuestos_tato;

-- Se necesita saber a que sucursal esta "conectado" cada dispositivo para
-- notificar solo a los almaceneros de la sucursal correcta. Se actualiza
-- cada vez que el usuario registra/renueva su token push desde la app
-- (despues de seleccionar sucursal en el login).
ALTER TABLE dispositivos_notificacion
    ADD COLUMN IF NOT EXISTS sucursal_id BIGINT NULL AFTER usuario_id,
    ADD CONSTRAINT IF NOT EXISTS FK_dispositivo_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales(id);
