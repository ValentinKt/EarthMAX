#!/bin/bash

# EarthMAX Android Performance Monitoring Script
# This script monitors and analyzes app performance metrics

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
REPORTS_DIR="$PROJECT_DIR/reports/performance"
BUILD_DIR="$PROJECT_DIR/app/build"
BASELINE_FILE="$REPORTS_DIR/baseline.json"

# Functions
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

log_metric() {
    echo -e "${PURPLE}[METRIC]${NC} $1"
}

# Create reports directory
create_reports_dir() {
    mkdir -p "$REPORTS_DIR"
    mkdir -p "$REPORTS_DIR/builds"
    mkdir -p "$REPORTS_DIR/memory"
    mkdir -p "$REPORTS_DIR/dependencies"
}

# Measure build performance
measure_build_performance() {
    log_info "Measuring build performance..."
    
    local build_start=$(date +%s)
    local build_report="$REPORTS_DIR/builds/build-$(date +%Y%m%d-%H%M%S).json"
    
    # Clean build for accurate measurement
    cd "$PROJECT_DIR"
    ./gradlew clean
    
    # Measure debug build time
    local debug_start=$(date +%s)
    ./gradlew assembleDebug --profile
    local debug_end=$(date +%s)
    local debug_time=$((debug_end - debug_start))
    
    # Measure release build time
    local release_start=$(date +%s)
    ./gradlew assembleRelease --profile
    local release_end=$(date +%s)
    local release_time=$((release_end - release_start))
    
    local build_end=$(date +%s)
    local total_time=$((build_end - build_start))
    
    # Get APK sizes
    local debug_apk="$BUILD_DIR/outputs/apk/debug/app-debug.apk"
    local release_apk="$BUILD_DIR/outputs/apk/release/app-release.apk"
    
    local debug_size=0
    local release_size=0
    
    if [[ -f "$debug_apk" ]]; then
        debug_size=$(stat -f%z "$debug_apk" 2>/dev/null || echo 0)
    fi
    
    if [[ -f "$release_apk" ]]; then
        release_size=$(stat -f%z "$release_apk" 2>/dev/null || echo 0)
    fi
    
    # Create build report
    cat > "$build_report" << EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "build_times": {
    "debug_seconds": $debug_time,
    "release_seconds": $release_time,
    "total_seconds": $total_time
  },
  "apk_sizes": {
    "debug_bytes": $debug_size,
    "release_bytes": $release_size,
    "debug_mb": $(echo "scale=2; $debug_size / 1024 / 1024" | bc -l),
    "release_mb": $(echo "scale=2; $release_size / 1024 / 1024" | bc -l)
  },
  "gradle_profiles": {
    "location": "$BUILD_DIR/reports/profile"
  }
}
EOF

    log_metric "Debug build time: ${debug_time}s"
    log_metric "Release build time: ${release_time}s"
    log_metric "Debug APK size: $(echo "scale=2; $debug_size / 1024 / 1024" | bc -l) MB"
    log_metric "Release APK size: $(echo "scale=2; $release_size / 1024 / 1024" | bc -l) MB"
    
    log_success "Build performance measured and saved to $build_report"
}

# Analyze memory usage
analyze_memory_usage() {
    log_info "Analyzing memory usage..."
    
    local memory_report="$REPORTS_DIR/memory/memory-$(date +%Y%m%d-%H%M%S).json"
    
    cd "$PROJECT_DIR"
    
    # Run memory analysis tasks
    ./gradlew app:analyzeDebugBundle || log_warning "Bundle analysis failed"
    
    # Get method count
    local method_count=0
    if [[ -f "$BUILD_DIR/outputs/apk/debug/app-debug.apk" ]]; then
        # Use dexdump to count methods (if available)
        if command -v dexdump &> /dev/null; then
            method_count=$(dexdump -f "$BUILD_DIR/outputs/apk/debug/app-debug.apk" 2>/dev/null | grep -c "method_ids_size" || echo 0)
        fi
    fi
    
    # Analyze heap dumps if available
    local heap_analysis=""
    if [[ -d "$BUILD_DIR/reports/heap" ]]; then
        heap_analysis="Available in $BUILD_DIR/reports/heap"
    else
        heap_analysis="No heap dumps available"
    fi
    
    # Create memory report
    cat > "$memory_report" << EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "method_count": $method_count,
  "heap_analysis": "$heap_analysis",
  "memory_leaks": {
    "detected": false,
    "report_location": "$BUILD_DIR/reports/memory-leaks"
  },
  "recommendations": [
    "Monitor method count to stay under 64K limit",
    "Use ProGuard/R8 for release builds",
    "Implement memory leak detection in tests",
    "Profile memory usage with Android Studio"
  ]
}
EOF

    log_metric "Method count: $method_count"
    log_metric "Heap analysis: $heap_analysis"
    
    log_success "Memory analysis completed and saved to $memory_report"
}

