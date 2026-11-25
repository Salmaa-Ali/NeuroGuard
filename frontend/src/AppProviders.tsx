import type { QueryClient } from '@tanstack/react-query'
import { QueryClientProvider } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { AuthProvider } from './auth/AuthProvider'
import { ToastProvider } from './components/Toast'
import { Toaster } from './components/ui/sonner'

/** Data fetching, toasts and auth. (The router goes inside these, see main.tsx.) */
export function AppProviders({ client, children }: { client: QueryClient; children: ReactNode }) {
  return (
    <QueryClientProvider client={client}>
      <ToastProvider>
        <AuthProvider>
          {children}
          <Toaster />
        </AuthProvider>
      </ToastProvider>
    </QueryClientProvider>
  )
}
