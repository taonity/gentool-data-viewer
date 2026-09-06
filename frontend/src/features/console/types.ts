export type ConsoleRole = 'NONE' | 'VIEWER' | 'EDITOR' | 'ADMIN' | 'OWNER'
export type AccessStatus = 'NONE' | 'PENDING' | 'APPROVED' | 'REJECTED'

export interface AccessInfo {
  email: string
  displayName: string
  role: ConsoleRole
  accessStatus: AccessStatus
  requestedRole: ConsoleRole | null
  canView: boolean
  canEdit: boolean
  isAdmin: boolean
  isOwner: boolean
}

export interface UserSummary {
  googleId: string
  email: string
  displayName: string
  role: ConsoleRole
  accessStatus: AccessStatus
  requestedRole: ConsoleRole | null
}

/** A dev-only stub login shortcut returned by /api/dev/stub-users. */
export interface StubLogin {
  registrationId: string
  label: string
}

export interface PendingRequest {
  googleId: string
  email: string
  displayName: string
  requestedRole: ConsoleRole | null
}

export interface PageResponse<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
  hasMore: boolean
}

export interface PageLocation {
  page: number
}

export interface AuditLog {
  id: string
  action: string
  targetType: string
  targetId: string | null
  actorEmail: string
  occurredAt: string
}

export type ConfigFieldType =
  | 'BOOL'
  | 'INT'
  | 'LONG'
  | 'DOUBLE'
  | 'STRING'
  | 'TEXT'
  | 'ENUM'
  | 'STRING_LIST'

export interface ConfigField {
  key: string
  group: string
  label: string
  type: ConfigFieldType
  min: number | null
  max: number | null
  enumValues: string[]
  defaultValue: unknown
  value: unknown
  overridden: boolean
}

export interface ConfigSchema {
  fields: ConfigField[]
}

export type ReplayCollectionStatus = 'QUEUED' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface ReplayCollectionJob {
  id: string
  triggerType: 'SCHEDULED' | 'MANUAL' | 'USER_RESCAN'
  status: ReplayCollectionStatus
  startDate: string
  endDate: string
  requestedBy: string
  userLimit: number | null
  targetPlayerId: string | null
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
  directoriesDiscovered: number
  directoriesScanned: number
  filesDiscovered: number
  filesImported: number
  filesSkipped: number
  failures: number
  errorMessage: string | null
}

export type GentoolLinkStatus = 'PENDING' | 'APPROVED' | 'REJECTED'

export interface GentoolLink {
  userId: string
  email: string
  displayName: string
  playerId: string
  playerName: string | null
  status: GentoolLinkStatus
  requestedAt: string
  decidedAt: string | null
}

export interface ReplayRescanHistory {
  id: string
  targetPlayerId: string
  ownTarget: boolean
  jobId: string
  status: ReplayCollectionStatus
  errorMessage: string | null
  requestedAt: string
}

export interface ReplayRescanDashboard {
  link: GentoolLink | null
  otherUsedToday: number
  otherDailyLimit: number
  quotaResetsAt: string
  targetCooldownSeconds: number
  lookbackDays: number
  history: ReplayRescanHistory[]
}

export interface ReplayRescanAccepted {
  jobId: string
  ownTarget: boolean
}

export interface ReplayPlayer {
  teamNumber: number
  slotNumber: number
  address: string
  name: string
  army: string | null
}

export interface ReplayAssociatedFile {
  name: string
  sizeBytes: number
}

export interface Replay {
  id: string
  sourceUrl: string
  sourceDate: string
  reporterId: string
  reporterName: string
  matchAt: string
  windowsCompat: string | null
  gentoolVersion: string | null
  gameVersion: string | null
  installType: string | null
  repInfoInUse: string | null
  mapName: string | null
  startCash: number | null
  matchType: string | null
  matchLengthSeconds: number | null
  matchMode: string | null
  systemInfo: string | null
  cpu: string | null
  replayFileName: string | null
  replaySizeBytes: number | null
  collectedAt: string
  fields: Record<string, string>
  players: ReplayPlayer[]
  associatedFiles: ReplayAssociatedFile[]
  rawText: string
}

export type CpuMatchStatus = 'EXACT' | 'MODEL' | 'AMBIGUOUS' | 'UNMATCHED' | 'NO_CPU'

export interface CpuPlayer {
  playerId: string
  mainName: string
  latestName: string
  aliases: string[]
  replayCount: number
  reportedCpu: string | null
  singleThreadScore: number | null
  benchmarkModel: string | null
  benchmarkUrl: string | null
  matchStatus: CpuMatchStatus
  observedAt: string
  gentoolUpdatedAt: string | null
  scoreUpdatedAt: string | null
}

export interface CpuPlayerSummary {
  totalPlayers: number
  ratedPlayers: number
  unmatchedPlayers: number
  ambiguousPlayers: number
  playersWithoutCpu: number
  catalogEntries: number
  catalogFetchedAt: string | null
}

export interface CpuBenchmarkSync {
  benchmarks: number
  fetchedAt: string
  totalPlayers: number
  ratedPlayers: number
  unmatchedPlayers: number
  ambiguousPlayers: number
  playersWithoutCpu: number
}
