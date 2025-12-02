# Requirements Document

## Introduction

This specification addresses critical security, compatibility, data integrity, performance, and maintainability issues identified in the Entertainment Browser codebase. These fixes are essential for production readiness, Play Store compliance, and ensuring a stable user experience across Android versions and device capabilities.

## Glossary

- **Scoped Storage**: Android 10+ storage access model that restricts direct file system access
- **JavaScript Interface**: Android WebView mechanism allowing JavaScript to call native code
- **DownloadRepository**: Central repository managing all download operations and tracking
- **Room Migration**: Database schema version upgrade mechanism
- **LRU Cache**: Least Recently Used cache eviction strategy
- **Monetization Tracking**: System counting user actions to trigger monetization ads
- **Ad-Block Whitelist**: List of domains exempt from ad-blocking rules
- **WebView Cache**: In-memory storage of WebView instances for tab management

## Requirements

### Requirement 1: Storage Permission Compliance

**User Story:** As an Android user on version 10 or higher, I want the app to use modern storage APIs so that it complies with platform requirements and passes Play Store review.

#### Acceptance Criteria

1. WHEN the app targets Android 10 or higher, THE Entertainment Browser SHALL NOT request WRITE_EXTERNAL_STORAGE permission
2. WHEN the app runs on Android 13 or higher, THE Entertainment Browser SHALL request READ_MEDIA_VIDEO permission for media access
3. WHEN the app runs on Android 9 or lower, THE Entertainment Browser SHALL request READ_EXTERNAL_STORAGE permission with maxSdkVersion="32"
4. WHEN downloads are initiated, THE Entertainment Browser SHALL use MediaStore API and scoped storage without requiring broad external storage access

### Requirement 2: JavaScript Interface Security

**User Story:** As a user browsing websites, I want the app to validate all JavaScript interactions so that malicious web pages cannot crash the app or trigger unintended behavior.

#### Acceptance Criteria

1. WHEN JavaScript calls onVideoDetected with a URL, THE Entertainment Browser SHALL validate the URL format, scheme (http/https only), length (max 2048 chars), and reject null/empty/"null" strings
2. WHEN JavaScript calls any interface method with malformed input, THE Entertainment Browser SHALL catch exceptions and log errors without crashing
3. WHEN JavaScript calls onVideoElementLongPress, THE Entertainment Browser SHALL apply the same validation as onVideoDetected before processing
4. WHEN a URL fails validation, THE Entertainment Browser SHALL reject the request and log the validation failure with details

### Requirement 3: Download Tracking Integration

**User Story:** As a user downloading videos, I want all downloads to appear in the downloads UI so that I can track progress, pause/resume, and view download history.

#### Acceptance Criteria

1. WHEN a download is initiated from WebView, THE Entertainment Browser SHALL use DownloadRepository instead of direct DownloadManager calls
2. WHEN a download starts, THE Entertainment Browser SHALL record the download in the Room database with URL, filename, and quality information
3. WHEN downloads are tracked centrally, THE Entertainment Browser SHALL enable pause/resume functionality for all downloads
4. WHEN users view the downloads screen, THE Entertainment Browser SHALL display all downloads initiated from any source

### Requirement 4: Database Migration Safety

**User Story:** As a user with saved favorites, tabs, and download history, I want database upgrades to preserve my data so that I don't lose my information during app updates.

#### Acceptance Criteria

1. WHEN a database schema upgrade occurs, THE Entertainment Browser SHALL use explicit migration paths without fallback to destructive migration
2. WHEN a migration path is missing, THE Entertainment Browser SHALL fail visibly with an error instead of silently deleting user data
3. WHEN developers add schema changes, THE Entertainment Browser SHALL require explicit migration implementation before deployment
4. WHEN migration errors occur, THE Entertainment Browser SHALL log detailed error information for debugging

### Requirement 5: WebView Cache Memory Management

**User Story:** As a user on a low-end device, I want the app to limit memory usage so that it doesn't crash or slow down my device when I open many tabs.

#### Acceptance Criteria

1. WHEN the WebView cache reaches MAX_CACHED_WEBVIEWS limit, THE Entertainment Browser SHALL evict the least recently used inactive WebView before creating a new one
2. WHEN a WebView is accessed, THE Entertainment Browser SHALL update its last access timestamp for LRU tracking
3. WHEN the app goes to background, THE Entertainment Browser SHALL proactively trim the cache to reduce memory pressure
4. WHEN cache eviction occurs, THE Entertainment Browser SHALL properly clean up the evicted WebView to free memory

### Requirement 6: Monetization Tracking Accuracy

**User Story:** As a user, I want monetization ads to appear at appropriate intervals so that I'm not interrupted too frequently during normal browsing.

#### Acceptance Criteria

1. WHEN a page loads automatically, THE Entertainment Browser SHALL NOT increment the monetization action counter
2. WHEN a URL updates during navigation, THE Entertainment Browser SHALL NOT increment the monetization action counter
3. WHEN video detection occurs passively, THE Entertainment Browser SHALL NOT increment the monetization action counter
4. WHEN a user manually opens a new tab, THE Entertainment Browser SHALL increment the monetization action counter once

### Requirement 7: Centralized Monetization Whitelist

**User Story:** As a developer maintaining the app, I want monetization domains defined in one place so that updates are consistent across all ad-blocking components.

#### Acceptance Criteria

1. WHEN monetization domains are defined, THE Entertainment Browser SHALL store them in MonetizationManager as the single source of truth
2. WHEN FastAdBlockEngine checks blocking rules, THE Entertainment Browser SHALL reference MonetizationManager whitelist
3. WHEN AdBlockWebViewClient evaluates navigation, THE Entertainment Browser SHALL reference MonetizationManager whitelist
4. WHEN HardcodedFilters evaluates blocking rules, THE Entertainment Browser SHALL reference MonetizationManager whitelist

### Requirement 8: Production URL Configuration

**User Story:** As a user accessing app settings, I want privacy policy and terms of service links to work so that I can review the app's legal documents.

#### Acceptance Criteria

1. WHEN the privacy policy link is displayed, THE Entertainment Browser SHALL provide a valid production URL instead of a placeholder
2. WHEN the terms of service link is displayed, THE Entertainment Browser SHALL provide a valid production URL instead of a placeholder
3. WHEN users click legal document links, THE Entertainment Browser SHALL open the correct documents
4. WHEN the app is submitted to Play Store, THE Entertainment Browser SHALL have valid privacy policy and terms URLs configured
