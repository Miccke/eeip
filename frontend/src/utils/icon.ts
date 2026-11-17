import type { Component } from 'vue'
import {
  DashboardOutlined, AppstoreOutlined, ApiOutlined, PartitionOutlined,
  LineChartOutlined, ThunderboltOutlined, InboxOutlined, SafetyCertificateOutlined,
  SettingOutlined, TeamOutlined, KeyOutlined, UnorderedListOutlined
} from '@ant-design/icons-vue'

const MAP: Record<string, Component> = {
  dashboard: DashboardOutlined,
  layers: AppstoreOutlined,
  api: ApiOutlined,
  route: PartitionOutlined,
  activity: LineChartOutlined,
  monitor: ThunderboltOutlined,
  inbox: InboxOutlined,
  shield: SafetyCertificateOutlined,
  settings: SettingOutlined,
  users: TeamOutlined,
  key: KeyOutlined,
  list: UnorderedListOutlined
}

export function iconOf(name?: string | null): Component {
  return (name && MAP[name]) || UnorderedListOutlined
}
