import type { ReactNode } from 'react'
import styles from './Alert.module.css'

export function Alert({ tone = 'danger', children }: { tone?: 'danger' | 'info'; children: ReactNode }) {
  return <div role={tone === 'danger' ? 'alert' : 'status'} className={`${styles.alert} ${styles[tone]}`}>{children}</div>
}
