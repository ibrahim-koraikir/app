# Tab Management Integration Test Plan

## Test Objective
Verify that Task 11 (Tab Management Implementation) is working correctly according to requirements 9 and 10.

## Prerequisites
- App installed on Android device or emulator (API 24+)
- At least one website available in the home screen

## Test Cases

### Test Case 1: Create New Tab
**Requirement**: 9.1 - Tab creation with unique ID

**Steps**:
1. Launch the app
2. Navigate to Home screen
3. Tap on any website card (e.g., Netflix)
4. Verify WebView opens with the website
5. Navigate to Tabs screen from bottom navigation

**Expected Result**:
- A new tab is created with a unique ID
- Tab appears in the Tabs screen
- Tab shows thumbnail or placeholder
- Tab is marked as active (red border)

---

### Test Case 2: Tab Limit Enforcement
**Requirement**: 9.2 - Maximum 20 tabs, 9.3 - Automatic oldest tab closure

**Steps**:
1. Create 20 tabs by opening different websites
2. Navigate to Tabs screen and verify 20 tabs exist
3. Open one more website (21st tab)
4. Navigate to Tabs screen

**Expected Result**:
- Only 20 tabs are displayed
- The oldest inactive tab was automatically closed
- The newest tab is active

---

### Test Case 3: Switch Between Tabs
**Requirement**: 9.5 - Tab switching

**Steps**:
1. Create 3 tabs by opening different websites
2. Navigate to Tabs screen
3. Tap on a different tab thumbnail
4. Verify the WebView loads the selected tab's URL

**Expected Result**:
- Selected tab becomes active (red border)
- Previous active tab becomes inactive
- WebView displays the correct website

---

### Test Case 4: Close Tab
**Requirement**: 10.5 - Immediate tab removal

**Steps**:
1. Create 2 tabs
2. Navigate to Tabs screen
3. Tap the close button (red X) on the active tab
4. Verify tab is removed

**Expected Result**:
- Tab is immediately removed from the list
- Tab count decreases by 1
- If other tabs exist, one becomes active

---

### Test Case 5: Tab Thumbnails
**Requirement**: 9.4 - Thumbnail previews

**Steps**:
1. Open a website with visible content
2. Wait for page to load completely
3. Navigate to Tabs screen
4. Observe the tab thumbnail

**Expected Result**:
- Tab displays a thumbnail preview of the website
- Thumbnail is circular (40x40dp)
- Active tab has red border
- If thumbnail fails, shows first letter of title

---

### Test Case 6: Empty Tabs State
**Requirement**: UI handling for no tabs

**Steps**:
1. Close all tabs
2. Navigate to Tabs screen

**Expected Result**:
- Empty state message: "No open tabs"
- Subtitle: "Open a website to start browsing"
- "Go to Home" button is displayed
- Tapping button navigates to Home screen

---

### Test Case 7: Tab Persistence
**Requirement**: 10.1-10.3 - Tab persistence across app restarts

**Steps**:
1. Create 3 tabs with different websites
2. Close the app (swipe away from recent apps)
3. Reopen the app
4. Navigate to Tabs screen

**Expected Result**:
- All 3 tabs are restored
- Tab URLs, titles, and thumbnails are preserved
- Active tab state is maintained

---

### Test Case 8: Tab Cleanup Worker
**Requirement**: 10.4 - Delete tabs older than 7 days

**Steps**:
1. Create a tab
2. Manually modify the tab's timestamp in the database to be 8 days old
3. Trigger the TabCleanupWorker (or wait for scheduled execution)
4. Check Tabs screen

**Expected Result**:
- Tabs older than 7 days are deleted
- Recent tabs remain intact
- Associated thumbnails are also deleted

---

### Test Case 9: Tab Navigation
**Requirement**: Navigation integration

**Steps**:
1. From Home screen, open a website
2. Navigate to Tabs screen using bottom navigation
3. Tap Home button in Tabs screen
4. Verify navigation to Home screen

**Expected Result**:
- Bottom navigation shows Tabs as active
- Home button navigates back to Home screen
- Navigation stack is properly managed

---

### Test Case 10: Error Handling
**Requirement**: 9.6 - Tab restoration failure handling

**Steps**:
1. Create a tab with a valid URL
2. Manually corrupt the tab data in database
3. Restart the app
4. Navigate to Tabs screen

**Expected Result**:
- Error state is displayed with message
- App doesn't crash
- User can navigate away from error state

---

## Performance Tests

### Test Case 11: Tab Creation Performance
**Steps**:
1. Measure time to create a new tab
2. Verify thumbnail capture doesn't block UI

**Expected Result**:
- Tab creation completes in < 500ms
- UI remains responsive during thumbnail capture

---

### Test Case 12: Tab Switching Performance
**Steps**:
1. Create 20 tabs
2. Measure time to switch between tabs

**Expected Result**:
- Tab switching completes in < 300ms
- No visible lag in UI

---

## Accessibility Tests

### Test Case 13: TalkBack Support
**Steps**:
1. Enable TalkBack
2. Navigate to Tabs screen
3. Use swipe gestures to navigate tabs

**Expected Result**:
- Tab thumbnails have content descriptions
- Close button is announced
- Home button is announced
- All interactive elements are accessible

---

## Test Summary

| Test Case | Status | Notes |
|-----------|--------|-------|
| 1. Create New Tab | ⏳ Pending | |
| 2. Tab Limit Enforcement | ⏳ Pending | |
| 3. Switch Between Tabs | ⏳ Pending | |
| 4. Close Tab | ⏳ Pending | |
| 5. Tab Thumbnails | ⏳ Pending | |
| 6. Empty Tabs State | ⏳ Pending | |
| 7. Tab Persistence | ⏳ Pending | |
| 8. Tab Cleanup Worker | ⏳ Pending | |
| 9. Tab Navigation | ⏳ Pending | |
| 10. Error Handling | ⏳ Pending | |
| 11. Tab Creation Performance | ⏳ Pending | |
| 12. Tab Switching Performance | ⏳ Pending | |
| 13. TalkBack Support | ⏳ Pending | |

---

## Notes
- Update status to ✅ Pass, ❌ Fail, or ⚠️ Partial as tests are executed
- Document any issues or bugs found during testing
- Verify all requirements from requirements.md are covered
