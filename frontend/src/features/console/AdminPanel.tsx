'use client'

import { useCallback, useDeferredValue, useEffect, useRef, useState } from 'react'
import Image from 'next/image'
import { Check, Link2, Loader2, Search, Unlink, X } from 'lucide-react'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { Skeleton } from '@/components/ui/skeleton'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { consoleApi } from './api'
import { DataTab, type Column } from './DataTab'
import { RecordTimestamp } from './RecordTimestamp'
import { pageRows } from './pageRows'
import { formatExactTime, formatTime } from './format'
import { DatePeriodFilter, type DatePeriod } from './DatePeriodFilter'
import type { AccessInfo, AdminDiscordUserOption, AdminGentoolPlayerOption, AuditLog, ConsoleRole, PendingRequest, UserSummary } from './types'

const AUDIT_COLUMNS: Column<AuditLog>[] = [
  {
    key: 'occurredAt',
    label: 'When',
    sortKey: 'occurredAt',
    initialSortDirection: 'desc',
    value: (a) => a.occurredAt,
    render: (a) => <RecordTimestamp value={a.occurredAt} />,
    cellClassName: 'whitespace-nowrap text-muted-foreground tabular-nums',
    defaultWidth: 180,
    skeleton: 'w-[85%]',
  },
  {
    key: 'action',
    mobilePrimary: true,
    label: 'Action',
    sortKey: 'action',
    value: (a) => a.action,
    cellClassName: 'truncate font-medium',
    defaultWidth: 180,
    searchKey: 'action',
  },
  {
    key: 'targetType',
    label: 'Target',
    sortKey: 'targetType',
    value: (a) => a.targetType,
    cellClassName: 'truncate',
    defaultWidth: 140,
    searchKey: 'targetType',
  },
  {
    key: 'targetId',
    label: 'Record id',
    sortKey: 'targetId',
    value: (a) => a.targetId ?? '',
    render: (a) => <span className="font-mono text-xs">{a.targetId ?? '—'}</span>,
    cellClassName: 'truncate',
    defaultWidth: 230,
    searchKey: 'targetId',
  },
  {
    key: 'actorUserId',
    label: 'Actor',
    sortKey: 'actorUserId',
    value: (a) => a.actorUserId,
    cellClassName: 'truncate',
    defaultWidth: 210,
    searchKey: 'actorUserId',
  },
]

