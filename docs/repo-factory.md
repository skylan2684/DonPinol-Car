# RepoFactory: crear repositorios desde ChatGPT sin Copilot Pro

**Alcance:** cuenta personal `skylan2684`, exclusivamente repositorios **privados y vacíos**.
El mecanismo se aloja en `DonPinol-Car`, pero no modifica ni compila la aplicación Android.

## Estado y activación

- La automatización **NO está activa** hasta fusionar su PR con `main`.
- Antes de usarla, el propietario debe crear un token y guardarlo como **secreto de GitHub Actions**.
- No pegar nunca el token en ChatGPT, en una incidencia, en código ni en un documento.

### Única configuración manual

1. Abrir https://github.com/settings/personal-access-tokens/new
2. Elegir un **Fine-grained personal access token**, propietario `skylan2684`, expiración limitada. Dar únicamente el permiso de repositorio **Repository creation: Read and write** (según las opciones mostradas por GitHub). No conceder acceso administrativo genérico.
3. Abrir https://github.com/skylan2684/DonPinol-Car/settings/secrets/actions
4. Crear un secreto de repositorio de tipo Actions llamado exactamente `REPO_FACTORY_PAT`, pegar su valor **directamente en GitHub** y guardar.
5. Revisar y fusionar la PR de RepoFactory. Esta fusión disparará también el workflow existente de compilación de DonPinol-Car por ser un cambio a `main`.

Si GitHub no ofrece el permiso específico de creación para ese tipo de token, detener la activación; no sustituirlo por un token de acceso total sin revisar el alcance.

## Uso después de activar

Cuando el propietario pida explícitamente en ChatGPT crear `NombreNuevo`:

1. Sol abre una incidencia en `skylan2684/DonPinol-Car` con el título exacto `[RepoFactory] NombreNuevo`.
2. Solo después de esa petición, Sol publica en esa incidencia el comentario exacto `AUTORIZAR: PRIVADO-VACIO` mediante la cuenta autorizada.
3. `repo-factory.yml` verifica que quien abrió la incidencia **y** quien publicó el comentario sea `skylan2684`, además de comprobar el repositorio origen, el texto de aprobación y el nombre.
4. GitHub Actions usa `REPO_FACTORY_PAT` para llamar a `POST /user/repos` con `private=true` y `auto_init=false`. No añade README, .gitignore ni licencia.
5. El workflow publica el enlace al repositorio nuevo en la incidencia y la cierra. Sol debe comprobar la existencia y privacidad del repositorio antes de decir que se completó.

## Controles de seguridad

- Nunca escucha un título o comentario cualquiera: exige una incidencia del propietario, una aprobación literal del propietario y un nombre válido.
- No acepta nombre de organización ni parámetros de visibilidad: siempre crea repositorios **privados** en `skylan2684`.
- Lee datos del evento como JSON, no ejecuta texto de la incidencia como código de shell.
- El secreto vive solamente en GitHub Actions; no aparece en el repositorio ni en ChatGPT.
- El `GITHUB_TOKEN` propio del workflow se utiliza solo para registrar el resultado y cerrar la incidencia.
- Una solicitud puede fallar si caduca/revocan el token, se agota la cuota de Actions o GitHub bloquea la operación.
- Repetir una aprobación puede producir un fallo por nombre duplicado; jamás se debe interpretar como repositorio creado sin verificación.
- Los permisos y secretos se pueden revocar desde los ajustes de GitHub.

**Pendiente de prueba real:** disparar una solicitud controlada una vez fusionada la PR y configurado el secreto. No declarar la automatización operativa antes de esa prueba.

Documentación oficial de GitHub:
- https://docs.github.com/en/rest/repos/repos#create-a-repository-for-the-authenticated-user
- https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens
- https://docs.github.com/en/actions/reference/workflows-and-actions/events-that-trigger-workflows#issue_comment
