import type { Session } from './session.ts'
import { expiredNotice } from './session.ts'

export type Resource = 'employees' | 'departments'
export type EmployeeStatus = 'ACTIVE' | 'INACTIVE' | 'RESIGNED'
export interface Employee { id: string; code: string; fullName: string; email: string; departmentId: string | null; status: EmployeeStatus }
export interface Department { id: string; name: string; managerId: string | null }
export interface Entities { employees: Employee; departments: Department }
export interface Page<T> { content: T[]; page: number; size: number; totalElements: number; totalPages: number }
export type EmployeeInput = Omit<Employee, 'id'>
export type DepartmentInput = Omit<Department, 'id'>
export class ApiError extends Error {
  status: number
  constructor(message: string, status = 0) { super(message); this.name = 'ApiError'; this.status = status }
}
const invalid = () => new ApiError('Phản hồi hệ thống không hợp lệ. Vui lòng thử lại.')
const record = (value: unknown): value is Record<string, unknown> => !!value && typeof value === 'object' && !Array.isArray(value)
const nullableId = (value: unknown) => value == null || typeof value === 'string'
function entity<K extends Resource>(kind: K, value: unknown): Entities[K] {
  if (!record(value) || typeof value.id !== 'string' || !value.id) throw invalid()
  if (kind === 'employees') {
    if (!['code', 'fullName', 'email'].every((key) => typeof value[key] === 'string') || !nullableId(value.departmentId) || !['ACTIVE', 'INACTIVE', 'RESIGNED'].includes(String(value.status))) throw invalid()
    return { ...value, departmentId: value.departmentId ?? null } as unknown as Entities[K]
  }
  if (typeof value.name !== 'string' || !nullableId(value.managerId)) throw invalid()
  return { ...value, managerId: value.managerId ?? null } as unknown as Entities[K]
}
export function errorMessage(error: unknown) {
  return error instanceof ApiError ? error.message : 'Không thể hoàn tất yêu cầu. Vui lòng thử lại.'
}
export function createClient(session: Session, onUnauthorized: (notice: string) => void) {
  async function request(path: string, method: string, signal?: AbortSignal, body?: unknown, noResult = false): Promise<unknown> {
    signal?.throwIfAborted()
    if (Date.now() >= session.expiresAt) { onUnauthorized(expiredNotice); throw new ApiError(expiredNotice, 401) }
    let response: Response
    try {
      response = await fetch(`/api/${path}`, { method, credentials: 'omit', cache: 'no-store', headers: { Accept: 'application/json', Authorization: `Bearer ${session.accessToken}`, ...(body === undefined ? {} : { 'Content-Type': 'application/json' }) }, body: body === undefined ? undefined : JSON.stringify(body), signal: signal ? AbortSignal.any([signal, AbortSignal.timeout(15000)]) : AbortSignal.timeout(15000) })
    } catch (error) {
      signal?.throwIfAborted()
      throw new ApiError(error instanceof DOMException && error.name === 'TimeoutError' ? 'Yêu cầu quá thời gian. Vui lòng thử lại.' : 'Không thể kết nối đến hệ thống. Vui lòng thử lại.')
    }
    signal?.throwIfAborted()
    if (response.status === 401) { onUnauthorized(expiredNotice); throw new ApiError(expiredNotice, 401) }
    if (response.status === 403) throw new ApiError('Bạn không có quyền thực hiện thao tác này (403).', 403)
    if (!response.ok) {
      const messages: Record<number, string> = { 400: 'Dữ liệu không hợp lệ. Vui lòng kiểm tra các trường.', 404: 'Bản ghi không còn tồn tại. Hãy tải lại danh sách.', 409: 'Dữ liệu bị trùng hoặc vừa thay đổi. Hãy kiểm tra và thử lại.', 429: 'Có quá nhiều yêu cầu. Vui lòng thử lại sau.' }
      throw new ApiError(messages[response.status] ?? 'Không thể hoàn tất yêu cầu. Vui lòng thử lại.', response.status)
    }
    let data: unknown
    try { data = await response.json() } catch { throw invalid() }
    signal?.throwIfAborted()
    if (!record(data) || data.code !== 1000 || (!noResult && data.result == null)) throw invalid()
    return data.result
  }
  return {
    async list<K extends Resource>(kind: K, page: number, size: number, signal?: AbortSignal): Promise<Page<Entities[K]>> {
      const data = await request(`${kind}?page=${page}&size=${size}`, 'GET', signal)
      if (!record(data) || !Array.isArray(data.content) || !['page', 'size', 'totalElements', 'totalPages'].every((key) => typeof data[key] === 'number' && Number.isSafeInteger(data[key]) && Number(data[key]) >= 0) || data.page !== page || data.size !== size || data.content.length > size) throw invalid()
      return { ...data, content: data.content.map((item) => entity(kind, item)) } as unknown as Page<Entities[K]>
    },
    async all<K extends Resource>(kind: K, signal?: AbortSignal): Promise<Entities[K][]> {
      const result: Entities[K][] = []
      let page = 0
      while (true) {
        const data = await this.list(kind, page, 100, signal)
        result.push(...data.content)
        if (page + 1 >= data.totalPages) return [...new Map(result.map((item) => [item.id, item])).values()]
        if (!data.content.length) throw invalid()
        page++
      }
    },
    async detail<K extends Resource>(kind: K, id: string, signal?: AbortSignal): Promise<Entities[K]> { return entity(kind, await request(`${kind}/${encodeURIComponent(id)}`, 'GET', signal)) },
    async save(kind: Resource, id: string | null, input: EmployeeInput | DepartmentInput, signal?: AbortSignal) { return entity(kind, await request(`${kind}${id ? `/${encodeURIComponent(id)}` : ''}`, id ? 'PUT' : 'POST', signal, input)) },
    async remove(kind: Resource, id: string, signal?: AbortSignal) { await request(`${kind}/${encodeURIComponent(id)}`, 'DELETE', signal, undefined, true) },
  }
}
export type Client = ReturnType<typeof createClient>
