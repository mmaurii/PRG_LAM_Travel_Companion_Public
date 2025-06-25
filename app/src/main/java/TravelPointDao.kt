import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.travelcompanion.TravelPoint

@Dao
interface TravelPointDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(travelPoint: TravelPoint)

    @Query("SELECT * FROM travel_points WHERE tripId = :id ORDER BY timeStamp DESC")
    fun getTravelPoints(id: Long): LiveData<List<TravelPoint>>

    @Query("SELECT * FROM travel_points ORDER BY timeStamp DESC")
    fun getAll(): LiveData<List<TravelPoint>>
}
