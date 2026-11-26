export type Role = 'USER' | 'TRANSCRIBER' | 'QA'
export type Priority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
export interface AppUser { id: number; name: string; role: Role }

export interface SpecialistInfo { taskId: number; status: 'OPEN' | 'CLAIMED' | 'DONE'; assignedTo?: number; source: string; reason?: string }
export interface QaCounts { open: number; claimed: number; passed: number; failed: number }
/** Request-level summary: the unit every queue and list works with. */
export interface Req {
  id: number; title: string; status: string; ownerId: number; caseId?: string; caseOwner?: string; dueDate?: string
  priority: Priority; createdAt: string; submittedAt?: string; documentCount: number; transcribedCount: number
  specialist?: SpecialistInfo; qa: QaCounts
}
export interface DocView {
  documentId: number; filename: string; sizeBytes: number; status: string; failureReason?: string
  transcriptionId?: number; versionNo: number; versionCount: number
  qaStatus: string; qaReviewId?: number; qaAssignedTo?: number; qaComment?: string
}
export interface Task { id: number; requestId: number; createdBy: number; createdAt: string; assignedTo?: number; status: string; source: string; reason?: string; completedAt?: string }
export interface Review {
  id: number; transcriptionId: number; requestId: number; source: string; requestedBy?: number; createdAt: string
  assignedTo?: number; status: string; reviewedVersionNo?: number; comment?: string; completedAt?: string
}
export interface RequestDetail { summary: Req; customFields: Record<string, string>; documents: DocView[]; tasks: Task[]; reviews: Review[] }
export interface RequestInput {
  title: string; caseId: string; caseOwner: string; dueDate: string; priority: Priority; customFields: Record<string, string>
}

export interface TranscriptionView {
  id: number; requestId: number; requestTitle: string; caseId?: string; documentId: number; documentName: string
  currentVersionNo: number; versionCount: number; qaStatus: string; qaReviewId?: number; qaAssignedTo?: number; qaComment?: string
  taskId?: number; taskStatus?: string; taskAssignedTo?: number
}
export interface Version {
  id: number; versionNo: number; content: string; source: string; authorId?: number; comment?: string; createdAt: string
}
export interface Detail { summary: TranscriptionView; content: string; currentVersion: Version }
export interface Audit { id: number; actorId?: number; entityType: string; entityId: number; action: string; detail?: string; createdAt: string }

const KEY = 'transcription.userId'
export const getUserId = () => localStorage.getItem(KEY)
export const setUserId = (id: number) => localStorage.setItem(KEY, String(id))

async function call<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  const uid = getUserId()
  if (uid) headers.set('X-User-Id', uid)
  if (typeof init.body === 'string') headers.set('Content-Type', 'application/json')
  const res = await fetch('/api' + path, { ...init, headers })
  if (!res.ok) {
    let msg = res.statusText
    try { msg = (await res.json()).error ?? msg } catch { /* not json */ }
    throw new Error(msg)
  }
  return res.json() as Promise<T>
}

const post = <T,>(path: string, body?: unknown) =>
  call<T>(path, { method: 'POST', body: body === undefined ? undefined : JSON.stringify(body) })

/** Empty strings become null so optional metadata is cleared rather than stored as "". */
const payload = (i: RequestInput) => ({ ...i, caseId: i.caseId || null, caseOwner: i.caseOwner || null, dueDate: i.dueDate || null })

export const api = {
  users: () => call<AppUser[]>('/users'),
  // requests
  requests: () => call<Req[]>('/requests'),
  generateDemo: (count: number) => post<Req[]>(`/demo/requests?count=${count}&submit=true`, {}),
  createRequest: (i: RequestInput) => post<Req>('/requests', payload(i)),
  updateMetadata: (id: number, i: RequestInput) => call<Req>(`/requests/${id}/metadata`, { method: 'PUT', body: JSON.stringify(payload(i)) }),
  request: (id: number) => call<RequestDetail>(`/requests/${id}`),
  upload: (id: number, file: File) => {
    const fd = new FormData()
    fd.append('file', file)
    return call(`/requests/${id}/documents`, { method: 'POST', body: fd })
  },
  /** Fetched with the identity header (an <audio src> can't send it) and returned as a playable object URL. */
  audioUrl: async (transcriptionId: number) => {
    const res = await fetch(`/api/transcriptions/${transcriptionId}/audio`, { headers: { 'X-User-Id': getUserId() ?? '' } })
    if (!res.ok) throw new Error('Could not load recording')
    return URL.createObjectURL(await res.blob())
  },
  submit: (id: number) => post<Req>(`/requests/${id}/submit`),
  retry: (id: number) => post<Req>(`/requests/${id}/retry`),
  sendToSpecialists: (id: number, reason?: string) => post(`/requests/${id}/send-to-specialists`, { reason }),
  completed: () => call<Req[]>('/queue/completed'),
  // documents' transcriptions
  transcription: (id: number) => call<Detail>(`/transcriptions/${id}`),
  versions: (id: number) => call<Version[]>(`/transcriptions/${id}/versions`),
  addVersion: (id: number, mode: 'EDIT' | 'REPLACEMENT', content: string, comment: string) =>
    post<Version>(`/transcriptions/${id}/versions`, { mode, content, comment }),
  requestQa: (transcriptionId: number) => post(`/transcriptions/${transcriptionId}/qa-request`),
  // shared specialist queue
  specialistQueue: () => call<Req[]>('/specialist/queue'),
  claimTask: (taskId: number) => post(`/specialist/tasks/${taskId}/claim`),
  completeTask: (taskId: number) => post(`/specialist/tasks/${taskId}/complete`),
  // shared QA queue
  qaQueue: () => call<Req[]>('/qa/queue'),
  qaAll: () => call<Req[]>('/qa/requests'),
  claimQa: (requestId: number) => post(`/qa/requests/${requestId}/claim`),
  verdict: (reviewId: number, verdict: 'PASS' | 'FAIL', comment: string) => post(`/qa/reviews/${reviewId}/verdict`, { verdict, comment }),
  audit: () => call<Audit[]>('/qa/audit'),
}
