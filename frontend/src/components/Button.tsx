import type { ButtonHTMLAttributes } from 'react'
import styles from './Button.module.css'
import { buttonClass, type ButtonVariant } from './buttonClass'

interface Props extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: ButtonVariant
  size?: 'sm' | 'md'
  loading?: boolean
}

export function Button({ variant, size, loading, disabled, className, children, ...rest }: Props) {
  return (
    <button {...rest} disabled={disabled || loading} className={[buttonClass({ variant, size }), className].filter(Boolean).join(' ')}>
      {loading && <span className={styles.spinner} aria-hidden />}
      {children}
    </button>
  )
}
