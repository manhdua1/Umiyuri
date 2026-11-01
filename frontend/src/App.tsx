import { useCallback, useEffect, useRef, useState, useSyncExternalStore } from 'react'
import type { FormEvent } from 'react'
import { login, LoginError } from './auth'
import { SessionStore } from './session'
import Admin from './Admin'
import './App.css'

function FaceMark({ className = '' }: { className?: string }) {
  return (
    <svg className={className} viewBox="0 0 200 220" fill="none" aria-hidden="true">
      <path className="frame" d="M48 24H24v32m128-32h24v32M24 164v32h24m104 0h24v-32" />
      <path className="face" d="M61 91c0-34 15-52 39-52s39 18 39 52v18c0 34-18 59-39 59s-39-25-39-59V91Z" />
      <path className="face" d="M80 96h1m38 0h1m-20 4-4 21h9m-20 17c10 7 20 7 30 0M55 188c11-9 27-14 45-14s34 5 45 14" />
      <path className="guide" d="M7 110h186M100 8v204" />
    </svg>
  )
}

function App() {
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [errors, setErrors] = useState({ username: '', password: '' })
  const [pending, setPending] = useState(false)
  const [message, setMessage] = useState('')
  const [store] = useState(() => new SessionStore())
  const state = useSyncExternalStore(store.subscribe, store.getSnapshot)
  const authenticated = Boolean(state.session)
  const request = useRef<AbortController | null>(null)
  const usernameInput = useRef<HTMLInputElement>(null)
  const passwordInput = useRef<HTMLInputElement>(null)
  const logoutButton = useRef<HTMLButtonElement>(null)

  useEffect(() => () => {
    request.current?.abort()
    request.current = null
    store.dispose()
  }, [store])

  useEffect(() => {
    if (authenticated) logoutButton.current?.focus()
    else usernameInput.current?.focus()
  }, [authenticated])

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (request.current || store.getSnapshot().session) return

    const nextErrors = {
      username: username.trim() ? '' : 'Vui lòng nhập tên đăng nhập.',
      password: password.trim() ? '' : 'Vui lòng nhập mật khẩu.',
    }
    setErrors(nextErrors)
    setMessage('')
    if (nextErrors.username || nextErrors.password) {
      if (nextErrors.username) usernameInput.current?.focus()
      else passwordInput.current?.focus()
      return
    }

    const controller = new AbortController()
    request.current = controller
    setPending(true)
    const timeout = window.setTimeout(() => controller.abort(), 15000)
    try {
      const result = await login(username, password, controller.signal)
      if (controller.signal.aborted) return
      setPassword('')
      setShowPassword(false)
      store.start(result)
    } catch (error) {
      if (request.current === controller && !controller.signal.aborted) {
        setMessage(error instanceof LoginError ? error.message : 'Phiên đăng nhập không hợp lệ. Vui lòng thử lại.')
      } else if (request.current === controller) {
        setMessage('Yêu cầu quá thời gian. Vui lòng thử lại.')
      }
    } finally {
      window.clearTimeout(timeout)
      if (request.current === controller) {
        request.current = null
        setPending(false)
      }
    }
  }

  const logout = useCallback((notice = '') => {
    request.current?.abort()
    request.current = null
    store.clear(notice)
    setUsername('')
    setPassword('')
    setShowPassword(false)
    setErrors({ username: '', password: '' })
    setMessage('')
    setPending(false)
  }, [store])

  const locked = pending || authenticated

  if (state.session?.role === 'Admin') return <Admin session={state.session} username={username} logout={logout} />

  return (
    <div className="login-page">
      <header className="brand-bar">
        <a className="brand" href="#main" aria-label="Solips — đến phần đăng nhập">
          <FaceMark className="brand-mark" />
          <span>solips<span className="brand-dot">.</span></span>
        </a>
        <span className="brand-description">Chấm công khuôn mặt</span>
      </header>

      <main id="main" className="login-layout">
        <section className="intro" aria-labelledby="intro-title">
          <div className="face-art"><FaceMark className="face-illustration" /></div>
          <h1 id="intro-title">Mỗi ngày làm việc,<br />một khởi đầu rõ ràng.</h1>
          <p>Solips — hệ thống quản lý chấm công<br className="desktop-break" /> bằng nhận diện khuôn mặt.</p>
          <div className="intro-caption"><span aria-hidden="true" />Con người là điểm bắt đầu.</div>
        </section>

        <section className="login-panel" aria-labelledby="login-title">
          <div className="panel-heading">
            <h2 id="login-title">Đăng nhập</h2>
          </div>

          <form onSubmit={handleSubmit} noValidate aria-busy={pending}>
            <div className="field">
              <label htmlFor="username">Tên đăng nhập</label>
              <input ref={usernameInput} id="username" name="username" autoComplete="username"
                autoCapitalize="none" spellCheck={false} required readOnly={locked}
                value={username} onChange={(event) => { setUsername(event.target.value); setErrors((previous) => ({ ...previous, username: '' })) }}
                aria-invalid={Boolean(errors.username)} aria-describedby={errors.username ? 'username-error' : undefined}
                placeholder="Nhập tên đăng nhập" />
              {errors.username && <p className="field-error" id="username-error">{errors.username}</p>}
            </div>

            <div className="field">
              <label htmlFor="password">Mật khẩu</label>
              <div className="password-control">
                <input ref={passwordInput} id="password" name="password" type={showPassword ? 'text' : 'password'}
                  autoComplete="current-password" required readOnly={locked} value={password}
                  onChange={(event) => { setPassword(event.target.value); setErrors((previous) => ({ ...previous, password: '' })) }}
                  aria-invalid={Boolean(errors.password)} aria-describedby={errors.password ? 'password-error' : undefined}
                  placeholder={authenticated ? 'Mật khẩu đã được xóa' : 'Nhập mật khẩu'} />
                <button className="password-toggle" type="button" disabled={locked}
                  aria-label={showPassword ? 'Ẩn mật khẩu' : 'Hiện mật khẩu'}
                  aria-controls="password" aria-pressed={showPassword} onClick={() => setShowPassword(!showPassword)}>
                  {showPassword ? 'Ẩn' : 'Hiện'}
                </button>
              </div>
              {errors.password && <p className="field-error" id="password-error">{errors.password}</p>}
            </div>

            <div className="form-feedback" aria-live="polite" aria-atomic="true">
              {(message || state.notice) && <p className="error-message">{message || state.notice}</p>}
              {authenticated && <div className="success-message"><strong>Đăng nhập thành công.</strong><span>Giao diện hiện chỉ hỗ trợ tài khoản Admin. Tài khoản {state.session?.role} chưa có trang làm việc.</span></div>}
              {pending && <p className="loading-message">Đang xác thực tài khoản, vui lòng chờ…</p>}
            </div>

            <button className="submit-button" type="submit" disabled={locked} aria-describedby={authenticated ? 'session-note' : undefined}>
              {pending ? 'Đang đăng nhập…' : authenticated ? 'Đã đăng nhập' : 'Đăng nhập'}
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true"><path d="M5 12h14m-6-6 6 6-6 6" stroke="currentColor" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" /></svg>
            </button>
            {authenticated && <button ref={logoutButton} className="logout-button" type="button" onClick={() => logout()}>Đăng xuất khỏi phiên này</button>}
            {authenticated && <p id="session-note" className="session-note">Đăng xuất để đăng nhập bằng tài khoản khác. Thao tác này chỉ xóa phiên trên trang, không thu hồi token ở máy chủ.</p>}
          </form>
        </section>
      </main>
      <footer className="page-footer"><span>Solips</span><span>Hệ thống chấm công dành cho đơn vị của bạn.</span></footer>
    </div>
  )
}

export default App
