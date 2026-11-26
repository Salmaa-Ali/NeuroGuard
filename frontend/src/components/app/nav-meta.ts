import {
  Activity,
  Banknote,
  CalendarDays,
  ClipboardList,
  FileText,
  FileUp,
  Gauge,
  LayoutDashboard,
  Pill,
  Receipt,
  ScanLine,
  ScrollText,
  Stethoscope,
  User,
  Users,
  UserCog,
  type LucideIcon,
} from 'lucide-react'
import type { NavItem } from '@/auth/access'

export const NAV_ICON: Record<string, LucideIcon> = {
  '/': LayoutDashboard,
  '/admin/staff': UserCog,
  '/audit-log': ScrollText,
  '/patients': Users,
  '/profile': User,
  '/appointments': CalendarDays,
  '/my/appointments': CalendarDays,
  '/mri-orders': ClipboardList,
  '/studies': ScanLine,
  '/dicom-upload': FileUp,
  '/clinical': Stethoscope,
  '/my/prescriptions': Pill,
  '/reports': FileText,
  '/my/reports': FileText,
  '/scanners': Gauge,
  '/invoices': Receipt,
  '/payments': Banknote,
  '/my/invoices': Receipt,
}

const GROUP_OF: Record<string, string> = {
  '/admin/staff': 'Administration',
  '/audit-log': 'Administration',
  '/patients': 'Care',
  '/appointments': 'Care',
  '/clinical': 'Care',
  '/profile': 'My care',
  '/my/appointments': 'My care',
  '/my/prescriptions': 'My care',
  '/my/reports': 'My care',
  '/my/invoices': 'My care',
  '/mri-orders': 'Imaging',
  '/studies': 'Imaging',
  '/dicom-upload': 'Imaging',
  '/reports': 'Imaging',
  '/scanners': 'Imaging',
  '/invoices': 'Billing',
  '/payments': 'Billing',
}
const ORDER = ['Administration', 'Care', 'My care', 'Imaging', 'Billing']

export function groupAreas(items: NavItem[]) {
  const map = new Map<string, NavItem[]>()
  for (const i of items) {
    const g = GROUP_OF[i.to] ?? 'Other'
    map.set(g, [...(map.get(g) ?? []), i])
  }
  return [...map.entries()].sort((a, b) => ORDER.indexOf(a[0]) - ORDER.indexOf(b[0]))
}

export const FallbackIcon = Activity
