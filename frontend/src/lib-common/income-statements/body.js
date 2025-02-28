// SPDX-FileCopyrightText: 2017-2022 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later
import { collectAttachmentIds } from 'lib-common/income-statements/attachments';
import * as Form from 'lib-common/income-statements/form';
import { stringToInt } from 'lib-common/utils/number';
export function fromBody(personType, formData, draft) {
    if (!draft && !formData.assure)
        return null;
    const startDate = formData.startDate;
    if (startDate === null)
        return null;
    if (!draft && formData.endDate && startDate > formData.endDate)
        return null;
    if (formData.highestFee) {
        if (personType === 'child') {
            return null;
        }
        return { type: 'HIGHEST_FEE', startDate, endDate: formData.endDate };
    }
    else {
        if (!draft && personType === 'adult' && !formData.endDate)
            return null;
    }
    if (formData.childIncome) {
        const attachmentIds = collectAttachmentIds(formData.attachments);
        if (attachmentIds.length === 0)
            return null;
        const childIncome = {
            type: 'CHILD_INCOME',
            startDate,
            endDate: formData.endDate,
            otherInfo: formData.otherInfo,
            attachmentIds
        };
        return childIncome;
    }
    // Require attachments "one-by-one" if there are no old-style attachments without a type
    if (!validateAttachments(formData, draft))
        return null;
    const gross = validateGross(formData.gross);
    const entrepreneur = validateEntrepreneur(formData.entrepreneur, draft);
    if (gross === invalid ||
        entrepreneur === invalid ||
        (!gross && !entrepreneur)) {
        return null;
    }
    return {
        type: 'INCOME',
        startDate,
        endDate: formData.endDate,
        gross,
        entrepreneur,
        student: formData.student,
        alimonyPayer: formData.alimonyPayer,
        otherInfo: formData.otherInfo,
        attachmentIds: collectAttachmentIds(formData.attachments)
    };
}
const invalid = Symbol();
function validateAttachments(formData, draft) {
    if (draft || !formData.attachments.typed)
        return true;
    const { attachmentsByType } = formData.attachments;
    const requiredAttachments = Form.computeRequiredAttachments(formData);
    return [...requiredAttachments].every((type) => (attachmentsByType[type]?.length ?? 0) > 0);
}
function validateGross(formData) {
    if (!formData.selected)
        return null;
    if (formData.incomeSource === null)
        return invalid;
    const estimatedMonthlyIncome = stringToInt(formData.estimatedMonthlyIncome) ?? invalid;
    if (estimatedMonthlyIncome === invalid)
        return invalid;
    return {
        incomeSource: formData.incomeSource,
        estimatedMonthlyIncome,
        otherIncome: formData.otherIncome ?? [],
        otherIncomeInfo: formData.otherIncomeInfo
    };
}
function validateEntrepreneur(formData, draft) {
    if (!formData.selected)
        return null;
    const { fullTime, companyName, businessId, spouseWorksInCompany, startupGrant, partnership, lightEntrepreneur, checkupConsent } = formData;
    const startOfEntrepreneurship = formData.startOfEntrepreneurship ?? invalid;
    const selfEmployed = validateSelfEmployed(formData.selfEmployed);
    const limitedCompany = validateLimitedCompany(formData.limitedCompany);
    if (fullTime === null ||
        startOfEntrepreneurship === invalid ||
        spouseWorksInCompany === null ||
        selfEmployed === invalid ||
        limitedCompany === invalid ||
        (!selfEmployed && !limitedCompany && !partnership && !lightEntrepreneur)) {
        return invalid;
    }
    const accountant = limitedCompany || selfEmployed || partnership
        ? validateAccountant(formData.accountant, draft)
        : null;
    if (accountant === invalid) {
        return invalid;
    }
    return {
        fullTime,
        startOfEntrepreneurship,
        companyName,
        businessId,
        spouseWorksInCompany,
        startupGrant,
        checkupConsent,
        selfEmployed,
        limitedCompany,
        partnership,
        lightEntrepreneur,
        accountant
    };
}
function validateSelfEmployed(formData) {
    if (!formData.selected)
        return null;
    const estimation = formData.estimation
        ? validateEstimatedIncome(formData)
        : null;
    if (estimation === invalid)
        return invalid;
    return {
        attachments: formData.attachments,
        estimatedIncome: estimation
    };
}
function validateEstimatedIncome(formData) {
    const estimatedMonthlyIncome = stringToInt(formData.estimatedMonthlyIncome) ?? invalid;
    if (estimatedMonthlyIncome === invalid || !formData.incomeStartDate) {
        return invalid;
    }
    if (formData.incomeEndDate &&
        formData.incomeStartDate > formData.incomeEndDate) {
        return invalid;
    }
    return {
        estimatedMonthlyIncome,
        incomeStartDate: formData.incomeStartDate,
        incomeEndDate: formData.incomeEndDate
    };
}
function validateLimitedCompany(formData) {
    if (!formData.selected)
        return null;
    if (formData.incomeSource === null)
        return invalid;
    return { incomeSource: formData.incomeSource };
}
function validateAccountant(accountant, draft) {
    const result = {
        name: accountant.name.trim(),
        address: accountant.address.trim(),
        phone: accountant.phone.trim(),
        email: accountant.email.trim()
    };
    if (!draft && (!result.name || !result.phone || !result.email))
        return invalid;
    return result;
}
//# sourceMappingURL=body.js.map