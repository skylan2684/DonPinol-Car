package com.donpinol.car

import android.service.notification.NotificationListenerService

/**
 * Servicio vacío: solo existe para que Android le dé a DonPinol Car permiso de
 * leer la música que suena (MediaSession). No lee ni guarda notificaciones.
 */
class MediaListenerService : NotificationListenerService()
