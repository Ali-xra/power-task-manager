# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

**Power** is a comprehensive Android productivity and task management application written in Kotlin. The app features an alarm-based motivation system, task categorization, progress tracking, and multilingual support (Persian/English).

### Core Features
- Smart alarm system with morning challenges and evening reflection
- Task management with categories, priorities, and time periods
- Daily rating and progress tracking
- Backup/restore functionality
- Multi-language support with dynamic language switching
- User authentication system

## Build System & Commands

### Gradle Commands
```bash
# Build the app
./gradlew build

# Install debug version
./gradlew installDebug

# Clean build
./gradlew clean

# Run tests
./gradlew test

# Run Android instrumented tests
./gradlew connectedAndroidTest
```

### Project Structure
- **compileSdk**: 34
- **minSdk**: 26
- **targetSdk**: 34
- **Kotlin version**: 2.0.21
- **Android Gradle Plugin**: 8.12.0

## Architecture & Key Components

### Core Architecture Patterns
- **MVVM-like pattern** using Activities with direct data layer access
- **BaseActivity** for consistent language management across all screens
- **PowerApplication** for global app state and language handling
- **PreferencesManager** as central data persistence layer using SharedPreferences + Gson

### Data Layer
- **PreferencesManager**: Centralized data storage using SharedPreferences with JSON serialization
- **Task**: Core data model with completion tracking, priority levels, and time periods
- **TaskCategory**: Categorization system with custom colors and icons
- **BackupManager**: Handles data export/import functionality

### UI Layer Structure
```
MainActivity (entry point after login)
├── AlarmsActivity (morning/evening alarm management)
├── TasksActivity (task creation and management)
├── GoalsActivity (category-based task organization)
├── CalendarActivity (calendar view of tasks and ratings)
├── StatsActivity (progress tracking and analytics)
├── BackupActivity (data backup/restore)
├── SettingsActivity (app configuration)
├── AlarmActivity (morning challenge interface)
└── EveningActivity (evening reflection interface)
```

### Service Components
- **AlarmService**: Foreground service for morning alarms
- **EveningService**: Foreground service for evening reminders
- **AlarmReceiver/EveningReceiver**: Broadcast receivers for alarm events
- **BootReceiver**: Handles alarm restoration after device restart

### Key Adapters
- **TasksAdapter**: Main task list display
- **CategoriesAdapter**: Category management
- **CategoriesWithTasksAdapter**: Combined category and task view
- **CalendarAdapter**: Calendar day view with ratings

## Language System

The app implements a comprehensive multilingual system:

### Language Management
- **PowerApplication**: Handles app-level language configuration
- **BaseActivity**: Ensures consistent language application across all activities
- Languages supported: Persian (`fa`) and English (`en`)
- Dynamic language switching without app restart

### Localization Files
- `res/values/strings.xml` (Persian - default)
- `res/values-en/strings.xml` (English)

## Data Models & Business Logic

### Task Management
```kotlin
Task(
    id: String,
    title: String,
    categoryId: String,
    timePeriod: TimePeriod, // TODAY, WEEK, MONTH
    priority: TaskPriority, // NORMAL, HIGH, URGENT
    isCompleted: Boolean
)
```

### Category System
```kotlin
TaskCategory(
    id: String,
    name: String,
    color: String, // Hex color
    icon: String   // Emoji
)
```

### Rating System
- Daily ratings (1-10 scale)
- Category-specific ratings
- Evening reflection process with multiple steps

## Development Guidelines

### Code Conventions
- **Language**: Kotlin with official code style
- **View Binding**: Enabled throughout the app
- **Comments**: Persian comments in code (preserve existing style)
- **Naming**: Follow Android naming conventions

### Key Dependencies
- AndroidX Core KTX (1.12.0)
- Material Design Components (1.11.0)
- ConstraintLayout (2.1.4)
- Gson (2.10.1) for JSON serialization

### Testing
- Unit tests: `./gradlew test`
- Instrumented tests: `./gradlew connectedAndroidTest`
- Test runner: AndroidJUnitRunner

### Permissions Required
- WAKE_LOCK, RECEIVE_BOOT_COMPLETED (alarm functionality)
- SYSTEM_ALERT_WINDOW, USE_FULL_SCREEN_INTENT (alarm display)
- FOREGROUND_SERVICE, POST_NOTIFICATIONS (background services)
- SCHEDULE_EXACT_ALARM (precise alarm timing)
- External storage permissions (backup functionality)

## Common Development Tasks

### Adding New Features
1. Check existing patterns in similar activities
2. Extend BaseActivity for consistent language support
3. Use PreferencesManager for data persistence
4. Follow existing adapter patterns for lists
5. Add appropriate string resources in both languages

### Working with Alarms
- Morning alarms: Use AlarmUtils with AlarmService
- Evening reminders: Use EveningService
- All alarms must handle boot completion via BootReceiver

### Data Management
- All data operations should go through PreferencesManager
- Use Gson for complex object serialization
- Implement backup/restore for new data types

### UI Development
- Use ViewBinding for view access
- Follow Material Design patterns
- Ensure RTL support for Persian language
- Test both language modes