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

## 5. Comprobar

Abre la URL de Vercel. Debe aparecer "¿Quién está viendo?" y, al entrar, el indicador verde **Spring Boot 3 API** (arriba a la derecha en escritorio, dentro del menú en móvil). Si en su lugar pone **Catálogo Local**, el frontend no está llegando al backend: mira la tabla de abajo.

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
| La app muestra "Catálogo Local" | `VITE_API_URL` no está definida, tiene un error, o no se volvió a desplegar tras cambiarla |
| La consola del navegador muestra errores de CORS o respuestas 403 "Invalid CORS request" | `CORS_ALLOWED_ORIGINS` no incluye la URL exacta del frontend (con `https://`, sin barra final) |
| La primera carga tarda mucho | Normal: el backend estaba dormido |

## Volver a los datos iniciales

En el editor SQL de Neon:

```sql
TRUNCATE movies, user_profiles RESTART IDENTITY;
```

Después reinicia el servicio en Render: al encontrar las tablas vacías, vuelve a cargar los datos iniciales.

## Probar la imagen en local (opcional)

Con Docker en marcha, esto reproduce lo que hará Render, contra un PostgreSQL local:

```
docker network create sukinema
docker run -d --name sukinema-pg --network sukinema -e POSTGRES_PASSWORD=secreto postgres:17
docker build -t sukinema-backend .
docker run --rm --network sukinema -p 8088:8088 -e PORT=8088 -e DATABASE_URL=postgresql://postgres:secreto@sukinema-pg/postgres sukinema-backend
```

El frontend en local (`npm run dev`) se conectará a él en `http://localhost:8088`.
