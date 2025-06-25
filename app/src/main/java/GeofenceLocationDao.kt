import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.travelcompanion.GeofencePoint
import kotlinx.coroutines.flow.Flow

@Dao
interface GeofenceLocationDao {
    @Query("SELECT * FROM geofences")
    fun getAll(): Flow<List<GeofencePoint>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(geofencePoint: GeofencePoint)

    @Delete
    suspend fun delete(geofencePoint: GeofencePoint)
}
