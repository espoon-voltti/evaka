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

## Phase 2: Service Layer Changes ✅ COMPLETED

## Phase 3: API & Controller Changes ✅ COMPLETED

## Phase 4: Architectural Refactoring - Move Publishing Data to PDF Version Table ✅ COMPLETED

**Implementation completed:**
- Migration updated to rename table to `child_document_published_version`
- Added `created_by`, `published_content`, `updated_at` columns
- Migrated existing published documents from `child_document` table
- Dropped old publishing columns (`published_at`, `published_by`, `published_content`)
- Updated `AsyncJob.CreateChildDocumentPdf` to include `versionNumber` and `user`
- Renamed query functions to use "Published" instead of "Pdf" in names
- Created helper function: `createPublishedVersionIfNeeded()`, `deleteChildDocumentReadMarkers()`
- Updated `updateChildDocumentStatus()` to replace `changeStatus()` and `changeStatusAndPublish()`
- Refactored `markCompletedAndPublish()` to filter changed content in SQL and return version map
- Updated service layer: `schedulePdfGeneration()` accepts version map, `createAndUploadPdf()` validates version and deletes read markers when PDF ready
- Updated controller endpoints: `publishDocument()`, `nextDocumentStatus()`, `acceptChildDocumentDecision()` use new publishing logic
- Updated metadata API to include `created_by` in `DocumentVersion`
- Updated all queries to read from `child_document_published_version` table

### Goal
Move publishing-related data (`published_at`, `published_by`, `published_content`) from `child_document` to `child_document_published_version`. A document is considered "published" if it has at least one completed version row in the version table.

### Key Architectural Decisions

**1. Publishing creates version row (not PDF generation)**
- Publishing functions insert version row with NULL `document_key`
- Async PDF generation updates the `document_key` once completed
- This decouples logical "publishing" from async PDF generation

**2. Only publish if content changed**
- Check if published content matches current content before creating version row
- If content unchanged, do nothing (no version row, no PDF generation)
- Avoids duplicate versions when user republishes without changes

**3. Document key nullable**
- `child_document_published_version.document_key` is NULLABLE
- NULL = PDF generation in progress or aborted
- NOT NULL = PDF ready for download

**4. Published status from latest version (regardless of PDF completion)**
- Document is published if: `EXISTS (SELECT ... FROM child_document_published_version ...)`
- `ChildDocumentSummary.publishedAt/publishedBy` fetched from latest version (even if `document_key` is NULL)
- Shows when document was last published, even if PDF generation is still pending
- PDF download will fail/wait until `document_key` is NOT NULL

**5. Race condition handling**
- Store `versionNumber` in `AsyncJob.CreateChildDocumentPdf` (nullable for backward compatibility)
- Existing scheduled jobs (before Phase 4a deployment) will have NULL version - treat as version 1
- Before generating PDF, check if this version is still latest
- If superseded by newer version, abort (leave `document_key` NULL)
- Rationale: Cannot recreate exact content state from that publish moment

**6. Read marker deletion timing**
- Read markers (`child_document_read`) deleted when PDF generation completes successfully
- NOT deleted at publish time (when version row created)
- Rationale: Read markers signal users that new content is available to download - only delete when PDF is actually ready

### Implementation Strategy

**Big-bang migration:**
- Move publishing data (`published_at`, `published_by`, `published_content`) from `child_document` to `child_document_published_version` in single migration
- Drop old columns immediately
- Update all code to read/write from new location
- Accept temporary issues during deployment when old service instances reference dropped columns

### Phase 4 Implementation

#### Database Migration

**Update `V576__child_document_versioned_pdfs.sql`** (not yet deployed, can be modified):

