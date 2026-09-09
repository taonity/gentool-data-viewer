'use client'

import { useCallback, useDeferredValue, useEffect, useRef, useState } from 'react'
import { Link2, Loader2, Search, Unlink } from 'lucide-react'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select'
import { Skeleton } from '@/components/ui/skeleton'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { consoleApi } from './api'
import { DataTab, type Column } from './DataTab'
import { formatTime } from './format'
import type {
  AccessInfo,
  AdminDiscordUserOption,
  AdminGentoolPlayerOption,
  AuditLog,
  ConsoleRole,
  PendingRequest,
  UserSummary,
} from './types'

const AUDIT_COLUMNS: Column<AuditLog>[] = [
  {
    key: 'occurredAt',
    label: 'When',
    value: (a) => a.occurredAt,
    render: (a) => formatTime(a.occurredAt),
    cellClassName: 'whitespace-nowrap text-muted-foreground tabular-nums',
    headClassName: 'w-[170px]',
    skeleton: 'w-[85%]',
  },
  {
    key: 'action',
    label: 'Action',
    value: (a) => a.action,
    cellClassName: 'truncate font-medium',
    headClassName: 'w-[22%]',
    searchKey: 'action',
  },
  {
    key: 'targetType',
    label: 'Target',
    value: (a) => a.targetType,
    cellClassName: 'truncate',
    headClassName: 'w-[16%]',
    searchKey: 'targetType',
  },
  {
    key: 'targetId',
    label: 'Record id',
    value: (a) => a.targetId ?? '',
    render: (a) => <span className="font-mono text-xs">{a.targetId ?? '—'}</span>,
    cellClassName: 'truncate',
    headClassName: 'w-[28%]',
    searchKey: 'targetId',
  },
  { key: 'actorUserId', label: 'Actor', value: (a) => a.actorUserId, cellClassName: 'truncate', searchKey: 'actorUserId' },
]

