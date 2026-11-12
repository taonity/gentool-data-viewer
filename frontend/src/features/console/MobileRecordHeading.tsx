import type { ReactNode } from 'react'
import { TableCell } from '@/components/ui/table'

export function MobileRecordHeading({ label, rank, children, actions }: {
  label: string
  rank?: number
  children: ReactNode
  actions?: ReactNode
}) {
  return (
    <TableCell className="record-heading">
      {rank !== undefined && <span className="record-rank"># {rank.toLocaleString()}</span>}
      <div className="record-title min-w-0 flex-1 font-medium">
        <span className="sr-only">{label}: </span>
        {children}
      </div>
      {actions && <div className="record-tools flex shrink-0 items-center gap-0.5">{actions}</div>}
    </TableCell>
  )
}