```sql
CREATE TABLE child_document_published_version (
    id uuid PRIMARY KEY DEFAULT ext.uuid_generate_v1mc(),
    child_document_id uuid NOT NULL REFERENCES child_document(id),
    document_key text,  -- Nullable: NULL = PDF generation pending/aborted, NOT NULL = ready
    created_at timestamp with time zone NOT NULL,
    updated_at timestamp with time zone NOT NULL DEFAULT now(),
    version_number integer NOT NULL,
    created_by uuid NOT NULL REFERENCES evaka_user(id),
    published_content jsonb NOT NULL,
    CONSTRAINT unique_version_per_document UNIQUE(child_document_id, version_number)
);

CREATE INDEX fk$child_document_published_version_document_id
    ON child_document_published_version(child_document_id);

CREATE INDEX fk$child_document_published_version_created_by
    ON child_document_published_version(created_by);

CREATE TRIGGER set_timestamp BEFORE UPDATE ON child_document_published_version 
    FOR EACH ROW EXECUTE PROCEDURE trigger_refresh_updated_at();

-- Migrate existing published documents to version table
INSERT INTO child_document_published_version (child_document_id, document_key, created_at, version_number, created_by, published_content)
SELECT
    cd.id AS child_document_id,
    cd.document_key,  -- May be NULL if PDF generation still pending
    cd.published_at AS created_at,
    1 AS version_number,
    cd.published_by AS created_by,
    cd.published_content
FROM child_document cd
WHERE cd.published_at IS NOT NULL;  -- published_consistency constraint ensures published_by and published_content are also NOT NULL

-- Drop old publishing columns
ALTER TABLE child_document
    DROP CONSTRAINT published_consistency,
    DROP COLUMN published_at,
    DROP COLUMN published_by,
    DROP COLUMN published_content,
    DROP COLUMN document_key;
```

**Changes to V576:**
- Remove `ON DELETE CASCADE` - version rows must be deleted asynchronously via `AsyncJob.DeleteChildDocumentPdf`
- Add `updated_at` column (timestamp with time zone, NOT NULL, DEFAULT now())
- Add trigger `set_timestamp` to automatically update `updated_at` on row updates
- Add `created_by` column (references evaka_user, NOT NULL)
- Add `published_content` column (jsonb, NOT NULL)
- Add index on `created_by`
- Update INSERT to migrate all published documents (WHERE published_at IS NOT NULL)
- Rely on `published_consistency` constraint to ensure published_by and published_content are also NOT NULL
- Documents with NULL document_key will have PDFs generated when existing async jobs are processed (versionNumber defaults to 1)
- **Drop document_key from child_document table** - now stored in child_document_published_version only

#### API Changes

**File: `ProcessMetadataController.kt`**

Update `DocumentVersion` data class:
```kotlin
data class DocumentVersion(
    val versionNumber: Int,
    val createdAt: HelsinkiDateTime,
    @Nested("created_by") val createdBy: EvakaUser,  // NOT NULL - created_by column is NOT NULL
    val downloadPath: String
)
```

**File: `ProcessMetadataQueries.kt`**

Update versions subquery in `getChildDocumentMetadata()`:
```kotlin
(
    SELECT coalesce(jsonb_agg(
        jsonb_build_object(
            'versionNumber', v.version_number,
            'createdAt', v.created_at,
            'createdById', vu.id,
            'createdByName', vu.name,
            'createdByType', vu.type,
            'downloadPath', '/employee/child-documents/' || v.child_document_id || '/pdf?version=' || v.version_number
        ) ORDER BY v.version_number DESC
    ), '[]'::jsonb)
    FROM child_document_published_version v
    JOIN evaka_user vu ON v.created_by = vu.id  -- JOIN (not LEFT JOIN) - created_by is NOT NULL
    WHERE v.child_document_id = cd.id
) AS versions
```

#### AsyncJob Changes

**File: `AsyncJob.kt`**
```kotlin
data class CreateChildDocumentPdf(
    val documentId: ChildDocumentId,
    val versionNumber: Int? = null,  // Nullable for backward compatibility with existing jobs
    override val user: AuthenticatedUser? = null
) : AsyncJob
```

#### Service Layer Changes

**File: `ChildDocumentQueries.kt`**

New function for publishing:
```kotlin
fun Database.Transaction.insertChildDocumentPublishedVersion(
    documentId: ChildDocumentId,
    createdAt: HelsinkiDateTime,
    createdBy: EvakaUserId,
    publishedContent: DocumentContent
): Int {
    return createQuery {
        sql("""
            INSERT INTO child_document_published_version
                (child_document_id, version_number, created_at, created_by, published_content, document_key)
            VALUES (
                ${bind(documentId)},
                COALESCE(
                    (SELECT MAX(version_number) + 1 
                     FROM child_document_published_version
                     WHERE child_document_id = ${bind(documentId)}), 
                    1
                ),
                ${bind(createdAt)},
                ${bind(createdBy)},
                ${bind(publishedContent)}::jsonb,
                NULL
            )
            RETURNING version_number
        """)
    }.exactlyOne()
}
```

New function to delete read markers (called when PDF generation completes successfully):
```kotlin
fun Database.Transaction.deleteChildDocumentReadMarkers(documentId: ChildDocumentId) {
    createUpdate {
        sql("DELETE FROM child_document_read WHERE document_id = ${bind(documentId)}")
    }.execute()
}
```

