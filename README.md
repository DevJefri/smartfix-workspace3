SmartFix — Sistema de Gestión de Reparaciones
Microservicios con Spring Boot 3 · PostgreSQL · JWT · Docker

Spring Boot 4.0.6  |  Java 25  |  PostgreSQL 16  |  Docker  |  JWT  |  Flyway

1. Problemática
El servicio técnico de celulares SmartFix gestionaba sus órdenes de reparación de forma manual mediante hojas de cálculo. Esto generaba los siguientes problemas:

Falta de trazabilidad: el cliente no conocía el estado real de su equipo.
Acoplamiento de datos: la información del cliente y del equipo estaban mezcladas, dificultando la gestión de clientes recurrentes.
Errores operativos: pérdida de información sobre fallas específicas y demoras en la entrega por falta de una base de datos centralizada.

2. Solución Implementada
Sistema backend basado en arquitectura de microservicios, donde cada servicio tiene su propia base de datos, su propia lógica y se comunica con los demás a través de HTTP REST.

3. Arquitectura

Microservicio
Puerto
Responsabilidad
Base de Datos
ms-cliente
8081
Registro y gestión de clientes + autenticación JWT
db_smartfix_clientes (PostgreSQL)
ms-reparacion
8082
Gestión de órdenes de reparación, valida RUT en ms-cliente
db_smartfix_reparaciones (PostgreSQL)


Flujo de una petición
El técnico hace login en ms-cliente y obtiene un token JWT.
Con el token, puede crear un cliente (POST /api/customers).
Luego crea una reparación en ms-reparacion (POST /api/reparaciones).
ms-reparacion consulta a ms-cliente si el RUT existe antes de guardar la ficha.
Si el RUT no existe, retorna error 404 con mensaje descriptivo.

4. Requerimientos Funcionales

ID
Requerimiento
Endpoint
RF1
Registro de cliente con RUT, nombre, teléfono y email
POST /api/customers
RF2
Ingreso de orden técnica vinculando cliente, modelo y descripción de falla
POST /api/reparaciones
RF3
Consulta de estado de reparación por RUT de cliente
GET /api/reparaciones/cliente/{rut}
RF4
Autenticación de técnicos (login/register)
POST /api/auth/login, /api/auth/register


5. Stack Tecnológico

Tecnología
Versión
Uso
Java
25 (LTS)
Lenguaje principal
Spring Boot
4.0.6
Framework base
Spring Security + JWT
JJWT 0.12.6
Autenticación stateless
Spring Data JPA
3.3.5
Persistencia con Hibernate
Flyway
10.x
Migraciones de base de datos
PostgreSQL
16
Motor de base de datos
Docker + Docker Compose
Latest
Contenedores y orquestación
Gradle
9.4.1
Build tool



6. Estructura del Proyecto

smartfix-workspace3/
├── docker-compose.yml
├── ms-cliente/
│   ├── Dockerfile
│   ├── build.gradle
│   └── src/main/java/cl/ms_cliente/
│       ├── controller/   AuthController, ClienteController
│       ├── dto/          AuthRequestDTO, AuthResponseDTO, ClienteRequestDTO, ClienteResponseDTO
│       ├── exception/    GlobalExceptionHandler, RecursoNoEncontradoException, RutDuplicadoException
│       ├── model/        Cliente, Usuario
│       ├── repository/   ClienteRepository, UsuarioRepository
│       ├── security/     JwtUtil, JwtFilter, SecurityConfig
│       └── service/      AuthService, ClienteService, ClientServiceImpl
└── ms-reparacion/
    ├── Dockerfile
    ├── build.gradle
    └── src/main/java/cl/ms_reparacion/
        ├── controller/   ReparacionController
        ├── dto/          ReparacionRequestDTO, ReparacionResponseDTO
        ├── exception/    GlobalExceptionHandler, RecursoNoEncontradoException, ClienteNoEncontradoException
        ├── model/        Reparacion
        ├── repository/   ReparacionRepository
        ├── security/     JwtUtil, JwtFilter, SecurityConfig
        └── service/      ClienteVerificacionService, ReparacionService, ReparacionServiceImpl

7. Levantar el Proyecto

Prerrequisitos
Docker Desktop instalado y corriendo
Git (para clonar el repositorio)

Comandos

# 1. Clonar el repositorio
git clone https://github.com/tu-usuario/smartfix-workspace3.git
cd smartfix-workspace3

# 2. Levantar todo el sistema
docker-compose up --build

# 3. Verificar que los servicios están corriendo
docker ps

Una vez levantado:
ms-cliente disponible en: http://localhost:8081
ms-reparacion disponible en: http://localhost:8082
db-cliente PostgreSQL en: localhost:5432
db-reparacion PostgreSQL en: localhost:5433

8. Guía de Pruebas en Postman

