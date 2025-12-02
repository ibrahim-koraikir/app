Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Monitoring Monetization Ad Loading" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "Watching for:" -ForegroundColor Yellow
Write-Host "  - MonetizationManager activity" -ForegroundColor White
Write-Host "  - Ad URL loading" -ForegroundColor White
Write-Host "  - WebView rendering" -ForegroundColor White
Write-Host "  - Page load status" -ForegroundColor White
Write-Host ""
Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Yellow
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

$adb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

# Clear logcat
& $adb logcat -c

# Start monitoring with color coding
& $adb logcat | ForEach-Object {
    if ($_ -match "MonetizationManager") {
        Write-Host $_ -ForegroundColor Green
    }
    elseif ($_ -match "WebViewViewModel.*💰") {
        Write-Host $_ -ForegroundColor Magenta
    }
    elseif ($_ -match "AdBlockMetrics.*✅") {
        Write-Host $_ -ForegroundColor Cyan
    }
    elseif ($_ -match "WebViewPool") {
        Write-Host $_ -ForegroundColor Yellow
    }
    elseif ($_ -match "CustomWebView") {
        Write-Host $_ -ForegroundColor Blue
    }
    elseif ($_ -match "chromium.*ERROR|GPUAUX|MALI.*BAD") {
        Write-Host $_ -ForegroundColor Red
    }
    elseif ($_ -match "MonetizationManager|WebViewViewModel|AdBlockMetrics|CustomWebView|WebViewPool|chromium:I") {
        Write-Host $_
    }
}
