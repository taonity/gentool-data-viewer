'use client'

import { useCallback, useDeferredValue, useEffect, useRef, useState } from 'react'
import { Clapperboard, Link2, Loader2, RotateCw, Search, Unlink, UserRoundSearch } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { consoleApi } from './api'
import { formatTime } from './format'
import type { CpuPlayer, GentoolLink, ReplayRescanDashboard } from './types'

export function MyGentoolTab({
  active,
  forceLoading,
  canRefresh,
  onNavigateToPlayer,
  onNavigateToReplays,
  onError,
}: {
  active: boolean
  forceLoading: boolean
  canRefresh: boolean
  onNavigateToPlayer: (playerId: string) => void
  onNavigateToReplays: (playerId: string) => void
  onError: (message: string) => void
}) {
  const [dashboard, setDashboard] = useState<ReplayRescanDashboard | null>(null)
  const [dashboardLoading, setDashboardLoading] = useState(true)
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)
  const [exactMatch, setExactMatch] = useState(false)
  const [players, setPlayers] = useState<CpuPlayer[] | null>(null)
  const [playerTotal, setPlayerTotal] = useState(0)
  const [playersLoading, setPlayersLoading] = useState(false)
  const [selectedPlayer, setSelectedPlayer] = useState<CpuPlayer | null>(null)
  const [linking, setLinking] = useState(false)
  const [unlinkingPlayerId, setUnlinkingPlayerId] = useState<string | null>(null)
  const [refreshingPlayerId, setRefreshingPlayerId] = useState<string | null>(null)
  const playerRequest = useRef(0)

  const loadDashboard = useCallback(async () => {
    setDashboardLoading(true)
    try {
      setDashboard(await consoleApi.getReplayRescanDashboard())
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to load your GenTool account.')
    } finally {
      setDashboardLoading(false)
    }
  }, [onError])

  const loadPlayers = useCallback(async (search: string, exact: boolean) => {
    const request = ++playerRequest.current
    if (!search.trim()) {
      setPlayers([])
      setPlayerTotal(0)
      setPlayersLoading(false)
      return
    }
    setPlayersLoading(true)
    try {
      const result = await consoleApi.listCpuPlayers(
        0,
        20,
        search || undefined,
        'all',
        'mainName',
        'asc',
        undefined,
        undefined,
        exact && Boolean(search),
      )
      if (request === playerRequest.current) {
        setPlayers(result.content)
        setPlayerTotal(result.totalElements)
      }
    } catch {
      if (request === playerRequest.current) onError('Failed to search GenTool players.')
    } finally {
      if (request === playerRequest.current) setPlayersLoading(false)
    }
  }, [onError])

  useEffect(() => {
    if (active && !forceLoading) void loadDashboard()
  }, [active, forceLoading, loadDashboard])

  useEffect(() => {
    if (active && !forceLoading) void loadPlayers(deferredQuery.trim(), exactMatch)
  }, [active, deferredQuery, exactMatch, forceLoading, loadPlayers])

  const hasActiveScans = dashboard?.history.some((job) => job.status === 'QUEUED' || job.status === 'RUNNING') ?? false
  useEffect(() => {
    if (!active || forceLoading || !hasActiveScans) return
    const timer = window.setInterval(() => void loadDashboard(), 3000)
    return () => window.clearInterval(timer)
  }, [active, forceLoading, hasActiveScans, loadDashboard])

  const refreshInactivePlayer = async (playerId: string) => {
    setRefreshingPlayerId(playerId)
    try {
      await consoleApi.requestReplayRescan(playerId)
      await loadDashboard()
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to refresh player data.')
    } finally {
      setRefreshingPlayerId(null)
    }
  }

  const linkPlayer = async () => {
    if (!selectedPlayer) return
    setLinking(true)
    try {
      await consoleApi.claimGentoolPlayer(selectedPlayer.playerId)
      setSelectedPlayer(null)
      await Promise.all([loadDashboard(), loadPlayers(deferredQuery.trim(), exactMatch)])
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to link GenTool player.')
    } finally {
      setLinking(false)
    }
  }

  const unlinkPlayer = async (link: GentoolLink) => {
    if (!confirm(`Remove your link to ${link.playerName ?? link.playerId}?`)) return
    setUnlinkingPlayerId(link.playerId)
    try {
      await consoleApi.unlinkMyGentoolPlayer(link.playerId)
      await Promise.all([loadDashboard(), loadPlayers(deferredQuery.trim(), exactMatch)])
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to remove GenTool link.')
    } finally {
      setUnlinkingPlayerId(null)
    }
  }

  const currentLinks = dashboard?.links ?? []
  const maxLinkedPlayers = dashboard?.maxLinkedPlayers ?? 0
  const atCapacity = dashboard !== null && currentLinks.length >= maxLinkedPlayers
  const additionalPlayers = Math.max(playerTotal - (players?.length ?? 0), 0)

  return (
    <div className="gentool-panel console-inset grid gap-4 sm:gap-6 lg:grid-cols-[minmax(260px,0.75fr)_minmax(360px,1.25fr)]">
      <section className="gentool-section min-w-0 border-b pb-6 lg:border-r lg:border-b-0 lg:pr-6">
        <div className="gentool-heading mb-4 flex flex-wrap items-center gap-2">
          <Link2 className="size-4 text-muted-foreground" />
          <h2 className="font-heading text-base font-medium">Your linked players</h2>
          {dashboard && (
            <>
              <Badge variant={atCapacity ? 'secondary' : 'outline'}>
                {currentLinks.length} / {maxLinkedPlayers}
              </Badge>
              <span className="text-xs text-muted-foreground">
                {Math.max(0, dashboard.otherDailyLimit - dashboard.otherUsedToday)} of {dashboard.otherDailyLimit} other-player refreshes left
              </span>
            </>
          )}
        </div>
        {forceLoading || (dashboardLoading && dashboard === null) ? (
          <div className="space-y-3">
            <Skeleton className="h-5 w-40" />
            <Skeleton className="h-4 w-28" />
            <Skeleton className="h-8 w-56" />
          </div>
        ) : currentLinks.length > 0 ? (
          <div className="linked-players divide-y rounded-lg border">
            {currentLinks.map((link) => (
              <div key={link.playerId} className="linked-player space-y-3 p-3">
                <div className="min-w-0 flex-1">
                  <div className="truncate font-medium">{link.playerName ?? link.playerId}</div>
                  <div className="mt-0.5 font-mono text-xs text-muted-foreground">{link.playerId}</div>
                  {link.playerName === null && (
                    <p className="mt-1 text-xs text-muted-foreground">Inactive: no player data within the retention period. Your link is preserved.</p>
                  )}
                </div>
                <div className="linked-player-actions flex flex-wrap gap-1">
                  {link.playerName === null && canRefresh && link.status === 'APPROVED' && (
                    <Button variant="ghost" size="xs" disabled={refreshingPlayerId !== null || hasActiveScans} aria-label={`Scan recent replays for ${link.playerId}`} title="Scan recent replays"
                      onClick={() => void refreshInactivePlayer(link.playerId)}>
                      {refreshingPlayerId === link.playerId ? <Loader2 className="animate-spin" /> : <RotateCw />}
                      <span className="linked-action-label">Scan recent replays</span>
                    </Button>
                  )}
                  <Button variant="ghost" size="xs" aria-label={`View player ${link.playerName ?? link.playerId}`} title="View player" onClick={() => onNavigateToPlayer(link.playerId)}>
                    <UserRoundSearch />
                    <span className="linked-action-label">Player</span>
                  </Button>
                  <Button variant="ghost" size="xs" aria-label={`View replays for ${link.playerName ?? link.playerId}`} title="View replays" onClick={() => onNavigateToReplays(link.playerId)}>
                    <Clapperboard />
                    <span className="linked-action-label">Replays</span>
                  </Button>
                  <Button
                    variant="ghost"
                    size="xs"
                    aria-label={`Remove link to ${link.playerName ?? link.playerId}`}
                    title="Remove link"
                    disabled={linking || unlinkingPlayerId !== null}
                    onClick={() => void unlinkPlayer(link)}
                  >
                    {unlinkingPlayerId === link.playerId ? <Loader2 className="animate-spin" /> : <Unlink />}
                    <span className="linked-action-label">Remove</span>
                  </Button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="gentool-message space-y-2 text-sm text-muted-foreground">
            <p>No GenTool players are linked to your Discord account.</p>
            <p>Choose your player from the search results.</p>
          </div>
        )}
      </section>

      <section className="gentool-section min-w-0">
        <div className="gentool-heading mb-4">
          <h2 className="font-heading text-base font-medium">Find your GenTool player</h2>
        </div>
        <div className="gentool-search-controls flex gap-2">
          <div className="relative min-w-0 flex-1">
            <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              className="pl-9"
              value={query}
              onChange={(event) => {
                setQuery(event.target.value)
                if (!event.target.value.trim()) {
                  ++playerRequest.current
                  setExactMatch(false)
                  setPlayers([])
                  setPlayerTotal(0)
                  setPlayersLoading(false)
                  setSelectedPlayer(null)
                }
              }}
              placeholder="Player name or GenTool ID"
            />
            {playersLoading && (
              <Loader2 className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 animate-spin text-muted-foreground" />
            )}
          </div>
          <label className="flex h-8 shrink-0 cursor-pointer items-center gap-1.5 text-xs text-muted-foreground">
            <input
              type="checkbox"
              className="size-4 accent-primary"
              aria-label="Exact match"
              checked={exactMatch}
              disabled={!query.trim()}
              onChange={(event) => setExactMatch(event.target.checked)}
            />
            Exact
          </label>
        </div>
        <div className="profile-search-results mt-3 max-h-72 overflow-y-auto rounded-lg border" aria-busy={playersLoading}>
          {players === null && playersLoading && <div className="space-y-2 p-3">{Array.from({ length: 4 }).map((_, index) => <Skeleton key={index} className="h-11 w-full" />)}</div>}
          {!query.trim() && (
            <div className="profile-search-empty flex h-24 items-center justify-center text-sm text-muted-foreground">Search by player name or GenTool ID.</div>
          )}
          {query.trim() && !playersLoading && !players?.length && (
            <div className="profile-search-empty flex h-24 items-center justify-center text-sm text-muted-foreground">No players found.</div>
          )}
          {players?.map((player) => {
            const isCurrent = currentLinks.some((link) => link.playerId === player.playerId)
            const unavailable = Boolean(player.discordUser && !isCurrent)
            const latestLineup = player.latestMatch?.teams
              .map((team) => team.join(' + '))
              .join(' vs ')
            return (
              <button
                type="button"
                key={player.playerId}
                disabled={unavailable || isCurrent}
                aria-pressed={selectedPlayer?.playerId === player.playerId}
                className={`profile-search-option flex w-full items-center gap-3 border-b px-3 py-2 text-left last:border-b-0 hover:bg-muted/50 disabled:cursor-not-allowed disabled:opacity-50 ${selectedPlayer?.playerId === player.playerId ? 'bg-muted' : ''}`}
                onClick={() => setSelectedPlayer(player)}
              >
                <span className="min-w-0 flex-1">
                  <span className="block truncate text-sm font-medium">{player.mainName}</span>
                  <span className="block truncate font-mono text-xs text-muted-foreground">{player.playerId}</span>
                  {player.latestMatch && latestLineup && (
                    <span
                      className="mt-1 block truncate text-xs text-muted-foreground"
                      title={`${formatTime(player.latestMatch.matchAt)} · ${latestLineup}`}
                    >
                      Latest: {formatTime(player.latestMatch.matchAt)} · {latestLineup}
                    </span>
                  )}
                </span>
                {(isCurrent || unavailable) && (
                  <Badge variant={isCurrent ? 'secondary' : 'outline'}>{isCurrent ? 'Current' : 'Linked'}</Badge>
                )}
              </button>
            )
          })}
        </div>
        {players && players.length > 0 && (
          <p className="gentool-message mt-1.5 text-xs text-muted-foreground" aria-live="polite">
            {players.length} shown · {additionalPlayers} more {additionalPlayers === 1 ? 'match' : 'matches'}
          </p>
        )}
        <div className="profile-search-footer mt-4 flex flex-wrap items-center gap-3 border-t pt-4">
          <Button disabled={!selectedPlayer || linking || unlinkingPlayerId !== null || atCapacity} onClick={() => void linkPlayer()}>
            {linking ? <Loader2 className="animate-spin" /> : <Link2 />}
            Link player
          </Button>
          <span className="min-w-0 truncate text-sm text-muted-foreground" aria-live="polite">
            {atCapacity
              ? `Link limit reached (${currentLinks.length} of ${maxLinkedPlayers}).`
              : selectedPlayer
                ? `${selectedPlayer.mainName} · ${selectedPlayer.playerId}`
                : 'Select your player.'}
          </span>
        </div>
      </section>
    </div>
  )
}
