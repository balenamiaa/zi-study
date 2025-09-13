# UI Components (UIx + DaisyUI 5)

This project ships a lightweight, symmetric, and theme-aware component set built with UIx and DaisyUI 5. Components favor clean Tailwind classes with minimal wrappers, and expose consistent props for size, width, and status.

## Quick Start

- Require components individually, e.g. `frontend.ui.button`, `frontend.ui.field`, `frontend.ui.card`, `frontend.ui.avatar`, `frontend.ui.feedback`.
- Global toasts are wired in `frontend.layouts.main-layout`: `($ Toasts {:portal? true :position :top-right})`.
- Dispatch toasts anywhere: `(frontend.ui.feedback/toast! {:text "Saved!" :variant :success})`.

## Common Props

- Sizes: `:xs :sm :md :lg :xl` for buttons; `:sm :md :lg` for inputs.
- Width: pass `:full? true` to make inputs/buttons full width (`w-full`).
- Validation: inputs accept `:invalid?` and `:valid?` to tint borders/focus.

## Buttons — `frontend.ui.button`

- `Button` — props: `:variant :primary|:secondary|:accent|:ghost|:outline|:link|:neutral|:success|:warning|:error`, `:size :xs|:sm|:md|:lg|:xl`, `:full?`, `:icon`, `:loading?`.
- `IconButton` — square button for icons; `:variant`, `:size` supported.

## Fields — `frontend.ui.field`

- `FieldWrapper` — `{ :id :label :hint :error }` wrapper that renders label and hint/error consistently.
- `TextInput` — `{ :id :size :start-icon :end-icon :invalid? :valid? :full? }`.
- `PasswordInput` — `{ :show? :toggle! ...TextInput-props }`, adds show/hide button with lucide `Eye`/`EyeOff`.
- `TextArea` — `{ :size :invalid? :valid? :full? }`.
- `Select` — `{ :size :invalid? :valid? :full? }` plus children `option`s.
- `Checkbox` — `{ :label :checked :on-change }`.
- `RadioGroup` — `{ :name :value :on-change :options [{:label :val} ...] }`.

## Display — `frontend.ui.card`, `frontend.ui.avatar`

- `Card` — `{ :compact? :hover? :glass? }`; contains children. Use `CardHeader` and `CardFooter` for structure.
- `Avatar` — `{ :src :name :email :size :xs|:sm|:md|:lg|:xl }`. If `:src` is absent, renders initials from `:name` or falls back to deriving from `:email`.

## Feedback — `frontend.ui.feedback`

Toasts are the single feedback primitive (alerts/flash consolidated into toasts).

- Mount once near the app root (already integrated in main layout):

  `($ frontend.ui.feedback/Toasts {:portal? true :position :top-right})`

- Show a toast anywhere:

  `(frontend.ui.feedback/toast! {:text "Updated" :variant :success})`

- API
  - `toast!` — accepts keys:
    - `:text` — message string (required)
    - `:variant` — one of `:info :success :warning :error` (tints)
    - `:timeout-ms` — milliseconds to auto-dismiss (default: 3500; set 0 to disable)
    - `:id` — optional id (auto-generated)
    - `:position` — optional per-toast position, one of:
      `:top-left :top-center :top-right :bottom-left :bottom-center :bottom-right`.
  - `Toasts` — props:
    - `:portal?` — when true (default), uses a DOM portal into `document.body`.
    - `:position` — keyword or collection of positions to render. Default `:top-right`.

## Navigation — `frontend.ui.link`
- `Link` — route-aware anchor. Props: `:route :params :query` or plain `:href`.

## Overlay — `frontend.ui.portal`
- `Portal` — portals children into `document.body` (or a provided target) and manages mount/unmount.

## Menus — `frontend.ui.dropdown`
- `Dropdown` — DaisyUI dropdown wrapper.
  - Props: `:trigger` (required), `:align :start|:end`, `:items` or `:children`, `:menu-class`, `:class`.
  - `:items` accepts a vector of maps: `{:label :icon :href :on-click :key}`; if `:href` is nil, it renders a clickable `a` tag that prevents default and calls `:on-click`.
  - If `:items` is omitted, `:children` is rendered as the menu.

## Styling Utilities (CSS)

- `.glass-panel` — subtle backdrop blur + border, pairs well with cards/toasts.
- `.glow-primary` — soft primary glow ring.
- `.ring-gradient` — conic gradient ring, for decorative accents.
- `.toast-item` — consistent surface for toast rows.

These live in `resources/public/css/main.css` and are theme-aware via DaisyUI variables.

## Notes

- Inputs intentionally use a boolean `:full?` instead of a size enum for width — this is more expressive alongside the size scale.
- The Toasts layer defaults to portal rendering to avoid clipping/stacking issues; disable with `{:portal? false}` if necessary.
