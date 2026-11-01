import { useEffect, useMemo, useRef, useState } from 'react'
import type { FormEvent, ReactNode } from 'react'
import { createClient, errorMessage } from './api.ts'
import type { Client, Department, Employee, EmployeeStatus, Page, Resource } from './api.ts'
import type { Session } from './session.ts'
import './admin.css'

const names = { employees: 'Nhân viên', departments: 'Phòng ban' }
const statuses: Record<EmployeeStatus, string> = { ACTIVE: 'Đang làm việc', INACTIVE: 'Tạm ngưng', RESIGNED: 'Đã nghỉ việc' }
type Row = Employee | Department
const label = (row: Row) => 'fullName' in row ? row.fullName : row.name

function Dialog({ title, children, close }: { title: string; children: ReactNode; close: () => void }) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const previous = document.activeElement as HTMLElement | null
    const dialog = ref.current!
    dialog.showModal()
    return () => { dialog.close(); if (previous?.isConnected) previous.focus() }
  }, [])
  return <dialog ref={ref} className="admin-dialog" aria-labelledby="dialog-title" onCancel={(event) => { event.preventDefault(); close() }}>
    <div className="dialog-heading"><h2 id="dialog-title">{title}</h2><button type="button" onClick={close} aria-label="Đóng">Đóng</button></div>{children}
  </dialog>
}

function Editor({ kind, id, mode, client, close, saved }: { kind: Resource; id: string | null; mode: 'detail' | 'edit' | 'create' | 'delete'; client: Client; close: () => void; saved: () => void }) {
  const [row, setRow] = useState<Row | null>(null)
  const [employees, setEmployees] = useState<Employee[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [loading, setLoading] = useState(true)
  const [ready, setReady] = useState(false)
  const [pending, setPending] = useState(false)
  const [error, setError] = useState('')
  const [retry, setRetry] = useState(0)
  const [values, setValues] = useState({ code: '', fullName: '', email: '', departmentId: '', status: 'ACTIVE' as EmployeeStatus, name: '', managerId: '' })
  const request = useRef<AbortController | null>(null)
  const busy = useRef(false)
  const form = useRef<HTMLFormElement>(null)
  const readonly = mode === 'detail'
  const remove = mode === 'delete'
  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      setLoading(true); setReady(false); setError('')
      try {
        const [detail, employeeOptions, departmentOptions] = await Promise.all([
          id ? client.detail(kind, id, controller.signal) : Promise.resolve(null),
          client.all('employees', controller.signal), client.all('departments', controller.signal),
        ])
        if (controller.signal.aborted) return
        setRow(detail); setEmployees(employeeOptions); setDepartments(departmentOptions); setReady(true)
        if (detail) setValues((previous) => ({ ...previous, ...detail, departmentId: 'departmentId' in detail ? detail.departmentId ?? '' : '', managerId: 'managerId' in detail ? detail.managerId ?? '' : '' }))
      } catch (failure) { if (!controller.signal.aborted) setError(errorMessage(failure)) }
      finally { if (!controller.signal.aborted) setLoading(false) }
    }
    void load()
    return () => { controller.abort(); request.current?.abort() }
  }, [client, kind, id, retry])
  useEffect(() => {
    if (!loading && !error) form.current?.querySelector<HTMLInputElement>('input')?.focus()
  }, [loading, error])
  async function submit(event: FormEvent) {
    event.preventDefault()
    if (busy.current || loading || !ready) return
    const controller = new AbortController()
    request.current = controller; busy.current = true; setPending(true); setError('')
    try {
      if (remove) await client.remove(kind, id!, controller.signal)
      else await client.save(kind, id, kind === 'employees' ? { code: values.code.trim(), fullName: values.fullName.trim(), email: values.email.trim(), departmentId: values.departmentId || null, status: values.status } : { name: values.name.trim(), managerId: values.managerId || null }, controller.signal)
      if (!controller.signal.aborted) saved()
    } catch (failure) { if (!controller.signal.aborted) setError(errorMessage(failure)) }
    finally { if (!controller.signal.aborted) { busy.current = false; setPending(false); request.current = null } }
  }
  const textField = (key: 'code' | 'fullName' | 'email' | 'name', title: string) => <div className="field"><label htmlFor={`edit-${key}`}>{title}</label><input id={`edit-${key}`} name={key} type={key === 'email' ? 'email' : 'text'} required maxLength={255} pattern={key === 'email' ? undefined : '.*\\S.*'} readOnly={readonly} value={values[key]} onChange={(event) => setValues({ ...values, [key]: event.target.value })} /></div>
  return <Dialog title={`${remove ? 'Xóa' : readonly ? 'Chi tiết' : id ? 'Chỉnh sửa' : 'Thêm'} ${names[kind].toLowerCase()}`} close={close}>
    {loading && <p role="status">Đang tải dữ liệu…</p>}
    {error && <div className="error-message" role="alert">{error} <button type="button" disabled={pending} onClick={() => setRetry(retry + 1)}>Tải lại dữ liệu</button></div>}
    {!loading && ready && (row || !id) && <form ref={form} onSubmit={submit} aria-busy={pending}>
      {remove ? <><p>Bạn muốn xóa <strong>{row && label(row)}</strong>?</p><p className="delete-note">{kind === 'employees' ? 'Nhân viên sẽ bị xóa khỏi danh sách và không còn là quản lý của các phòng ban liên quan.' : 'Phòng ban sẽ bị xóa. Nhân viên liên quan sẽ không còn được gán phòng ban.'} Không thể hoàn tác từ giao diện này.</p></> : <fieldset disabled={pending}>
        {kind === 'employees' ? <>{textField('code', 'Mã nhân viên')}{textField('fullName', 'Họ và tên')}{textField('email', 'Email')}<div className="field"><label htmlFor="departmentId">Phòng ban</label><select id="departmentId" disabled={readonly} value={values.departmentId} onChange={(event) => setValues({ ...values, departmentId: event.target.value })}><option value="">Không có phòng ban</option>{values.departmentId && !departments.some((item) => item.id === values.departmentId) && <option value={values.departmentId}>Không còn khả dụng ({values.departmentId})</option>}{departments.map((item) => <option key={item.id} value={item.id}>{item.name}</option>)}</select></div><div className="field"><label htmlFor="status">Trạng thái</label><select id="status" required disabled={readonly} value={values.status} onChange={(event) => setValues({ ...values, status: event.target.value as EmployeeStatus })}>{Object.entries(statuses).map(([value, title]) => <option key={value} value={value}>{title}</option>)}</select></div></> : <>{textField('name', 'Tên phòng ban')}<div className="field"><label htmlFor="managerId">Quản lý</label><select id="managerId" disabled={readonly} value={values.managerId} onChange={(event) => setValues({ ...values, managerId: event.target.value })}><option value="">Không có quản lý</option>{values.managerId && !employees.some((item) => item.id === values.managerId) && <option value={values.managerId}>Không còn khả dụng ({values.managerId})</option>}{employees.map((item) => <option key={item.id} value={item.id}>{item.fullName} ({item.code})</option>)}</select></div></>}
      </fieldset>}
      <div className="dialog-actions"><button type="button" onClick={close}>{readonly ? 'Đóng' : 'Hủy'}</button>{!readonly && <button className={remove ? 'danger-button' : 'primary-button'} type="submit" disabled={pending || loading}>{pending ? 'Đang xử lý…' : remove ? 'Xác nhận xóa' : 'Lưu thay đổi'}</button>}</div>
    </form>}
  </Dialog>
}

