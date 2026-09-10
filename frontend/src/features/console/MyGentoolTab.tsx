'use client'

import { useCallback, useDeferredValue, useEffect, useRef, useState } from 'react'
import { Clapperboard, Link2, Loader2, Search, Unlink, UserRoundSearch } from 'lucide-react'
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
  onNavigateToPlayer,
  onNavigateToReplays,
  onError,
}: {
  active: boolean
  forceLoading: boolean
  onNavigateToPlayer: (playerId: string) => void
  onNavigateToReplays: (playerId: string) => void
  onError: (message: string) => void
}) {
  const [dashboard, setDashboard] = useState<ReplayRescanDashboard | null>(null)
  const [dashboardLoading, setDashboardLoading] = useState(true)
  const [query, setQuery] = useState('')
  const deferredQuery = useDeferredValue(query)
  const [players, setPlayers] = useState<CpuPlayer[] | null>(null)
  const [playersLoading, setPlayersLoading] = useState(false)
  const [selectedPlayer, setSelectedPlayer] = useState<CpuPlayer | null>(null)
  const [linking, setLinking] = useState(false)
  const [unlinkingPlayerId, setUnlinkingPlayerId] = useState<string | null>(null)
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

  const loadPlayers = useCallback(async (search: string) => {
    const request = ++playerRequest.current
    setPlayersLoading(true)
    try {
      const result = await consoleApi.listCpuPlayers(
        0,
        20,
        search || undefined,
        'all',
        'mainName',
        'asc',
      )
      if (request === playerRequest.current) setPlayers(result.content)
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
    if (active && !forceLoading) void loadPlayers(deferredQuery.trim())
  }, [active, deferredQuery, forceLoading, loadPlayers])

  const linkPlayer = async () => {
    if (!selectedPlayer) return
    setLinking(true)
    try {
      await consoleApi.claimGentoolPlayer(selectedPlayer.playerId)
      setSelectedPlayer(null)
      await Promise.all([loadDashboard(), loadPlayers(deferredQuery.trim())])
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
      await Promise.all([loadDashboard(), loadPlayers(deferredQuery.trim())])
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to remove GenTool link.')
    } finally {
      setUnlinkingPlayerId(null)
    }
  }

  const currentLinks = dashboard?.links ?? []
  const maxLinkedPlayers = dashboard?.maxLinkedPlayers ?? 0
  const atCapacity = dashboard !== null && currentLinks.length >= maxLinkedPlayers

  return (
    <div className="grid gap-6 lg:grid-cols-[minmax(260px,0.75fr)_minmax(360px,1.25fr)]">
      <section className="min-w-0 border-b pb-6 lg:border-r lg:border-b-0 lg:pr-6">
        <div className="mb-4 flex items-center gap-2">
          <Link2 className="size-4 text-muted-foreground" />
          <h2 className="font-heading text-base font-medium">Your linked players</h2>
          {dashboard && (
            <Badge variant={atCapacity ? 'secondary' : 'outline'}>
              {currentLinks.length} / {maxLinkedPlayers}
            </Badge>
          )}
        </div>
        {forceLoading || dashboardLoading ? (
          <div className="space-y-3">
            <Skeleton className="h-5 w-40" />
            <Skeleton className="h-4 w-28" />
            <Skeleton className="h-8 w-56" />
          </div>
        ) : currentLinks.length > 0 ? (
          <div className="divide-y rounded-lg border">
            {currentLinks.map((link) => (
              <div key={link.playerId} className="space-y-3 p-3">
                <div>
                  <div className="truncate font-medium">{link.playerName ?? link.playerId}</div>
                  <div className="mt-0.5 font-mono text-xs text-muted-foreground">{link.playerId}</div>
                </div>
                <div className="flex flex-wrap gap-1">
                  <Button variant="ghost" size="xs" onClick={() => onNavigateToPlayer(link.playerId)}>
                    <UserRoundSearch />
                    Player
                  </Button>
                  <Button variant="ghost" size="xs" onClick={() => onNavigateToReplays(link.playerId)}>
                    <Clapperboard />
                    Replays
                  </Button>
                  <Button
                    variant="ghost"
                    size="xs"
                    disabled={linking || unlinkingPlayerId !== null}
                    onClick={() => void unlinkPlayer(link)}
                  >
                    {unlinkingPlayerId === link.playerId ? <Loader2 className="animate-spin" /> : <Unlink />}
                    Remove
                  </Button>
                </div>
              </div>
            ))}
          </div>
        ) : (
          <div className="space-y-2 text-sm text-muted-foreground">
            <p>No GenTool players are linked to your Discord account.</p>
            <p>Choose your player from the search results.</p>
          </div>
        )}
      </section>

      <section className="min-w-0">
        <div className="mb-4">
          <h2 className="font-heading text-base font-medium">Find your GenTool player</h2>
        </div>
        <div className="relative">
          <Search className="pointer-events-none absolute top-1/2 left-2.5 size-4 -translate-y-1/2 text-muted-foreground" />
          <Input
            className="pl-9"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
            placeholder="Player name or GenTool ID"
          />
          {playersLoading && (
            <Loader2 className="pointer-events-none absolute top-1/2 right-3 size-4 -translate-y-1/2 animate-spin text-muted-foreground" />
          )}
        </div>
        <div className="mt-3 max-h-72 overflow-y-auto rounded-lg border" aria-busy={playersLoading}>
          {players === null && <div className="space-y-2 p-3">{Array.from({ length: 4 }).map((_, index) => <Skeleton key={index} className="h-11 w-full" />)}</div>}
          {players?.length === 0 && (
            <div className="flex h-24 items-center justify-center text-sm text-muted-foreground">No players found.</div>
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
                className={`flex w-full items-center gap-3 border-b px-3 py-2 text-left last:border-b-0 hover:bg-muted/50 disabled:cursor-not-allowed disabled:opacity-50 ${selectedPlayer?.playerId === player.playerId ? 'bg-muted' : ''}`}
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
        <div className="mt-4 flex flex-wrap items-center gap-3 border-t pt-4">
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