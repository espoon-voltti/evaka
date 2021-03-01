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
}