**Remove redundant wrappers:**
- Delete `publishChildDocument()` - callers use `createPublishedVersionIfNeeded()` directly
- Delete `changeStatusAndPublish()` - callers do status update + publishing separately

Keep `updateChildDocumentStatus()` simple (only status updates, no publishing):
```kotlin
fun Database.Transaction.updateChildDocumentStatus(
    id: ChildDocumentId,
    statusTransition: StatusTransition,
    now: HelsinkiDateTime,
    answeredBy: EvakaUserId? = null,
    userId: EvakaUserId,
) {
    createUpdate {
        sql("""
            UPDATE child_document
            SET status = ${bind(statusTransition.newStatus)},
                modified_at = ${bind(now)},
                modified_by = ${bind(userId)}
                ${if (answeredBy != null) ", answered_at = ${bind(now)}, answered_by = ${bind(answeredBy)}" else ""}
            WHERE id = ${bind(id)} AND status = ${bind(statusTransition.currentStatus)}
        """)
    }.updateExactlyOne()
}
```

Update `markCompletedAndPublish()` for batch operations:
```kotlin
fun Database.Transaction.markCompletedAndPublish(
    ids: List<ChildDocumentId>,
    now: HelsinkiDateTime,
): Map<ChildDocumentId, Int> {  // Returns map of documentId -> versionNumber
    val userId = AuthenticatedUser.SystemInternalUser.evakaUserId
    
    // Always update status for all documents
    createUpdate {
        sql("""
            UPDATE child_document
            SET status = 'COMPLETED',
                modified_at = ${bind(now)},
                modified_by = ${bind(userId)}
            WHERE id = ANY(${bind(ids)})
        """)
    }.execute()
    
    // Batch insert into version table using CTE, filtering for changed content in SQL
    return createQuery {
        sql("""
            WITH documents_to_publish AS (
                SELECT cd.id, cd.content
                FROM child_document cd
                LEFT JOIN LATERAL (
                    SELECT v.published_content
                    FROM child_document_published_version v
                    WHERE v.child_document_id = cd.id
                    ORDER BY v.version_number DESC
                    LIMIT 1
                ) latest_version ON true
                WHERE cd.id = ANY(${bind(ids)})
                  AND (latest_version.published_content IS NULL OR cd.content != latest_version.published_content)
            ),
            next_versions AS (
                SELECT 
                    d.id,
                    d.content,
                    COALESCE(MAX(v.version_number) + 1, 1) as next_version
                FROM documents_to_publish d
                LEFT JOIN child_document_published_version v ON v.child_document_id = d.id
                GROUP BY d.id, d.content
            ),
            inserted AS (
                INSERT INTO child_document_published_version
                    (child_document_id, version_number, created_at, created_by, published_content, document_key)
                SELECT 
                    id,
                    next_version,
                    ${bind(now)},
                    ${bind(userId)},
                    content::jsonb,
                    NULL
                FROM next_versions
                RETURNING child_document_id, version_number
            )
            SELECT child_document_id, version_number FROM inserted
        """)
    }.toMap { 
        column<ChildDocumentId>("child_document_id") to column<Int>("version_number") 
    }
}
```

Helper functions:
```kotlin
/** 
 * Creates a new published version if content has changed. Returns version number or null if skipped.
 */
fun Database.Transaction.createPublishedVersionIfNeeded(
    id: ChildDocumentId,
    now: HelsinkiDateTime,
    userId: EvakaUserId,
): Int? {
    data class Result(val content: DocumentContent, val upToDate: Boolean)
    
    val result = createQuery {
        sql(
            """
            SELECT 
                cd.content,
                (lv.published_content IS NOT NULL AND cd.content = lv.published_content) AS up_to_date
            FROM child_document cd
            LEFT JOIN LATERAL (
                SELECT v.published_content
                FROM child_document_published_version v
                WHERE v.child_document_id = cd.id
                ORDER BY v.version_number DESC
                LIMIT 1
            ) lv ON true
            WHERE cd.id = ${bind(id)}
            """
        )
    }.exactlyOneOrNull<Result>() ?: throw NotFound("Document $id not found")
    
    if (result.upToDate) return null
    
    return insertChildDocumentPublishedVersion(id, now, userId, result.content)
}
```

**File: `ChildDocumentService.kt`**

