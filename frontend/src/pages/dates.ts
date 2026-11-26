/** Due dates are plain dates (YYYY-MM-DD); compare as strings in local time to avoid timezone drift. */
export const todayIso = () => {
  const d = new Date()
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`
}

export const isOverdue = (due?: string) => !!due && due < todayIso()

export const formatDate = (iso?: string) => (iso ? new Date(iso + 'T00:00:00').toLocaleDateString() : '—')
