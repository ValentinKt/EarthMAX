# Supabase Test Configuration Guide

## Purpose
This guide provides alternative configurations to test and isolate the email validation issue in the EarthMAX Android app.

## Test Configuration 1: Disable Email Confirmation

### Step 1: Modify SupabaseClient.kt
Create a test version with minimal Auth configuration:

```kotlin
// File: core/core-network/src/main/java/com/earthmax/core/network/SupabaseClient.kt

object SupabaseClient {
    val client = createSupabaseClient(
        supabaseUrl = BuildConfig.SUPABASE_URL,
        supabaseKey = BuildConfig.SUPABASE_ANON_KEY
    ) {
        install(Auth) {
            // Minimal configuration for testing
            // Remove all custom settings temporarily
        }
        install(Postgrest)
        install(Realtime)
        install(Storage)
    }
}
```

### Step 2: Supabase Dashboard Settings
1. Go to **Authentication → Settings**
2. **Disable** "Enable email confirmations"
3. Set **Site URL** to: `http://localhost:3000` (or any valid URL)
4. Clear **Additional redirect URLs**
5. Save changes

### Step 3: Test Registration
- Build and install the app
- Try registering with a test email
- Check if registration succeeds without email confirmation

## Test Configuration 2: Alternative Email Provider

### Create Test Supabase Project
1. Create a new Supabase project for testing
2. Use default authentication settings
3. Update `local.properties` with new project credentials:

```properties
SUPABASE_URL=https://your-test-project.supabase.co
SUPABASE_ANON_KEY=your-test-anon-key
```

### Test with Default Settings
- Use the new project with default Auth configuration
- Test if email registration works with fresh settings
- Compare behavior with main project

## Test Configuration 3: Debug Mode

### Enhanced Logging Configuration
Add to `SupabaseClient.kt`:

```kotlin
install(Auth) {
    // Enable debug mode
    httpConfig {
        install(Logging) {
            logger = Logger.DEFAULT
            level = LogLevel.ALL
        }
    }
}
```

### Capture Network Logs
Use ADB to capture detailed network logs:

```bash
# Clear logs
adb logcat -c

# Start logging with network details
adb logcat -s "SupabaseAuth" "System.out" "okhttp" | tee supabase_debug.log

# Test registration and capture logs
```

## Test Configuration 4: Manual API Testing

### Direct API Call Test
Create a simple test function to call Supabase Auth API directly:

```kotlin
// Add to SupabaseAuthRepository.kt for testing
suspend fun testDirectSignup(email: String, password: String): String {
    return try {
        val client = HttpClient()
        val response = client.post("${SupabaseClient.client.supabaseUrl}/auth/v1/signup") {
            headers {
                append("apikey", BuildConfig.SUPABASE_ANON_KEY)
                append("Authorization", "Bearer ${BuildConfig.SUPABASE_ANON_KEY}")
                append("Content-Type", "application/json")
            }
            setBody("""
                {
                    "email": "$email",
                    "password": "$password"
                }
            """.trimIndent())
        }
        "Success: ${response.status}"
    } catch (e: Exception) {
        "Error: ${e.message}"
    }
}
```

## Test Configuration 5: Minimal Reproduction

### Create Minimal Test App
1. Create a new Android project with minimal Supabase setup
2. Use only basic Auth configuration
3. Test email registration with same Supabase project
4. Compare results with main EarthMAX app

### Basic Test Implementation
```kotlin
// Minimal test activity
class TestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        lifecycleScope.launch {
            try {
                val client = createSupabaseClient(
                    supabaseUrl = "YOUR_SUPABASE_URL",
                    supabaseKey = "YOUR_ANON_KEY"
                ) {
                    install(Auth)
                }
                
                val result = client.auth.signUpWith(Email) {
                    email = "test@example.com"
                    password = "testpassword123"
                }
                
                Log.d("TestAuth", "Success: $result")
            } catch (e: Exception) {
                Log.e("TestAuth", "Error: ${e.message}", e)
            }
        }
    }
}
```

## Expected Outcomes

### If Test Config 1 Works
- Issue is related to email confirmation settings
- Check email template configuration
- Verify redirect URL settings

### If Test Config 2 Works
- Issue is specific to current Supabase project
- Compare project settings between working and non-working projects
- Check for custom Auth hooks or policies

### If All Tests Fail
- Issue might be with Supabase client library version
- Check for known issues with current Supabase Kotlin client
- Consider downgrading/upgrading client version

### If Direct API Test Works
- Issue is with Supabase Kotlin client configuration
- Review client initialization and Auth plugin setup
- Check for conflicting dependencies

## Rollback Instructions

### Restore Original Configuration
1. Revert `SupabaseClient.kt` to original state
2. Restore original Supabase dashboard settings
3. Update `local.properties` with original credentials
4. Remove test code and debug logging

### Original SupabaseClient.kt
```kotlin
install(Auth) {
    scheme = "earthmax"
    host = "earthmax.app"
}
```

---
**Note**: Always test configurations in a development environment before applying to production.

