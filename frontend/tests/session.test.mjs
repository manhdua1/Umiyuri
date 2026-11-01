import assert from 'node:assert/strict'
import { test } from 'node:test'
import { decodeSession, SessionStore } from '../src/session.ts'

function token(claims) { return `e30.${Buffer.from(JSON.stringify(claims)).toString('base64url')}.c2ln` }
function value(claims = { role: 'Admin', exp: Math.floor(Date.now() / 1000) + 3600 }) { return { accessToken: token(claims), tokenType: 'Bearer', expiresIn: 3600 } }

test('accepts exact roles and caps expiry by login response lifetime', () => {
  for (const role of ['Admin', 'Manager', 'Employee']) {
    const session = decodeSession(value({ role, exp: 10000 }), 1000)
    assert.equal(session.role, role)
    assert.equal(session.expiresAt, 3601000)
  }
})
for (const claims of [{}, { role: 'admin', exp: 10000 }, { role: 'ROLE_Admin', exp: 10000 }, { role: 'Admin', exp: '10000' }, { role: 'Admin', exp: 1 }, { role: 'Admin', exp: 100.5 }, { role: 'Admin', exp: 1e20 }, null, []]) {
  test(`rejects unsafe or expired claims ${JSON.stringify(claims)}`, () => assert.throws(() => decodeSession(value(claims), 2000)))
}
test('rejects malformed JWT segments and invalid JSON', () => {
  for (const accessToken of ['opaque', 'a.b.c.d', 'a.@@@@.c', 'a.eA.c', '.e30.c']) assert.throws(() => decodeSession({ ...value(), accessToken }))
})
test('memory-only store notifies, logs out, and replaces sessions', () => {
  const store = new SessionStore()
  let updates = 0
  const unsubscribe = store.subscribe(() => updates++)
  store.start(value())
  assert.equal(store.getSnapshot().session.role, 'Admin')
  store.start(value({ role: 'Employee', exp: Math.floor(Date.now() / 1000) + 3600 }))
  assert.equal(store.getSnapshot().session.role, 'Employee')
  store.clear('expired')
  assert.deepEqual(store.getSnapshot(), { session: null, notice: 'expired' })
  assert.equal(updates, 3)
  unsubscribe(); store.dispose()
})
test('expiry timer clears session without another request', (context) => {
  context.mock.timers.enable({ apis: ['Date', 'setTimeout'], now: 100000 })
  const store = new SessionStore()
  store.start(value({ role: 'Admin', exp: 101 }))
  context.mock.timers.tick(1000)
  assert.equal(store.getSnapshot().session, null)
  assert.match(store.getSnapshot().notice, /hết hạn/)
  store.dispose()
})
