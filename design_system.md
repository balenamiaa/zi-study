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
9. [Design Principles](#design-principles)

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
| Primary 700 | `#C2185B` | Active states, focus rings |
| Primary 800 | `#AD1457` | Deep accents, specialized UI |
| Primary 900 | `#880E4F` | Very dark accents, specialized UI |

### Secondary Colors

Our secondary palette is based on Material Design's Deep Purple:

| Name | Hex | Usage |
|------|-----|-------|
| Secondary Light | `#B388FF` | Light accents, focus states |
| Secondary | `#7C4DFF` | Secondary actions, complementary elements |
| Secondary Dark | `#651FFF` | Hover states for secondary actions |

### Theme Colors

#### Light Theme (Soft Pink)

| Name | Hex | Usage |
|------|-----|-------|
| Light Background | `#FDF7F9` | Main background with subtle pink tint |
| Light Background Paper | `#F8EEF2` | Cards, elevated surfaces with light pink tint |
| Light Card | `#FFFFFF` | Card backgrounds, dialogs (remained white for contrast) |
| Light Text Primary | `#212121` | Primary text |
| Light Text Secondary | `#757575` | Secondary, less important text |
| Light Divider | `#F1D8E2` | Borders, dividers with soft pink tint |

#### Dark Theme (Pink-Tinted)

| Name | Hex | Usage |
|------|-----|-------|
| Dark Background | `#1E151D` | Main background with subtle pink tint |
| Dark Background Paper | `#261A25` | Cards, elevated surfaces with pink tint |
| Dark Card | `#2E1F2D` | Card backgrounds, dialogs with pink tint |
| Dark Text Primary | `#FFFFFF` | Primary text |
| Dark Text Secondary | `#B0B0B0` | Secondary, less important text |
| Dark Divider | `#42353F` | Borders, dividers with pink tint |

### State Colors

#### Success (Green)

| Name | Hex | Usage |
|------|-----|-------|
| Success 50 | `#E8F5E9` | Very light success backgrounds, hover states |
| Success 100 | `#C8E6C9` | Light success backgrounds |
| Success 200 | `#A5D6A7` | Success accents |
| Success 300 | `#81C784` | Success highlights in dark mode |
| Success 400 | `#66BB6A` | Lesser emphasis success elements |
| Success 500 | `#4CAF50` | **Main success color** |
| Success 600 | `#43A047` | Hover states for success elements |
| Success 700 | `#388E3C` | Active states for success elements |
| Success 800 | `#2E7D32` | Deep success accents |
| Success 900 | `#1B5E20` | Very dark success accents |

#### Warning (Orange)

| Name | Hex | Usage |
|------|-----|-------|
| Warning 50 | `#FFF3E0` | Very light warning backgrounds, hover states |
| Warning 100 | `#FFE0B2` | Light warning backgrounds |
| Warning 200 | `#FFCC80` | Warning accents |
| Warning 300 | `#FFB74D` | Warning highlights in dark mode |
| Warning 400 | `#FFA726` | Lesser emphasis warning elements |
| Warning 500 | `#FF9800` | **Main warning color** |
| Warning 600 | `#FB8C00` | Hover states for warning elements |
| Warning 700 | `#F57C00` | Active states for warning elements |
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
| Error 500 | `#F44336` | **Main error color** |
| Error 600 | `#E53935` | Hover states for error elements |
| Error 700 | `#D32F2F` | Active states for error elements |
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
| Info 500 | `#2196F3` | **Main info color** |
| Info 600 | `#1E88E5` | Hover states for info elements |
| Info 700 | `#1976D2` | Active states for info elements |
| Info 800 | `#1565C0` | Deep info accents |
| Info 900 | `#0D47A1` | Very dark info accents |

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
| Button text | 0.875rem (14px) | 500 | 1.5 |

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

### Buttons

Buttons have been redesigned for better visual impact and interaction feedback:

#### Button Variants

- **Primary**:
  - Beautiful gradient background
  - Enhanced shadow with color tint
  - Scale transform on press
  - Smooth hover transition

- **Secondary**:
  - Deep purple gradient
  - Consistent interaction patterns
  - Complementary to primary actions

- **Outlined**:
  - 2px borders for better visibility
  - Subtle hover background
  - Maintains consistent height with other variants
  - Color-matched focus rings

- **Text**:
  - Clean, minimal design
  - Hover background effect
  - Maintains consistent padding
  - Clear active state

#### Button Properties

- **Size**: Larger default size for better touch targets
- **Padding**: Horizontal: 1.5rem (24px), Vertical: 0.75rem (12px)
- **Border Radius**: 0.5rem (8px)
- **Font**: Inter Medium, 1rem (16px)
- **Transitions**: 300ms with spring easing
- **Icons**: Optional start and end icons with proper spacing

#### Button States

- **Hover**:
  - Gradient shift
  - Scale transform (1.02)
  - Shadow enhancement

- **Active**:
  - Scale down (0.99)
  - Darker gradient
  - Maintains shadow

- **Focus**:
  - Visible ring with offset
  - Color-matched to variant
  - High contrast in dark mode

- **Disabled**:
  - Reduced opacity
  - Removed hover effects
  - Maintained visual structure

### Cards

Cards are used to group related content and actions. They feature beautiful gradients, smooth transitions, and thoughtful spacing:

#### Card Variants

- **Standard Card**: 
  - Elegant shadow with subtle pink-tinted border
  - Smooth hover transition with enhanced elevation
  - Increased font size (1.05rem) for better readability
  - Generous padding (2rem) for content breathing room

- **Card Header**:
  - Gradient background from white to soft pink (dark mode: dark gradient with pink tint)
  - Beautiful text gradient on titles
  - Left accent border option for visual hierarchy
  - Optional icon with soft background

- **Card Content**:
  - Clean, well-spaced layout
  - Optimized line height (1.6) for readability
  - Consistent padding across all sections

- **Card Footer**:
  - Subtle gradient background
  - Clear separation with refined border
  - Flexible alignment options for actions

#### Card Interactions

- **Hover Effects**:
  - Subtle elevation increase
  - Border color enhancement
  - Smooth transition (300ms)
  - Optional scale transform

- **Dark Mode**:
  - Rich, pink-tinted dark backgrounds
  - Preserved readability with optimized contrast
  - Consistent interaction patterns

### Form Controls

#### Text Input

- **Standard**: With label, optional helper text
- **With icons**: Leading or trailing icons
- **States**: Normal, focus, disabled, error, autofill
- **Variants**: Outlined, filled

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

- **Checkbox**: For multi-select options
- **Radio button**: For single-select options
- **Switch**: For binary on/off states
- **Dropdown**: For selecting from a list

#### Dropdown Menu

Dropdowns have been redesigned for a consistent and elegant experience:

##### Visual Design

- **Menu Container**:
  - Clean white background (dark: themed dark card background)
  - Subtle border with rounded corners (8px)
  - Elegant shadow for elevation
  - Minimum width of 180px for readable content
  - z-index priority to appear above other content

- **Menu Items**:
  - Consistent padding (16px horizontal, 8px vertical)
  - Standard text size (14px/0.875rem)
  - Smooth hover transitions (150ms)
  - Clear visual feedback states

##### Item States

- **Default**: Clean text on white background
- **Selected**: 
  - Light pink background with deeper pink text
  - Medium font weight for emphasis
  - Dark mode: Semi-transparent pink background with lighter pink text

- **Disabled**:
  - Reduced opacity (50%)
  - Cursor indication (not-allowed)
  - Secondary text color

- **Danger**:
  - Error red text color
  - Hover state with light red background
  - Dark mode: Lighter red text with semi-transparent hover

##### Implementation

- Use the `dropdown` component with consistent width handling
- Customize with options like `width: :match-trigger` for consistent sizing
- Support for start and end icons in menu items
- Optional multi-select mode with clear/apply actions
- Fixed positioning with intelligent placement around trigger elements

## Effects and Animations

### Ripple Effect

Material-style ripple effect provides tactile feedback for interactive elements:

- Applied to buttons, cards, and other clickable elements
- Created using the `.ripple` class
- Works for nested elements (propagates to parent)
- Subtle but effective visual cue for user interactions

### Micro-interactions

Subtle animations that provide feedback and enhance the user experience:

- Button hover/press states with scale transforms (95%-105%)
- Card hover effects with subtle elevation changes
- Input focus animations with smooth border transitions
- Success/error state animations
- Loading indicators and progress animations

### Transitions

Standard timing functions for smooth UI transitions:

- **Standard**: cubic-bezier(0.4, 0, 0.2, 1) - for most transitions (300ms)
- **Enter**: cubic-bezier(0, 0, 0.2, 1) - for elements entering the screen (225ms)
- **Exit**: cubic-bezier(0.4, 0, 1, 1) - for elements leaving the screen (195ms)
- **Emphasis**: cubic-bezier(0.4, 0, 0.6, 1) - for attention-grabbing elements (500ms)

### Theme Switching

Smooth transition between light and dark themes:

- Background, text, and surface transitions last 300ms
- Icon rotation effect during theme switching (360 degrees)
- Subtle scale effect on theme icon (1.2x)
- Consistent ripple effect across themes
- Color shifts use the standard timing function

## Best Practices

### Accessibility

- Maintain a minimum contrast ratio of 4.5:1 for normal text and 3:1 for large text
- Ensure all interactive elements are keyboard accessible
- Provide text alternatives for non-text content
- Design focus states that are clearly visible

### Responsive Design

- Use fluid layouts that adapt to different screen sizes
- Design mobile-first, then enhance for larger screens
- Use appropriate component sizing for touch targets (minimum 44×44px)
- Test layouts at multiple breakpoints

### Visual Hierarchy

- Use size, color, and spacing to create clear hierarchy
- Maintain consistent alignment throughout the interface
- Group related elements visually
- Limit the number of visual weights on a single screen

### Color Usage

- Use the primary color for main actions and key elements
- Apply accent colors sparingly for emphasis
- Rely on neutral colors for most UI elements
- Ensure color is not the only means of conveying information
- Dark theme uses pink-tinted background for brand consistency
- Light theme uses soft pink-tinted background for a calm, modern feel

## Iconography

ZiStudy uses **Lucide Icons** for all iconography. Lucide provides a comprehensive, consistent set of modern, clean icons that align perfectly with our design aesthetic. All icons should use the ZiIcon component which wraps Lucide icons.

### Usage Guidelines
- All icons should use the `ZiIcon` component with the appropriate icon from the `Icons` enum.
- Standard size classes should be used (e.g., `text-sm`, `text-2xl`, etc.).
- Color classes should follow our color system (e.g., `text-primary dark:text-primary-300`).
- Icons should have appropriate aria-labels for accessibility.

### Icon Styling
- Use consistent sizing within context (navigation, buttons, etc.)
- Apply subtle animations for interactive icons (hover, active states)
- Maintain adequate spacing around icons (minimum 4px)
- Use the same weight/style throughout the application

#### Common Icons (Lucide)
| Usage | Icon Enum |
|-------|----------|
| User | Icons.User |
| Home | Icons.House |
| Study | Icons.Book |
| Notes | Icons.FileText |
| Security | Icons.Shield |
| Settings | Icons.Cog |
| Logout | Icons.Logout |
| Login | Icons.Login |
| Register | Icons.UserPlus |
| Check | Icons.CheckCircle |
| Menu | Icons.Menu |
| Close | Icons.X |
| Dropdown | Icons.ChevronDown |
| Email | Icons.Email |
| Lightning | Icons.Zap |
| Book | Icons.BookOpen |
| Message | Icons.MessageSquare |
| Sparkle | Icons.Sparkles |

## Component Encapsulation

All basic UI elements (such as **inputs**, **buttons**, **selects**, etc.) must be encapsulated as reusable components. This ensures consistency, maintainability, and adherence to the design system. Each component should:
- Encapsulate all styling, icon usage, and state logic.
- Accept parameters for customization (e.g., label, icon, error state, etc.).
- Use FontAwesome for any iconography.
- Follow the patterns shown in the `Input.kt` example (see `src/webMain/kotlin/zi_study/components/Input.kt`).
- Be placed in the `components` package/directory.

This approach applies to:
- Buttons (primary, secondary, outlined, text, etc.)
- Inputs (text, password, email, etc.)
- Selects and dropdowns
- Cards
- Any other reusable UI element

---

## Design Principles

### Clarity
- Content is presented clearly and efficiently
- Visual elements support rather than distract from content
- Information hierarchy guides users through the interface
- Typography enhances readability across all screen sizes

### Consistency
- UI elements behave predictably throughout the application
- Visual language remains consistent across all screens
- Spacing, sizing, and positioning follow established patterns
- Color usage adheres to the defined palette and meanings

### Feedback
- Interactive elements provide clear visual feedback
- System status is always visible and understandable
- Errors are presented constructively with solutions
- Animations and transitions provide context for state changes

### Efficiency
- Common tasks can be completed with minimal steps
- UI is optimized for both novice and expert users
- Layout adapts intelligently to different screen sizes
- Performance is prioritized for a smooth experience

### Delight
- Micro-interactions add moments of delight
- Animations are purposeful and enhance usability
- Visual design is polished and professional
- The overall experience feels cohesive and thoughtful

This design system is a living document and will evolve as ZiStudy grows and develops.

### Toggle Switch

The toggle switch has been completely redesigned for a more modern and engaging experience:

#### Visual Design

- **Track**:
  - Width: 3.5rem (56px)
  - Height: 1.75rem (28px)
  - Rounded pill shape
  - Beautiful gradient when active
  - Subtle background when inactive

- **Thumb**:
  - Size: 1.25rem (20px)
  - Pure white circle
  - Elevated with shadow
  - Glow effect when active
  - Spring animation on toggle

#### Interactions

- **Toggle Animation**:
  - Smooth slide transition (300ms)
  - Spring effect on the thumb
  - Scale animation on hover
  - Glow effect when active

- **States**:
  - Unchecked: Subtle gray track
  - Checked: Gradient from primary colors
  - Hover: Thumb scale (1.05)
  - Active: Enhanced glow effect
  - Disabled: Reduced opacity

#### Accessibility

- **Keyboard Navigation**:
  - Clear focus indicators
  - Spacebar toggle support
  - ARIA attributes
  - Role="switch"

- **Labels**:
  - Optional left or right position
  - Clear connection to toggle
  - Proper spacing and alignment
  - Clickable area includes label