# YoloMedia

A modern, feature-rich native Android media player and gallery app built with Kotlin.

## Features

### Videos
- Auto-scan and display all videos from device storage via MediaStore API
- Group videos by folders with thumbnails and media count
- Custom MediaPlayer-based video player with auto-orientation, gesture controls, frame scrubbing
- Video options: delete, details, move to Safe, share

### Gallery
- Grid layout displaying all device images
- Full-screen image viewer with swipe controls
- Image options: delete, details, move to Safe, share

### Safe (Vault)
- PIN authentication with setup flow
- Biometric authentication (fingerprint/face)
- Folder-based organization with Videos/Photos tabs
- Media hidden by changing extension to `.nomedia`
- Restore media back to gallery

### Settings
- Theme switching: Light / Dark (dark blue) / System
- Accent color selection (10 colors)
- Grid/List view toggle
- Sort options: name, date, size (ascending/descending)
- Enable/disable animations
- Bottom bar styles: Fixed, Floating, Compact
- App version info

## Architecture
- **MVVM** with ViewModel + LiveData
- **Repository pattern** for data access
- **MediaStore API** for scoped storage support (Android 10+)
- **Custom MediaPlayer** for video playback with frame scrubbing
- **Glide** for image/video thumbnail loading
- **AndroidX Biometric** for secure authentication

## Tech Stack
- Kotlin
- AndroidX (AppCompat, ConstraintLayout, RecyclerView, CardView, Navigation)
- Custom MediaPlayer (built-in, no external dependency)
- Glide
- Biometric API
- ViewBinding

## Requirements
- Android 8.0+ (API 26)
- Target SDK 34

## Building
```bash
./gradlew assembleDebug    # Debug APK
./gradlew assembleRelease  # Release APK
```
