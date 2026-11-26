import { useState } from 'react'
import type { Priority, RequestInput } from '../api'
import { Button, Field, Input, Select } from '../components'
import styles from './pages.module.css'

const PRIORITIES: Priority[] = ['LOW', 'NORMAL', 'HIGH', 'URGENT']

interface Props {
  initial?: RequestInput
  submitLabel: string
  pending?: boolean
  onSubmit: (input: RequestInput) => void
  onCancel?: () => void
}

/** Request metadata: fixed fields plus free-form custom fields. Used to create and to edit. */
export function RequestForm({ initial, submitLabel, pending, onSubmit, onCancel }: Props) {
  const [title, setTitle] = useState(initial?.title ?? '')
  const [caseId, setCaseId] = useState(initial?.caseId ?? '')
  const [caseOwner, setCaseOwner] = useState(initial?.caseOwner ?? '')
  const [dueDate, setDueDate] = useState(initial?.dueDate ?? '')
  const [priority, setPriority] = useState<Priority>(initial?.priority ?? 'NORMAL')
  const [custom, setCustom] = useState<{ key: string; value: string }[]>(
    Object.entries(initial?.customFields ?? {}).map(([key, value]) => ({ key, value })),
  )
  const setRow = (i: number, patch: Partial<{ key: string; value: string }>) =>
    setCustom((rows) => rows.map((r, k) => (k === i ? { ...r, ...patch } : r)))

  return (
    <form
      className={styles.stack}
      onSubmit={(e) => {
        e.preventDefault()
        const customFields: Record<string, string> = {}
        custom.forEach((r) => { if (r.key.trim()) customFields[r.key.trim()] = r.value })
        onSubmit({ title, caseId, caseOwner, dueDate, priority, customFields })
      }}
    >
      <div className={styles.formGrid}>
        <Field label="Request title"><Input value={title} onChange={(e) => setTitle(e.target.value)} required /></Field>
        <Field label="Case ID"><Input value={caseId} onChange={(e) => setCaseId(e.target.value)} placeholder="e.g. C-1042" /></Field>
        <Field label="Case owner"><Input value={caseOwner} onChange={(e) => setCaseOwner(e.target.value)} /></Field>
        <Field label="Due date"><Input type="date" value={dueDate} onChange={(e) => setDueDate(e.target.value)} /></Field>
        <Field label="Priority">
          <Select value={priority} onChange={(e) => setPriority(e.target.value as Priority)}>
            {PRIORITIES.map((p) => <option key={p} value={p}>{p.charAt(0) + p.slice(1).toLowerCase()}</option>)}
          </Select>
        </Field>
      </div>

      <fieldset className={styles.fieldset}>
        <legend>Custom fields</legend>
        {custom.map((row, i) => (
          <div key={i} className={styles.customRow}>
            <Input aria-label="Field name" placeholder="Name" value={row.key} onChange={(e) => setRow(i, { key: e.target.value })} />
            <Input aria-label="Field value" placeholder="Value" value={row.value} onChange={(e) => setRow(i, { value: e.target.value })} />
            <Button type="button" variant="ghost" size="sm" onClick={() => setCustom((rows) => rows.filter((_, k) => k !== i))}>Remove</Button>
          </div>
        ))}
        <div><Button type="button" variant="secondary" size="sm" onClick={() => setCustom((rows) => [...rows, { key: '', value: '' }])}>Add custom field</Button></div>
      </fieldset>

      <div className={styles.buttons}>
        <Button loading={pending}>{submitLabel}</Button>
        {onCancel && <Button type="button" variant="ghost" onClick={onCancel}>Cancel</Button>}
      </div>
    </form>
  )
}
