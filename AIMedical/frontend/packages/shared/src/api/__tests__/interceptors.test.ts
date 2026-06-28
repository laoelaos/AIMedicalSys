import { describe, it, expect, vi, beforeEach } from 'vitest'

const captured = vi.hoisted(() => {
  const store: { success?: Function; error?: Function } = {}
  return store
})

vi.mock('axios', () => {
  const mockInstance = {
    interceptors: {
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
    expect(result.data).toEqual({ id: 1 })
  })

  it('unwraps nested data from SUCCESS response', () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'SUCCESS', data: { nested: 'value' } } }
    const result = handler(mockResponse)
    expect(result.data).toEqual({ nested: 'value' })
  })

  it('unwraps array data from SUCCESS response', () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'SUCCESS', data: ['a', 'b'] } }
    const result = handler(mockResponse)
    expect(result.data).toEqual(['a', 'b'])
  })

  it('rejects with BusinessError when code is not SUCCESS', async () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'BUSINESS_ERROR', message: '业务异常' } }
    await expect(handler(mockResponse)).rejects.toEqual({
      code: 'BUSINESS_ERROR',
      message: '业务异常',
      isBusinessError: true,
    })
  })

  it('rejects with empty message fallback when message is undefined', async () => {
    const handler = captured.success!
    const mockResponse = { data: { code: 'UNKNOWN_ERROR' } }
    await expect(handler(mockResponse)).rejects.toEqual({
      code: 'UNKNOWN_ERROR',
      message: '',
      isBusinessError: true,
    })
  })
})

describe('Error interceptor', () => {
  it('rejects with NETWORK_ERROR when error.response is undefined', async () => {
    const handler = captured.error!
    const error = { response: undefined }
    await expect(handler(error)).rejects.toEqual({
      code: 'NETWORK_ERROR',
      message: '网络不可达，请检查网络连接',
      isBusinessError: true,
    })
  })

  it('rejects with UNAUTHORIZED for 401', async () => {
    const handler = captured.error!
    const error = { response: { status: 401 } }
    await expect(handler(error)).rejects.toEqual({
      code: 'UNAUTHORIZED',
      message: '登录已过期，请重新登录',
      isBusinessError: true,
    })
  })

  it('rejects with FORBIDDEN for 403', async () => {
    const handler = captured.error!
    const error = { response: { status: 403 } }
    await expect(handler(error)).rejects.toEqual({
      code: 'FORBIDDEN',
      message: '无权限访问',
      isBusinessError: true,
    })
  })

  it('rejects with HTTP_ERROR for 500', async () => {
    const handler = captured.error!
    const error = { response: { status: 500 } }
    await expect(handler(error)).rejects.toEqual({
      code: 'HTTP_ERROR',
      message: '请求失败（500）',
      isBusinessError: true,
    })
  })

  it('rejects with HTTP_ERROR for 404', async () => {
    const handler = captured.error!
    const error = { response: { status: 404 } }
    await expect(handler(error)).rejects.toEqual({
      code: 'HTTP_ERROR',
      message: '请求失败（404）',
      isBusinessError: true,
    })
  })

  it('error handler returns rejected promise (not synchronously thrown)', async () => {
    const handler = captured.error!
    const error = { response: { status: 500 } }
    const result = handler(error)
    await expect(result).rejects.toBeDefined()
  })

  it('all branches reject with { code, message, isBusinessError } shape', async () => {
    const handler = captured.error!
    const errors = [
      { response: undefined },
      { response: { status: 401 } },
      { response: { status: 403 } },
      { response: { status: 502 } },
    ]
    for (const err of errors) {
      await expect(handler(err)).rejects.toMatchObject({
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
    expect(result).toEqual({ id: 1 })
  })

  it('apiGet error is processed by error interceptor', async () => {
    vi.mocked(apiClient.get).mockImplementation(async () => captured.error!({ response: undefined }))

    const result = await apiGet('/test')
    expect(result).toEqual({ code: 'NETWORK_ERROR', message: '网络不可达，请检查网络连接', isBusinessError: true })
  })
})
