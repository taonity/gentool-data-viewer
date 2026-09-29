import { afterEach, expect, it, vi } from 'vitest'
import { fetchWithTimeout } from './clientApi'

afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

it('creates a local 504 after ten seconds even when the server sends no error', async () => {
  vi.useFakeTimers()
  const abort = vi.fn()
  vi.stubGlobal('fetch', vi.fn((_input: RequestInfo | URL, init: RequestInit) => new Promise<Response>((_resolve, reject) => {
    init.signal?.addEventListener('abort', () => {
      abort()
      reject(new DOMException('aborted', 'AbortError'))
    })
  })))

  const pending = fetchWithTimeout('/api/console/replays?q=example', { timeoutMs: 10_000 })
  await vi.advanceTimersByTimeAsync(9_999)
  expect(abort).not.toHaveBeenCalled()
  await vi.advanceTimersByTimeAsync(1)
  expect((await pending).status).toBe(504)
  expect(abort).toHaveBeenCalledOnce()
})
