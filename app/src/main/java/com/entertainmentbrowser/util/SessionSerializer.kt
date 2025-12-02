package com.entertainmentbrowser.util

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Utility object for serializing and deserializing session data.
 * Uses Kotlinx Serialization for JSON encoding/decoding of tab IDs.
 */
object SessionSerializer {
    
    /**
     * Serializes a list of tab IDs to a JSON string.
     * 
     * @param tabIds List of tab IDs to serialize
     * @return JSON string representation of the tab IDs
     * 
     * Example: ["tab1", "tab2", "tab3"] -> "[\"tab1\",\"tab2\",\"tab3\"]"
     */
    fun serializeTabIds(tabIds: List<String>): String {
        return Json.encodeToString(tabIds)
    }
    
    /**
     * Deserializes a JSON string to a list of tab IDs.
     * 
     * @param json JSON string containing tab IDs
     * @return List of tab IDs
     * 
     * Example: "[\"tab1\",\"tab2\",\"tab3\"]" -> ["tab1", "tab2", "tab3"]
     */
    fun deserializeTabIds(json: String): List<String> {
        return try {
            Json.decodeFromString(json)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