export function AdminPanel({
  active = true,
  access,
  forceLoading = false,
  onError,
  onPendingCountChange,
}: {
  active?: boolean
  access: AccessInfo
  forceLoading?: boolean
  onError: (message: string) => void
  onPendingCountChange?: (count: number) => void
}) {
  const [requests, setRequests] = useState<PendingRequest[] | null>(null)
  const [grantRoles, setGrantRoles] = useState<Record<string, ConsoleRole>>({})
  const [busyId, setBusyId] = useState<string | null>(null)
  const wasActive = useRef(false)

  const loadRequests = useCallback(async () => {
    try {
      const result = await consoleApi.listPendingRequests()
      setRequests(result)
      onPendingCountChange?.(result.length)
      setGrantRoles(
        Object.fromEntries(
          result.map((r) => [r.googleId, (r.requestedRole ?? 'VIEWER') as ConsoleRole]),
        ),
      )
    } catch {
      onError('Failed to load access requests.')
    }
  }, [onError, onPendingCountChange])

  useEffect(() => {
    if (forceLoading || !active || !access.accessRequestsEnabled) {
      wasActive.current = false
      return
    }
    if (!wasActive.current) void loadRequests()
    wasActive.current = true
  }, [access.accessRequestsEnabled, active, forceLoading, loadRequests])

  const visibleRequests = forceLoading ? null : requests

  const decide = async (req: PendingRequest, approve: boolean) => {
    setBusyId(req.googleId)
    try {
      if (approve) {
        await consoleApi.approveRequest(req.googleId, grantRoles[req.googleId] ?? 'VIEWER')
      } else {
        await consoleApi.rejectRequest(req.googleId)
      }
      await loadRequests()
    } catch {
      onError(approve ? 'Failed to approve request.' : 'Failed to reject request.')
    } finally {
      setBusyId(null)
    }
  }

  return (
    <div className="flex flex-col gap-6">
      {access.accessRequestsEnabled && <Card>
        <CardHeader>
          <CardTitle className="text-base">Pending access requests</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="overflow-hidden rounded-lg border">
            <Table className="min-w-[640px] table-fixed [&_td]:py-2 [&_tr]:h-12">
              <colgroup>
                <col className="w-[18%]" />
                <col className="w-[26%]" />
                <col className="w-[14%]" />
                <col className="w-[132px]" />
                <col className="w-[176px]" />
              </colgroup>
              <TableHeader>
                <TableRow className="bg-muted/40">
                  <TableHead>User</TableHead>
                  <TableHead>Discord user ID</TableHead>
                  <TableHead>Requested</TableHead>
                  <TableHead>Grant</TableHead>
                  <TableHead className="text-right">Decision</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {!visibleRequests &&
                  Array.from({ length: 2 }).map((_, index) => (
                    <TableRow key={index} className="h-12 hover:bg-transparent">
                      <TableCell><Skeleton className="h-4 w-28" /></TableCell>
                      <TableCell><Skeleton className="h-4 w-40" /></TableCell>
                      <TableCell><Skeleton className="h-4 w-16" /></TableCell>
                      <TableCell><Skeleton className="h-8 w-[120px]" /></TableCell>
                      <TableCell><Skeleton className="ml-auto h-8 w-36" /></TableCell>
                    </TableRow>
                  ))}
                {visibleRequests?.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} className="h-20 text-center text-muted-foreground">
                      No pending requests.
                    </TableCell>
                  </TableRow>
                )}
                {visibleRequests?.map((r) => (
                    <TableRow key={r.googleId} className="h-12">
                      <TableCell className="truncate font-medium">{r.displayName}</TableCell>
                      <TableCell className="truncate font-mono text-xs text-muted-foreground">{r.discordUserId}</TableCell>
                      <TableCell>{r.requestedRole ?? '—'}</TableCell>
                      <TableCell>
                        <Select
                          items={{ VIEWER: 'Viewer', EDITOR: 'Editor' }}
                          value={grantRoles[r.googleId] ?? 'VIEWER'}
                          onValueChange={(v) =>
                            setGrantRoles((prev) => ({
                              ...prev,
                              [r.googleId]: (v ?? 'VIEWER') as ConsoleRole,
                            }))
                          }
                        >
                          <SelectTrigger size="sm" className="w-[120px]">
                            <SelectValue />
                          </SelectTrigger>
                          <SelectContent>
                            <SelectItem value="VIEWER">Viewer</SelectItem>
                            <SelectItem value="EDITOR">Editor</SelectItem>
                          </SelectContent>
                        </Select>
                      </TableCell>
                      <TableCell className="text-right">
                        <div className="flex justify-end gap-2">
                          <Button
                            size="sm"
                            disabled={busyId === r.googleId}
                            onClick={() => decide(r, true)}
                          >
                            Approve
                          </Button>
                          <Button
                            size="sm"
                            variant="destructive"
                            disabled={busyId === r.googleId}
                            onClick={() => decide(r, false)}
                          >
                            Reject
                          </Button>
                        </div>
                      </TableCell>
                    </TableRow>
                  ))}
              </TableBody>
            </Table>
          </div>
        </CardContent>
      </Card>}

      <PlayerLinksCard active={active} forceLoading={forceLoading} onError={onError} />

      <UsersCard active={active} access={access} forceLoading={forceLoading} onError={onError} />

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Audit log</CardTitle>
          <p className="text-xs text-muted-foreground">
            Retained for 2 weeks. Records what changed and by whom — never the changed data.
          </p>
        </CardHeader>
        <CardContent>
          <DataTab<AuditLog>
            active={active}
            columns={AUDIT_COLUMNS}
            columnWidthsKey="audit-log"
            rowKey={(a) => a.id}
            load={(page, size, q, field) => consoleApi.listAuditLogs(page, size, q, field)}
            emptyLabel="No audit entries."
            forceLoading={forceLoading}
            onError={onError}
          />
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
      className="flex size-8 shrink-0 items-center justify-center rounded-full bg-muted bg-cover bg-center text-[10px] font-semibold text-muted-foreground ring-1 ring-border"
      style={pictureUrl ? { backgroundImage: `url(${JSON.stringify(pictureUrl)})` } : undefined}
    >
      {initials}
    </span>
  )
}

