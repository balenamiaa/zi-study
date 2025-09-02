# Frontend Quick Guide (for AI/LLM Agents)

## Stack & Entry Points
- React via UIx: `uix.core`, root in `src/cljs/frontend/core.cljs`.
- State: re-frame (`re-frame.core`) with subs/events and a central `app-db`.
- Router: Reitit frontend (`reitit.frontend.easy`), routes in `src/cljs/frontend/routes.cljs` (supports `:protected?`).
- HTTP: custom `::fetch` effect wrapping Fetch API (`src/cljs/frontend/state/http_fx.cljs`).
- CSS/UI: Tailwind + DaisyUI, theme via `data-theme` attribute.
 - Auth: Session cookie (HttpOnly, SameSite=Lax) with `/api/auth/*` endpoints.
 - Env: Integrant key `:config/env` loads from `secret.edn`, then ENV vars, then defaults (logs warnings on defaults).
   - secret.edn search order: `SECRET_EDN_PATH` → project root `./secret.edn` → classpath `resources/secret.edn` (classpath option discouraged for production).
   - Example `secret.example.edn` is provided; copy to `secret.edn` and edit.

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
  - Use inside UIx via `frontend.uix.hooks/use-subscribe`, e.g. `(use-subscribe [::state.todos.subs/todos])`.
  - Auth: `frontend.state.auth.subs` provides `::authenticated?`, `::current-user`.
- Events:
  - Pure DB updates: `rf/reg-event-db` (e.g. `::set-current-route`, `::set-theme`).
  - Effects: `rf/reg-event-fx` returns effects map, e.g. HTTP call via `::state.http-fx/fetch`.
  - Auth: `frontend.state.auth.handlers` has `::get-me`, `::login`, `::logout`, `::register`.
- Effects:
  - Register custom effects with `rf/reg-fx`. We use `::fetch` in `http_fx.cljs`.
  - Built-in effect keys:
    - `:dispatch` — dispatch a single event vector, e.g. `{:dispatch [::evt arg]}`.
    - `:fx` — run multiple effects in sequence: `{:fx [[:dispatch [::evt]] [::ns/custom-effect payload]]]}`.
    - `:db` — replace the app-db value.
  - `reg-event-db` returns only `:db`. `reg-event-fx` returns an effects map (can include `:db`, `:dispatch`, `:fx`, custom effects, etc.).

## HTTP Fetch Flow (Todos example)
1. UI dispatches `::state.todos.handlers/get-todos`.
2. Event returns effects:
   - `[:dispatch [:http/init [:todos]]]` → sets `:http [:todos] :status` to `:initial-loading`/`:loading`.
   - `[::state.http-fx/fetch {...}]` → triggers Fetch with Transit/JSON handling.
3. On success/failure:
   - `:http/success` or `:http/failure` updates `:http [:todos]` with `:resp` or `:error` and `:status`.
4. Derived sub:
   - `:http/body` pulls `:body` from `:http` path.
   - `::state.todos.subs/todos` sorts by `:id` for rendering.

## Routing
- Routes carry `:layout` and `:view` in `:data` (`src/cljs/frontend/routes.cljs`).
- `frontend.core` stores current match in `app-db` under `:current-route` on navigation.
- `app` component renders `($ layout {:current-route ...} ($ view match))` and includes `route-guard` to redirect when needed.
- Protected routes: add `:protected? true`. `frontend.core` guards and redirects to `:login` with `?redirect` preserved.

## Theming
- Theme state in `[:ui :theme]` (`:system` | `:light` | `:dark`).
- `frontend.utilities.theme`:
  - `apply-theme` sets `<html data-theme>` to DaisyUI theme names (`gold_dark` / `gold_light`).
  - `apply-theme` applies DOM only. `persist-theme!` writes cookie + localStorage.
  - `initialize-theme` dispatches `::state.handlers/set-theme` with saved theme, and wires system preference listener that re-dispatches when in `:system`.
