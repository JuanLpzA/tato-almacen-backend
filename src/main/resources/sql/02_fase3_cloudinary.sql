-- =====================================================================
-- Ajuste para Fase 3: guardar el public_id de Cloudinary en las fotos
-- de referencia (necesario para poder eliminarlas de Cloudinary despues).
-- Ejecutar UNA VEZ sobre motorepuestos_tato (despues del script 01).
-- =====================================================================

USE motorepuestos_tato;

ALTER TABLE fotos_referencia_producto
    ADD COLUMN IF NOT EXISTS public_id_cloudinary VARCHAR(255) NULL AFTER url_foto;
