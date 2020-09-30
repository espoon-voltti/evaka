// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import React from 'react'
import { storiesOf } from '@storybook/react'
import { action } from '@storybook/addon-actions'
import LocalDate from '@evaka/lib-common/src/local-date'
import Toolbar from 'components/shared/molecules/Toolbar'

storiesOf('evaka/molecules/Toolbar', module)
  .add('Basic', () => (
    <Toolbar
      dateRange={{
        startDate: LocalDate.of(2000, 1, 1),
        endDate: LocalDate.of(2040, 1, 1)
      }}
      onEdit={action('edit')}
      onDelete={action('delete')}
    />
  ))
  .add('Warning', () => (
    <Toolbar
      dateRange={{
        startDate: LocalDate.of(2040, 1, 1),
        endDate: LocalDate.of(2050, 1, 1)
      }}
      onEdit={action('edit')}
      onDelete={action('delete')}
      warning={{
        text: 'Tässä on joku ongelma',
        tooltipId: 'warning-tooltip-1'
      }}
    />
  ))
