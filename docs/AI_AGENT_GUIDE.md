# Frontend Quick Guide (for AI/LLM Agents)

## Stack & Entry Points
- React via UIx: `uix.core`, root in `src/cljs/frontend/core.cljs`.
- State: re-frame (`re-frame.core`) with subs/events and a central `app-db`.
- Router: Reitit frontend (`reitit.frontend.easy`), routes in `src/cljs/frontend/routes.cljs`.
- HTTP: custom `::fetch` effect wrapping Fetch API (`src/cljs/frontend/http_fx.cljs`).
- CSS/UI: Tailwind + DaisyUI, theme via `data-theme` attribute.

## Rendering & UIx
- Root: `uix.dom/create-root` + `uix.dom/render-root ($ app) root`.
- UI elements: `($ :div {:class "..."} children...)`.
- React component interop: call components directly, e.g. `($ Button {:on-click ...})`.
- Lucide icons: import named icons and call, e.g. `($ Home {:size 20})`.

## re-frame State Model
- `app-db`: single map holding UI, route and domain data.
- Subscriptions:
  - Register with `rf/reg-sub`. Example: `::state/current-route`, `::state/ui-state`.
  - Derived subs can depend on other subs using `:<-`.
  - Use inside UIx via `frontend.uix.hooks/use-subscribe`, e.g. `(use-subscribe [::s/todos])`.
- Events:
  - Pure DB updates: `rf/reg-event-db` (e.g. `::set-current-route`, `::set-theme`).
  - Effects: `rf/reg-event-fx` returns effects map, e.g. HTTP call via `::http/fetch`.
- Effects:
  - Register custom effects with `rf/reg-fx`. We use `::fetch` in `http_fx.cljs`.

## HTTP Fetch Flow (Todos example)
1. UI dispatches `::handlers/get-todos`.
2. Event returns effects:
   - `[:dispatch [:http/init [:todos]]]` → sets `:http [:todos] :status` to `:initial-loading`/`:loading`.
   - `[::http/fetch {...}]` → triggers Fetch with Transit/JSON handling.
3. On success/failure:
   - `:http/success` or `:http/failure` updates `:http [:todos]` with `:resp` or `:error` and `:status`.
4. Derived sub:
   - `:http/body` pulls `:body` from `:http` path.
   - `::subs/todos` sorts by `:id` for rendering.

## Routing
- Routes carry `:layout` and `:view` in `:data` (`src/cljs/frontend/routes.cljs`).
- `frontend.core` stores current match in `app-db` under `:current-route` on navigation.
- `app` component renders `($ layout {:current-route ...} ($ view match))`.

## Theming
- Theme state in `[:ui :theme]` (`:system` | `:light` | `:dark`).
- `frontend.utilities.theme`:
  - `apply-theme` sets `<html data-theme>` to DaisyUI theme names (`gold_dark` / `gold_light`).
  - `set-theme` updates re-frame state, applies theme, persists to `localStorage` and a `theme` cookie (used server-side).
  - `initialize-theme` reads saved theme and wires system preference listener.
- Server boot:
  - `backend.routes/index` reads `theme` cookie and sets `data-theme` before CSS to avoid FOUC; small inline script covers `system`.

## Icons & JS Interop
- Import named icons: `(:require ["lucide-react" :refer [Home Info ...]])`.
- Use directly: `($ Home {:size 20})`. For dynamic component values, call the value directly: `($ icon {:size 20})`.
- For React components, UIx maps `:class` → `className`; nested JS objects must be passed as JS (except `:style`, which is auto-converted).

## Development
- Shadow-cljs build: `:app` in `shadow-cljs.edn`.
- Fast refresh: `dev/cljs/preload.cljs` with `uix.dev`.
- React 19 note: avoid Reagent DevTools (uses removed ReactDOM.render).

## Add a Feature: Recipe
1. Define events in a new or existing module under `frontend.handlers`.
2. If you fetch data:
   - Use `[:dispatch [:http/init [:your/path]]]` and `::http/fetch` with `:on-success`/`:on-failure`.
3. Register subscriptions (raw or derived) under `frontend.subs` or a domain sub ns.
4. In UIx components, read data with `(use-subscribe [::<ns>/your-sub ...])` and `rf/dispatch` events on user actions.
5. Compose views in layouts via routes (`src/cljs/frontend/routes.cljs`).

## Common Pitfalls
- Don’t call `rf/subscribe` outside render/hook contexts. Use `use-subscribe` in UIx or read from `@re-frame.db/app-db` in callbacks.
- With React 19, do not use `ReactDOM.render`. Use UIx root APIs.
- When using third-party React components, pass props in Clojure style; for DOM-only props, use `:class`, for React components `:className` works as well.
- When passing complex JS props (objects/arrays), use JS values (except `:style`).

## File Map (key ones)
- `frontend/core.cljs` — App root, router start, layout/view composition.
- `frontend/routes.cljs` — Route table, nav links.
- `frontend/layouts/main_layout.cljs` — Shell layout (header/footer), theme switcher.
- `frontend/pages/*` — Views (home/about/todos/not-found).
- `frontend/handlers.cljs` — Re-frame events for Todos.
- `frontend/subs.cljs` — Derived subs (Todos).
- `frontend/http_fx.cljs` — Fetch effect + http status events + `:http/body` sub.
- `frontend/state.cljs` — Global UI/route events and subs (theme, current route).
- `frontend/utilities/theme.cljs` — Theme application and persistence.
- `backend/routes.clj` — Server routes and theme-aware index.

