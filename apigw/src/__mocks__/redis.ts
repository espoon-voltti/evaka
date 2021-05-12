// SPDX-FileCopyrightText: 2017-2021 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// Automatic mock for Jest that replaces all imports of redis with redis-mock.
// See: https://jestjs.io/docs/manual-mocks#mocking-node-modules for details

import redis from 'redis-mock'

export const createClient = redis.createClient
export default redis
