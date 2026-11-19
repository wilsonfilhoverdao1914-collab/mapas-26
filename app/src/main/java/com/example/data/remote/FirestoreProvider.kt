package com.example.data.remote

import android.content.Context
import com.example.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.PersistentCacheSettings

object FirestoreProvider {
  @Volatile private var configured = false

  fun getDatabase(context: Context): FirebaseFirestore {
    val appContext = context.applicationContext
    val databaseId = appContext.getString(R.string.firestore_database_id)
    val db = FirebaseFirestore.getInstance(databaseId)
    if (!configured) {
      synchronized(this) {
        if (!configured) {
          try {
            val cacheSettings =
              PersistentCacheSettings.newBuilder()
                .setSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            val settings =
              FirebaseFirestoreSettings.Builder()
                .setLocalCacheSettings(cacheSettings)
                .build()
            db.firestoreSettings = settings
          } catch (_: IllegalStateException) {
            // Já configurado anteriormente no ciclo de vida do processo.
          } finally {
            configured = true
          }
        }
      }
    }
    return db
  }
}