function PlayerLinksCard({
  active,
  forceLoading,
  onError,
}: {
  active: boolean
  forceLoading: boolean
  onError: (message: string) => void
}) {
  const [userQuery, setUserQuery] = useState('')
  const [playerQuery, setPlayerQuery] = useState('')
  const deferredUserQuery = useDeferredValue(userQuery)
  const deferredPlayerQuery = useDeferredValue(playerQuery)
  const [users, setUsers] = useState<AdminDiscordUserOption[] | null>(null)
  const [players, setPlayers] = useState<AdminGentoolPlayerOption[] | null>(null)
  const [selectedUser, setSelectedUser] = useState<AdminDiscordUserOption | null>(null)
  const [selectedPlayer, setSelectedPlayer] = useState<AdminGentoolPlayerOption | null>(null)
  const [usersLoading, setUsersLoading] = useState(false)
  const [resolvingUser, setResolvingUser] = useState(false)
  const [playersLoading, setPlayersLoading] = useState(false)
  const [busy, setBusy] = useState<'link' | 'unlink' | null>(null)
  const [resultMessage, setResultMessage] = useState<string | null>(null)
  const userRequest = useRef(0)
  const playerRequest = useRef(0)

  const loadUsers = useCallback(async (query: string) => {
    const request = ++userRequest.current
    setUsersLoading(true)
    try {
      const result = await consoleApi.searchDiscordUsersForLink(query)
      if (request === userRequest.current) setUsers(result)
    } catch {
      if (request === userRequest.current) onError('Failed to search Discord users.')
    } finally {
      if (request === userRequest.current) setUsersLoading(false)
    }
  }, [onError])

  const loadPlayers = useCallback(async (query: string) => {
    const request = ++playerRequest.current
    setPlayersLoading(true)
    try {
      const result = await consoleApi.searchGentoolPlayersForLink(query)
      if (request === playerRequest.current) setPlayers(result)
    } catch {
      if (request === playerRequest.current) onError('Failed to search GenTool players.')
    } finally {
      if (request === playerRequest.current) setPlayersLoading(false)
    }
  }, [onError])

  useEffect(() => {
    if (!active || forceLoading) return
    void loadUsers(deferredUserQuery)
  }, [active, deferredUserQuery, forceLoading, loadUsers])

  useEffect(() => {
    if (!active || forceLoading) return
    void loadPlayers(deferredPlayerQuery)
  }, [active, deferredPlayerQuery, forceLoading, loadPlayers])

  const userWillMove = Boolean(
    selectedUser?.linkedPlayerId && selectedUser.linkedPlayerId !== selectedPlayer?.playerId,
  )
  const playerWillMove = Boolean(
    selectedPlayer?.linkedUserId && selectedPlayer.linkedUserId !== selectedUser?.userId,
  )
  const canResolveUser = /^[0-9]{17,20}$/.test(userQuery.trim())

  const resolveUser = async (discordUserId = userQuery.trim()) => {
    if (!/^[0-9]{17,20}$/.test(discordUserId)) return
    setResolvingUser(true)
    setResultMessage(null)
    try {
      const resolved = await consoleApi.resolveDiscordUserForLink(discordUserId)
      if (!resolved) return
      setUsers((current) => [resolved, ...(current ?? []).filter((user) => user.userId !== resolved.userId)])
      setSelectedUser(resolved)
      setResultMessage(`Verified ${resolved.displayName} with Discord.`)
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to fetch Discord user.')
    } finally {
      setResolvingUser(false)
    }
  }

  const assign = async () => {
    if (!selectedUser || !selectedPlayer) return
    if ((userWillMove || playerWillMove) && !confirm('Replace the existing profile link?')) return
    setBusy('link')
    setResultMessage(null)
    try {
      const result = await consoleApi.assignGentoolLink(selectedUser.userId, selectedPlayer.playerId)
      if (!result) return
      setSelectedUser(result.user)
      setSelectedPlayer(result.player)
      setResultMessage(`Linked ${result.user.displayName} to ${result.player.mainName}.`)
      await Promise.all([loadUsers(deferredUserQuery), loadPlayers(deferredPlayerQuery)])
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to link profiles.')
    } finally {
      setBusy(null)
    }
  }

  const unlinkUser = async () => {
    if (!selectedUser?.linkedPlayerId) return
    if (!confirm(`Remove ${selectedUser.displayName}'s GenTool link?`)) return
    setBusy('unlink')
    setResultMessage(null)
    try {
      await consoleApi.unlinkGentoolUser(selectedUser.userId)
      const playerName = selectedUser.linkedPlayerName ?? selectedUser.linkedPlayerId
      setSelectedUser({ ...selectedUser, linkedPlayerId: null, linkedPlayerName: null, linkStatus: null })
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
      await Promise.all([loadUsers(deferredUserQuery), loadPlayers(deferredPlayerQuery)])
    } catch (error) {
      onError(error instanceof Error ? error.message : 'Failed to remove profile link.')
    } finally {
      setBusy(null)
    }
  }

  return (
    <Card>
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
                value={userQuery}
                onChange={(event) => setUserQuery(event.target.value)}
                placeholder="Display name or Discord user ID"
              />
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
            <div className="h-56 overflow-y-auto rounded-lg border" aria-busy={usersLoading}>
              {users === null && usersLoading && <LinkSearchSkeleton />}
              {users === null && !usersLoading && <EmptySearchResult label="No Discord users found." />}
              {users?.length === 0 && <EmptySearchResult label="No Discord users found." />}
              {users?.map((user) => (
                <button
                  type="button"
                  key={user.userId}
                  className={`flex w-full items-center gap-3 border-b px-3 py-2 text-left last:border-b-0 hover:bg-muted/50 ${selectedUser?.userId === user.userId ? 'bg-muted' : ''}`}
                  onClick={() => { setSelectedUser(user); setResultMessage(null) }}
                >
                  <ProfileAvatar name={user.displayName} pictureUrl={user.pictureUrl} />
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-medium">{user.displayName}</span>
                    <span className="block truncate font-mono text-xs text-muted-foreground">{user.discordUserId}</span>
                  </span>
                  {user.linkedPlayerId && (
                    <Badge variant="secondary" className="max-w-32 shrink-0 truncate font-normal">
                      {user.linkedPlayerName ?? user.linkedPlayerId}
                    </Badge>
                  )}
                </button>
              ))}
            </div>
          </div>

          <div className="hidden h-8 items-center justify-center text-muted-foreground lg:flex lg:pt-7">
            <Link2 className="size-4" />
          </div>

          <div className="min-w-0 space-y-2">
            <Label htmlFor="gentool-player-search">GenTool player</Label>
            <Input
              id="gentool-player-search"
              value={playerQuery}
              onChange={(event) => setPlayerQuery(event.target.value)}
              placeholder="Player name or GenTool ID"
            />
            <div className="h-56 overflow-y-auto rounded-lg border" aria-busy={playersLoading}>
              {players === null && playersLoading && <LinkSearchSkeleton />}
              {players === null && !playersLoading && <EmptySearchResult label="No GenTool players found." />}
              {players?.length === 0 && <EmptySearchResult label="No GenTool players found." />}
              {players?.map((player) => (
                <button
                  type="button"
                  key={player.playerId}
                  className={`flex w-full items-center gap-3 border-b px-3 py-2 text-left last:border-b-0 hover:bg-muted/50 ${selectedPlayer?.playerId === player.playerId ? 'bg-muted' : ''}`}
                  onClick={() => { setSelectedPlayer(player); setResultMessage(null) }}
                >
                  <span className="min-w-0 flex-1">
                    <span className="block truncate text-sm font-medium">{player.mainName}</span>
                    <span className="block truncate font-mono text-xs text-muted-foreground">{player.playerId}</span>
                  </span>
                  {player.linkedUserId && (
                    <Badge variant="secondary" className="max-w-36 shrink-0 truncate font-normal">
                      {player.linkedDisplayName ?? player.linkedDiscordUserId}
                    </Badge>
                  )}
                </button>
              ))}
            </div>
          </div>
        </div>

        {(userWillMove || playerWillMove) && (
          <div className="rounded-lg border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm">
            Existing profile {userWillMove && playerWillMove ? 'links' : 'link'} will be replaced.
          </div>
        )}

        <div className="flex flex-wrap items-center gap-2 border-t pt-4">
          <Button disabled={!selectedUser || !selectedPlayer || busy !== null} onClick={() => void assign()}>
            {busy === 'link' ? <Loader2 className="animate-spin" /> : <Link2 />}
            Link profiles
          </Button>
          {selectedUser?.linkedPlayerId && (
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
  return <div className="flex h-full items-center justify-center px-4 text-sm text-muted-foreground">{label}</div>
}

const ROLE_LABELS: Record<ConsoleRole, string> = {
  NONE: 'No access',
  VIEWER: 'Viewer',
  EDITOR: 'Editor',
  ADMIN: 'Admin',
  OWNER: 'Owner',
}

function UsersCard({
  active,
  access,
  forceLoading,
  onError,
}: {
  active: boolean
  access: AccessInfo
  forceLoading: boolean
  onError: (message: string) => void
}) {
  const [users, setUsers] = useState<UserSummary[] | null>(null)
  const [busyId, setBusyId] = useState<string | null>(null)
  const wasActive = useRef(false)

  const load = useCallback(async () => {
    try {
      setUsers(await consoleApi.listUsers())
    } catch {
      onError('Failed to load users.')
    }
  }, [onError])

  useEffect(() => {
    if (forceLoading || !active) {
      wasActive.current = false
      return
    }
    if (!wasActive.current) void load()
    wasActive.current = true
  }, [active, forceLoading, load])

  const visibleUsers = forceLoading ? null : users

  // Owners can assign any role; admins only up to EDITOR.
  const assignableRoles: ConsoleRole[] = access.isOwner
    ? ['NONE', 'VIEWER', 'EDITOR', 'ADMIN', 'OWNER']
    : ['NONE', 'VIEWER', 'EDITOR']

  const roleItems = Object.fromEntries(
    assignableRoles.map((r) => [r, ROLE_LABELS[r]]),
  ) as Record<string, string>

  const changeRole = async (user: UserSummary, role: ConsoleRole) => {
    if (role === user.role) return
    setBusyId(user.googleId)
    try {
      const updated = await consoleApi.changeUserRole(user.googleId, role)
      if (updated) {
        setUsers((prev) => (prev ?? []).map((u) => (u.googleId === updated.googleId ? updated : u)))
      }
    } catch {
      onError('Failed to change role. Admin roles can only be managed by the owner.')
      void load()
    } finally {
      setBusyId(null)
    }
  }

  return (
    <Card>
      <CardHeader>
        <CardTitle className="text-base">Users</CardTitle>
        <p className="text-xs text-muted-foreground">
          {access.isOwner
            ? 'You can assign any role, including admins.'
            : 'You can manage roles up to Editor. Admin roles are managed by the owner.'}
        </p>
      </CardHeader>
      <CardContent>
        <div className="overflow-hidden rounded-lg border">
          <Table className="min-w-[560px] table-fixed [&_td]:py-2 [&_tr]:h-12">
            <colgroup>
              <col className="w-[24%]" />
              <col className="w-[34%]" />
              <col className="w-[20%]" />
              <col className="w-[180px]" />
            </colgroup>
            <TableHeader>
              <TableRow className="bg-muted/40">
                <TableHead>User</TableHead>
                <TableHead>Discord user ID</TableHead>
                <TableHead>Status</TableHead>
                <TableHead className="w-[180px]">Role</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {!visibleUsers &&
                  Array.from({ length: 2 }).map((_, index) => (
                  <TableRow key={index} className="h-12 hover:bg-transparent">
                    <TableCell><Skeleton className="h-4 w-28" /></TableCell>
                    <TableCell><Skeleton className="h-4 w-40" /></TableCell>
                    <TableCell><Skeleton className="h-5 w-20 rounded-full" /></TableCell>
                    <TableCell><Skeleton className="h-8 w-40" /></TableCell>
                  </TableRow>
                ))}
              {visibleUsers?.length === 0 && (
                <TableRow>
                  <TableCell colSpan={4} className="h-20 text-center text-muted-foreground">
                    No users yet.
                  </TableCell>
                </TableRow>
              )}
              {visibleUsers?.map((u) => {
                  const isSelf = u.googleId === access.userId
                  // Only the owner may change an existing admin/owner.
                  const targetIsAdmin = u.role === 'ADMIN' || u.role === 'OWNER'
                  const locked = isSelf || (targetIsAdmin && !access.isOwner)
                  return (
                    <TableRow key={u.googleId} className="h-12">
                      <TableCell className="truncate font-medium">{u.displayName}</TableCell>
                      <TableCell className="truncate font-mono text-xs text-muted-foreground">{u.discordUserId}</TableCell>
                      <TableCell>
                        <Badge variant="secondary" className="font-normal">
                          {u.accessStatus}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        {locked ? (
                          <Badge variant="outline">{ROLE_LABELS[u.role]}</Badge>
                        ) : (
                          <Select
                            items={roleItems}
                            value={u.role}
                            onValueChange={(v) => v && changeRole(u, v as ConsoleRole)}
                          >
                            <SelectTrigger size="sm" className="w-[160px]" disabled={busyId === u.googleId}>
                              <SelectValue />
                            </SelectTrigger>
                            <SelectContent>
                              {assignableRoles.map((r) => (
                                <SelectItem key={r} value={r}>
                                  {ROLE_LABELS[r]}
                                </SelectItem>
                              ))}
                            </SelectContent>
                          </Select>
                        )}
                      </TableCell>
                    </TableRow>
                  )
                })}
            </TableBody>
          </Table>
        </div>
      </CardContent>
    </Card>
  )
}
