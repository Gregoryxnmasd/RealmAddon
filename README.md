# RealmAddon

RealmAddon integra RealmCore con Iris (Dimension Engine) para que los realms se creen usando generadores Iris en lugar de biomas vanilla o templates.

## Requisitos

- **Paper 1.21.8+** (Java 21)
- **RealmCore**
- **Iris** (Dimension Engine)

## Configuración requerida de RealmCore

En `plugins/RealmCore/settings.yml`:

```yaml
creator:
  automatic: false
  unique-biome: true
  template: "" # desactivado o ignorado
```

## Instalación

1. Compila el proyecto con Gradle (`./gradlew build`).
2. Copia el jar de `paper/build/libs` a tu servidor Paper.
3. (Opcional) Copia el jar de `bungee/build/libs` a tu proxy Bungee/Waterfall si usarás `NETWORK_MESSAGING`.
4. Configura `plugins/RealmAddon/config.yml`.

## Modos de red

- `SINGLE`: servidor único, en memoria.
- `NETWORK_MYSQL`: guarda la selección en MySQL (compartido entre servidores).
- `NETWORK_MESSAGING`: requiere el módulo Bungee; reenvía la elección por plugin messaging.

En modo `NETWORK_MESSAGING`, puedes definir `integration.messagingTargetServer` para enviar la elección a un servidor específico (si se deja vacío, se transmite a todos).

Para `NETWORK_MYSQL` se crea la tabla:

```sql
CREATE TABLE realmaddon_pending_choice (
  uuid VARCHAR(36) PRIMARY KEY,
  generator_key VARCHAR(64) NOT NULL,
  created_at BIGINT NOT NULL
);
```

## Comandos

- `/realmaddon reload`
- `/realmaddon set <player> <generatorKey>`
- `/realmaddon debug <player>`

## Permisos

- `realmaddon.use`
- `realmaddon.admin`

## Flujo

1. Intercepta `/realm create` o `/realm reset`.
2. Muestra un selector GUI de generadores Iris.
3. Fuerza la creación del mundo Iris con el nombre exacto que usa RealmCore.
4. Ejecuta `/realm home` tras finalizar.

## Notas

- El plugin usa reflexión para resolver el nombre real del mundo de RealmCore.
- Si no encuentra el nombre, observa el filesystem para detectar la carpeta creada.
