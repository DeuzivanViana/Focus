package com.focus.core.database

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.focus.core.model.ReviewItem
import com.focus.core.model.RoutineBlock
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "routine_blocks")
data class RoutineBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "subject_id") val subjectId: Long?,
    /** Bit 0 = Monday ... bit 6 = Sunday. */
    @ColumnInfo(name = "days_mask") val daysMask: Int,
    @ColumnInfo(name = "start_minute") val startMinute: Int,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    /** Null = no reminder. */
    @ColumnInfo(name = "remind_before") val remindBefore: Int?,
)

fun RoutineBlockEntity.asModel() =
    RoutineBlock(id, title, subjectId, RoutineBlock.maskToDays(daysMask), startMinute, durationMinutes, remindBefore)

fun RoutineBlock.asEntity() =
    RoutineBlockEntity(id, title, subjectId, RoutineBlock.daysToMask(days), startMinute, durationMinutes, remindBefore)

@Entity(tableName = "reviews")
data class ReviewEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "subject_id") val subjectId: Long?,
    /** Epoch day. */
    @ColumnInfo(name = "studied_day") val studiedDay: Long,
    @ColumnInfo(name = "stage") val stage: Int,
    @ColumnInfo(name = "created_at") val createdAt: Long,
)

fun ReviewEntity.asModel() = ReviewItem(id, title, subjectId, LocalDate.ofEpochDay(studiedDay), stage, createdAt)
fun ReviewItem.asEntity() = ReviewEntity(id, title, subjectId, studiedOn.toEpochDay(), stage, createdAt)

@Dao
interface PlanDao {
    @Query("SELECT * FROM routine_blocks ORDER BY start_minute ASC")
    fun observeBlocks(): Flow<List<RoutineBlockEntity>>

    @Query("SELECT * FROM routine_blocks")
    suspend fun getBlocks(): List<RoutineBlockEntity>

    @Insert
    suspend fun insertBlock(block: RoutineBlockEntity): Long

    @Update
    suspend fun updateBlock(block: RoutineBlockEntity)

    @Query("DELETE FROM routine_blocks WHERE id = :id")
    suspend fun deleteBlock(id: Long)

    @Query("SELECT * FROM reviews ORDER BY studied_day DESC")
    fun observeReviews(): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews")
    suspend fun getReviews(): List<ReviewEntity>

    @Insert
    suspend fun insertReview(review: ReviewEntity): Long

    @Update
    suspend fun updateReview(review: ReviewEntity)

    @Query("DELETE FROM reviews WHERE id = :id")
    suspend fun deleteReview(id: Long)
}
