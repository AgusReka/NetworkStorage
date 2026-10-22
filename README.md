# Network Storage

Mod para **Hytale** que agrega una red de almacenamiento centralizada: conectás bloques entre sí y
accedés a todos tus ítems desde una sola terminal, con buscador y scroll.

Está inspirado en **[Magic Storage](https://github.com/blushiemagic/MagicStorage)**, el mod de
Terraria (tModLoader) creado por **blushiemagic**. Ver [Créditos](#créditos).

**Versión actual:** 1.0.0 · **Servidor de Hytale:** 0.7.2 (`>=0.7.2 <0.8.0`)

## Instalación

1. Descargá el jar de [`releases/NetworkStorage-1.0.0.jar`](releases/NetworkStorage-1.0.0.jar).
2. Copialo a la carpeta de mods de Hytale:
   - Windows: `%appdata%/Hytale/UserData/Mods/`
3. Para un servidor dedicado, ponelo en la carpeta de mods del servidor. Los jugadores que se
   conectan no necesitan instalar nada: el servidor les manda los modelos, texturas y ventanas.

## Bloques

| Bloque | Qué hace |
|---|---|
| **Storage Terminal** | Corazón de la red: guarda todos los ítems y abre la ventana para verlos, buscarlos, guardarlos y sacarlos. Es una consola de 2 bloques de ancho. Una red tiene una sola terminal. |
| **Storage Unit T1 / T2 / T3** | Agregan capacidad a la red: 18, 36 y 54 espacios. No se abren. Una red admite hasta **16 units**. |
| **Storage Pipe** | Conecta bloques de la red sin agregar espacio. Cambia de forma según lo que tenga alrededor. |

Los bloques se conectan por cualquiera de sus 6 caras (sin diagonales).

## Recetas

Todas se fabrican en la **mesa de trabajo**, categoría Tinkering.

| Ítem | Ingredientes | Nivel de mesa |
|---|---|---|
| Storage Terminal | 5 lingotes de hierro, 5 de cobre, 1 de oro | 1 |
| Storage Pipe (x4) | 1 lingote de hierro | 1 |
| Storage Unit T1 | 5 lingotes de hierro, 5 de cobre | 1 |
| Storage Unit T2 | 1 Storage Unit T1, 5 lingotes de cobalto, 5 de torio | 2 |
| Storage Unit T3 | 1 Storage Unit T2, 5 lingotes de adamantita, 5 de mitrilo | 3 |

## Cómo se usa

- Poné una **Terminal** y conectale **Units** directamente o con **Pipes**.
- Usá la Terminal (tecla de interacción, `F` por defecto) para abrir la ventana de la red.
  - **Ítems de la red:** clic izquierdo saca un stack; clic derecho saca uno.
  - **Tu inventario:** clic izquierdo guarda el stack; clic derecho guarda uno.
  - El **buscador** filtra por el nombre del ítem en tu idioma (sin importar tildes ni mayúsculas).
- La ventana muestra los espacios usados y la capacidad. Si la red se llena (o queda sobrecargada
  por romper units), todo sigue visible y se puede sacar, pero no acepta ítems nuevos hasta agregar
  o mejorar units.
- Las mesas de crafteo cercanas pueden usar los ítems guardados en la Terminal, igual que con un
  cofre.

### Reglas importantes

- **Romper una Unit no hace perder ítems:** solo baja la capacidad. Para mejorar una Unit, rompela,
  mejorala en la mesa y volvé a ponerla.
- **La Terminal no se puede romper mientras tenga ítems**, ni con herramientas ni con explosiones.
  Vaciala antes de moverla.
- No se puede poner un bloque que conecte **dos terminales** en la misma red ni que deje la red
  con **más de 16 units**.

### Multijugador

Todo funciona del lado del servidor: los ítems se guardan en el mundo, sobreviven a reinicios y
todos los jugadores ven la misma red. Si varios tienen la Terminal abierta, las ventanas se
actualizan al instante y las acciones se procesan de a una, así que no se duplican ítems.

## Desarrollo

Requisitos: Java 25 y el launcher de Hytale instalado.

```powershell
.\gradlew.bat build                         # compila el jar en build/libs/
.\gradlew.bat runServer --console=plain     # levanta un servidor local con el mod
```

- El código está en `src/main/java/dev/agustinr/networkstorage/`.
- Los modelos, texturas, íconos y JSON del Pipe, las Units y la Terminal se generan con
  `tools/generate-assets.ps1`. No se editan a mano: se cambia el script y se vuelve a correr:

  ```powershell
  powershell -ExecutionPolicy Bypass -File tools\generate-assets.ps1
  ```

- `CLAUDE.md` tiene el diseño completo y las notas técnicas sobre la API de Hytale.

## Créditos

- **[Magic Storage](https://github.com/blushiemagic/MagicStorage)** para Terraria, creado por
  **blushiemagic** (con sprites de AdipemDragon). La idea de una red de
  almacenamiento con unidades, una terminal central y una vista con todos los ítems agrupados viene
  de ese mod. Network Storage es una reinterpretación para Hytale escrita desde cero: no usa código
  ni assets de Magic Storage.
- **[Hytale Plugin Template](https://github.com/AzureDoom/plugin-template)** y el Gradle plugin
  `com.azuredoom.hytale-tools`, de **AzureDoom**, como base del proyecto.
- Estilo visual inspirado en la tecnología goblin de Hytale.

Network Storage es un mod de fans y no está afiliado a Hypixel Studios, Re-Logic ni a los autores de
Magic Storage.

**Autor:** agustinr
