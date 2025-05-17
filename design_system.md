# ZiStudy Design System

This document outlines the design system used in ZiStudy, including colors, typography, spacing, and component usage guidelines. ZiStudy follows a modern, clean aesthetic with a focus on readability, usability, and visual appeal.

## Table of Contents

1. [Color Palette](#color-palette)
2. [Typography](#typography)
3. [Spacing and Layout](#spacing-and-layout)
4. [Components](#components)
5. [Best Practices](#best-practices)
6. [Effects and Animations](#effects-and-animations)
7. [Iconography](#iconography)
8. [Component Encapsulation](#component-encapsulation)
9. [Utilities](#utilities)
10. [Design Principles](#design-principles)

## Color Palette

### Primary Colors

Our primary color scheme is based on Material Design's Pink palette:

| Name | Hex | Usage |
|------|-----|-------|
| Primary 50 | `#FCE4EC` | Very light backgrounds, hover states in light mode |
| Primary 100 | `#F8BBD0` | Light backgrounds, subtle accents |
| Primary 200 | `#F48FB1` | Secondary buttons, accents |
| Primary 300 | `#F06292` | Highlights in dark mode, complementary color |
| Primary 400 | `#EC407A` | Lesser emphasis elements |
| Primary 500 | `#E91E63` | **Main brand color**, primary buttons |
| Primary 600 | `#D81B60` | Hover states for primary buttons |
| Primary 700 | `#C2185B` | Active states, focus rings, primary button border bottom |
| Primary 800 | `#AD1457` | Deep accents, specialized UI |
| Primary 900 | `#880E4F` | Very dark accents, specialized UI |

### Secondary Colors

Our secondary palette is based on Material Design's Deep Purple:

| Name | Hex | Usage |
|------|-----|-------|
| Secondary Light | `#B388FF` | Light accents, focus states for secondary elements |
| Secondary | `#7C4DFF` | Secondary actions, complementary elements |
| Secondary Dark | `#651FFF` | Hover states for secondary actions, secondary button border bottom |

### Theme Colors

#### Light Theme (Soft Pink)

| Name | Hex | Usage |
|------|-----|-------|
| Light Background | `#FDF7F9` | Main background with subtle pink tint |
| Light Background Paper | `#F8EEF2` | Cards, elevated surfaces, filled inputs |
| Light Background Hover | `#F1E6EB` | Hover states for light theme elements |
| Light Background Selected | `#E8DCE3` | Selected states for light theme elements |
| Light Card | `#FFFFFF` | Card backgrounds, dialogs (remained white for contrast) |
| Light Text Primary | `#212121` | Primary text |
| Light Text Secondary | `#757575` | Secondary, less important text |
| Light Divider | `#F1D8E2` | Borders, dividers with soft pink tint |

#### Dark Theme (Pink-Tinted)

| Name | Hex | Usage |
|------|-----|-------|
| Dark Background | `#1E151D` | Main background with subtle pink tint |
| Dark Background Paper | `#261A25` | Cards, elevated surfaces, filled inputs |
| Dark Background Hover | `#2E222D` | Hover states for dark theme elements |
| Dark Background Selected | `#372A36` | Selected states for dark theme elements |
| Dark Card | `#2E1F2D` | Card backgrounds, dialogs with pink tint |
| Dark Text Primary | `#FFFFFF` | Primary text |
| Dark Text Secondary | `#B0B0B0` | Secondary, less important text |
| Dark Divider | `#42353F` | Borders, dividers with pink tint |

### Back Button Specific Colors

These colors are defined for specific use with back buttons to ensure consistent styling.

| Name                                 | CSS Variable                           | Usage                                    |
|--------------------------------------|----------------------------------------|------------------------------------------|
| Light Theme Back Button Text         | `--color-light-back-button-text`       | Default text color in light mode         |
| Light Theme Back Button Text Hover   | `--color-light-back-button-text-hover` | Text color on hover in light mode        |
| Light Theme Back Button Bg Hover     | `--color-light-back-button-bg-hover`   | Background color on hover in light mode  |
| Dark Theme Back Button Text          | `--color-dark-back-button-text`        | Default text color in dark mode          |
| Dark Theme Back Button Text Hover    | `--color-dark-back-button-text-hover`  | Text color on hover in dark mode         |
| Dark Theme Back Button Bg Hover      | `--color-dark-back-button-bg-hover`    | Background color on hover in dark mode   |

### State Colors

#### Success (Green)

| Name | Hex | Usage |
|------|-----|-------|
| Success 50 | `#E8F5E9` | Very light success backgrounds, hover states |
| Success 100 | `#C8E6C9` | Light success backgrounds |
| Success 200 | `#A5D6A7` | Success accents |
| Success 300 | `#81C784` | Success highlights in dark mode |
| Success 400 | `#66BB6A` | Lesser emphasis success elements |
| Success 500 | `#4CAF50` | Main success color (as per Material palette) |
| Success 600 | `#43A047` | Hover states for success elements |
| Success 700 | `#388E3C` | Active states for success elements |
| Success 800 | `#2E7D32` | **Main success color (application default)**, Deep success accents |
| Success 900 | `#1B5E20` | Very dark success accents |

#### Warning (Orange)

| Name | Hex | Usage |
|------|-----|-------|
| Warning 50 | `#FFF3E0` | Very light warning backgrounds, hover states |
| Warning 100 | `#FFE0B2` | Light warning backgrounds |
| Warning 200 | `#FFCC80` | Warning accents |
| Warning 300 | `#FFB74D` | Warning highlights in dark mode |
| Warning 400 | `#FFA726` | Lesser emphasis warning elements |
| Warning 500 | `#FF9800` | Main warning color (as per Material palette) |
| Warning 600 | `#FB8C00` | Hover states for warning elements |
| Warning 700 | `#F57C00` | **Main warning color (application default)**, Active states for warning elements |
| Warning 800 | `#EF6C00` | Deep warning accents |
| Warning 900 | `#E65100` | Very dark warning accents |

#### Error (Red)

| Name | Hex | Usage |
|------|-----|-------|
| Error 50 | `#FFEBEE` | Very light error backgrounds, hover states |
| Error 100 | `#FFCDD2` | Light error backgrounds |
| Error 200 | `#EF9A9A` | Error accents |
| Error 300 | `#E57373` | Error highlights in dark mode |
| Error 400 | `#EF5350` | Lesser emphasis error elements |
| Error 500 | `#F44336` | Main error color (as per Material palette) |
| Error 600 | `#E53935` | Hover states for error elements |
| Error 700 | `#D32F2F` | **Main error color (application default)**, Active states for error elements |
| Error 800 | `#C62828` | Deep error accents |
| Error 900 | `#B71C1C` | Very dark error accents |

#### Info (Blue)

| Name | Hex | Usage |
|------|-----|-------|
| Info 50 | `#E3F2FD` | Very light info backgrounds, hover states |
| Info 100 | `#BBDEFB` | Light info backgrounds |
| Info 200 | `#90CAF9` | Info accents |
| Info 300 | `#64B5F6` | Info highlights in dark mode |
| Info 400 | `#42A5F5` | Lesser emphasis info elements |
| Info 500 | `#2196F3` | Main info color (as per Material palette) |
| Info 600 | `#1E88E5` | Hover states for info elements |
| Info 700 | `#1976D2` | **Main info color (application default)**, Active states for info elements |
| Info 800 | `#1565C0` | Deep info accents |
| Info 900 | `#0D47A1` | Very dark info accents |

### Alpha Colors

Colors with alpha transparency for overlays and subtle backgrounds.

| Name | RGBA | Usage |
|------|------|-------|
| Primary Alpha 10 | `rgba(233, 30, 99, 0.1)` | Subtle primary-tinted backgrounds, nav link hover |
| Primary Alpha 15 | `rgba(233, 30, 99, 0.15)` | Light primary-tinted backgrounds, nav link active |
| Primary Alpha 20 | `rgba(233, 30, 99, 0.2)` | Slightly more prominent primary-tinted backgrounds |
| Success Alpha 10 | `rgba(76, 175, 80, 0.1)` | Subtle success-tinted backgrounds |
| Success Alpha 15 | `rgba(76, 175, 80, 0.15)` | Light success-tinted backgrounds |
| Error Alpha 10 | `rgba(244, 67, 54, 0.1)` | Subtle error-tinted backgrounds |
| Error Alpha 15 | `rgba(244, 67, 54, 0.15)` | Light error-tinted backgrounds |
| Warning Alpha 10 | `rgba(255, 152, 0, 0.1)` | Subtle warning-tinted backgrounds |
| Warning Alpha 15 | `rgba(255, 152, 0, 0.15)` | Light warning-tinted backgrounds |
| Info Alpha 10 | `rgba(33, 150, 243, 0.1)` | Subtle info-tinted backgrounds |
| Info Alpha 15 | `rgba(33, 150, 243, 0.15)` | Light info-tinted backgrounds |

### Alert Background Colors

Specific background colors for alert components, ensuring good contrast and thematic consistency.

| Name | RGBA (Light) | RGBA (Dark) | Usage |
|------|--------------|-------------|-------|
| Alert Info Background | `rgba(25, 118, 210, 0.08)` | `rgba(25, 118, 210, 0.15)` | Background for info alerts |
| Alert Error Background | `rgba(211, 47, 47, 0.08)` | `rgba(211, 47, 47, 0.15)` | Background for error alerts |
| Alert Success Background | `rgba(46, 125, 50, 0.08)` | `rgba(46, 125, 50, 0.15)` | Background for success alerts |
| Alert Warning Background | `rgba(245, 124, 0, 0.08)` | `rgba(245, 124, 0, 0.15)` | Background for warning alerts |

## Typography

ZiStudy uses a combination of fonts to create hierarchy and visual interest:

### Font Families

- **Headers**: Poppins (semi-bold, medium)
- **Body**: Inter (regular, medium)

### Font Sizes

| Element | Size | Weight | Line Height |
|---------|------|--------|-------------|
| h1 | 2.5rem (40px) | 500 | 1.2 |
| h2 | 2rem (32px) | 500 | 1.25 |
| h3 | 1.5rem (24px) | 500 | 1.3 |
| h4 | 1.25rem (20px) | 500 | 1.35 |
| h5 | 1.125rem (18px) | 500 | 1.4 |
| h6 | 1rem (16px) | 500 | 1.5 |
| Body text | 1rem (16px) | 400 | 1.5 |
| Small text | 0.875rem (14px) | 400 | 1.5 |
| Caption | 0.75rem (12px) | 400 | 1.5 |
| Button text | 1rem (16px) | 500 | Normal |

## Spacing and Layout

ZiStudy follows a 4px grid system for spacing and layout. The following spacing increments are used throughout the application:

| Name | Size | Usage |
|------|------|-------|
| xs | 4px | Very small gaps |
| sm | 8px | Small gaps, internal padding |
| md | 16px | Standard spacing |
| lg | 24px | Larger separation between related elements |
| xl | 32px | Section spacing |
| 2xl | 48px | Major section divisions |
| 3xl | 64px | Page-level spacing |

### Container Widths

- **Small**: 640px
- **Medium**: 768px
- **Large**: 1024px
- **Extra Large**: 1280px
- **2XL**: 1536px
- **Max Content Width**: 1440px

## Components

### Class Name Handling

All components use the `cx` utility function for managing class names, which provides:
- Intelligent class merging based on conditions
- Automatic conflict resolution for Tailwind classes
- Support for conditional dynamic class application
- Better dark/light mode theme transitions

### Buttons

Buttons have been redesigned for better visual impact and interaction feedback:

#### Button Variants

- **Primary**: (`btn-primary`)
  - Background: Gradient from `Primary-500` to `Primary-600`.
  - Text: White.
  - Shadow: `shadow-lg`, dark mode: `dark:shadow-primary-500/20`.
  - Border: `border-bottom: 4px solid var(--color-primary-700)`.
  - Hover: Gradient from `Primary-600` to `Primary-700`.
  - Active: Gradient from `Primary-700` to `Primary-800`, `border-bottom-width: 2px`, `transform: translateY(2px)`.
  - Focus: `focus:ring-primary-300`.

- **Secondary**: (`btn-secondary`)
  - Background: Gradient from `Secondary` to `Secondary Dark`.
  - Text: White.
  - Shadow: `shadow-lg`, dark mode: `dark:shadow-secondary/20`.
  - Border: `border-bottom: 4px solid var(--color-secondary-dark)`.
  - Hover: Gradient from `Secondary Dark` to `Secondary Dark`.
  - Active: Gradient from `Secondary Dark` to `Secondary Dark`, `border-bottom-width: 2px`, `transform: translateY(2px)`.
  - Focus: `focus:ring-secondary-light`.

- **Outlined**: (`btn-outlined`)
  - Background: Transparent.
  - Text: `Primary` color. Dark: `Primary-300`.
  - Border: `2px solid var(--color-primary)`, `border-bottom-width: 4px`. Dark: `border-primary-300`.
  - Hover: Background `Primary-50`, border `Primary-600`, text `Primary-700`. Dark: `bg-[#331A2A]`, border `Primary-200`, text `Primary-200`.
  - Active: `border-bottom-width: 2px`, `transform: translateY(2px)`.
  - Focus: `focus:ring-primary-300`.

- **Text**: (`btn-text`)
  - Background: Transparent.
  - Text: `Primary` color. Dark: `Primary-300`.
  - Hover: Background `Primary-50`. Dark: `bg-[#331A2A]`.
  - Focus: `focus:ring-primary-300`.

#### Button Properties

- **Base Styling**: `.btn` class: `inline-flex items-center justify-center rounded-lg px-6 py-3 text-base font-medium`.
- **Font**: Inter, 500 weight, 1rem (16px), letter-spacing `0.01em`.
- **Padding**: Default: `px-6` (24px horizontal), `py-3` (12px vertical).
- **Border Radius**: `rounded-lg` (0.5rem / 8px).
- **Transitions**: `transition-all duration-300 ease-in-out`.
- **Focus**: `focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-transparent`.
- **Active State**: Base active transform `translateY(1px) scale(0.99)`. Specific variants have further active state changes.
- **Icons**: Optional start and end icons with proper spacing (handled by component logic).
- **ARIA Support**: Built-in accessibility attributes (handled by component logic).
- **Ripple Effect**: Applied via JS, visual style `ripple-effect` class.

#### Button States

- **Hover**: Detailed in variant descriptions. Generally involves background/gradient shifts.
- **Active**: Detailed in variant descriptions. Often involves `transform: translateY(2px)` and reduced border-bottom.
- **Focus**: Uses `focus:ring-*` utilities, color-matched to variant.
- **Disabled**: `opacity-50 cursor-not-allowed` (applied by component logic).
- **Loading**: Spinner animation, opacity reduction, disabled interaction (handled by component logic).

### Cards

Cards are used to group related content and actions. They feature consistent styling and thoughtful spacing:

#### Card Design

- **Standard Card**: (`.card`)
  - Background: `var(--color-light-card)` or `var(--color-dark-card)`.
  - Shadow: `var(--shadow-3)` by default.
  - Border: `2px solid var(--color-light-divider)` or `var(--color-dark-divider)`.
  - Rounded corners: `rounded-lg` (0.5rem/8px).
  - Font size: `1.05rem`, line height `1.6`.
  - Hover: `box-shadow: var(--shadow-4)`, `border-color: var(--color-primary-300)` (light) / `var(--color-primary-400)` (dark).
  - Transitions: `transition-all duration-300 ease-in-out`.

- **Card Header**: (`.card-header`)
  - Background: Gradient from `var(--color-light-card)` to `#F9E4EC` (light), or `var(--color-dark-card)/90` to `#3A1F38` (dark).
  - Padding: `px-6 py-4`.
  - Border: Bottom border `var(--color-light-divider)` or `var(--color-dark-divider)`.
  - Title (`h3`): `text-xl font-medium tracking-tight`.

- **Card Content**: (`.card-body`)
  - Padding: `px-6 py-5`.
  - Clean, well-spaced layout.

- **Card Footer**: (`.card-footer`)
  - Padding: `px-6 py-4`.
  - Border: Top border `var(--color-light-divider)` or `var(--color-dark-divider)`.
  - Flexible alignment options for actions (:start, :center, :end, :between) handled by component logic using flex utilities.

#### Card Interactions

- **Hover Effects**:
  - Subtle elevation increase
  - Border color enhancement to primary color
  - Smooth transition (300ms)
  - Optional scale transform with slight upward movement
  - Shadow expansion

- **Dark Mode**:
  - Rich, pink-tinted dark backgrounds
  - Preserved readability with optimized contrast
  - Consistent interaction patterns
  - Border color shifts to match dark theme

#### Card Media

- Support for images with proper aspect ratio handling
- Options for rounded corners on top, bottom, or both
- Customizable height
- Proper alt text support for accessibility

### Form Controls

#### Text Input

- **Base Style**: (`.input`)
  - Dimensions: `w-full px-4 py-2 rounded-md`.
  - Border: `2px solid var(--color-light-divider)`, `border-bottom-width: 3px`.
  - Background: `var(--color-light-bg)`. Dark: `var(--color-dark-bg)`.
  - Text Color: `var(--color-light-text-primary)`. Dark: `var(--color-dark-text-primary)`.
  - Font size: `1rem`.
  - Transitions: `transition-all duration-200`.
- **Focus State**:
  - Border: `2px solid var(--color-primary-400)`, `border-bottom: 3px solid var(--color-primary-500)`. Dark: `var(--color-primary-600)` and `var(--color-primary-500)` respectively.
  - Background: Remains `var(--color-light-bg)` or `var(--color-dark-bg)`.
  - Shadow: `box-shadow: 0 0 0 2px rgba(233, 30, 99, 0.15)`. Dark: `box-shadow: 0 0 0 2px var(--color-primary-50)`.
- **States**: Normal, focus, disabled, error, autofill are handled by component logic and utility classes.
- **Variants**: Outlined, filled (as described in `input.cljs`, CSS mainly shows one base style with variations for focus/error).
- **Enhanced Validation**: Clear visual feedback for validation states, typically changing border colors to `var(--color-error)`.

##### Autofill Handling

Form inputs have special styling to handle browser autofill functionality using Tailwind's autofill utilities:
- Maintains dark mode background when browser autofills inputs
- Explicitly sets text color to white in dark mode to ensure visibility
- Ensures cursor color remains visible in autofilled fields
- Applies consistent border styling with the rest of the design system
- Handles combined states (autofill + focus, autofill + active)
- Prevents the default yellow background in both light and dark modes
- Ensures text is immediately visible without requiring user interaction
- Uses Tailwind's autofill modifiers for consistent styling across browsers
- Uses direct text-white class for dark mode to override browser defaults

#### Selection Controls

- **Checkbox**: (`.checkbox-base`)
  - `appearance-none rounded border-2 bg-light-card dark:bg-dark-card border-light-divider dark:border-dark-divider`.
  - Checked: `bg-primary-600 border-primary-600 dark:bg-primary-500 dark:border-primary-500`, with SVG background image for checkmark.
  - Focus: `ring-1 ring-offset-1 ring-primary-300 dark:ring-primary-700`.
  - Custom check mark animation (implied by SVG and transition).

- **Radio button**: (`.radio-base`)
  - `appearance-none rounded-full border-2 bg-light-card dark:bg-dark-card border-light-divider dark:border-dark-divider`.
  - Dimensions: `w-1.25rem h-1.25rem`.
  - Checked: `border-primary-600 dark:border-primary-400`, with an `::after` pseudo-element for the inner dot (`bg-white`, `w-0.5rem h-0.5rem`).
  - Focus: `ring-2 ring-offset-2 ring-primary-300 dark:ring-primary-700`.

- **Switch**: See [Toggle Switch](#toggle-switch) section below for detailed redesign.

- **Dropdown**: For selecting from a list

#### Dropdown Menu

Dropdowns have been redesigned for a consistent and elegant experience:

##### Visual Design

- **Menu Container**: (`.dropdown-menu`)
  - Background: `bg-white dark:bg-dark-card`.
  - Border: `rounded-md border border-solid border-light-divider dark:border-dark-divider`.
  - Shadow: `shadow-lg`.
  - Minimum width: `min-w-[180px]`.
  - Z-index: `z-50`.

- **Menu Items**:
  - Consistent padding (16px horizontal, 8px vertical)
  - Standard text size (14px/0.875rem)
  - Smooth hover transitions (150ms)
  - Clear visual feedback states
  - Support for icons at start and end positions
  - Optional description text

##### Item States

- **Default**: Clean text on white background
- **Selected**: 
  - Light pink background with deeper pink text
  - Medium font weight for emphasis
  - Dark mode: Semi-transparent pink background with lighter pink text
  - Optional checkmark icon

- **Disabled**:
  - Reduced opacity (50%)
  - Cursor indication (not-allowed)
  - Secondary text color
  - No hover effects

- **Danger**:
  - Error red text color
  - Hover state with light red background
  - Dark mode: Lighter red text with semi-transparent hover
  - Warning icon option

##### Implementation

- Use the `dropdown` component with consistent width handling
- Customize with options like `width: :match-trigger` for consistent sizing
- Support for start and end icons in menu items
- Optional multi-select mode with clear/apply actions
- Fixed positioning with intelligent placement around trigger elements
- Support for nested menus
- Keyboard navigation support (arrows, escape, enter)

### Modal/Dialog

Modals provide focused content and interaction without leaving the current page:

#### Visual Design
- Clean white background in light mode
- Dark themed background in dark mode
- Subtle rounded corners (12px)
- Elegant shadow for elevation
- Proper spacing for content (32px padding)
- Optional backdrop blur effect
- Customizable max-width settings

#### Animation
- Smooth entrance animation (scale + fade)
- Quick exit animation
- Backdrop fade transition
- Spring physics for natural feel

#### Structure
- **Header**: Title with optional close button
- **Body**: Main content area with scrolling if needed
- **Footer**: Action buttons with consistent alignment
- **Overlay**: Semi-transparent backdrop that blocks interaction

#### Accessibility
- Focus trap inside modal
- ESC key to close
- ARIA attributes (role="dialog", aria-modal="true")
- Focus returns to trigger element when closed
- Prevents background scrolling

### Tooltip

Tooltips provide additional information on hover:

#### Visual Characteristics
- Rich dark background with slight transparency
- Subtle rounded corners (6px)
- Proper padding (8px 12px)
- Small arrow pointing to the trigger
- Maximum width for readability (250px)
- Text wrapping for longer content

#### Positioning
- Intelligent placement based on available space
- 8 possible positions (top, bottom, left, right, and corners)
- Adjusts to stay within viewport
- Consistent distance from trigger (8px)

#### Behavior
- Appears on hover or focus
- Slight delay before showing (150ms)
- Fade-in animation
- Disappears when cursor leaves
- Remains visible when hovering the tooltip itself

#### Implementation
- Simple implementation with the `tooltip` component
- Content can be plain text or rich HTML
- Optional custom styling
- Control over delay timing

## Effects and Animations

### Ripple Effect

Material-style ripple effect provides tactile feedback for interactive elements:

- Applied to buttons, cards, and other clickable elements using JavaScript (`add-ripple-effect`).
- Visual style: `.ripple-effect` class for the animated span.
  - `position: absolute`, `border-radius: 50%`, `background-color: rgba(255, 255, 255, 0.5)` (dark: `bg-primary-50`).
  - Animation: `ripple 0.6s var(--animation-standard)` (scales up and fades out).
- Works for nested elements (propagates to parent via JS).

### Micro-interactions

Subtle animations that provide feedback and enhance the user experience:

- Button hover/press states with scale transforms (95%-105%)
- Card hover effects with subtle elevation changes
- Input focus animations with smooth border transitions
- Success/error state animations
- Loading indicators and progress animations
- Dropdown and modal entrance/exit animations

### Transitions

Standard timing functions for smooth UI transitions, defined as CSS custom properties:

- **Standard**: `var(--animation-standard)` which is `cubic-bezier(0.4, 0, 0.2, 1)` - for most transitions (300ms default in components).
- **Enter**: `var(--animation-enter)` which is `cubic-bezier(0, 0, 0.2, 1)` - for elements entering the screen (225ms).
- **Exit**: `var(--animation-exit)` which is `cubic-bezier(0.4, 0, 1, 1)` - for elements leaving the screen (195ms).

### Theme Switching

Smooth transition between light and dark themes:

- Background, text, and surface transitions last 300ms
- Icon rotation effect during theme switching (360 degrees)
- Subtle scale effect on theme icon (1.2x)
- Consistent ripple effect across themes
- Color shifts use the standard timing function

### Added Animation Classes

The following animation utility classes are available:

- `.animate-in`: Base class for animations (default 200ms duration, `cubic-bezier(0.4, 0, 0.2, 1)` timing).
- `.fade-in`: Simple opacity transition from 0 to 1.
- `.zoom-in-90`: Scale and opacity transition starting at 90% scale and 0 opacity.
- `.animate-slide-down`: Elements sliding down into view (`@keyframes slideDown`).
- `.animate-slide-in`: Elements sliding in from left (`@keyframes slideIn`, typically for width transitions).
- `.animate-pulse-slow`: Slow pulsing effect (`@keyframes pulse`, 3s duration).
- `.scale-95`, `.scale-100`: Transform scale utilities.
- `.cloze-blank-animated`: Pop animation for cloze question blanks (`@keyframes pop`).
- **Flash Message Animations** (from `flash-animations.css`):
  - `slide-in-right`, `slide-in-left`, `slide-in-top`, `slide-in-bottom`
  - `slide-out-right`, `slide-out-left`, `slide-out-top`
  - `.animate-enter`: Base class for flash message entry.

## Best Practices

### Accessibility

- Maintain a minimum contrast ratio of 4.5:1 for normal text and 3:1 for large text
- Ensure all interactive elements are keyboard accessible
- Provide text alternatives for non-text content
- Design focus states that are clearly visible
- Use ARIA attributes when necessary
- Support screen readers through proper semantic markup
- Test with keyboard-only navigation

### Responsive Design

- Use fluid layouts that adapt to different screen sizes
- Design mobile-first, then enhance for larger screens
- Use appropriate component sizing for touch targets (minimum 44×44px)
- Test layouts at multiple breakpoints
- Use appropriate font sizes across devices
- Implement responsive spacing using the spacing system

### Visual Hierarchy

- Use size, color, and spacing to create clear hierarchy
- Maintain consistent alignment throughout the interface
- Group related elements visually
- Limit the number of visual weights on a single screen
- Use typography scale to establish content importance
- Apply animations selectively to guide attention

### Color Usage

- Use the primary color for main actions and key elements
- Apply accent colors sparingly for emphasis
- Rely on neutral colors for most UI elements
- Ensure color is not the only means of conveying information
- Dark theme uses pink-tinted background for brand consistency
- Light theme uses soft pink-tinted background for a calm, modern feel
- Use CSS custom properties for consistent color application

### Dark Mode

- Apply appropriate color adjustments for dark mode
- Test all components in both light and dark themes
- Ensure adequate contrast in both modes
- Use proper hex codes instead of rgba() for dark mode backgrounds
- Use custom properties with proper fallbacks
- Consider reducing visual intensity of certain elements in dark mode

## Iconography

ZiStudy uses **Lucide Icons** for all iconography. Lucide provides a comprehensive, consistent set of modern, clean icons that align perfectly with our design aesthetic. All icons should use the ZiIcon component which wraps Lucide icons.

### Usage Guidelines
- All icons should use the appropriate icon from the Lucide library
- Standard size classes should be used (e.g., `text-sm`, `