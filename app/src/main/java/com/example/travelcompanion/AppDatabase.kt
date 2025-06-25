import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.travelcompanion.GeofencePoint
import com.example.travelcompanion.GpsPoint
import com.example.travelcompanion.TravelPoint
import com.example.travelcompanion.Trip

@Database(entities = [Trip::class, TravelPoint::class, GpsPoint::class, GeofencePoint::class], version = 11)
abstract class AppDatabase : RoomDatabase() {
    abstract fun tripDao(): TripDao
    abstract fun geofenceDao(): GeofenceLocationDao

    companion object {
        @Volatile
        private var TRIP_INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return TRIP_INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "trip_db"
                )
                    .fallbackToDestructiveMigration() // ATTENZIONE: cancella i dati
                    .build()
                TRIP_INSTANCE = instance
                instance
            }
        }
    }
}
