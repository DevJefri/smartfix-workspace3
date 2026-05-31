# SmartFix — Sistema de Gestión de Reparaciones de Celulares

---

## El Problema

Los talleres de reparación de celulares gestionan sus órdenes de trabajo de forma manual, usando hojas de cálculo o cuadernos físicos. Esto genera problemas críticos:

- **Sin trazabilidad:** el técnico no sabe en qué estado están las reparaciones anteriores del cliente.
- **Datos mezclados:** la información del cliente y del equipo están en el mismo registro, dificultando buscar clientes recurrentes.
- **Errores operativos:** pérdida de información sobre fallas específicas, modelos incorrectos registrados, y demoras por no tener una base de datos centralizada.
- **Sin validación:** se crean fichas de reparación para clientes que no existen en el sistema.

---

## La Solución

SmartFix es una **plataforma de microservicios** que separa la gestión de clientes de la gestión de reparaciones, permitiendo escalar cada componente de forma independiente:

| Servicio | Responsabilidad | Puerto |
|---|---|---|
| **ms-cliente** | Registro y gestión de clientes + autenticación JWT | 8081 |
| **ms-reparacion** | Gestión de fichas de reparación, valida RUT contra ms-cliente | 8082 |

**¿Por qué microservicios?** Si el volumen de reparaciones crece exponencialmente, solo se escala `ms-reparacion`. Si se necesita mejorar la autenticación, solo se toca `ms-cliente`. Cada servicio tiene su propia base de datos y puede desplegarse de forma independiente.

---

## Comunicación entre Microservicios

`ms-cliente` genera tokens JWT al hacer login. `ms-reparacion` los valida usando el mismo secret compartido. Además, antes de crear una ficha de reparación, `ms-reparacion` consulta a `ms-cliente` para verificar que el RUT del cliente existe:
## Comunicación entre Microservicios

```
  ┌─────────────┐        ①  POST /api/auth/login         ┌──────────────────────┐
  │             │ ──────────────────────────────────────► │                      │
  │             │                                         │     ms-cliente       │
  │             │ ◄──────────────────────────────────────  │      :8081           │
  │             │        ②  { token: "eyJ..." }           │                      │
  │             │                                         └──────────────────────┘
  │  TÉCNICO    │
  │             │        ③  POST /api/reparaciones        ┌──────────────────────┐
  │             │            Bearer: eyJ...               │                      │
  │             │ ──────────────────────────────────────► │   ms-reparacion      │
  │             │                                         │      :8082           │
  └─────────────┘                                         │                      │
                                                          │  ④ Valida JWT        │
                                                          │    (mismo secret)    │
                                                          │                      │
                                                          │  ⑤ GET /api/         │
                                                          │    customers/{rut} ──┼──►  ms-cliente :8081
                                                          │                      │◄──  200 OK / 404
                                                          │  ⑥ Si RUT existe:   │
                                                          │    Guarda ficha      │
                                                          └──────────────────────┘
                                                                    │
                                                          ⑦ 201 Created
                                                            { id, rutCliente,
                                                              modelo, estado }
```

### Detalle de cada paso

| Paso | Quién | Acción | Resultado |
|---|---|---|---|
| **①** | Técnico → ms-cliente | `POST /api/auth/login` con username y password | Autenticación contra BD de usuarios |
| **②** | ms-cliente → Técnico | Devuelve `{ token, username, role }` | El técnico guarda el JWT |
| **③** | Técnico → ms-reparacion | `POST /api/reparaciones` con `Bearer <token>` en el header | Solicitud de nueva ficha técnica |
| **④** | ms-reparacion interno | `JwtFilter` valida la firma del token con el secret compartido | Sin consultar ninguna base de datos |
| **⑤** | ms-reparacion → ms-cliente | `GET /api/customers/{rut}` por HTTP interno | Verifica si el RUT existe en ms-cliente |
| **⑥** | ms-reparacion interno | Si RUT existe (200 OK): guarda la ficha. Si no (404): lanza error | Validación de integridad entre servicios |
| **⑦** | ms-reparacion → Técnico | Devuelve la ficha creada con estado `RECIBIDO` | Flujo completado |

