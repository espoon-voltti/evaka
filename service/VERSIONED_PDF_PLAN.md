# Child Document Versioned PDF Feature Plan

## Scope

**This implementation will focus on back-end changes only.** Frontend changes and infrastructure changes are out of scope for now.

## Current Behavior

Currently, when a child document is published or goes through certain state transitions:
- A background job is triggered to generate a PDF
- The PDF is uploaded to S3
- The path to the file is stored in the `document_key` column of the `child_document` table
- When this happens again, the PDF in S3 is **replaced** by the new version (overwriting the previous one)

## Desired New Functionality

### Core Requirement: Store Multiple Versioned PDFs

Instead of replacing the PDF, the system should:
- Store a new versioned PDF file each time a document is published or transitions state
- Create a new table to store multiple document keys per child document
- Track the order/version of each PDF

### Download Behavior

#### Default Behavior (Regular Users)
- When downloading a document, default to downloading the **latest version**

#### Admin User Behavior (Users with READ_METADATA Permission)
- Users who can access the `getChildDocumentMetadata` endpoint should be able to:
  - See the list of all versions with timestamps indicating when they were created
  - Download any specific version from the list

### Technical Changes Required

## Context Information

### Current Implementation
- **Database**: `child_document` table has a `document_key` column (text, nullable)
- **S3 Storage**: PDFs stored with key format `child-documents/child_document_{documentId}.pdf`
- **PDF Generation**: Triggered via `AsyncJob.CreateChildDocumentPdf` in `ChildDocumentService.createAndUploadPdf()`
  - Generates HTML → PDF via `pdfGenerator.render()`
  - Uploads to S3 using `DocumentKey.ChildDocument(documentId)` 
  - Calls `tx.updateChildDocumentKey(documentId, key.key)` to store the key
  - **This overwrites the previous key**, effectively replacing the old PDF
- **Download**: `getPdfResponse()` in `ChildDocumentService` retrieves the document using `getChildDocumentKey()`
- **Metadata**: `getChildDocumentMetadata()` in `ProcessMetadataQueries.kt` returns `DocumentMetadata` with a single `downloadPath: String?`

### Key Files
- `ChildDocumentService.kt` - PDF generation and upload logic
- `ChildDocumentQueries.kt` - Database queries including `updateChildDocumentKey()`, `getChildDocumentKey()`
- `ProcessMetadataController.kt` - Metadata endpoint
- `ProcessMetadataQueries.kt` - Metadata queries
- `DocumentKey.kt` - S3 key generation
- Migration: `V392__add_document_key_to_child_document.sql` added the column

## Phase 1: Database Schema ✅ COMPLETED

**Migration**: `src/main/resources/db/migration/V576__child_document_versioned_pdfs.sql`

**Summary**: 
- Created `child_document_pdf_version` table to store versioned PDFs
- Migrated existing document keys from `child_document` table as version 1
- Kept `child_document.document_key` column for backward compatibility

**Schema details**: See migration file for complete table definition, constraints, and indexes

## Phase 2: Service Layer Changes ✅ COMPLETED

**Summary**: 
- Modified PDF generation to create versioned PDFs instead of replacing existing ones
- Added query functions for managing and retrieving PDF versions
- Implemented version parameter support in download endpoints (admin only for specific versions)
- Maintained backward compatibility with existing code

**Implementation details**: See detailed summary documents for transactional safety improvements, access control implementation, and code refactoring details

## Phase 3: API & Controller Changes

### Data Classes

**File: `ProcessMetadataController.kt`**
Add new data class for document versions:

```kotlin
data class DocumentVersion(
    val versionNumber: Int,
    val createdAt: HelsinkiDateTime,
    val downloadPath: String
)

// Update existing DocumentMetadata to include versions
data class DocumentMetadata(
    val documentId: UUID,
    val name: String,
    val createdAtDate: LocalDate?,
    val createdAtTime: LocalTime?,
    @Nested("created_by") val createdBy: EvakaUser?,
    val confidential: Boolean?,
    @Nested("confidentiality") val confidentiality: DocumentConfidentiality?,
    val downloadPath: String?,  // Latest version (backward compat)
    val receivedBy: DocumentOrigin?,
    val sfiDeliveries: List<SfiDelivery>,
    val versions: List<DocumentVersion> = emptyList()  // All versions
)
```

### Metadata Query

**File: `ProcessMetadataQueries.kt`**
Update `getChildDocumentMetadata()`:

```kotlin
fun Database.Read.getChildDocumentMetadata(documentId: ChildDocumentId): DocumentMetadata =
    createQuery {
        sql("""
            SELECT 
                dt.id,
                dt.name,
                cd.created,
                e.id AS created_by_id,
                e.name AS created_by_name,
                e.type AS created_by_type,
                dt.confidential,
                dt.confidentiality_duration_years,
                dt.confidentiality_basis,
                cd.document_key,
                (
                    $sfiDeliverySelect
                    WHERE sm.document_id = cd.id
                ) AS sfi_deliveries,
                (
                    SELECT coalesce(jsonb_agg(
                        jsonb_build_object(
                            'versionNumber', version_number,
                            'createdAt', created_at,
                            'downloadPath', '/employee/child-documents/' || ${bind(documentId)} || '/pdf?version=' || version_number
                        ) ORDER BY version_number DESC
                    ), '[]'::jsonb)
                    FROM child_document_pdf_version
                    WHERE child_document_id = cd.id
                ) AS versions
            FROM child_document cd
            JOIN document_template dt ON dt.id = cd.template_id
            LEFT JOIN evaka_user e ON e.employee_id = cd.created_by
            WHERE cd.id = ${bind(documentId)}
        """)
    }
    .map {
        val createdAt = column<HelsinkiDateTime>("created")
        DocumentMetadata(
            documentId = column("id"),
            name = column("name"),
            createdAtDate = createdAt.toLocalDate(),
            createdAtTime = createdAt.toLocalTime(),
            createdBy = column<EvakaUserId?>("created_by_id")?.let {
                EvakaUser(
                    id = it,
                    name = column("created_by_name"),
                    type = column("created_by_type"),
                )
            },
            confidential = column("confidential"),
            confidentiality = if (column<Boolean>("confidential")) {
                DocumentConfidentiality(
                    durationYears = column("confidentiality_duration_years"),
                    basis = column("confidentiality_basis"),
                )
            } else null,
            downloadPath = "/employee/child-documents/$documentId/pdf",
            receivedBy = null,
            sfiDeliveries = jsonColumn("sfi_deliveries"),
            versions = jsonColumn("versions")
        )
    }
    .exactlyOne()
```

