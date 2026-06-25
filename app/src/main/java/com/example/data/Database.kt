package com.example.data

import android.content.Context
import androidx.room.*
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

// ==========================================
// ROOM ENTITIES
// ==========================================

@Entity(tableName = "tracks")
data class TrackEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val artist: String,
    val bpm: Double,
    val musicalKey: String, // e.g. "Am", "F#m"
    val camelotKey: String, // e.g. "8A", "11B"
    val energy: Int, // 0 - 100
    val danceability: Double, // 0.0 - 1.0
    val valence: Double, // 0.0 - 1.0
    val loudness: Double, // dB e.g. -6.5
    val duration: Int, // in seconds
    val genre: String,
    val releaseDate: String,
    val artworkUrl: String = "",
    val popularity: Int = 80 // 0 - 100
)

@Entity(tableName = "flows")
data class FlowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "flow_nodes",
    foreignKeys = [
        ForeignKey(
            entity = FlowEntity::class,
            parentColumns = ["id"],
            childColumns = ["flowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("flowId")]
)
data class FlowNodeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val flowId: Long,
    val trackId: Long,
    val x: Float,
    val y: Float
)

@Entity(
    tableName = "flow_edges",
    foreignKeys = [
        ForeignKey(
            entity = FlowEntity::class,
            parentColumns = ["id"],
            childColumns = ["flowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("flowId")]
)
data class FlowEdgeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val flowId: Long,
    val sourceTrackId: Long,
    val targetTrackId: Long
)

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false,
    val type: String // "LIVE" or "GENERATED"
)

// ==========================================
// DATA ACCESS OBJECTS (DAOs)
// ==========================================

@Dao
interface TrackDao {
    @Query("SELECT * FROM tracks ORDER BY artist ASC, title ASC")
    fun getAllTracksFlow(): Flow<List<TrackEntity>>

    @Query("SELECT * FROM tracks ORDER BY artist ASC, title ASC")
    suspend fun getAllTracks(): List<TrackEntity>

    @Query("SELECT * FROM tracks WHERE id = :id")
    suspend fun getTrackById(id: Long): TrackEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrack(track: TrackEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTracks(tracks: List<TrackEntity>)

    @Update
    suspend fun updateTrack(track: TrackEntity)

    @Delete
    suspend fun deleteTrack(track: TrackEntity)

    @Query("SELECT COUNT(*) FROM tracks")
    fun getTrackCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM tracks")
    suspend fun getTrackCount(): Int
}

@Dao
interface FlowDao {
    @Query("SELECT * FROM flows ORDER BY updatedAt DESC")
    fun getAllFlows(): Flow<List<FlowEntity>>

    @Query("SELECT * FROM flows WHERE id = :id")
    suspend fun getFlowById(id: Long): FlowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlow(flow: FlowEntity): Long

    @Update
    suspend fun updateFlow(flow: FlowEntity)

    @Delete
    suspend fun deleteFlow(flow: FlowEntity)

    // Flow Nodes
    @Query("SELECT * FROM flow_nodes WHERE flowId = :flowId")
    fun getNodesForFlow(flowId: Long): Flow<List<FlowNodeEntity>>

    @Query("SELECT * FROM flow_nodes WHERE flowId = :flowId")
    suspend fun getNodesForFlowSync(flowId: Long): List<FlowNodeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNode(node: FlowNodeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNodes(nodes: List<FlowNodeEntity>)

    @Query("DELETE FROM flow_nodes WHERE flowId = :flowId")
    suspend fun deleteNodesForFlow(flowId: Long)

    // Flow Edges
    @Query("SELECT * FROM flow_edges WHERE flowId = :flowId")
    fun getEdgesForFlow(flowId: Long): Flow<List<FlowEdgeEntity>>

    @Query("SELECT * FROM flow_edges WHERE flowId = :flowId")
    suspend fun getEdgesForFlowSync(flowId: Long): List<FlowEdgeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdge(edge: FlowEdgeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEdges(edges: List<FlowEdgeEntity>)

    @Query("DELETE FROM flow_edges WHERE flowId = :flowId")
    suspend fun deleteEdgesForFlow(flowId: Long)

    @Query("SELECT COUNT(*) FROM flows")
    fun getFlowCountFlow(): Flow<Int>
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY createdAt DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: SessionEntity): Long

    @Query("SELECT COUNT(*) FROM sessions")
    fun getSessionCountFlow(): Flow<Int>
}

// ==========================================
// DATABASE SETUP
// ==========================================

@Database(
    entities = [
        TrackEntity::class,
        FlowEntity::class,
        FlowNodeEntity::class,
        FlowEdgeEntity::class,
        SessionEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun trackDao(): TrackDao
    abstract fun flowDao(): FlowDao
    abstract fun sessionDao(): SessionDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mixgraph_database"
                )
                .addCallback(DatabaseCallback(context))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DatabaseCallback(private val context: Context) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            // Prepopulate some standard amazing DJ tracks so the user is ready to test immediately!
            INSTANCE?.let { database ->
                CoroutineScope(Dispatchers.IO).launch {
                    val trackDao = database.trackDao()
                    val preloaded = listOf(
                        TrackEntity(
                            title = "Losing It",
                            artist = "FISHER",
                            bpm = 125.0,
                            musicalKey = "Am",
                            camelotKey = "8A",
                            energy = 88,
                            danceability = 0.76,
                            valence = 0.65,
                            loudness = -5.4,
                            duration = 248,
                            genre = "Tech House",
                            releaseDate = "2018",
                            popularity = 95
                        ),
                        TrackEntity(
                            title = "Cola",
                            artist = "CamelPhat & Elderbrook",
                            bpm = 122.0,
                            musicalKey = "Am",
                            camelotKey = "8A",
                            energy = 78,
                            danceability = 0.82,
                            valence = 0.54,
                            loudness = -6.2,
                            duration = 223,
                            genre = "Tech House",
                            releaseDate = "2017",
                            popularity = 92
                        ),
                        TrackEntity(
                            title = "Starry Night",
                            artist = "Peggy Gou",
                            bpm = 123.0,
                            musicalKey = "D#m",
                            camelotKey = "12A",
                            energy = 82,
                            danceability = 0.78,
                            valence = 0.70,
                            loudness = -5.8,
                            duration = 354,
                            genre = "Deep House",
                            releaseDate = "2019",
                            popularity = 88
                        ),
                        TrackEntity(
                            title = "Innerbloom",
                            artist = "RÜFÜS DU SOL",
                            bpm = 120.0,
                            musicalKey = "G#m",
                            camelotKey = "1A",
                            energy = 68,
                            danceability = 0.65,
                            valence = 0.35,
                            loudness = -7.5,
                            duration = 578,
                            genre = "Melodic House",
                            releaseDate = "2016",
                            popularity = 94
                        ),
                        TrackEntity(
                            title = "Return to Oz (Artbat Remix)",
                            artist = "Monolink",
                            bpm = 124.0,
                            musicalKey = "Dm",
                            camelotKey = "7A",
                            energy = 80,
                            danceability = 0.68,
                            valence = 0.28,
                            loudness = -6.8,
                            duration = 480,
                            genre = "Melodic Techno",
                            releaseDate = "2019",
                            popularity = 91
                        ),
                        TrackEntity(
                            title = "Glue",
                            artist = "Bicep",
                            bpm = 130.0,
                            musicalKey = "F#m",
                            camelotKey = "11A",
                            energy = 75,
                            danceability = 0.70,
                            valence = 0.40,
                            loudness = -7.0,
                            duration = 269,
                            genre = "Breaks",
                            releaseDate = "2017",
                            popularity = 93
                        ),
                        TrackEntity(
                            title = "Where You Are",
                            artist = "John Summit & Hayla",
                            bpm = 126.0,
                            musicalKey = "C#",
                            camelotKey = "4B",
                            energy = 85,
                            danceability = 0.74,
                            valence = 0.45,
                            loudness = -5.2,
                            duration = 235,
                            genre = "Progressive House",
                            releaseDate = "2023",
                            popularity = 96
                        ),
                        TrackEntity(
                            title = "Delilah (pull me out of this)",
                            artist = "Fred again..",
                            bpm = 134.0,
                            musicalKey = "D#m",
                            camelotKey = "12A",
                            energy = 90,
                            danceability = 0.80,
                            valence = 0.58,
                            loudness = -4.8,
                            duration = 250,
                            genre = "UK House",
                            releaseDate = "2022",
                            popularity = 95
                        ),
                        TrackEntity(
                            title = "Rhyme Dust",
                            artist = "Dom Dolla & MK",
                            bpm = 128.0,
                            musicalKey = "Bbm",
                            camelotKey = "3A",
                            energy = 86,
                            danceability = 0.81,
                            valence = 0.62,
                            loudness = -5.5,
                            duration = 215,
                            genre = "Tech House",
                            releaseDate = "2023",
                            popularity = 90
                        ),
                        TrackEntity(
                            title = "Your Mind",
                            artist = "Adam Beyer & Bart Skils",
                            bpm = 127.0,
                            musicalKey = "F#m",
                            camelotKey = "11A",
                            energy = 92,
                            danceability = 0.72,
                            valence = 0.15,
                            loudness = -4.5,
                            duration = 504,
                            genre = "Techno",
                            releaseDate = "2018",
                            popularity = 89
                        ),
                        TrackEntity(
                            title = "Nova",
                            artist = "Tale Of Us",
                            bpm = 124.0,
                            musicalKey = "Em",
                            camelotKey = "9A",
                            energy = 72,
                            danceability = 0.63,
                            valence = 0.22,
                            loudness = -7.2,
                            duration = 432,
                            genre = "Melodic Techno",
                            releaseDate = "2018",
                            popularity = 85
                        ),
                        TrackEntity(
                            title = "Piece Of Your Heart",
                            artist = "Meduza",
                            bpm = 124.0,
                            musicalKey = "Am",
                            camelotKey = "8A",
                            energy = 84,
                            danceability = 0.88,
                            valence = 0.63,
                            loudness = -4.9,
                            duration = 153,
                            genre = "Dance-Pop",
                            releaseDate = "2019",
                            popularity = 94
                        ),
                        TrackEntity(
                            title = "Do It To It",
                            artist = "ACRAZE",
                            bpm = 125.0,
                            musicalKey = "Fm",
                            camelotKey = "4A",
                            energy = 85,
                            danceability = 0.85,
                            valence = 0.75,
                            loudness = -5.0,
                            duration = 158,
                            genre = "Tech House",
                            releaseDate = "2021",
                            popularity = 93
                        ),
                        TrackEntity(
                            title = "Gravity",
                            artist = "Boris Brejcha",
                            bpm = 125.0,
                            musicalKey = "Ebm",
                            camelotKey = "2A",
                            energy = 84,
                            danceability = 0.79,
                            valence = 0.48,
                            loudness = -6.0,
                            duration = 565,
                            genre = "High-Tech Minimal",
                            releaseDate = "2019",
                            popularity = 87
                        ),
                        TrackEntity(
                            title = "Opus",
                            artist = "Eric Prydz",
                            bpm = 126.0,
                            musicalKey = "Dm",
                            camelotKey = "7A",
                            energy = 89,
                            danceability = 0.60,
                            valence = 0.20,
                            loudness = -6.3,
                            duration = 543,
                            genre = "Progressive House",
                            releaseDate = "2015",
                            popularity = 92
                        ),
                        TrackEntity(
                            title = "The Business",
                            artist = "Tiësto",
                            bpm = 120.0,
                            musicalKey = "Abm",
                            camelotKey = "1A",
                            energy = 79,
                            danceability = 0.85,
                            valence = 0.56,
                            loudness = -5.8,
                            duration = 164,
                            genre = "Deep House",
                            releaseDate = "2020",
                            popularity = 94
                        )
                    )
                    trackDao.insertTracks(preloaded)

                    // Preload a default Flow
                    val flowDao = database.flowDao()
                    val flowId = flowDao.insertFlow(
                        FlowEntity(
                            name = "My Ultimate Set Opening",
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )

                    // Insert nodes (x, y coordinates for drawing)
                    val nodes = listOf(
                        FlowNodeEntity(flowId = flowId, trackId = 1, x = 150f, y = 150f), // Losing It
                        FlowNodeEntity(flowId = flowId, trackId = 2, x = 450f, y = 150f), // Cola
                        FlowNodeEntity(flowId = flowId, trackId = 12, x = 150f, y = 450f), // Piece of Your Heart
                        FlowNodeEntity(flowId = flowId, trackId = 13, x = 450f, y = 450f)  // Do It To It
                    )
                    flowDao.insertNodes(nodes)

                    // Insert edges (connections)
                    val edges = listOf(
                        FlowEdgeEntity(flowId = flowId, sourceTrackId = 1, targetTrackId = 2),
                        FlowEdgeEntity(flowId = flowId, sourceTrackId = 2, targetTrackId = 13),
                        FlowEdgeEntity(flowId = flowId, sourceTrackId = 1, targetTrackId = 12)
                    )
                    flowDao.insertEdges(edges)

                    // Preload some live/generated session count
                    val sessionDao = database.sessionDao()
                    sessionDao.insertSession(
                        SessionEntity(
                            name = "Tech House Club Set",
                            createdAt = System.currentTimeMillis() - 86400000,
                            isCompleted = true,
                            type = "GENERATED"
                        )
                    )
                    sessionDao.insertSession(
                        SessionEntity(
                            name = "Live Set @ Warehouse",
                            createdAt = System.currentTimeMillis() - 172800000,
                            isCompleted = true,
                            type = "LIVE"
                        )
                    )
                }
            }
        }
    }
}
