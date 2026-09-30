# Sukinema — backend

API REST de Sukinema, un catálogo de tráilers estilo Netflix. Spring Boot 3 + Java 21.

El frontend está en [sukinema-frontend](https://github.com/costanna/sukinema-frontend).

## Arrancar en local

Requiere Java 21 y Maven.

```
mvn spring-boot:run
```

Queda en http://localhost:8088. No hace falta configurar nada: usa una base de datos H2 **en memoria**, que vuelve a los 15 tráilers y 4 perfiles iniciales de `DataInitializer` cada vez que se reinicia. Consola en http://localhost:8088/h2-console (JDBC `jdbc:h2:mem:sukinemadb`, usuario `sa`, sin contraseña).

## API

| Método | Ruta | Descripción |
| --- | --- | --- |
| GET | `/api/movies` | Todos los tráilers |
| GET | `/api/movies/featured` | Tráiler destacado del banner |
| GET | `/api/movies/categories` | Tráilers agrupados por categoría |
| GET | `/api/movies/trending` | Tráilers en tendencia |
| GET | `/api/movies/category/{category}` | Tráilers de una categoría |
| GET | `/api/movies/search?query=` | Búsqueda por título, géneros, reparto o sinopsis |
| GET | `/api/movies/{id}` | Un tráiler |
| POST | `/api/movies` | Crear |
| PUT | `/api/movies/{id}` | Editar |
| POST | `/api/movies/{id}/like` | Sumar un like |
| DELETE | `/api/movies/{id}` | Eliminar |
| GET | `/api/profiles` | Perfiles |
| GET | `/api/profiles/{id}` | Un perfil |
| POST | `/api/profiles` | Crear perfil |
| PUT | `/api/profiles/{id}` | Editar perfil (avatar y color se conservan si no se envían) |
| DELETE | `/api/profiles/{id}` | Eliminar perfil |
| GET | `/api/health` | Comprobación de salud (`{"status":"UP"}`) |

## Variables de entorno

| Variable | Para qué | Sin ella |
| --- | --- | --- |
| `DATABASE_URL` | Cadena de conexión de PostgreSQL (`postgresql://...` o `jdbc:postgresql://...`) | H2 en memoria; con el perfil `prod` el arranque falla |
| `CORS_ALLOWED_ORIGINS` | Orígenes que pueden llamar a la API, separados por comas; admite comodines | Cualquier origen |
| `PORT` | Puerto de escucha | 8088 |
| `SPRING_PROFILES_ACTIVE` | `prod` desactiva la consola H2 y ajusta el pool de conexiones; el Dockerfile ya lo fija | Configuración local |

## Despliegue

Guía paso a paso para publicar la app completa gratis con Neon, Render y Vercel en [DEPLOY.md](DEPLOY.md).

## Tests

```
mvn test
```
