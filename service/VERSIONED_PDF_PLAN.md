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

## Phase 3: API & Controller Changes ✅ COMPLETED

**Summary**:
- Added `DocumentVersion` data class to represent individual PDF versions
- Updated `DocumentMetadata` to include `versions` list (backward compatible with default empty list)
- Modified metadata query to fetch all versions from `child_document_pdf_version` table
- Employee download endpoint already supports optional `version` parameter (ADMIN only for specific versions)
- Citizen download endpoint always returns latest version (no version parameter)

## Phase 4: Access Control

**No changes required** - use existing permissions:
- `Action.ChildDocument.READ_METADATA` - Required to see version list in metadata endpoint
- `Action.ChildDocument.DOWNLOAD` - Required to download any version (employee)
- `Action.Citizen.ChildDocument.DOWNLOAD` - Required to download latest version (citizen)

Citizens can only download the latest version (no version parameter available in citizen endpoint).

## Implementation Summary

### Completed Changes (Phases 1-3):
- Database schema with `child_document_pdf_version` table
- PDF generation now creates versioned files instead of replacing
- Metadata endpoint returns list of all versions
- Download endpoints support version parameter (admin only for specific versions)
- Backward compatibility maintained throughout

### Files Modified:
- Migration: `V576__child_document_versioned_pdfs.sql`
- `DocumentKey.kt`, `ChildDocumentQueries.kt`, `ChildDocumentService.kt`
- `ProcessMetadataController.kt`, `ProcessMetadataQueries.kt`
- Access control already in place (no changes needed)

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

