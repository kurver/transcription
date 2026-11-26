import type { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import type { AppUser } from '../api'
import { Select } from './Field'
import { ThemeToggle } from './ThemeToggle'
import styles from './AppShell.module.css'

const NAV: Record<AppUser['role'], { to: string; label: string }[]> = {
  USER: [{ to: '/requests', label: 'My requests' }, { to: '/completed', label: 'Completed queue' }],
  TRANSCRIBER: [{ to: '/specialist', label: 'Transcriber queue' }],
  QA: [{ to: '/qa', label: 'QA queue' }, { to: '/qa/all', label: 'All requests' }, { to: '/qa/audit', label: 'Audit log' }],
}

interface Props { users: AppUser[]; me?: AppUser; onSwitch: (id: number) => void; children: ReactNode }

export function AppShell({ users, me, onSwitch, children }: Props) {
  return (
    <>
      <header className={styles.header}>
        <div className={styles.inner}>
          <span className={styles.brand}>Transcription</span>
          <nav className={styles.nav} aria-label="Main">
            {me && NAV[me.role].map((n) => (
              <NavLink key={n.to} to={n.to} end className={({ isActive }) => `${styles.link} ${isActive ? styles.active : ''}`}>{n.label}</NavLink>
            ))}
          </nav>
          <div className={styles.controls}>
            <ThemeToggle />
            <label className={styles.switcher}>
              <span>Acting as</span>
              <Select value={me?.id ?? ''} onChange={(e) => onSwitch(Number(e.target.value))}>
                <option value="" disabled>Select a user…</option>
                {users.map((u) => <option key={u.id} value={u.id}>{u.name}</option>)}
              </Select>
            </label>
          </div>
        </div>
      </header>
      <main className={styles.main}>{children}</main>
    </>
  )
}
