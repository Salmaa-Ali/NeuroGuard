import { useId, type ComponentProps } from 'react'

export interface SelectProps extends ComponentProps<'select'> {
  label: string
  error?: string
}

/** Labelled select with an accessible error message. Works with react-hook-form's register(). */
export function Select({ label, error, id, className = '', children, ...rest }: SelectProps) {
  const autoId = useId()
  const selectId = id ?? autoId
  const errorId = `${selectId}-error`

  return (
    <div className="space-y-1">
      <label htmlFor={selectId} className="block text-sm font-medium text-slate-700">
        {label}
      </label>
      <select
        id={selectId}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? errorId : undefined}
        className={`block w-full rounded-md border bg-white px-3 py-2 text-sm shadow-sm disabled:bg-slate-100 ${error ? 'border-red-500' : 'border-slate-300'} ${className}`}
        {...rest}
      >
        {children}
      </select>
      {error && (
        <p id={errorId} role="alert" className="text-xs text-red-600">
          {error}
        </p>
      )}
    </div>
  )
}
