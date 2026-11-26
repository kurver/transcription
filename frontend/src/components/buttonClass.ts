import styles from './Button.module.css'

export type ButtonVariant = 'primary' | 'secondary' | 'ghost' | 'danger'

/** Class names for button styling; lets a <label> (file picker) look like a Button. */
export const buttonClass = ({ variant = 'primary', size = 'md' }: { variant?: ButtonVariant; size?: 'sm' | 'md' } = {}) =>
  [styles.button, styles[variant], styles[size]].join(' ')
