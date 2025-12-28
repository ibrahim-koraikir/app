package com.entertainmentbrowser.util

import android.net.Uri
import com.entertainmentbrowser.domain.model.SearchEngine
import java.net.URLEncoder
import java.util.Locale

/**
 * Utility object for URL parsing and search query handling.
 */
object UrlUtils {
    
    /**
     * Whether to append locale parameters to search URLs.
     * Can be toggled for testing or user preference.
     */
    var useLocaleInSearch: Boolean = true
    
    // Common TLDs for URL detection
    private val COMMON_TLDS = setOf(
        "com", "org", "net", "edu", "gov", "io", "co", "me", "tv", "info",
        "biz", "app", "dev", "xyz", "online", "site", "tech", "store", "blog"
    )
    
    // IPv4 pattern
    private val IPV4_PATTERN = Regex(
        """^(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})(:\d+)?(/.*)?$"""
    )
    
    // IPv6 pattern (simplified)
    private val IPV6_PATTERN = Regex(
        """^\[?([a-fA-F0-9:]+)\]?(:\d+)?(/.*)?$"""
    )
    
    /**
     * Determines if the input looks like a URL or a search query.
     * Returns the properly formatted URL if it's a URL, or a search URL using the specified engine.
     * 
     * @param input The user input (URL or search query)
     * @param searchEngine The search engine to use for queries (defaults to Google)
     */
    fun resolveSearchInput(
        input: String,
        searchEngine: SearchEngine = SearchEngine.GOOGLE
    ): String {
        val trimmed = input.trim()
        
        if (trimmed.isBlank()) return ""
        
        // Already has a scheme
        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            return normalizeUrl(trimmed)
        }
        
        // Contains whitespace - definitely a search query
        if (trimmed.contains(" ") || trimmed.contains("\t")) {
            return buildSearchUrl(trimmed, searchEngine)
        }
        
        // Check if it looks like a URL
        if (looksLikeUrl(trimmed)) {
            return "https://${normalizeHost(trimmed)}"
        }
        
        // Default to search
        return buildSearchUrl(trimmed, searchEngine)
    }
    
    /**
     * Checks if the input looks like a URL (host, IP, or domain).
     */
    private fun looksLikeUrl(input: String): Boolean {
        val normalized = input.lowercase()
        
        // Check for localhost
        if (normalized == "localhost" || normalized.startsWith("localhost:") || normalized.startsWith("localhost/")) {
            return true
        }
        
        // Check for IPv4 address
        if (isValidIpv4(normalized)) {
            return true
        }
        
        // Check for IPv6 address
        if (IPV6_PATTERN.matches(normalized)) {
            return true
        }
        
        // Check for domain with TLD
        if (hasDomainStructure(normalized)) {
            return true
        }
        
        // Check if it has a port number (e.g., "myserver:8080")
        if (normalized.contains(":") && normalized.substringAfter(":").all { it.isDigit() || it == '/' }) {
            return true
        }
        
        return false
    }
    
    /**
     * Validates IPv4 address format.
     */
    private fun isValidIpv4(input: String): Boolean {
        val match = IPV4_PATTERN.matchEntire(input) ?: return false
        
        // Validate each octet is 0-255
        for (i in 1..4) {
            val octet = match.groupValues[i].toIntOrNull() ?: return false
            if (octet < 0 || octet > 255) return false
        }
        return true
    }
    
    /**
     * Checks if input has a valid domain structure (e.g., example.com, sub.example.org).
     */
    private fun hasDomainStructure(input: String): Boolean {
        // Remove path and port for analysis
        val hostPart = input.substringBefore("/").substringBefore(":")
        
        if (!hostPart.contains(".")) return false
        
        val parts = hostPart.split(".")
        if (parts.size < 2) return false
        
        // Check if last part is a valid TLD or looks like one
        val tld = parts.last().lowercase()
        
        // Known TLD
        if (tld in COMMON_TLDS) return true
        
        // Country code TLD (2 letters)
        if (tld.length == 2 && tld.all { it.isLetter() }) return true
        
        // Generic TLD pattern (2-6 letters)
        if (tld.length in 2..6 && tld.all { it.isLetter() }) return true
        
        return false
    }
    
    /**
     * Normalizes a host string by trimming, lowercasing, and removing trailing dots.
     */
    private fun normalizeHost(input: String): String {
        var result = input.trim().lowercase()
        
        // Remove trailing dots from host
        val pathIndex = result.indexOf("/")
        if (pathIndex > 0) {
            val host = result.substring(0, pathIndex).trimEnd('.')
            val path = result.substring(pathIndex)
            result = host + path
        } else {
            result = result.trimEnd('.')
        }
        
        return result
    }
    
    /**
     * Normalizes a full URL.
     */
    private fun normalizeUrl(url: String): String {
        return try {
            val uri = Uri.parse(url)
            val host = uri.host?.lowercase()?.trimEnd('.') ?: return url
            
            Uri.Builder()
                .scheme(uri.scheme ?: "https")
                .encodedAuthority(
                    if (uri.port != -1) "$host:${uri.port}" else host
                )
                .encodedPath(uri.encodedPath ?: "")
                .encodedQuery(uri.encodedQuery)
                .encodedFragment(uri.encodedFragment)
                .build()
                .toString()
        } catch (e: Exception) {
            url
        }
    }
    
    /**
     * Builds a search URL for the given query using the specified search engine.
     * Optionally appends locale parameters based on device settings.
     */
    private fun buildSearchUrl(query: String, searchEngine: SearchEngine): String {
        val encodedQuery = URLEncoder.encode(query.trim(), "UTF-8")
        val baseUrl = searchEngine.searchUrlTemplate.replace("%s", encodedQuery)
        
        if (!useLocaleInSearch) {
            return baseUrl
        }
        
        return appendLocaleParams(baseUrl, searchEngine)
    }
    
    /**
     * Appends locale parameters to the search URL based on device locale.
     */
    private fun appendLocaleParams(baseUrl: String, searchEngine: SearchEngine): String {
        val locale = Locale.getDefault()
        val language = locale.language.lowercase() // e.g., "en", "ar", "es"
        val region = locale.country.uppercase()    // e.g., "US", "SA", "MX"
        
        val params = StringBuilder()
        
        // Append language parameter if supported
        searchEngine.languageParam?.let { param ->
            val langValue = when (searchEngine) {
                SearchEngine.DUCKDUCKGO -> {
                    // DuckDuckGo uses combined format like "us-en", "ar-xa"
                    if (region.isNotEmpty()) "${region.lowercase()}-$language" else language
                }
                SearchEngine.STARTPAGE -> {
                    // Startpage uses format like "english", "arabic"
                    getStartpageLanguage(language)
                }
                else -> language
            }
            params.append("&$param=$langValue")
        }
        
        // Append region parameter if supported (and different from language param)
        searchEngine.regionParam?.let { param ->
            if (region.isNotEmpty()) {
                params.append("&$param=$region")
            }
        }
        
        return baseUrl + params.toString()
    }
    
    /**
     * Maps ISO language codes to Startpage language names.
     */
    private fun getStartpageLanguage(langCode: String): String {
        return when (langCode) {
            "ar" -> "arabic"
            "zh" -> "chinese"
            "nl" -> "dutch"
            "en" -> "english"
            "fr" -> "french"
            "de" -> "german"
            "it" -> "italian"
            "ja" -> "japanese"
            "ko" -> "korean"
            "pl" -> "polish"
            "pt" -> "portuguese"
            "ru" -> "russian"
            "es" -> "spanish"
            "tr" -> "turkish"
            else -> "english"
        }
    }
}
