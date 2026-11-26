import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api, type Version } from '../api'
import { Alert, Badge, Button, Card, DataTable, EmptyRow, PageHeader } from '../components'
import { RequestTable } from './RequestTable'
import styles from './pages.module.css'

/** Shared QA queue: requests with recordings waiting for review (sampled or explicitly requested). */
export function QaQueue() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['qa-queue'], queryFn: api.qaQueue, refetchInterval: 3000 })
  const claim = useMutation({ mutationFn: (requestId: number) => api.claimQa(requestId), onSuccess: () => qc.invalidateQueries() })
  return (
    <>
      <PageHeader title="QA queue" subtitle="Shared by all QA reviewers. Recordings arrive by random sampling or when a user or transcriber requests a review." />
      {claim.error && <Alert>{claim.error.message}</Alert>}
      <RequestTable
        requests={q.data}
        empty="Nothing waiting for QA."
        extraHeader="Review"
        extra={(r) => (
          <div className={styles.row}>
            {r.qa.open > 0 && <><Badge status="OPEN" /> {r.qa.open}</>}
            {r.qa.claimed > 0 && <><Badge status="CLAIMED" /> {r.qa.claimed}</>}
            {r.qa.open > 0 && <Button size="sm" loading={claim.isPending} onClick={() => claim.mutate(r.id)}>Take</Button>}
            {r.qa.open === 0 && r.qa.claimed > 0 && <span className={styles.muted}>yours</span>}
          </div>
        )}
      />
    </>
  )
}

/** Read-only oversight list of every request. */
export function QaAllRequests() {
  const q = useQuery({ queryKey: ['qa-all'], queryFn: api.qaAll, refetchInterval: 5000 })
  return (
    <>
      <PageHeader title="All requests" subtitle="Read-only view across all users." />
      <RequestTable
        requests={q.data}
        empty="No requests yet."
        highlightOverdue={false}
        extraHeader="QA"
        extra={(r) => (
          <div className={styles.row}>
            {r.qa.open > 0 && <Badge status="OPEN" />}{r.qa.claimed > 0 && <Badge status="CLAIMED" />}
            {r.qa.passed > 0 && <Badge status="PASSED" />}{r.qa.failed > 0 && <Badge status="FAILED" />}
          </div>
        )}
      />
    </>
  )
}

export function QaAudit() {
  const q = useQuery({ queryKey: ['audit'], queryFn: api.audit, refetchInterval: 5000 })
  return (
    <>
      <PageHeader title="Audit log" subtitle="Most recent events first." />
      <DataTable>
        <thead><tr><th>When</th><th>Actor</th><th>Entity</th><th>Action</th><th>Detail</th></tr></thead>
        <tbody>
          {q.data?.map((e) => (
            <tr key={e.id}>
              <td>{new Date(e.createdAt).toLocaleString()}</td>
              <td>{e.actorId ?? 'system'}</td>
              <td>{e.entityType} #{e.entityId}</td>
              <td>{e.action}</td>
              <td className={styles.muted}>{e.detail}</td>
            </tr>
          ))}
          {q.data?.length === 0 && <EmptyRow cols={5}>No events yet.</EmptyRow>}
        </tbody>
      </DataTable>
    </>
  )
}

/** Minimal line diff (LCS) between two texts. */
function diffLines(a: string, b: string): { t: ' ' | '-' | '+'; line: string }[] {
  const x = a.split('\n'), y = b.split('\n')
  const dp = Array.from({ length: x.length + 1 }, () => new Array<number>(y.length + 1).fill(0))
  for (let i = x.length - 1; i >= 0; i--)
    for (let j = y.length - 1; j >= 0; j--)
      dp[i][j] = x[i] === y[j] ? dp[i + 1][j + 1] + 1 : Math.max(dp[i + 1][j], dp[i][j + 1])
  const out: { t: ' ' | '-' | '+'; line: string }[] = []
  let i = 0, j = 0
  while (i < x.length && j < y.length) {
    if (x[i] === y[j]) { out.push({ t: ' ', line: x[i] }); i++; j++ }
    else if (dp[i + 1][j] >= dp[i][j + 1]) out.push({ t: '-', line: x[i++] })
    else out.push({ t: '+', line: y[j++] })
  }
  while (i < x.length) out.push({ t: '-', line: x[i++] })
  while (j < y.length) out.push({ t: '+', line: y[j++] })
  return out
}

export function VersionHistory({ versions }: { versions: Version[] }) {
  return (
    <ol className={styles.history}>
      {[...versions].reverse().map((v) => {
        const prev = versions.find((p) => p.versionNo === v.versionNo - 1)
        const lines = prev ? diffLines(prev.content, v.content) : diffLines('', v.content).filter((d) => d.t === '+')
        return (
          <li key={v.id}>
            <Card>
              <div className={styles.historyMeta}>
                <strong>v{v.versionNo}</strong> <Badge status={v.source} />
                <span className={styles.muted}>by {v.authorId ?? 'system'} · {new Date(v.createdAt).toLocaleString()}</span>
              </div>
              {v.comment && <p className={styles.comment}>“{v.comment}”</p>}
              <details className={styles.details}>
                <summary>{prev ? 'Changes from previous version' : 'Content'}</summary>
                <pre className={styles.diff}>
                  {lines.map((d, k) => (
                    <div key={k} className={`${styles.diffLine} ${d.t === '+' ? styles.add : d.t === '-' ? styles.remove : ''}`}>{d.t} {d.line}</div>
                  ))}
                </pre>
              </details>
            </Card>
          </li>
        )
      })}
    </ol>
  )
}
