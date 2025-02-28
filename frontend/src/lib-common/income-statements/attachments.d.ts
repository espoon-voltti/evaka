import { Attachment } from '../generated/api-types/attachment';
import { IncomeStatementAttachment, IncomeStatementAttachmentType } from '../generated/api-types/incomestatement';
import { AttachmentId } from '../generated/api-types/shared';
export type AttachmentsByType = Partial<Record<IncomeStatementAttachmentType, Attachment[]>>;
export type IncomeStatementAttachments = {
    typed: true;
    attachmentsByType: AttachmentsByType;
} | {
    typed: false;
    untypedAttachments: Attachment[];
};
/**
 * Income statements can contain typed an untyped attachments. New code always adds a type to attachments,
 * but income statements created before the introduction of attachment types have untyped attachments.
 * An income statement has only typed or only untyped attachments.
 */
export declare function toIncomeStatementAttachments(attachments: IncomeStatementAttachment[]): IncomeStatementAttachments;
export declare function numAttachments(incomeStatementAttachments: IncomeStatementAttachments): number;
export declare function collectAttachmentIds(incomeStatementAttachments: IncomeStatementAttachments): AttachmentId[];
