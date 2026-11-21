import { Link } from 'react-router-dom'

/** The "403" screen. Shown by route guards and by data views when the backend answers 403. */
export function AccessDenied() {
  return (
    <div className="mx-auto max-w-md py-16 text-center">
      <p className="text-5xl font-semibold text-accent-600">403</p>
      <h1 className="mt-3 text-xl font-semibold text-slate-900">
        You don&apos;t have access to this page
      </h1>
      <p className="mt-2 text-sm text-slate-600">
        Your account is signed in, but its role doesn&apos;t include this area. If you think this is
        a mistake, ask an administrator.
      </p>
      <Link
        to="/"
        className="mt-6 inline-block rounded-md bg-accent-600 px-4 py-2 text-sm font-medium text-white hover:bg-accent-700"
      >
        Back to dashboard
      </Link>
    </div>
  )
}
