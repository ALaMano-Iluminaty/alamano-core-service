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
