import { ACCESS } from '../../auth/access'
import type { Feature } from '../types'
import { DicomUploadPage } from './DicomUploadPage'
import { MriOrdersPage } from './MriOrdersPage'
import { StudiesPage } from './StudiesPage'
import { ViewerPage } from './viewer/ViewerPage'

export const imaging: Feature = {
  id: 'imaging',
  routes: [
    {
      path: '/mri-orders',
      element: <MriOrdersPage />,
      access: ACCESS.mriOrders,
      navLabel: 'MRI orders',
    },
    {
      path: '/studies',
      element: <StudiesPage />,
      access: ACCESS.studies,
      navLabel: 'MRI studies / Viewer',
    },
    // Phase 3: the DICOM team mounts Cornerstone3D inside ViewerPage.
    { path: '/studies/:studyUid/viewer', element: <ViewerPage />, access: ACCESS.studies },
    {
      path: '/dicom-upload',
      element: <DicomUploadPage />,
      access: ACCESS.dicomUpload,
      navLabel: 'DICOM upload',
    },
  ],
}