Update `schedulePdfGeneration()`:
```kotlin
fun schedulePdfGeneration(
    tx: Database.Transaction,
    user: AuthenticatedUser,
    documentVersions: Map<ChildDocumentId, Int>,  // documentId -> versionNumber
    now: HelsinkiDateTime,
) {
    logger.info { "Scheduling generation of ${documentVersions.size} child document pdfs" }
    
    asyncJobRunner.plan(
        tx,
        payloads = documentVersions.map { (documentId, versionNumber) ->
            AsyncJob.CreateChildDocumentPdf(documentId, versionNumber, user)
        },
        runAt = now,
        retryCount = 10,
    )
}
```

Update `createAndUploadPdf()` with version validation:
```kotlin
fun createAndUploadPdf(
    db: Database.Connection,
    clock: EvakaClock,
    msg: AsyncJob.CreateChildDocumentPdf,
) {
    val documentId = msg.documentId
    val requestedVersion = msg.versionNumber ?: 1  // Default to v1 for backward compatibility
    
    val document = db.read { tx -> tx.getChildDocument(documentId) }
        ?: throw NotFound("document $documentId not found")
    
    // Check if this version is still the latest
    val latestVersion = db.read { tx ->
        tx.createQuery {
            sql("""
                SELECT MAX(version_number)
                FROM child_document_published_version
                WHERE child_document_id = ${bind(documentId)}
            """)
        }.exactlyOneOrNull<Int>()
    }
    
    if (latestVersion != null && requestedVersion < latestVersion) {
        logger.warn {
            "Aborting PDF generation for document $documentId version $requestedVersion " +
            "(superseded by version $latestVersion). Leaving document_key NULL."
        }
        return  // Abort - this version is stale
    }
    
    val html = generateChildDocumentHtml(document)
    val pdfBytes = pdfGenerator.render(html)

    db.transaction { tx ->
        val versionedKey = DocumentKey.ChildDocument(documentId, requestedVersion)
        val location = documentClient.upload(versionedKey, pdfBytes, "application/pdf")

        tx.updateChildDocumentPublishedVersionKey(documentId, requestedVersion, location.key)
        
        // Delete read markers now that PDF is ready to download
        tx.deleteChildDocumentReadMarkers(documentId)

        if (document.decision != null) {
            // ...existing SFI message logic...
        }
    }
}
```

Update controller call sites:
```kotlin
// In ChildDocumentController.kt

@PutMapping("/{documentId}/publish")
fun publishDocument(...): ResponseEntity<Unit> {
    return db.connect { dbc ->
        dbc.transaction { tx ->
            // ...access control...

            val versionNumber = tx.createPublishedVersionIfNeeded(documentId, clock.now(), user.evakaUserId)
            
            if (versionNumber != null) {
                // Content changed, schedule PDF generation
                childDocumentService.schedulePdfGeneration(
                    tx,
                    user,
                    mapOf(documentId to versionNumber),
                    clock.now()
                )
                childDocumentService.scheduleEmailNotification(tx, listOf(documentId), clock.now())
            }
            // else: content unchanged, nothing to do
        }
    }
    // ...audit...
}

@PutMapping("/{documentId}/next-status")
fun nextDocumentStatus(...): ResponseEntity<Unit> {
    return db.connect { dbc ->
        dbc.transaction { tx ->
            // ...access control...
            
            val document = tx.getChildDocument(documentId) ?: throw NotFound()
            val statusTransition = validateStatusTransition(document, newStatus, goingForward = true)
            
            // For decisions: just change status (publishing happens later after accept/reject)
            if (document.template.type.decision) {
                tx.updateChildDocumentStatus(documentId, statusTransition, clock.now(), userId = user.evakaUserId)
            } else {
                // For non-decisions: change status + publish if content changed
                val versionNumber = tx.createPublishedVersionIfNeeded(documentId, clock.now(), user.evakaUserId)
                
                val answeredBy = user.evakaUserId.takeIf {
                    document.template.type == ChildDocumentType.CITIZEN_BASIC &&
                    statusTransition.newStatus == DocumentStatus.COMPLETED
                }
                tx.updateChildDocumentStatus(documentId, statusTransition, clock.now(), answeredBy, user.evakaUserId)
                
                if (versionNumber != null) {
                    childDocumentService.schedulePdfGeneration(
                        tx,
                        user,
                        mapOf(documentId to versionNumber),
                        clock.now()
                    )
                }
                
                if (statusTransition.newStatus == DocumentStatus.CITIZEN_DRAFT || versionNumber != null) {
                    childDocumentService.scheduleEmailNotification(tx, listOf(documentId), clock.now())
                }
            }
            
            updateDocumentCaseProcessHistory(tx, document, statusTransition.newStatus, clock.now(), user.evakaUserId)
        }
    }
    // ...audit...
}
```

