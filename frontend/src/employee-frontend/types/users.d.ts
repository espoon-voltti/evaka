import { UUID } from '../../lib-common/types'
import { AdRole } from './index'

interface DaycareRole {
  daycareId: string
  daycareName: string
  role: AdRole
}

export interface Employee {
  id: UUID
  firstName: string
  lastName: string
  globalRoles: AdRole[]
  daycareRoles: DaycareRole[]
}