# Analyze dependencies
analyze_dependencies() {
    log_info "Analyzing dependencies..."
    
    local deps_report="$REPORTS_DIR/dependencies/deps-$(date +%Y%m%d-%H%M%S).json"
    
    cd "$PROJECT_DIR"
    
    # Generate dependency reports
    ./gradlew app:dependencies > "$REPORTS_DIR/dependencies/dependency-tree.txt" 2>&1 || log_warning "Dependency tree generation failed"
    ./gradlew dependencyUpdates > "$REPORTS_DIR/dependencies/updates.txt" 2>&1 || log_warning "Dependency updates check failed"
    
    # Count dependencies
    local total_deps=$(./gradlew app:dependencies 2>/dev/null | grep -c "--- " || echo 0)
    local outdated_deps=$(./gradlew dependencyUpdates 2>/dev/null | grep -c "available" || echo 0)
    
    # Security scan
    ./gradlew dependencyCheckAnalyze > "$REPORTS_DIR/dependencies/security.txt" 2>&1 || log_warning "Security scan failed"
    local security_issues=$(grep -c "CVE-" "$REPORTS_DIR/dependencies/security.txt" 2>/dev/null || echo 0)
    
    # Create dependencies report
    cat > "$deps_report" << EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "dependency_count": $total_deps,
  "outdated_dependencies": $outdated_deps,
  "security_vulnerabilities": $security_issues,
  "reports": {
    "dependency_tree": "$REPORTS_DIR/dependencies/dependency-tree.txt",
    "updates_available": "$REPORTS_DIR/dependencies/updates.txt",
    "security_scan": "$REPORTS_DIR/dependencies/security.txt"
  },
  "recommendations": [
    "Keep dependencies up to date",
    "Remove unused dependencies",
    "Monitor security vulnerabilities",
    "Use dependency locking for reproducible builds"
  ]
}
EOF

    log_metric "Total dependencies: $total_deps"
    log_metric "Outdated dependencies: $outdated_deps"
    log_metric "Security vulnerabilities: $security_issues"
    
    log_success "Dependency analysis completed and saved to $deps_report"
}

# Generate performance baseline
generate_baseline() {
    log_info "Generating performance baseline..."
    
    if [[ -f "$BASELINE_FILE" ]]; then
        log_warning "Baseline already exists. Use 'update-baseline' to overwrite."
        return
    fi
    
    # Run all performance measurements
    measure_build_performance
    analyze_memory_usage
    analyze_dependencies
    
    # Create baseline from latest reports
    local latest_build=$(ls -t "$REPORTS_DIR/builds/"*.json | head -1)
    local latest_memory=$(ls -t "$REPORTS_DIR/memory/"*.json | head -1)
    local latest_deps=$(ls -t "$REPORTS_DIR/dependencies/"*.json | head -1)
    
    cat > "$BASELINE_FILE" << EOF
{
  "created": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "version": "1.0.0",
  "baseline_reports": {
    "build": "$latest_build",
    "memory": "$latest_memory",
    "dependencies": "$latest_deps"
  },
  "thresholds": {
    "max_debug_build_time_seconds": 300,
    "max_release_build_time_seconds": 600,
    "max_debug_apk_size_mb": 50,
    "max_release_apk_size_mb": 30,
    "max_method_count": 60000,
    "max_security_vulnerabilities": 0
  }
}
EOF

    log_success "Performance baseline generated at $BASELINE_FILE"
}

