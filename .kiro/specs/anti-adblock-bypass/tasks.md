 n# Implementation Plan

- [x] 1. Create AntiAdblockScripts object with bypass JavaScript




  - [x] 1.1 Create AntiAdblockScripts.kt with object spoofing script

    - Create `app/src/main/java/com/entertainmentbrowser/util/adblock/AntiAdblockScripts.kt`
    - Implement `objectSpoofingScript` that defines googletag, adsbygoogle, __ads, fuckAdBlock objects
    - Each spoofed object should have stub methods that return safe defaults
    - Wrap all code in try-catch for error handling
    - _Requirements: 1.3, 3.1, 3.3, 3.4_

  - [ ] 1.2 Write property test for spoofed objects completeness





    - **Property 6: Spoofed Objects Completeness**
    - **Validates: Requirements 1.3, 3.1**
    - Test that objectSpoofingScript contains definitions for all required ad objects


  - [x] 1.3 Add bait element preservation script
    - Implement `baitPreservationScript` that prevents removal of ad-related elements
    - Use MutationObserver to detect and restore removed bait elements
    - Target common bait class names: ad, ads, adsbox, ad-placeholder, ad-banner
    - _Requirements: 1.4, 5.2_

  - [x] 1.4 Add element hiding CSS selectors and script
    - Implement `elementHidingSelectors` list with common anti-adblock warning selectors
    - Include selectors for: .adblock-warning, .adblock-modal, #adblock-notice, etc.
    - Implement `elementHidingScript` that applies CSS to hide matched elements
    - Use MutationObserver to hide dynamically added warning elements
    - _Requirements: 1.2, 3.2, 5.2_

  - [ ]* 1.5 Write property test for element hiding selector validity
    - **Property 5: Element Hiding Selector Validity**
    - **Validates: Requirements 1.2, 3.2**
    - Test that all CSS selectors in elementHidingSelectors are valid CSS syntax


  - [x] 1.6 Add site-specific bypass rules
    - Implement `siteSpecificRules` map with domain patterns as keys
    - Add rules for common streaming sites: fmovies, 123movies, putlocker, etc.
    - Each rule should target site-specific anti-adblock detection methods
    - _Requirements: 2.1, 4.1_

  - [x] 1.7 Combine scripts into genericBypassScript

    - Implement `genericBypassScript` that combines all bypass techniques
    - Order: object spoofing → bait preservation → element hiding
    - Ensure entire script is wrapped in IIFE with try-catch
    - _Requirements: 2.2, 2.3_

  - [ ]* 1.8 Write property test for try-catch wrapping
    - **Property 7: JavaScript Try-Catch Wrapping**
    - **Validates: Requirements 3.4**
    - Test that genericBypassScript contains try-catch wrapper

- [x] 2. Create AntiAdblockBypass coordinator class





  - [x] 2.1 Create AntiAdblockBypass.kt with basic structure


    - Create `app/src/main/java/com/entertainmentbrowser/util/adblock/AntiAdblockBypass.kt`
    - Add @Singleton annotation and @Inject constructor
    - Inject ApplicationContext for DataStore access
    - Add private exclusionList set for excluded domains
    - _Requirements: 6.1, 6.2, 6.3_


  - [x] 2.2 Implement shouldApplyBypass method
    - Extract domain from URL parameter
    - Return false if domain is in exclusion list
    - Return true for all other valid URLs
    - Handle null/invalid URLs gracefully (return false)
    - _Requirements: 6.3_


  - [x] 2.3 Implement getBypassScript method
    - Check for site-specific rules first using getSiteSpecificRules
    - If site-specific rule exists, combine with generic script
    - If no site-specific rule, return generic script only
    - Return empty string if shouldApplyBypass returns false
    - _Requirements: 1.1, 2.1, 2.2_

  - [ ]* 2.4 Write property test for bypass script injection completeness
    - **Property 1: Bypass Script Injection Completeness**
    - **Validates: Requirements 1.1, 1.3**
    - Test that for any URL where shouldApplyBypass returns true, getBypassScript returns non-empty script

  - [ ]* 2.5 Write property test for site-specific rule inclusion
    - **Property 3: Site-Specific Rule Inclusion**
    - **Validates: Requirements 2.1**
    - Test that domains in site-specific rules get both generic and site-specific scripts

  - [ ]* 2.6 Write property test for generic bypass fallback
    - **Property 4: Generic Bypass Fallback**
    - **Validates: Requirements 2.2**
    - Test that domains NOT in site-specific rules still get generic bypass script


  - [x] 2.7 Implement exclusion list methods
    - Implement addToExclusionList(domain: String)
    - Implement removeFromExclusionList(domain: String)
    - Implement isExcluded(domain: String): Boolean
    - Store exclusion list in memory (DataStore persistence optional)
    - _Requirements: 6.1, 6.2, 6.3_

  - [ ]* 2.8 Write property test for exclusion list round-trip
    - **Property 2: Exclusion List Round-Trip Consistency**
    - **Validates: Requirements 6.1, 6.2, 6.3**
    - Test add → isExcluded returns true → remove → isExcluded returns false

- [x] 3. Integrate AntiAdblockBypass with AdBlockWebViewClient







  - [-] 3.1 Add AntiAdblockBypass dependency to AdBlockWebViewClient

    - Add antiAdblockBypass parameter to AdBlockWebViewClient constructor
    - Update all instantiation sites to provide AntiAdblockBypass instance
    - _Requirements: 1.1_



  - [ ] 3.2 Inject bypass script in onPageStarted
    - Override onPageStarted if not already overridden
    - Check shouldApplyBypass for current URL
    - Call view.evaluateJavascript with early bypass script (object spoofing + bait preservation)
    - Log injection for debugging

    - _Requirements: 1.1, 2.4_

  - [ ] 3.3 Inject element hiding script in onPageFinished
    - In existing onPageFinished override, add element hiding injection
    - Check shouldApplyBypass for current URL
    - Call view.evaluateJavascript with element hiding script
    - _Requirements: 1.2_

- [x] 4. Update dependency injection






  - [x] 4.1 Add AntiAdblockBypass to AppModule


    - Open `app/src/main/java/com/entertainmentbrowser/di/AppModule.kt`
    - Add @Provides method for AntiAdblockBypass if needed
    - Ensure singleton scope
    - _Requirements: 1.1_

  - [x] 4.2 Update WebViewScreen to inject AntiAdblockBypass


    - Update WebViewScreen or WebViewViewModel to receive AntiAdblockBypass
    - Pass to AdBlockWebViewClient constructor
    - _Requirements: 1.1_

- [x] 5. Checkpoint - Ensure all tests pass





  - Ensure all tests pass, ask the user if questions arise.

- [ ] 6. Manual testing and verification
  - [ ] 6.1 Test on sites with anti-adblock detection
    - Test on fmovies.to and verify no "ad blocker detected" warning
    - Test on 123movies and verify video playback works
    - Test on sites with aggressive anti-adblock (forbes.com, wired.com)
    - Verify bypass doesn't break legitimate site functionality
    - _Requirements: 1.5, 3.3_

  - [ ] 6.2 Test exclusion list functionality
    - Add a site to exclusion list and verify bypass is not applied
    - Remove site from exclusion list and verify bypass resumes
    - _Requirements: 6.1, 6.2_

- [ ] 7. Final Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.
