import assert from 'node:assert/strict'
import { afterEach, test } from 'node:test'
import { createClient, ApiError } from '../src/api.ts'
import { SessionStore } from '../src/session.ts'
const original = globalThis.fetch
const session = { accessToken: 'mock-only', expiresAt: Date.now() + 3600000, role: 'Admin' }
const employee = { id: 'e1', code: 'E1', fullName: 'Test', email: 'test@example.invalid', departmentId: null, status: 'RESIGNED' }
const department = { id: 'd1', name: 'Test', managerId: null }
const response = (result) => Response.json({ code: 1000, result })
afterEach(() => { globalThis.fetch = original })

test('bearer client uses documented routes, full PUT null refs, POST and delete without result', async () => {
  const calls = []
  globalThis.fetch = async (url, options) => {
    calls.push([url, options])
    assert.equal(options.headers.Authorization, 'Bearer mock-only')
    assert.equal(options.credentials, 'omit')
    assert.equal(options.cache, 'no-store')
    if (options.method === 'DELETE') return Response.json({ code: 1000 })
    return response(url.includes('employees') ? employee : department)
  }
  const client = createClient(session, () => assert.fail())
  await client.detail('employees', 'e1')
  const { id: _id, ...input } = employee
  await client.save('employees', 'e1', input)
  await client.save('departments', null, { name: 'Test', managerId: null })
  await client.save('departments', 'd1', { name: 'Test', managerId: null })
  await client.remove('employees', 'e1')
  assert.deepEqual(calls.map(([url, options]) => [url, options.method]), [['/api/employees/e1', 'GET'], ['/api/employees/e1', 'PUT'], ['/api/departments', 'POST'], ['/api/departments/d1', 'PUT'], ['/api/employees/e1', 'DELETE']])
  assert.deepEqual(JSON.parse(calls[1][1].body), input)
  assert.equal(JSON.parse(calls[3][1].body).managerId, null)
})
test('selectors traverse pages beyond 100 and retain all employment statuses', async () => {
  const urls = []
  globalThis.fetch = async (url) => {
    urls.push(url)
    const page = Number(new URL(url, 'http://mock').searchParams.get('page'))
    return response({ content: Array.from({ length: page === 2 ? 5 : 100 }, (_, index) => ({ ...employee, id: `${page}-${index}` })), page, size: 100, totalElements: 205, totalPages: 3 })
  }
  const result = await createClient(session, () => {}).all('employees')
  assert.equal(result.length, 205)
  assert.equal(result[204].status, 'RESIGNED')
  assert.deepEqual(urls, [0, 1, 2].map((page) => `/api/employees?page=${page}&size=100`))
})
test('401 clears actual session store, 403 is distinct and preserves session', async () => {
  const store = new SessionStore()
  const accessToken = `e30.${Buffer.from(JSON.stringify({ role: 'Admin', exp: Math.floor(Date.now() / 1000) + 3600 })).toString('base64url')}.c2ln`
  store.start({ accessToken, tokenType: 'Bearer', expiresIn: 3600 })
  const client = createClient(store.getSnapshot().session, store.clear)
  globalThis.fetch = async () => Response.json({ message: 'private' }, { status: 403 })
  await assert.rejects(client.detail('employees', 'e1'), (error) => error.status === 403 && !error.message.includes('private'))
  assert.ok(store.getSnapshot().session)
  globalThis.fetch = async () => new Response('', { status: 401 })
  await assert.rejects(client.detail('employees', 'e1'), (error) => error.status === 401)
  assert.equal(store.getSnapshot().session, null)
  store.dispose()
})
test('expired session prevents network requests', async () => {
  let cleared = false
  globalThis.fetch = async () => assert.fail('must not fetch')
  await assert.rejects(createClient({ ...session, expiresAt: 0 }, () => { cleared = true }).detail('employees', 'e1'), (error) => error.status === 401)
  assert.equal(cleared, true)
})
for (const status of [400, 404, 409, 429, 500]) {
  test(`sanitizes HTTP ${status}`, async () => {
    globalThis.fetch = async () => Response.json({ message: 'private trace' }, { status })
    await assert.rejects(createClient(session, () => {}).detail('employees', 'e1'), (error) => error instanceof ApiError && error.status === status && !error.message.includes('private'))
  })
}
for (const payload of [null, { code: 9999, message: 'private' }, { code: 1000 }, { code: 1000, result: {} }, { code: 1000, result: { ...employee, status: 'OTHER' } }]) {
  test(`rejects malformed envelope/entity ${JSON.stringify(payload)}`, async () => {
    globalThis.fetch = async () => Response.json(payload)
    await assert.rejects(createClient(session, () => {}).detail('employees', 'e1'), ApiError)
  })
}
test('validates pagination and handles empty last page', async () => {
  globalThis.fetch = async () => response({ content: [], page: 2, size: 20, totalElements: 40, totalPages: 2 })
  assert.deepEqual((await createClient(session, () => {}).list('employees', 2, 20)).content, [])
  globalThis.fetch = async () => response({ items: [], page: 0, size: 20, totalElements: 0, totalPages: 0 })
  await assert.rejects(createClient(session, () => {}).list('employees', 0, 20), ApiError)
})
test('abort before or after fetch cannot return data or clear session', async () => {
  const controller = new AbortController()
  const client = createClient(session, () => assert.fail())
  controller.abort()
  globalThis.fetch = async () => assert.fail()
  await assert.rejects(client.detail('employees', 'e1', controller.signal), { name: 'AbortError' })
  const second = new AbortController()
  globalThis.fetch = async () => { second.abort(); return response(employee) }
  await assert.rejects(client.detail('employees', 'e1', second.signal), { name: 'AbortError' })
})
test('sanitizes network and non-JSON responses', async () => {
  const client = createClient(session, () => {})
  globalThis.fetch = async () => { throw new Error('private trace') }
  await assert.rejects(client.detail('employees', 'e1'), (error) => !error.message.includes('private'))
  globalThis.fetch = async () => new Response('<html>private trace</html>')
  await assert.rejects(client.detail('employees', 'e1'), ApiError)
})
