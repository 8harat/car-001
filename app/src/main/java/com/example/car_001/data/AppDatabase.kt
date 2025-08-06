package com.example.car_001.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings WHERE id = 1")
    suspend fun getSettings(): Settings?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettings(settings: Settings)
    
    @Update
    suspend fun updateSettings(settings: Settings)
}

@Dao
interface CrashAlertDao {
    @Query("SELECT * FROM crash_alerts ORDER BY receivedAt DESC")
    fun getAllAlerts(): Flow<List<CrashAlertEntity>>
    
    @Query("SELECT * FROM crash_alerts WHERE status = 'new' ORDER BY receivedAt DESC")
    fun getNewAlerts(): Flow<List<CrashAlertEntity>>
    
    @Insert
    suspend fun insertAlert(alert: CrashAlertEntity)
    
    @Update
    suspend fun updateAlert(alert: CrashAlertEntity)
    
    @Query("UPDATE crash_alerts SET status = :status WHERE id = :alertId")
    suspend fun updateAlertStatus(alertId: Int, status: String)
    
    @Delete
    suspend fun deleteAlert(alert: CrashAlertEntity)
}

@Database(
    entities = [Settings::class, CrashAlertEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun settingsDao(): SettingsDao
    abstract fun crashAlertDao(): CrashAlertDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: android.content.Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "car_crash_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 