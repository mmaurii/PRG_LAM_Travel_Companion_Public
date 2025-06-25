import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.travelcompanion.GpsPoint
import com.example.travelcompanion.TravelPoint
import com.example.travelcompanion.Trip
import com.example.travelcompanion.TripWithPoints

@Dao
interface TripDao {

    @Transaction
    @Query("SELECT * FROM trips ORDER BY date DESC")
    fun getAllTripsWithPoints(): LiveData<List<TripWithPoints>>

    @Insert
    suspend fun insertTrip(trip: Trip): Long

    @Insert
    suspend fun insertTravelPoint(point: TravelPoint)

    @Insert
    suspend fun insertGpsPoints(point: List<GpsPoint>)

    @Insert
    suspend fun insertGpsPoint(point: GpsPoint)

    @Query("SELECT MAX(date) FROM trips")
    suspend fun getLastTripDate(): Long?

    @Delete
    suspend fun deleteTrip(trip: Trip)

    @Query("DELETE FROM trips WHERE title IS NULL OR title = ''")
    suspend fun deleteTripsWithoutTitle()

    @Query("DELETE FROM trips WHERE id = :id")
    suspend fun deleteUninitializeTrip(id: Long)

    @Query("DELETE FROM travel_points WHERE id = :id")
    suspend fun deleteTravelPointById(id: Int)

    @Query("DELETE FROM travel_points WHERE tripId = :tripId")
    suspend fun deleteTravelPointByTripId(tripId: Long)

    @Query("SELECT * FROM travel_points WHERE tripId = :tripId")
    suspend fun getTravelPointsNow(tripId: Long): List<TravelPoint>

    @Query("SELECT * FROM travel_points")
    suspend fun getTravelPoints(): List<TravelPoint>

    @Query("SELECT * FROM travel_points WHERE tripId = :id")
    fun getTravelPointsById(id: Long): LiveData<List<TravelPoint>>

    @Transaction
    @Query("SELECT * FROM trips WHERE id = :id")
    fun getTripWithPoints(id: Long): LiveData<TripWithPoints>

    @Query("SELECT * FROM gps_point")
    suspend fun getGpsPoints(): List<GpsPoint>


    @Query("SELECT * FROM gps_point WHERE tripId = :tripId")
    suspend fun getPointsForTrip(tripId: Long): List<GpsPoint>

    @Query("DELETE FROM gps_point WHERE tripId = :tripId")
    suspend fun deletePointsByTripId(tripId: Long)

    @Update(entity = Trip::class)
    suspend fun updateTrip(newTrip: Trip)

    @Query(
        """
        SELECT DISTINCT t.*
        FROM trips t
        INNER JOIN travel_points p ON t.id = p.tripId
        WHERE p.timeStamp BETWEEN :startDate AND :endDate
    """
    )
    fun getTripsWithPointsBetweenDates(
        startDate: String,
        endDate: String
    ): LiveData<List<TripWithPoints>>
}
