# Tato Almacen Movil - Backend (Fases 1-5 completas)

API REST con Spring Boot 4.1.0 + Java 21, para ser consumida por la app
Android nativa. Usa la misma base de datos MySQL `motorepuestos_tato` del
sistema web de gestion (sin alterar ninguna tabla existente).

## Antes de correr

1. Ejecuta `src/main/resources/sql/01_tablas_nuevas.sql` sobre tu base de
   datos `motorepuestos_tato` (crea las tablas nuevas + datos iniciales:
   rol Almacenero, config_almacen por defecto para tus sucursales).
2. Ejecuta tambien `src/main/resources/sql/02_fase3_cloudinary.sql`
   (agrega la columna `public_id_cloudinary` para poder borrar fotos).
3. Ejecuta tambien `src/main/resources/sql/03_fase5_almacen.sql`
   (agrega `sucursal_id` a `dispositivos_notificacion` para poder
   notificar solo a los almaceneros de la sucursal correcta).
4. Edita `src/main/resources/application.properties`:
   - `spring.datasource.password`
   - `jwt.secret` (pon algo largo y aleatorio en produccion)
   - `cloudinary.cloud_name` / `cloudinary.api_key` / `cloudinary.api_secret`
     (puedes usar las mismas credenciales de tu proyecto Altoque)
   - `firebase.credentials.path` (opcional, ver seccion de Firebase mas abajo)
5. `mvn spring-boot:run`

## Dependencias (Spring Initializr) usadas en este proyecto

- Spring Web
- Spring Data JPA
- Validation
- MySQL Driver
- Lombok
- (agregadas manualmente en el pom, no estan en el wizard):
  spring-security-crypto (solo BCrypt), jjwt-api/impl/gson 0.11.5

## Endpoints de la Fase 1 (auth + dashboard)

- `POST /auth/login` `{ "correo": "...", "password": "..." }`
  -> usuario + lista de sucursales activas
- `POST /auth/sucursal` `{ "usuarioId": 1, "sucursalId": 1 }`
  -> JWT + datos de sesion
- `GET /api/dashboard/resumen` (header `Authorization: Bearer <token>`)
  -> ventas del dia, % variacion, stock critico, actividad reciente

## Endpoints de la Fase 2 (Identificar IA + ubicaciones)

Todos requieren `Authorization: Bearer <token>` salvo que se indique lo contrario.

- `POST /api/admin/productos/{id}/generar-perfil-ia` (multipart, campo `fotos` con
  1 o mas imagenes reales del producto) -> genera con Gemini una descripcion visual
  detallada y la guarda en `perfiles_ia_producto`. Correr esto una vez por cada
  producto antes de usar Identificar IA (mejora mucho la precision; si no se corre,
  el sistema usa como respaldo `productos.descripcion`/`nombre`/`marca`).

- `POST /api/identificar` (multipart, campo `foto`) -> toma la sucursal del token,
  arma el catalogo de esa sucursal (con perfil IA si existe) y devuelve el top 3
  de coincidencias `[{ idProducto, nombre, stock, porcentaje, estado }]`.
  `estado` es `Coincidencia` (>=80%), `Alternativa` (<80%) o `Sin stock`.

- `GET /api/productos/{id}/ubicacion` (opcional `?sucursalId=`, si no se manda
  usa la del token) -> `{ productoId, estante, fila, columna, abreviatura }`.
  Devuelve 404 con mensaje claro si el producto no tiene ubicacion asignada.

- `POST /api/admin/ubicaciones` `{ productoId, sucursalId, estante, fila, columna }`
  -> crea o actualiza la ubicacion de un producto. Valida contra el grid
  configurado en `config_almacen` (si existe) para no permitir coordenadas
  fuera de rango.

- `GET /api/admin/config-almacen/{sucursalId}` -> tamaño del grid de esa sucursal.
- `PUT /api/admin/config-almacen` `{ sucursalId, totalEstantes, totalFilas, totalColumnas }`
  -> crea o actualiza el tamaño del grid (ya viene precargado 5x3x5 por el script SQL).

