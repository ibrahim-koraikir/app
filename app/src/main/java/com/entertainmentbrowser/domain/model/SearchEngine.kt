package com.entertainmentbrowser.domain.model

/**
 * Supported search engines for the browser.
 * 
 * @param displayName User-friendly name shown in settings
 * @param searchUrlTemplate URL template with %s placeholder for query
 * @param languageParam Parameter name for language/locale (null if not supported)
 * @param regionParam Parameter name for region (null if not supported)
 */
enum class SearchEngine(
    val displayName: String,
    val searchUrlTemplate: String,
    val languageParam: String? = null,
    val regionParam: String? = null
) {
    GOOGLE(
        displayName = "Google",
        searchUrlTemplate = "https://www.google.com/search?q=%s",
        languageParam = "hl",
        regionParam = "gl"
    ),
    DUCKDUCKGO(
        displayName = "DuckDuckGo",
        searchUrlTemplate = "https://duckduckgo.com/?q=%s",
        languageParam = "kl"  // DuckDuckGo uses kl for region/language combo
    ),
    BING(
        displayName = "Bing",
        searchUrlTemplate = "https://www.bing.com/search?q=%s",
        languageParam = "setlang",
        regionParam = "cc"
    ),
    STARTPAGE(
        displayName = "Startpage",
        searchUrlTemplate = "https://www.startpage.com/sp/search?query=%s",
        languageParam = "language"
    ),
    ECOSIA(
        displayName = "Ecosia",
        searchUrlTemplate = "https://www.ecosia.org/search?q=%s"
        // Ecosia auto-detects locale
    );

    companion object {
        fun fromOrdinal(ordinal: Int): SearchEngine =
            entries.getOrElse(ordinal) { GOOGLE }
    }
}
