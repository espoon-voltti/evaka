UPDATE voucher_value_decision d SET
    service_need_fee_description_sv = coalesce(opt.fee_description_sv, d.service_need_fee_description_sv),
    service_need_voucher_value_description_sv = coalesce(opt.voucher_value_description_sv, d.service_need_voucher_value_description_sv)
FROM service_need_option opt
WHERE opt.fee_description_fi = d.service_need_fee_description_fi AND opt.voucher_value_description_fi = service_need_voucher_value_description_fi;
