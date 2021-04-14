// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Selector } from 'testcafe'

export default class MobileGroupsPage {
  readonly allGroups = Selector('[data-qa="btn-group-all"]')

  readonly groupButton = (groupId: string) =>
    Selector(`[data-qa="btn-group-${groupId}"]`)

  readonly childRow = (childId: string) =>
    Selector(`[data-qa="child-${childId}"]`)

  readonly childName = (childId: string) =>
    this.childRow(childId).find('[data-qa="child-name"]')

  readonly childStatus = (childId: string) =>
    this.childRow(childId).find('[data-qa="child-status"]')

  readonly childDailyNoteLink = (childId: string) =>
    this.childRow(childId).find('[data-qa="link-child-daycare-daily-note"]')

  readonly childDailyNoteLink2 = Selector(
    '[data-qa="link-child-daycare-daily-note"]'
  )

  readonly childSensitiveInfoLink = Selector(
    '[data-qa="link-child-sensitive-info"]'
  )

  readonly pinLoginStaffSelector = Selector('[data-qa="select-staff"]')
  readonly pinInput = Selector('[data-qa="set-pin"]')
  readonly submitPin = Selector('[data-qa="submit-pin"]')

  readonly comingTab = Selector('[data-qa="coming-tab"]')
  readonly presentTab = Selector('[data-qa="present-tab"]')
  readonly departedTab = Selector('[data-qa="departed-tab"]')
  readonly absentTab = Selector('[data-qa="absent-tab"]')

  readonly noChildrenIndicator = Selector('[data-qa="no-children-indicator"]')

  readonly childInfoName = Selector('[data-qa="child-info-name"]')
  readonly childInfoPreferredName = Selector(
    '[data-qa="child-info-preferred-name"]'
  )
  readonly childInfoChildAddress = Selector(
    '[data-qa="child-info-child-address"]'
  )
  readonly childInfoAllergies = Selector('[data-qa="child-info-allergies"]')
  readonly childInfoDiet = Selector('[data-qa="child-info-diet"]')
  readonly childInfoMedication = Selector('[data-qa="child-info-medication"]')
  readonly childInfoContact1Name = Selector(
    '[data-qa="child-info-contact1-name"]'
  )
  readonly childInfoContact1Phone = Selector(
    '[data-qa="child-info-contact1-phone"]'
  )
  readonly childInfoContact1BackupPhone = Selector(
    '[data-qa="child-info-contact1-backup-phone"]'
  )
  readonly childInfoContact1Email = Selector(
    '[data-qa="child-info-contact1-email"]'
  )
  readonly childInfoContact2Name = Selector(
    '[data-qa="child-info-contact2-name"]'
  )
  readonly childInfoContact2Phone = Selector(
    '[data-qa="child-info-contact2-phone"]'
  )
  readonly childInfoContact2BackupPhone = Selector(
    '[data-qa="child-info-contact2-backup-phone"]'
  )
  readonly childInfoContact2Email = Selector(
    '[data-qa="child-info-contact2-email"]'
  )

  readonly childInfoBackupPickup1Name = Selector(
    '[data-qa="child-info-backup-pickup1-name"]'
  )
  readonly childInfoBackupPickup1Phone = Selector(
    '[data-qa="child-info-backup-pickup1-phone"]'
  )

  readonly childInfoBackupPickup2Name = Selector(
    '[data-qa="child-info-backup-pickup2-name"]'
  )
  readonly childInfoBackupPickup2Phone = Selector(
    '[data-qa="child-info-backup-pickup2-phone"]'
  )
}
