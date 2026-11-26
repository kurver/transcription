import { useCallback, useState } from 'react'

export type ThemeChoice = 'system' | 'light' | 'dark'
const KEY = 'transcription.theme'

function read(): ThemeChoice {
  try {
    const v = localStorage.getItem(KEY)
    return v === 'light' || v === 'dark' ? v : 'system'
  } catch {
    return 'system'
  }
}

/** Applies the choice to <html data-theme>; 'system' removes the attribute so the OS setting wins. */
export function applyTheme(choice: ThemeChoice) {
  if (choice === 'system') document.documentElement.removeAttribute('data-theme')
  else document.documentElement.dataset.theme = choice
}

export function useTheme() {
  const [choice, setChoice] = useState<ThemeChoice>(read)
  const update = useCallback((next: ThemeChoice) => {
    setChoice(next)
    applyTheme(next)
    try {
      if (next === 'system') localStorage.removeItem(KEY)
      else localStorage.setItem(KEY, next)
    } catch { /* storage unavailable: choice just won't persist */ }
  }, [])
  return [choice, update] as const
}