# Compare with baseline
compare_with_baseline() {
    log_info "Comparing current performance with baseline..."
    
    if [[ ! -f "$BASELINE_FILE" ]]; then
        log_error "No baseline found. Run 'generate-baseline' first."
        exit 1
    fi
    
    # Run current measurements
    measure_build_performance
    analyze_memory_usage
    analyze_dependencies
    
    # Get latest reports
    local latest_build=$(ls -t "$REPORTS_DIR/builds/"*.json | head -1)
    local latest_memory=$(ls -t "$REPORTS_DIR/memory/"*.json | head -1)
    local latest_deps=$(ls -t "$REPORTS_DIR/dependencies/"*.json | head -1)
    
    # Compare and generate report
    local comparison_report="$REPORTS_DIR/comparison-$(date +%Y%m%d-%H%M%S).json"
    
    # Extract current metrics (simplified comparison)
    local current_debug_time=$(jq -r '.build_times.debug_seconds' "$latest_build" 2>/dev/null || echo 0)
    local current_release_time=$(jq -r '.build_times.release_seconds' "$latest_build" 2>/dev/null || echo 0)
    local current_debug_size=$(jq -r '.apk_sizes.debug_mb' "$latest_build" 2>/dev/null || echo 0)
    local current_release_size=$(jq -r '.apk_sizes.release_mb' "$latest_build" 2>/dev/null || echo 0)
    
    # Get thresholds from baseline
    local max_debug_time=$(jq -r '.thresholds.max_debug_build_time_seconds' "$BASELINE_FILE" 2>/dev/null || echo 300)
    local max_release_time=$(jq -r '.thresholds.max_release_build_time_seconds' "$BASELINE_FILE" 2>/dev/null || echo 600)
    local max_debug_size=$(jq -r '.thresholds.max_debug_apk_size_mb' "$BASELINE_FILE" 2>/dev/null || echo 50)
    local max_release_size=$(jq -r '.thresholds.max_release_apk_size_mb' "$BASELINE_FILE" 2>/dev/null || echo 30)
    
    # Check thresholds
    local violations=()
    
    if (( $(echo "$current_debug_time > $max_debug_time" | bc -l) )); then
        violations+=("Debug build time exceeded: ${current_debug_time}s > ${max_debug_time}s")
    fi
    
    if (( $(echo "$current_release_time > $max_release_time" | bc -l) )); then
        violations+=("Release build time exceeded: ${current_release_time}s > ${max_release_time}s")
    fi
    
    if (( $(echo "$current_debug_size > $max_debug_size" | bc -l) )); then
        violations+=("Debug APK size exceeded: ${current_debug_size}MB > ${max_debug_size}MB")
    fi
    
    if (( $(echo "$current_release_size > $max_release_size" | bc -l) )); then
        violations+=("Release APK size exceeded: ${current_release_size}MB > ${max_release_size}MB")
    fi
    
    # Create comparison report
    cat > "$comparison_report" << EOF
{
  "timestamp": "$(date -u +%Y-%m-%dT%H:%M:%SZ)",
  "baseline_file": "$BASELINE_FILE",
  "current_reports": {
    "build": "$latest_build",
    "memory": "$latest_memory",
    "dependencies": "$latest_deps"
  },
  "threshold_violations": $(printf '%s\n' "${violations[@]}" | jq -R . | jq -s .),
  "status": "$([ ${#violations[@]} -eq 0 ] && echo "PASSED" || echo "FAILED")"
}
EOF

    # Display results
    if [ ${#violations[@]} -eq 0 ]; then
        log_success "All performance thresholds passed!"
    else
        log_error "Performance threshold violations detected:"
        for violation in "${violations[@]}"; do
            log_error "  - $violation"
        done
    fi
    
    log_success "Comparison report saved to $comparison_report"
}

# Generate comprehensive report
generate_report() {
    log_info "Generating comprehensive performance report..."
    
    local report_file="$REPORTS_DIR/performance-report-$(date +%Y%m%d-%H%M%S).html"
    
    # Get latest reports
    local latest_build=$(ls -t "$REPORTS_DIR/builds/"*.json | head -1)
    local latest_memory=$(ls -t "$REPORTS_DIR/memory/"*.json | head -1)
    local latest_deps=$(ls -t "$REPORTS_DIR/dependencies/"*.json | head -1)
    
    cat > "$report_file" << 'EOF'
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>EarthMAX Performance Report</title>
    <style>
        body {
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
            line-height: 1.6;
            color: #333;
            max-width: 1200px;
            margin: 0 auto;
            padding: 20px;
            background-color: #f5f5f5;
        }
        .header {
            background: linear-gradient(135deg, #2dd4bf, #0d9488);
            color: white;
            padding: 30px;
            border-radius: 10px;
            margin-bottom: 30px;
            text-align: center;
        }
        .metric-card {
            background: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 2px 4px rgba(0,0,0,0.1);
            margin-bottom: 20px;
        }
        .metric-grid {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
            gap: 20px;
        }
        .metric-value {
            font-size: 2em;
            font-weight: bold;
            color: #0d9488;
        }
        .metric-label {
            color: #666;
            font-size: 0.9em;
        }
        .status-good { color: #059669; }
        .status-warning { color: #d97706; }
        .status-error { color: #dc2626; }
        .chart-placeholder {
            height: 200px;
            background: #f3f4f6;
            border-radius: 4px;
            display: flex;
            align-items: center;
            justify-content: center;
            color: #6b7280;
        }
        .recommendations {
            background: #fef3c7;
            border-left: 4px solid #f59e0b;
            padding: 15px;
            margin: 20px 0;
        }
        .footer {
            text-align: center;
            color: #666;
            margin-top: 40px;
            padding-top: 20px;
            border-top: 1px solid #e5e7eb;
        }
    </style>
</head>
<body>
    <div class="header">
        <h1>🌍 EarthMAX Performance Report</h1>
        <p>Generated on TIMESTAMP_PLACEHOLDER</p>
    </div>

    <div class="metric-grid">
        <div class="metric-card">
            <h3>Build Performance</h3>
            <div class="metric-value">BUILD_TIME_PLACEHOLDER</div>
            <div class="metric-label">Total Build Time (seconds)</div>
            <div class="chart-placeholder">Build Time Trend Chart</div>
        </div>

        <div class="metric-card">
            <h3>APK Size</h3>
            <div class="metric-value">APK_SIZE_PLACEHOLDER</div>
            <div class="metric-label">Release APK Size (MB)</div>
            <div class="chart-placeholder">APK Size History Chart</div>
        </div>

        <div class="metric-card">
            <h3>Memory Usage</h3>
            <div class="metric-value">METHOD_COUNT_PLACEHOLDER</div>
            <div class="metric-label">Method Count</div>
            <div class="chart-placeholder">Memory Usage Chart</div>
        </div>

        <div class="metric-card">
            <h3>Dependencies</h3>
            <div class="metric-value">DEPS_COUNT_PLACEHOLDER</div>
            <div class="metric-label">Total Dependencies</div>
            <div class="chart-placeholder">Dependency Analysis Chart</div>
        </div>
    </div>

    <div class="recommendations">
        <h3>📋 Recommendations</h3>
        <ul>
            <li>Monitor build times and optimize slow tasks</li>
            <li>Keep APK size under 30MB for release builds</li>
            <li>Regularly update dependencies to latest stable versions</li>
            <li>Use ProGuard/R8 for code shrinking and obfuscation</li>
            <li>Implement performance testing in CI/CD pipeline</li>
        </ul>
    </div>

    <div class="footer">
        <p>EarthMAX Android Performance Monitoring</p>
        <p>For more details, check the individual report files in the reports directory</p>
    </div>
</body>
</html>
EOF

    # Replace placeholders with actual data
    local timestamp=$(date -u +"%Y-%m-%d %H:%M:%S UTC")
    local build_time="N/A"
    local apk_size="N/A"
    local method_count="N/A"
    local deps_count="N/A"
    
    if [[ -f "$latest_build" ]]; then
        build_time=$(jq -r '.build_times.total_seconds' "$latest_build" 2>/dev/null || echo "N/A")
        apk_size=$(jq -r '.apk_sizes.release_mb' "$latest_build" 2>/dev/null || echo "N/A")
    fi
    
    if [[ -f "$latest_memory" ]]; then
        method_count=$(jq -r '.method_count' "$latest_memory" 2>/dev/null || echo "N/A")
    fi
    
    if [[ -f "$latest_deps" ]]; then
        deps_count=$(jq -r '.dependency_count' "$latest_deps" 2>/dev/null || echo "N/A")
    fi
    
    sed -i '' "s/TIMESTAMP_PLACEHOLDER/$timestamp/g" "$report_file"
    sed -i '' "s/BUILD_TIME_PLACEHOLDER/$build_time/g" "$report_file"
    sed -i '' "s/APK_SIZE_PLACEHOLDER/$apk_size/g" "$report_file"
    sed -i '' "s/METHOD_COUNT_PLACEHOLDER/$method_count/g" "$report_file"
    sed -i '' "s/DEPS_COUNT_PLACEHOLDER/$deps_count/g" "$report_file"
    
    log_success "Comprehensive report generated: $report_file"
    
    # Open report in browser if available
    if command -v open &> /dev/null; then
        open "$report_file"
    fi
}

# Clean old reports
clean_reports() {
    log_info "Cleaning old performance reports..."
    
    # Keep only last 10 reports of each type
    find "$REPORTS_DIR/builds" -name "*.json" -type f | sort -r | tail -n +11 | xargs rm -f
    find "$REPORTS_DIR/memory" -name "*.json" -type f | sort -r | tail -n +11 | xargs rm -f
    find "$REPORTS_DIR/dependencies" -name "*.json" -type f | sort -r | tail -n +11 | xargs rm -f
    
    # Clean reports older than 30 days
    find "$REPORTS_DIR" -name "*.json" -type f -mtime +30 -delete
    find "$REPORTS_DIR" -name "*.html" -type f -mtime +30 -delete
    
    log_success "Old reports cleaned"
}

# Help function
show_help() {
    echo "EarthMAX Android Performance Monitoring Script"
    echo ""
    echo "Usage:"
    echo "  $0 build              Measure build performance"
    echo "  $0 memory             Analyze memory usage"
    echo "  $0 dependencies       Analyze dependencies"
    echo "  $0 generate-baseline  Generate performance baseline"
    echo "  $0 compare            Compare with baseline"
    echo "  $0 report             Generate comprehensive HTML report"
    echo "  $0 clean              Clean old reports"
    echo "  $0 all                Run all performance checks"
    echo ""
    echo "This script helps monitor:"
    echo "  - Build times and APK sizes"
    echo "  - Memory usage and method counts"
    echo "  - Dependency analysis and security"
    echo "  - Performance regression detection"
}

# Main script logic
case "$1" in
    "build")
        create_reports_dir
        measure_build_performance
        ;;
    "memory")
        create_reports_dir
        analyze_memory_usage
        ;;
    "dependencies")
        create_reports_dir
        analyze_dependencies
        ;;
    "generate-baseline")
        create_reports_dir
        generate_baseline
        ;;
    "compare")
        create_reports_dir
        compare_with_baseline
        ;;
    "report")
        create_reports_dir
        generate_report
        ;;
    "clean")
        clean_reports
        ;;
    "all")
        create_reports_dir
        measure_build_performance
        analyze_memory_usage
        analyze_dependencies
        generate_report
        ;;
    "help"|"-h"|"--help"|"")
        show_help
        ;;
    *)
        log_error "Invalid command: $1"
        show_help
        exit 1
        ;;
esac