**File: `ChildDocumentService.kt` - automated process**

Update `completeAndPublishChildDocumentsAtEndOfTerm()`:
```kotlin
fun completeAndPublishChildDocumentsAtEndOfTerm(
    tx: Database.Transaction,
    now: HelsinkiDateTime,
) {
    val documentIds = // ...existing query...

    if (documentIds.isNotEmpty()) {
        val versionMap = tx.markCompletedAndPublish(documentIds, now)
        
        if (versionMap.isNotEmpty()) {
            schedulePdfGeneration(tx, AuthenticatedUser.SystemInternalUser, versionMap, now)
            scheduleEmailNotification(tx, versionMap.keys.toList(), now)
        }

        documentIds.forEach { documentId ->
            autoCompleteDocumentCaseProcessHistory(tx = tx, documentId = documentId, now = now)
        }
    }
}
```


### Testing Plan

Based on analysis of existing integration tests, the following tests are needed to cover the NEW functionality introduced by the versioned PDF feature:

#### 1. Version Creation Logic Tests
Tests for the new `createPublishedVersionIfNeeded()` logic:
- **Publishing unchanged content skips version creation** - Verify that calling publish on unchanged content returns null and creates no version row
- **Publishing changed content creates new version** - Verify that publishing with changed content creates version row with incremental version number
- **Multiple publishes create incremental versions** - Test version 1, 2, 3 sequence with different content

#### 2. PDF Generation with Version Validation Tests
Tests for the updated `createAndUploadPdf()` with version checking:
- **PDF generation succeeds for latest version** - Verify PDF is generated and document_key updated when version is still latest
- **Backward compatibility with existing jobs** - Test that AsyncJob.CreateChildDocumentPdf with versionNumber=null defaults to version 1
- **Versioned S3 keys are created** - Verify that PDFs are stored with format like `child-documents/child_document_{documentId}_v{version}.pdf`

#### 3. Data Integrity Tests
Tests for the database migration and architectural changes:
- **Published status derived from version table** - Test that document.publishedAt/publishedBy comes from latest version row, not original table
- **Null document_key handling** - Test that documents with version rows but NULL document_key show as published but PDF download fails gracefully

#### 4. Metadata API Version Support Tests  
Tests for the new multi-version metadata endpoint:
- **Version list in metadata response** - Verify getChildDocumentMetadata returns array of versions with createdAt, createdBy, versionNumber
- **Version-specific download paths** - Test that download URLs include version parameter: `/pdf?version=2`
- **Admin-only version access** - Verify that users without READ_METADATA permission can only access latest version
- **Empty versions array for unpublished documents** - Test metadata for documents with no published versions

#### 5. Read Marker Management with Versions Tests
Tests for the updated read marker behavior:
- **Read markers deleted when PDF generation completes** - Verify deleteChildDocumentReadMarkers() is called when document_key is set, not at publish time

#### 6. Batch Operations with Versioning Tests
Tests for the updated `markCompletedAndPublish()` function:
- **Batch end-of-term filtering by changed content** - Verify that only documents with changed content get new versions during automated completion, but all get status updated

#### 7. Controller Integration with New Publishing Logic Tests
Tests for updated controller endpoints using the new architecture:
- **publishDocument endpoint with content change detection** - Test that explicit publish button creates version only if content changed
- **nextDocumentStatus endpoint publishing behavior** - Test status transitions that trigger publishing use new version logic
- **Decision document workflows unchanged** - Verify decision documents still work correctly with new architecture (publishing happens on accept, not status change)

#### 8. Error Handling and Edge Cases Tests
- **Document deletion with existing versions** - Test that document deletion properly cleans up version table rows and also deletes the documents from S3 via the scheduled async jobs.

### Tests NOT Needed (Already Covered)
Based on existing test analysis, these areas are already well covered and don't need new tests:
- Basic status transitions (hojks, pedagogical, decision workflows) - `ChildDocumentControllerIntegrationTest` has comprehensive coverage
- Document creation, content updates, permissions - Already tested
- SFI message sending - Already tested  
- Email notifications - Already tested in `ChildDocumentServiceIntegrationTest`
- Case process history updates - Already tested
- Basic metadata functionality - Already tested

## Implementation Summary

- Database schema with `child_document_published_version` table
- Publishing data moved from `child_document` to version table
- Old columns (`published_at`, `published_by`, `published_content`) dropped in migration
- PDF generation now creates versioned files instead of replacing
- Metadata endpoint returns list of all versions
- Download endpoints support version parameter (admin only for specific versions)