export function AdminPanel({
  active = true,
  access,
  forceLoading = false,
  onError,
  onPendingCountChange,
  refreshKey = 0,
}: {
  active?: boolean
  access: AccessInfo
  forceLoading?: boolean
  onError: (message: string) => void
  onPendingCountChange?: (count: number) => void
  refreshKey?: number
}) {
  const [datePeriod, setDatePeriod] = useState<DatePeriod>({ startDate: '', endDate: '' })
  const [grantRoles, setGrantRoles] = useState<Record<string, ConsoleRole>>({})
  const [busyId, setBusyId] = useState<string | null>(null)
  const [accessRevision, setAccessRevision] = useState(0)
  const refreshAccess = useCallback(() => setAccessRevision(previous => previous + 1), [])
  const tableRefreshKey = refreshKey + accessRevision

  const decide = async (req: PendingRequest, approve: boolean) => {
    setBusyId(req.googleId)
    try {
      if (approve) {
        await consoleApi.approveRequest(req.googleId, grantRoles[req.googleId] ?? req.requestedRole ?? 'VIEWER')
      } else {
        await consoleApi.rejectRequest(req.googleId)
      }
      refreshAccess()
    } catch {
      onError(approve ? 'Failed to approve request.' : 'Failed to reject request.')
    } finally {
      setBusyId(null)
    }
  }

  const decisionButtons = (request: PendingRequest) => (
    <div className="flex justify-end gap-1">
      <Button size="sm" disabled={busyId !== null} onClick={() => void decide(request, true)}
        aria-label={`Approve access for ${request.displayName}`} title="Approve access">
        <Check className="lg:hidden" /><span className="hidden lg:inline">Approve</span>
      </Button>
      <Button size="sm" variant="destructive" disabled={busyId !== null} onClick={() => void decide(request, false)}
        aria-label={`Reject access for ${request.displayName}`} title="Reject access">
        <X className="lg:hidden" /><span className="hidden lg:inline">Reject</span>
      </Button>
    </div>
  )

  return (
    <div className="admin-panel flex min-w-0 flex-col gap-4 sm:gap-6">
      {access.accessRequestsEnabled && <Card>
        <CardHeader>
          <CardTitle className="text-base">Pending access requests</CardTitle>
        </CardHeader>
        <CardContent>
          <DataTab<PendingRequest> active={active}
            columns={[
              ...REQUEST_COLUMNS,
              {
                key: 'grant', label: 'Grant', sortKey: 'grant', sortable: false,
                value: row => ROLE_LABELS[grantRoles[row.googleId] ?? row.requestedRole ?? 'VIEWER'],
                defaultWidth: 150, interactive: true, mobileCompact: true,
                render: row => (
                  <Select items={{ VIEWER: 'Viewer', EDITOR: 'Editor' }}
                    value={grantRoles[row.googleId] ?? row.requestedRole ?? 'VIEWER'}
                    onValueChange={value => value && setGrantRoles(previous => ({ ...previous, [row.googleId]: value as ConsoleRole }))}>
                    <SelectTrigger size="sm" className="w-full" aria-label={`Grant role for ${row.displayName}`} disabled={busyId !== null}>
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="VIEWER">Viewer</SelectItem>
                      <SelectItem value="EDITOR">Editor</SelectItem>
                    </SelectContent>
                  </Select>
                ),
              },
            ]}
            columnWidthsKey="pending-access-requests"
            defaultSortKey="displayName" defaultSortDirection="asc"
            rowKey={row => row.googleId}
            rowActions={decisionButtons} rowActionsWidth={180} stickyActions={false}
            load={async (page, size, q, field, sort, direction, exact) => {
              const rows = await consoleApi.listPendingRequests()
              onPendingCountChange?.(rows.length)
              return pageRows(rows, REQUEST_COLUMNS, row => row.googleId, page, size, q, field, sort, direction, exact)
            }}
            emptyLabel="No pending requests." forceLoading={forceLoading} onError={onError}
            refreshToken={tableRefreshKey}
          />
        </CardContent>
      </Card>}

      <PlayerLinksCard active={active} forceLoading={forceLoading} onError={onError} onChanged={refreshAccess} />

      <UsersCard
        active={active}
        access={access}
        forceLoading={forceLoading}
        onError={onError}
        refreshKey={tableRefreshKey}
        onAccessChange={refreshAccess}
      />

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Audit log</CardTitle>
          <p className="text-xs text-muted-foreground">
            Retained for 2 weeks. Records what changed and by whom — never the changed data.
          </p>
        </CardHeader>
        <CardContent>
          <div className="flex min-w-0 flex-col gap-3">
            <DatePeriodFilter value={datePeriod} onChange={setDatePeriod} active={active} forceLoading={forceLoading} label="Audit dates (UTC)"
              loadRange={consoleApi.getAuditDateRange} refreshToken={tableRefreshKey} onError={onError} />
            <DataTab<AuditLog> active={active}
              columns={AUDIT_COLUMNS}
              columnWidthsKey="audit-log"
              showRowNumbers
              expandOnRowClick
              expand={row => <RecordDetails values={{ 'Record ID': row.id, When: formatExactTime(row.occurredAt), Action: row.action, Target: row.targetType, 'Target ID': row.targetId ?? '\u2014', Actor: row.actorUserId }} />}
              defaultSortKey="occurredAt"
              defaultSortDirection="desc"
              rowKey={(a) => a.id}
              filterKey={`${datePeriod.startDate}:${datePeriod.endDate}`}
              load={(page, size, q, field, sort, direction, exact) =>
                consoleApi.listAuditLogs(page, size, q, field, sort, direction, exact, datePeriod.startDate, datePeriod.endDate)
              }
              emptyLabel="No audit entries."
              forceLoading={forceLoading}
              onError={onError}
              refreshToken={tableRefreshKey}
            />
          </div>
        </CardContent>
      </Card>
    </div>
  )
}

