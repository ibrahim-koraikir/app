package com.entertainmentbrowser.util.adblock

/**
 * Contains JavaScript code for anti-adblock bypass functionality.
 * 
 * This object provides scripts that:
 * - Spoof ad-related JavaScript objects (googletag, adsbygoogle, etc.)
 * - Preserve bait elements that anti-adblock scripts check
 * - Hide anti-adblock warning elements
 * - Apply site-specific bypass rules
 * 
 * All scripts are wrapped in try-catch blocks to prevent errors from
 * breaking page functionality.
 * 
 * @see AntiAdblockBypass for the coordinator class that manages injection
 */
object AntiAdblockScripts {

    /**
     * JavaScript that spoofs common ad-related objects to fool anti-adblock detection.
     * 
     * Defines stub implementations for:
     * - googletag (Google Publisher Tag)
     * - adsbygoogle (Google AdSense)
     * - __ads (generic ad object)
     * - fuckAdBlock / BlockAdBlock (popular anti-adblock libraries)
     * 
     * Each object has minimal stub methods that return safe defaults.
     * 
     * Requirements: 1.3, 3.1, 3.3, 3.4
     */
    val objectSpoofingScript: String = """
        (function() {
            try {
                // Spoof googletag (Google Publisher Tag)
                if (typeof window.googletag === 'undefined') {
                    window.googletag = {
                        cmd: [],
                        apiReady: true,
                        pubadsReady: true,
                        defineSlot: function() { return this; },
                        defineSizeMapping: function() { return this; },
                        defineOutOfPageSlot: function() { return this; },
                        enableServices: function() { return this; },
                        display: function() { return this; },
                        destroySlots: function() { return this; },
                        refresh: function() { return this; },
                        setTargeting: function() { return this; },
                        clearTargeting: function() { return this; },
                        getSlots: function() { return []; },
                        pubads: function() {
                            return {
                                enableSingleRequest: function() { return this; },
                                enableAsyncRendering: function() { return this; },
                                enableLazyLoad: function() { return this; },
                                disableInitialLoad: function() { return this; },
                                collapseEmptyDivs: function() { return this; },
                                setTargeting: function() { return this; },
                                clearTargeting: function() { return this; },
                                setCentering: function() { return this; },
                                setLocation: function() { return this; },
                                setPublisherProvidedId: function() { return this; },
                                setPrivacySettings: function() { return this; },
                                setSafeFrameConfig: function() { return this; },
                                refresh: function() { return this; },
                                clear: function() { return this; },
                                updateCorrelator: function() { return this; },
                                addEventListener: function() { return this; },
                                removeEventListener: function() { return this; },
                                getSlots: function() { return []; },
                                getSlotIdMap: function() { return {}; }
                            };
                        },
                        companionAds: function() {
                            return {
                                enableServices: function() { return this; },
                                setRefreshUnfilledSlots: function() { return this; }
                            };
                        },
                        content: function() {
                            return {
                                setContent: function() { return this; }
                            };
                        },
                        sizeMapping: function() {
                            return {
                                addSize: function() { return this; },
                                build: function() { return []; }
                            };
                        }
                    };
                }

                // Spoof adsbygoogle (Google AdSense)
                if (typeof window.adsbygoogle === 'undefined') {
                    window.adsbygoogle = {
                        loaded: true,
                        push: function() { return this; }
                    };
                }

                // Spoof __ads (generic ad object used by some sites)
                if (typeof window.__ads === 'undefined') {
                    window.__ads = {
                        loaded: true,
                        init: function() { return this; },
                        display: function() { return this; },
                        refresh: function() { return this; }
                    };
                }

                // Spoof fuckAdBlock / BlockAdBlock (popular anti-adblock detection library)
                if (typeof window.fuckAdBlock === 'undefined') {
                    window.fuckAdBlock = {
                        _options: { checkOnLoad: false, resetOnEnd: false },
                        _var: { detected: false, checking: false },
                        check: function(callback) { 
                            if (callback) callback(false);
                            return this; 
                        },
                        emitEvent: function() { return this; },
                        clearEvent: function() { return this; },
                        on: function() { return this; },
                        onDetected: function() { return this; },
                        onNotDetected: function(callback) { 
                            if (callback) callback();
                            return this; 
                        },
                        setOption: function() { return this; }
                    };
                }

                // Alias for BlockAdBlock (same library, different name)
                if (typeof window.blockAdBlock === 'undefined') {
                    window.blockAdBlock = window.fuckAdBlock;
                }

                // Spoof sniffAdBlock (another detection library)
                if (typeof window.sniffAdBlock === 'undefined') {
                    window.sniffAdBlock = {
                        check: function() { return false; }
                    };
                }

                // Spoof canRunAds flag (simple detection method)
                window.canRunAds = true;
                window.canShowAds = true;
                window.isAdBlockActive = false;

            } catch(e) {
                // Silently fail - don't break page functionality
            }
        })();
    """.trimIndent()


