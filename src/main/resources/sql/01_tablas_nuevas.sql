        -- =====================================================================
        -- Tato Almacen Movil - Tablas NUEVAS (no se altera ninguna tabla existente)
        -- Ejecutar sobre la base de datos motorepuestos_tato ya existente.
        -- =====================================================================

        USE motorepuestos_tato;

        -- Redirect configurable (por si mas adelante se reutiliza para otra cosa)
        CREATE TABLE IF NOT EXISTS configuracion_app (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            clave VARCHAR(100) NOT NULL UNIQUE,
            valor VARCHAR(500) NOT NULL
        );

        -- Configuracion del grid de almacen por sucursal (Estante x Fila x Columna)
        CREATE TABLE IF NOT EXISTS config_almacen (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            sucursal_id BIGINT NOT NULL UNIQUE,
            total_estantes INT NOT NULL,
            total_filas INT NOT NULL,
            total_columnas INT NOT NULL,
            CONSTRAINT FK_config_almacen_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales(id)
        );

        -- Ubicacion fisica de cada producto dentro del almacen de una sucursal
        CREATE TABLE IF NOT EXISTS ubicaciones_producto (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            producto_id BIGINT NOT NULL,
            sucursal_id BIGINT NOT NULL,
            estante INT NOT NULL,
            fila INT NOT NULL,
            columna INT NOT NULL,
            UNIQUE KEY uq_producto_sucursal (producto_id, sucursal_id),
            CONSTRAINT FK_ubicacion_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
            CONSTRAINT FK_ubicacion_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales(id)
        );

        -- Perfil generado por IA para poder comparar una foto contra el catalogo
        CREATE TABLE IF NOT EXISTS perfiles_ia_producto (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            producto_id BIGINT NOT NULL UNIQUE,
            descripcion_ia TEXT NOT NULL,
            palabras_clave VARCHAR(500),
            fecha_generado DATETIME(6) NOT NULL,
            CONSTRAINT FK_perfil_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
        );

        -- Fotos de referencia adicionales de un producto (para generar el perfil IA)
        CREATE TABLE IF NOT EXISTS fotos_referencia_producto (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            producto_id BIGINT NOT NULL,
            url_foto VARCHAR(500) NOT NULL,
            es_principal BIT(1) NOT NULL DEFAULT b'0',
            fecha_subida DATETIME(6) NOT NULL,
            CONSTRAINT FK_foto_ref_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
        );

        -- Tokens FCM para notificaciones push (modulo Almacen, fase 5)
            CREATE TABLE IF NOT EXISTS dispositivos_notificacion (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            usuario_id BIGINT NOT NULL,
            token_push VARCHAR(500) NOT NULL,
            plataforma VARCHAR(20) NOT NULL DEFAULT 'ANDROID',
            activo BIT(1) NOT NULL DEFAULT b'1',
            fecha_registro DATETIME(6) NOT NULL,
            CONSTRAINT FK_dispositivo_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
        );

        -- Pedidos internos que un almacenero debe preparar/entregar (fase 5)
        CREATE TABLE IF NOT EXISTS pedidos_almacen (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            venta_id BIGINT NULL,
            sucursal_id BIGINT NOT NULL,
            almacenero_id BIGINT NULL,
            estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
            cliente_nombre VARCHAR(255),
            observaciones VARCHAR(500),
            fecha_creacion DATETIME(6) NOT NULL,
            fecha_asignacion DATETIME(6) NULL,
            fecha_entrega DATETIME(6) NULL,
            CONSTRAINT FK_pedido_almacen_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
            CONSTRAINT FK_pedido_almacen_sucursal FOREIGN KEY (sucursal_id) REFERENCES sucursales(id),
            CONSTRAINT FK_pedido_almacen_usuario FOREIGN KEY (almacenero_id) REFERENCES usuarios(id)
        );

        CREATE TABLE IF NOT EXISTS detalle_pedido_almacen (
            id BIGINT AUTO_INCREMENT PRIMARY KEY,
            pedido_almacen_id BIGINT NOT NULL,
            producto_id BIGINT NOT NULL,
            cantidad INT NOT NULL,
            ubicacion_snapshot VARCHAR(50),
            recogido BIT(1) NOT NULL DEFAULT b'0',
            CONSTRAINT FK_detalle_pedido_almacen FOREIGN KEY (pedido_almacen_id) REFERENCES pedidos_almacen(id),
            CONSTRAINT FK_detalle_pedido_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
        );

        -- =====================================================================
        -- Datos iniciales
        -- =====================================================================

        INSERT INTO configuracion_app (clave, valor) VALUES
        ('url_redirect_desktop', 'https://tato-motorepuestos.com')
        ON DUPLICATE KEY UPDATE valor = VALUES(valor);

        -- Rol nuevo para almaceneros (dato, no altera la estructura de roles)
        INSERT INTO roles (nombre, activo, permiso_usuarios, permiso_roles, permiso_productos, permiso_categorias,
            permiso_sucursales, permiso_stocks, permiso_traslados, permiso_historial, permiso_compras_ingresar,
            permiso_compras_registro, permiso_ventas_realizar, permiso_ventas_registro, permiso_clientes,
            permiso_web, permiso_cajas, permiso_cajasadmin)
        SELECT 'Almacenero', b'1', b'0',b'0',b'0',b'0',b'0',b'1',b'0',b'0',b'1',b'1',b'0',b'0',b'0',b'0',b'0',b'0'
        WHERE NOT EXISTS (SELECT 1 FROM roles WHERE nombre = 'Almacenero');

        -- Grid de almacen por defecto para las sucursales que ya existen
        -- (5 estantes x 3 filas x 5 columnas -- ajustar despues segun el almacen real)
        INSERT INTO config_almacen (sucursal_id, total_estantes, total_filas, total_columnas)
        SELECT id, 5, 3, 5 FROM sucursales
        WHERE id NOT IN (SELECT sucursal_id FROM config_almacen);