function ResourcePage({ kind, client, changed, revision }: { kind: Resource; client: Client; changed: () => void; revision: number }) {
  const [page, setPage] = useState(0)
  const [size, setSize] = useState(20)
  const [data, setData] = useState<Page<Row> | null>(null)
  const [employees, setEmployees] = useState<Employee[]>([])
  const [departments, setDepartments] = useState<Department[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [reload, setReload] = useState(0)
  const [notice, setNotice] = useState('')
  const [editor, setEditor] = useState<{ id: string | null; mode: 'detail' | 'edit' | 'create' | 'delete' } | null>(null)
  const addButton = useRef<HTMLButtonElement>(null)
  useEffect(() => {
    const controller = new AbortController()
    async function load() {
      setLoading(true); setError(''); setData(null)
      try {
        const [result, allEmployees, allDepartments] = await Promise.all([client.list(kind, page, size, controller.signal), client.all('employees', controller.signal), client.all('departments', controller.signal)])
        if (controller.signal.aborted) return
        if (page > 0 && page >= result.totalPages) { setPage(Math.max(0, result.totalPages - 1)); return }
        setData(result); setEmployees(allEmployees); setDepartments(allDepartments)
      } catch (failure) { if (!controller.signal.aborted) setError(errorMessage(failure)) }
      finally { if (!controller.signal.aborted) setLoading(false) }
    }
    void load()
    return () => controller.abort()
  }, [client, kind, page, size, revision, reload])
  const departmentName = (id: string | null) => id ? departments.find((item) => item.id === id)?.name ?? id : 'Chưa gán'
  const employeeName = (id: string | null) => id ? employees.find((item) => item.id === id)?.fullName ?? id : 'Chưa gán'
  return <section aria-labelledby="resource-title" aria-busy={loading}>
    <div className="resource-heading"><h2 id="resource-title">{names[kind]}</h2><div className="resource-tools"><button type="button" onClick={() => setReload(reload + 1)} disabled={loading}>Tải lại</button><button ref={addButton} type="button" className="primary-button" onClick={() => { setNotice(''); setEditor({ id: null, mode: 'create' }) }}>Thêm {names[kind].toLowerCase()}</button></div></div>
    <div role="status" className="resource-status">{loading ? 'Đang tải danh sách…' : notice}</div>
    {error && <p role="alert" className="error-message">{error}</p>}
    {data && <><div className="table-scroll" tabIndex={0} role="region" aria-label={`Danh sách ${names[kind].toLowerCase()}, cuộn ngang nếu cần`}><table><caption className="sr-only">{names[kind]} — trang {page + 1}</caption><thead><tr>{kind === 'employees' ? <><th scope="col">Mã</th><th scope="col">Họ và tên</th><th scope="col">Email</th><th scope="col">Phòng ban</th><th scope="col">Trạng thái</th></> : <><th scope="col">Tên phòng ban</th><th scope="col">Quản lý</th></>}<th scope="col">Thao tác</th></tr></thead><tbody>{data.content.map((row) => <tr key={row.id}>{'fullName' in row ? <><td>{row.code}</td><th scope="row">{row.fullName}</th><td>{row.email}</td><td>{departmentName(row.departmentId)}</td><td><span className="status-badge">{statuses[row.status]}</span></td></> : <><th scope="row">{row.name}</th><td>{employeeName(row.managerId)}</td></>}<td><div className="row-actions">{(['detail', 'edit', 'delete'] as const).map((mode) => <button key={mode} type="button" aria-label={`${mode === 'detail' ? 'Chi tiết' : mode === 'edit' ? 'Sửa' : 'Xóa'} ${label(row)}`} onClick={() => { setNotice(''); setEditor({ id: row.id, mode }) }}>{mode === 'detail' ? 'Chi tiết' : mode === 'edit' ? 'Sửa' : 'Xóa'}</button>)}</div></td></tr>)}</tbody></table></div>{!data.content.length && <p className="empty-state">Chưa có {names[kind].toLowerCase()}. Chọn “Thêm {names[kind].toLowerCase()}” để tạo bản ghi đầu tiên.</p>}
      <div className="pagination"><span>{data.totalElements} bản ghi · Trang {data.totalPages ? page + 1 : 0}/{data.totalPages}</span><label>Số dòng <select value={size} onChange={(event) => { setSize(Number(event.target.value)); setPage(0) }}>{[10, 20, 50, 100].map((value) => <option key={value}>{value}</option>)}</select></label><div><button type="button" disabled={loading || page === 0} onClick={() => setPage(page - 1)}>Trước</button><button type="button" disabled={loading || page + 1 >= data.totalPages} onClick={() => setPage(page + 1)}>Sau</button></div></div></>}
    {editor && <Editor {...editor} kind={kind} client={client} close={() => setEditor(null)} saved={() => { setEditor(null); setNotice('Đã lưu thay đổi.'); changed(); addButton.current?.focus() }} />}
  </section>
}

export default function Admin({ session, username, logout }: { session: Session; username: string; logout: (notice?: string) => void }) {
  const [active, setActive] = useState<'dashboard' | Resource>('dashboard')
  const [revision, setRevision] = useState(0)
  const client = useMemo(() => createClient(session, logout), [session, logout])
  const heading = useRef<HTMLHeadingElement>(null)
  useEffect(() => { heading.current?.focus() }, [active])
  return <div className="admin-shell"><a className="skip-link" href="#admin-main">Đến nội dung chính</a><aside className="admin-sidebar"><a className="brand" href="#admin-main">solips<span className="brand-dot">.</span></a><p className="sidebar-description">Quản trị hệ thống</p><nav aria-label="Điều hướng quản trị">{(['dashboard', 'employees', 'departments'] as const).map((item) => <button key={item} type="button" aria-current={active === item ? 'page' : undefined} onClick={() => setActive(item)}>{item === 'dashboard' ? 'Dashboard' : names[item]}</button>)}</nav><div className="admin-account"><span>{username}</span><small>Admin</small><button type="button" onClick={() => logout()}>Đăng xuất</button></div></aside><main id="admin-main" className="admin-main"><header className="admin-header"><h1 ref={heading} tabIndex={-1}>{active === 'dashboard' ? 'Dashboard' : names[active]}</h1><span>Solips / Quản trị</span></header>{active !== 'dashboard' && <ResourcePage key={active} kind={active} client={client} revision={revision} changed={() => setRevision((value) => value + 1)} />}</main></div>
}
