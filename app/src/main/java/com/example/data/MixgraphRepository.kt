package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow

class MixgraphRepository(private val db: AppDatabase) {

    val allTracks: Flow<List<TrackEntity>> = db.trackDao().getAllTracksFlow()
    val allFlows: Flow<List<FlowEntity>> = db.flowDao().getAllFlows()
    val allSessions: Flow<List<SessionEntity>> = db.sessionDao().getAllSessions()

    val trackCount: Flow<Int> = db.trackDao().getTrackCountFlow()
    val flowCount: Flow<Int> = db.flowDao().getFlowCountFlow()
    val sessionCount: Flow<Int> = db.sessionDao().getSessionCountFlow()

    // Tracks
    suspend fun getAllTracksList(): List<TrackEntity> {
        return db.trackDao().getAllTracks()
    }

    suspend fun getTrackById(id: Long): TrackEntity? {
        return db.trackDao().getTrackById(id)
    }

    suspend fun insertTrack(track: TrackEntity): Long {
        return db.trackDao().insertTrack(track)
    }

    suspend fun updateTrack(track: TrackEntity) {
        db.trackDao().updateTrack(track)
    }

    suspend fun deleteTrack(track: TrackEntity) {
        db.trackDao().deleteTrack(track)
    }

    // Flows
    suspend fun getFlowById(id: Long): FlowEntity? {
        return db.flowDao().getFlowById(id)
    }

    suspend fun createFlow(name: String): Long {
        return db.flowDao().insertFlow(FlowEntity(name = name))
    }

    suspend fun updateFlowName(flow: FlowEntity, newName: String) {
        db.flowDao().updateFlow(flow.copy(name = newName, updatedAt = System.currentTimeMillis()))
    }

    suspend fun deleteFlow(flow: FlowEntity) {
        db.flowDao().deleteFlow(flow)
    }

    // Nodes & Edges
    fun getNodesForFlow(flowId: Long): Flow<List<FlowNodeEntity>> {
        return db.flowDao().getNodesForFlow(flowId)
    }

    fun getEdgesForFlow(flowId: Long): Flow<List<FlowEdgeEntity>> {
        return db.flowDao().getEdgesForFlow(flowId)
    }

    suspend fun saveFlowCanvas(flowId: Long, nodes: List<FlowNodeEntity>, edges: List<FlowEdgeEntity>) {
        db.flowDao().deleteNodesForFlow(flowId)
        db.flowDao().deleteEdgesForFlow(flowId)
        db.flowDao().insertNodes(nodes)
        db.flowDao().insertEdges(edges)
        
        // Touch the updated timestamp of the flow
        db.flowDao().getFlowById(flowId)?.let { flow ->
            db.flowDao().updateFlow(flow.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    // Sessions
    suspend fun insertSession(name: String, type: String): Long {
        return db.sessionDao().insertSession(
            SessionEntity(
                name = name,
                type = type,
                createdAt = System.currentTimeMillis(),
                isCompleted = true
            )
        )
    }

    companion object {
        @Volatile
        private var INSTANCE: MixgraphRepository? = null

        fun getRepository(context: Context): MixgraphRepository {
            return INSTANCE ?: synchronized(this) {
                val db = AppDatabase.getDatabase(context)
                val repository = MixgraphRepository(db)
                INSTANCE = repository
                repository
            }
        }
    }
}
