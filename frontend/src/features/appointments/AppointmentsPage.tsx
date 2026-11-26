import { useState } from 'react'
import { PageHeader } from '@/components/app/PageHeader'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { toApiError } from '@/api/errors'
import { useAuth } from '@/auth/context'
import { usePatients } from '@/features/patients/api'
import { useAppointmentStatus, useAppointments, useCreateAppointment, type AppointmentStatus } from './api'

export function AppointmentsPage() {
  const {user}=useAuth(); const canSchedule=user?.scope==='NEUROLOGIST'; const query=useAppointments(); const create=useCreateAppointment(); const update=useAppointmentStatus()
  const [patientId,setPatientId]=useState(''); const [patientFilter,setPatientFilter]=useState('')
  const patientsQuery=usePatients(patientFilter,0,100,canSchedule)
  const [when,setWhen]=useState(''); const [reason,setReason]=useState(''); const [message,setMessage]=useState('')
  const rows=query.data??[]
  async function submit(e:React.FormEvent) { e.preventDefault(); setMessage(''); try { await create.mutateAsync({patientId,staffId:user?.id,scheduledTime:new Date(when).toISOString(),durationMinutes:30,reason}); setPatientId('');setPatientFilter('');setWhen('');setReason('');setMessage('Appointment scheduled.') } catch(err) { const error=toApiError(err); setMessage(error.fieldErrors.length?`${error.message}: ${error.fieldErrors.map(field=>`${field.field} ${field.message}`).join(', ')}`:error.message) } }
  async function change(id:string,status:AppointmentStatus) { try { await update.mutateAsync({id,status}) } catch(err) {setMessage(toApiError(err).message)} }
  return <><PageHeader eyebrow="Scheduling" title="Appointments" description="Schedule visits and manage upcoming and past appointments." />
    {canSchedule&&<DataCard title="Schedule an appointment"><form className="grid gap-3 p-5 md:grid-cols-2" onSubmit={submit}>
      <label className="space-y-1 text-sm">Find patient<Input value={patientFilter} onChange={e=>setPatientFilter(e.target.value)} placeholder="Search by name or patient number" /></label>
      <label className="space-y-1 text-sm">Patient<select required value={patientId} onChange={e=>setPatientId(e.target.value)} className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm" disabled={patientsQuery.isLoading}><option value="">{patientsQuery.isLoading?'Loading patients…':'Choose a patient'}</option>{(patientsQuery.data?.content??[]).map(p=><option key={p.id} value={p.id}>{p.name} — {p.patientNumber}</option>)}</select></label>
      <label className="space-y-1 text-sm">Date and time<Input required type="datetime-local" value={when} onChange={e=>setWhen(e.target.value)} /></label>
      <label className="space-y-1 text-sm">Reason<Textarea value={reason} onChange={e=>setReason(e.target.value)} /></label>
      <div className="flex items-center gap-3 md:col-span-2"><Button disabled={create.isPending||!patientId||!user?.id}>Schedule</Button><span role="status" className="text-sm text-muted-foreground">{patientsQuery.error?toApiError(patientsQuery.error).message:message}</span></div>
    </form></DataCard>}
    <div className={canSchedule?'mt-5':''}><DataCard title="All appointments">{query.isLoading?<TableSkeleton/>:query.error?<EmptyState text={toApiError(query.error).message}/>:rows.length===0?<EmptyState text="No appointments yet."/>:<div className="overflow-x-auto"><table className="w-full"><thead><tr><th className={th}>Date</th><th className={th}>Patient</th><th className={th}>Clinician</th><th className={th}>Reason</th><th className={th}>Status</th>{canSchedule&&<th className={th}>Actions</th>}</tr></thead><tbody className="divide-y">{rows.map(a=><tr key={a.id}><td className={td}>{new Date(a.scheduledTime).toLocaleString()}</td><td className={td}>{a.patientName}</td><td className={td}>{a.staffName}</td><td className={td}>{a.reason||'—'}</td><td className={td}>{a.status}</td>{canSchedule&&<td className={td}><select aria-label={`Status for ${a.patientName}`} className="h-9 rounded-md border bg-background px-2" value={a.status} onChange={e=>void change(a.id,e.target.value as AppointmentStatus)}>{(['SCHEDULED','COMPLETED','CANCELLED','MISSED'] as const).map(s=><option key={s}>{s}</option>)}</select></td>}</tr>)}</tbody></table></div>}</DataCard></div>
  </>
}
