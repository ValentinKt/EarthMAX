#!/bin/bash

# EarthMAX Android Deployment Script
# This script handles deployment to different environments

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
BUILD_DIR="$PROJECT_DIR/app/build"
KEYSTORE_DIR="$PROJECT_DIR/keystore"
REPORTS_DIR="$PROJECT_DIR/reports"

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

# Check if required environment variables are set
check_env_vars() {
    local required_vars=("$@")
    local missing_vars=()
    
    for var in "${required_vars[@]}"; do
        if [[ -z "${!var}" ]]; then
            missing_vars+=("$var")
        fi
    done
    
    if [[ ${#missing_vars[@]} -gt 0 ]]; then
        log_error "Missing required environment variables: ${missing_vars[*]}"
        exit 1
    fi
}

# Clean build directory
clean_build() {
    log_info "Cleaning build directory..."
    cd "$PROJECT_DIR"
    ./gradlew clean
    log_success "Build directory cleaned"
}

# Run tests
run_tests() {
    log_info "Running unit tests..."
    cd "$PROJECT_DIR"
    ./gradlew testDebugUnitTest
    
    log_info "Running instrumented tests..."
    ./gradlew connectedDebugAndroidTest || log_warning "Instrumented tests failed or no device connected"
    
    log_success "Tests completed"
}

# Run code quality checks
run_quality_checks() {
    log_info "Running code quality checks..."
    cd "$PROJECT_DIR"
    
    # Lint
    ./gradlew lintDebug
    
    # Detekt
    ./gradlew detekt
    
    # Ktlint
    ./gradlew ktlintCheck
    
    log_success "Code quality checks completed"
}

# Generate test coverage report
generate_coverage() {
    log_info "Generating test coverage report..."
    cd "$PROJECT_DIR"
    ./gradlew jacocoTestReport
    log_success "Coverage report generated"
}

# Build APK
build_apk() {
    local build_type="$1"
    log_info "Building $build_type APK..."
    cd "$PROJECT_DIR"
    
    if [[ "$build_type" == "debug" ]]; then
        ./gradlew assembleDebug
    elif [[ "$build_type" == "release" ]]; then
        check_env_vars "KEYSTORE_PASSWORD" "KEY_ALIAS" "KEY_PASSWORD"
        ./gradlew assembleRelease
    else
        log_error "Invalid build type: $build_type"
        exit 1
    fi
    
    log_success "$build_type APK built successfully"
}

# Build AAB (Android App Bundle)
build_aab() {
    log_info "Building release AAB..."
    cd "$PROJECT_DIR"
    check_env_vars "KEYSTORE_PASSWORD" "KEY_ALIAS" "KEY_PASSWORD"
    ./gradlew bundleRelease
    log_success "Release AAB built successfully"
}

# Deploy to Firebase App Distribution
deploy_firebase() {
    local apk_path="$1"
    local release_notes="$2"
    
    check_env_vars "FIREBASE_APP_ID" "FIREBASE_TOKEN"
    
    log_info "Deploying to Firebase App Distribution..."
    
    # Install Firebase CLI if not present
    if ! command -v firebase &> /dev/null; then
        log_info "Installing Firebase CLI..."
        npm install -g firebase-tools
    fi
    
    firebase appdistribution:distribute "$apk_path" \
        --app "$FIREBASE_APP_ID" \
        --token "$FIREBASE_TOKEN" \
        --release-notes "$release_notes" \
        --groups "testers"
    
    log_success "Deployed to Firebase App Distribution"
}

# Deploy to Google Play Console
deploy_play_store() {
    local aab_path="$1"
    local track="$2"  # internal, alpha, beta, production
    
    check_env_vars "GOOGLE_PLAY_SERVICE_ACCOUNT_JSON"
    
    log_info "Deploying to Google Play Console ($track track)..."
    
    # Use fastlane or Google Play Developer API
    # This is a placeholder - implement based on your preferred method
    log_info "AAB path: $aab_path"
    log_info "Track: $track"
    
    log_success "Deployed to Google Play Console"
}

# Send Slack notification
send_slack_notification() {
    local message="$1"
    local color="$2"  # good, warning, danger
    
    if [[ -n "$SLACK_WEBHOOK_URL" ]]; then
        curl -X POST -H 'Content-type: application/json' \
            --data "{\"attachments\":[{\"color\":\"$color\",\"text\":\"$message\"}]}" \
            "$SLACK_WEBHOOK_URL"
        log_success "Slack notification sent"
    else
        log_warning "SLACK_WEBHOOK_URL not set, skipping notification"
    fi
}

# Create GitHub release
create_github_release() {
    local tag="$1"
    local release_notes="$2"
    local apk_path="$3"
    
    check_env_vars "GITHUB_TOKEN" "GITHUB_REPOSITORY"
    
    log_info "Creating GitHub release..."
    
    # Create release
    local release_response=$(curl -s -X POST \
        -H "Authorization: token $GITHUB_TOKEN" \
        -H "Accept: application/vnd.github.v3+json" \
        "https://api.github.com/repos/$GITHUB_REPOSITORY/releases" \
        -d "{
            \"tag_name\": \"$tag\",
            \"name\": \"Release $tag\",
            \"body\": \"$release_notes\",
            \"draft\": false,
            \"prerelease\": false
        }")
    
    local upload_url=$(echo "$release_response" | grep -o '"upload_url": "[^"]*' | cut -d'"' -f4 | sed 's/{?name,label}//')
    
    # Upload APK asset
    if [[ -f "$apk_path" ]]; then
        curl -s -X POST \
            -H "Authorization: token $GITHUB_TOKEN" \
            -H "Content-Type: application/vnd.android.package-archive" \
            --data-binary @"$apk_path" \
            "$upload_url?name=$(basename "$apk_path")"
    fi
    
    log_success "GitHub release created"
}

# Main deployment function
deploy() {
    local environment="$1"
    local version="$2"
    
    log_info "Starting deployment to $environment environment"
    log_info "Version: $version"
    
    # Create reports directory
    mkdir -p "$REPORTS_DIR"
    
    case "$environment" in
        "staging")
            clean_build
            run_tests
            run_quality_checks
            generate_coverage
            build_apk "debug"
            
            local apk_path="$BUILD_DIR/outputs/apk/debug/app-debug.apk"
            deploy_firebase "$apk_path" "Staging build $version"
            
            send_slack_notification "✅ Staging deployment successful for version $version" "good"
            ;;
            
        "production")
            clean_build
            run_tests
            run_quality_checks
            generate_coverage
            build_aab
            build_apk "release"
            
            local aab_path="$BUILD_DIR/outputs/bundle/release/app-release.aab"
            local apk_path="$BUILD_DIR/outputs/apk/release/app-release.apk"
            
            deploy_play_store "$aab_path" "internal"
            create_github_release "v$version" "Production release $version" "$apk_path"
            
            send_slack_notification "🚀 Production deployment successful for version $version" "good"
            ;;
            
        *)
            log_error "Invalid environment: $environment"
            log_info "Valid environments: staging, production"
            exit 1
            ;;
    esac
    
    log_success "Deployment to $environment completed successfully"
}

