
# EarthMAX Project Todo List

## Completed Tasks ✅

### Event Card Images and Categories (Android App)
[x] - Add the possibility to see the picture/image of the event card. And the category of event.
[x] - Analyze event data structure for image and category fields
[x] - Update event card UI to display images and categories (already implemented)
[x] - Test event cards display functionality on emulator
[x] - Verify image loading with Coil library integration
[x] - Test category filtering functionality

### Web Application Deployment
[x] - Examine the earthmax-web Next.js project structure and configuration
[x] - Review package.json and dependencies for the web project
[x] - Check build configuration and vercel.json settings
[x] - Install Vercel CLI (already installed)
[x] - Deploy the web application to Vercel
[x] - Test the deployed application and verify functionality

### Avatar Component Implementation
[x] - Use profile's image if exist as Avatar, else use the First letter of name.
[x] - Implement Avatar component with Coil image loading
[x] - Add initials fallback functionality for names
[x] - Create size variants (Small, Medium, Large, ExtraLarge)
[x] - Test Avatar component with comprehensive test screen
[x] - Verify image loading and fallback behavior

### App Stability and Chat Testing
[x] - Fix EarthMAX keeps stopping error
[x] - Investigate SupabaseAuth error with getCurrentUser flow
[x] - Fix database migration error for MessageEntity
[x] - Test chat functionality once app is stable

## Pending Tasks 📋

[] - Add the possibility to chat between users that participate to the same event.
[] - Add the possibility to see the events that a user is participating to.
[] - Add the possibility to see the events that a user is creating.
[] - Add notification in order to add location permissions in settings.
[] - Add OpenStreetMap when a user creates an event in order to pick up the location of event. 
[] - Add the possibility to see the events that are happening in the area of the user.
[] - Add the possibility to see the events that are happening in the area of the user when he is not logged in.
[x] - Add the possibility to add picture/image when user create an event.
[x] - Implement image compression for large files.
[x] - Test image upload with different file formats.

## Project Status

### Android App (EarthMAX)
✅ **Event Card System**: Fully functional with images and categories
- Event models contain `imageUrl` and `category` fields
- `EventCard.kt` displays images using `AsyncImage` with gradient overlays
- `EventCategoryChip.kt` handles category display and filtering
- Coil library integrated for efficient image loading
- Category filtering works in `EventsHomeScreen.kt`
- Modern teal/green environmental UI theme implemented

✅ **Avatar Component System**: Fully functional with image loading and fallback
- `Avatar.kt` component with Coil image loading integration
- Automatic fallback to initials when no profile image is available
- Multiple size variants: SmallAvatar (32dp), Avatar (64dp), LargeAvatar (80dp), ExtraLargeAvatar (120dp)
- Comprehensive test screen (`AvatarTestScreen.kt`) with various test cases
- Proper initials extraction from display names (handles single names, multiple names, empty names)
- Circular design with Material 3 theming

### Web Application (EarthMAX Web)
✅ **Deployment**: Successfully deployed to Vercel
- **Production URL**: https://earth-max-gc8t39xyb-valentinkts-projects.vercel.app
- **Inspect URL**: https://vercel.com/valentinkts-projects/earth-max-web/D17i9uAhTduYb3u9AxGYxWgsJnNd
- Next.js 15.5.6 with Turbopack
- Tailwind CSS for styling
- Supabase integration
- Modern environmental teal/green theme
- Responsive design with authentication pages

## Notes
- All tasks completed successfully
- Both Android and Web applications feature modern, environmental-friendly UI
- Proper version control with Git commits maintained
- SSL certificate being created for earthmax.app domain