import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import type { CalibrationDetail } from '@/api/types'
import { DataCard, EmptyState, TableSkeleton } from '@/components/app/DataTable'
import { PageHeader } from '@/components/app/PageHeader'
import { ErrorState } from '@/components/ErrorState'
import { Button } from '@/components/ui/button'
import { useQaTests, useRecordCalibration, useScanner } from './api'
import { CalibrationForm } from './CalibrationForm'
import { FLAGGED_TEXT, formatDate } from './qa'
import { ResultBadge, RiskScore } from './QaStatusBadge'

/** Biomedical engineer only (route access). Shows the server's saved result after submit. */
export function RecordCalibrationPage() {
  const { scannerId = '' } = useParams()
  const navigate = useNavigate()
  const scanner = useScanner(scannerId)
  const tests = useQaTests(true)
  const record = useRecordCalibration(scannerId)
  const [saved, setSaved] = useState<CalibrationDetail | null>(null)

  if (scanner.isPending || tests.isPending) return <TableSkeleton />
  if (scanner.isError) return <ErrorState error={scanner.error} onRetry={() => scanner.refetch()} />
  if (tests.isError) return <ErrorState error={tests.error} onRetry={() => tests.refetch()} />

  return (
    <>
      <PageHeader
        eyebrow="Scanners & QA"
        title="Record calibration"
        description={`${scanner.data.name} · interval ${scanner.data.calibrationIntervalDays} days`}
      />
      {saved ? (
        <DataCard title="Session saved">
          <div className="space-y-3 p-5 text-sm" role="status">
            <p className="flex items-center gap-2">
              Result (computed by the server): <ResultBadge result={saved.result} />
              {saved.result === 'FAIL' && <span className="text-destructive">{FLAGGED_TEXT}</span>}
            </p>
            <p>Next due: {formatDate(saved.nextDueDate)}</p>
            <p>
              <RiskScore value={saved.riskScore} withLabel />
            </p>
            <div className="flex gap-2 pt-2">
              <Button asChild>
                <Link to={`/scanners/${scannerId}`}>Back to scanner</Link>
              </Button>
              <Button variant="outline" onClick={() => setSaved(null)}>
                Record another
              </Button>
            </div>
          </div>
        </DataCard>
      ) : tests.data.length === 0 ? (
        <DataCard>
          <EmptyState text="There are no active QA tests. Add them on the QA tests page first." />
        </DataCard>
      ) : (
        <DataCard>
          <div className="p-5">
            <CalibrationForm
              tests={tests.data}
              onCancel={() => navigate(`/scanners/${scannerId}`)}
              onSubmit={async (input) => setSaved(await record.mutateAsync(input))}
            />
          </div>
        </DataCard>
      )}
    </>
  )
}
