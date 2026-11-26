import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import type { Req } from '../api'
import { Badge, DataTable, EmptyRow } from '../components'
import { formatDate, isOverdue } from './dates'
import styles from './pages.module.css'

interface Props {
  requests: Req[] | undefined
  empty: string
  /** Flag past-due dates (off for finished work). */
  highlightOverdue?: boolean
  /** Optional trailing column, e.g. queue-specific state and actions. */
  extraHeader?: string
  extra?: (r: Req) => ReactNode
}

/** The one list used by every queue: items are requests, never individual recordings. */
export function RequestTable({ requests, empty, highlightOverdue = true, extraHeader, extra }: Props) {
  const cols = 7 + (extra ? 1 : 0)
  return (
    <DataTable>
      <thead>
        <tr>
          <th>Case ID</th><th>Request</th><th>Case owner</th><th>Priority</th><th>Due</th><th>Status</th><th>Recordings</th>
          {extra && <th>{extraHeader}</th>}
        </tr>
      </thead>
      <tbody>
        {requests?.map((r) => (
          <tr key={r.id}>
            <td className={styles.nowrap}>{r.caseId ?? <span className={styles.muted}>—</span>}</td>
            <td><Link to={`/requests/${r.id}`}>{r.title}</Link></td>
            <td>{r.caseOwner ?? <span className={styles.muted}>—</span>}</td>
            <td><Badge status={r.priority} /></td>
            <td className={highlightOverdue && isOverdue(r.dueDate) ? styles.overdue : undefined}>
              {formatDate(r.dueDate)}{highlightOverdue && isOverdue(r.dueDate) && ' (overdue)'}
            </td>
            <td><Badge status={r.status} /></td>
            <td>{r.transcribedCount}/{r.documentCount}</td>
            {extra && <td>{extra(r)}</td>}
          </tr>
        ))}
        {requests?.length === 0 && <EmptyRow cols={cols}>{empty}</EmptyRow>}
      </tbody>
    </DataTable>
  )
}
