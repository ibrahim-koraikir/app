package com.entertainmentbrowser.util.adblock

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Singleton that holds the current ad-blocking status for UI observation.
 * Allows the presentation layer to show warnings when ad-blocking is degraded.
 */
object AdBlockStatus {
    
    private const val TAG = "AdBlockStatus"
    
    /**
     * Detailed rule statistics from AdvancedAdBlockEngine.
     * Provides separate counts of loaded vs dropped rules for monitoring.
     */
    data class RuleStats(
        val blockedDomains: Int = 0,
        val blockedPaths: Int = 0,
        val wildcardPatternsLoaded: Int = 0,
        val wildcardPatternsDropped: Int = 0,
        val wildcardPatternsLimit: Int = 0,
        val regexPatternsLoaded: Int = 0,
        val regexPatternsDropped: Int = 0,
        val regexPatternsLimit: Int = 0,
        val totalRulesLoaded: Int = 0,
        val totalRulesDropped: Int = 0
    ) {
        fun getWildcardUtilization(): Float = 
            if (wildcardPatternsLimit > 0) wildcardPatternsLoaded.toFloat() / wildcardPatternsLimit else 0f
        
        fun getRegexUtilization(): Float = 
            if (regexPatternsLimit > 0) regexPatternsLoaded.toFloat() / regexPatternsLimit else 0f
        
        fun getTruncationPercentage(): Float {
            val total = totalRulesLoaded + totalRulesDropped
            return if (total > 0) (totalRulesDropped.toFloat() / total) * 100f else 0f
        }
        
        fun hasDroppedRules(): Boolean = totalRulesDropped > 0
    }
    
    /**
     * Represents the current state of ad-blocking engines.
     */
    data class Status(
        val isInitialized: Boolean = false,
        val isDegraded: Boolean = false,
        val isTruncated: Boolean = false,
        val fastEngineHealthy: Boolean = false,
        val advancedEngineHealthy: Boolean = false,
        val errorMessage: String? = null,
        val truncationPercentage: Float = 0f,
        val blockedDomainsCount: Int = 0,
        val lastUpdateTime: Long = 0L,
        val isRefreshing: Boolean = false,
        // Detailed rule statistics
        val ruleStats: RuleStats = RuleStats(),
        // Telemetry counters (privacy-safe, no PII)
        val initializationFailureCount: Int = 0,
        val truncationEventCount: Int = 0
    ) {
        /**
         * Returns true if ad-blocking is fully operational.
         */
        fun isFullyOperational(): Boolean = 
            isInitialized && !isDegraded && !isTruncated && fastEngineHealthy && advancedEngineHealthy
        
        /**
         * Returns true if both engines have failed.
         */
        fun isBothEnginesFailed(): Boolean = 
            isInitialized && !fastEngineHealthy && !advancedEngineHealthy
        
        /**
         * Returns a user-friendly status message.
         */
        fun getStatusMessage(): String = when {
            !isInitialized -> "Ad blocker initializing..."
            isBothEnginesFailed() -> "Ad blocking unavailable"
            isDegraded && !advancedEngineHealthy && fastEngineHealthy -> 
                "Basic ad blocking active"
            isDegraded && errorMessage != null -> "Ad blocking degraded: $errorMessage"
            isDegraded -> "Ad blocking is running in degraded mode"
            isTruncated -> "Ad blocking active (${truncationPercentage.toInt()}% rules truncated)"
            else -> "Ad blocking active ($blockedDomainsCount rules loaded)"
        }
        
        /**
         * Returns a detailed user-friendly message for degraded states.
         * Used in Settings screen to explain the current state.
         */
        fun getDetailedStatusMessage(): String = when {
            !isInitialized -> "Ad blocker is initializing. Please wait..."
            isBothEnginesFailed() -> 
                "Both ad-blocking engines failed to initialize. Tap Refresh to try updating filters."
            isDegraded && !advancedEngineHealthy && fastEngineHealthy -> 
                "Advanced blocking reduced due to filter size; basic blocking still active."
            isDegraded && !fastEngineHealthy && advancedEngineHealthy ->
                "Fast blocking engine failed; advanced blocking still active."
            isTruncated && ruleStats.hasDroppedRules() -> {
                val dropped = ruleStats.totalRulesDropped
                val wildcardDropped = ruleStats.wildcardPatternsDropped
                val regexDropped = ruleStats.regexPatternsDropped
                "Advanced blocking reduced: $dropped rules dropped (wildcard: $wildcardDropped, regex: $regexDropped). Basic blocking still active."
            }
            isTruncated -> "Some advanced rules were truncated. Basic blocking still active."
            else -> "Ad blocking is fully operational with ${blockedDomainsCount} rules loaded."
        }
    }
    
