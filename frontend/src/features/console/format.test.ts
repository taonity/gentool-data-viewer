import { describe, expect, it } from 'vitest'
import { formatExactTime } from './format'

describe('formatExactTime', () => {
  it('shows the absolute UTC date and time including seconds', () => {
    expect(formatExactTime('2026-09-18T18:04:27+03:00')).toBe('2026-09-18 15:04:27 UTC')
  })

  it('handles absent or invalid timestamps', () => {
    expect(formatExactTime(null)).toBe('\u2014')
    expect(formatExactTime(undefined)).toBe('\u2014')
    expect(formatExactTime('invalid')).toBe('invalid')
  })
})