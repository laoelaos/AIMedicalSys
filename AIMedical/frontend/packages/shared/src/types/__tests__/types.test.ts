import { describe, it, expect } from 'vitest'
import type { ApiSuccess, ApiError, ApiResponse, ApiResult } from '../../types'

describe('ApiSuccess<T>', () => {
  it('creates a SUCCESS branch with data', () => {
    const success: ApiSuccess<string> = { code: 'SUCCESS', data: 'test' }
    expect(success.code).toBe('SUCCESS')
    expect(success.data).toBe('test')
  })

  it('accepts different data types', () => {
    const num: ApiSuccess<number> = { code: 'SUCCESS', data: 42 }
    expect(num.data).toBe(42)

    const obj: ApiSuccess<{ id: number }> = { code: 'SUCCESS', data: { id: 1 } }
    expect(obj.data.id).toBe(1)
  })
})

describe('ApiError', () => {
  it('creates a NETWORK_ERROR branch', () => {
    const err: ApiError = { code: 'NETWORK_ERROR', message: '网络不可达' }
    expect(err.code).toBe('NETWORK_ERROR')
    expect(err.message).toBe('网络不可达')
  })

  it('creates a UNAUTHORIZED branch', () => {
    const err: ApiError = { code: 'UNAUTHORIZED', message: '登录已过期' }
    expect(err.code).toBe('UNAUTHORIZED')
  })

  it('creates a FORBIDDEN branch', () => {
    const err: ApiError = { code: 'FORBIDDEN', message: '无权限访问' }
    expect(err.code).toBe('FORBIDDEN')
  })

  it('creates a HTTP_ERROR branch', () => {
    const err: ApiError = { code: 'HTTP_ERROR', message: '请求失败（500）' }
    expect(err.code).toBe('HTTP_ERROR')
  })

  it('uses as const assertion for literal type preservation', () => {
    const err: ApiError = { code: 'NETWORK_ERROR' as const, message: 'test' }
    expect(err.code).toBe('NETWORK_ERROR')
  })
})

describe('ApiResponse<T> discriminated union', () => {
  it('narrows to SUCCESS branch via code check', () => {
    const res: ApiResponse<string> = { code: 'SUCCESS', data: 'hello' }
    if (res.code === 'SUCCESS') {
      expect(res.data).toBe('hello')
    }
  })

  it('narrows to error branch via code check', () => {
    const res: ApiResponse<string> = { code: 'NETWORK_ERROR', message: 'fail' }
    if (res.code !== 'SUCCESS') {
      expect(res.message).toBeDefined()
    }
  })

  it('handles UNAUTHORIZED branch in discriminated union', () => {
    const res: ApiResponse<unknown> = { code: 'UNAUTHORIZED', message: 'expired' }
    if (res.code === 'UNAUTHORIZED') {
      expect(res.message).toBe('expired')
    }
  })

  it('handles FORBIDDEN branch in discriminated union', () => {
    const res: ApiResponse<unknown> = { code: 'FORBIDDEN', message: 'no permission' }
    if (res.code === 'FORBIDDEN') {
      expect(res.message).toBe('no permission')
    }
  })

  it('handles HTTP_ERROR branch in discriminated union', () => {
    const res: ApiResponse<unknown> = { code: 'HTTP_ERROR', message: '请求失败（404）' }
    if (res.code === 'HTTP_ERROR') {
      expect(res.message).toBe('请求失败（404）')
    }
  })
})

describe('ApiResult backward compatibility', () => {
  it('ApiResult interface is still usable', () => {
    const result: ApiResult = { code: 'SUCCESS', data: 'test' }
    expect(result.code).toBe('SUCCESS')
  })
})
