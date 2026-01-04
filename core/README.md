# Core Module

## Purpose
The `core` module contains shared logic, utility classes, and common dependencies used across all feature modules. It serves as the foundation for the application's architecture.

## Architecture
This module follows the Clean Architecture principles adopted by the rest of the app but focuses on reusable components:
*   **Data Layer**: Common network configurations, database definitions (Room), and data sources.
*   **Domain Layer**: Shared use cases and domain models.
*   **UI Layer**: Common UI components, themes, and extension functions.

## Key Components
*   **Base Classes**: Base Activities/Fragments or ViewModels if applicable.
*   **DI**: Core Hilt modules providing application-wide singletons.
*   **Utils**: Extension functions for Kotlin standard library and Android framework.
