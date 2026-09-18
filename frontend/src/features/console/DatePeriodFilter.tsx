'use client'

import { useEffect, useId, useState } from 'react'
import { Check, X } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { DatePicker } from '@/components/ui/date-picker'
import { Skeleton } from '@/components/ui/skeleton'
import { consoleApi } from './api'

export interface DatePeriod {
  startDate: string
  endDate: string
}

export function DatePeriodFilter({ value, onChange, active, forceLoading, refreshToken = 0, onError }: {
  value: DatePeriod
  onChange: (period: DatePeriod) => void
  active: boolean
  forceLoading: boolean
  refreshToken?: number
  onError: (message: string) => void
}) {
  const id = useId()
  const [available, setAvailable] = useState<DatePeriod | null>(null)
  const [editedDraft, setDraft] = useState<DatePeriod | null>(null)
  const [loading, setLoading] = useState(true)
  const filtered = Boolean(value.startDate || value.endDate)
  const applied = filtered ? value : available ?? value
  const draft = editedDraft ?? applied
  const invalid = Boolean(draft.startDate && draft.endDate && draft.startDate > draft.endDate)
  const changed = draft.startDate !== applied.startDate || draft.endDate !== applied.endDate
  const showLoading = forceLoading || (loading && available === null)

  useEffect(() => {
    if (!active || forceLoading) return
    let current = true
    setLoading(true)
    consoleApi.getReplayDateRange()
      .then((range) => {
        if (current) setAvailable({ startDate: range.startDate ?? '', endDate: range.endDate ?? '' })
      })
      .catch(() => {
        if (current) onError('Failed to load available replay dates.')
      })
      .finally(() => {
        if (current) setLoading(false)
      })
    return () => { current = false }
  }, [active, forceLoading, refreshToken, onError])

  return (
    <div role="group" aria-label="Match date period" className="flex max-w-full flex-wrap items-center gap-x-3 gap-y-2 border-b pb-3">
      <span className="text-xs font-medium text-muted-foreground">Match dates (UTC)</span>
      <div className="flex max-w-full flex-wrap items-center gap-2">
        <div className="flex items-center gap-2">
          <label htmlFor={`${id}-start`} className="w-8 shrink-0 text-xs text-muted-foreground">From</label>
          {showLoading ? <Skeleton className="h-8 w-40" /> : <DatePicker
            id={`${id}-start`}
            label="Start date (UTC)"
            invalid={invalid}
            describedBy={invalid ? `${id}-error` : undefined}
            value={draft.startDate}
            max={draft.endDate || undefined}
            onChange={(startDate) => setDraft({ ...draft, startDate })}
          />}
        </div>
        <div className="flex items-center gap-2">
          <label htmlFor={`${id}-end`} className="shrink-0 text-xs text-muted-foreground">To</label>
          {showLoading ? <Skeleton className="h-8 w-40" /> : <DatePicker
            id={`${id}-end`}
            label="End date (UTC)"
            invalid={invalid}
            describedBy={invalid ? `${id}-error` : undefined}
            value={draft.endDate}
            min={draft.startDate || undefined}
            onChange={(endDate) => setDraft({ ...draft, endDate })}
          />}
        </div>
        <Button variant="outline" size="icon-sm" aria-label="Apply date period" title="Apply date period"
          disabled={!changed || invalid || showLoading} onClick={() => {
            const fullRange = available && draft.startDate === available.startDate && draft.endDate === available.endDate
            onChange(fullRange ? { startDate: '', endDate: '' } : draft)
            setDraft(null)
          }}>
          <Check />
        </Button>
        <Button variant="ghost" size="icon-sm" aria-label="Clear date period" title="Clear date period"
          disabled={(!changed && !filtered) || showLoading}
          onClick={() => {
            setDraft(null)
            onChange({ startDate: '', endDate: '' })
          }}>
          <X />
        </Button>
      </div>
      {invalid ? (
        <span id={`${id}-error`} role="alert" className="text-xs text-destructive">Start date must not be after end date.</span>
      ) : !value.startDate && !value.endDate ? (
        <span className="text-xs text-muted-foreground">All time</span>
      ) : null}
    </div>
  )
}