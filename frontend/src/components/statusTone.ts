export type Tone = 'neutral' | 'info' | 'success' | 'warning' | 'danger' | 'review'

/** Single source of truth: workflow status → visual tone. */
const TONES: Record<string, Tone> = {
  // request / document
  DRAFT: 'neutral', PENDING: 'neutral', QUEUED: 'info', PROCESSING: 'info',
  COMPLETED: 'success', TRANSCRIBED: 'success', FAILED: 'danger',
  // review / escalation
  NONE: 'neutral', ESCALATED: 'warning', OPEN: 'warning', IN_REVIEW: 'review', CLAIMED: 'review',
  REVIEWED: 'success', DONE: 'success',
  // priority
  LOW: 'neutral', NORMAL: 'info', HIGH: 'warning', URGENT: 'danger',
  // QA review state, task / review sources
  PASSED: 'success', USER_REQUEST: 'neutral', TRANSCRIBER_REQUEST: 'neutral', SAMPLED: 'info', RECHECK: 'warning', QA_FAILURE: 'danger',
  // version source
  MOCK_API: 'neutral', EDIT: 'info', REPLACEMENT: 'review',
}

export const statusTone = (status: string): Tone => TONES[status] ?? 'neutral'

const LABELS: Record<string, string> = { MOCK_API: 'Mock API', QA_FAILURE: 'QA failure', NONE: 'No QA' }

export const statusLabel = (status: string): string => LABELS[status] ?? status.replace(/_/g, ' ').toLowerCase()
