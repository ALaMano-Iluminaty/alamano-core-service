# ALaMano Core Service

Servicio de dominio de ALaMano. Aquí viven las reglas de negocio y la API REST. No distribuye eventos por WebSocket: eso lo hace `alamano-realtime-gateway`.

## Capas

- `domain`: modelo y reglas. No usa Spring ni conoce los adaptadores.
- `application`: casos de uso y puertos de entrada y de salida.
- `infrastructure`: adaptadores HTTP, Postgres, Redis y RabbitMQ.

El test `HexagonalArchitectureTest` impide que el dominio dependa de las capas exteriores y que los casos de uso dependan de la infraestructura.

## Ejecutar localmente

Requiere Java 21 y Maven Wrapper. El servicio escucha en el puerto `8082`. El gateway usa el `8083`.

En Windows, desde PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

El health check queda en `http://localhost:8082/actuator/health`.

## HU9: estados del servicio

Flujo permitido: `RESERVED` → `EN_ROUTE` → `ARRIVED` → `IN_PROGRESS` → `COMPLETED`. Desde `RESERVED` o `EN_ROUTE` también se puede pasar a `CANCELLED`.

- `POST /api/services` — crea un servicio en `RESERVED` (provisional hasta HU10/reserva).
- `PATCH /api/services/{id}/status` — cambia el estado; transición inválida → **409**.

Cada cambio válido persiste en Postgres con `UPDATE` condicional y publica `service.status.changed` en el exchange `alamano.events` (mismo sobre que consume el Gateway).

Al crear un servicio, `POST /api/services` guarda primero y publica `service.status.changed` con `previousStatus: null`, `status: "RESERVED"` y `version: 0`.

Permisos para `PATCH /api/services/{id}/status` (el usuario se identifica por el `sub` del JWT):

| Quién | Permiso |
|---|---|
| Vendedor (`sub = professionalId`) | Cualquier transición válida |
| Cliente (`sub = clientId`) | Solo cancelar (`CANCELLED`) desde `RESERVED` o `EN_ROUTE` |
| Otro usuario | Sin permiso: **403** (`service_access_denied`) |

Una transición inválida devuelve **409** (`invalid_transition`); un conflicto de escritura concurrente también devuelve **409** (`status_conflict`).

## Seguridad

Los endpoints requieren `Authorization: Bearer <jwt>`, excepto `/actuator/health` y sus rutas hijas. El Core valida localmente la firma RS256 usando la llave pública compartida con Auth y el Realtime Gateway; no llama al servicio Auth. Por defecto lee `classpath:keys/public.pem`. Se puede cambiar la ubicación con la variable `JWT_PUBLIC_KEY_LOCATION`.

## HU2: vendedores cercanos

`GET /api/professionals/nearby?lat=4.6486&lng=-74.0628&radiusKm=5` devuelve profesionales disponibles dentro del radio, ordenados por distancia. `lat` y `lng` son obligatorios; `radiusKm` es opcional, con valor por defecto de 5 km y máximo de 20 km. La respuesta incluye el identificador, ubicación y distancia en kilómetros redondeada a dos decimales. La lista queda vacía (`[]`) si no hay profesionales disponibles en el área.

Ejemplo de respuesta:

```json
[
  {
    "professionalId": "pro-123",
    "latitude": 4.6576,
    "longitude": -74.0628,
    "distanceKm": 1.0
  }
]
```

Estados de un profesional: `OFFLINE` (desconectado), `AVAILABLE` (conectado y libre; aparece en el mapa) y `BUSY` (con servicio reservado o en curso; no aparece en el mapa).

## HU4: conectarme

`POST /api/professionals/me/online` marca al vendedor como disponible (`AVAILABLE`) en la posición enviada, para que aparezca en la búsqueda de vendedores cercanos.

- **Rol:** solo `PROFESSIONAL`. El id del vendedor sale del `sub` del token; nunca se envía en el cuerpo.
- **Header opcional:** `X-Correlation-Id`. Si no llega, el Core genera uno.
- **Cuerpo:**

```json
{ "latitude": 4.6486, "longitude": -74.0628 }
```

La primera conexión crea al vendedor. Conectarse estando ya disponible es válido (por ejemplo, al recargar la página) y solo actualiza la posición.

| Respuesta | Cuándo |
|---|---|
| **200** | Quedó disponible. Devuelve `professionalId`, `status`, `latitude`, `longitude` y `version`. |
| **400** | Falta `latitude` o `longitude` (`invalid_request`) o están fuera de rango (`invalid_search_area`). |
| **401** | Sin token o con un token inválido. |
| **403** | El token no tiene rol `PROFESSIONAL`. |
| **409** | El vendedor tiene un servicio en curso (`professional_busy`) o fue modificado al mismo tiempo dos veces seguidas (`concurrent_update`). |

Después de guardar, el Core publica `professional.online` en el exchange `alamano.events` (routing key `professional.online`), que el Realtime Gateway reenvía a `/topic/map`:

```json
{
  "eventId": "7d0f2c1e-...",
  "type": "professional.online",
  "schemaVersion": 1,
  "occurredAt": "2026-09-30T12:00:00Z",
  "correlationId": "c0ffee-...",
  "payload": {
    "professionalId": "pro-1",
    "latitude": 4.6486,
    "longitude": -74.0628,
    "status": "AVAILABLE",
    "version": 0
  }
}
```

## HU6: ETA y tracking

