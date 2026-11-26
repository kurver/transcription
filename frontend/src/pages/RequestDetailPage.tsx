import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, type DocView, type RequestInput, type Role } from '../api'
import { Alert, Badge, Button, buttonClass, Card, DataTable, EmptyRow, PageHeader } from '../components'
import { formatDate, isOverdue } from './dates'
import { RequestForm } from './RequestForm'
import styles from './pages.module.css'

const formatSize = (b: number) => (b < 1024 ? `${b} B` : b < 1048576 ? `${(b / 1024).toFixed(1)} KB` : `${(b / 1048576).toFixed(1)} MB`)
const ACTIVE_QA = ['OPEN', 'CLAIMED']

/** One request page for every role; the available actions depend on who is looking. */
export function RequestDetailPage({ role, userId }: { role: Role; userId: number }) {
  const id = Number(useParams().id)
  const qc = useQueryClient()
  const [editing, setEditing] = useState(false)
  const q = useQuery({ queryKey: ['request', id], queryFn: () => api.request(id), refetchInterval: 2500 })
  const refresh = () => qc.invalidateQueries()

  const upload = useMutation({ mutationFn: async (files: File[]) => { for (const f of files) await api.upload(id, f) }, onSuccess: refresh })
  const submit = useMutation({ mutationFn: () => api.submit(id), onSuccess: refresh })
  const retry = useMutation({ mutationFn: () => api.retry(id), onSuccess: refresh })
  const send = useMutation({ mutationFn: () => api.sendToSpecialists(id), onSuccess: refresh })
  const save = useMutation({ mutationFn: (i: RequestInput) => api.updateMetadata(id, i), onSuccess: () => { setEditing(false); refresh() } })
  const claimTask = useMutation({ mutationFn: (taskId: number) => api.claimTask(taskId), onSuccess: refresh })
  const completeTask = useMutation({ mutationFn: (taskId: number) => api.completeTask(taskId), onSuccess: refresh })
  const claimQa = useMutation({ mutationFn: () => api.claimQa(id), onSuccess: refresh })
  const requestQa = useMutation({ mutationFn: (tid: number) => api.requestQa(tid), onSuccess: refresh })

  if (q.error) return <Alert>{q.error.message}</Alert>
  if (!q.data) return <p>Loading…</p>
  const { summary: s, documents, customFields, tasks, reviews } = q.data
  const spec = s.specialist
  const isOwner = role === 'USER'
  const iHoldTask = role === 'TRANSCRIBER' && spec?.status === 'CLAIMED' && spec.assignedTo === userId
  const canSend = isOwner && s.status === 'COMPLETED' && (!spec || spec.status === 'DONE')
  const canRequestQa = (d: DocView) => d.transcriptionId != null && !ACTIVE_QA.includes(d.qaStatus) && (isOwner || iHoldTask)
  const err = upload.error ?? submit.error ?? retry.error ?? send.error ?? claimTask.error ?? completeTask.error ?? claimQa.error ?? requestQa.error
  const initial: RequestInput = {
    title: s.title, caseId: s.caseId ?? '', caseOwner: s.caseOwner ?? '', dueDate: s.dueDate ?? '', priority: s.priority, customFields,
  }

  return (
    <>
      <PageHeader
        title={s.title}
        status={s.status}
        subtitle={[s.caseId && `Case ${s.caseId}`, s.caseOwner && `Owner ${s.caseOwner}`].filter(Boolean).join(' · ') || undefined}
        actions={<>
          {isOwner && (
            <label className={buttonClass({ variant: 'secondary' })}>
              {upload.isPending ? 'Uploading…' : 'Add recordings'}
              <input type="file" accept="audio/*,.mp3,.wav,.m4a,.aac,.ogg,.flac,.wma,.webm" multiple hidden onChange={(e) => { const f = Array.from(e.target.files ?? []); e.target.value = ''; if (f.length) upload.mutate(f) }} />
            </label>
          )}
          {isOwner && s.status === 'DRAFT' && <Button disabled={!documents.length} loading={submit.isPending} onClick={() => submit.mutate()}>Submit for transcription</Button>}
          {isOwner && s.status === 'FAILED' && <Button variant="danger" loading={retry.isPending} onClick={() => retry.mutate()}>Retry failed recordints</Button>}
          {canSend && <Button loading={send.isPending} onClick={() => send.mutate()}>Send to specialist team</Button>}
          {role === 'TRANSCRIBER' && spec?.status === 'OPEN' && <Button loading={claimTask.isPending} onClick={() => claimTask.mutate(spec.taskId)}>Take this request</Button>}
          {iHoldTask && <Button loading={completeTask.isPending} onClick={() => completeTask.mutate(spec!.taskId)}>Mark work complete</Button>}
          {role === 'QA' && s.qa.open > 0 && <Button loading={claimQa.isPending} onClick={() => claimQa.mutate()}>Take for QA review ({s.qa.open})</Button>}
        </>}
      />
      {err && <Alert>{err.message}</Alert>}

      <section className={styles.section}>
        <Card>
          {editing ? (
            <>
              {save.error && <Alert>{save.error.message}</Alert>}
              <RequestForm initial={initial} submitLabel="Save details" pending={save.isPending} onSubmit={(i) => save.mutate(i)} onCancel={() => setEditing(false)} />
            </>
          ) : (
            <>
              <dl className={styles.meta}>
                <div><dt>Case ID</dt><dd>{s.caseId ?? '—'}</dd></div>
                <div><dt>Case owner</dt><dd>{s.caseOwner ?? '—'}</dd></div>
                <div><dt>Due</dt><dd className={isOverdue(s.dueDate) && s.status !== 'COMPLETED' ? styles.overdue : undefined}>{formatDate(s.dueDate)}</dd></div>
                <div><dt>Priority</dt><dd><Badge status={s.priority} /></dd></div>
                <div><dt>Submitted</dt><dd>{s.submittedAt ? new Date(s.submittedAt).toLocaleString() : 'Not yet'}</dd></div>
                {Object.entries(customFields).map(([k, v]) => <div key={k}><dt>{k}</dt><dd>{v || '—'}</dd></div>)}
              </dl>
              {isOwner && <p style={{ marginTop: 'var(--space-4)' }}><Button variant="secondary" size="sm" onClick={() => setEditing(true)}>Edit details</Button></p>}
            </>
          )}
        </Card>
      </section>

      {(spec || s.qa.open + s.qa.claimed + s.qa.passed + s.qa.failed > 0) && (
        <section className={styles.section}>
          <Card>
            <div className={styles.stack}>
              {spec && (
                <div className={styles.row}>
                  <strong>Specialist team</strong> <Badge status={spec.status} /> <Badge status={spec.source} />
                  {spec.reason && <span className={styles.muted}>{spec.reason}</span>}
                </div>
              )}
              {s.qa.open + s.qa.claimed + s.qa.passed + s.qa.failed > 0 && (
                <div className={styles.row}>
                  <strong>QA</strong>
                  {s.qa.open > 0 && <><Badge status="OPEN" /> {s.qa.open} waiting</>}
                  {s.qa.claimed > 0 && <><Badge status="CLAIMED" /> {s.qa.claimed} in review</>}
                  {s.qa.passed > 0 && <><Badge status="PASSED" /> {s.qa.passed}</>}
                  {s.qa.failed > 0 && <><Badge status="FAILED" /> {s.qa.failed}</>}
                </div>
              )}
            </div>
          </Card>
        </section>
      )}

      <section className={styles.section}>
        <h3>Recordings</h3>
        <DataTable>
          <thead><tr><th>Recording</th><th>Size</th><th>Status</th><th>Version</th><th>QA</th><th /></tr></thead>
          <tbody>
            {documents.map((d) => (
              <tr key={d.documentId}>
                <td>{d.transcriptionId ? <Link to={`/transcriptions/${d.transcriptionId}`}>{d.filename}</Link> : d.filename}</td>
                <td>{formatSize(d.sizeBytes)}</td>
                <td><Badge status={d.status} />{d.failureReason && <span className={styles.muted}> {d.failureReason}</span>}</td>
                <td>{d.transcriptionId ? `v${d.versionNo}` : '—'}</td>
                <td>
                  {d.qaStatus !== 'NONE' && <Badge status={d.qaStatus} />}
                  {d.qaComment && <div className={styles.muted}>{d.qaComment}</div>}
                </td>
                <td>{canRequestQa(d) && <Button size="sm" variant="secondary" disabled={requestQa.isPending} onClick={() => requestQa.mutate(d.transcriptionId!)}>Request QA</Button>}</td>
              </tr>
            ))}
            {documents.length === 0 && <EmptyRow cols={6}>No recordings yet.{isOwner && ' Use “Add recordings” to upload audio.'}</EmptyRow>}
          </tbody>
        </DataTable>
      </section>

      {(tasks.length > 0 || reviews.length > 0) && role !== 'USER' && (
        <section className={styles.section}>
          <h3>Workflow history</h3>
          <DataTable>
            <thead><tr><th>Type</th><th>When</th><th>Source</th><th>Status</th><th>Detail</th></tr></thead>
            <tbody>
              {tasks.map((t) => (
                <tr key={`t${t.id}`}>
                  <td>Specialist task</td><td>{new Date(t.createdAt).toLocaleString()}</td>
                  <td><Badge status={t.source} /></td><td><Badge status={t.status} /></td><td className={styles.muted}>{t.reason}</td>
                </tr>
              ))}
              {reviews.map((r) => (
                <tr key={`r${r.id}`}>
                  <td>QA review · {documents.find((d) => d.transcriptionId === r.transcriptionId)?.filename}</td><td>{new Date(r.createdAt).toLocaleString()}</td>
                  <td><Badge status={r.source} /></td><td><Badge status={r.status} /></td><td className={styles.muted}>{r.comment}</td>
                </tr>
              ))}
            </tbody>
          </DataTable>
        </section>
      )}
    </>
  )
}
