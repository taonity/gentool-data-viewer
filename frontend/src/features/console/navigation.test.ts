import { describe, expect, it } from 'vitest'
import { browserNavigation, consoleTabUrl } from './navigation'

describe('console navigation', () => {
  it('restores valid tabs and falls back predictably', () => {
    expect(browserNavigation('', 'admin').tab).toBe('admin')
    expect(browserNavigation('?tab=config', 'players').tab).toBe('config')
    expect(browserNavigation('?tab=invalid', 'admin').tab).toBe('players')
  })
  it('preserves replay/player deep links only where they apply', () => {
    expect(browserNavigation('?tab=players&player=abc', 'players')).toEqual({ tab: 'players', playerId: 'ABC', replayId: null })
    expect(browserNavigation('?tab=replays&replay=123&player=abc', 'players')).toEqual({ tab: 'replays', playerId: null, replayId: '123' })
    expect(browserNavigation('?tab=config&player=abc', 'players').playerId).toBeNull()
  })
  it('preserves unrelated query parameters and hash while removing stale record links', () => {
    expect(consoleTabUrl('https://example.test/?tab=replays&player=ABC&replay=123&view=compact#details', 'admin'))
      .toBe('/?tab=admin&view=compact#details')
    expect(consoleTabUrl('https://example.test/', 'players', 'ABC')).toBe('/?tab=players&player=ABC')
  })
})
