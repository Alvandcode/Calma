package app.calma.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import app.calma.data.local.dao.CalmDao
import app.calma.data.local.dao.FocusDao
import app.calma.data.local.dao.MoodDao
import app.calma.data.local.dao.SoundMixDao
import app.calma.data.local.entity.ActiveSessionEntity
import app.calma.data.local.entity.CalmSessionEntity
import app.calma.data.local.entity.FocusSessionEntity
import app.calma.data.local.entity.MixLayerEntity
import app.calma.data.local.entity.MoodEntryEntity
import app.calma.data.local.entity.SoundMixEntity

@Database(
    entities = [
        FocusSessionEntity::class,
        ActiveSessionEntity::class,
        MoodEntryEntity::class,
        CalmSessionEntity::class,
        SoundMixEntity::class,
        MixLayerEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class CalmaDatabase : RoomDatabase() {

    abstract fun focusDao(): FocusDao
    abstract fun moodDao(): MoodDao
    abstract fun calmDao(): CalmDao
    abstract fun soundMixDao(): SoundMixDao

    companion object {
        const val NAME = "calma.db"

        @Volatile
        private var instance: CalmaDatabase? = null

        fun get(context: Context): CalmaDatabase = instance ?: synchronized(this) {
            instance ?: build(context.applicationContext).also { instance = it }
        }

        private fun build(context: Context): CalmaDatabase =
            Room.databaseBuilder(context, CalmaDatabase::class.java, NAME)
                // No migrations exist yet (schema v1). Exporting schemas to app/schemas
                // means future versions can ship a real Migration instead of
                // destructive fallback, which protects users' journal data.
                .build()

        internal fun inMemory(context: Context): CalmaDatabase =
            Room.inMemoryDatabaseBuilder(context, CalmaDatabase::class.java)
                .allowMainThreadQueries()
                .build()
    }
}