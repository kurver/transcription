import type { ReactNode } from 'react'
import { Badge } from './Badge'
import styles from './PageHeader.module.css'

interface Props { title: string; status?: string; subtitle?: ReactNode; actions?: ReactNode }

export function PageHeader({ title, status, subtitle, actions }: Props) {
  return (
    <div className={styles.header}>
      <div>
        <h2 className={styles.title}>{title} {status && <Badge status={status} />}</h2>
        {subtitle && <p className={styles.subtitle}>{subtitle}</p>}
      </div>
      {actions && <div className={styles.actions}>{actions}</div>}
    </div>
  )
}
