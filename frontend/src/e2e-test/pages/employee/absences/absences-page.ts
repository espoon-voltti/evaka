// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector, t } from 'testcafe'
import UnitPage, {
  daycareGroupElement,
  daycareGroupPlacementElement,
  missingPlacementElement
} from '../../../pages/employee/units/unit-page'
import GroupPlacementModal from '../../../pages/employee/units/group-placement-modal'
import { DaycarePlacement } from 'e2e-test-common/dev-api/types'
import { Checkbox } from '../../../utils/helpers'

export type AbsenceType =
  | 'OTHER_ABSENCE'
  | 'SICKLEAVE'
  | 'UNKNOWN_ABSENCE'
  | 'PLANNED_ABSENCE'
  | 'TEMPORARY_RELOCATION'
  | 'TEMPORARY_VISITOR'
  | 'PARENTLEAVE'
  | 'FORCE_MAJEURE'
  | 'PRESENCE'

const unitPage = new UnitPage()
const groupPlacementModal = new GroupPlacementModal()

export default class AbsencesPage {
  readonly serviceNeedElement = (root: Selector) => ({
    root,
    serviceNeedValue: root.find('[data-qa="service-need-value"]').innerText,
    buttonRemoveServiceNeed: root.find('[data-qa="btn-remove-service-need"]'),
    buttonConfirmServiceNeedRemoval: root.find('[data-qa="modal-okBtn"]')
  })

  readonly btnOpenAbsenceDiary: Selector = Selector(
    '[data-qa="open-absence-diary-button"]'
  )
  readonly btnAbsencesPageReturn: Selector = Selector(
    '[data-qa="absences-page-return-button"]'
  )
  readonly btnNextMonth: Selector = Selector(
    '[data-qa="period-picker-next-month"]'
  )
  readonly absencesTitle: Selector = Selector('[data-qa="absences-title"]')
  readonly absenceTableChildLink: Selector = Selector('.absence-child-link')
  readonly absenceCell: Selector = Selector('[data-qa="absence-cell"]')
  readonly btnAddAbsence: Selector = Selector('[data-qa="add-absences-button"]')
  readonly checkboxBillable = new Checkbox(
    Selector('[data-qa="absences-select-caretype-BILLABLE"]')
  )
  readonly btnSaveAbsence: Selector = Selector('[data-qa="modal-okBtn"]')
  readonly staffAttendanceInput: Selector = Selector(
    '[data-qa="staff-attendance-input"]'
  )

  absenceIndicatorRight(type: AbsenceType): Selector {
    return Selector(`.absence-cell-right-${type}`)
  }

  async addDaycareGroupPlacement() {
    const missingPlacement = missingPlacementElement(
      unitPage.missingPlacementRows.nth(0)
    )
    await missingPlacement.addToGroup()
    await t.expect(groupPlacementModal.root.visible).ok()
    await groupPlacementModal.submit()
  }

  async removeDaycareGroupPlacement() {
    const group = daycareGroupElement(unitPage.groups.nth(0))

    const groupPlacement = daycareGroupPlacementElement(
      group.groupPlacementRows.nth(0)
    )
    await groupPlacement.remove()
  }

  async tryToFindAnyChildWithinNext24Months(placement: DaycarePlacement) {
    if (new Date() < new Date(placement.endDate)) {
      for (let i = 0; i < 24; i++) {
        if (await this.absenceTableChildLink.exists) {
          break
        }
        await t.click(this.btnNextMonth)
      }
    }
  }

  async addBillableAbsence(type: AbsenceType) {
    await t.click(this.absenceCell)
    await t.expect(this.absenceCell.find('.absence-cell-selected').exists).ok()

    await t.click(this.btnAddAbsence)
    await t.click(Selector(`#${type}`, { timeout: 50 }))
    await t.click(
      Selector('[data-qa="absences-select-caretype-BILLABLE"]', {
        timeout: 200
      })
    )
    await t.expect(this.checkboxBillable.checked).eql(true)
    await t.click(this.btnSaveAbsence)
  }
}