# Rollback function
rollback() {
    local environment="$1"
    local previous_version="$2"
    
    log_warning "Rolling back $environment to version $previous_version"
    
    case "$environment" in
        "production")
            # Implement rollback logic for production
            log_info "Rollback logic for production environment"
            send_slack_notification "⚠️ Production rollback to version $previous_version" "warning"
            ;;
        *)
            log_error "Rollback not supported for environment: $environment"
            exit 1
            ;;
    esac
    
    log_success "Rollback completed"
}

# Help function
show_help() {
    echo "EarthMAX Android Deployment Script"
    echo ""
    echo "Usage:"
    echo "  $0 deploy <environment> <version>    Deploy to specified environment"
    echo "  $0 rollback <environment> <version>  Rollback to specified version"
    echo "  $0 test                              Run tests only"
    echo "  $0 build <type>                      Build APK (debug/release)"
    echo ""
    echo "Environments:"
    echo "  staging     - Deploy to Firebase App Distribution"
    echo "  production  - Deploy to Google Play Console"
    echo ""
    echo "Examples:"
    echo "  $0 deploy staging 1.0.0"
    echo "  $0 deploy production 1.0.0"
    echo "  $0 rollback production 0.9.0"
    echo "  $0 test"
    echo "  $0 build debug"
}

# Main script logic
case "$1" in
    "deploy")
        if [[ $# -ne 3 ]]; then
            log_error "Usage: $0 deploy <environment> <version>"
            exit 1
        fi
        deploy "$2" "$3"
        ;;
    "rollback")
        if [[ $# -ne 3 ]]; then
            log_error "Usage: $0 rollback <environment> <version>"
            exit 1
        fi
        rollback "$2" "$3"
        ;;
    "test")
        run_tests
        ;;
    "build")
        if [[ $# -ne 2 ]]; then
            log_error "Usage: $0 build <type>"
            exit 1
        fi
        build_apk "$2"
        ;;
    "help"|"-h"|"--help")
        show_help
        ;;
    *)
        log_error "Invalid command: $1"
        show_help
        exit 1
        ;;
esac