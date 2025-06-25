package com.example.travelcompanion
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

@Entity(
    tableName = "travel_points",
    foreignKeys = [ForeignKey(
        entity = Trip::class,
        parentColumns = ["id"],
        childColumns = ["tripId"],
        onDelete = ForeignKey.CASCADE
    )]
)
@IgnoreExtraProperties // Per Firebase
@Parcelize
data class TravelPoint(
    val title: String,
    val timeStamp: Long,
    val notes: String,
    val foto: String,
    val latitude: Double,
    val longitude: Double,

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var tripId: Long = 0L
) : Parcelable {
    @Exclude
    fun getDocumentId(): String = "$title-$timeStamp"
}
