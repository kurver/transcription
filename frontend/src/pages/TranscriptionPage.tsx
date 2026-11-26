import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, type Role } from '../api'
import { Alert, Button, Card, Field, Input, PageHeader, Textarea } from '../components'
import { VersionHistory } from './QaPages'
import styles from './pages.module.css'

/** A single recording's transcript, with playback of the original audio. Always reached from its request; actions depend on role and workflow state. */
export function TranscriptionPage({ role, userId }: { role: Role; userId: number }) {
  const id = Number(useParams().id)
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['transcription', id], queryFn: () => api.transcription(id) })
  const audio = useQuery({ queryKey: ['audio', id], queryFn: () => api.audioUrl(id), staleTime: Infinity, gcTime: 0 })
  const versions = useQuery({ queryKey: ['versions', id], queryFn: () => api.versions(id) })
  const [draft, setDraft] = useState<string | null>(null)
  const [comment, setComment] = useState('')
  const [verdictComment, setVerdictComment] = useState('')
  const refresh = () => qc.invalidateQueries()
  const requestQa = useMutation({ mutationFn: () => api.requestQa(id), onSuccess: refresh })
  const save = useMutation({
    mutationFn: (mode: 'EDIT' | 'REPLACEMENT') => api.addVersion(id, mode, draft ?? '', comment),
    onSuccess: () => { setDraft(null); setComment(''); refresh() },
  })
  const verdict = useMutation({
    mutationFn: (v: 'PASS' | 'FAIL') => api.verdict(q.data!.summary.qaReviewId!, v, verdictComment),
    onSuccess: () => { setVerdictComment(''); refresh() },
  })

  if (q.error) return <Alert>{q.error.message}</Alert>
  if (!q.data) return <p>Loading…</p>
  const s = q.data.summary
  const qaActive = s.qaStatus === 'OPEN' || s.qaStatus === 'CLAIMED'
  const canEdit = role === 'TRANSCRIBER' && s.taskStatus === 'CLAIMED' && s.taskAssignedTo === userId
  const canRequestQa = (role === 'USER' || canEdit) && !qaActive
  const canVerdict = role === 'QA' && s.qaStatus === 'CLAIMED' && s.qaAssignedTo === userId
  const err = requestQa.error ?? save.error ?? verdict.error
  return (
    <>
      <p style={{ marginBottom: 'var(--space-3)' }}>
        <Link to={`/requests/${s.requestId}`}>← {s.requestTitle}{s.caseId && ` (Case ${s.caseId})`}</Link>
      </p>
      <PageHeader
        title={s.documentName}
        status={s.qaStatus === 'NONE' ? undefined : s.qaStatus}
        subtitle={`Version ${s.currentVersionNo} of ${s.versionCount}`}
        actions={<>
          {canRequestQa && <Button variant="secondary" loading={requestQa.isPending} onClick={() => requestQa.mutate()}>Request QA for this recording</Button>}
          {canEdit && draft === null && <Button onClick={() => setDraft(q.data!.content)}>Edit / replace…</Button>}
        </>}
      />
      {err && <Alert>{err.message}</Alert>}
      <Card className={styles.section}>
        {audio.data ? <audio controls src={audio.data} className={styles.audio} aria-label={`Recording: ${s.documentName}`} /> : audio.error ? <p>{audio.error.message}</p> : <p>Loading recording…</p>}
      </Card>
      {s.qaComment && <Alert tone="info">QA comment: {s.qaComment}</Alert>}

      {canVerdict && (
        <Card className={styles.section}>
          <div className={styles.verdict}>
            <strong>QA verdict</strong>
            <Field label="Comment" hint="Required when failing a transcript. A failed recording goes back to the transcriber queue.">
              <Input value={verdictComment} onChange={(e) => setVerdictComment(e.target.value)} />
            </Field>
            <div className={styles.buttons}>
              <Button loading={verdict.isPending} onClick={() => verdict.mutate('PASS')}>Pass</Button>
              <Button variant="danger" disabled={!verdictComment.trim()} loading={verdict.isPending} onClick={() => verdict.mutate('FAIL')}>Fail</Button>
            </div>
          </div>
        </Card>
      )}

      {draft === null ? (
        <Card className={styles.transcript}><pre>{q.data.content}</pre></Card>
      ) : (
        <Card className={styles.editor}>
          <div className={styles.stack}>
            <Field label="Transcription" hint="Saving creates a new version. Earlier versions are always kept.">
              <Textarea rows={14} value={draft} onChange={(e) => setDraft(e.target.value)} />
            </Field>
            <Field label="Comment (required)">
              <Input placeholder="Why this change?" value={comment} onChange={(e) => setComment(e.target.value)} />
            </Field>
            <div className={styles.buttons}>
              <Button disabled={!comment.trim() || draft === q.data.content} loading={save.isPending} onClick={() => save.mutate('EDIT')}>Save as edit</Button>
              <Button variant="secondary" disabled={!comment.trim()} loading={save.isPending} onClick={() => save.mutate('REPLACEMENT')}>Save as full replacement</Button>
              <Button variant="ghost" onClick={() => setDraft(null)}>Cancel</Button>
            </div>
          </div>
        </Card>
      )}
      <h3>History</h3>
      {versions.data && <VersionHistory versions={versions.data} />}
    </>
  )
}
