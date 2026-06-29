import { describe, it, expect, vi, beforeEach } from 'vitest'

const captured = vi.hoisted(() => {
  const store: { success?: Function; error?: Function } = {}
  return store
})

vi.mock('axios', () => {
  const mockInstance = {
    interceptors: {
      request: {
        use: vi.fn(),
      },
      response: {
        use: vi.fn((onFulfilled: Function, onRejected: Function) => {
          captured.success = onFulfilled
          captured.error = onRejected
        }),
      },
    },
    get: vi.fn(() => Promise.resolve({ data: { code: 'SUCCESS', data: null } })),
    post: vi.fn(() => Promise.resolve({ data: { code: 'SUCCESS', data: null } })),
    put: vi.fn(() => Promise.resolve({ data: { code: 'SUCCESS', data: null } })),
    delete: vi.fn(() => Promise.resolve({ data: { code: 'SUCCESS', data: null } })),
  }
  return {
    default: {
      create: vi.fn(() => mockInstance),
    },
  }
})

import { apiClient, apiGet, apiPost, apiPut, apiDelete } from '../index'

beforeEach(() => {
  vi.clearAllMocks()
})

describe('Success interceptor', () => {
  it('unwraps SUCCESS response to body.data', () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'SUCCESS', data: { id: 1 } } }
    const result = handler(mockResponse)
    // Interceptor returns body.data directly (unwrapped)
    expect(result).toEqual({ id: 1 })
  })

  it('unwraps nested data from SUCCESS response', () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'SUCCESS', data: { nested: 'value' } } }
    const result = handler(mockResponse)
    expect(result).toEqual({ nested: 'value' })
  })

  it('unwraps array data from SUCCESS response', () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'SUCCESS', data: ['a', 'b'] } }
    const result = handler(mockResponse)
    expect(result).toEqual(['a', 'b'])
  })

  it('returns BusinessError when code is not SUCCESS', () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'BUSINESS_ERROR', message: '业务异常' } }
    const result = handler(mockResponse)
    expect(result).toEqual({
      code: 'BUSINESS_ERROR',
      message: '业务异常',
      isBusinessError: true,
    })
  })

  it('returns BusinessError with empty message fallback when message is undefined', () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'UNKNOWN_ERROR' } }
    const result = handler(mockResponse)
    expect(result).toEqual({
      code: 'UNKNOWN_ERROR',
      message: '',
      isBusinessError: true,
    })
  })
})

describe('Error interceptor', () => {
  it('returns NETWORK_ERROR when error.response is undefined', async () => {
    const handler = captured.error!
    const error = { response: undefined }
    const result = handler(error)
    // Error interceptor returns/rejects based on whether 401 retry logic fires.
    // For non-401 cases it resolves with a BusinessError directly.
    await expect(result).resolves.toEqual({
      code: 'NETWORK_ERROR',
      message: '网络不可达，请检查网络连接',
      isBusinessError: true,
    })
  })

  it('returns UNAUTHORIZED for 401', async () => {
    const handler = captured.error!
    const error = { response: { status: 401 } }
    const result = handler(error)
    await expect(result).resolves.toEqual({
      code: 'UNAUTHORIZED',
      message: '登录已过期，请重新登录',
      isBusinessError: true,
    })
  })

  it('returns FORBIDDEN for 403', async () => {
    const handler = captured.error!
    const error = { response: { status: 403 } }
    const result = handler(error)
    await expect(result).resolves.toEqual({
      code: 'FORBIDDEN',
      message: '无权限访问',
      isBusinessError: true,
    })
  })

  it('returns HTTP_ERROR for 500', async () => {
    const handler = captured.error!
    const error = { response: { status: 500 } }
    const result = handler(error)
    await expect(result).resolves.toEqual({
      code: 'HTTP_ERROR',
      message: '请求失败（500）',
      isBusinessError: true,
    })
  })

  it('returns HTTP_ERROR for 404', async () => {
    const handler = captured.error!
    const error = { response: { status: 404 } }
    const result = handler(error)
    await expect(result).resolves.toEqual({
      code: 'HTTP_ERROR',
      message: '请求失败（404）',
      isBusinessError: true,
    })
  })

  it('error handler returns a Promise', async () => {
    const handler = captured.error!
    const error = { response: { status: 500 } }
    const result = handler(error)
    expect(result).toBeInstanceOf(Promise)
    await expect(result).resolves.toBeDefined()
  })

  it('all branches resolve with { code, message, isBusinessError } shape', async () => {
    const handler = captured.error!
    const errors = [
      { response: undefined },
      { response: { status: 401 } },
      { response: { status: 403 } },
      { response: { status: 502 } },
    ]
    for (const err of errors) {
      await expect(handler(err)).resolves.toMatchObject({
        code: expect.any(String),
        message: expect.any(String),
        isBusinessError: true,
      })
    }
  })
})