    /**
     * JavaScript that preserves bait elements used by anti-adblock detection.
     * 
     * Anti-adblock scripts often create hidden elements with ad-related class names
     * and check if they get removed by ad blockers. This script uses MutationObserver
     * to detect when these elements are removed and restores them.
     * 
     * Target class names: ad, ads, adsbox, ad-placeholder, ad-banner
     * 
     * Requirements: 1.4, 5.2
     */
    val baitPreservationScript: String = """
        (function() {
            try {
                // Store references to bait elements
                var baitClasses = ['ad', 'ads', 'adsbox', 'ad-placeholder', 'ad-banner', 
                                   'ad-container', 'ad-wrapper', 'advertisement', 'advert',
                                   'adsense', 'adblock-test', 'ad-test', 'pub_300x250'];
                var baitIds = ['ad', 'ads', 'adsbox', 'ad-placeholder', 'ad-banner',
                               'ad-container', 'ad-wrapper', 'advertisement'];
                var preservedElements = new Map();

                // Function to create a bait element
                function createBaitElement(className, id) {
                    var el = document.createElement('div');
                    if (className) el.className = className;
                    if (id) el.id = id;
                    el.style.cssText = 'position:absolute!important;left:-9999px!important;top:-9999px!important;width:1px!important;height:1px!important;opacity:0!important;pointer-events:none!important;';
                    el.setAttribute('data-bait-preserved', 'true');
                    return el;
                }

                // Function to ensure bait elements exist
                function ensureBaitElements() {
                    baitClasses.forEach(function(cls) {
                        var existing = document.querySelector('.' + cls + ':not([data-bait-preserved])');
                        if (!existing && !preservedElements.has('class-' + cls)) {
                            var bait = createBaitElement(cls, null);
                            document.body.appendChild(bait);
                            preservedElements.set('class-' + cls, bait);
                        }
                    });
                    baitIds.forEach(function(id) {
                        var existing = document.getElementById(id);
                        if (!existing && !preservedElements.has('id-' + id)) {
                            var bait = createBaitElement(null, id);
                            document.body.appendChild(bait);
                            preservedElements.set('id-' + id, bait);
                        }
                    });
                }

                // Set up MutationObserver to detect removed bait elements
                var observer = new MutationObserver(function(mutations) {
                    mutations.forEach(function(mutation) {
                        if (mutation.type === 'childList' && mutation.removedNodes.length > 0) {
                            mutation.removedNodes.forEach(function(node) {
                                if (node.nodeType === 1) { // Element node
                                    var className = node.className || '';
                                    var id = node.id || '';
                                    
                                    // Check if removed element was a bait element
                                    var isBait = baitClasses.some(function(cls) {
                                        return className.indexOf(cls) !== -1;
                                    }) || baitIds.some(function(bid) {
                                        return id === bid;
                                    });
                                    
                                    if (isBait && !node.hasAttribute('data-bait-preserved')) {
                                        // Restore the bait element
                                        setTimeout(function() {
                                            ensureBaitElements();
                                        }, 10);
                                    }
                                }
                            });
                        }
                    });
                });

                // Start observing when DOM is ready
                function startObserving() {
                    if (document.body) {
                        ensureBaitElements();
                        observer.observe(document.body, {
                            childList: true,
                            subtree: true
                        });
                    } else {
                        setTimeout(startObserving, 10);
                    }
                }

                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', startObserving);
                } else {
                    startObserving();
                }

            } catch(e) {
                // Silently fail - don't break page functionality
            }
        })();
    """.trimIndent()


