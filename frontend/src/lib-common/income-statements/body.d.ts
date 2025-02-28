import { IncomeStatementBody } from 'lib-common/generated/api-types/incomestatement';
import * as Form from 'lib-common/income-statements/form';
export declare function fromBody(personType: 'adult' | 'child', formData: Form.IncomeStatementForm, draft: boolean): IncomeStatementBody | null;
