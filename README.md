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
