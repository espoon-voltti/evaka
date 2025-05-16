// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import type { TerminatablePlacementGroup } from 'lib-common/generated/api-types/placement'
import { evakaUserId, fromUuid } from 'lib-common/id-type'
import LocalDate from 'lib-common/local-date'

import { terminatedPlacementInfo } from './utils'

describe('Terminated placement info', () => {
  it('terminated daycare placement', () => {
    const data: TerminatablePlacementGroup = {
      type: 'DAYCARE',
      unitId: fromUuid('b53d80e0-319b-4d2b-950c-f5c3c9f834bc'),
      unitName: 'Alkuräjähdyksen eskari',
      startDate: LocalDate.of(2021, 1, 1),
      endDate: LocalDate.of(2021, 1, 31),
      terminatable: true,
      placements: [
        {
          id: fromUuid('d4e1cf34-72d5-4fad-b40c-5b1b4cb36883'),
          type: 'DAYCARE',
          childId: fromUuid('5a4f3ccc-5270-4d28-bd93-d355182b6768'),
          unitId: fromUuid('b53d80e0-319b-4d2b-950c-f5c3c9f834bc'),
          unitName: 'Alkuräjähdyksen eskari',
          startDate: LocalDate.of(2021, 1, 1),
          endDate: LocalDate.of(2021, 1, 31),
          terminatable: true,
          terminationRequestedDate: LocalDate.of(2021, 1, 20),
          terminatedBy: {
            id: evakaUserId(fromUuid('87a5c962-9b3d-11ea-bb37-0242ac130002')),
            name: 'Karhula Johannes Olavi Antero Tapio',
            type: 'CITIZEN'
          }
        }
      ],
      additionalPlacements: []
    }
    expect(terminatedPlacementInfo(data)).toEqual({
      type: { type: 'placement', placementType: 'DAYCARE' },
      unitId: 'b53d80e0-319b-4d2b-950c-f5c3c9f834bc',
      unitName: 'Alkuräjähdyksen eskari',
      lastDay: LocalDate.of(2021, 1, 31)
    })
  })

  it('terminated additional connected daycare placement', () => {
    const data: TerminatablePlacementGroup = {
      type: 'PRESCHOOL',
      unitId: fromUuid('b53d80e0-319b-4d2b-950c-f5c3c9f834bc'),
      unitName: 'Alkuräjähdyksen eskari',
      terminatable: true,
      startDate: LocalDate.of(2022, 1, 1),
      endDate: LocalDate.of(2022, 6, 1),
      placements: [
        {
          id: fromUuid('d4e1cf34-72d5-4fad-b40c-5b1b4cb36883'),
          type: 'PRESCHOOL_DAYCARE',
          childId: fromUuid('5a4f3ccc-5270-4d28-bd93-d355182b6768'),
          unitId: fromUuid('b53d80e0-319b-4d2b-950c-f5c3c9f834bc'),
          unitName: 'Alkuräjähdyksen eskari',
          startDate: LocalDate.of(2022, 1, 1),
          endDate: LocalDate.of(2022, 6, 1),
          terminatable: true,
          terminationRequestedDate: null,
          terminatedBy: null
        }
      ],
      additionalPlacements: [
        {
          id: fromUuid('08e9e908-03d2-48a2-9634-5468b2165945'),
          type: 'DAYCARE',
          childId: fromUuid('5a4f3ccc-5270-4d28-bd93-d355182b6768'),
          unitId: fromUuid('b53d80e0-319b-4d2b-950c-f5c3c9f834bc'),
          unitName: 'Alkuräjähdyksen eskari',
          startDate: LocalDate.of(2022, 2, 2),
          endDate: LocalDate.of(2022, 11, 1),
          terminatable: true,
          terminationRequestedDate: LocalDate.of(2022, 3, 1),
          terminatedBy: {
            id: evakaUserId(fromUuid('87a5c962-9b3d-11ea-bb37-0242ac130002')),
            name: 'Karhula Johannes Olavi Antero Tapio',
            type: 'CITIZEN'
          }
        }
      ]
    }
    expect(terminatedPlacementInfo(data)).toEqual({
      type: { type: 'connectedDaycare' },
      unitId: 'b53d80e0-319b-4d2b-950c-f5c3c9f834bc',
      unitName: 'Alkuräjähdyksen eskari',
      lastDay: LocalDate.of(2022, 11, 1)
    })
  })

  it('terminated connected daycare before preschool placement', () => {
    const data: TerminatablePlacementGroup = {
      type: 'PRESCHOOL',
      unitId: fromUuid('b53d80e0-319b-4d2b-950c-f5c3c9f834bc'),
      unitName: 'Alkuräjähdyksen eskari',
      startDate: LocalDate.of(2022, 1, 1),
      endDate: LocalDate.of(2022, 2, 28),
      terminatable: true,
      placements: [
        {
          id: fromUuid('8920e598-8b3a-11ed-bf51-4f02a3804b0b'),
          type: 'PRESCHOOL_DAYCARE',
          childId: fromUuid('5a4f3ccc-5270-4d28-bd93-d355182b6768'),
          unitId: fromUuid('b53d80e0-319b-4d2b-950c-f5c3c9f834bc'),
          unitName: 'Alkuräjähdyksen eskari',
          startDate: LocalDate.of(2022, 1, 1),
          endDate: LocalDate.of(2022, 2, 10),
          terminatable: true,
          terminationRequestedDate: LocalDate.of(2022, 2, 1),
          terminatedBy: {
            id: evakaUserId(fromUuid('c174821e-81d1-11ed-b390-3330163bf811')),
            name: 'Karhula Johannes Olavi Antero Tapio',
            type: 'CITIZEN'
          }
        },
        {
          id: fromUuid('94e520b0-8b3a-11ed-b202-df238eb02b71'),
          type: 'PRESCHOOL',
          childId: fromUuid('5a4f3ccc-5270-4d28-bd93-d355182b6768'),
          unitId: fromUuid('b53d80e0-319b-4d2b-950c-f5c3c9f834bc'),
          unitName: 'Alkuräjähdyksen eskari',
          startDate: LocalDate.of(2022, 2, 11),
          endDate: LocalDate.of(2022, 2, 28),
          terminatable: true,
          terminationRequestedDate: null,
          terminatedBy: null
        }
      ],
      additionalPlacements: []
    }
    expect(terminatedPlacementInfo(data)).toEqual({
      type: { type: 'connectedDaycare' },
      unitId: 'b53d80e0-319b-4d2b-950c-f5c3c9f834bc',
      unitName: 'Alkuräjähdyksen eskari',
      lastDay: LocalDate.of(2022, 2, 10)
    })
  })
})
