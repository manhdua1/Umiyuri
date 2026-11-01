export interface LoginSession {
  accessToken: string
  tokenType: 'Bearer'
  expiresIn: number
}

export class LoginError extends Error {
  constructor(message: string) {
    super(message)
    this.name = 'LoginError'
  }
}

const unavailable = 'Không thể kết nối đến hệ thống. Vui lòng thử lại sau.'
const invalidResponse = 'Hệ thống trả về phản hồi không hợp lệ. Vui lòng thử lại sau.'

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

export async function login(
  username: string,
  password: string,
  signal?: AbortSignal,
): Promise<LoginSession> {
  let response: Response
  try {
    response = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      credentials: 'omit',
      cache: 'no-store',
      body: JSON.stringify({ username, password }),
      signal,
    })
  } catch {
    throw new LoginError(unavailable)
  }

  if (response.status === 401) {
    throw new LoginError('Tên đăng nhập hoặc mật khẩu không đúng, hoặc tài khoản đã bị vô hiệu hóa.')
  }
  if (response.status === 429) {
    throw new LoginError('Có quá nhiều yêu cầu. Vui lòng chờ một lúc rồi thử lại.')
  }
  if (!response.ok) {
    throw new LoginError(response.status >= 500 ? unavailable : 'Không thể đăng nhập. Vui lòng kiểm tra thông tin và thử lại.')
  }

  let payload: unknown
  try {
    payload = await response.json()
  } catch {
    throw new LoginError(invalidResponse)
  }

  if (!isRecord(payload) || payload.code !== 1000 || !isRecord(payload.result)) {
    throw new LoginError(invalidResponse)
  }
  const { accessToken, tokenType, expiresIn } = payload.result
  if (
    typeof accessToken !== 'string' || !accessToken.trim() ||
    tokenType !== 'Bearer' || typeof expiresIn !== 'number' ||
    !Number.isSafeInteger(expiresIn) || expiresIn <= 0
  ) {
    throw new LoginError(invalidResponse)
  }

  return { accessToken, tokenType, expiresIn }
}
