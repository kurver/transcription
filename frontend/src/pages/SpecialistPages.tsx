import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from '../api'
import { Alert, Badge, Button, PageHeader } from '../components'
import { RequestTable } from './RequestTable'
import styles from './pages.module.css'

/** Shared queue for all transcribers: take a request, work through its recordings, mark it complete. */
export function SpecialistQueue() {
  const qc = useQueryClient()
  const q = useQuery({ queryKey: ['specialist'], queryFn: api.specialistQueue, refetchInterval: 3000 })
  const claim = useMutation({ mutationFn: (taskId: number) => api.claimTask(taskId), onSuccess: () => qc.invalidateQueries() })
  return (
    <>
      <PageHeader title="Transcriber queue" subtitle="Shared by all transcribers. Take a request to work on its recordings; it then leaves everyone else's queue." />
      {claim.error && <Alert>{claim.error.message}</Alert>}
      <RequestTable
        requests={q.data}
        empty="The queue is empty."
        extraHeader="Queue state"
        extra={(r) => r.specialist && (
          <div className={styles.row}>
            <Badge status={r.specialist.status} />
            {r.specialist.source === 'QA_FAILURE' && <Badge status="QA_FAILURE" />}
            {r.specialist.status === 'OPEN' && <Button size="sm" onClick={() => claim.mutate(r.specialist!.taskId)}>Take</Button>}
            {r.specialist.status === 'CLAIMED' && <span className={styles.muted}>yours</span>}
          </div>
        )}
      />
    </>
  )
}
