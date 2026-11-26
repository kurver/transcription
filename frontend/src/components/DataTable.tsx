import type { ReactNode } from 'react'
import styles from './DataTable.module.css'

/** Styled scroll container + table. Pass <thead>/<tbody> as children. */
export function DataTable({ children }: { children: ReactNode }) {
  return (
    <div className={styles.wrap}>
      <table className={styles.table}>{children}</table>
    </div>
  )
}

export function EmptyRow({ cols, children }: { cols: number; children: ReactNode }) {
  return (
    <tr>
      <td colSpan={cols} className={styles.empty}>{children}</td>
    </tr>
  )
}
