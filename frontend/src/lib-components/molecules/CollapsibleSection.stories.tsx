// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import { faChild, faInfo, faUsers } from 'lib-icons'
import { LoremParagraph } from '../story-utils'
import CollapsibleSection from './CollapsibleSection'

storiesOf('evaka/molecules/CollapsibleSection', module).add('default', () => (
  <div>
    <CollapsibleSection title="Lapsen tiedot" icon={faChild}>
      <LoremParagraph />
    </CollapsibleSection>

    <CollapsibleSection title="Perheen tiedot" icon={faUsers} startCollapsed>
      <LoremParagraph />
      <LoremParagraph />
    </CollapsibleSection>

    <CollapsibleSection title="Lisätiedot" icon={faInfo} startCollapsed>
      <LoremParagraph />
      <LoremParagraph />
    </CollapsibleSection>
  </div>
))
