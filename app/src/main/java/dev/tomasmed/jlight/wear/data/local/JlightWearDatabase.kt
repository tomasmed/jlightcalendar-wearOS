package dev.tomasmed.jlight.wear.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import dev.tomasmed.jlight.wear.domain.model.CalendarEvent
import dev.tomasmed.jlight.wear.domain.model.ChoreItem
import dev.tomasmed.jlight.wear.domain.model.SharedListItem
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val galaxyId: String,
    val title: String,
    val dateIso: String,
    val timeIso: String?,
    val endDateIso: String?,
    val endTimeIso: String?,
    val memberIdsCsv: String,
    val rrule: String?,
    val exdatesCsv: String,
    val location: String?,
    val description: String?,
) {
    fun toDomain(): CalendarEvent =
        CalendarEvent(
            id = id,
            galaxyId = galaxyId,
            title = title,
            date = LocalDate.parse(dateIso),
            time = timeIso?.takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) },
            endDate = endDateIso?.takeIf { it.isNotBlank() }?.let { LocalDate.parse(it) },
            endTime = endTimeIso?.takeIf { it.isNotBlank() }?.let { LocalTime.parse(it) },
            memberIds = memberIdsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.ifEmpty { listOf("Everyone") },
            rrule = rrule,
            exdates = exdatesCsv.split(",")
                .mapNotNull { raw -> raw.trim().takeIf { it.isNotEmpty() }?.let { LocalDate.parse(it) } }
                .toSet(),
            location = location,
            description = description,
        )

    companion object {
        fun fromDomain(event: CalendarEvent): EventEntity =
            EventEntity(
                id = event.id,
                galaxyId = event.galaxyId,
                title = event.title,
                dateIso = event.date.toString(),
                timeIso = event.time?.toString(),
                endDateIso = event.endDate?.toString(),
                endTimeIso = event.endTime?.toString(),
                memberIdsCsv = event.memberIds.joinToString(","),
                rrule = event.rrule,
                exdatesCsv = event.exdates.joinToString(",") { it.toString() },
                location = event.location,
                description = event.description,
            )
    }
}

@Entity(tableName = "chores")
data class ChoreEntity(
    @PrimaryKey val id: String,
    val galaxyId: String,
    val title: String,
    val memberId: String,
    val completed: Boolean,
    val stars: Int,
    val frequency: String,
) {
    fun toDomain(): ChoreItem =
        ChoreItem(
            id = id,
            galaxyId = galaxyId,
            title = title,
            memberId = memberId,
            completed = completed,
            stars = stars,
            frequency = frequency,
        )

    companion object {
        fun fromDomain(chore: ChoreItem): ChoreEntity =
            ChoreEntity(
                id = chore.id,
                galaxyId = chore.galaxyId,
                title = chore.title,
                memberId = chore.memberId,
                completed = chore.completed,
                stars = chore.stars,
                frequency = chore.frequency,
            )
    }
}

@Entity(tableName = "shared_lists")
data class ListItemEntity(
    @PrimaryKey val id: String,
    val galaxyId: String,
    val text: String,
    val completed: Boolean,
    val category: String,
) {
    fun toDomain(): SharedListItem =
        SharedListItem(
            id = id,
            galaxyId = galaxyId,
            text = text,
            completed = completed,
            category = category,
        )

    companion object {
        fun fromDomain(item: SharedListItem): ListItemEntity =
            ListItemEntity(
                id = item.id,
                galaxyId = item.galaxyId,
                text = item.text,
                completed = item.completed,
                category = item.category,
            )
    }
}

@Dao
interface JlightWearDao {
    @Query("SELECT * FROM events WHERE galaxyId = :galaxyId")
    fun observeEvents(galaxyId: String): Flow<List<EventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEvents(events: List<EventEntity>)

    @Query("SELECT * FROM chores WHERE galaxyId = :galaxyId ORDER BY completed ASC, title ASC")
    fun observeChores(galaxyId: String): Flow<List<ChoreEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertChores(chores: List<ChoreEntity>)

    @Query("SELECT * FROM shared_lists WHERE galaxyId = :galaxyId ORDER BY completed ASC, text ASC")
    fun observeListItems(galaxyId: String): Flow<List<ListItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertListItems(items: List<ListItemEntity>)
}

@Database(
    entities = [EventEntity::class, ChoreEntity::class, ListItemEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class JlightWearDatabase : RoomDatabase() {
    abstract fun wearDao(): JlightWearDao
}
