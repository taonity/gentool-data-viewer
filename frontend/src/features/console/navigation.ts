export type TabKey = 'players' | 'replays' | 'gentool' | 'collection' | 'config' | 'admin' | 'about'
const TAB_KEYS = new Set<TabKey>(['players', 'replays', 'gentool', 'collection', 'config', 'admin', 'about'])

export function browserNavigation(search: string, fallbackTab: TabKey): { tab: TabKey; playerId: string | null; replayId: string | null } {
  const params = new URLSearchParams(search)
  const requestedTab = params.get('tab')
  const tab = requestedTab === null ? fallbackTab : TAB_KEYS.has(requestedTab as TabKey) ? requestedTab as TabKey : 'players'
  const replayId = tab === 'replays' ? params.get('replay')?.trim() || null : null
  const playerId = !replayId && (tab === 'players' || tab === 'replays')
    ? params.get('player')?.trim().toUpperCase() || null
    : null
  return { tab, playerId, replayId }
}

export function consoleTabUrl(currentUrl: string, tab: TabKey, playerId: string | null = null, replayId: string | null = null): string {
  const url = new URL(currentUrl)
  url.searchParams.set('tab', tab)
  if (playerId && (tab === 'players' || tab === 'replays')) url.searchParams.set('player', playerId)
  else url.searchParams.delete('player')
  if (replayId && tab === 'replays') url.searchParams.set('replay', replayId)
  else url.searchParams.delete('replay')
  return `${url.pathname}${url.search}${url.hash}`
}
