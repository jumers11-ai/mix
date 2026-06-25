package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class MixgraphTab {
    DASHBOARD,
    TRACKS,
    FLOW_BUILDER,
    SET_GENERATOR,
    LIVE_MODE,
    AI_ASSISTANT
}

data class ChatMessage(
    val sender: String, // "user" or "assistant"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

class MixgraphViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MixgraphRepository.getRepository(application)

    // Current Active Tab
    private val _currentTab = MutableStateFlow(MixgraphTab.DASHBOARD)
    val currentTab: StateFlow<MixgraphTab> = _currentTab.asStateFlow()

    // Database Flows
    val tracks = repository.allTracks.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val flows = repository.allFlows.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val sessions = repository.allSessions.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val trackCount = repository.trackCount.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val flowCount = repository.flowCount.stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val sessionCount = repository.sessionCount.stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Average compatibility calculation of all pairs (for Dashboard stats)
    val averageCompatibility = tracks.map { list ->
        if (list.size < 2) 75 else {
            var sum = 0
            var count = 0
            for (i in list.indices) {
                for (j in i + 1 until list.size) {
                    sum += CompatibilityEngine.calculateCompatibility(list[i], list[j])
                    count++
                    if (count > 200) break // Cap search to keep dashboard loading instantaneous
                }
            }
            if (count > 0) sum / count else 75
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 75)

    // ----------------------------------------------------
    // TRACKS TAB: Filtering and Management States
    // ----------------------------------------------------
    val searchQuery = MutableStateFlow("")
    val filterGenre = MutableStateFlow("All")
    val filterCamelot = MutableStateFlow("All")
    val filterBpmMin = MutableStateFlow(110)
    val filterBpmMax = MutableStateFlow(140)
    val filterEnergyMin = MutableStateFlow(0)

    val filteredTracks = combine(
        tracks, searchQuery, filterGenre, filterCamelot, filterBpmMin, filterBpmMax, filterEnergyMin
    ) { flowsArray ->
        @Suppress("UNCHECKED_CAST")
        val list = flowsArray[0] as List<TrackEntity>
        val query = flowsArray[1] as String
        val genre = flowsArray[2] as String
        val camelot = flowsArray[3] as String
        val bpmMin = flowsArray[4] as Int
        val bpmMax = flowsArray[5] as Int
        val energyMin = flowsArray[6] as Int

        list.filter { track ->
            val matchQuery = query.isEmpty() || 
                    track.title.contains(query, ignoreCase = true) || 
                    track.artist.contains(query, ignoreCase = true)
            val matchGenre = genre == "All" || track.genre.equals(genre, ignoreCase = true)
            val matchCamelot = camelot == "All" || track.camelotKey.equals(camelot, ignoreCase = true)
            val matchBpm = track.bpm >= bpmMin && track.bpm <= bpmMax
            val matchEnergy = track.energy >= energyMin
            matchQuery && matchGenre && matchCamelot && matchBpm && matchEnergy
        }
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // List of unique genres in DB
    val availableGenres = tracks.map { list ->
        listOf("All") + list.map { it.genre }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Lazily, listOf("All"))

    fun addTrack(
        title: String,
        artist: String,
        bpm: Double,
        camelotKey: String,
        musicalKey: String,
        energy: Int,
        genre: String,
        durationSeconds: Int = 240
    ) {
        viewModelScope.launch {
            val track = TrackEntity(
                title = title,
                artist = artist,
                bpm = bpm,
                camelotKey = camelotKey.uppercase().trim(),
                musicalKey = musicalKey.trim(),
                energy = energy,
                genre = genre,
                releaseDate = "2026",
                duration = durationSeconds,
                danceability = 0.7 + (abs(energy - 50) / 100.0).coerceAtMost(0.25),
                valence = 0.5 + (energy - 50) / 200.0,
                loudness = -6.0 + (energy - 50) / 20.0
            )
            repository.insertTrack(track)
        }
    }

    fun deleteTrack(track: TrackEntity) {
        viewModelScope.launch {
            repository.deleteTrack(track)
        }
    }

    // ----------------------------------------------------
    // LIVE MODE TAB
    // ----------------------------------------------------
    private val _selectedLiveTrack = MutableStateFlow<TrackEntity?>(null)
    val selectedLiveTrack = _selectedLiveTrack.asStateFlow()

    val liveRecommendations = combine(
        selectedLiveTrack, tracks
    ) { selected, all ->
        selected?.let {
            val start = System.currentTimeMillis()
            val recs = CompatibilityEngine.getRecommendations(it, all, limit = 20)
            val end = System.currentTimeMillis()
            // Log or save performance (the user request specifies < 200 ms, this typically takes < 2 ms in Kotlin!)
            recs
        } ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun selectLiveTrack(track: TrackEntity) {
        _selectedLiveTrack.value = track
    }

    // ----------------------------------------------------
    // AUTO BUILD SET GENERATOR TAB
    // ----------------------------------------------------
    val setGenre = MutableStateFlow("All")
    val setBpmStart = MutableStateFlow(120)
    val setBpmEnd = MutableStateFlow(128)
    val setEnergyTarget = MutableStateFlow(80)
    val setLength = MutableStateFlow(8) // Number of tracks

    private val _generatedSet = MutableStateFlow<List<TrackEntity>>(emptyList())
    val generatedSet = _generatedSet.asStateFlow()

    fun generateSet() {
        viewModelScope.launch {
            val allTracksList = repository.getAllTracksList()
            if (allTracksList.isEmpty()) return@launch

            val preferredGenre = setGenre.value
            val startBpm = setBpmStart.value.toDouble()
            val endBpm = setBpmEnd.value.toDouble()
            val length = setLength.value
            val targetEnergy = setEnergyTarget.value

            val pool = if (preferredGenre == "All") {
                allTracksList
            } else {
                allTracksList.filter { it.genre.equals(preferredGenre, ignoreCase = true) }.ifEmpty { allTracksList }
            }

            val resultList = mutableListOf<TrackEntity>()
            val remaining = pool.toMutableList()

            // 1. Choose opening track closest to starting BPM and energy
            val opening = remaining.minByOrNull {
                abs(it.bpm - startBpm) * 2.0 + abs(it.energy - targetEnergy) * 0.5
            } ?: pool.first()

            resultList.add(opening)
            remaining.remove(opening)

            // 2. Greedy search with progression constraints
            for (i in 1 until length) {
                if (remaining.isEmpty()) break

                val prev = resultList.last()
                val progressRatio = i.toDouble() / (length - 1)
                val targetBpmForStep = startBpm + (endBpm - startBpm) * progressRatio

                val nextTrack = remaining.maxByOrNull { candidate ->
                    // Calculate raw compatibility
                    val compatibility = CompatibilityEngine.calculateCompatibility(prev, candidate)

                    // BPM progression weight: reward candidate that is close to the ideal step BPM
                    val bpmDiff = abs(candidate.bpm - targetBpmForStep)
                    val bpmProgressionScore = (20.0 - bpmDiff).coerceAtLeast(0.0) * 1.5 // up to 30 bonus points

                    compatibility + bpmProgressionScore
                }

                if (nextTrack != null) {
                    resultList.add(nextTrack)
                    remaining.remove(nextTrack)
                }
            }

            _generatedSet.value = resultList

            // Save the session history
            repository.insertSession(
                name = "Generowany Set: ${preferredGenre} (${setBpmStart.value}-${setBpmEnd.value} BPM)",
                type = "GENERATED"
            )
        }
    }

    fun saveGeneratedSetAsFlow(flowName: String) {
        val currentSet = _generatedSet.value
        if (currentSet.isEmpty()) return
        viewModelScope.launch {
            val flowId = repository.createFlow(flowName)
            val nodes = currentSet.mapIndexed { idx, track ->
                FlowNodeEntity(
                    flowId = flowId,
                    trackId = track.id,
                    x = 100f + (idx * 300f),
                    y = 250f + (if (idx % 2 == 0) -80f else 80f)
                )
            }
            val edges = mutableListOf<FlowEdgeEntity>()
            for (i in 0 until currentSet.size - 1) {
                edges.add(
                    FlowEdgeEntity(
                        flowId = flowId,
                        sourceTrackId = currentSet[i].id,
                        targetTrackId = currentSet[i + 1].id
                    )
                )
            }
            repository.saveFlowCanvas(flowId, nodes, edges)
            _selectedFlowId.value = flowId
            _currentTab.value = MixgraphTab.FLOW_BUILDER
        }
    }

    // ----------------------------------------------------
    // FLOW BUILDER (INTERACTIVE CANVAS STATE)
    // ----------------------------------------------------
    private val _selectedFlowId = MutableStateFlow<Long?>(null)
    val selectedFlowId = _selectedFlowId.asStateFlow()

    private val _canvasNodes = MutableStateFlow<List<FlowNodeEntity>>(emptyList())
    val canvasNodes = _canvasNodes.asStateFlow()

    private val _canvasEdges = MutableStateFlow<List<FlowEdgeEntity>>(emptyList())
    val canvasEdges = _canvasEdges.asStateFlow()

    init {
        // Collect nodes & edges automatically when flow selection changes
        viewModelScope.launch {
            selectedFlowId.collectLatest { flowId ->
                if (flowId != null) {
                    repository.getNodesForFlow(flowId).collect {
                        _canvasNodes.value = it
                    }
                } else {
                    _canvasNodes.value = emptyList()
                }
            }
        }

        viewModelScope.launch {
            selectedFlowId.collectLatest { flowId ->
                if (flowId != null) {
                    repository.getEdgesForFlow(flowId).collect {
                        _canvasEdges.value = it
                    }
                } else {
                    _canvasEdges.value = emptyList()
                }
            }
        }

        // Select the first flow if available
        viewModelScope.launch {
            flows.collectLatest { list ->
                if (selectedFlowId.value == null && list.isNotEmpty()) {
                    _selectedFlowId.value = list.first().id
                }
            }
        }
    }

    fun selectFlow(flowId: Long) {
        _selectedFlowId.value = flowId
    }

    fun createNewFlow(name: String) {
        viewModelScope.launch {
            val newId = repository.createFlow(name)
            _selectedFlowId.value = newId
        }
    }

    fun deleteCurrentFlow() {
        val flowId = selectedFlowId.value ?: return
        viewModelScope.launch {
            val current = flows.value.find { it.id == flowId }
            if (current != null) {
                repository.deleteFlow(current)
                _selectedFlowId.value = flows.value.firstOrNull { it.id != flowId }?.id
            }
        }
    }

    fun addTrackToCanvas(trackId: Long, x: Float = 200f, y: Float = 200f) {
        val flowId = selectedFlowId.value ?: return
        viewModelScope.launch {
            val currentNodes = _canvasNodes.value.toMutableList()
            val newNode = FlowNodeEntity(flowId = flowId, trackId = trackId, x = x, y = y)
            currentNodes.add(newNode)
            repository.saveFlowCanvas(flowId, currentNodes, _canvasEdges.value)
        }
    }

    fun removeNodeFromCanvas(nodeId: Long) {
        val flowId = selectedFlowId.value ?: return
        viewModelScope.launch {
            val node = _canvasNodes.value.find { it.id == nodeId } ?: return@launch
            val updatedNodes = _canvasNodes.value.filter { it.id != nodeId }
            // Also remove connected edges
            val updatedEdges = _canvasEdges.value.filter {
                it.sourceTrackId != node.trackId && it.targetTrackId != node.trackId
            }
            repository.saveFlowCanvas(flowId, updatedNodes, updatedEdges)
        }
    }

    fun updateNodePosition(nodeId: Long, x: Float, y: Float) {
        val flowId = selectedFlowId.value ?: return
        viewModelScope.launch {
            val updated = _canvasNodes.value.map {
                if (it.id == nodeId) it.copy(x = x, y = y) else it
            }
            _canvasNodes.value = updated
            // Debounced or direct save in database
            repository.saveFlowCanvas(flowId, updated, _canvasEdges.value)
        }
    }

    fun connectNodesInCanvas(sourceTrackId: Long, targetTrackId: Long) {
        val flowId = selectedFlowId.value ?: return
        if (sourceTrackId == targetTrackId) return
        viewModelScope.launch {
            // Avoid duplicate edges
            val exists = _canvasEdges.value.any {
                it.sourceTrackId == sourceTrackId && it.targetTrackId == targetTrackId
            }
            if (!exists) {
                val updatedEdges = _canvasEdges.value.toMutableList()
                updatedEdges.add(FlowEdgeEntity(flowId = flowId, sourceTrackId = sourceTrackId, targetTrackId = targetTrackId))
                repository.saveFlowCanvas(flowId, _canvasNodes.value, updatedEdges)
            }
        }
    }

    fun clearCanvas() {
        val flowId = selectedFlowId.value ?: return
        viewModelScope.launch {
            repository.saveFlowCanvas(flowId, emptyList(), emptyList())
        }
    }

    // ----------------------------------------------------
    // SMART DJ ASSISTANT (AI CHAT)
    // ----------------------------------------------------
    private val _chatHistory = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                sender = "assistant",
                content = "Cześć! Jestem Twoim inteligentnym asystentem DJ-a Mixgraph Solo. Pomogę Ci wyszukać idealne przejścia tonacyjne, stworzyć harmonogram setu muzycznego lub dobrać utwory z Twojej biblioteki. O co chcesz zapytać?"
            )
        )
    )
    val chatHistory = _chatHistory.asStateFlow()

    val chatInput = MutableStateFlow("")

    private val _isChatLoading = MutableStateFlow(false)
    val isChatLoading = _isChatLoading.asStateFlow()

    fun sendChatMessage() {
        val messageText = chatInput.value.trim()
        if (messageText.isEmpty()) return

        chatInput.value = ""
        val userMsg = ChatMessage(sender = "user", content = messageText)
        _chatHistory.value = _chatHistory.value + userMsg

        _isChatLoading.value = true

        viewModelScope.launch {
            val currentTracks = tracks.value
            val response = GeminiAssistant.chatWithAssistant(messageText, currentTracks)
            _chatHistory.value = _chatHistory.value + ChatMessage(sender = "assistant", content = response)
            _isChatLoading.value = false
        }
    }

    fun clearChat() {
        _chatHistory.value = listOf(
            ChatMessage(
                sender = "assistant",
                content = "Rozmowa została zresetowana. Jak mogę Ci pomóc w miksowaniu?"
            )
        )
    }

    fun selectTab(tab: MixgraphTab) {
        _currentTab.value = tab
    }
}