El Gateway publica `location.updated` cada vez que recibe una nueva ubicación del vendedor. El Core valida el evento y que el vendedor corresponda al servicio, persiste la ubicación solo si su `recordedAt` es más reciente y después publica `tracking.updated`. La cola durable `core.location-updates` usa `alamano.events.dlx` y la routing key `core.dlq` para eventos rechazados.

| Evento | Emisor | Payload |
|---|---|---|
| `location.updated` | Realtime Gateway | `professionalId`, `serviceId`, `latitude`, `longitude`, `recordedAt` |
| `tracking.updated` | Core | `serviceId`, `professionalId`, `latitude`, `longitude`, `etaSeconds`, `recordedAt` |

El ETA usa la distancia Haversine entre la ubicación recibida y el destino del servicio. Para servicios `RESERVED` o `EN_ROUTE`, se estima el tiempo con la velocidad media configurable `alamano.tracking.average-speed-kmh` (25 km/h por defecto) y se redondea hacia arriba a segundos. En `ARRIVED` o `IN_PROGRESS` el ETA es cero; si no hay destino, es `null`.

Al crear un servicio, `POST /api/services` acepta opcionalmente `destinationLatitude` y `destinationLongitude`; deben enviarse juntas. Si se omiten, la ubicación se sigue guardando y `etaSeconds` queda en `null`. La persistencia compara `last_tracked_at` en el propio `UPDATE`, por lo que una ubicación atrasada no pisa la más nueva.

```json
{
  "eventId": "uuid-nuevo",
  "type": "tracking.updated",
  "schemaVersion": 1,
  "occurredAt": "2026-10-07T21:30:00Z",
  "correlationId": "uuid-de-correlacion",
  "payload": {
    "serviceId": "00000000-0000-0000-0000-000000000337",
    "professionalId": "pro-1",
    "latitude": 4.65,
    "longitude": -74.06,
    "etaSeconds": 720,
    "recordedAt": "2026-10-07T21:29:58Z"
  }
}
```

## HU5: desconectarme

`POST /api/professionals/me/offline` marca al vendedor como `OFFLINE` para que deje de aparecer en `GET /api/professionals/nearby`. El endpoint no recibe cuerpo; el identificador se toma del `sub` del token. Acepta el header opcional `X-Correlation-Id`.

| Respuesta | Cuándo |
|---|---|
| **200** | Quedó desconectado o ya estaba `OFFLINE` (operación idempotente). Devuelve `ProfessionalResponse`. |
| **401** | Falta el token o no es válido. |
| **403** | El token no tiene rol `PROFESSIONAL`. |
| **404** | No existe el vendedor (`professional_not_found`). |
| **409** | El vendedor está `BUSY` (`professional_busy`) o hubo dos conflictos de versión (`concurrent_update`). |

El Core consume el aviso técnico del Gateway en la cola durable `core.professional-connection`. Solo desconecta a vendedores `AVAILABLE` cuya conexión más reciente no sea posterior a la hora del aviso. Si hubo una reconexión posterior, se ignora el aviso antiguo y el vendedor continúa visible en el mapa.

| Evento | Emisor | Significado |
|---|---|---|
| `professional.connection.lost` | Realtime Gateway | Hecho técnico: se perdió el socket del vendedor. El Core lo consume y decide si debe desconectarlo. |
| `professional.disconnected` | Core | Hecho de negocio: el vendedor quedó `OFFLINE`; el Gateway lo distribuye a los clientes del mapa. |

El Core publica `professional.disconnected` después de guardar el cambio, con routing key del mismo nombre. El payload incluye el motivo manual (`MANUAL`) o la pérdida de conexión (`CONNECTION_LOST`):

```json
{
  "eventId": "7d0f2c1e-...",
  "type": "professional.disconnected",
  "schemaVersion": 1,
  "occurredAt": "2026-09-30T12:05:00Z",
  "correlationId": "c0ffee-...",
  "payload": {
    "professionalId": "pro-1",
    "status": "OFFLINE",
    "version": 1,
    "reason": "MANUAL"
  }
}
```


## HU12: publicar promoción con cupos limitados

- `POST /api/promotions` — solo rol `PROFESSIONAL`. Cuerpo: `{"description": "2x1 en cortes", "totalSlots": 50}`. El vendedor sale del `sub` del token. Responde **201** con `id`, `totalSlots` y `availableSlots`.
- `GET /api/promotions/{id}` — cualquier usuario autenticado; `availableSlots` sale del contador de Redis.

Primero se guarda en Postgres (tabla `promotions`, migración `V4`) y después se inicia el contador atómico en Redis con la clave `promotion:{id}:cupos`. Postgres y Redis no comparten transacción: si Redis falla, se borra la promoción recién guardada y la respuesta es **503** (`promotion_counter_unavailable`), así no queda una promoción visible sin cupos. Si además falla el borrado, queda una fila sin contador y la excepción original conserva el error secundario como `suppressed`.

Validaciones (**400**): descripción obligatoria de máximo 500 caracteres y cupos entre 1 y 10000. Un cliente recibe **403**; sin token, **401**.

`PromotionCounterPort` solo tiene `initialize` y `remaining`. HU11 agrega ahí el decremento atómico (script Lua) con la misma clave. Redis se configura con `REDIS_HOST` y `REDIS_PORT`. En el perfil `test` el contador es en memoria (`InMemoryPromotionCounterAdapter`), igual que los publicadores NoOp de RabbitMQ.