    /**
     * List of CSS selectors targeting common anti-adblock warning elements.
     * 
     * These selectors target:
     * - Modal dialogs asking to disable ad blocker
     * - Warning banners and notices
     * - Overlay elements blocking content
     * 
     * Requirements: 1.2, 3.2
     */
    val elementHidingSelectors: List<String> = listOf(
        // Generic anti-adblock warning classes
        ".adblock-warning",
        ".adblock-modal",
        ".adblock-notice",
        ".adblock-overlay",
        ".adblock-message",
        ".adblock-detected",
        ".adblock-blocker",
        ".adblocker-warning",
        ".adblocker-modal",
        ".adblocker-notice",
        ".adblocker-overlay",
        ".adblocker-message",
        ".adblocker-detected",
        
        // Generic anti-adblock warning IDs
        "#adblock-warning",
        "#adblock-modal",
        "#adblock-notice",
        "#adblock-overlay",
        "#adblock-message",
        "#adblock-detected",
        "#adblocker-warning",
        "#adblocker-modal",
        "#adblocker-notice",
        "#adblocker-overlay",
        
        // Common naming patterns
        ".ad-blocker-warning",
        ".ad-blocker-modal",
        ".ad-blocker-notice",
        ".ad-block-warning",
        ".ad-block-modal",
        ".ad-block-notice",
        "#ad-blocker-warning",
        "#ad-blocker-modal",
        "#ad-block-warning",
        "#ad-block-modal",
        
        // Disable adblock messages
        ".disable-adblock",
        ".disable-adblocker",
        ".turn-off-adblock",
        ".please-disable-adblock",
        "#disable-adblock",
        "#disable-adblocker",
        "#turn-off-adblock",
        
        // Anti-adblock specific
        ".anti-adblock",
        ".anti-adblocker",
        ".antiAdBlock",
        ".antiAdblock",
        "#anti-adblock",
        "#anti-adblocker",
        "#antiAdBlock",
        
        // FuckAdBlock / BlockAdBlock specific
        ".fuckAdBlock",
        ".blockadblock",
        ".BlockAdBlock",
        "#fuckAdBlock",
        "#blockadblock",
        "#BlockAdBlock",
        
        // Popup/modal patterns
        ".adblock-popup",
        ".adblocker-popup",
        ".adblock-dialog",
        ".adblocker-dialog",
        "#adblock-popup",
        "#adblocker-popup",
        
        // Content blocking overlays
        ".content-blocked",
        ".content-blocker",
        ".blocked-content",
        ".blocked-overlay",
        "#content-blocked",
        "#blocked-content",
        
        // Subscription/whitelist prompts
        ".whitelist-prompt",
        ".whitelist-modal",
        ".subscribe-to-remove-ads",
        "#whitelist-prompt",
        "#whitelist-modal",
        
        // Generic warning/notice patterns that might be anti-adblock
        "[class*='adblock-']",
        "[class*='adblocker-']",
        "[id*='adblock-']",
        "[id*='adblocker-']",
        "[class*='AdBlock']",
        "[class*='AdBlocker']"
    )

