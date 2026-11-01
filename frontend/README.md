# Solips frontend

Vietnamese account login for the facial attendance system. React + TypeScript + Vite; no camera access, face authentication, dashboard, or redirect.

## Development

Use `npm install` then `npm run dev` from this directory. The backend must be configured and started separately; frontend commands do not start it.

Vite's development server forwards `/api` to `http://localhost:8080` (Spring Boot's default; no explicit server port is configured in the inspected backend). To override, set the server-side environment variable `API_PROXY_TARGET` before starting Vite, or put it in an ignored `frontend/.env.local`. Do not put secrets in that URL or use a `VITE_*` variable for it. Restart Vite after changing proxy settings.

**Deployment:** the Vite dev proxy is not part of the production build or `vite preview`. Serve `dist/` behind HTTPS and configure the production web server/reverse proxy to forward same-origin `/api` to the backend. Otherwise login will not work. No backend CORS changes are required for same-origin deployment.

Typography uses Be Vietnam Pro from Google Fonts, with a local fallback if unavailable. Production CSP/network policy must permit that stylesheet/font service, or the font should be self-hosted before deployment.

## API and session behavior

- POST `/api/auth/login`, JSON `{ username, password }`; credentials are sent unchanged, without cookies, and requests are not cached.
- Success must be HTTP 2xx with `{ code: 1000, result: { accessToken: string, tokenType: "Bearer", expiresIn: positive integer } }`.
- HTTP 401, rate limiting, network failures, a 15-second timeout, server errors, and malformed responses have sanitized Vietnamese feedback; raw response messages are never shown.
- The token is held only in a React ref. It is never rendered, logged, persisted, decoded, or used to infer a role. Password is cleared on success.
- Success stays on the login page and blocks resubmission. The displayed success confirms the login request, not a persistent or indefinitely valid session. No protected API is called and no automatic expiry/refresh flow is implemented.
- Optional logout clears only the in-memory session and resets the form. It does not revoke a server token. Reloading or unmounting also loses the session; the existing backend has no logout/revocation endpoint.
- Required fields, linked inline errors, keyboard focus, a named password toggle, loading/live feedback, and mobile layouts are included.

## Validation

- `npm run build`: TypeScript and production asset build.
- `npm run lint`: Oxlint.
- `npm test`: dependency-free Node test runner (Node 22.18+ or Node 24 recommended for native TypeScript stripping). Tests mock fetch and never contact the backend/database.

Browser validation remains a separate step: mock `/api/auth/login` in a browser test before submitting; cover empty fields, 401, network failure, malformed success, loading/double-submit, success without navigation, cleared password, local logout, keyboard-only use, and narrow mobile viewports. Do not use live credentials or call the database for smoke checks.