## Endpoints de la Fase 3 (Asistente de voz + fotos de referencia)

- `POST /api/asistente/consultar` `{ "texto": "¿dónde está el aceite Repsol?" }`
  -> intenta un matching directo por texto contra el catalogo de la sucursal
  (rapido, sin costo de IA); si es ambiguo o no encuentra nada, cae a Gemini
  para interpretar la intencion. Responde:
  `{ textoRespuesta, productoId, nombreProducto, stock, ubicacion }` listo
  para que el APK lo lea con TextToSpeech. La transcripcion voz->texto la
  hace la app Android de forma nativa (`SpeechRecognizer`), este endpoint
  solo recibe texto plano.

- `POST /api/admin/productos/{id}/fotos-referencia` (multipart, campo `fotos`)
  -> sube fotos a Cloudinary (carpeta `tato-almacen/productos/{id}`) y las
  guarda en `fotos_referencia_producto`.
- `GET /api/admin/productos/{id}/fotos-referencia` -> lista las fotos guardadas.
- `DELETE /api/admin/productos/{id}/fotos-referencia/{fotoId}` -> elimina la
  foto de Cloudinary y de la BD.
- `POST /api/admin/productos/{id}/generar-perfil-ia` ahora tiene dos modos:
  - Con campo `fotos` (multipart): sube esas fotos a Cloudinary como
    referencia Y genera el perfil con ellas.
  - Sin `fotos`: regenera el perfil usando las fotos de referencia que el
    producto ya tenga guardadas (no hace falta volver a fotografiar).

## Endpoints de la Fase 4 (Registrar Compra)

Usa las tablas EXISTENTES `compras`/`detalle_compras` -- no se crean tablas
nuevas para esto.

- `POST /api/compras/analizar` (multipart: `foto`, `tipoCaptura` = `ETIQUETA`
  o `BOLETA`) -> **solo lectura**, no guarda nada todavia. Gemini extrae los
  items (nombre, marca, cantidad, precio si es visible) y por cada uno se
  busca el producto mas parecido en TODA la BD (similitud de texto por
  tokens, sin IA para esto -- rapido y gratis). Devuelve una lista donde
  cada item indica si hay match (`productoExistente: true` + `productoIdSugerido`)
  o si parece nuevo (`productoExistente: false`), para que el usuario
  confirme/edite en la app antes de guardar.

- `POST /api/compras/confirmar` -> **escritura real**. Recibe la lista ya
  revisada (con la decision final por item: usar producto existente o crear
  uno nuevo con su categoria). Crea productos nuevos si hace falta (codigo
  interno autogenerado tipo `AUTO-XXXXXXXX` para no chocar con la numeracion
  manual `0001, 0002...`), guarda la compra en `compras`/`detalle_compras`,
  suma el stock en `inventario_sucursal` (o crea la fila si el producto no
  tenia inventario en esa sucursal) y registra en `historial`. Calcula
  subtotal/IGV/total automaticamente segun `incluyeIgv` (IGV Peru 18%).

## Endpoints de la Fase 5 (módulo Almacén + notificaciones push)

- `POST /api/admin/pedidos-almacen/simular?clienteNombre=...` -> **temporal**,
  para probar el módulo aislado mientras el sistema web de gestión (que en
  el futuro creará estos pedidos automáticamente al registrar una venta
  presencial) no está implementado todavía. Crea un pedido `PENDIENTE` con
  1-3 productos al azar (con stock > 0) de tu sucursal, y dispara la
  notificación push a los almaceneros conectados a esa sucursal.

- `GET /api/almacen/pedidos?estado=PENDIENTE` -> lista pedidos de tu
  sucursal (estado opcional: `PENDIENTE`, `ASIGNADO`, `ENTREGADO`, etc.)

- `GET /api/almacen/pedidos/{id}` -> detalle completo (productos,
  cantidades, ubicación snapshot, si cada ítem ya fue recogido).