    /**
     * JavaScript that hides anti-adblock warning elements using CSS.
     * Uses MutationObserver to hide dynamically added warning elements.
     * 
     * Requirements: 1.2, 3.2, 5.2
     */
    val elementHidingScript: String = """
        (function() {
            try {
                var selectors = [
                    '.adblock-warning', '.adblock-modal', '.adblock-notice', '.adblock-overlay',
                    '.adblock-message', '.adblock-detected', '.adblock-blocker',
                    '.adblocker-warning', '.adblocker-modal', '.adblocker-notice', '.adblocker-overlay',
                    '.adblocker-message', '.adblocker-detected',
                    '#adblock-warning', '#adblock-modal', '#adblock-notice', '#adblock-overlay',
                    '#adblock-message', '#adblock-detected',
                    '#adblocker-warning', '#adblocker-modal', '#adblocker-notice', '#adblocker-overlay',
                    '.ad-blocker-warning', '.ad-blocker-modal', '.ad-blocker-notice',
                    '.ad-block-warning', '.ad-block-modal', '.ad-block-notice',
                    '#ad-blocker-warning', '#ad-blocker-modal', '#ad-block-warning', '#ad-block-modal',
                    '.disable-adblock', '.disable-adblocker', '.turn-off-adblock', '.please-disable-adblock',
                    '#disable-adblock', '#disable-adblocker', '#turn-off-adblock',
                    '.anti-adblock', '.anti-adblocker', '.antiAdBlock', '.antiAdblock',
                    '#anti-adblock', '#anti-adblocker', '#antiAdBlock',
                    '.fuckAdBlock', '.blockadblock', '.BlockAdBlock',
                    '#fuckAdBlock', '#blockadblock', '#BlockAdBlock',
                    '.adblock-popup', '.adblocker-popup', '.adblock-dialog', '.adblocker-dialog',
                    '#adblock-popup', '#adblocker-popup',
                    '.content-blocked', '.content-blocker', '.blocked-content', '.blocked-overlay',
                    '#content-blocked', '#blocked-content',
                    '.whitelist-prompt', '.whitelist-modal', '.subscribe-to-remove-ads',
                    '#whitelist-prompt', '#whitelist-modal'
                ];

                // Create and inject CSS to hide elements
                function injectHidingCSS() {
                    var styleId = 'anti-adblock-bypass-styles';
                    if (document.getElementById(styleId)) return;
                    
                    var style = document.createElement('style');
                    style.id = styleId;
                    style.textContent = selectors.join(',') + 
                        '{display:none!important;visibility:hidden!important;opacity:0!important;' +
                        'pointer-events:none!important;position:absolute!important;left:-9999px!important;}';
                    
                    (document.head || document.documentElement).appendChild(style);
                }

                // Function to hide matching elements
                function hideElements() {
                    selectors.forEach(function(selector) {
                        try {
                            var elements = document.querySelectorAll(selector);
                            elements.forEach(function(el) {
                                if (!el.hasAttribute('data-adblock-hidden')) {
                                    el.style.setProperty('display', 'none', 'important');
                                    el.style.setProperty('visibility', 'hidden', 'important');
                                    el.style.setProperty('opacity', '0', 'important');
                                    el.setAttribute('data-adblock-hidden', 'true');
                                }
                            });
                        } catch(e) {}
                    });
                    
                    // Also check for elements with adblock-related text content
                    var textPatterns = ['disable your ad', 'turn off your ad', 'ad blocker detected',
                                        'adblocker detected', 'please disable', 'whitelist this site',
                                        'disable adblock', 'turn off adblock'];
                    
                    document.querySelectorAll('div, section, aside, dialog, [role="dialog"], [role="alertdialog"]').forEach(function(el) {
                        var text = (el.textContent || '').toLowerCase();
                        var isWarning = textPatterns.some(function(pattern) {
                            return text.indexOf(pattern) !== -1;
                        });
                        
                        if (isWarning && !el.hasAttribute('data-adblock-hidden')) {
                            // Check if this looks like a modal/overlay
                            var style = window.getComputedStyle(el);
                            var isModal = style.position === 'fixed' || style.position === 'absolute' ||
                                          style.zIndex > 1000 || el.classList.contains('modal') ||
                                          el.classList.contains('overlay') || el.classList.contains('popup');
                            
                            if (isModal) {
                                el.style.setProperty('display', 'none', 'important');
                                el.setAttribute('data-adblock-hidden', 'true');
                            }
                        }
                    });
                }

                // Remove body scroll lock that anti-adblock modals often apply
                function removeScrollLock() {
                    document.body.style.removeProperty('overflow');
                    document.documentElement.style.removeProperty('overflow');
                    document.body.classList.remove('modal-open', 'no-scroll', 'overflow-hidden');
                }

                // Set up MutationObserver to hide dynamically added elements
                var observer = new MutationObserver(function(mutations) {
                    var shouldCheck = false;
                    mutations.forEach(function(mutation) {
                        if (mutation.type === 'childList' && mutation.addedNodes.length > 0) {
                            shouldCheck = true;
                        }
                    });
                    if (shouldCheck) {
                        hideElements();
                        removeScrollLock();
                    }
                });

                // Start observing
                function startObserving() {
                    injectHidingCSS();
                    hideElements();
                    removeScrollLock();
                    
                    if (document.body) {
                        observer.observe(document.body, {
                            childList: true,
                            subtree: true
                        });
                    }
                }

                if (document.readyState === 'loading') {
                    document.addEventListener('DOMContentLoaded', startObserving);
                } else {
                    startObserving();
                }

                // Also run periodically for the first few seconds
                var checkCount = 0;
                var checkInterval = setInterval(function() {
                    hideElements();
                    removeScrollLock();
                    checkCount++;
                    if (checkCount >= 10) {
                        clearInterval(checkInterval);
                    }
                }, 500);

            } catch(e) {
                // Silently fail - don't break page functionality
            }
        })();
    """.trimIndent()


