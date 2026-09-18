'use client'

import { useState } from 'react'
import { CalendarDays, X } from 'lucide-react'
import { TZDate } from 'react-day-picker'
import { Button } from '@/components/ui/button'
import { Calendar } from '@/components/ui/calendar'
import { Popover, PopoverContent, PopoverTrigger } from '@/components/ui/popover'
import { cn } from '@/lib/utils'

function calendarDate(value: string): Date | undefined {
  return value ? new TZDate(`${value}T00:00:00Z`, 'UTC') : undefined
}

export function DatePicker({ id, label, value, onChange, min, max, invalid, describedBy }: {
  id: string
  label: string
  value: string
  onChange: (value: string) => void
  min?: string
  max?: string
  invalid?: boolean
  describedBy?: string
}) {
  const [open, setOpen] = useState(false)
  const selected = calendarDate(value)
  const today = new Date().toISOString().slice(0, 10)
  const todayUnavailable = Boolean((min && today < min) || (max && today > max))
  const select = (next: string) => {
    onChange(next)
    setOpen(false)
  }

  return (
    <Popover open={open} onOpenChange={setOpen}>
      <PopoverTrigger render={<Button variant="outline" />} id={id}
        aria-label={`${label}: ${value || 'Not set'}`}
        aria-invalid={invalid}
        aria-describedby={describedBy}
        className={cn('h-8 w-40 justify-start gap-2 text-xs font-normal', !value && 'text-muted-foreground')}>
        <CalendarDays className="size-3.5 text-muted-foreground" />
        <span className="tabular-nums">
          {selected ? selected.toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric', timeZone: 'UTC' }) : 'Select date'}
        </span>
      </PopoverTrigger>
      <PopoverContent side="bottom" align="start" className="w-auto max-w-[calc(100vw-1rem)] max-h-(--available-height) overflow-y-auto p-0"
        aria-label={label}>
        <Calendar
          mode="single"
          timeZone="UTC"
          selected={selected}
          defaultMonth={selected ?? calendarDate(min || max || today)}
          onSelect={(date) => select(date?.toISOString().slice(0, 10) ?? '')}
          disabled={[
            ...(min ? [{ before: calendarDate(min)! }] : []),
            ...(max ? [{ after: calendarDate(max)! }] : []),
          ]}
          captionLayout="dropdown"
          autoFocus
          fixedWeeks
        />
        <div className="flex items-center justify-between gap-2 border-t p-2">
          <Button variant="ghost" size="sm" disabled={todayUnavailable} onClick={() => select(today)}>
            <CalendarDays />
            Today
          </Button>
          <Button variant="ghost" size="sm" disabled={!value} onClick={() => select('')}>
            <X />
            Clear
          </Button>
        </div>
      </PopoverContent>
    </Popover>
  )
}