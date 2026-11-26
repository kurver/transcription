import { useTheme, type ThemeChoice } from './useTheme'
import styles from './ThemeToggle.module.css'

const OPTIONS: { value: ThemeChoice; label: string }[] = [
  { value: 'system', label: 'Auto' },
  { value: 'light', label: 'Light' },
  { value: 'dark', label: 'Dark' },
]

export function ThemeToggle() {
  const [choice, setChoice] = useTheme()
  return (
    <div role="group" aria-label="Colour theme" className={styles.group}>
      {OPTIONS.map((o) => (
        <button key={o.value} type="button" aria-pressed={choice === o.value}
          className={`${styles.option} ${choice === o.value ? styles.active : ''}`} onClick={() => setChoice(o.value)}>
          {o.label}
        </button>
      ))}
    </div>
  )
}