    /**
     * Map of site-specific bypass rules.
     * 
     * Keys are domain patterns (without protocol).
     * Values are JavaScript code specific to that site's anti-adblock detection.
     * 
     * Requirements: 2.1, 4.1
     */
    val siteSpecificRules: Map<String, String> = mapOf(
        // FMovies and variants - uses custom anti-adblock detection
        "fmovies" to """
            (function() {
                try {
                    // FMovies specific: Override their detection functions
                    window.BetterJsPop = { add: function(){}, count: function(){ return 0; } };
                    window.open = function() { return null; };
                    
                    // Block their ad check callbacks
                    if (typeof window.adcashMacros !== 'undefined') {
                        window.adcashMacros = {};
                    }
                    
                    // Hide FMovies specific warning elements
                    var style = document.createElement('style');
                    style.textContent = '.fc-ab-root, .fc-dialog-container, .fc-dialog-overlay, ' +
                        '.fbox, #fbox, .adblock-msg, #adblock-msg, .adb-overlay, #adb-overlay ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                    
                    // Remove body class that blocks scrolling
                    document.body.classList.remove('fc-ab-root-visible');
                } catch(e) {}
            })();
        """.trimIndent(),

        // 123movies variants
        "123movies" to """
            (function() {
                try {
                    // 123movies uses popunder scripts
                    window.PopAds = { add: function(){} };
                    window.popns = { add: function(){} };
                    window.ExoLoader = { serve: function(){} };
                    
                    // Block popup windows
                    var origOpen = window.open;
                    window.open = function(url) {
                        if (url && (url.indexOf('ad') !== -1 || url.indexOf('pop') !== -1)) {
                            return null;
                        }
                        return origOpen.apply(this, arguments);
                    };
                    
                    // Hide their specific warning elements
                    var style = document.createElement('style');
                    style.textContent = '.adblock-notify, #adblock-notify, .adb-wrap, #adb-wrap ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // Putlocker variants
        "putlocker" to """
            (function() {
                try {
                    // Putlocker specific overrides
                    window.adBlockDetected = false;
                    window.adBlockEnabled = false;
                    
                    // Override their check function
                    if (typeof window.checkAdBlock === 'function') {
                        window.checkAdBlock = function() { return false; };
                    }
                    
                    // Hide warning elements
                    var style = document.createElement('style');
                    style.textContent = '.adblock-warning, #adblock-warning, .adb-message ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // GoMovies
        "gomovies" to """
            (function() {
                try {
                    window.adBlockDetected = false;
                    window.BetterJsPop = { add: function(){}, count: function(){ return 0; } };
                    
                    // Hide their specific elements
                    var style = document.createElement('style');
                    style.textContent = '.fc-ab-root, .adb-overlay, #adb-overlay ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // SolarMovie
        "solarmovie" to """
            (function() {
                try {
                    window.adBlockDetected = false;
                    window.canRunAds = true;
                    
                    // Override detection
                    Object.defineProperty(window, 'adBlockDetected', {
                        get: function() { return false; },
                        set: function() {}
                    });
                    
                    var style = document.createElement('style');
                    style.textContent = '.adblock-msg, #adblock-msg, .adb-wrap ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // YesMovies
        "yesmovies" to """
            (function() {
                try {
                    window.adBlockDetected = false;
                    window.BetterJsPop = { add: function(){}, count: function(){ return 0; } };
                    
                    var style = document.createElement('style');
                    style.textContent = '.fc-ab-root, .adb-overlay ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // Soap2Day
        "soap2day" to """
            (function() {
                try {
                    window.adBlockDetected = false;
                    window.open = function() { return null; };
                    
                    // Block their popup scripts
                    window.PopAds = { add: function(){} };
                    
                    var style = document.createElement('style');
                    style.textContent = '.adblock-warning, .popup-overlay, #popup-overlay ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // HDToday
        "hdtoday" to """
            (function() {
                try {
                    window.adBlockDetected = false;
                    window.canRunAds = true;
                    
                    var style = document.createElement('style');
                    style.textContent = '.adblock-msg, .adb-overlay ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // FlixHQ
        "flixhq" to """
            (function() {
                try {
                    window.adBlockDetected = false;
                    window.BetterJsPop = { add: function(){}, count: function(){ return 0; } };
                    
                    var style = document.createElement('style');
                    style.textContent = '.fc-ab-root, .adb-overlay, .adblock-notify ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // LookMovie
        "lookmovie" to """
            (function() {
                try {
                    window.adBlockDetected = false;
                    window.canRunAds = true;
                    
                    // Override their specific detection
                    if (typeof window.detectAdBlock === 'function') {
                        window.detectAdBlock = function() { return false; };
                    }
                    
                    var style = document.createElement('style');
                    style.textContent = '.adblock-warning, .adb-message ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // Forbes (news site with aggressive anti-adblock)
        "forbes" to """
            (function() {
                try {
                    // Forbes uses a paywall/adblock detection combo
                    window.canRunAds = true;
                    window.adBlockDetected = false;
                    
                    // Override their detection
                    Object.defineProperty(window, 'fbs_settings', {
                        get: function() { return { adblock: false }; },
                        set: function() {}
                    });
                    
                    var style = document.createElement('style');
                    style.textContent = '.ad-blocker-warning, .fbs-ad-wrapper, #ad-blocker-warning ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                    
                    // Remove scroll lock
                    document.body.style.overflow = 'auto';
                } catch(e) {}
            })();
        """.trimIndent(),

        // Wired (news site with anti-adblock)
        "wired" to """
            (function() {
                try {
                    window.canRunAds = true;
                    window.adBlockDetected = false;
                    
                    var style = document.createElement('style');
                    style.textContent = '.ad-blocker-warning, .paywall-bar, #ad-blocker-warning ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                    
                    document.body.style.overflow = 'auto';
                } catch(e) {}
            })();
        """.trimIndent(),

        // Generic streaming site pattern
        "streamwish" to """
            (function() {
                try {
                    window.adBlockDetected = false;
                    window.open = function() { return null; };
                    
                    var style = document.createElement('style');
                    style.textContent = '.adblock-warning, .adb-overlay ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // VidSrc
        "vidsrc" to """
            (function() {
                try {
                    window.adBlockDetected = false;
                    window.canRunAds = true;
                    
                    var style = document.createElement('style');
                    style.textContent = '.adblock-msg, .adb-overlay ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent(),

        // 2embed
        "2embed" to """
            (function() {
                try {
                    window.adBlockDetected = false;
                    window.BetterJsPop = { add: function(){}, count: function(){ return 0; } };
                    
                    var style = document.createElement('style');
                    style.textContent = '.fc-ab-root, .adb-overlay ' +
                        '{display:none!important;}';
                    document.head.appendChild(style);
                } catch(e) {}
            })();
        """.trimIndent()
    )

    /**
     * Get site-specific rules for a domain.
     * Matches partial domain names (e.g., "fmovies" matches "fmovies.to", "fmovies.wtf", etc.)
     * 
     * @param domain The domain to check
     * @return The site-specific script or null if no match
     */
    fun getSiteSpecificScript(domain: String): String? {
        val lowerDomain = domain.lowercase()
        return siteSpecificRules.entries.firstOrNull { (pattern, _) ->
            lowerDomain.contains(pattern)
        }?.value
    }


    /**
     * Combined generic bypass script that includes all bypass techniques.
     * 
     * Order of execution:
     * 1. Object spoofing (runs first to define fake ad objects)
     * 2. Bait preservation (maintains bait elements)
     * 3. Element hiding (hides warning elements)
     * 
     * The entire script is wrapped in an IIFE with try-catch for safety.
     * 
     * Requirements: 2.2, 2.3
     */
    val genericBypassScript: String by lazy {
        """
        (function() {
            try {
                // === PHASE 1: Object Spoofing ===
                $objectSpoofingScript
                
                // === PHASE 2: Bait Element Preservation ===
                $baitPreservationScript
                
                // === PHASE 3: Element Hiding ===
                $elementHidingScript
                
            } catch(e) {
                // Silently fail - don't break page functionality
                console.log('Anti-adblock bypass error:', e);
            }
        })();
        """.trimIndent()
    }

    /**
     * Early injection script containing only object spoofing and bait preservation.
     * This should be injected at onPageStarted before page scripts run.
     */
    val earlyBypassScript: String by lazy {
        """
        (function() {
            try {
                // === Object Spoofing ===
                $objectSpoofingScript
                
                // === Bait Element Preservation ===
                $baitPreservationScript
                
            } catch(e) {
                // Silently fail
            }
        })();
        """.trimIndent()
    }

    /**
     * Late injection script containing element hiding.
     * This should be injected at onPageFinished after page loads.
     */
    val lateBypassScript: String = elementHidingScript

    /**
     * Get the complete bypass script for a domain, including site-specific rules if available.
     * 
     * @param domain The domain to get bypass script for
     * @return Combined bypass script (generic + site-specific if available)
     */
    fun getCompleteBypassScript(domain: String): String {
        val siteSpecific = getSiteSpecificScript(domain)
        return if (siteSpecific != null) {
            """
            (function() {
                try {
                    // === Generic Bypass ===
                    $genericBypassScript
                    
                    // === Site-Specific Bypass ===
                    $siteSpecific
                    
                } catch(e) {
                    // Silently fail
                }
            })();
            """.trimIndent()
        } else {
            genericBypassScript
        }
    }
}