function ProfileAvatar({ name, pictureUrl }: { name: string; pictureUrl: string | null }) {
  const initials = name
    .split(/\s+/)
    .filter(Boolean)
    .slice(0, 2)
    .map((part) => part[0])
    .join('')
    .toUpperCase()
  return (
    <span
      aria-hidden="true"
      className="relative flex size-8 shrink-0 items-center justify-center overflow-hidden rounded-full bg-muted text-[10px] font-semibold text-muted-foreground ring-1 ring-border"
    >
      {initials}
      {pictureUrl && (
        <Image
          src={pictureUrl}
          alt=""
          width={32}
          height={32}
          unoptimized
          className="absolute inset-0 size-full object-cover"
          onError={(event) => { event.currentTarget.hidden = true }}
        />
      )}
    </span>
  )
}

function PlayerLinksCard({
  active,
  forceLoading,
  onError,
  onChanged,
}: {
  active: boolean
  forceLoading: boolean
  onError: (message: string) => void
  onChanged: () => void
}) {
  const [userQuery, setUserQuery] = useState('')
  const [playerQuery, setPlayerQuery] = useState('')
  const deferredUserQuery = useDeferredValue(userQuery)
  const deferredPlayerQuery = useDeferredValue(playerQuery)
  const [exactUserMatch, setExactUserMatch] = useState(false)
  const [exactPlayerMatch, setExactPlayerMatch] = useState(false)
  const [users, setUsers] = useState<AdminDiscordUserOption[] | null>(null)
  const [players, setPlayers] = useState<AdminGentoolPlayerOption[] | null>(null)
  const [playerTotal, setPlayerTotal] = useState(0)
  const [selectedUser, setSelectedUser] = useState<AdminDiscordUserOption | null>(null)
  const [selectedPlayer, setSelectedPlayer] = useState<AdminGentoolPlayerOption | null>(null)
  const [usersLoading, setUsersLoading] = useState(false)
  const [resolvingUser, setResolvingUser] = useState(false)
  const [playersLoading, setPlayersLoading] = useState(false)
  const [busy, setBusy] = useState<'link' | 'unlink' | null>(null)
  const [resultMessage, setResultMessage] = useState<string | null>(null)
  const userRequest = useRef(0)
  const playerRequest = useRef(0)

  const loadUsers = useCallback(async (query: string, exact: boolean) => {
    const request = ++userRequest.current
    const search = query.trim()
    if (!search) {
      setUsers([])
      setUsersLoading(false)
      return
    }
    setUsersLoading(true)
    try {
      const result = await consoleApi.searchDiscordUsersForLink(search, exact)
      if (request === userRequest.current) setUsers(result)
    } catch {
      if (request === userRequest.current) onError('Failed to search Discord users.')
    } finally {
      if (request === userRequest.current) setUsersLoading(false)
    }
  }, [onError])

  const loadPlayers = useCallback(async (query: string, exact: boolean) => {
    const request = ++playerRequest.current
    const search = query.trim()
    if (!search) {
      setPlayers([])
      setPlayerTotal(0)
      setPlayersLoading(false)
      return
    }
    setPlayersLoading(true)
    try {
      const result = await consoleApi.searchGentoolPlayersForLink(search, exact)
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
    if (!active || forceLoading) return
    void loadUsers(deferredUserQuery, exactUserMatch)
  }, [active, deferredUserQuery, exactUserMatch, forceLoading, loadUsers])

  useEffect(() => {
    if (!active || forceLoading) return
    void loadPlayers(deferredPlayerQuery, exactPlayerMatch)
  }, [active, deferredPlayerQuery, exactPlayerMatch, forceLoading, loadPlayers])

  const playerWillMove = Boolean(
    selectedPlayer?.linkedUserId && selectedPlayer.linkedUserId !== selectedUser?.userId,
  )
  const selectedLink = selectedUser?.linkedPlayers.find((link) => link.playerId === selectedPlayer?.playerId)
  const userAtCapacity = Boolean(
    selectedUser
      && selectedUser.linkedPlayers.length >= selectedUser.maxLinkedPlayers
      && !selectedLink,
  )
  const canResolveUser = /^[0-9]{17,20}$/.test(userQuery.trim())
  const additionalPlayers = Math.max(playerTotal - (players?.length ?? 0), 0)

  const resolveUser = async (discordUserId = userQuery.trim()) => {
    if (!/^[0-9]{17,20}$/.test(discordUserId)) return
    const request = ++userRequest.current
    setUsersLoading(false)
    setResolvingUser(true)
    setResultMessage(null)
    try {
      const resolved = await consoleApi.resolveDiscordUserForLink(discordUserId)
      if (!resolved || request !== userRequest.current) return
      setUsers((current) => [resolved, ...(current ?? []).filter((user) => user.userId !== resolved.userId)])
      setSelectedUser(resolved)
      setResultMessage(`Verified ${resolved.displayName} with Discord.`)
    } catch (error) {
      if (request === userRequest.current) onError(error instanceof Error ? error.message : 'Failed to fetch Discord user.')
    } finally {
      setResolvingUser(false)
    }
  }

  const assign = async () => {
    if (!selectedUser || !selectedPlayer) return
    if (playerWillMove && !confirm('Move this player from its existing Discord account?')) return
    setBusy('link')
    setResultMessage(null)
    try {
      const result = await consoleApi.assignGentoolLink(selectedUser.userId, selectedPlayer.playerId)
      if (!result) return
      setSelectedUser(result.user)
      setSelectedPlayer(result.player)
      setResultMessage(`Linked ${result.user.displayName} to ${result.player.mainName}.`)
      onChanged()
      await Promise.all([
        loadUsers(deferredUserQuery, exactUserMatch),
        loadPlayers(deferredPlayerQuery, exactPlayerMatch),
      ])
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to link profiles.')
    } finally {
      setBusy(null)
    }
  }

  const unlinkUser = async () => {
    if (!selectedUser || !selectedLink) return
    if (!confirm(`Remove ${selectedUser.displayName}'s link to ${selectedLink.playerName ?? selectedLink.playerId}?`)) return
    setBusy('unlink')
    setResultMessage(null)
    try {
      await consoleApi.unlinkGentoolUser(selectedUser.userId, selectedLink.playerId)
      const playerName = selectedLink.playerName ?? selectedLink.playerId
      setSelectedUser({
        ...selectedUser,
        linkedPlayers: selectedUser.linkedPlayers.filter((link) => link.playerId !== selectedLink.playerId),
      })
      if (selectedPlayer?.linkedUserId === selectedUser.userId) {
        setSelectedPlayer({
          ...selectedPlayer,
          linkedUserId: null,
          linkedDiscordUserId: null,
          linkedDisplayName: null,
          linkStatus: null,
        })
      }
      setResultMessage(`Removed the link to ${playerName}.`)
      onChanged()
      await Promise.all([
        loadUsers(deferredUserQuery, exactUserMatch),
        loadPlayers(deferredPlayerQuery, exactPlayerMatch),
      ])
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to remove profile link.')
    } finally {
      setBusy(null)
    }
  }

  return (
    <Card className="profile-links-card">
      <CardHeader>
        <CardTitle className="text-base">Player links</CardTitle>
      </CardHeader>
      <CardContent className="space-y-4">
        <div className="grid gap-4 lg:grid-cols-[minmax(0,1fr)_32px_minmax(0,1fr)] lg:items-start">
          <div className="min-w-0 space-y-2">
            <Label htmlFor="discord-user-search">Discord account</Label>
            <div className="flex gap-2">
              <Input
                id="discord-user-search"
                className="min-w-0 flex-1"
                value={userQuery}
                onChange={(event) => {
                  setUserQuery(event.target.value)
                  if (!event.target.value.trim()) {
                    ++userRequest.current
                    setExactUserMatch(false)
                    setUsers([])
                    setUsersLoading(false)
                    setSelectedUser(null)
                  }
                }}
                placeholder="Display name or Discord user ID"
              />
              <label className="flex h-8 shrink-0 cursor-pointer items-center gap-1.5 text-xs text-muted-foreground">
                <input
                  type="checkbox"
                  className="size-4 accent-primary"
                  aria-label="Exact Discord account match"
                  checked={exactUserMatch}
                  disabled={!userQuery.trim()}
                  onChange={(event) => setExactUserMatch(event.target.checked)}
                />
                Exact
              </label>
              <Button
                type="button"
                variant="outline"
                disabled={!canResolveUser || resolvingUser}
                title="Fetch this user ID from Discord"
                onClick={() => void resolveUser()}
              >
                {resolvingUser ? <Loader2 className="animate-spin" /> : <Search />}
                Fetch
              </Button>
            </div>
            <div className="profile-search-results h-56 overflow-y-auto rounded-lg border" data-empty={!usersLoading && !users?.length || undefined} aria-busy={usersLoading}>
              {users === null && usersLoading && <LinkSearchSkeleton />}
              {!userQuery.trim() && <EmptySearchResult label="Search by display name or Discord user ID." />}
              {userQuery.trim() && !usersLoading && !users?.length && <EmptySearchResult label="No Discord users found." />}
              {users?.map((user) => (
                <button
                  type="button"
                  key={user.userId}
                  aria-pressed={selectedUser?.userId === user.userId}
                  className={`profile-search-option flex w-full items-center gap-3 border-b px-3 py-2 text-left last:border-b-0 hover:bg-muted/50 ${selectedUser?.userId === user.userId ? 'bg-muted' : ''}`}
                  onClick={() => { setSelectedUser(user); setSelectedPlayer(null); setResultMessage(null) }}
                >
                  <ProfileAvatar name={user.displayName} pictureUrl={user.pictureUrl} />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-medium">{user.displayName}</span>
                    <span className="block truncate font-mono text-xs text-muted-foreground">{user.discordUserId}</span>
                  </span>
                  {user.linkedPlayers.length > 0 && (
                    <Badge variant="secondary" className="max-w-32 shrink-0 truncate font-normal">
                      {user.linkedPlayers.length} linked
                    </Badge>
                  )}
                </button>
              ))}
            </div>
            {selectedUser && (
              <div className="flex min-h-7 flex-wrap items-center gap-1.5 text-xs text-muted-foreground">
                <span>Linked ({selectedUser.linkedPlayers.length}/{selectedUser.maxLinkedPlayers}):</span>
                {selectedUser.linkedPlayers.length === 0 && <span>None</span>}
                {selectedUser.linkedPlayers.map((link) => (
                  <Button
                    key={link.playerId}
                    variant={selectedLink?.playerId === link.playerId ? 'secondary' : 'outline'}
                    size="xs"
                    onClick={() => setSelectedPlayer({
                      playerId: link.playerId,
                      mainName: link.playerName ?? link.playerId,
                      linkedUserId: selectedUser.userId,
                      linkedDiscordUserId: selectedUser.discordUserId,
                      linkedDisplayName: selectedUser.displayName,
                      linkStatus: link.linkStatus,
                      latestMatch: null,
                    })}
                  >
                    {link.playerName ?? link.playerId}
                  </Button>
                ))}
              </div>
            )}
          </div>

          <div className="hidden h-8 items-center justify-center text-muted-foreground lg:flex lg:pt-7">
            <Link2 className="size-4" />
          </div>

          <div className="min-w-0 space-y-2">
            <Label htmlFor="gentool-player-search">GenTool player</Label>
            <div className="flex gap-2">
              <Input
                id="gentool-player-search"
                className="min-w-0 flex-1"
                value={playerQuery}
                onChange={(event) => {
                  setPlayerQuery(event.target.value)
                  if (!event.target.value.trim()) {
                    ++playerRequest.current
                    setExactPlayerMatch(false)
                    setPlayers([])
                    setPlayerTotal(0)
                    setPlayersLoading(false)
                    setSelectedPlayer(null)
                  }
                }}
                placeholder="Player name or GenTool ID"
              />
              <label className="flex h-8 shrink-0 cursor-pointer items-center gap-1.5 text-xs text-muted-foreground">
                <input
                  type="checkbox"
                  className="size-4 accent-primary"
                  aria-label="Exact match"
                  checked={exactPlayerMatch}
                  disabled={!playerQuery.trim()}
                  onChange={(event) => setExactPlayerMatch(event.target.checked)}
                />
                Exact
              </label>
            </div>
            <div className="profile-search-results h-56 overflow-y-auto rounded-lg border" data-empty={!playersLoading && !players?.length || undefined} aria-busy={playersLoading}>
              {players === null && playersLoading && <LinkSearchSkeleton />}
              {!playerQuery.trim() && <EmptySearchResult label="Search by player name or GenTool ID." />}
              {playerQuery.trim() && !playersLoading && !players?.length && <EmptySearchResult label="No GenTool players found." />}
              {players?.map((player) => {
                const latestLineup = player.latestMatch?.teams
                  .map((team) => team.join(' + '))
                  .join(' vs ')
                return <button
                  type="button"
                  key={player.playerId}
                  aria-pressed={selectedPlayer?.playerId === player.playerId}
                  className={`profile-search-option flex w-full items-center gap-3 border-b px-3 py-2 text-left last:border-b-0 hover:bg-muted/50 ${selectedPlayer?.playerId === player.playerId ? 'bg-muted' : ''}`}
                  onClick={() => { setSelectedPlayer(player); setResultMessage(null) }}
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
                  {player.linkedUserId && (
                    <Badge variant="secondary" className="max-w-36 shrink-0 truncate font-normal">
                      {player.linkedDisplayName ?? player.linkedDiscordUserId}
                    </Badge>
                  )}
                </button>
              })}
            </div>
            {players && players.length > 0 && (
              <p className="text-xs text-muted-foreground" aria-live="polite">
                {players.length} shown · {additionalPlayers} more {additionalPlayers === 1 ? 'match' : 'matches'}
              </p>
            )}
          </div>
        </div>

        {playerWillMove && (
          <div className="rounded-lg border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm">
            This player will move from its existing Discord account.
          </div>
        )}
        {userAtCapacity && (
          <div className="rounded-lg border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm">
            This Discord account has reached its limit of {selectedUser?.maxLinkedPlayers} linked players.
          </div>
        )}

        <div className="profile-search-footer flex flex-wrap items-center gap-2 border-t pt-4">
          <Button disabled={!selectedUser || !selectedPlayer || busy !== null || userAtCapacity || Boolean(selectedLink)} onClick={() => void assign()}>
            {busy === 'link' ? <Loader2 className="animate-spin" /> : <Link2 />}
            Link profiles
          </Button>
          {selectedLink && (
            <Button variant="outline" disabled={busy !== null} onClick={() => void unlinkUser()}>
              {busy === 'unlink' ? <Loader2 className="animate-spin" /> : <Unlink />}
              Remove link
            </Button>
          )}
          <span className="min-w-0 text-sm text-muted-foreground" aria-live="polite">
            {resultMessage ?? (
              selectedUser && selectedPlayer
                ? `${selectedUser.displayName} → ${selectedPlayer.mainName}`
                : 'Select one Discord account and one GenTool player.'
            )}
          </span>
        </div>
      </CardContent>
    </Card>
  )
}

function LinkSearchSkeleton() {
  return (
    <div className="space-y-3 p-3">
      {Array.from({ length: 3 }).map((_, index) => <Skeleton key={index} className="h-10 w-full" />)}
    </div>
  )
}

function EmptySearchResult({ label }: { label: string }) {
  return <div className="profile-search-empty flex h-full items-center justify-center px-4 text-sm text-muted-foreground">{label}</div>
}

const ROLE_LABELS: Record<ConsoleRole, string> = {
  NONE: 'No access',
  VIEWER: 'Viewer',
  EDITOR: 'Editor',
  ADMIN: 'Admin',
  OWNER: 'Owner',
}

const REQUEST_COLUMNS: Column<PendingRequest>[] = [
  { key: 'displayName', label: 'User', sortKey: 'displayName', searchKey: 'displayName', value: row => row.displayName, defaultWidth: 180, mobilePrimary: true, cellClassName: 'truncate font-medium' },
  { key: 'discordUserId', label: 'Discord user ID', sortKey: 'discordUserId', searchKey: 'discordUserId', value: row => row.discordUserId, defaultWidth: 260, cellClassName: 'truncate text-muted-foreground' },
  { key: 'requestedRole', label: 'Requested', sortKey: 'requestedRole', searchKey: 'requestedRole', value: row => row.requestedRole ? ROLE_LABELS[row.requestedRole] : '', render: row => row.requestedRole ? ROLE_LABELS[row.requestedRole] : '\u2014', defaultWidth: 140, mobileCompact: true },
]

const USER_COLUMNS: Column<UserSummary>[] = [
  ...REQUEST_COLUMNS.slice(0, 2),
  { key: 'accessStatus', label: 'Status', sortKey: 'accessStatus', searchKey: 'accessStatus', value: row => row.accessStatus, render: row => <Badge variant="secondary" className="font-normal">{row.accessStatus}</Badge>, defaultWidth: 140, mobileCompact: true },
  { key: 'role', label: 'Role', sortKey: 'role', searchKey: 'role', value: row => ROLE_LABELS[row.role], defaultWidth: 180, interactive: true, mobileTools: true },
]

function RecordDetails({ values }: { values: Record<string, string> }) {
  return <dl className="grid grid-cols-2 gap-3 text-xs sm:grid-cols-3">
    {Object.entries(values).map(([label, value]) =>
      <div key={label} className="min-w-0"><dt className="text-muted-foreground">{label}</dt><dd className="wrap-anywhere">{value}</dd></div>)}
  </dl>
}

function UsersCard({ active, access, forceLoading, onError, refreshKey, onAccessChange }: {
  active: boolean
  access: AccessInfo
  forceLoading: boolean
  onError: (message: string) => void
  refreshKey: number
  onAccessChange: () => void
}) {
  const [busyId, setBusyId] = useState<string | null>(null)
  // Owners can assign any role; admins only up to EDITOR.
  const assignableRoles: ConsoleRole[] = access.isOwner
    ? ['NONE', 'VIEWER', 'EDITOR', 'ADMIN', 'OWNER']
    : ['NONE', 'VIEWER', 'EDITOR']
  const roleItems = Object.fromEntries(assignableRoles.map(role => [role, ROLE_LABELS[role]]))

  const changeRole = async (user: UserSummary, role: ConsoleRole) => {
    if (role === user.role) return
    setBusyId(user.googleId)
    try {
      await consoleApi.changeUserRole(user.googleId, role)
      onAccessChange()
    } catch {
      onError('Failed to change role. Admin roles can only be managed by the owner.')
    } finally {
      setBusyId(null)
    }
  }

  const roleControl = (user: UserSummary) => {
    const locked = user.googleId === access.userId || (!access.isOwner && (user.role === 'ADMIN' || user.role === 'OWNER'))
    return locked ? <Badge variant="outline">{ROLE_LABELS[user.role]}</Badge> : (
      <Select items={roleItems} value={user.role} onValueChange={value => value && void changeRole(user, value as ConsoleRole)}>
        <SelectTrigger size="sm" className="w-full min-w-0 md:w-[160px]" disabled={busyId !== null} aria-label={`Role for ${user.displayName}`}>
          <SelectValue />
        </SelectTrigger>
        <SelectContent>
          {assignableRoles.map(role => <SelectItem key={role} value={role}>{ROLE_LABELS[role]}</SelectItem>)}
        </SelectContent>
      </Select>
    )
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Users</CardTitle>
        <p className="text-xs text-muted-foreground">
          {access.isOwner ? 'You can assign any role, including admins.' : 'You can manage roles up to Editor. Admin roles are managed by the owner.'}
        </p>
      </CardHeader>
      <CardContent>
        <DataTab<UserSummary> active={active}
          columns={USER_COLUMNS.map(column => column.key === 'role' ? { ...column, render: roleControl } : column)}
          columnWidthsKey="users" defaultSortKey="displayName" defaultSortDirection="asc"
          rowKey={row => row.googleId} showRowNumbers
          load={async (page, size, q, field, sort, direction, exact) =>
            pageRows(await consoleApi.listUsers(), USER_COLUMNS, row => row.googleId, page, size, q, field, sort, direction, exact)}
          emptyLabel="No users yet." forceLoading={forceLoading} onError={onError} refreshToken={refreshKey}
        />
      </CardContent>
    </Card>
  )
}
