// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { expect } from '../playwright'
import type { Page } from '../utils/page'

/**
 * The collapsible sections of the shared application editor, which the citizen
 * and employee editors both render.
 */
export class ApplicationEditorSections {
  constructor(private readonly page: Page) {}

  section = (name: string) => this.page.findByDataQa(`${name}-section`)

  header = (name: string) => this.page.findByDataQa(`${name}-section-header`)

  async open(name: string) {
    // The section renders before its open/closed state is known, so wait for
    // the attribute to exist before reading it.
    await expect(this.section(name)).toHaveAttribute('data-status', /.*/)
    const status = await this.section(name).getAttribute('data-status')
    if (status !== 'open') {
      await this.header(name).click()
    }
  }
}
