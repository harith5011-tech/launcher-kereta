package my.kereta.launcher

import android.service.notification.NotificationListenerService

/**
 * Perlu ada supaya launcher dibenarkan membaca lagu yang sedang dimainkan oleh app lain
 * (Tetapan > Akses notifikasi). Servis ini tidak membaca atau menyimpan notifikasi.
 */
class MediaListener : NotificationListenerService()
