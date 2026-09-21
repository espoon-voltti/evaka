// SPDX-FileCopyrightText: 2017-2026 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

// GENERATED FILE: no manual modifications

/**
* Generated from evaka.core.dataremoval.DataRetentionOrphan
*/
export interface DataRetentionOrphan {
  referenceColumn: string
  referencedTable: string
}

/**
* Generated from evaka.core.dataremoval.DataRetentionReferenceEdge
*/
export interface DataRetentionReferenceEdge {
  alsoNull: string[]
  kind: ReferenceKind
  referenceColumn: string
  referencedTable: string
  referencingTable: string
  role: Handler
}

/**
* Generated from evaka.core.dataremoval.DataRetentionSchemaGraph
*/
export interface DataRetentionSchemaGraph {
  references: DataRetentionReferenceEdge[]
  tables: DataRetentionTableNode[]
}

/**
* Generated from evaka.core.dataremoval.DataRetentionTableNode
*/
export interface DataRetentionTableNode {
  asyncJobColumns: string[]
  deletedWithParent: boolean
  deletionBundleRoot: string | null
  expirationRule: string | null
  external: boolean
  financeFreeze: string | null
  handledBy: Handler | null
  identifiedByCols: string[]
  independentRows: boolean
  name: string
  orphansToDelete: DataRetentionOrphan[]
  queuesAsyncJobs: boolean
  safeForIntegrations: Integration[]
}

/**
* Generated from evaka.core.dataremoval.Handler
*/
export type Handler =
  | 'CHILD'
  | 'ADULT'

/**
* Generated from evaka.core.dataremoval.Integration
*/
export type Integration =
  | 'KOSKI'
  | 'VARDA'

/**
* Generated from evaka.core.dataremoval.ReferenceKind
*/
export type ReferenceKind =
  | 'PRIMARY'
  | 'SECONDARY'
  | 'OPTIONAL'
