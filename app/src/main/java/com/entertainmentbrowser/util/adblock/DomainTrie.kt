package com.entertainmentbrowser.util.adblock

/**
 * DomainTrie - Efficient data structure for domain matching.
 * 
 * Stores domains in reverse order (com.google.ads instead of ads.google.com)
 * to enable efficient subdomain matching with O(k) lookup where k = domain parts.
 * 
 * Benefits over HashSet:
 * - Subdomain matching: If "ads.google.com" is blocked, "tracker.ads.google.com" is also blocked
 * - Memory efficient: Shared prefixes stored once
 * - Fast lookup: O(k) where k = number of domain parts (typically 2-4)
 * 
 * Example:
 * - Add "doubleclick.net" → blocks "ad.doubleclick.net", "tracker.doubleclick.net", etc.
 * - Add "ads.google.com" → blocks "video.ads.google.com" but NOT "google.com"
 */
class DomainTrie {
    
    private class TrieNode {
        val children = HashMap<String, TrieNode>(4) // Most domains have few children
        var isBlocked = false
    }
    
    private val root = TrieNode()
    
    @Volatile
    private var domainCount = 0
    
    /**
     * Add a domain to the trie.
     * Domain is stored in reverse order for efficient subdomain matching.
     * 
     * @param domain Domain to block (e.g., "ads.google.com")
     */
    fun add(domain: String) {
        if (domain.isBlank()) return
        
        val parts = domain.lowercase().split(".").reversed()
        var node = root
        
        for (part in parts) {
            node = node.children.getOrPut(part) { TrieNode() }
        }
        
        if (!node.isBlocked) {
            node.isBlocked = true
            domainCount++
        }
    }
    
    /**
     * Add multiple domains efficiently.
     */
    fun addAll(domains: Collection<String>) {
        domains.forEach { add(it) }
    }
    
    /**
     * Check if a domain or any of its parent domains are blocked.
     * 
     * For "tracker.ads.google.com", checks:
     * 1. "com" (root TLD - unlikely to be blocked)
     * 2. "google.com" 
     * 3. "ads.google.com"
     * 4. "tracker.ads.google.com"
     * 
     * Returns true as soon as any parent is found to be blocked.
     * 
     * @param domain Domain to check
     * @return true if domain or any parent is blocked
     */
    fun isBlocked(domain: String): Boolean {
        if (domain.isBlank()) return false
        
        val parts = domain.lowercase().split(".").reversed()
        var node = root
        
        for (part in parts) {
            node = node.children[part] ?: return false
            if (node.isBlocked) return true
        }
        
        return false
    }
    
    /**
     * Check if exact domain is blocked (no subdomain matching).
     * 
     * @param domain Domain to check
     * @return true if exact domain is blocked
     */
    fun isExactMatch(domain: String): Boolean {
        if (domain.isBlank()) return false
        
        val parts = domain.lowercase().split(".").reversed()
        var node = root
        
        for (part in parts) {
            node = node.children[part] ?: return false
        }
        
        return node.isBlocked
    }
    
    /**
     * Remove a domain from the trie.
     * Note: This only unmarks the exact domain, not its children.
     * 
     * @param domain Domain to unblock
     * @return true if domain was found and removed
     */
    fun remove(domain: String): Boolean {
        if (domain.isBlank()) return false
        
        val parts = domain.lowercase().split(".").reversed()
        var node = root
        
        for (part in parts) {
            node = node.children[part] ?: return false
        }
        
        if (node.isBlocked) {
            node.isBlocked = false
            domainCount--
            return true
        }
        return false
    }
    
    /**
     * Get the number of blocked domains.
     */
    fun size(): Int = domainCount
    
    /**
     * Check if trie is empty.
     */
    fun isEmpty(): Boolean = domainCount == 0
    
    /**
     * Clear all domains from the trie.
     */
    fun clear() {
        root.children.clear()
        domainCount = 0
    }
    
    /**
     * Get statistics about the trie.
     */
    fun getStats(): Stats {
        var nodeCount = 0
        var maxDepth = 0
        
        fun traverse(node: TrieNode, depth: Int) {
            nodeCount++
            maxDepth = maxOf(maxDepth, depth)
            for (child in node.children.values) {
                traverse(child, depth + 1)
            }
        }
        
        traverse(root, 0)
        
        return Stats(
            domainCount = domainCount,
            nodeCount = nodeCount,
            maxDepth = maxDepth
        )
    }
    
    data class Stats(
        val domainCount: Int,
        val nodeCount: Int,
        val maxDepth: Int
    )
}
