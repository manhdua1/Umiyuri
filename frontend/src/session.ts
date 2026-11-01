import type { LoginSession } from './auth.ts'

export type Role = 'Admin' | 'Manager' | 'Employee'
export interface Session extends LoginSession { role: Role; expiresAt: number }
export interface SessionState { session: Session | null; notice: string }
export const expiredNotice = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.'

// These unverified claims select UI only. Authorization remains the server's job.
export function decodeSession(value: LoginSession, now = Date.now()): Session {
  try {
    const parts = value.accessToken.split('.')
    if (parts.length !== 3 || parts.some((part) => !part || !/^[A-Za-z0-9_-]+$/.test(part))) throw new Error()
    const encoded = parts[1].replace(/-/g, '+').replace(/_/g, '/')
    const bytes = Uint8Array.from(atob(encoded + '='.repeat((4 - encoded.length % 4) % 4)), (char) => char.charCodeAt(0))
    const claims: unknown = JSON.parse(new TextDecoder('utf-8', { fatal: true }).decode(bytes))
    if (!claims || typeof claims !== 'object' || Array.isArray(claims)) throw new Error()
    const { role, exp } = claims as Record<string, unknown>
    if (role !== 'Admin' && role !== 'Manager' && role !== 'Employee') throw new Error()
    if (typeof exp !== 'number' || !Number.isSafeInteger(exp) || exp <= 0 || exp > Number.MAX_SAFE_INTEGER / 1000) throw new Error()
    if (!Number.isSafeInteger(value.expiresIn) || value.expiresIn <= 0 || value.tokenType !== 'Bearer') throw new Error()
    const expiresAt = Math.min(exp * 1000, now + value.expiresIn * 1000)
    if (expiresAt <= now) throw new Error()
    return { ...value, role, expiresAt }
  } catch {
    throw new Error('Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.')
  }
}

export class SessionStore {
  private state: SessionState = { session: null, notice: '' }
  private listeners = new Set<() => void>()
  private timer: ReturnType<typeof setTimeout> | undefined
  getSnapshot = () => this.state
  subscribe = (listener: () => void) => { this.listeners.add(listener); return () => { this.listeners.delete(listener) } }
  private emit() { this.listeners.forEach((listener) => listener()) }
  start(value: LoginSession) {
    const session = decodeSession(value)
    clearTimeout(this.timer)
    this.state = { session, notice: '' }
    const check = () => {
      const remaining = session.expiresAt - Date.now()
      if (remaining <= 0) this.clear(expiredNotice)
      else this.timer = setTimeout(check, Math.min(remaining, 2_147_483_647))
    }
    check()
    this.emit()
  }
  clear = (notice = '') => {
    clearTimeout(this.timer)
    this.state = { session: null, notice }
    this.emit()
  }
  dispose() { clearTimeout(this.timer); this.state = { session: null, notice: '' }; this.listeners.clear() }
}