- `POST /api/almacen/pedidos/{id}/asignarme` -> auto-asignación **atómica**
  (`UPDATE ... WHERE estado='PENDIENTE'`): si dos almaceneros lo tocan al
  mismo tiempo, solo uno gana; el otro recibe 409 "ya fue tomado por otro
  almacenero".

- `POST /api/almacen/pedidos/{id}/detalle/{detalleId}/marcar-recogido`

- `POST /api/almacen/pedidos/{id}/entregar?forzar=false` -> valida que
  todos los ítems estén recogidos (salvo `forzar=true`), descuenta
  `inventario_sucursal.stock`, marca `ENTREGADO` y registra en `historial`.

- `POST /api/dispositivos/registrar` `{ "tokenPush": "...", "plataforma": "ANDROID" }`
  -> registra/renueva el token FCM del dispositivo, asociado a la sucursal
  actual del token JWT. Llamar despues de cada login exitoso desde la app.

**Seguridad**: todos los endpoints de pedidos validan que el pedido
pertenezca a la sucursal del usuario autenticado (403 si no).

### Configurar Firebase (para que las notificaciones push funcionen)

1. Ve a [Firebase Console](https://console.firebase.google.com), crea un
   proyecto (gratis).
2. Configuración del proyecto -> Cuentas de servicio -> "Generar nueva
   clave privada" -> descarga el JSON.
3. Copia ese archivo a la raíz del proyecto backend como
   `firebase-service-account.json` (o cambia la ruta en
   `firebase.credentials.path`).
4. **No es bloqueante**: si el archivo no existe, la app arranca igual
   (verás un warning en el log) y todo el módulo Almacén funciona excepto
   el envío de push. Puedes desarrollar/probar todo lo demás sin Firebase
   configurado.

## Correcciones aplicadas en la revisión final

- **`AsistenteVozService`**: `encontrado` se reasignaba y luego se capturaba
  dentro de un lambda (`.map(u -> ...)`), lo cual no compila en Java
  (*"variable used in lambda expression should be final or effectively
  final"*). Se introdujo una variable `final` intermedia.
- **`PedidoAlmacenRepository.asignarSiPendiente`**: el `UPDATE` JPQL hacía
  `SET p.almacenero.id = :usuarioId`, lo cual **no es JPQL válido** (no se
  puede navegar al campo de una asociación en el SET de un UPDATE masivo,
  solo se puede asignar la asociación completa). Se cambió a
  `SET p.almacenero = :almacenero` recibiendo un `Usuario` (obtenido con
  `getReferenceById`, que no dispara un SELECT extra).
- **`InventarioSucursalRepository.countStockCritico`**: `COUNT(i)` en JPQL
  siempre devuelve `Long`, pero el metodo estaba declarado como `Integer`.
  Se cambio a `Long` y se convierte explicitamente en `DashboardService`
  para no depender de conversion implicita.

## Endpoints adicionales (Categorías)

- `GET /api/categorias` -> `[{ id, nombre }]`. Usado por Registrar Compra
  en la app Android para que el usuario elija categoría al crear un
  producto nuevo.

## Notas importantes

- `ddl-auto=none`: Hibernate JAMAS modifica la BD, solo lee la estructura.
  Las tablas nuevas se crean a mano con el script SQL.
- No se especifica `hibernate.dialect` a proposito: Hibernate 7 lo
  autodetecta por la URL JDBC. Ponerlo manual con el nombre de una version
  vieja (ej. `MySQL8Dialect`) rompe el arranque.
- El interceptor JWT protege todo `/api/**`. `/auth/**` queda libre.
- `gemini.api.key` reutiliza la misma key de tu proyecto Altoque. Si la
  rotas allá, actualizala tambien aca.
- `GeminiClient` es generico (recibe systemInstruction + texto + lista de
  imagenes en bytes) para poder reutilizarlo en las fases 3 y 4 sin
  duplicar codigo de llamada a la API.
