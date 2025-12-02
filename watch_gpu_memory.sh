#!/bin/bash

echo "========================================"
echo "GPU Memory Monitor"
echo "========================================"
echo ""
echo "Watching for:"
echo "- Mali GPU errors (BAD ALLOC)"
echo "- GPU AUX errors (Null anb)"
echo "- GpuMemoryManager activity"
echo ""
echo "Press Ctrl+C to stop"
echo "========================================"
echo ""

adb logcat -c
adb logcat | grep -E "MALI.*DEBUG|GpuMemoryManager|GPUAUX"
