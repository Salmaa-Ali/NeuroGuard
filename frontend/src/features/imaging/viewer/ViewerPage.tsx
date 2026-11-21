/**
 * Route: /studies/:studyUid/viewer
 *
 * Intentionally empty. Phase 3 (DICOM team): mount Cornerstone3D inside the container below.
 * Read the study with `useParams<{ studyUid: string }>()`.
 */
export function ViewerPage() {
  return <div id="viewer-root" data-testid="viewer-container" className="h-[calc(100vh-8rem)]" />
}