### Controller Changes

**File: `ChildDocumentController.kt`**
Add version parameter to download endpoint:

```kotlin
@GetMapping("/{documentId}/pdf")
fun downloadChildDocument(
    db: Database,
    user: AuthenticatedUser.Employee,
    clock: EvakaClock,
    @PathVariable documentId: ChildDocumentId,
    @RequestParam(required = false) version: Int?
): ResponseEntity<Any> {
    return db.connect { dbc ->
        dbc.read { tx ->
            accessControl.requirePermissionFor(
                tx,
                user,
                clock,
                Action.ChildDocument.DOWNLOAD,
                documentId
            )
            childDocumentService.getPdfResponse(tx, documentId, version)
        }
    }.also {
        Audit.ChildDocumentDownload.log(
            targetId = AuditId(documentId),
            meta = version?.let { mapOf("version" to it) } ?: emptyMap()
        )
    }
}
```

**File: `ChildDocumentControllerCitizen.kt`**
Keep citizen endpoint simple (no version parameter - always latest):

```kotlin
@GetMapping("/{documentId}/pdf")
fun downloadChildDocument(
    db: Database,
    user: AuthenticatedUser.Citizen,
    clock: EvakaClock,
    @PathVariable documentId: ChildDocumentId,
): ResponseEntity<Any> {
    return db.connect { dbc ->
        dbc.read { tx ->
            accessControl.requirePermissionFor(
                tx,
                user,
                clock,
                Action.Citizen.ChildDocument.DOWNLOAD,
                documentId
            )
            // Citizens always get latest version (no version parameter)
            childDocumentService.getPdfResponse(tx, documentId, null)
        }
    }.also {
        Audit.ChildDocumentDownloadCitizen.log(targetId = AuditId(documentId))
    }
}
```

## Phase 4: Access Control

**No changes required** - use existing permissions:
- `Action.ChildDocument.READ_METADATA` - Required to see version list in metadata endpoint
- `Action.ChildDocument.DOWNLOAD` - Required to download any version (employee)
- `Action.Citizen.ChildDocument.DOWNLOAD` - Required to download latest version (citizen)

Citizens can only download the latest version (no version parameter available in citizen endpoint).

## Implementation Summary

### Changes by File:
1. **Migration**: `V{next}__child_document_versioned_pdfs.sql` - Create table, migrate data
2. **DocumentKey.kt** - Add version parameter to `ChildDocument` constructor
3. **ChildDocumentQueries.kt** - Add 4 new query functions for versioned PDFs
4. **ChildDocumentService.kt** - Update `createAndUploadPdf()` and `getPdfResponse()`
5. **ProcessMetadataController.kt** - Add `DocumentVersion` data class, update `DocumentMetadata`
6. **ProcessMetadataQueries.kt** - Update `getChildDocumentMetadata()` to include versions
7. **ChildDocumentController.kt** - Add `version` query parameter to download endpoint
8. **ChildDocumentControllerCitizen.kt** - No changes (citizens always get latest)

### Backward Compatibility:
- Existing `downloadPath` field still works (points to latest version)
- `child_document.document_key` column remains (will be deprecated in future)
- Old PDFs in S3 remain unchanged (new versions use versioned naming)
- Default download behavior unchanged (no version param = latest)

### Testing Considerations:
- Test migration with existing documents
- Test PDF generation creates new versions
- Test version download with valid/invalid version numbers
- Test metadata endpoint shows all versions
- Test citizen endpoint only gets latest version
- Test access control for version downloads

## Future Considerations

### Migration Race Condition
**Issue**: When migration `V576__child_document_versioned_pdfs.sql` is executed, there may be background jobs (`AsyncJob.CreateChildDocumentPdf`) already scheduled or running. These jobs will update `child_document.document_key` after the migration's `INSERT` statement has completed, meaning those document keys won't be migrated to the new `child_document_pdf_version` table.

**Impact**: 
- Documents with PDFs generated during/after migration might not have entries in the versioned table
- The backward compatibility approach (keeping `child_document.document_key`) mitigates this for Phase 2
- When Phase 2 is implemented, the service layer should handle missing version 1 entries gracefully

**Mitigation Options**:
1. Run a follow-up data fix after Phase 2 deployment to catch any missing entries
2. Modify Phase 2 implementation to check if version 1 exists before creating version 2, and create it if missing
3. Accept that some documents may only have versions 2+ (older documents will have version 1)

**Recommended approach**: Option 2 - Make Phase 2 implementation defensive by checking for existing versions and backfilling if needed.