    private val _status = MutableStateFlow(Status())
    
    /**
     * Observable status flow for UI components.
     */
    val status: StateFlow<Status> = _status.asStateFlow()
    
    /**
     * Update the ad-blocking status after initialization.
     * Called from EntertainmentBrowserApp after engines are loaded.
     * 
     * @param isInitialized Whether initialization has completed
     * @param fastEngineHealthy Whether FastAdBlockEngine is healthy
     * @param advancedEngineHealthy Whether AdvancedAdBlockEngine is healthy
     * @param isTruncated Whether rules were truncated due to limits
     * @param truncationPercentage Percentage of rules that were dropped
     * @param blockedDomainsCount Total number of blocked domains
     * @param errorMessage Optional error message for degraded states
     * @param ruleStats Detailed rule statistics from AdvancedAdBlockEngine
     */
    fun updateStatus(
        isInitialized: Boolean,
        fastEngineHealthy: Boolean,
        advancedEngineHealthy: Boolean,
        isTruncated: Boolean = false,
        truncationPercentage: Float = 0f,
        blockedDomainsCount: Int = 0,
        errorMessage: String? = null,
        ruleStats: RuleStats = RuleStats()
    ) {
        val isDegraded = !fastEngineHealthy || !advancedEngineHealthy
        val currentStatus = _status.value
        
        // Increment telemetry counters
        val newInitFailureCount = if (isDegraded && !currentStatus.isDegraded) {
            currentStatus.initializationFailureCount + 1
        } else {
            currentStatus.initializationFailureCount
        }
        
        val newTruncationCount = if (isTruncated && !currentStatus.isTruncated) {
            currentStatus.truncationEventCount + 1
        } else {
            currentStatus.truncationEventCount
        }
        
        // Log telemetry (privacy-safe, no PII)
        if (isDegraded || isTruncated) {
            Log.i(TAG, "📊 AdBlock telemetry: degraded=$isDegraded, truncated=$isTruncated, " +
                "initFailures=$newInitFailureCount, truncationEvents=$newTruncationCount")
        }
        
        _status.value = Status(
            isInitialized = isInitialized,
            isDegraded = isDegraded,
            isTruncated = isTruncated,
            fastEngineHealthy = fastEngineHealthy,
            advancedEngineHealthy = advancedEngineHealthy,
            errorMessage = errorMessage,
            truncationPercentage = truncationPercentage,
            blockedDomainsCount = blockedDomainsCount,
            lastUpdateTime = System.currentTimeMillis(),
            isRefreshing = false,
            ruleStats = ruleStats,
            initializationFailureCount = newInitFailureCount,
            truncationEventCount = newTruncationCount
        )
    }
    
    /**
     * Mark that a filter refresh is in progress.
     */
    fun setRefreshing(refreshing: Boolean) {
        _status.value = _status.value.copy(isRefreshing = refreshing)
    }
    
    /**
     * Mark initialization failure.
     */
    fun setInitializationFailed(errorMessage: String) {
        _status.value = Status(
            isInitialized = true,
            isDegraded = true,
            fastEngineHealthy = false,
            advancedEngineHealthy = false,
            errorMessage = errorMessage,
            lastUpdateTime = System.currentTimeMillis()
        )
    }
    
    /**
     * Check if user should be warned about degraded ad-blocking.
     * Returns true only once per degraded state to avoid spamming.
     */
    private var hasShownDegradedWarning = false
    
    fun shouldShowDegradedWarning(): Boolean {
        val current = _status.value
        if ((current.isDegraded || current.isTruncated) && !hasShownDegradedWarning) {
            hasShownDegradedWarning = true
            return true
        }
        return false
    }
    
    /**
     * Reset the warning flag (e.g., after successful filter refresh).
     */
    fun resetWarningFlag() {
        hasShownDegradedWarning = false
    }
}
