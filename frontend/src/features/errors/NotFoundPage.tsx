import { Link } from 'react-router-dom'
import { Button } from '@/components/ui/button'

export function NotFoundPage() {
  return (
    <div className="mx-auto max-w-md py-20 text-center">
      <p className="text-6xl font-bold text-primary">404</p>
      <h1 className="mt-4 text-2xl font-semibold tracking-tight text-foreground">Page not found</h1>
      <p className="mt-2 text-sm text-muted-foreground">
        The page you are looking for doesn&apos;t exist or has moved.
      </p>
      <div className="mt-6">
        <Button asChild>
          <Link to="/">Back to dashboard</Link>
        </Button>
      </div>
    </div>
  )
}
