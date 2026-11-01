import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import { login, LoginError } from '../src/auth.ts'

const originalFetch = globalThis.fetch
const valid = { code: 1000, result: { accessToken: 'mock-only-token', tokenType: 'Bearer', expiresIn: 3600 } }
afterEach(() => { globalThis.fetch = originalFetch })

function mockJson(payload, status = 200) {
  globalThis.fetch = async () => Response.json(payload, { status })
}

test('posts exact credentials and returns the documented session, without cookies', async () => {
  const controller = new AbortController()
  globalThis.fetch = async (url, options) => {
    assert.equal(url, '/api/auth/login')
    assert.equal(options.method, 'POST')
    assert.equal(options.headers['Content-Type'], 'application/json')
    assert.equal(options.credentials, 'omit')
    assert.equal(options.cache, 'no-store')
    assert.equal(options.signal, controller.signal)
    assert.deepEqual(JSON.parse(options.body), { username: ' mock-user ', password: ' mock-password ' })
    return Response.json(valid)
  }
  assert.deepEqual(await login(' mock-user ', ' mock-password ', controller.signal), valid.result)
})

test('invalid credentials are sanitized rather than displaying server details', async () => {
  mockJson({ code: 1001, message: 'private server detail' }, 401)
  await assert.rejects(login('mock', 'mock'), (error) => error instanceof LoginError && /không đúng/.test(error.message) && !error.message.includes('private'))
})

test('unavailable and network failures are safe and actionable', async () => {
  mockJson({ message: 'internal trace' }, 503)
  await assert.rejects(login('mock', 'mock'), /Không thể kết nối/)
  globalThis.fetch = async () => { throw new Error('private network detail') }
  await assert.rejects(login('mock', 'mock'), /Không thể kết nối/)
})

test('rate limiting and other HTTP failures have safe messages', async () => {
  mockJson({}, 429)
  await assert.rejects(login('mock', 'mock'), /quá nhiều yêu cầu/)
  mockJson({}, 400)
  await assert.rejects(login('mock', 'mock'), /kiểm tra thông tin/)
})

test('HTML or malformed JSON never counts as successful login', async () => {
  globalThis.fetch = async () => new Response('<html>proxy error</html>')
  await assert.rejects(login('mock', 'mock'), /phản hồi không hợp lệ/)
})

for (const [label, payload] of [
  ['null', null],
  ['missing result', { code: 1000 }],
  ['wrong application code', { ...valid, code: 1001 }],
  ['empty token', { code: 1000, result: { ...valid.result, accessToken: ' ' } }],
  ['incorrect token type', { code: 1000, result: { ...valid.result, tokenType: 'Basic' } }],
  ['string expiry', { code: 1000, result: { ...valid.result, expiresIn: '3600' } }],
  ['expired session', { code: 1000, result: { ...valid.result, expiresIn: 0 } }],
]) {
  test(`rejects invalid success response: ${label}`, async () => {
    mockJson(payload)
    await assert.rejects(login('mock', 'mock'), /phản hồi không hợp lệ/)
  })
}

test('an aborted request cannot produce a session', async () => {
  globalThis.fetch = async (_url, options) => {
    options.signal.throwIfAborted()
    return Response.json(valid)
  }
  const controller = new AbortController()
  controller.abort()
  await assert.rejects(login('mock', 'mock', controller.signal), LoginError)
})
