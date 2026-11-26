import { useQuery } from '@tanstack/react-query'
import { Navigate, Route, Routes, useNavigate } from 'react-router-dom'
import { api, getUserId, setUserId, type Role } from './api'
import { Alert, AppShell, EmptyState } from './components'
import { queryClient } from './main'
import { QaAllRequests, QaAudit, QaQueue } from './pages/QaPages'
import { RequestDetailPage } from './pages/RequestDetailPage'
import { SpecialistQueue } from './pages/SpecialistPages'
import { TranscriptionPage } from './pages/TranscriptionPage'
import { CompletedQueue, RequestsPage } from './pages/UserPages'

const HOME: Record<Role, string> = { USER: '/requests', TRANSCRIBER: '/specialist', QA: '/qa' }

export default function App() {
  const navigate = useNavigate()
  const users = useQuery({ queryKey: ['users'], queryFn: api.users })
  const me = users.data?.find((u) => String(u.id) === getUserId())

  if (users.isLoading) return <main style={{ padding: 'var(--space-6)' }}>Loading…</main>
  if (users.error) return <main style={{ padding: 'var(--space-6)' }}><Alert>Cannot reach the backend: {users.error.message}</Alert></main>

  const switchUser = (id: number) => {
    setUserId(id)
    queryClient.clear()
    navigate(HOME[users.data!.find((x) => x.id === id)!.role])
  }

  return (
    <AppShell users={users.data!} me={me} onSwitch={switchUser}>
      {!me ? <EmptyState>Select a user above to begin (dev role switcher).</EmptyState> : (
        <Routes>
          <Route path="/requests" element={<RequestsPage />} />
          <Route path="/requests/:id" element={<RequestDetailPage role={me.role} userId={me.id} />} />
          <Route path="/completed" element={<CompletedQueue />} />
          <Route path="/specialist" element={<SpecialistQueue />} />
          <Route path="/qa" element={<QaQueue />} />
          <Route path="/qa/all" element={<QaAllRequests />} />
          <Route path="/qa/audit" element={<QaAudit />} />
          <Route path="/transcriptions/:id" element={<TranscriptionPage role={me.role} userId={me.id} />} />
          <Route path="*" element={<Navigate to={HOME[me.role]} replace />} />
        </Routes>
      )}
    </AppShell>
  )
}
