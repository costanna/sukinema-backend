# Sukinema — backend

API REST de Sukinema, un catálogo de tráilers estilo Netflix. Spring Boot 3 + Java 21.

El frontend está en [sukinema-frontend](https://github.com/costanna/sukinema-frontend).

## Arrancar en local

Requiere Java 21 y Maven.

```
mvn spring-boot:run
```

Queda en http://localhost:8088. No hace falta configurar nada: usa una base de datos H2 **en memoria**, que vuelve a los 15 tráilers iniciales de `DataInitializer` (y sin cuentas) cada vez que se reinicia. Consola en http://localhost:8088/h2-console (JDBC `jdbc:h2:mem:sukinemadb`, usuario `sa`, sin contraseña).

## Cuentas y permisos

Toda la API exige iniciar sesión, salvo el registro, el acceso y la comprobación de salud. El acceso devuelve un token que se envía en cada petición como `Authorization: Bearer <token>` y dura 7 días.

| Quién | Qué puede hacer |
| --- | --- |
| Cualquier cuenta | Ver el catálogo y gestionar sus propios perfiles (entre 1 y 5), cada uno con su lista y sus likes |
| Cuenta administradora | Además, añadir, editar y eliminar tráilers |

La cuenta administradora es la que se registra con el correo de `ADMIN_EMAIL`. Si esa variable no está definida, lo es **la primera cuenta que se registre**, que además hereda los perfiles que existieran antes de haber cuentas.

Las contraseñas se guardan cifradas con BCrypt. Tras 5 intentos fallidos seguidos, el acceso con ese correo se bloquea 5 minutos.

### Contraseña olvidada

No se envían correos. Al registrarse, cada cuenta recibe un **código de recuperación** (`ABCD-EFGH-JKLM`) que solo se muestra en ese momento y se guarda cifrado. Con el correo y ese código se puede poner una contraseña nueva; el código es de un solo uso y se entrega otro al usarlo. Desde la sesión se puede generar uno nuevo, que invalida el anterior.

Cambiar o restablecer la contraseña cierra las sesiones abiertas en otros dispositivos.

## API

Los errores devuelven `{"message": "..."}` con el motivo.

| Método | Ruta | Descripción | Acceso |
| --- | --- | --- | --- |
| GET | `/api/health` | Comprobación de salud (`{"status":"UP"}`) | Público |
| POST | `/api/auth/register` | Crear cuenta (`name`, `email`, `password` de 8 a 72 caracteres) | Público |
| POST | `/api/auth/login` | Iniciar sesión (`email`, `password`) | Público |
| POST | `/api/auth/recover` | Poner una contraseña nueva con el código de recuperación (`email`, `recoveryCode`, `newPassword`) | Público |
| GET | `/api/auth/me` | Cuenta de la sesión | Sesión |
| POST | `/api/auth/password` | Cambiar la contraseña (`currentPassword`, `newPassword`); devuelve un token nuevo | Sesión |
| POST | `/api/auth/recovery-code` | Generar un código de recuperación nuevo (`password`) | Sesión |
| GET | `/api/movies` | Todos los tráilers | Sesión |
| GET | `/api/movies/featured` | Tráiler destacado del banner | Sesión |
| GET | `/api/movies/categories` | Tráilers agrupados por categoría | Sesión |
| GET | `/api/movies/trending` | Tráilers en tendencia | Sesión |
| GET | `/api/movies/category/{category}` | Tráilers de una categoría | Sesión |
| GET | `/api/movies/search?query=` | Búsqueda por título, géneros, reparto, director o sinopsis | Sesión |
| GET | `/api/movies/{id}` | Un tráiler | Sesión |
| POST | `/api/movies` | Crear | Administradora |
| PUT | `/api/movies/{id}` | Editar | Administradora |
| DELETE | `/api/movies/{id}` | Eliminar | Administradora |
| GET | `/api/profiles` | Perfiles de la cuenta | Sesión |
| GET | `/api/profiles/{id}` | Un perfil de la cuenta | Sesión |
| POST | `/api/profiles` | Crear perfil | Sesión |
| PUT | `/api/profiles/{id}` | Editar perfil (avatar y color se conservan si no se envían) | Sesión |
| DELETE | `/api/profiles/{id}` | Eliminar perfil (siempre queda al menos uno) | Sesión |
| GET | `/api/profiles/{id}/library` | "Mi Lista" y likes del perfil: `{"myList": [ids], "likes": [ids]}` | Sesión |
| PUT | `/api/profiles/{id}/my-list/{movieId}` | Añadir un tráiler a "Mi Lista" | Sesión |
| DELETE | `/api/profiles/{id}/my-list/{movieId}` | Quitarlo de "Mi Lista" | Sesión |
| PUT | `/api/profiles/{id}/likes/{movieId}` | Dar like (uno por perfil); devuelve el tráiler con su contador | Sesión |
| DELETE | `/api/profiles/{id}/likes/{movieId}` | Quitar el like; devuelve el tráiler con su contador | Sesión |

Solo puede haber un tráiler destacado: al marcar uno, se desmarca el anterior. La URL del tráiler debe ser un enlace o un ID de vídeo de YouTube.

## Variables de entorno

| Variable | Para qué | Sin ella |
| --- | --- | --- |
| `DATABASE_URL` | Cadena de conexión de PostgreSQL (`postgresql://...` o `jdbc:postgresql://...`) | H2 en memoria; con el perfil `prod` el arranque falla |
| `CORS_ALLOWED_ORIGINS` | Orígenes que pueden llamar a la API, separados por comas; admite comodines | Cualquier origen |
| `ADMIN_EMAIL` | Correo de la cuenta administradora | Administra la primera cuenta que se registre |
| `JWT_SECRET` | Clave con la que se firman las sesiones | Se genera una y se guarda en la base de datos |
| `PORT` | Puerto de escucha | 8088 |
| `SPRING_PROFILES_ACTIVE` | `prod` desactiva la consola H2 y ajusta el pool de conexiones; el Dockerfile ya lo fija | Configuración local |

## Despliegue

Guía paso a paso para publicar la app completa gratis con Neon, Render y Vercel en [DEPLOY.md](DEPLOY.md).

## Tests

```
mvn test
```
