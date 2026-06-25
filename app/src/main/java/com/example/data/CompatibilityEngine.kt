package com.example.data

import kotlin.math.abs

object CompatibilityEngine {

    /**
     * Compute compatibility score between source and target track (0 to 100).
     *
     * Formula:
     * - 40% Harmonic Match (Camelot match)
     * - 25% BPM Match (tempo deviation)
     * - 20% Energy Match
     * - 10% Genre Similarity
     * - 5% Popularity Match
     */
    fun calculateCompatibility(source: TrackEntity, target: TrackEntity): Int {
        val harmonicPoints = getHarmonicScore(source.camelotKey, target.camelotKey) * 40.0 // 0.0 to 40.0
        val bpmPoints = getBpmScore(source.bpm, target.bpm) * 25.0 // 0.0 to 25.0
        val energyPoints = getEnergyScore(source.energy, target.energy) * 20.0 // 0.0 to 20.0
        val genrePoints = getGenreScore(source.genre, target.genre) * 10.0 // 0.0 to 10.0
        val popularityPoints = getPopularityScore(source.popularity, target.popularity) * 5.0 // 0.0 to 5.0

        val total = harmonicPoints + bpmPoints + energyPoints + genrePoints + popularityPoints
        return total.coerceIn(0.0, 100.0).toInt()
    }

    /**
     * Parse Camelot key into Number (1-12) and Letter ('A' or 'B').
     * e.g., "8A" -> Pair(8, 'A')
     */
    fun parseCamelot(key: String): Pair<Int, Char>? {
        val trimmed = key.trim().uppercase()
        if (trimmed.isEmpty()) return null
        val letter = trimmed.last()
        if (letter != 'A' && letter != 'B') return null
        val numberStr = trimmed.dropLast(1)
        val number = numberStr.toIntOrNull() ?: return null
        if (number !in 1..12) return null
        return Pair(number, letter)
    }

    /**
     * Calculates the harmonic score (0.0 to 1.0) based on Camelot Wheel rules.
     */
    fun getHarmonicScore(sourceKey: String, targetKey: String): Double {
        val src = parseCamelot(sourceKey) ?: return 0.1
        val dst = parseCamelot(targetKey) ?: return 0.1

        val (srcNum, srcLet) = src
        val (dstNum, dstLet) = dst

        if (srcNum == dstNum && srcLet == dstLet) {
            return 1.0 // Identical Key
        }

        val numDiff = abs(srcNum - dstNum)
        val distance = if (numDiff > 6) 12 - numDiff else numDiff

        // 1. Direct Harmonically Adjacent moves:
        // - Same number, different letter (e.g. 8A <-> 8B): relative major/minor swap
        // - Same letter, distance of exactly 1 (e.g. 8A <-> 9A or 8A <-> 7A)
        if (srcNum == dstNum && srcLet != dstLet) {
            return 0.95
        }
        if (distance == 1 && srcLet == dstLet) {
            return 0.90
        }

        // 2. Semi-compatible or interesting moves:
        // - Shift of letter and shift of number by 1 (diagonal move, e.g., 8A <-> 9B)
        if (distance == 1 && srcLet != dstLet) {
            return 0.65
        }

        // - Whole step jump (distance of exactly 2, e.g., 8A <-> 10A) - energy boost!
        if (distance == 2 && srcLet == dstLet) {
            return 0.50
        }

        // - Shift of 7 (major third shift e.g. 1A to 8A)
        if (distance == 7 && srcLet == dstLet) {
            return 0.40
        }

        return 0.15 // Incompatible (standard mix mismatch)
    }

    /**
     * Returns key relation label for transitions.
     */
    fun getKeyRelationLabel(sourceKey: String, targetKey: String): String {
        val src = parseCamelot(sourceKey) ?: return "Brak tonacji"
        val dst = parseCamelot(targetKey) ?: return "Brak tonacji"

        val (srcNum, srcLet) = src
        val (dstNum, dstLet) = dst

        if (srcNum == dstNum && srcLet == dstLet) return "Identyczna tonacja"
        if (srcNum == dstNum && srcLet != dstLet) return "Zamiana dur/moll"
        val numDiff = abs(srcNum - dstNum)
        val distance = if (numDiff > 6) 12 - numDiff else numDiff

        if (distance == 1 && srcLet == dstLet) return "Sąsiedni krok Camelota"
        if (distance == 1 && srcLet != dstLet) return "Przejście diagonalne"
        if (distance == 2 && srcLet == dstLet) return "Podbicie energii (+2)"
        if (distance == 7 && srcLet == dstLet) return "Zmiana tonacji (+7)"
        return "Niezgodna tonacja"
    }

