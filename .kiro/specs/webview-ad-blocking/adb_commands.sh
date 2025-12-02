#!/bin/bash
# Ad-Blocking Testing - ADB Commands Helper Script
# This script provides convenient commands for testing ad-blocking functionality

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

PACKAGE="com.entertainmentbrowser"

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}Ad-Blocking Testing - ADB Helper${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Function to check if device is connected
check_device() {
    if ! adb devices | grep -q "device$"; then
        echo -e "${RED}❌ No device connected${NC}"
        echo "Please connect a device or start an emulator"
        exit 1
    fi
    echo -e "${GREEN}✅ Device connected${NC}"
}

# Function to display menu
show_menu() {
    echo ""
    echo -e "${YELLOW}Select an option:${NC}"
    echo "1. Install debug APK"
    echo "2. Clear app data"
    echo "3. Monitor ad-blocking logs (real-time)"
    echo "4. Monitor performance metrics"
    echo "5. Check memory usage"
    echo "6. Measure filter load time"
    echo "7. Enable GPU profiling"
    echo "8. Disable GPU profiling"
    echo "9. Capture full logs to file"
    echo "10. Take screenshot"
    echo "11. Record screen"
    echo "12. Run all performance tests"
    echo "13. Generate test report data"
    echo "0. Exit"
    echo ""
    read -p "Enter choice: " choice
}

# 1. Install debug APK
install_apk() {
    echo -e "${BLUE}📦 Installing debug APK...${NC}"
    cd ../../../ || exit
    ./gradlew installDebug
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ APK installed successfully${NC}"
    else
        echo -e "${RED}❌ Failed to install APK${NC}"
    fi
}

# 2. Clear app data
clear_data() {
    echo -e "${BLUE}🧹 Clearing app data...${NC}"
    adb shell pm clear $PACKAGE
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ App data cleared${NC}"
    else
        echo -e "${RED}❌ Failed to clear app data${NC}"
    fi
}

# 3. Monitor ad-blocking logs
monitor_logs() {
    echo -e "${BLUE}📊 Monitoring ad-blocking logs (Ctrl+C to stop)...${NC}"
    echo -e "${YELLOW}Watching: FastAdBlockEngine, AdBlockWebViewClient, AdBlockMetrics${NC}"
    echo ""
    adb logcat -c  # Clear existing logs
    adb logcat -s FastAdBlockEngine:D AdBlockWebViewClient:D AdBlockMetrics:D
}

# 4. Monitor performance metrics
monitor_performance() {
    echo -e "${BLUE}⚡ Monitoring performance metrics (Ctrl+C to stop)...${NC}"
    echo ""
    
    # Start monitoring in background
    adb logcat -c
    
    echo -e "${YELLOW}Press Enter after navigating to test pages...${NC}"
    read
    
    echo -e "${GREEN}Collecting metrics...${NC}"
    
    # Get filter load time
    echo -e "\n${BLUE}Filter Load Time:${NC}"
    adb logcat -d | grep "Loaded in" | tail -1
    
    # Get blocked counts
    echo -e "\n${BLUE}Blocked Requests:${NC}"
    adb logcat -d | grep "Page finished" | tail -5
    
    # Get memory usage
    echo -e "\n${BLUE}Memory Usage:${NC}"
    adb shell dumpsys meminfo $PACKAGE | grep "TOTAL PSS"
}

# 5. Check memory usage
check_memory() {
    echo -e "${BLUE}💾 Checking memory usage...${NC}"
    echo ""
    
    echo -e "${YELLOW}Current memory usage:${NC}"
    adb shell dumpsys meminfo $PACKAGE | grep -A 20 "App Summary"
    
    echo ""
    echo -e "${YELLOW}Total PSS:${NC}"
    adb shell dumpsys meminfo $PACKAGE | grep "TOTAL PSS"
    
    echo ""
    echo -e "${BLUE}Continuous monitoring? (y/n)${NC}"
    read -p "> " cont
    
    if [ "$cont" = "y" ]; then
        echo -e "${YELLOW}Monitoring every 5 seconds (Ctrl+C to stop)...${NC}"
        while true; do
            clear
            echo -e "${BLUE}Memory Usage - $(date)${NC}"
            adb shell dumpsys meminfo $PACKAGE | grep "TOTAL PSS"
            sleep 5
        done
    fi
}

# 6. Measure filter load time
measure_load_time() {
    echo -e "${BLUE}⏱️  Measuring filter load time...${NC}"
    echo ""
    
    # Clear app data for cold start
    echo "Clearing app data for cold start..."
    adb shell pm clear $PACKAGE
    
    # Clear logcat
    adb logcat -c
    
    echo -e "${YELLOW}Please launch the app now...${NC}"
    echo "Waiting for filter load..."
    
    # Wait for load message
    timeout 30 adb logcat -s FastAdBlockEngine:D | grep -m 1 "Loaded in"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Filter load completed${NC}"
        echo ""
        echo "Full statistics:"
        adb logcat -d -s FastAdBlockEngine:D | grep -A 3 "Loaded in"
    else
        echo -e "${RED}❌ Timeout waiting for filter load${NC}"
    fi
}

# 7. Enable GPU profiling
enable_gpu_profiling() {
    echo -e "${BLUE}🎨 Enabling GPU profiling...${NC}"
    adb shell setprop debug.hwui.profile visual_bars
    echo -e "${GREEN}✅ GPU profiling enabled${NC}"
    echo "You should now see colored bars on screen showing frame rendering time"
    echo "Green bars below red line = 60fps (good)"
    echo "Bars above red line = frame drops (bad)"
}

# 8. Disable GPU profiling
disable_gpu_profiling() {
    echo -e "${BLUE}🎨 Disabling GPU profiling...${NC}"
    adb shell setprop debug.hwui.profile false
    echo -e "${GREEN}✅ GPU profiling disabled${NC}"
}

# 9. Capture full logs
capture_logs() {
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    FILENAME="adblock_logs_${TIMESTAMP}.txt"
    
    echo -e "${BLUE}📝 Capturing logs to ${FILENAME}...${NC}"
    adb logcat -d > "$FILENAME"
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Logs saved to ${FILENAME}${NC}"
        echo "File size: $(wc -l < "$FILENAME") lines"
    else
        echo -e "${RED}❌ Failed to capture logs${NC}"
    fi
}

# 10. Take screenshot
take_screenshot() {
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    FILENAME="screenshot_${TIMESTAMP}.png"
    
    echo -e "${BLUE}📸 Taking screenshot...${NC}"
    adb shell screencap -p /sdcard/screenshot.png
    adb pull /sdcard/screenshot.png "$FILENAME"
    adb shell rm /sdcard/screenshot.png
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Screenshot saved to ${FILENAME}${NC}"
    else
        echo -e "${RED}❌ Failed to take screenshot${NC}"
    fi
}

# 11. Record screen
record_screen() {
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    FILENAME="recording_${TIMESTAMP}.mp4"
    
    echo -e "${BLUE}🎥 Recording screen...${NC}"
    echo -e "${YELLOW}Press Ctrl+C to stop recording${NC}"
    
    adb shell screenrecord /sdcard/recording.mp4
    
    echo -e "${BLUE}Pulling recording...${NC}"
    adb pull /sdcard/recording.mp4 "$FILENAME"
    adb shell rm /sdcard/recording.mp4
    
    if [ $? -eq 0 ]; then
        echo -e "${GREEN}✅ Recording saved to ${FILENAME}${NC}"
    else
        echo -e "${RED}❌ Failed to save recording${NC}"
    fi
}

# 12. Run all performance tests
run_all_tests() {
    echo -e "${BLUE}🧪 Running all performance tests...${NC}"
    echo ""
    
    # Test 1: Filter load time
    echo -e "${YELLOW}Test 1/4: Filter Load Time${NC}"
    measure_load_time
    sleep 2
    
    # Test 2: Memory usage
    echo ""
    echo -e "${YELLOW}Test 2/4: Memory Usage${NC}"
    echo "Navigate to 5 different websites, then press Enter..."
    read
    adb shell dumpsys meminfo $PACKAGE | grep "TOTAL PSS"
    sleep 2
    
    # Test 3: Blocked requests
    echo ""
    echo -e "${YELLOW}Test 3/4: Blocked Requests${NC}"
    adb logcat -d | grep "Page finished" | tail -5
    sleep 2
    
    # Test 4: Performance summary
    echo ""
    echo -e "${YELLOW}Test 4/4: Performance Summary${NC}"
    adb logcat -d -s FastAdBlockEngine:D | grep -A 3 "Loaded in"
    
    echo ""
    echo -e "${GREEN}✅ All tests completed${NC}"
}

# 13. Generate test report data
generate_report_data() {
    TIMESTAMP=$(date +%Y%m%d_%H%M%S)
    REPORT_FILE="test_data_${TIMESTAMP}.txt"
    
    echo -e "${BLUE}📊 Generating test report data...${NC}"
    
    {
        echo "========================================="
        echo "Ad-Blocking Test Data"
        echo "Generated: $(date)"
        echo "========================================="
        echo ""
        
        echo "FILTER LOAD TIME:"
        adb logcat -d -s FastAdBlockEngine:D | grep "Loaded in" | tail -1
        echo ""
        
        echo "FILTER STATISTICS:"
        adb logcat -d -s FastAdBlockEngine:D | grep "Blocked domains" -A 2 | tail -3
        echo ""
        
        echo "BLOCKED REQUESTS (Last 10 pages):"
        adb logcat -d -s AdBlockWebViewClient:D | grep "Page finished" | tail -10
        echo ""
        
        echo "MEMORY USAGE:"
        adb shell dumpsys meminfo $PACKAGE | grep "TOTAL PSS"
        echo ""
        
        echo "SAMPLE BLOCKED URLS (Last 20):"
        adb logcat -d | grep "Blocked by" | tail -20
        echo ""
        
    } > "$REPORT_FILE"
    
    echo -e "${GREEN}✅ Test data saved to ${REPORT_FILE}${NC}"
    echo "Use this data to fill out the test report template"
}

# Main loop
check_device

while true; do
    show_menu
    
    case $choice in
        1) install_apk ;;
        2) clear_data ;;
        3) monitor_logs ;;
        4) monitor_performance ;;
        5) check_memory ;;
        6) measure_load_time ;;
        7) enable_gpu_profiling ;;
        8) disable_gpu_profiling ;;
        9) capture_logs ;;
        10) take_screenshot ;;
        11) record_screen ;;
        12) run_all_tests ;;
        13) generate_report_data ;;
        0) 
            echo -e "${GREEN}Goodbye!${NC}"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid option${NC}"
            ;;
    esac
    
    echo ""
    read -p "Press Enter to continue..."
done
