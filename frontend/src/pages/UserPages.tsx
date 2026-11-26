import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { api } from '../api'
import { Alert, Button, Card, PageHeader } from '../components'
import { RequestForm } from './RequestForm'
import { RequestTable } from './RequestTable'

export function RequestsPage() {
  const qc = useQueryClient()
  const navigate = useNavigate()
  const [creating, setCreating] = useState(false)
  const list = useQuery({ queryKey: ['requests'], queryFn: api.requests, refetchInterval: 3000 })
  const create = useMutation({
    mutationFn: api.createRequest,
    onSuccess: (r) => { qc.invalidateQueries({ queryKey: ['requests'] }); navigate(`/requests/${r.id}`) },
  })
  const demo = useMutation({
    mutationFn: (n: number) => api.generateDemo(n),
    onSuccess: () => qc.invalidateQueries({ queryKey: ['requests'] }),
  })
  return (
    <>
      <PageHeader
        title="My requests"
        subtitle="A request groups the audio recordings for one piece of work. Add case details, upload recordings, then submit."
        actions={!creating && (
          <>
            <Button variant="secondary" loading={demo.isPending} onClick={() => demo.mutate(3)} title="Test feature: creates and submits sample requests with placeholder audio recordings">
              Generate 3 demo requests
            </Button>
            <Button onClick={() => setCreating(true)}>New request</Button>
          </>
        )}
      />
      {demo.error && <Alert>{demo.error.message}</Alert>}
      {creating && (
        <Card style={{ marginBottom: 'var(--space-5)' }}>
          {create.error && <Alert>{create.error.message}</Alert>}
          <RequestForm submitLabel="Create request" pending={create.isPending} onSubmit={(i) => create.mutate(i)} onCancel={() => setCreating(false)} />
        </Card>
      )}
      <RequestTable requests={list.data} empty="No requests yet. Create one to get started." />
    </>
  )
}

export function CompletedQueue() {
  const q = useQuery({ queryKey: ['completed'], queryFn: api.completed, refetchInterval: 3000 })
  return (
    <>
      <PageHeader title="Completed queue" subtitle="Requests whose recordings are all transcribed. Only you can see these." />
      <RequestTable requests={q.data} empty="Nothing completed yet." highlightOverdue={false} />
    </>
  )
}
