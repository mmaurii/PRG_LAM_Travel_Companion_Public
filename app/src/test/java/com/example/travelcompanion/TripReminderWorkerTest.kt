import android.content.Context
import androidx.work.WorkerParameters
import com.example.travelcompanion.TripReminderWorker
import com.example.travelcompanion.NotificationHelper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.*
import org.junit.Assert.assertEquals
import androidx.work.ListenableWorker.Result

@OptIn(ExperimentalCoroutinesApi::class)
class TripReminderWorkerTest {

    private val context = mock<Context>()
    private val workerParams = mock<WorkerParameters>()
    private val tripDao = mock<TripDao>()
    private val notificationHelper = mock<NotificationHelper>()

    @Test
    fun `doWork should call sendReminder when last trip is too old`() = runTest {
        val oldTimestamp = System.currentTimeMillis() - 2 * 60 * 1000 // 2 minuti fa
        whenever(tripDao.getLastTripDate()).thenReturn(oldTimestamp)

        val worker = TripReminderWorker(
            context,
            workerParams,
            tripDao = tripDao,
            notificationHelper = notificationHelper
        )

        val result = worker.doWork()

        verify(notificationHelper).sendReminder()
        assert(result == Result.success())
    }

    @Test
    fun `doWork should not call sendReminder when last trip is recent`() = runTest {
        val recentTimestamp = System.currentTimeMillis()
        whenever(tripDao.getLastTripDate()).thenReturn(recentTimestamp)

        val worker = TripReminderWorker(
            context,
            workerParams,
            tripDao = tripDao,
            notificationHelper = notificationHelper
        )

        val result = worker.doWork()

        verify(notificationHelper, never()).sendReminder()
        assertEquals(Result.success(), result)
    }
}
