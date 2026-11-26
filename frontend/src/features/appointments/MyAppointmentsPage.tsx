import { useAuth } from '@/auth/context'
import { PageHeader } from '@/components/app/PageHeader'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { toApiError } from '@/api/errors'
import { useAppointments } from './api'

export function MyAppointmentsPage() {
  const {user}=useAuth(); const query=useAppointments(user?.id); const rows=query.data??[]
  return <><PageHeader eyebrow="Appointments" title="My appointments" description="Your upcoming and past neurology consultations and MRI slots."/><DataCard title="Appointment history">{query.isLoading?<TableSkeleton/>:query.error?<EmptyState text={toApiError(query.error).message}/>:rows.length===0?<EmptyState text="You have no appointments yet."/>:<div className="overflow-x-auto"><table className="w-full"><thead><tr><th className={th}>Date</th><th className={th}>Clinician</th><th className={th}>Duration</th><th className={th}>Reason</th><th className={th}>Status</th></tr></thead><tbody className="divide-y">{rows.map(a=><tr key={a.id}><td className={td}>{new Date(a.scheduledTime).toLocaleString()}</td><td className={td}>{a.staffName}</td><td className={td}>{a.durationMinutes} min</td><td className={td}>{a.reason||'—'}</td><td className={td}>{a.status}</td></tr>)}</tbody></table></div>}</DataCard></>
}