Sigue este orden exacto para probar el sistema completo.

8.1 Autenticación (ms-cliente · puerto 8081)

Registrar usuario técnico
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "tecnico1",
  "password": "password123"
}

Respuesta esperada (200 OK):
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "tecnico1",
  "role": "USER"
}

Login
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "username": "tecnico1",
  "password": "password123"
}

Copia el token de la respuesta. Lo usarás en todos los endpoints siguientes como:
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...

Validar protección — sin token (debe rechazar)
GET http://localhost:8081/api/customers
(sin header Authorization)

Respuesta esperada: 401 Unauthorized — confirma que el sistema bloquea accesos sin token.

8.2 CRUD de Clientes (ms-cliente · puerto 8081)
Agrega en todos: Authorization: Bearer <tu_token>

Crear cliente
POST http://localhost:8081/api/customers
{
  "rut": "12345678-9",
  "nombre": "Juan Pérez",
  "telefono": "+56912345678",
  "email": "juan@email.com"
}

Respuesta esperada: 201 Created

Listar todos los clientes
GET http://localhost:8081/api/customers
Respuesta esperada: 200 OK con array de clientes.

Buscar cliente por RUT
GET http://localhost:8081/api/customers/12345678-9
Respuesta esperada: 200 OK con datos del cliente.

Actualizar cliente
PUT http://localhost:8081/api/customers/12345678-9
{
  "rut": "12345678-9",
  "nombre": "Juan Pérez Actualizado",
  "telefono": "+56987654321",
  "email": "juan.nuevo@email.com"
}
Respuesta esperada: 200 OK con datos actualizados.

Eliminar cliente
DELETE http://localhost:8081/api/customers/12345678-9
Respuesta esperada: 204 No Content.

8.3 Probar Validaciones

RUT con formato inválido
POST http://localhost:8081/api/customers
{
  "rut": "INVALIDO",
  "nombre": "Test"
}
Respuesta esperada: 400 Bad Request con mensaje de error descriptivo.

RUT duplicado
// Crea el cliente 12345678-9 dos veces
POST http://localhost:8081/api/customers (mismo RUT)
Respuesta esperada: 409 Conflict.

RUT no encontrado
GET http://localhost:8081/api/customers/99999999-9
Respuesta esperada: 404 Not Found con mensaje descriptivo.

8.4 Reparaciones (ms-reparacion · puerto 8082)
Agrega en todos: Authorization: Bearer <tu_token> (el mismo token generado en ms-cliente)

Crear reparación — RUT existente (debe funcionar)
Primero asegúrate de tener creado el cliente 12345678-9 en ms-cliente.

POST http://localhost:8082/api/reparaciones
{
  "rutCliente": "12345678-9",
  "modelo": "iPhone 14 Pro",
  "descripcion": "Pantalla rota y batería dañada"
}
Respuesta esperada: 201 Created con estado RECIBIDO.

Crear reparación — RUT inexistente (debe rechazar)
POST http://localhost:8082/api/reparaciones
{
  "rutCliente": "99999999-9",
  "modelo": "Samsung S24",
  "descripcion": "No enciende"
}
Respuesta esperada: 404 Not Found — el sistema consultó ms-cliente y no encontró el RUT.

Listar todas las reparaciones
GET http://localhost:8082/api/reparaciones
Respuesta esperada: 200 OK con lista de reparaciones.

Listar reparaciones por RUT de cliente
GET http://localhost:8082/api/reparaciones/cliente/12345678-9
Respuesta esperada: 200 OK con reparaciones del cliente.

Actualizar estado de reparación
PATCH http://localhost:8082/api/reparaciones/1/estado?nuevoEstado=EN_PROCESO
Estados válidos sugeridos: RECIBIDO, EN_PROCESO, TERMINADO, ENTREGADO
Respuesta esperada: 200 OK con estado actualizado.

9. Variables de Entorno

Variable
Valor por defecto
Descripción
SPRING_DATASOURCE_URL
jdbc:postgresql://db-cliente:5432/db_smartfix_clientes
URL de conexión a PostgreSQL
SPRING_DATASOURCE_USERNAME
postgres
Usuario de la base de datos
SPRING_DATASOURCE_PASSWORD
postgres
Contraseña de la base de datos
JWT_SECRET
smartfix-secret-key-256-bits-minimo-para-hs256-aqui
Clave secreta para firmar tokens JWT
JWT_EXPIRATION_MS
3600000
Expiración del token (1 hora en ms)
SERVICES_CLIENTE_URL
http://ms-cliente:8081
URL interna de ms-cliente (solo ms-reparacion)


10. Autor

Proyecto desarrollado como parte de la asignatura de Desarrollo Full Stack.
Sistema: SmartFix — Gestión de Reparaciones de Celulares
Arquitectura: Microservicios con Spring Boot 3
Año: 2025
