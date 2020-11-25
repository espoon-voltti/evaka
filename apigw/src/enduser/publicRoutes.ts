// SPDX-FileCopyrightText: 2017-2020 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import { Router } from 'express'
import { createProxy } from '../shared/proxy-utils'
import { autocomplete, geocode } from './services/map'
import { getEnduserAreas } from '../shared/service-client'

const router = Router()
const proxy = createProxy()

router.get('/units', proxy)

router.get('/areas', (req, res, next) => {
  getEnduserAreas(req)
    .then((locations) => res.status(200).json(locations))
    .catch((error) => next(error))
})

router.get('/autocomplete', (req, res, next) => {
  const { address } = req.query
  autocomplete(address as string)
    .then((places) => {
      res.status(200).json(places)
    })
    .catch((error) => next(error))
})

router.get('/geocode', (req, res, next) => {
  const { id } = req.query
  geocode(id as string)
    .then((points) => {
      res.status(200).json(points)
    })
    .catch((error) => next(error))
})

export default router
