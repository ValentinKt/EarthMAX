# Supabase Email Validation Troubleshooting Guide

## Issue Summary
**Problem**: Users cannot register with any email address, receiving `email_address_invalid` error
**Status**: Server-side Supabase configuration issue (NOT client-side)
**Project**: EarthMAX Android App
**Supabase Project ID**: `ciawfcutbbjvkhdhhwae`

## Error Details
```
E SupabaseAuth: io.github.jan.supabase.auth.exception.AuthRestException: email_address_invalid
E SupabaseAuth: Email address "test@example.com" is invalid: email_address_invalid
```

## Investigation Results
✅ **Client code is correct** - Android implementation properly uses Supabase Auth API
✅ **Email format validation passes** - Tested with various valid email formats
✅ **Network connectivity confirmed** - API calls reach Supabase servers
❌ **Server-side validation failing** - Error occurs before client-side processing

## Required Actions (Supabase Dashboard)

### 1. Check Authentication Settings
Navigate to **Supabase Dashboard → Authentication → Settings**

#### Email Configuration
- [ ] Verify **Email confirmation** is properly configured
- [ ] Check if **Email confirmation** is enabled but misconfigured
- [ ] Ensure **Email templates** are valid (if using custom templates)
- [ ] Verify **Site URL** matches your app configuration

#### Domain Restrictions
- [ ] Check **Domain allowlists** - ensure no restrictive domain filtering
- [ ] Verify **Blocked domains** list doesn't include common email providers
- [ ] Review **Email domain validation** settings

### 2. Review Auth Hooks
Navigate to **Authentication → Hooks**
- [ ] Check for custom **Email validation hooks**
- [ ] Review any **Pre-signup hooks** that might reject emails
- [ ] Verify **Custom validation functions** are not overly restrictive

### 3. Project Configuration
Navigate to **Settings → API**
- [ ] Confirm **Project URL** matches `local.properties`
- [ ] Verify **Anon key** is correct and active
- [ ] Check **Service role key** permissions (if used)

### 4. Database Policies
Navigate to **Authentication → Policies**
- [ ] Review **Row Level Security (RLS)** policies on `auth.users`
- [ ] Check if any policies are blocking user creation
- [ ] Verify **profiles** table policies allow new user insertion

## Debugging Steps

### Step 1: Test Email Confirmation Settings
1. **Disable email confirmation temporarily**:
   - Go to Authentication → Settings
   - Turn OFF "Enable email confirmations"
   - Test user registration

### Step 2: Check Supabase Logs
1. Navigate to **Logs** in Supabase Dashboard
2. Filter by **Auth** logs
3. Look for detailed error messages during signup attempts
4. Check for any custom validation failures

### Step 3: Test Different Email Providers
Try registering with emails from different providers:
- [ ] Gmail (@gmail.com)
- [ ] Yahoo (@yahoo.com)
- [ ] Outlook (@outlook.com)
- [ ] Custom domain (@yourdomain.com)

### Step 4: Verify Project Status
1. Check **Organization billing** status
2. Verify **Project is not paused**
3. Confirm **API limits** are not exceeded

## Quick Fixes to Try

### Fix 1: Reset Email Configuration
1. Go to Authentication → Settings
2. Disable email confirmation
3. Save changes
4. Re-enable email confirmation
5. Test registration

### Fix 2: Update Site URL
1. Go to Authentication → Settings
2. Set **Site URL** to: `https://earthmax.app`
3. Add **Additional redirect URLs**:
   - `earthmax://earthmax.app`
   - `https://earthmax.app/auth/callback`
4. Save and test

### Fix 3: Check Email Templates
1. Go to Authentication → Email Templates
2. Verify **Confirm signup** template is valid
3. Check for any syntax errors in custom templates
4. Reset to default template if needed

## Testing Configuration

### Alternative Test Setup
If you need to test without email confirmation:

1. **Temporarily modify SupabaseClient.kt**:
```kotlin
Auth {
    // Comment out custom redirect URLs for testing
    // scheme = "earthmax"
    // host = "earthmax.app"
}
```

2. **Disable email confirmation in dashboard**
3. **Test user registration**
4. **Re-enable settings after confirming fix**

## Expected Resolution
Once the correct Supabase dashboard setting is identified and fixed:
- Users should be able to register successfully
- Email confirmation emails should be sent (if enabled)
- Deep link handling should work for email confirmation

## Contact Information
- **Issue Date**: Current
- **Investigated By**: Development Team
- **Status**: Awaiting Supabase dashboard configuration fix
- **Next Steps**: Check Supabase dashboard settings as outlined above

---
**Note**: This is a server-side configuration issue. The Android client code is functioning correctly and does not require changes.