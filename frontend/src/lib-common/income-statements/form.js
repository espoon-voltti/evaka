// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { toIncomeStatementAttachments } from 'lib-common/income-statements/attachments';
export const emptyIncomeStatementForm = {
    startDate: null,
    endDate: null,
    highestFee: false,
    childIncome: false,
    gross: {
        selected: false,
        incomeSource: null,
        estimatedMonthlyIncome: '',
        otherIncome: [],
        otherIncomeInfo: ''
    },
    entrepreneur: {
        selected: false,
        fullTime: null,
        startOfEntrepreneurship: null,
        companyName: '',
        businessId: '',
        spouseWorksInCompany: null,
        startupGrant: false,
        checkupConsent: false,
        selfEmployed: {
            selected: false,
            attachments: false,
            estimation: false,
            estimatedMonthlyIncome: '',
            incomeStartDate: null,
            incomeEndDate: null
        },
        limitedCompany: {
            selected: false,
            incomeSource: null
        },
        partnership: false,
        lightEntrepreneur: false,
        accountant: {
            name: '',
            email: '',
            address: '',
            phone: ''
        }
    },
    student: false,
    alimonyPayer: false,
    otherInfo: '',
    attachments: { typed: true, attachmentsByType: {} },
    assure: false
};
export function fromIncomeStatement(incomeStatement) {
    return {
        ...emptyIncomeStatementForm,
        startDate: incomeStatement.startDate,
        endDate: incomeStatement.endDate,
        highestFee: incomeStatement.type === 'HIGHEST_FEE',
        ...(incomeStatement.type === 'INCOME'
            ? {
                gross: mapGross(incomeStatement.gross),
                entrepreneur: mapEntrepreneur(incomeStatement.entrepreneur),
                student: incomeStatement.student,
                alimonyPayer: incomeStatement.alimonyPayer,
                otherInfo: incomeStatement.otherInfo,
                attachments: toIncomeStatementAttachments(incomeStatement.attachments)
            }
            : incomeStatement.type === 'CHILD_INCOME'
                ? {
                    childIncome: true,
                    otherInfo: incomeStatement.otherInfo,
                    attachments: toIncomeStatementAttachments(incomeStatement.attachments)
                }
                : undefined)
    };
}
function mapGross(gross) {
    if (!gross)
        return emptyIncomeStatementForm.gross;
    return {
        selected: true,
        incomeSource: gross.incomeSource,
        estimatedMonthlyIncome: gross.estimatedMonthlyIncome?.toString() ?? '',
        otherIncome: gross.otherIncome,
        otherIncomeInfo: gross.otherIncomeInfo
    };
}
function mapEstimation(estimatedIncome) {
    return {
        estimatedMonthlyIncome: estimatedIncome?.estimatedMonthlyIncome?.toString() ?? '',
        incomeStartDate: estimatedIncome?.incomeStartDate ?? null,
        incomeEndDate: estimatedIncome?.incomeEndDate ?? null
    };
}
function mapEntrepreneur(entrepreneur) {
    if (!entrepreneur)
        return emptyIncomeStatementForm.entrepreneur;
    return {
        selected: true,
        fullTime: entrepreneur.fullTime,
        startOfEntrepreneurship: entrepreneur.startOfEntrepreneurship,
        companyName: entrepreneur.companyName,
        businessId: entrepreneur.businessId,
        spouseWorksInCompany: entrepreneur.spouseWorksInCompany,
        startupGrant: entrepreneur.startupGrant,
        checkupConsent: entrepreneur.checkupConsent,
        selfEmployed: mapSelfEmployed(entrepreneur.selfEmployed),
        limitedCompany: mapLimitedCompany(entrepreneur.limitedCompany),
        partnership: entrepreneur.partnership,
        lightEntrepreneur: entrepreneur.lightEntrepreneur,
        accountant: entrepreneur.accountant ??
            emptyIncomeStatementForm.entrepreneur.accountant
    };
}
function mapSelfEmployed(selfEmployed) {
    if (!selfEmployed)
        return emptyIncomeStatementForm.entrepreneur.selfEmployed;
    return {
        selected: true,
        attachments: selfEmployed.attachments,
        estimation: selfEmployed.estimatedIncome != null,
        ...mapEstimation(selfEmployed.estimatedIncome)
    };
}
function mapLimitedCompany(limitedCompany) {
    if (!limitedCompany)
        return emptyIncomeStatementForm.entrepreneur.limitedCompany;
    return {
        selected: true,
        incomeSource: limitedCompany.incomeSource
    };
}
export function computeRequiredAttachments(formData) {
    const result = new Set();
    const { gross, entrepreneur, alimonyPayer, student } = formData;
    if (gross.selected) {
        if (gross.incomeSource === 'ATTACHMENTS')
            result.add('PAYSLIP_GROSS');
        if (gross.otherIncome)
            gross.otherIncome.forEach((item) => result.add(item));
    }
    if (entrepreneur.selected) {
        if (entrepreneur.startupGrant)
            result.add('STARTUP_GRANT');
        if (entrepreneur.selfEmployed.selected &&
            !entrepreneur.selfEmployed.estimation) {
            result.add('PROFIT_AND_LOSS_STATEMENT_SELF_EMPLOYED');
        }
        if (entrepreneur.limitedCompany.selected) {
            if (entrepreneur.limitedCompany.incomeSource === 'ATTACHMENTS') {
                result.add('PAYSLIP_LLC');
            }
            result.add('ACCOUNTANT_REPORT_LLC');
        }
        if (entrepreneur.partnership) {
            result
                .add('PROFIT_AND_LOSS_STATEMENT_PARTNERSHIP')
                .add('ACCOUNTANT_REPORT_PARTNERSHIP');
        }
        if (entrepreneur.lightEntrepreneur) {
            result.add('SALARY');
        }
    }
    if (gross.selected || entrepreneur.selected) {
        if (student)
            result.add('PROOF_OF_STUDIES');
        if (alimonyPayer)
            result.add('ALIMONY_PAYOUT');
    }
    return result;
}
//# sourceMappingURL=form.js.map