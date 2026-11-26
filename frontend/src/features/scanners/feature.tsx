import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { QaTestsPage } from './QaTestsPage'
import { RecordCalibrationPage } from './RecordCalibrationPage'
import { ScannerDetailPage } from './ScannerDetailPage'
import { ScannersPage } from './ScannersPage'

/**
 * Readers (admin, neurologist, radiologist, MRI tech, biomedical engineer) may open the scanner pages;
 * the sidebar entry is only for the biomedical engineer and admin. Writing is biomedical engineer only.
 */
export const scanners: Feature = {
  id: 'scanners',
  routes: [
    {
      path: '/scanners',
      element: <ScannersPage />,
      access: ACCESS.scannersRead,
      navAccess: ACCESS.scanners,
      navLabel: 'Scanners & QA',
    },
    { path: '/scanners/qa-tests', element: <QaTestsPage />, access: ACCESS.scannersWrite },
    { path: '/scanners/:scannerId', element: <ScannerDetailPage />, access: ACCESS.scannersRead },
    {
      path: '/scanners/:scannerId/calibrations/new',
      element: <RecordCalibrationPage />,
      access: ACCESS.scannersWrite,
    },
  ],
}
