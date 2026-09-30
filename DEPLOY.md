# Desplegar Sukinema gratis

| Pieza | Servicio | Qué hace |
| --- | --- | --- |
| Base de datos | [Neon](https://neon.tech) | PostgreSQL; aquí se guardan tráilers y perfiles |
| Backend | [Render](https://render.com) | Ejecuta la API Spring Boot desde el `Dockerfile` de este repositorio |
| Frontend | [Vercel](https://vercel.com) | Sirve la app React compilada |

El código está en dos repositorios de GitHub, y cada servicio despliega desde el suyo:

- Backend: [costanna/sukinema-backend](https://github.com/costanna/sukinema-backend) (este)
- Frontend: [costanna/sukinema-frontend](https://github.com/costanna/sukinema-frontend)

El orden importa: cada paso necesita un dato del anterior.

```
Neon ──(cadena de conexión)──▶ Render ──(URL del backend)──▶ Vercel ──(URL del frontend)──▶ Render (CORS)
```

## 1. Neon: la base de datos

1. Crea un proyecto en Neon. Región recomendada: **AWS Europe (Frankfurt)**, la misma que usa el backend en Render.
2. Pulsa **Connect** y copia la cadena de conexión, con *Connection pooling* activado. Tiene esta forma:

   ```
   postgresql://neondb_owner:CONTRASEÑA@ep-xxxx-pooler.eu-central-1.aws.neon.tech/neondb?sslmode=require&channel_binding=require
   ```

No hay que crear tablas: el backend las crea y carga los 15 tráilers y 4 perfiles iniciales la primera vez que arranca.

## 2. Render: el backend

1. En Render: **New → Blueprint** y elige el repositorio `sukinema-backend`. Render lee `render.yaml` y propone el servicio `sukinema-backend`.
2. Te pedirá dos variables:

   | Variable | Valor |
   | --- | --- |
   | `DATABASE_URL` | La cadena de Neon, pegada tal cual |
   | `CORS_ALLOWED_ORIGINS` | De momento `https://*.vercel.app`; se ajusta en el paso 4 |

3. Aplica el Blueprint. La primera compilación tarda unos minutos.
4. Cuando termine, comprueba que `https://TU-SERVICIO.onrender.com/api/health` responde `{"status":"UP"}` y apunta esa URL.

## 3. Vercel: el frontend

1. En Vercel: **Add New → Project** e importa el repositorio `sukinema-frontend`.
2. Deja la configuración que propone: Vercel detecta Vite por sí solo.
3. En **Environment Variables** añade:

   | Variable | Valor |
   | --- | --- |
   | `VITE_API_URL` | La URL de Render, sin `/api` al final: `https://TU-SERVICIO.onrender.com` |

4. Despliega y apunta la URL que te da Vercel.

`VITE_API_URL` se incrusta al compilar: si la cambias después, hay que volver a desplegar en Vercel para que tenga efecto.

## 4. Cerrar el CORS

En Render, cambia `CORS_ALLOWED_ORIGINS` por la URL exacta de Vercel, por ejemplo `https://sukinema.vercel.app`. Así solo tu frontend puede llamar a la API. Admite varias separadas por comas.

Si quieres que también funcionen las vistas previas que Vercel crea en cada rama, añade un patrón con el nombre de tu proyecto: `https://sukinema.vercel.app,https://sukinema-*.vercel.app`.

## 5. Crear tu cuenta (la administradora)

Abre la URL de Vercel: aparece la pantalla de inicio de sesión. Pulsa **Crea una cuenta** y regístrate.

Al terminar verás tu **código de recuperación**: apúntalo, porque es la única forma de recuperar la cuenta si olvidas la contraseña y no se vuelve a mostrar.

**La primera cuenta que se registra es la administradora**: la única que puede añadir, editar y eliminar tráilers. Las demás cuentas solo ven el catálogo, dan likes y gestionan sus perfiles. Por eso conviene registrarse nada más desplegar, antes de compartir la dirección.

Si prefieres no depender del orden, define en Render la variable `ADMIN_EMAIL` con tu correo antes de registrarte: entonces administra la cuenta que se registre con ese correo, sea o no la primera.

## 6. Comprobar

Tras entrar debe aparecer "¿Quién está viendo?" y, al elegir perfil, el indicador verde de conexión (arriba a la derecha; en pantallas anchas dice **Spring Boot 3 API**, y en móvil está dentro del menú). Con la cuenta administradora se ve además el botón **Nuevo Tráiler**.

Si la pantalla de acceso avisa de que el servidor no responde, el frontend no está llegando al backend: mira la tabla de abajo.

## Qué esperar del plan gratuito

- **Render duerme el backend** tras unos 15 minutos sin visitas. La siguiente visita tarda cerca de un minuto; la app muestra "El servidor se está despertando" mientras tanto.
- **Neon suspende la base de datos** tras unos minutos sin uso y la reanuda sola en la primera consulta.
- Los datos ya no se pierden al reiniciar el backend: viven en Neon.
- Los límites de los planes gratuitos cambian; revisa los actuales en cada servicio.

## Si algo falla

| Síntoma | Causa probable |
| --- | --- |
| El despliegue en Render falla con "Falta la variable de entorno DATABASE_URL" | No se definió `DATABASE_URL` en el servicio |
| Render falla con "The connection attempt failed" o "password authentication failed" | La cadena de Neon está incompleta o mal copiada |
| La pantalla de acceso dice "El servidor no responde" u ofrece el modo demo | `VITE_API_URL` no está definida, tiene un error, o no se volvió a desplegar tras cambiarla |
| La consola del navegador muestra errores de CORS o respuestas 403 "Invalid CORS request" | `CORS_ALLOWED_ORIGINS` no incluye la URL exacta del frontend (con `https://`, sin barra final) |
| La primera carga o el primer inicio de sesión tardan mucho | Normal: el backend estaba dormido |
| No aparece el botón "Nuevo Tráiler" | La cuenta no es la administradora (o el perfil activo es infantil) |
| "Demasiados intentos fallidos" | Cinco contraseñas o códigos de recuperación erróneos seguidos bloquean ese correo 5 minutos |
| Alguien ha olvidado la contraseña y ha perdido su código de recuperación | No se puede recuperar esa cuenta; bórrala (abajo) para que pueda registrarse de nuevo con el mismo correo |

## Cambiar quién administra

En el editor SQL de Neon (cambia el correo por el de la cuenta):

```sql
UPDATE accounts SET role = 'ADMIN' WHERE email = 'tu@correo.com';
```

Para quitar el permiso, lo mismo con `'USER'`. El cambio vale desde la siguiente petición, sin reiniciar nada.

## Borrar una cuenta

En el editor SQL de Neon (cambia el correo). Borra la cuenta con sus perfiles, listas y likes:

```sql
DELETE FROM profile_my_list WHERE profile_id IN (SELECT p.id FROM user_profiles p JOIN accounts a ON a.id = p.account_id WHERE a.email = 'tu@correo.com');
DELETE FROM profile_likes WHERE profile_id IN (SELECT p.id FROM user_profiles p JOIN accounts a ON a.id = p.account_id WHERE a.email = 'tu@correo.com');
DELETE FROM user_profiles WHERE account_id = (SELECT id FROM accounts WHERE email = 'tu@correo.com');
DELETE FROM accounts WHERE email = 'tu@correo.com';
```

## Volver a los datos iniciales

Para restaurar solo el catálogo, en el editor SQL de Neon:

```sql
TRUNCATE movies, profile_my_list, profile_likes RESTART IDENTITY;
```

Después reinicia el servicio en Render: al encontrar la tabla vacía, vuelve a cargar los 15 tráilers iniciales.

Para borrar también todas las cuentas y sus perfiles (la siguiente cuenta que se registre volverá a ser la administradora):

```sql
TRUNCATE profile_my_list, profile_likes, user_profiles, accounts RESTART IDENTITY CASCADE;
```

## Probar la imagen en local (opcional)

Con Docker en marcha, esto reproduce lo que hará Render, contra un PostgreSQL local:

```
docker network create sukinema
docker run -d --name sukinema-pg --network sukinema -e POSTGRES_PASSWORD=secreto postgres:17
docker build -t sukinema-backend .
docker run --rm --network sukinema -p 8088:8088 -e PORT=8088 -e DATABASE_URL=postgresql://postgres:secreto@sukinema-pg/postgres sukinema-backend
```

El frontend en local (`npm run dev`) se conectará a él en `http://localhost:8088`.