describe('apiGet', () => {
  it('calls apiClient.get with url', async () => {
    await apiGet('/users')
    expect(apiClient.get).toHaveBeenCalledWith('/users', undefined)
  })

  it('passes config to apiClient.get', async () => {
    const config = { headers: { Authorization: 'Bearer token' } }
    await apiGet('/users', config)
    expect(apiClient.get).toHaveBeenCalledWith('/users', config)
  })

  it('returns unwrapped data when interceptor processes SUCCESS', async () => {
    vi.mocked(apiClient.get).mockImplementationOnce(async () =>
      captured.success!({ data: { code: 'SUCCESS', data: null } } as never),
    )
    const result = await apiGet<null>('/users/1')
    expect(result).toBeNull()
  })
})

describe('apiPost', () => {
  it('calls apiClient.post with url and data', async () => {
    const data = { name: 'test' }
    await apiPost('/users', data)
    expect(apiClient.post).toHaveBeenCalledWith('/users', data, undefined)
  })

  it('passes config to apiClient.post', async () => {
    const config = { timeout: 5000 }
    await apiPost('/users', {}, config)
    expect(apiClient.post).toHaveBeenCalledWith('/users', {}, config)
  })

  it('works without data argument', async () => {
    await apiPost('/users')
    expect(apiClient.post).toHaveBeenCalledWith('/users', undefined, undefined)
  })
})

describe('apiPut', () => {
  it('calls apiClient.put with url and data', async () => {
    const data = { id: 1, name: 'updated' }
    await apiPut('/users/1', data)
    expect(apiClient.put).toHaveBeenCalledWith('/users/1', data, undefined)
  })

  it('passes config to apiClient.put', async () => {
    const config = { headers: { 'X-Custom': 'val' } }
    await apiPut('/users/1', {}, config)
    expect(apiClient.put).toHaveBeenCalledWith('/users/1', {}, config)
  })
})

describe('apiDelete', () => {
  it('calls apiClient.delete with url', async () => {
    await apiDelete('/users/1')
    expect(apiClient.delete).toHaveBeenCalledWith('/users/1', undefined)
  })

  it('passes config to apiClient.delete', async () => {
    const config = { timeout: 3000 }
    await apiDelete('/users/1', config)
    expect(apiClient.delete).toHaveBeenCalledWith('/users/1', config)
  })
})

describe('Integration: wrapper functions & interceptors', () => {
  it('apiGet response is processed by success interceptor', async () => {
    const axiosResponse = { data: { code: 'SUCCESS', data: { id: 1 } } }
    vi.mocked(apiClient.get).mockImplementation(async () => captured.success!(axiosResponse))

    const result = await apiGet<{ id: number }>('/users/1')
    // apiGet try-catch: the interceptor returns body.data directly via apiClient.get,
    // which resolves. apiGet does `return await apiClient.get(...) as T`.
    expect(result).toEqual({ id: 1 })
  })

  it('apiGet error is processed by error interceptor', async () => {
    vi.mocked(apiClient.get).mockImplementation(async () => captured.error!({ response: { status: 500 } }))

    const result = await apiGet('/test')
    // Error interceptor returns BusinessError (Promise that resolves), apiGet
    // awaits it and gets the BusinessError object. Since it's not a thrown error,
    // the try path returns it as T | BusinessError.
    expect(result).toMatchObject({
      code: expect.any(String),
      message: expect.any(String),
      isBusinessError: true,
    })
  })
})
