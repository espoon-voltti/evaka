// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import styled from 'styled-components'

import AsyncButton from './LegacyAsyncButton'

/**
 * @deprecated use AsyncButton and appearance="inline" instead
 */
export default styled(AsyncButton)`
  padding: 0;
  min-width: 0;
  min-height: 0;
  border: none;
  background-color: transparent;
`