- Global handlers (`frontend.state.handlers`):
  - `::set-theme` is an `reg-event-fx` that updates `[:ui :theme]` and triggers the custom fx to apply + persist.
- Server boot:
  - `backend.routes/index` reads `theme` cookie and sets `data-theme` before CSS to avoid FOUC; small inline script covers `system`.
- App init:
  - `frontend.state.app/init!` initializes theme and dispatches `::state.auth.handlers/get-me`.

## Icons & JS Interop
- Import named icons: `(:require ["lucide-react" :refer [Home Info ...]])`.
- Use directly: `($ Home {:size 20})`. For dynamic component values, call the value directly: `($ icon {:size 20})`.
- For React components, UIx maps `:class` → `className`; nested JS objects must be passed as JS (except `:style`, which is auto-converted).

## Development
- Shadow-cljs build: `:app` in `shadow-cljs.edn`.
- Fast refresh: `dev/cljs/preload.cljs` with `uix.dev`.
- React 19 note: avoid Reagent DevTools (uses removed ReactDOM.render).

## Add a Feature: Recipe
1. Define events in a new or existing module under `frontend/state/<feature>/handlers.cljs`.
2. If you fetch data:
   - Use `[:dispatch [:http/init [:your/path]]]` and `::state.http-fx/fetch` with `:on-success`/`:on-failure`.
3. Register subscriptions (raw or derived) under `frontend/state/<feature>/subs.cljs`.
4. In UIx components, read data with `(use-subscribe [::<ns>/your-sub ...])` and `rf/dispatch` events on user actions.
5. Compose views in layouts via routes (`src/cljs/frontend/routes.cljs`).
6. To protect a view, add `:protected? true` and ensure auth bootstrap (`::state.auth.handlers/get-me`) is dispatched on app init (already wired in `frontend.core`).

## Common Pitfalls
- Don’t call `rf/subscribe` outside render/hook contexts. Use `use-subscribe` in UIx or read from `@re-frame.db/app-db` in callbacks.
- With React 19, do not use `ReactDOM.render`. Use UIx root APIs.
- When using third-party React components, pass props in Clojure style; for DOM-only props, use `:class`, for React components `:className` works as well.
- When passing complex JS props (objects/arrays), use JS values (except `:style`).

## File Map (key ones)
- `frontend/core.cljs` — App root, router start, layout/view composition.
- `frontend/routes.cljs` — Route table, nav links.
- `frontend/layouts/main_layout.cljs` — Shell layout (header/footer), theme switcher.
- `frontend/pages/*` — Views (home/about/todos/not-found/login).
- `frontend/state/todos/handlers.cljs` — Re-frame events for Todos.
- `frontend/state/todos/subs.cljs` — Derived subs (Todos).
- `frontend/state/http_fx.cljs` — Fetch effect + http status events + `:http/body` sub.
- `frontend/state/handlers.cljs` — Global UI/route events and side-effecting theme event.
- `frontend/state/subs.cljs` — Global subs (`::current-route`, `::ui-state`).
- `frontend/utilities/theme.cljs` — Theme DOM/persistence helpers and `listen-system-change!`.
 - Auth pages: `frontend/pages/login.cljs` for manual + Google sign-in start.
- `backend/routes.clj` — Server routes and theme-aware index.
- `backend/api/auth.clj` — Session-based auth endpoints (register/login/logout/me, Google skeleton).
- `backend/env.clj` — Env loader (secret.edn > ENV > defaults) as Integrant key `:config/env`.
- `resources/config.edn` — Wires `:web/routes` with `:env` from `:config/env`.

## Uploads
- Root folder is configurable via env key `:uploads {:root "uploads"}` (defaults to `uploads` under CWD).
- Override with env var `UPLOADS_ROOT`.
- Files are served under `/uploads/**`; avatars go to `<root>/avatars` and are accessible at `/uploads/avatars/<file>`.
