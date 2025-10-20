#!/bin/bash

# EarthMAX Android CI Setup Script
# This script sets up the CI/CD environment and dependencies

set -e  # Exit on any error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
KEYSTORE_DIR="$PROJECT_DIR/keystore"
CONFIG_DIR="$PROJECT_DIR/config"

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

# Create necessary directories
create_directories() {
    log_info "Creating necessary directories..."
    
    mkdir -p "$KEYSTORE_DIR"
    mkdir -p "$PROJECT_DIR/reports"
    mkdir -p "$PROJECT_DIR/.github/workflows"
    mkdir -p "$CONFIG_DIR/detekt"
    mkdir -p "$CONFIG_DIR/ktlint"
    mkdir -p "$CONFIG_DIR/owasp"
    
    log_success "Directories created"
}

# Generate debug keystore
generate_debug_keystore() {
    log_info "Generating debug keystore..."
    
    local debug_keystore="$KEYSTORE_DIR/debug.keystore"
    
    if [[ ! -f "$debug_keystore" ]]; then
        keytool -genkey -v \
            -keystore "$debug_keystore" \
            -storepass android \
            -alias androiddebugkey \
            -keypass android \
            -keyalg RSA \
            -keysize 2048 \
            -validity 10000 \
            -dname "CN=Android Debug,O=Android,C=US"
        
        log_success "Debug keystore generated"
    else
        log_info "Debug keystore already exists"
    fi
}

