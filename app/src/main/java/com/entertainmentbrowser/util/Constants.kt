package com.entertainmentbrowser.util

import com.entertainmentbrowser.core.constants.Constants as CoreConstants

object Constants {
    // Adsterra URL built at runtime from BuildConfig - no hardcoded key
    val ADSTERRA_DIRECT_LINK: String
        get() = CoreConstants.ADSTERRA_DIRECT_LINK
    
    // Re-export ad networks from core (built at runtime)
    val AD_NETWORKS: List<CoreConstants.AdNetwork>
        get() = CoreConstants.AD_NETWORKS
}