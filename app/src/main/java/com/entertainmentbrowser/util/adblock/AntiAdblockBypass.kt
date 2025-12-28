package com.entertainmentbrowser.util.adblock

import android.content.Context
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Coordinator class that manages anti-adblock bypass script injection.
 * 
 * This class determines when and what bypass scripts should be injected
 * into web pages to neutralize anti-adblock detection mechanisms.
 * 
 * Features:
 * - Provides bypass scripts for URLs (generic and site-specific)
 * - Manages an exclusion list for sites where bypass should not be applied
 * - Extracts domains from URLs for rule matching
 * 
 * @see AntiAdblockScripts for the actual JavaScript bypass code
 * 
 * Requirements: 6.1, 6.2, 6.3
 */
@Singleton
class AntiAdblockBypass @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * In-memory set of excluded domains where bypass should not be applied.
     * Thread-safe using synchronized access.
     */
    private val exclusionList: MutableSet<String> = mutableSetOf()
    
    /**
     * Lock object for thread-safe access to exclusion list.
     */
    private val exclusionLock = Any()


    /**
     * Extracts the domain from a URL string.
     * 
     * @param url The URL to extract domain from
     * @return The domain (host) or null if URL is invalid
     */
    private fun extractDomain(url: String?): String? {
        if (url.isNullOrBlank()) return null
        return try {
            val uri = Uri.parse(url)
            uri.host?.lowercase()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if bypass should be applied for the given URL.
     * 
     * Returns false if:
     * - URL is null or invalid
     * - Domain is in the exclusion list
     * 
     * Returns true for all other valid URLs.
     * 
     * @param url The URL to check
     * @return true if bypass should be applied, false otherwise
     * 
     * Requirements: 6.3
     */
    fun shouldApplyBypass(url: String?): Boolean {
        val domain = extractDomain(url) ?: return false
        return !isExcluded(domain)
    }

    /**
     * Gets the appropriate bypass script for a URL.
     * 
     * - If shouldApplyBypass returns false, returns empty string
     * - If site-specific rules exist for the domain, combines generic + site-specific scripts
     * - Otherwise, returns only the generic bypass script
     * 
     * @param url The URL to get bypass script for
     * @return The bypass script to inject, or empty string if bypass should not be applied
     * 
     * Requirements: 1.1, 2.1, 2.2
     */
    fun getBypassScript(url: String?): String {
        if (!shouldApplyBypass(url)) {
            return ""
        }
        
        val domain = extractDomain(url) ?: return ""
        val siteSpecificScript = getSiteSpecificRules(domain)
        
        return if (siteSpecificScript != null) {
            // Combine generic and site-specific scripts
            """
            (function() {
                try {
                    // === Generic Bypass ===
                    ${AntiAdblockScripts.genericBypassScript}
                    
                    // === Site-Specific Bypass ===
                    $siteSpecificScript
                    
                } catch(e) {
                    // Silently fail - don't break page functionality
                }
            })();
            """.trimIndent()
        } else {
            // Return only generic bypass script
            AntiAdblockScripts.genericBypassScript
        }
    }

    /**
     * Gets site-specific bypass rules for a domain if available.
     * 
     * @param domain The domain to check for site-specific rules
     * @return The site-specific script or null if no rules exist
     * 
     * Requirements: 2.1
     */
    fun getSiteSpecificRules(domain: String): String? {
        return AntiAdblockScripts.getSiteSpecificScript(domain)
    }

    /**
     * Gets the early bypass script for injection at onPageStarted.
     * Contains object spoofing and bait preservation.
     * 
     * @param url The URL to get early bypass script for
     * @return The early bypass script or empty string if bypass should not be applied
     */
    fun getEarlyBypassScript(url: String?): String {
        if (!shouldApplyBypass(url)) {
            return ""
        }
        return AntiAdblockScripts.earlyBypassScript
    }

    /**
     * Gets the late bypass script for injection at onPageFinished.
     * Contains element hiding functionality.
     * 
     * @param url The URL to get late bypass script for
     * @return The late bypass script or empty string if bypass should not be applied
     */
    fun getLateBypassScript(url: String?): String {
        if (!shouldApplyBypass(url)) {
            return ""
        }
        
        val domain = extractDomain(url) ?: return ""
        val siteSpecificScript = getSiteSpecificRules(domain)
        
        return if (siteSpecificScript != null) {
            // Combine element hiding with site-specific rules
            """
            (function() {
                try {
                    ${AntiAdblockScripts.lateBypassScript}
                    $siteSpecificScript
                } catch(e) {}
            })();
            """.trimIndent()
        } else {
            AntiAdblockScripts.lateBypassScript
        }
    }

    /**
     * Adds a domain to the exclusion list.
     * Bypass scripts will not be injected for excluded domains.
     * 
     * @param domain The domain to exclude (e.g., "example.com")
     * 
     * Requirements: 6.1
     */
    fun addToExclusionList(domain: String) {
        val normalizedDomain = domain.lowercase().trim()
        if (normalizedDomain.isNotEmpty()) {
            synchronized(exclusionLock) {
                exclusionList.add(normalizedDomain)
            }
        }
    }

    /**
     * Removes a domain from the exclusion list.
     * Bypass scripts will resume being injected for this domain.
     * 
     * @param domain The domain to remove from exclusion (e.g., "example.com")
     * 
     * Requirements: 6.2
     */
    fun removeFromExclusionList(domain: String) {
        val normalizedDomain = domain.lowercase().trim()
        synchronized(exclusionLock) {
            exclusionList.remove(normalizedDomain)
        }
    }

    /**
     * Checks if a domain is in the exclusion list.
     * 
     * @param domain The domain to check
     * @return true if the domain is excluded, false otherwise
     * 
     * Requirements: 6.3
     */
    fun isExcluded(domain: String): Boolean {
        val normalizedDomain = domain.lowercase().trim()
        synchronized(exclusionLock) {
            // Check for exact match or if any excluded domain is contained in the URL domain
            return exclusionList.any { excluded ->
                normalizedDomain == excluded || normalizedDomain.endsWith(".$excluded")
            }
        }
    }

    /**
     * Gets a copy of the current exclusion list.
     * 
     * @return Set of excluded domains
     */
    fun getExclusionList(): Set<String> {
        synchronized(exclusionLock) {
            return exclusionList.toSet()
        }
    }

    /**
     * Clears all domains from the exclusion list.
     */
    fun clearExclusionList() {
        synchronized(exclusionLock) {
            exclusionList.clear()
        }
    }
}
