package com.entertainmentbrowser.util.adblock

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/**
 * BloomFilter - Probabilistic data structure for fast negative lookups.
 * 
 * A Bloom filter can tell you with 100% certainty that an element is NOT in the set,
 * but may have false positives (saying something is in the set when it isn't).
 * 
 * For ad-blocking:
 * - If Bloom filter says "not blocked" → definitely not blocked (skip expensive checks)
 * - If Bloom filter says "maybe blocked" → do full check
 * 
 * This dramatically reduces CPU usage for legitimate URLs that aren't in any filter list.
 * 
 * @param expectedElements Expected number of elements to store
 * @param falsePositiveRate Desired false positive rate (0.01 = 1%)
 */
class BloomFilter(
    expectedElements: Int,
    falsePositiveRate: Double = 0.01
) {
    // Calculate optimal bit array size: m = -n*ln(p) / (ln(2)^2)
    private val bitArraySize: Int = run {
        val m = (-expectedElements * ln(falsePositiveRate) / (ln(2.0).pow(2))).toInt()
        // Ensure minimum size and round up to nearest 64 for efficient long array
        maxOf(64, ((m + 63) / 64) * 64)
    }
    
    // Calculate optimal number of hash functions: k = (m/n) * ln(2)
    private val numHashFunctions: Int = run {
        val k = (bitArraySize.toDouble() / expectedElements * ln(2.0)).toInt()
        maxOf(1, minOf(k, 10)) // Clamp between 1 and 10
    }
    
    // Bit array stored as LongArray for efficiency
    private val bits = LongArray(bitArraySize / 64)
    
    // Track number of elements added
    @Volatile
    private var elementCount = 0
    
    /**
     * Add an element to the Bloom filter.
     * Thread-safe via volatile reads/writes on individual bits.
     */
    fun add(element: String) {
        val hashes = getHashes(element)
        for (hash in hashes) {
            val index = abs(hash % bitArraySize)
            val longIndex = index / 64
            val bitIndex = index % 64
            synchronized(bits) {
                bits[longIndex] = bits[longIndex] or (1L shl bitIndex)
            }
        }
        elementCount++
    }
    
    /**
     * Add multiple elements efficiently.
     */
    fun addAll(elements: Collection<String>) {
        elements.forEach { add(it) }
    }
    
    /**
     * Check if element might be in the set.
     * 
     * @return false = definitely NOT in set (100% certain)
     *         true = MIGHT be in set (need to verify with actual lookup)
     */
    fun mightContain(element: String): Boolean {
        val hashes = getHashes(element)
        for (hash in hashes) {
            val index = abs(hash % bitArraySize)
            val longIndex = index / 64
            val bitIndex = index % 64
            if ((bits[longIndex] and (1L shl bitIndex)) == 0L) {
                return false // Definitely not in set
            }
        }
        return true // Might be in set
    }
    
    /**
     * Generate multiple hash values using double hashing technique.
     * Uses two independent hash functions to generate k hashes:
     * h(i) = h1 + i * h2
     */
    private fun getHashes(element: String): IntArray {
        val h1 = element.hashCode()
        val h2 = murmurHash(element)
        
        return IntArray(numHashFunctions) { i ->
            h1 + i * h2
        }
    }
    
    /**
     * Simple MurmurHash-inspired hash for second hash function.
     */
    private fun murmurHash(str: String): Int {
        var hash = 0x811c9dc5.toInt()
        for (c in str) {
            hash = hash xor c.code
            hash *= 0x01000193
        }
        return hash
    }
    
    /**
     * Get statistics about the Bloom filter.
     */
    fun getStats(): Stats {
        var setBits = 0L
        for (long in bits) {
            setBits += java.lang.Long.bitCount(long)
        }
        val fillRatio = setBits.toDouble() / bitArraySize
        
        return Stats(
            bitArraySize = bitArraySize,
            numHashFunctions = numHashFunctions,
            elementCount = elementCount,
            setBits = setBits.toInt(),
            fillRatio = fillRatio,
            estimatedFalsePositiveRate = fillRatio.pow(numHashFunctions.toDouble())
        )
    }
    
    data class Stats(
        val bitArraySize: Int,
        val numHashFunctions: Int,
        val elementCount: Int,
        val setBits: Int,
        val fillRatio: Double,
        val estimatedFalsePositiveRate: Double
    )
    
    /**
     * Clear the Bloom filter.
     */
    fun clear() {
        synchronized(bits) {
            for (i in bits.indices) {
                bits[i] = 0L
            }
        }
        elementCount = 0
    }
}
