import { useState } from 'react'
import { useAuth } from '@/auth/context'
import { usePatients } from '@/features/patients/api'
import { PageHeader } from '@/components/app/PageHeader'
import { DataCard, EmptyState, TableSkeleton, td, th } from '@/components/app/DataTable'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { toApiError } from '@/api/errors'
import { useAppointments } from '@/features/appointments/api'
import { useCreateMriOrder, useMriOrderStatus, useMriOrders, type MriOrderStatus } from './api'

export function MriOrdersPage() {
  const {user}=useAuth(); const canOrder=user?.scope==='NEUROLOGIST'; const canUpdate=user?.scope==='MRI_TECHNOLOGIST'
  const patientId=user?.role==='PATIENT'?user.id:undefined
  const query=useMriOrders(patientId); const create=useCreateMriOrder(); const update=useMriOrderStatus()
  const [selectedPatientId,setSelectedPatientId]=useState(''); const [patientFilter,setPatientFilter]=useState('')
  const patientsQuery=usePatients(patientFilter,0,100,canOrder)
  const appointmentsQuery=useAppointments(selectedPatientId||undefined,Boolean(selectedPatientId))
  const [appointmentId,setAppointmentId]=useState(''); const [accessionNumber,setAccession]=useState(''); const [indication,setIndication]=useState(''); const [message,setMessage]=useState(''); const rows=query.data??[]
  async function submit(e:React.FormEvent){e.preventDefault();setMessage('');try{await create.mutateAsync({appointmentId,accessionNumber,indication});setSelectedPatientId('');setPatientFilter('');setAppointmentId('');setAccession('');setIndication('');setMessage('MRI order created.')}catch(err){setMessage(toApiError(err).message)}}
  async function change(id:string,status:MriOrderStatus){try{await update.mutateAsync({id,status})}catch(err){setMessage(toApiError(err).message)}}
  return <><PageHeader eyebrow="Imaging" title="MRI orders" description="Create MRI orders and track each scan from order through reporting."/>
    {canOrder&&<DataCard title="Create an MRI order"><form className="grid gap-3 p-5 md:grid-cols-2" onSubmit={submit}>
      <label className="space-y-1 text-sm">Find patient<Input value={patientFilter} onChange={e=>setPatientFilter(e.target.value)} placeholder="Search by name or patient number"/></label>
      <label className="space-y-1 text-sm">Patient<select required value={selectedPatientId} onChange={e=>{setSelectedPatientId(e.target.value);setAppointmentId('')}} className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm" disabled={patientsQuery.isLoading}><option value="">{patientsQuery.isLoading?'Loading patients…':'Choose a patient'}</option>{(patientsQuery.data?.content??[]).map(p=><option key={p.id} value={p.id}>{p.name} — {p.patientNumber}</option>)}</select></label>
      <label className="space-y-1 text-sm">Appointment<select required value={appointmentId} onChange={e=>setAppointmentId(e.target.value)} className="h-9 w-full rounded-md border border-input bg-background px-3 text-sm" disabled={!selectedPatientId||appointmentsQuery.isLoading}><option value="">{!selectedPatientId?'Select a patient first':appointmentsQuery.isLoading?'Loading appointments…':'Choose an appointment'}</option>{(appointmentsQuery.data??[]).map(a=><option key={a.id} value={a.id}>{new Date(a.scheduledTime).toLocaleString()} — {a.status}</option>)}</select></label>
      <label className="space-y-1 text-sm">Accession number<Input required value={accessionNumber} onChange={e=>setAccession(e.target.value)} maxLength={64}/></label>
      <label className="space-y-1 text-sm md:col-span-2">Clinical indication<Textarea value={indication} onChange={e=>setIndication(e.target.value)}/></label>
      <div className="flex items-center gap-3 md:col-span-2"><Button disabled={create.isPending||!appointmentId}>Create order</Button><span role="status" className="text-sm text-muted-foreground">{patientsQuery.error?toApiError(patientsQuery.error).message:appointmentsQuery.error?toApiError(appointmentsQuery.error).message:message}</span></div>
    </form></DataCard>}
    <div className={canOrder?'mt-5':''}><DataCard title="Orders">{query.isLoading?<TableSkeleton/>:query.error?<EmptyState text={toApiError(query.error).message}/>:rows.length===0?<EmptyState text="No MRI orders found."/>:<div className="overflow-x-auto"><table className="w-full"><thead><tr><th className={th}>Accession</th><th className={th}>Patient</th><th className={th}>Appointment</th><th className={th}>Indication</th><th className={th}>Status</th>{canUpdate&&<th className={th}>Update status</th>}</tr></thead><tbody className="divide-y">{rows.map(o=><tr key={o.id}><td className={`${td} font-mono`}>{o.accessionNumber}</td><td className={td}>{o.patientName}</td><td className={`${td} font-mono text-xs`}>{o.appointmentId}</td><td className={td}>{o.indication||'—'}</td><td className={td}>{o.status}</td>{canUpdate&&<td className={td}><select aria-label={`Status for ${o.accessionNumber}`} className="h-9 rounded-md border bg-background px-2" value={o.status} onChange={e=>void change(o.id,e.target.value as MriOrderStatus)}>{(['ORDERED','SCHEDULED','ACQUIRED','REPORTED','CANCELLED'] as const).map(s=><option key={s}>{s}</option>)}</select></td>}</tr>)}</tbody></table></div>}</DataCard></div>
    {message&&!canOrder&&<p role="status" className="mt-3 text-sm text-destructive">{message}</p>}
  </>
}
