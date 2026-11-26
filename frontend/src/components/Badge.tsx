import { statusLabel, statusTone } from './statusTone'
import styles from './Badge.module.css'

export function Badge({ status }: { status: string }) {
  if (!status) return null
  return (
    <span className={`${styles.badge} ${styles[statusTone(status)]}`}>
      {statusLabel(status)}
    </span>
  )
}