# Generate release keystore template
generate_release_keystore_template() {
    log_info "Creating release keystore template..."
    
    local keystore_template="$KEYSTORE_DIR/README.md"
    
    cat > "$keystore_template" << 'EOF'
# Keystore Configuration

## Release Keystore Setup

To set up the release keystore for production builds:

1. Generate a release keystore:
```bash
keytool -genkey -v -keystore release.keystore -alias earthmax -keyalg RSA -keysize 2048 -validity 10000
```

2. Set the following environment variables in your CI/CD system:
```bash
export KEYSTORE_PASSWORD="your_keystore_password"
export KEY_ALIAS="earthmax"
export KEY_PASSWORD="your_key_password"
```

3. Upload the keystore file to your CI/CD system's secure file storage.

## Environment Variables Required

### For CI/CD
- `KEYSTORE_PASSWORD`: Password for the keystore file
- `KEY_ALIAS`: Alias of the key in the keystore
- `KEY_PASSWORD`: Password for the key

### For Firebase App Distribution
- `FIREBASE_APP_ID`: Firebase App ID
- `FIREBASE_TOKEN`: Firebase CI token

### For Google Play Console
- `GOOGLE_PLAY_SERVICE_ACCOUNT_JSON`: Service account JSON content

### For GitHub Releases
- `GITHUB_TOKEN`: GitHub personal access token
- `GITHUB_REPOSITORY`: Repository in format "owner/repo"

### For Slack Notifications
- `SLACK_WEBHOOK_URL`: Slack webhook URL for notifications

### For SonarCloud
- `SONAR_TOKEN`: SonarCloud authentication token
- `SONAR_HOST_URL`: SonarCloud server URL (usually https://sonarcloud.io)

## Security Notes

- Never commit keystore files to version control
- Store sensitive information in CI/CD environment variables
- Use different keystores for debug and release builds
- Regularly rotate access tokens and passwords
EOF

    log_success "Release keystore template created"
}

# Setup Git hooks
setup_git_hooks() {
    log_info "Setting up Git hooks..."
    
    local hooks_dir="$PROJECT_DIR/.git/hooks"
    
    # Pre-commit hook
    cat > "$hooks_dir/pre-commit" << 'EOF'
#!/bin/bash

# Run Ktlint check before commit
echo "Running Ktlint check..."
./gradlew ktlintCheck

if [ $? -ne 0 ]; then
    echo "Ktlint check failed. Please fix formatting issues."
    echo "You can run './gradlew ktlintFormat' to auto-fix some issues."
    exit 1
fi

# Run Detekt check before commit
echo "Running Detekt check..."
./gradlew detekt

if [ $? -ne 0 ]; then
    echo "Detekt check failed. Please fix code quality issues."
    exit 1
fi

echo "Pre-commit checks passed!"
EOF

    # Pre-push hook
    cat > "$hooks_dir/pre-push" << 'EOF'
#!/bin/bash

# Run tests before push
echo "Running unit tests..."
./gradlew testDebugUnitTest

if [ $? -ne 0 ]; then
    echo "Unit tests failed. Please fix failing tests before pushing."
    exit 1
fi

echo "Pre-push checks passed!"
EOF

    chmod +x "$hooks_dir/pre-commit"
    chmod +x "$hooks_dir/pre-push"
    
    log_success "Git hooks set up"
}

# Validate Gradle wrapper
validate_gradle_wrapper() {
    log_info "Validating Gradle wrapper..."
    
    cd "$PROJECT_DIR"
    
    if [[ ! -f "gradlew" ]]; then
        log_error "Gradle wrapper not found. Please run 'gradle wrapper' first."
        exit 1
    fi
    
    # Make gradlew executable
    chmod +x gradlew
    
    # Test Gradle wrapper
    ./gradlew --version
    
    log_success "Gradle wrapper validated"
}

# Setup IDE configurations
setup_ide_config() {
    log_info "Setting up IDE configurations..."
    
    local idea_dir="$PROJECT_DIR/.idea"
    
    if [[ -d "$idea_dir" ]]; then
        # Code style configuration
        mkdir -p "$idea_dir/codeStyles"
        
        cat > "$idea_dir/codeStyles/Project.xml" << 'EOF'
<component name="ProjectCodeStyleConfiguration">
  <code_scheme name="Project" version="173">
    <JetCodeStyleSettings>
      <option name="PACKAGES_TO_USE_STAR_IMPORTS">
        <value>
          <package name="java.util" alias="false" withSubpackages="false" />
          <package name="kotlinx.android.synthetic" alias="false" withSubpackages="true" />
          <package name="io.ktor" alias="false" withSubpackages="true" />
        </value>
      </option>
      <option name="PACKAGES_IMPORT_LAYOUT">
        <value>
          <package name="" alias="false" withSubpackages="true" />
          <package name="java" alias="false" withSubpackages="true" />
          <package name="javax" alias="false" withSubpackages="true" />
          <package name="kotlin" alias="false" withSubpackages="true" />
          <package name="" alias="true" withSubpackages="true" />
        </value>
      </option>
      <option name="CODE_STYLE_DEFAULTS" value="KOTLIN_OFFICIAL" />
    </JetCodeStyleSettings>
    <codeStyleSettings language="kotlin">
      <option name="CODE_STYLE_DEFAULTS" value="KOTLIN_OFFICIAL" />
      <option name="KEEP_LINE_BREAKS" value="true" />
      <option name="KEEP_FIRST_COLUMN_COMMENT" value="true" />
      <option name="KEEP_CONTROL_STATEMENT_IN_ONE_LINE" value="false" />
      <option name="KEEP_BLANK_LINES_IN_DECLARATIONS" value="1" />
      <option name="KEEP_BLANK_LINES_IN_CODE" value="1" />
      <option name="KEEP_BLANK_LINES_BEFORE_RBRACE" value="0" />
      <option name="BLANK_LINES_AFTER_CLASS_HEADER" value="0" />
      <option name="ALIGN_MULTILINE_PARAMETERS" value="false" />
      <option name="ALIGN_MULTILINE_PARAMETERS_IN_CALLS" value="false" />
      <option name="ALIGN_MULTILINE_METHOD_BRACKETS" value="false" />
      <option name="ALIGN_MULTILINE_BINARY_OPERATION" value="false" />
      <option name="ALIGN_MULTILINE_ASSIGNMENT" value="false" />
      <option name="ALIGN_MULTILINE_TERNARY_OPERATION" value="false" />
      <option name="ALIGN_MULTILINE_THROWS_LIST" value="false" />
      <option name="ALIGN_THROWS_KEYWORD" value="false" />
      <option name="ALIGN_MULTILINE_EXTENDS_LIST" value="false" />
      <option name="CALL_PARAMETERS_WRAP" value="5" />
      <option name="CALL_PARAMETERS_LPAREN_ON_NEXT_LINE" value="false" />
      <option name="CALL_PARAMETERS_RPAREN_ON_NEXT_LINE" value="false" />
      <option name="METHOD_PARAMETERS_WRAP" value="5" />
      <option name="METHOD_PARAMETERS_LPAREN_ON_NEXT_LINE" value="false" />
      <option name="METHOD_PARAMETERS_RPAREN_ON_NEXT_LINE" value="false" />
      <option name="EXTENDS_LIST_WRAP" value="1" />
      <option name="METHOD_CALL_CHAIN_WRAP" value="1" />
      <option name="ASSIGNMENT_WRAP" value="1" />
      <option name="TERNARY_OPERATION_WRAP" value="1" />
      <option name="TERNARY_OPERATION_SIGNS_ON_NEXT_LINE" value="false" />
      <option name="KEEP_SIMPLE_BLOCKS_IN_ONE_LINE" value="false" />
      <option name="KEEP_SIMPLE_METHODS_IN_ONE_LINE" value="false" />
      <option name="KEEP_SIMPLE_LAMBDAS_IN_ONE_LINE" value="false" />
      <option name="KEEP_SIMPLE_CLASSES_IN_ONE_LINE" value="false" />
      <option name="ARRAY_INITIALIZER_WRAP" value="1" />
      <option name="ARRAY_INITIALIZER_LBRACE_ON_NEXT_LINE" value="false" />
      <option name="ARRAY_INITIALIZER_RBRACE_ON_NEXT_LINE" value="false" />
      <option name="WRAP_ELVIS_EXPRESSIONS" value="1" />
      <option name="IF_BRACE_FORCE" value="3" />
      <option name="DOWHILE_BRACE_FORCE" value="3" />
      <option name="WHILE_BRACE_FORCE" value="3" />
      <option name="FOR_BRACE_FORCE" value="3" />
    </codeStyleSettings>
  </code_scheme>
</component>
EOF

        log_success "IDE configurations set up"
    else
        log_info "No .idea directory found, skipping IDE configuration"
    fi
}

# Install required tools
install_tools() {
    log_info "Checking required tools..."
    
    # Check Java
    if ! command -v java &> /dev/null; then
        log_error "Java is not installed. Please install Java 11 or higher."
        exit 1
    fi
    
    # Check Android SDK
    if [[ -z "$ANDROID_HOME" ]]; then
        log_warning "ANDROID_HOME is not set. Please set it to your Android SDK path."
    fi
    
    # Check Node.js (for Firebase CLI)
    if ! command -v node &> /dev/null; then
        log_warning "Node.js is not installed. Firebase CLI won't be available."
    fi
    
    log_success "Tool check completed"
}

# Run initial build
run_initial_build() {
    log_info "Running initial build to verify setup..."
    
    cd "$PROJECT_DIR"
    
    # Clean and build
    ./gradlew clean
    ./gradlew assembleDebug
    
    log_success "Initial build completed successfully"
}

# Main setup function
setup_ci() {
    log_info "Setting up CI/CD environment for EarthMAX Android"
    
    create_directories
    generate_debug_keystore
    generate_release_keystore_template
    validate_gradle_wrapper
    setup_git_hooks
    setup_ide_config
    install_tools
    run_initial_build
    
    log_success "CI/CD setup completed successfully!"
    
    echo ""
    echo -e "${GREEN}Next Steps:${NC}"
    echo "1. Set up your release keystore (see keystore/README.md)"
    echo "2. Configure environment variables in your CI/CD system"
    echo "3. Set up SonarCloud project and get authentication token"
    echo "4. Configure Firebase App Distribution"
    echo "5. Set up Google Play Console service account"
    echo "6. Configure Slack webhook for notifications"
    echo ""
    echo -e "${BLUE}Useful Commands:${NC}"
    echo "./scripts/deploy.sh test                    - Run tests"
    echo "./scripts/deploy.sh build debug             - Build debug APK"
    echo "./scripts/deploy.sh deploy staging 1.0.0    - Deploy to staging"
    echo "./scripts/deploy.sh deploy production 1.0.0 - Deploy to production"
}

# Help function
show_help() {
    echo "EarthMAX Android CI Setup Script"
    echo ""
    echo "Usage:"
    echo "  $0 setup     Setup complete CI/CD environment"
    echo "  $0 hooks     Setup Git hooks only"
    echo "  $0 keystore  Generate debug keystore only"
    echo "  $0 build     Run initial build test"
    echo ""
    echo "This script will:"
    echo "  - Create necessary directories"
    echo "  - Generate debug keystore"
    echo "  - Set up Git hooks for code quality"
    echo "  - Configure IDE settings"
    echo "  - Validate Gradle wrapper"
    echo "  - Run initial build test"
}

# Main script logic
case "$1" in
    "setup"|"")
        setup_ci
        ;;
    "hooks")
        setup_git_hooks
        ;;
    "keystore")
        create_directories
        generate_debug_keystore
        ;;
    "build")
        run_initial_build
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