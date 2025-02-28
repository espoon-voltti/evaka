import * as ApiTypes from 'lib-common/generated/api-types/incomestatement';
import { IncomeSource, IncomeStatementAttachmentType, OtherIncome } from 'lib-common/generated/api-types/incomestatement';
import { IncomeStatementAttachments } from 'lib-common/income-statements/attachments';
import LocalDate from 'lib-common/local-date';
export interface IncomeStatementForm {
    startDate: LocalDate | null;
    endDate: LocalDate | null;
    highestFee: boolean;
    childIncome: boolean;
    gross: Gross;
    entrepreneur: Entrepreneur;
    student: boolean;
    alimonyPayer: boolean;
    otherInfo: string;
    attachments: IncomeStatementAttachments;
    assure: boolean;
}
export interface Gross {
    selected: boolean;
    incomeSource: IncomeSource | null;
    estimatedMonthlyIncome: string;
    otherIncome: OtherIncome[];
    otherIncomeInfo: string;
}
export interface Entrepreneur {
    selected: boolean;
    fullTime: boolean | null;
    startOfEntrepreneurship: LocalDate | null;
    companyName: string;
    businessId: string;
    spouseWorksInCompany: boolean | null;
    startupGrant: boolean;
    checkupConsent: boolean;
    selfEmployed: SelfEmployed;
    limitedCompany: LimitedCompany;
    partnership: boolean;
    lightEntrepreneur: boolean;
    accountant: Accountant;
}
export interface SelfEmployed {
    selected: boolean;
    attachments: boolean;
    estimation: boolean;
    estimatedMonthlyIncome: string;
    incomeStartDate: LocalDate | null;
    incomeEndDate: LocalDate | null;
}
export interface LimitedCompany {
    selected: boolean;
    incomeSource: IncomeSource | null;
}
export interface Accountant {
    name: string;
    email: string;
    address: string;
    phone: string;
}
export declare const emptyIncomeStatementForm: IncomeStatementForm;
export declare function fromIncomeStatement(incomeStatement: ApiTypes.IncomeStatement): IncomeStatementForm;
export declare function computeRequiredAttachments(formData: IncomeStatementForm): Set<IncomeStatementAttachmentType>;
