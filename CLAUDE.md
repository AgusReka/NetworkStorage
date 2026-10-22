# Network Storage — Mod para Hytale

Mod inspirado en Magic Storage (Terraria): una red de almacenamiento centralizada formada por bloques conectados. Plugin Java + asset pack, empaquetados juntos.

## Reglas para Claude Code

- **Nunca inventes nombres de clases o métodos de la API de Hytale.** Antes de escribir código que use la API, inspeccioná `HytaleServer.jar` (por ejemplo con `javap` o descompilando las clases relevantes) o la Javadoc que trae el template. La API cambia entre updates del Early Access.
- Si existe código del mod de cofre anterior en `reference/`, usalo como referencia principal para manejo de contenedores, inventarios y ventanas de cofre.
- Trabajá por etapas (ver "Plan de desarrollo"). Compilá y verificá cada etapa antes de pasar a la siguiente.
- Pedí permiso antes de instalar software en el sistema.
- Respondé en español.

## Setup inicial (primera sesión)

1. Detectar el sistema operativo.
2. Verificar Java: `java -version`. Se requiere **Java 25 SDK**. Si falta, proponer instalarlo (Windows: `winget`; macOS: `brew`; Linux: gestor de la distro).
3. Verificar que `git` esté instalado.
4. Clonar un template de plugin de Hytale en esta carpeta (preferido: https://github.com/AzureDoom/plugin-template, que usa el Gradle plugin `com.azuredoom.hytale-tools`). Leer su README y seguir sus pasos de setup.
5. Configurar nombre del proyecto, grupo Maven (`gradle.properties`) y `manifest.json` (nombre del mod, clase `Main`).
6. Correr la sincronización de Gradle y compilar el plugin de ejemplo para confirmar que todo funciona.
7. Ubicar `HytaleServer.jar` y confirmar la versión del servidor contra la que se compila.

Requisito previo del usuario: launcher de Hytale instalado y actualizado, con sesión iniciada.

## Diseño del mod

### Bloques

| Bloque | Función |
|---|---|
| **Storage Terminal** | Centro de la red. **Guarda todos los ítems** y abre la ventana de la red para ver, meter y sacar ítems. Una red tiene exactamente una terminal (no se permite poner un bloque que conecte dos). |
| **Storage Unit** | No se puede abrir. Solo aporta capacidad a la red: **18 slots (2 filas, como un cofre chico)**. Puede haber tiers con distinta capacidad. |
| **Storage Pipe** | Solo conecta bloques de la red, no aporta slots. Conecta por las 6 caras. Cambia de forma según sus vecinos (10 formas, `ConnectedBlockRuleSet` "Patterned"; la Terminal y la Unit también cuentan como conexión). |

Los assets del pipe (modelos, textura, rule set y JSON del bloque) se generan con `tools/generate-assets.ps1` (que también genera las texturas de la Terminal y la Unit y los íconos): **no editarlos a mano**, cambiar el script y volver a correrlo.

Ya no existe un bloque Core separado: la terminal cumple ese rol.

El crafteo NO se implementa: Hytale ya permite craftear con ítems de contenedores cercanos. (Opcional a futuro: investigar si la terminal puede registrarse como contenedor cercano para las mesas de crafteo.)

### Red

- Los bloques forman una red por adyacencia (6 vecinos, sin diagonales).
- Al poner o romper cualquier bloque del mod, recalcular la red con una búsqueda por vecinos (BFS).
- Capacidad de la red = suma de los slots aportados por las units conectadas.

### Inventario

- Vive en la terminal, como lista de stacks (respetando el tamaño máximo de stack vanilla).
- Debe persistir con el mundo (sobrevivir reinicios y descarga de chunks).
- Romper una unit **no dropea ni borra ítems**: solo baja la capacidad.

### Regla de sobrecarga

- Slots mostrados = `max(capacidad, slotsOcupados)`, redondeado a filas completas.
- **Meter ítems**: permitido solo si `slotsOcupados < capacidad`, o si el ítem entra en un stack existente no lleno.
- **Sacar ítems**: siempre permitido.
- Si la red queda sobrecargada, todo sigue visible y extraíble; no acepta ítems nuevos hasta agregar units.

### UI

- La ventana nativa de cofre **no sirve**: con muchos slots se rompe y no tiene scroll (probado).
- Se usa una **página custom** (`Common/UI/Custom/NetworkStorage/`): grilla con scroll de los ítems de la red (un ícono por tipo de ítem con su total, estilo Magic Storage), buscador, y el inventario del jugador al costado. Clic izquierdo/derecho para sacar o guardar un stack o una unidad.
- El inventario real de la terminal es un `ItemContainerBlock` de capacidad fija (9216 slots, el tope de la red); la capacidad de la red se aplica al guardar.
- Si la red cambia mientras alguien tiene la ventana abierta, refrescarla.
- Considerar varios jugadores con la terminal abierta a la vez (evitar duplicación de ítems).

### Romper la terminal

No se puede romper mientras tenga ítems: un jugador tiene que vaciarla antes (`NetworkBreakSystem`) y las explosiones no le hacen daño (`TerminalExplosionGuardSystem`). Vacía se rompe normalmente.

Ojo: los eventos de bloques sin jugador (`DamageBlockEvent` de explosiones, `EnvironmentBreakBlockEvent`) se disparan a nivel de mundo y se escuchan con `WorldEventSystem`, no con `EntityEventSystem`.

## Plan de desarrollo

1. Setup del proyecto (ver arriba).
2. Registrar los 2 bloques con placeholder de modelo/textura y receta; verificar que aparecen en el juego.
3. Detección de red al poner/romper bloques (con logs para depurar).
4. Inventario en la terminal con persistencia.
5. Terminal abre ventana de cofre con tamaño dinámico.
6. Reglas de capacidad y sobrecarga.
7. Casos borde: multijugador, cambios de red con ventana abierta, romper la terminal.
8. Modelos finales (Blockbench) y texturas.

## Pruebas

- Ejecutar el servidor local con la tarea/configuración que provee el template.
- Conectarse con el cliente de Hytale al servidor local.
- Instalación manual para un jugador: copiar el `.jar` a `%appdata%/Hytale/UserData/Mods/` (Windows).
