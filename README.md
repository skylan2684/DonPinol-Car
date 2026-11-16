# DonPinol Car

Launcher (pantalla principal) para la radio Android del carro de Pinol
(plataforma TS7, Android 11, ARMv7 32 bits, 2 GB RAM, pantalla 1024×600).
También funciona en un teléfono Android 8 o superior.

## Qué muestra (solo datos que la radio sí tiene)
- Velocidad, rumbo y kilómetros del viaje: GPS de la radio.
- Hora, fecha, wifi y Bluetooth: sistema.
- Navegación: botones Casa y Trabajo (dirección guardada; mantener presionado para cambiarla) y Abrir mapa.
- Música: canción, portada y controles de cualquier reproductor (requiere activar "Acceso a notificaciones").
- Accesos: Mapas, Música, Radio FM, Teléfono, Apps, Ajustes (abren las apps que ya trae la radio).

No incluye gasolina, temperatura, RPM ni puertas: la radio no recibe datos CAN del carro.

## Descargar
Cada cambio en `main` compila el APK con GitHub Actions y lo publica en **Releases** como `DonPinolCar.apk`.

## Instalar en la radio
1. Copiar `DonPinolCar.apk` a una memoria USB y abrirlo con el administrador de archivos de la radio.
2. Permitir "orígenes desconocidos" si lo pide.
3. Pulsar Inicio y elegir **DonPinol Car** → "Siempre".
4. Aceptar el permiso de ubicación y, para la música, activar DonPinol Car en "Acceso a notificaciones".
