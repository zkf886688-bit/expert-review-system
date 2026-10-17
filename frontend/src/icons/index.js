import {
  ArrowLeft,
  BarChart3,
  Bell,
  BookOpen,
  ChevronDown,
  CircleCheck,
  ClipboardPen,
  ClipboardList,
  Eye,
  FilePen,
  FileSpreadsheet,
  FileText,
  FolderOpen,
  Inbox,
  LayoutDashboard,
  LoaderCircle,
  Lock,
  LogOut,
  Menu,
  PanelLeftClose,
  PanelRightOpen,
  PieChart,
  Printer,
  Send,
  ScrollText,
  Settings,
  Shield,
  TriangleAlert,
  User,
  UserCog,
  Users,
  Layers
} from 'lucide-vue-next'

/** @type {Record<string, import('vue').Component>} */
export const iconMap = {
  'arrow-left': ArrowLeft,
  'bar-chart': BarChart3,
  bell: Bell,
  'book-open': BookOpen,
  'chevron-down': ChevronDown,
  'circle-check': CircleCheck,
  'clipboard-pen': ClipboardPen,
  'clipboard-list': ClipboardList,
  eye: Eye,
  'file-pen': FilePen,
  'file-spreadsheet': FileSpreadsheet,
  'file-text': FileText,
  'folder-open': FolderOpen,
  inbox: Inbox,
  'layout-dashboard': LayoutDashboard,
  loader: LoaderCircle,
  lock: Lock,
  'log-out': LogOut,
  menu: Menu,
  'panel-left-close': PanelLeftClose,
  'panel-right-open': PanelRightOpen,
  'pie-chart': PieChart,
  printer: Printer,
  send: Send,
  'scroll-text': ScrollText,
  settings: Settings,
  shield: Shield,
  'triangle-alert': TriangleAlert,
  user: User,
  'user-cog': UserCog,
  users: Users,
  layers: Layers
}

/** Element Plus icon names → Lucide keys */
export const iconAliases = {
  ArrowLeft: 'arrow-left',
  Bell: 'bell',
  CircleCheck: 'circle-check',
  DataAnalysis: 'layout-dashboard',
  Document: 'file-text',
  EditPen: 'clipboard-pen',
  Folder: 'folder-open',
  List: 'scroll-text',
  Loading: 'loader',
  Lock: 'lock',
  PieChart: 'pie-chart',
  Printer: 'printer',
  Setting: 'settings',
  User: 'user',
  UserFilled: 'user-cog',
  View: 'eye',
  Warning: 'triangle-alert'
}

export function resolveIconName(name) {
  if (!name) return null
  const key = String(name)
  if (iconMap[key]) return key
  if (iconAliases[key]) return iconAliases[key]
  const kebab = key.replace(/([a-z0-9])([A-Z])/g, '$1-$2').toLowerCase()
  return iconMap[kebab] ? kebab : null
}

export function getIconComponent(name) {
  const resolved = resolveIconName(name)
  return resolved ? iconMap[resolved] : null
}
