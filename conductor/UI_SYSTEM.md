# UI System: Comiqueta
[Voltar ao Índice](./INDEX.md)

The UI is built entirely with Jetpack Compose, following a custom design system ("Comiqueta Design").

## 🎨 Design Tokens

- **Theme**: `ComiquetaTheme` (defined in `core:theme`).
- **Mode**: Automated Dark/Light mode support.
- **Colors**: Vibrant, high-contrast palette for comic covers.

## 🌍 Localization (L10n)

- **Supported**: EN (Default), PT, ES, DE.
- **Constraint**: **NO hardcoded strings** in Screen composables. All strings must come from `res/strings.xml`.
- **Dynamic**: RTL support where applicable.

## 🧩 Shared Components (`core:ui`)

- `ComiquetaScaffold`: Standard screen wrapper with TopBar/BottomBar support.
- `ComicCard`: Optimized for grid display with Coil for image loading.
- `ReaderView`: Optimized for high-resolution bitmap rendering and gesture handling.

## 🕹️ Micro-animations
- Used for page transitions in the viewer.
- Feedbacks on button interactions (Surface elevations/colors).

---
Status: **Active**
Last Updated: 2026-02-08
