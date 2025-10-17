# EarthMAX Android App 🌍

EarthMAX is an environmental community platform that connects eco-conscious individuals to participate in local environmental events and initiatives. Built with modern Android development practices using Jetpack Compose and Material 3 design.

## Features ✨

### 🔐 Authentication
- User registration and login with Supabase Auth
- Secure user session management
- Profile creation and management

### 🌱 Events Management
- Browse environmental events in your area
- Create and organize eco-friendly events
- Join events and connect with like-minded individuals
- Event categories: Tree Planting, Beach Cleanup, Recycling, Wildlife Conservation, and more
- Real-time event updates and notifications

### 👤 User Profile
- Personal profile management
- Track your environmental impact
- View participation history
- Customize your eco-preferences

### 🗺️ Location Services
- Google Maps integration for event locations
- Location-based event discovery
- Interactive map view of nearby events

## Tech Stack 🛠️

### Architecture
- **Multi-module architecture** for scalability and maintainability
- **MVVM pattern** with ViewModels and StateFlow
- **Clean Architecture** principles
- **Dependency Injection** with Hilt

### UI/UX
- **Jetpack Compose** for modern declarative UI
- **Material 3** design system with environmental theme
- **Navigation Compose** for seamless navigation
- **Responsive design** for various screen sizes

### Backend & Data
- **Supabase Authentication** for user management
- **Supabase PostgreSQL** for real-time data storage
- **OpenStreetMap** for interactive maps
- **Room Database** for local data caching
- **Repository pattern** for data abstraction

### Additional Libraries
- **Coil** for efficient image loading
- **Google Maps SDK** for location services
- **Kotlin Coroutines** for asynchronous programming
- **Paging 3** for efficient data loading

## Project Structure 📁

```
EarthMAX/
├── app/                          # Main application module
├── core/
│   ├── core-model/              # Data models and entities
│   ├── core-ui/                 # Reusable UI components and theme
│   ├── core-utils/              # Utility functions and extensions
│   └── core-network/            # Network configuration and APIs
├── data/                        # Data layer with repositories
├── feature-auth/                # Authentication feature module
├── feature-events/              # Events management feature module
└── feature-profile/             # User profile feature module
```

## Getting Started 🚀

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9+
- Supabase project setup

### Setup Instructions

1. **Clone the repository**
   ```bash
   git clone https://github.com/your-username/earthmax-android.git
   cd earthmax-android
   ```

2. **Supabase Configuration**
   - Create a new Supabase project at [Supabase Console](https://supabase.com/dashboard)
   - Set up the database schema using the provided `supabase_setup.sql` file
   - Copy your project URL and anon key from the API settings
   - Create a `local.properties` file based on `local.properties.example`:
     ```
     SUPABASE_URL=your_supabase_project_url
     SUPABASE_ANON_KEY=your_supabase_anon_key
     ```

3. **Build and Run**
   - Open the project in Android Studio
   - Sync the project with Gradle files
   - Run the app on an emulator or physical device

## Configuration Files 📋

### Required Files (Not in Git)
- `local.properties` - Supabase configuration and local SDK paths

### Environment Variables
Add these to your `local.properties`:
```properties
SUPABASE_URL=your_supabase_project_url
SUPABASE_ANON_KEY=your_supabase_anon_key
# Optional for admin operations
SUPABASE_SERVICE_ROLE_KEY=your_service_role_key
```

## Contributing 🤝

We welcome contributions to make EarthMAX even better! Please follow these guidelines:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add some amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Code Style
- Follow Kotlin coding conventions
- Use meaningful variable and function names
- Add comments for complex logic
- Ensure all tests pass before submitting

## License 📄

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact 📧

For questions, suggestions, or support:
- Email: support@earthmax.com
- GitHub Issues: [Create an issue](https://github.com/your-username/earthmax-android/issues)

## Acknowledgments 🙏

- Thanks to all contributors who help make our planet greener
- Supabase for providing excellent backend services
- OpenStreetMap for providing free mapping services
- The open-source community for amazing libraries

---

**Together, we can make a difference! 🌱**# EarthMAX