### ¿Qué pasa si el RUT no existe?

```
  ms-reparacion                ms-cliente
       │                            │
       │── GET /api/customers/99── ►│
       │                            │ RUT no encontrado en BD
       │◄── 404 Not Found ──────────│
       │
       │ Lanza ClienteNoEncontradoException
       │
       ▼
  {
    "status": 404,
    "error": "Not Found",
    "mensaje": "No existe cliente con RUT 99999999-9.
                Registre al cliente primero."
  }
```

---

SmartFix es una plataforma de microservicios construida con **Spring Boot**, **Java 21** y **Gradle** que gestiona clientes y órdenes de reparación de celulares. Está compuesta por dos servicios completamente independientes con sus propias bases de datos PostgreSQL, orquestados con Docker Compose.

---

## Tabla de Contenidos

1. [Arquitectura General](#1-arquitectura-general)
2. [Requisitos Previos](#2-requisitos-previos)
3. [Estructura del Proyecto](#3-estructura-del-proyecto)
4. [Microservicio: ms-cliente](#4-microservicio-ms-cliente)
5. [Microservicio: ms-reparacion](#5-microservicio-ms-reparacion)
6. [Cómo Levantar el Sistema](#6-cómo-levantar-el-sistema)
7. [Flujo de Uso Completo](#7-flujo-de-uso-completo)
8. [Pruebas con REST Client (VS Code)](#8-pruebas-con-rest-client-vs-code)
9. [Seguridad y JWT](#9-seguridad-y-jwt)
10. [Referencia Completa de Endpoints](#10-referencia-completa-de-endpoints)
11. [Variables de Entorno](#11-variables-de-entorno)
12. [Migraciones de Base de Datos (Flyway)](#12-migraciones-de-base-de-datos-flyway)
13. [Preguntas Frecuentes](#13-preguntas-frecuentes)

---

## 1. Arquitectura General

```
┌──────────────────────────────────┐
                    │     TÉCNICO / CLIENTE             │
                    │  (Postman · REST Client · App)    │
                    └────────────┬─────────────┬────────┘
                                 │             │
                      JWT Auth   │             │   JWT Auth
                                 ▼             ▼
            ┌────────────────────────┐   ┌────────────────────────┐
            │      ms-cliente        │   │    ms-reparacion        │
            │      puerto 8081       │   │      puerto 8082        │
            │                        │   │                         │
            │  ▸ Register / Login    │◄──│  ▸ Verifica RUT         │
            │  ▸ CRUD Clientes       │   │  ▸ Crear ficha técnica  │
            │  ▸ Emite JWT           │   │  ▸ Actualizar estado    │
            └──────────┬─────────────┘   └──────────┬─────────────┘
                       │                             │
                       │ Flyway                      │ Flyway
                       ▼                             ▼
            ┌────────────────────┐       ┌────────────────────────┐
            │  db_smartfix_      │       │  db_smartfix_          │
            │    clientes        │       │    reparaciones        │
            │                    │       │                        │
            │  • clientes        │       │  • reparaciones        │
            │  • usuarios        │       │                        │
            │  PostgreSQL :5432  │       │  PostgreSQL :5433      │
            └────────────────────┘       └────────────────────────┘
```

### Resumen de servicios

| | ms-cliente | ms-reparacion |
|---|---|---|
| **Puerto** | `8081` | `8082` |
| **Base de datos** | `db_smartfix_clientes` | `db_smartfix_reparaciones` |
| **Tablas** | `clientes`, `usuarios` | `reparaciones` |
| **Responsabilidad** | Registro de clientes + autenticación JWT | Fichas técnicas de reparación |
| **Endpoints públicos** | `/api/auth/**`, `/api/customers/{rut}` | Ninguno |
| **Genera JWT** | ✅ Sí | ❌ No |
| **Valida JWT** | ✅ Sí | ✅ Sí |
| **Comunica con** | — | `ms-cliente` (valida RUT por HTTP) |
| **Migraciones** | V1 clientes · V2 usuarios | V1 reparaciones |

### Regla de oro

> `ms-reparacion` **nunca** accede directamente a la base de datos de clientes.
> Para verificar si un RUT existe, hace una llamada HTTP a `ms-cliente`.
> Esto garantiza que cada servicio sea dueño exclusivo de sus datos.

**¿Por qué dos bases de datos separadas?**
Cada microservicio es dueño de sus datos. `ms-reparacion` no consulta directamente la base de datos de clientes: llama al API de `ms-cliente` por HTTP para verificar si un RUT existe. Esto garantiza que los servicios estén desacoplados y puedan evolucionar de forma independiente.

---

## 2. Requisitos Previos

| Herramienta | Versión mínima | Cómo verificar |
|---|---|---|
| **Docker Desktop** | 4.x | `docker --version` |
| **Git** | cualquiera | `git --version` |

> **No necesitas Java ni PostgreSQL instalados localmente.** Docker se encarga de todo: compila el código Java dentro del contenedor y levanta PostgreSQL automáticamente.

### Instalación de Docker Desktop

- **Windows / macOS:** Descarga desde [docker.com/products/docker-desktop](https://www.docker.com/products/docker-desktop/)
- **Linux:** Sigue la guía oficial para tu distribución en [docs.docker.com](https://docs.docker.com/engine/install/)

Verifica la instalación:
```bash
docker --version
docker-compose --version
```

---

## 3. Estructura del Proyecto

```
smartfix-workspace3/
├── docker-compose.yml                      ← Orquesta todos los servicios
│
├── ms-cliente/                             ← Microservicio de clientes (puerto 8081)
│   ├── Dockerfile                          ← Build multietapa: compila y ejecuta
│   ├── build.gradle                        ← Dependencias del proyecto
│   ├── settings.gradle
│   ├── gradlew / gradlew.bat
│   └── src/main/java/cl/ms_cliente/
│       ├── MsClienteApplication.java       ← Punto de entrada
│       ├── controller/
│       │   ├── AuthController.java         ← POST /api/auth/register y /login
│       │   └── ClienteController.java      ← CRUD /api/customers
│       ├── dto/
│       │   ├── AuthRequestDTO.java         ← @Record: username + password
│       │   ├── AuthResponseDTO.java        ← @Record: token + username + role
│       │   ├── ClienteRequestDTO.java      ← @Record: rut + nombre + telefono + email
│       │   └── ClienteResponseDTO.java     ← @Record: respuesta del cliente
│       ├── exception/
│       │   ├── GlobalExceptionHandler.java ← Manejo global de errores (@RestControllerAdvice)
│       │   ├── RecursoNoEncontradoException.java
│       │   └── RutDuplicadoException.java
│       ├── model/
│       │   ├── Cliente.java                ← Entidad JPA tabla "clientes"
│       │   └── Usuario.java                ← Entidad JPA tabla "usuarios"
│       ├── repository/
│       │   ├── ClienteRepository.java
│       │   └── UsuarioRepository.java
│       ├── security/
│       │   ├── JwtUtil.java                ← Genera y valida tokens JWT
│       │   ├── JwtFilter.java              ← Intercepta requests y valida el token
│       │   ├── SecurityConfig.java         ← Reglas de acceso por endpoint
│       │   └── PasswordConfig.java         ← Bean BCryptPasswordEncoder
│       └── service/
│           ├── AuthService.java            ← Lógica de register y login
│           ├── ClienteService.java         ← Interfaz del servicio
│           └── ClientServiceImpl.java      ← Implementación del CRUD
│
└── ms-reparacion/                          ← Microservicio de reparaciones (puerto 8082)
    ├── Dockerfile
    ├── build.gradle
    ├── settings.gradle
    ├── gradlew / gradlew.bat
    └── src/main/java/cl/ms_reparacion/
        ├── MsReparacionApplication.java
        ├── controller/
        │   └── ReparacionController.java   ← CRUD /api/reparaciones
        ├── dto/
        │   ├── ReparacionRequestDTO.java   ← @Record: rutCliente + modelo + descripcion
        │   └── ReparacionResponseDTO.java  ← @Record: id + rutCliente + modelo + descripcion + estado
        ├── exception/
        │   ├── GlobalExceptionHandler.java
        │   ├── ClienteNoEncontradoException.java
        │   └── RecursoNoEncontradoException.java
        ├── model/
        │   └── Reparacion.java             ← Entidad JPA tabla "reparaciones"
        ├── repository/
        │   └── ReparacionRepository.java
        ├── security/
        │   ├── JwtUtil.java
        │   ├── JwtFilter.java
        │   └── SecurityConfig.java
        └── service/
            ├── ClienteVerificacionService.java ← Consulta HTTP a ms-cliente para validar RUT
            ├── ReparacionService.java
            └── ReparacionServiceImpl.java
```

---

## 4. Microservicio: ms-cliente

**Puerto:** `8081`
**Base de datos:** `db_smartfix_clientes`
**Responsabilidad:** Gestión de clientes y autenticación JWT.

### Tabla: `clientes`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | BIGSERIAL (PK) | Identificador único autoincremental |
| `rut` | VARCHAR(20) UNIQUE | RUT del cliente (formato: 12345678-9) |
| `nombre` | VARCHAR(100) | Nombre completo del cliente |
| `telefono` | VARCHAR(20) | Teléfono de contacto |
| `email` | VARCHAR(100) | Correo electrónico |

### Tabla: `usuarios`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | BIGSERIAL (PK) | Identificador único |
| `username` | VARCHAR(50) UNIQUE | Nombre de usuario para login |
| `password` | VARCHAR(255) | Contraseña encriptada con BCrypt |
| `role` | VARCHAR(20) | Rol del usuario (por defecto: `USER`) |

### Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/auth/register` | Público | Registra un nuevo técnico |
| `POST` | `/api/auth/login` | Público | Autentica y devuelve un JWT |
| `POST` | `/api/customers` | Autenticado | Crea un nuevo cliente |
| `GET` | `/api/customers` | Autenticado | Lista todos los clientes |
| `GET` | `/api/customers/{rut}` | Público* | Busca cliente por RUT |
| `PUT` | `/api/customers/{rut}` | Autenticado | Actualiza datos del cliente |
| `DELETE` | `/api/customers/{rut}` | Autenticado | Elimina un cliente |

> *`GET /api/customers/{rut}` es público para permitir que `ms-reparacion` verifique el RUT internamente sin necesitar token.

---

## 5. Microservicio: ms-reparacion

**Puerto:** `8082`
**Base de datos:** `db_smartfix_reparaciones`
**Responsabilidad:** Gestión de fichas técnicas de reparación. Antes de crear una ficha, valida que el RUT del cliente exista en `ms-cliente`.

### Tabla: `reparaciones`

| Columna | Tipo | Descripción |
|---|---|---|
| `id` | BIGSERIAL (PK) | Identificador único |
| `rut_cliente` | VARCHAR(20) | RUT del cliente (verificado en ms-cliente) |
| `modelo` | VARCHAR(100) | Modelo del equipo a reparar |
| `descripcion` | TEXT | Descripción de la falla reportada |
| `estado` | VARCHAR(30) | Estado actual (RECIBIDO, EN_PROCESO, TERMINADO, ENTREGADO) |

### Endpoints

| Método | Ruta | Acceso | Descripción |
|---|---|---|---|
| `POST` | `/api/reparaciones` | Autenticado | Crea ficha de reparación (valida RUT) |
| `GET` | `/api/reparaciones` | Autenticado | Lista todas las reparaciones |
| `GET` | `/api/reparaciones/cliente/{rut}` | Autenticado | Lista reparaciones por RUT de cliente |
| `PATCH` | `/api/reparaciones/{id}/estado` | Autenticado | Actualiza el estado de la reparación |

---

## 6. Cómo Levantar el Sistema

### Paso 1 — Clonar el repositorio

```bash
git clone https://github.com/tu-usuario/smartfix-workspace3.git
cd smartfix-workspace3
```

### Paso 2 — Levantar todo el sistema

```bash
docker-compose up --build
```

La primera vez tardará entre 3 y 8 minutos porque Docker descarga las imágenes base y compila el código Java. Las siguientes veces es más rápido.

Espera hasta ver en los logs:

```
ms_cliente    | Started MsClienteApplication in X.XXX seconds
ms_reparacion | Started MsReparacionApplication in X.XXX seconds
```

### Paso 3 — Verificar que los servicios están corriendo

```bash
docker ps
```

Deberías ver exactamente 5 contenedores activos:

| Contenedor | Puerto | Estado |
|---|---|---|
| `smartfix-workspace3` | — | grupo |
| `db_smartfix_clientes` | 5432 | ✅ running |
| `db_smartfix_reparaciones` | 5433 | ✅ running |
| `ms_cliente` | 8081 | ✅ running |
| `ms_reparacion` | 8082 | ✅ running |

### Detener el sistema

```bash
# Detener sin borrar datos
docker-compose down

# Detener y borrar todos los datos (bases de datos limpias)
docker-compose down -v
```

---

## 7. Flujo de Uso Completo

Sigue este orden exacto para probar el sistema desde cero:

### Paso A — Registrar un técnico

```http
POST http://localhost:8081/api/auth/register
Content-Type: application/json

{
  "username": "tecnico1",
  "password": "password123"
}
```

**Respuesta (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "tecnico1",
  "role": "USER"
}
```

### Paso B — Login

```http
POST http://localhost:8081/api/auth/login
Content-Type: application/json

{
  "username": "tecnico1",
  "password": "password123"
}
```

Copia el `token` de la respuesta. Lo usarás en todos los endpoints protegidos como:
```
Authorization: Bearer eyJhbGciOiJIUzI1NiJ9...
```

### Paso C — Registrar un cliente

```http
POST http://localhost:8081/api/customers
Content-Type: application/json
Authorization: Bearer <tu_token>

{
  "rut": "12345678-9",
  "nombre": "Juan Pérez",
  "telefono": "+56912345678",
  "email": "juan@email.com"
}
```

**Respuesta (201 Created):**
```json
{
  "rut": "12345678-9",
  "nombre": "Juan Pérez",
  "telefono": "+56912345678",
  "email": "juan@email.com"
}
```

### Paso D — Crear ficha de reparación (RUT existente)

```http
POST http://localhost:8082/api/reparaciones
Content-Type: application/json
Authorization: Bearer <tu_token>

{
  "rutCliente": "12345678-9",
  "modelo": "iPhone 14 Pro",
  "descripcion": "Pantalla rota y batería dañada"
}
```

**Respuesta (201 Created):**
```json
{
  "id": 1,
  "rutCliente": "12345678-9",
  "modelo": "iPhone 14 Pro",
  "descripcion": "Pantalla rota y batería dañada",
  "estado": "RECIBIDO"
}
```

### Paso E — Actualizar estado de reparación

```http
PATCH http://localhost:8082/api/reparaciones/1/estado?nuevoEstado=EN_PROCESO
Authorization: Bearer <tu_token>
```

**Estados disponibles:** `RECIBIDO` → `EN_PROCESO` → `TERMINADO` → `ENTREGADO`

### Paso F — Ver reparaciones del cliente

```http
GET http://localhost:8082/api/reparaciones/cliente/12345678-9
Authorization: Bearer <tu_token>
```

---

## 8. Pruebas con REST Client (VS Code)

Instala la extensión **REST Client** de Huachao Mao (`Ctrl+Shift+X` → buscar "REST Client").

Crea el archivo `smartfix.http` en la raíz del proyecto:

```http
### Variables
@baseCliente = http://localhost:8081
@baseReparacion = http://localhost:8082
@token = PEGA_TU_TOKEN_AQUI

### Register
POST {{baseCliente}}/api/auth/register
Content-Type: application/json

{ "username": "tecnico1", "password": "password123" }

### Login
POST {{baseCliente}}/api/auth/login
Content-Type: application/json

{ "username": "tecnico1", "password": "password123" }

### Sin token (debe dar 401)
GET {{baseCliente}}/api/customers

### Crear cliente
POST {{baseCliente}}/api/customers
Content-Type: application/json
Authorization: Bearer {{token}}

{ "rut": "12345678-9", "nombre": "Juan Pérez", "telefono": "+56912345678", "email": "juan@email.com" }

### Listar clientes
GET {{baseCliente}}/api/customers
Authorization: Bearer {{token}}

### Buscar por RUT
GET {{baseCliente}}/api/customers/12345678-9
Authorization: Bearer {{token}}

### Actualizar cliente
PUT {{baseCliente}}/api/customers/12345678-9
Content-Type: application/json
Authorization: Bearer {{token}}

{ "rut": "12345678-9", "nombre": "Juan Pérez Actualizado", "telefono": "+56987654321", "email": "nuevo@email.com" }

### Eliminar cliente
DELETE {{baseCliente}}/api/customers/12345678-9
Authorization: Bearer {{token}}

### Crear reparación (RUT existe - debe funcionar)
POST {{baseReparacion}}/api/reparaciones
Content-Type: application/json
Authorization: Bearer {{token}}

{ "rutCliente": "12345678-9", "modelo": "iPhone 14 Pro", "descripcion": "Pantalla rota" }

### Crear reparación (RUT NO existe - debe dar 404)
POST {{baseReparacion}}/api/reparaciones
Content-Type: application/json
Authorization: Bearer {{token}}

{ "rutCliente": "99999999-9", "modelo": "Samsung S24", "descripcion": "No enciende" }

### Listar todas las reparaciones
GET {{baseReparacion}}/api/reparaciones
Authorization: Bearer {{token}}

### Reparaciones por RUT cliente
GET {{baseReparacion}}/api/reparaciones/cliente/12345678-9
Authorization: Bearer {{token}}

### Actualizar estado
PATCH {{baseReparacion}}/api/reparaciones/1/estado?nuevoEstado=EN_PROCESO
Authorization: Bearer {{token}}
```

Haz clic en **Send Request** sobre cada bloque `###` para ejecutarlo. La respuesta aparece en el panel derecho.

---

## 9. Seguridad y JWT

### ¿Cómo funciona el JWT en SmartFix?

1. El técnico hace login en `ms-cliente` (puerto 8081).
2. El servicio genera un **token JWT** firmado con el secret configurado en las variables de entorno.
3. El token contiene: username del técnico y su rol.
4. En cada request a cualquier endpoint protegido, el cliente envía el token en el header `Authorization: Bearer <token>`.
5. El filtro `JwtFilter` de cada microservicio valida la firma del token usando **el mismo secret compartido**, sin consultar ninguna base de datos.

### Estructura del token JWT

```
eyJhbGciOiJIUzI1NiJ9          ← Header (algoritmo HS256)
.eyJzdWIiOiJ0ZWNuaWNvMSIsInJvbGUiOiJVU0VSIn0=   ← Payload
.firma_hmac_sha256              ← Firma
```

El **Payload** decodificado contiene:
```json
{
  "sub": "tecnico1",
  "role": "USER",
  "iat": 1716636000,
  "exp": 1716639600
}
```

### Importante: el secret JWT

Ambos servicios usan **exactamente el mismo secret** definido en `docker-compose.yml` como variable de entorno `JWT_SECRET`. Si cambias el secret en uno, debes cambiarlo en el otro, de lo contrario los tokens de `ms-cliente` no podrán ser validados por `ms-reparacion`.

El secret debe tener **mínimo 32 caracteres** (256 bits) para el algoritmo HS256.

---

## 10. Referencia Completa de Endpoints

### ms-cliente (puerto 8081)

#### `POST /api/auth/register`

Registra un nuevo técnico en el sistema.

**Body:**
```json
{
  "username": "string (requerido)",
  "password": "string (requerido)"
}
```

**Respuestas:**
- `200 OK` — Técnico registrado, devuelve token y rol.
- `409 Conflict` — El username ya existe.
- `400 Bad Request` — Campos vacíos o inválidos.

---

#### `POST /api/auth/login`

Autentica un técnico existente.

**Body:**
```json
{
  "username": "string (requerido)",
  "password": "string (requerido)"
}
```

**Respuestas:**
- `200 OK` — Devuelve token, username y rol.
- `404 Not Found` — Usuario no encontrado o credenciales incorrectas.

---

#### `POST /api/customers`

Crea un nuevo cliente. **Requiere autenticación.**

**Body:**
```json
{
  "rut": "12345678-9 (requerido, formato válido)",
  "nombre": "string (requerido, 2-100 caracteres)",
  "telefono": "+56912345678 (opcional)",
  "email": "email válido (opcional)"
}
```

**Respuestas:**
- `201 Created` — Cliente creado.
- `400 Bad Request` — RUT con formato inválido u otros campos inválidos.
- `409 Conflict` — Ya existe un cliente con ese RUT.
- `401 Unauthorized` — Token no enviado o inválido.

---

#### `GET /api/customers`

Lista todos los clientes. **Requiere autenticación.**

**Respuesta `200 OK`:**
```json
[
  {
    "rut": "12345678-9",
    "nombre": "Juan Pérez",
    "telefono": "+56912345678",
    "email": "juan@email.com"
  }
]
```

---

#### `GET /api/customers/{rut}`

Busca un cliente por su RUT.

**Respuestas:**
- `200 OK` — Datos del cliente.
- `404 Not Found` — Cliente no encontrado.

---

#### `PUT /api/customers/{rut}`

Actualiza los datos de un cliente. **Requiere autenticación.**

**Body:** mismo formato que POST.

**Respuestas:**
- `200 OK` — Cliente actualizado.
- `404 Not Found` — Cliente no encontrado.

---

#### `DELETE /api/customers/{rut}`

Elimina un cliente por su RUT. **Requiere autenticación.**

**Respuestas:**
- `204 No Content` — Cliente eliminado.
- `404 Not Found` — Cliente no encontrado.

---

### ms-reparacion (puerto 8082)

#### `POST /api/reparaciones`

Crea una ficha de reparación. **Valida que el RUT exista en ms-cliente.** Requiere autenticación.

**Body:**
```json
{
  "rutCliente": "12345678-9 (requerido, formato válido)",
  "modelo": "string (requerido)",
  "descripcion": "string (requerido)"
}
```

**Respuestas:**
- `201 Created` — Ficha creada con estado `RECIBIDO`.
- `404 Not Found` — El RUT no existe en ms-cliente.
- `400 Bad Request` — Campos inválidos.
- `401 Unauthorized` — Token no enviado o inválido.

---

#### `GET /api/reparaciones`

Lista todas las reparaciones. **Requiere autenticación.**

---

#### `GET /api/reparaciones/cliente/{rut}`

Lista todas las reparaciones de un cliente específico. **Requiere autenticación.**

**Respuesta `200 OK`:**
```json
[
  {
    "id": 1,
    "rutCliente": "12345678-9",
    "modelo": "iPhone 14 Pro",
    "descripcion": "Pantalla rota y batería dañada",
    "estado": "EN_PROCESO"
  }
]
```

---

#### `PATCH /api/reparaciones/{id}/estado`

Actualiza el estado de una reparación. **Requiere autenticación.**

**Query param:** `?nuevoEstado=EN_PROCESO`

**Estados válidos:** `RECIBIDO`, `EN_PROCESO`, `TERMINADO`, `ENTREGADO`

**Respuestas:**
- `200 OK` — Estado actualizado.
- `404 Not Found` — Reparación no encontrada.

---

## 11. Variables de Entorno

Todas las variables se configuran en `docker-compose.yml`. No necesitas tocar los `application.properties` para desarrollo local con Docker.

| Variable | Valor por defecto | Descripción |
|---|---|---|
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://db-cliente:5432/db_smartfix_clientes` | URL de conexión a PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Usuario de la base de datos |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Contraseña de la base de datos |
| `JWT_SECRET` | `smartfix-secret-key-256-bits-minimo-para-hs256-aqui` | Clave secreta JWT (mínimo 32 caracteres) |
| `JWT_EXPIRATION_MS` | `3600000` | Expiración del token (1 hora) |
| `SERVICES_CLIENTE_URL` | `http://ms-cliente:8081` | URL interna de ms-cliente (solo ms-reparacion) |

> **Para producción:** Genera un secret seguro con `openssl rand -base64 32` y nunca lo subas al repositorio.

---

## 12. Migraciones de Base de Datos (Flyway)

Flyway crea las tablas automáticamente al arrancar cada servicio. No necesitas ejecutar SQL manual.

**ms-cliente:**
- `V1__create_table_clientes.sql` — Crea la tabla `clientes`
- `V2__create_table_usuarios.sql` — Crea la tabla `usuarios`

**ms-reparacion:**
- `V1__create_table_reparaciones.sql` — Crea la tabla `reparaciones`

> **Importante:** Nunca modifiques un archivo de migración ya ejecutado. Si necesitas cambiar el esquema, crea un nuevo archivo `V3__descripcion.sql`. Flyway verifica el checksum de cada migración y lanzará un error si detecta cambios en archivos ya aplicados.

---

## 13. Preguntas Frecuentes

**¿Por qué al crear una reparación obtengo "El servicio de clientes no está disponible"?**

Verifica que:
1. `ms-cliente` esté corriendo (`docker ps`).
2. La variable `SERVICES_CLIENTE_URL` en el `docker-compose.yml` sea `http://ms-cliente:8081` (no `localhost`).
3. Ambos servicios estén en la misma red Docker (`smartfix-net`).

---

**¿Por qué obtengo 401 Unauthorized?**

El token JWT no fue enviado o es inválido. Verifica:
1. Que el header sea exactamente `Authorization: Bearer <token>` (con espacio entre "Bearer" y el token).
2. Que el token no haya expirado (por defecto expiran en 1 hora).
3. Que hayas hecho login y copiado el token correctamente.

---

**¿Por qué Docker muestra 4 bases de datos en lugar de 2?**

Quedaron volúmenes huérfanos de ejecuciones anteriores. Ejecuta:
```bash
docker-compose down -v
docker volume prune -f
docker-compose up --build
```

---

**¿Por qué la primera vez que corro `docker-compose up --build` tarda tanto?**

Docker descarga las imágenes base (`eclipse-temurin:21-jdk-alpine`, `postgres:16-alpine`) y Gradle descarga todas las dependencias de Maven Central. Dependiendo de tu conexión puede tardar entre 3 y 10 minutos. Las siguientes veces usa el caché y es mucho más rápido.

---

**¿Puedo probar los endpoints sin Postman?**

Sí. Instala la extensión **REST Client** en VS Code y usa el archivo `smartfix.http` incluido en el proyecto. Ver sección [Pruebas con REST Client](#8-pruebas-con-rest-client-vs-code).

---

**¿Cómo detengo los servicios?**

```bash
# Detener sin borrar datos
docker-compose down

# Detener y borrar datos (empezar desde cero)
docker-compose down -v
```

---

## Tecnologías Utilizadas

| Tecnología | Versión | Uso |
|---|---|---|
| Java | 21 (LTS) | Lenguaje principal |
| Spring Boot | 3.3.5 | Framework base |
| Spring Security | (incluido en Boot) | Seguridad y control de acceso |
| Spring Data JPA | (incluido en Boot) | Acceso a base de datos con Hibernate |
| Flyway | 10.x | Migraciones automáticas de esquema SQL |
| PostgreSQL | 16 | Motor de base de datos relacional |
| JJWT | 0.12.6 | Generación y validación de tokens JWT |
| Docker + Docker Compose | Latest | Contenedores y orquestación |
| Gradle | 9.4.1 | Sistema de build |
| REST Client (VS Code) | — | Pruebas de endpoints desde el editor |