    /**
     * BPM Match Score (0.0 to 1.0):
     * Ideal difference is 0. Difference <= 3 BPM gives 1.0.
     * Difference <= 8 BPM drops linearly to 0.5.
     * Difference <= 15 BPM drops to 0.0.
     */
    fun getBpmScore(bpm1: Double, bpm2: Double): Double {
        val diff = abs(bpm1 - bpm2)
        return when {
            diff <= 3.0 -> 1.0
            diff <= 8.0 -> 1.0 - 0.1 * (diff - 3.0)
            diff <= 15.0 -> 0.5 - (0.5 / 7.0) * (diff - 8.0)
            else -> 0.0
        }
    }

    /**
     * Energy score (0.0 to 1.0):
     * Difference of <= 10 is perfect (1.0).
     * Linear decline down to 0.0 for difference of 60.
     */
    fun getEnergyScore(e1: Int, e2: Int): Double {
        val diff = abs(e1 - e2)
        if (diff <= 10) return 1.0
        if (diff >= 60) return 0.0
        return 1.0 - (diff - 10) / 50.0
    }

    /**
     * Genre similarity (0.0 to 1.0):
     * Exact match: 1.0.
     * Substring overlap (e.g. "House" in both): 0.7.
     * Else: 0.1.
     */
    fun getGenreScore(g1: String, g2: String): Double {
        val g1L = g1.trim().lowercase()
        val g2L = g2.trim().lowercase()
        if (g1L == g2L) return 1.0
        if (g1L.contains("house") && g2L.contains("house")) return 0.8
        if (g1L.contains("techno") && g2L.contains("techno")) return 0.8
        if (g1L.contains("trance") && g2L.contains("trance")) return 0.8
        if (g1L.contains(g2L) || g2L.contains(g1L)) return 0.7
        return 0.1
    }

    /**
     * Popularity score (0.0 to 1.0):
     * Simple difference scaling.
     */
    fun getPopularityScore(p1: Int, p2: Int): Double {
        val diff = abs(p1 - p2)
        return (100 - diff) / 100.0
    }

    /**
     * Generates a structural explanation of the transition.
     */
    fun getTransitionExplanation(source: TrackEntity, target: TrackEntity, score: Int): String {
        val bpmDiff = target.bpm - source.bpm
        val keyLabel = getKeyRelationLabel(source.camelotKey, target.camelotKey)
        val energyTrend = when {
            target.energy > source.energy + 15 -> "Podbija energię"
            target.energy < source.energy - 15 -> "Obniża energię"
            else -> "Utrzymuje stałą energię"
        }

        val bpmSign = if (bpmDiff >= 0) "+" else ""
        val bpmStr = "${bpmSign}${String.format("%.1f", bpmDiff)} BPM"

        return "Kompatybilność: $score%. Tonacja: $keyLabel. Tempo: $bpmStr. $energyTrend."
    }

    /**
     * Build top recommended transitions.
     */
    fun getRecommendations(source: TrackEntity, allTracks: List<TrackEntity>, limit: Int = 20): List<Recommendation> {
        return allTracks
            .filter { it.id != source.id }
            .map { target ->
                val score = calculateCompatibility(source, target)
                Recommendation(
                    track = target,
                    score = score,
                    bpmDelta = target.bpm - source.bpm,
                    keyRelation = getKeyRelationLabel(source.camelotKey, target.camelotKey),
                    energyDelta = target.energy - source.energy,
                    explanation = getTransitionExplanation(source, target, score)
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
    }
}

data class Recommendation(
    val track: TrackEntity,
    val score: Int,
    val bpmDelta: Double,
    val keyRelation: String,
    val energyDelta: Int,
    val explanation: String
)
