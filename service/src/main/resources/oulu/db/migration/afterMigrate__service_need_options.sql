-- SPDX-FileCopyrightText: 2024 City of Oulu
--
-- SPDX-License-Identifier: LGPL-2.1-or-later

INSERT INTO service_need_option
(id, name_fi, name_sv, name_en, contract_days_per_month, valid_placement_type, default_option, fee_coefficient, occupancy_coefficient, daycare_hours_per_week, part_day, part_week, fee_description_fi, fee_description_sv, voucher_value_description_fi, voucher_value_description_sv, display_order, occupancy_coefficient_under_3y, show_for_citizen, realized_occupancy_coefficient, realized_occupancy_coefficient_under_3y, valid_from, valid_to)
VALUES
    -- DAYCARE
    ('de463972-9f97-11ec-a22c-931f5a294cea','Kokoaikainen varhaiskasvatus','Kokoaikainen varhaiskasvatus','Full-time early childhood education',NULL,'DAYCARE',TRUE,1.0,1.0,40,FALSE,FALSE,'Kokoaikainen varhaiskasvatus','Kokoaikainen varhaiskasvatus','Kokoaikainen varhaiskasvatus','Kokoaikainen varhaiskasvatus', null, 1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('86ef70a0-bf85-11eb-91e6-1fb57a101161','Kokoaikainen varhaiskasvatus','Kokoaikainen varhaiskasvatus','Full-time early childhood education',NULL,'DAYCARE',FALSE,1.0,1.0,40,FALSE,FALSE,'Kokoaikainen varhaiskasvatus','Kokoaikainen varhaiskasvatus','Kokoaikainen varhaiskasvatus','Kokoaikainen varhaiskasvatus', 1, 1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('503590f0-b961-11eb-b520-53740af3f7ee','Kokoaikainen varhaiskasvatus 10 pv/kk','Kokoaikainen varhaiskasvatus 10 pv/kk','Full-time early childhood education 10 d/M',10,'DAYCARE',FALSE,0.5,1.0,20.2,FALSE,FALSE,'Kokoaikainen varhaiskasvatus 10 pv/kk','Kokoaikainen varhaiskasvatus 10 pv/kk','Kokoaikainen varhaiskasvatus 10 pv/kk','Kokoaikainen varhaiskasvatus 10 pv/kk', 2, 1.75, true, 0.54, 1.75, '2000-01-01'::date, null),
    ('503591ae-b961-11eb-b521-1fca99358eef','Kokoaikainen varhaiskasvatus 13 pv/kk','Kokoaikainen varhaiskasvatus 13 pv/kk','Full-time early childhood education 13 d/M',13,'DAYCARE',FALSE,0.75,1.0,26.3,FALSE,FALSE,'Kokoaikainen varhaiskasvatus 13 pv/kk','Kokoaikainen varhaiskasvatus 13 pv/kk','Kokoaikainen varhaiskasvatus 13 pv/kk','Kokoaikainen varhaiskasvatus 13 pv/kk', 3, 1.75, true, 0.77, 1.75, '2000-01-01'::date, null),
    ('c2886f84-9ef2-11ec-8538-734ab2ac6c71','Varhaiskasvatus alle 7-h päivä','Varhaiskasvatus alle 7-h päivä','Early childhood education under 7-h day',NULL,'DAYCARE',FALSE,0.75,1.0,35,FALSE,FALSE,'Varhaiskasvatus alle 7-h päivä','Varhaiskasvatus alle 7-h päivä','Varhaiskasvatus alle 7-h päivä','Varhaiskasvatus alle 7-h päivä', 4,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a058-9ef2-11ec-853e-9bcaa26ea729','Yksityinen varhaiskasvatus','Yksityinen varhaiskasvatus','Private early childhood education',NULL,'DAYCARE',FALSE,1.0,1.0,40,FALSE,FALSE,'Yksityinen varhaiskasvatus','Yksityinen varhaiskasvatus','Yksityinen varhaiskasvatus','Yksityinen varhaiskasvatus', 5, 1.75, false, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a0c6-9ef2-11ec-853f-47b02ebd51bb','Yksityinen varhaiskasvatus palse','Yksityinen varhaiskasvatus palse','Private early childhood education service voucher',NULL,'DAYCARE',FALSE,1.0,1.0,40,FALSE,FALSE,'Yksityinen varhaiskasvatus palse','Yksityinen varhaiskasvatus palse','Yksityinen varhaiskasvatus palse','Yksityinen varhaiskasvatus palse', 6,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('19094188-130d-11ed-a572-473907d23b65','Yksityinen perhepäivähoito palse','Yksityinen perhepäivähoito palse','Private family daycare service voucher',NULL,'DAYCARE',FALSE,1.0,1.0,40,FALSE,FALSE,'Yksityinen perhepäivähoito palse','Yksityinen perhepäivähoito palse','Yksityinen perhepäivähoito palse','Yksityinen perhepäivähoito palse', 7,1, false, 1.0, 1.75, '2000-01-01'::date, null),

    -- DAYCARE_PART_TIME
    ('de463bd4-9f97-11ec-a22d-4791bc036bd9','Varhaiskasvatus alle 5-h päivä','Varhaiskasvatus alle 5-h päivä','Early childhood education under 5-h day',NULL,'DAYCARE_PART_TIME',TRUE,0.6,1.0,25,FALSE,FALSE,'Varhaiskasvatus alle 5-h päivä','Varhaiskasvatus alle 5-h päivä','Varhaiskasvatus alle 5-h päivä','Varhaiskasvatus alle 5-h päivä', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('86ef7370-bf85-11eb-91e7-6fcd728c518d','Varhaiskasvatus alle 5-h päivä','Varhaiskasvatus alle 5-h päivä','Early childhood education under 5-h day',NULL,'DAYCARE_PART_TIME',FALSE,0.6,1.0,25,FALSE,FALSE,'Varhaiskasvatus alle 5-h päivä','Varhaiskasvatus alle 5-h päivä','Varhaiskasvatus alle 5-h päivä','Varhaiskasvatus alle 5-h päivä', 1000,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c2886fca-9ef2-11ec-8539-1b9652921b6d','Yksityinen osa-aikainen 12-13pv/kk palse','Yksityinen osa-aikainen 12-13pv/kk palse','Private part time 12-13d/m service voucher',NULL,'DAYCARE_PART_TIME',FALSE,0.65,1.0,26.3,FALSE,FALSE,'Yksityinen osa-aikainen 12-13pv/kk palse','Yksityinen osa-aikainen 12-13pv/kk palse','Yksityinen osa-aikainen 12-13pv/kk palse','Yksityinen osa-aikainen 12-13pv/kk palse', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, null),
    ('c2889e1e-9ef2-11ec-853a-aff6768857cb','Yksityinen osa-aikainen 29h/viikko palse','Yksityinen osa-aikainen 29h/viikko palse','Private part time 29h/week service voucher',NULL,'DAYCARE_PART_TIME',FALSE,0.6,1.0,29,FALSE,FALSE,'Yksityinen osa-aikainen 29h/viikko palse','Yksityinen osa-aikainen 29h/viikko palse','Yksityinen osa-aikainen 29h/viikko palse','Yksityinen osa-aikainen 29h/viikko palse', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, null),
    ('c2889ef0-9ef2-11ec-853b-2f545a699ef0','Yksityinen osapäivä','Yksityinen osapäivä','Private part time',NULL,'DAYCARE_PART_TIME',FALSE,0.5,1.0,20,TRUE,FALSE,'Yksityinen osapäivä','Yksityinen osapäivä','Yksityinen osapäivä','Yksityinen osapäivä', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, null),
    ('c2889f72-9ef2-11ec-853c-3bc6a35d44a3','Yksityinen osapäivä 4h palse','Yksityinen osapäivä 4h palse','Private part time 4h service voucher',NULL,'DAYCARE_PART_TIME',FALSE,0.5,1.0,20,TRUE,FALSE,'Yksityinen osapäivä 4h palse','Yksityinen osapäivä 4h palse','Yksityinen osapäivä 4h palse','Yksityinen osapäivä 4h palse', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('50359334-b961-11eb-b525-f3febdfea5d3','Yksityinen osaviikko 20h palse','Yksityinen osaviikko 20h palse','Private part time 20h service voucher',NULL,'DAYCARE_PART_TIME',FALSE,0.5,1.0,20,FALSE,TRUE,'Yksityinen osaviikko 20h palse','Yksityinen osaviikko 20h palse','Yksityinen osaviikko 20h palse','Yksityinen osaviikko 20h palse', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, null),
    ('86495b70-130d-11ed-a573-bfda992853b8','Yksityinen perhepäivähoito osaviikko 20h palse','Yksityinen perhepäivähoito osaviikko 20h palse','Private family daycare part time 20h service voucher',NULL,'DAYCARE_PART_TIME',FALSE,0.55,1.0,20,FALSE,TRUE,'Yksityinen perhepäivähoito osaviikko 20h palse','Yksityinen perhepäivähoito osaviikko 20h palse','Yksityinen perhepäivähoito osaviikko 20h palse','Yksityinen perhepäivähoito osaviikko 20h palse', null,1, false, 1.0, 1.0, '2000-01-01'::date, null),
    ('d146f678-130d-11ed-a574-c39e98171347','Yksityinen perhepäivähoito osa-aikainen 12-13pv/kk palse','Yksityinen perhepäivähoito osa-aikainen 12-13pv/kk palse','Private family daycare part time 12-13d/m service voucher',NULL,'DAYCARE_PART_TIME',FALSE,0.65,1.0,26.3,FALSE,FALSE,'Yksityinen perhepäivähoito osa-aikainen 12-13pv/kk palse','Yksityinen perhepäivähoito osa-aikainen 12-13pv/kk palse','Yksityinen perhepäivähoito osa-aikainen 12-13pv/kk palse','Yksityinen perhepäivähoito osa-aikainen 12-13pv/kk palse', null,1, false, 1.0, 1.0, '2000-01-01'::date, null),
    ('07a1bde8-130e-11ed-a575-bbe09459e8d0','Yksityinen perhepäivähoito osa-aikainen 29h/viikko palse','Yksityinen perhepäivähoito osa-aikainen 29h/viikko palse','Private family daycare part time 29h/week service voucher',NULL,'DAYCARE_PART_TIME',FALSE,0.6,1.0,29,FALSE,FALSE,'Yksityinen perhepäivähoito osa-aikainen 29h/viikko palse','Yksityinen perhepäivähoito osa-aikainen 29h/viikko palse','Yksityinen perhepäivähoito osa-aikainen 29h/viikko palse','Yksityinen perhepäivähoito osa-aikainen 29h/viikko palse', null,1, false, 1.0, 1.0, '2000-01-01'::date, null),
    ('5c8bdebc-3350-11ed-a1e8-23631d9473d4','Yksityinen perhepäivähoito osapäivä 4h palse','Yksityinen perhepäivähoito osapäivä 4h palse','Private family daycare part time 4h service voucher',NULL,'DAYCARE_PART_TIME',FALSE,0.5,1.0,20,TRUE,FALSE,'Yksityinen perhepäivähoito osapäivä 4h palse','Yksityinen perhepäivähoito osapäivä 4h palse','Yksityinen perhepäivähoito osapäivä 4h palse','Yksityinen perhepäivähoito osapäivä 4h palse', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, null),

    -- PRESCHOOL
    ('de463c38-9f97-11ec-a22e-97907801ecdc','Esiopetus 4h ','Esiopetus 4h ','Pre-school education 4h ',NULL,'PRESCHOOL',TRUE,0,1.0,20,TRUE,FALSE,'Esiopetus 4h ','Esiopetus 4h ','Esiopetus 4h ','Esiopetus 4h', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a152-9ef2-11ec-8540-2fc7b01eb49e','Esiopetus 4h ','Esiopetus 4h ','Pre-school education 4h ',NULL,'PRESCHOOL',FALSE,0,1.0,20,TRUE,FALSE,'Esiopetus 4h ','Esiopetus 4h ','Esiopetus 4h ','Esiopetus 4h', 2000,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a26a-9ef2-11ec-8541-57b8edf0098f','Kaksivuotinen esiopetuskokeilu','Kaksivuotinen esiopetuskokeilu','A two-year pre-school trial',NULL,'PRESCHOOL',FALSE,0,1.0,20,TRUE,FALSE,'Kaksivuotinen esiopetuskokeilu','Kaksivuotinen esiopetuskokeilu','Kaksivuotinen esiopetuskokeilu','Kaksivuotinen esiopetuskokeilu', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, '2024-05-31'::date),
    ('c288a2d8-9ef2-11ec-8542-231141d61e61','Yksityinen esiopetus 4 h palse','Yksityinen esiopetus 4 h palse','Private pre-school education 4 h service voucher',NULL,'PRESCHOOL',FALSE,0.0,1.0,20,TRUE,FALSE,'Yksityinen esiopetus 4 h palse','Yksityinen esioeptus 4 h palse','Yksityinen esioeptus 4 h palse','Yksityinen esioeptus 4 h palse', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, '2024-07-31'::date),

    -- PRESCHOOL_DAYCARE
    ('de463c92-9f97-11ec-a22f-67d2f441c52f','Esiopetus + varhaiskasvatus','Esiopetus + varhaiskasvatus','Pre-school education + early childhood education',NULL,'PRESCHOOL_DAYCARE',TRUE,0.6,1.0,25,FALSE,FALSE,'Esiopetus + varhaiskasvatus','Esiopetus + varhaiskasvatus','Esiopetus + varhaiskasvatus','Esiopetus + varhaiskasvatus', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a33c-9ef2-11ec-8543-c7f61d98bbff','Esiopetus + varhaiskasvatus','Esiopetus + varhaiskasvatus','Pre-school education + early childhood education',NULL,'PRESCHOOL_DAYCARE',FALSE,0.6,1.0,25,FALSE,FALSE,'Esiopetus + varhaiskasvatus','Esiopetus + varhaiskasvatus','Esiopetus + varhaiskasvatus','Esiopetus + varhaiskasvatus', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('2e0f93a8-e57b-11ec-a452-7f636f92b30c','Esiopetus + varhaiskasvatus 2v','Esiopetus + varhaiskasvatus 2v','Pre-school education + early childhood education 2y',NULL,'PRESCHOOL_DAYCARE',FALSE,0.6,1.0,25,FALSE,FALSE,'Esiopetus + varhaiskasvatus 2v','Esiopetus + varhaiskasvatus 2v','Esiopetus + varhaiskasvatus 2v','Esiopetus + varhaiskasvatus 2v', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, '2024-05-31'::date),
    ('c288a3aa-9ef2-11ec-8544-072366604c6f','Esiopetus + varhaiskasvatus alle 3h','Esiopetus + varhaiskasvatus alle 3h','Pre-school education + early childhood education less than 3h',NULL,'PRESCHOOL_DAYCARE',FALSE,0.45,1.0,20,FALSE,FALSE,'Esiopetus + varhaiskasvatus alle 3h','Esiopetus + varhaiskasvatus alle 3h','Esiopetus + varhaiskasvatus alle 3h','Esiopetus + varhaiskasvatus alle 3h', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a3fa-9ef2-11ec-8545-0feaf5efd1b9','Esiopetus + varhaiskasvatus 10 pv/kk','Esiopetus + varhaiskasvatus 10 pv/kk','Pre-school education + early childhood education 10 d/m',10,'PRESCHOOL_DAYCARE',FALSE,0.3,1.0,9.5,FALSE,FALSE,'Esiopetus + varhaiskasvatus 10 pv/kk','Esiopetus + varhaiskasvatus 10 pv/kk','Esiopetus + varhaiskasvatus 10 pv/kk','Esiopetus + varhaiskasvatus 10 pv/kk', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a440-9ef2-11ec-8546-8f405f297780','Esiopetus + varhaiskasvatus 13 pv/kk','Esiopetus + varhaiskasvatus 13 pv/kk','Pre-school education + early childhood education 13 d/m',13,'PRESCHOOL_DAYCARE',FALSE,0.45,1.0,12.4,FALSE,FALSE,'Esiopetus + varhaiskasvatus 13 pv/kk','Esiopetus + varhaiskasvatus 13 pv/kk','Esiopetus + varhaiskasvatus 13 pv/kk','Esiopetus + varhaiskasvatus 13 pv/kk', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('91a50d60-3f28-11ed-8217-b35ad6248e8d','Esiopetus + varhaiskasvatus 10 pv/kk 2v','Esiopetus + varhaiskasvatus 10 pv/kk 2v','Pre-school education + early childhood education 10 d/m 2y',10,'PRESCHOOL_DAYCARE',FALSE,0.3,1.0,9.5,FALSE,FALSE,'Esiopetus + varhaiskasvatus 10 pv/kk 2v','Esiopetus + varhaiskasvatus 10 pv/kk 2v','Esiopetus + varhaiskasvatus 10 pv/kk 2v','Esiopetus + varhaiskasvatus 10 pv/kk 2v', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, '2024-05-31'::date),
    ('bf4b4946-3f28-11ed-8218-0392ee6d4528','Esiopetus + varhaiskasvatus 13 pv/kk 2v','Esiopetus + varhaiskasvatus 13 pv/kk 2v','Pre-school education + early childhood education 13 d/m 2y',13,'PRESCHOOL_DAYCARE',FALSE,0.45,1.0,12.4,FALSE,FALSE,'Esiopetus + varhaiskasvatus 13 pv/kk 2v','Esiopetus + varhaiskasvatus 13 pv/kk 2v','Esiopetus + varhaiskasvatus 13 pv/kk 2v','Esiopetus + varhaiskasvatus 13 pv/kk 2v', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, '2024-05-31'::date),
    ('99ab7baa-4083-11ed-8078-87d60cea9aea','Esiopetus + varhaiskasvatus alle 3h 2v','Esiopetus + varhaiskasvatus alle 3h 2v','Pre-school education + early childhood education less than 3h 2y',NULL,'PRESCHOOL_DAYCARE',FALSE,0.45,1.0,20,FALSE,FALSE,'Esiopetus + varhaiskasvatus alle 3h 2v','Esiopetus + varhaiskasvatus alle 3h 2v','Esiopetus + varhaiskasvatus alle 3h 2v','Esiopetus + varhaiskasvatus alle 3h 2v', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, '2024-05-31'::date),
    ('c288a490-9ef2-11ec-8547-ab2d0b032c1c','Yksityinen esiopetus + varhaiskasvatus palse','Yksityinen esiopetus + varhaiskasvatus palse','Private pre-school education + early childhood education service voucher',NULL,'PRESCHOOL_DAYCARE',FALSE,0.6,1.0,25,FALSE,FALSE,'Yksityinen esiopetus + varhaiskasvatus palse','Yksityinen esiopetus + varhaiskasvatus palse','Yksityinen esiopetus + varhaiskasvatus palse','Yksityinen esiopetus + varhaiskasvatus palse', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, '2024-07-31'::date),

    -- CLUB
    ('de463cec-9f97-11ec-a230-bb19e6bd6663','Kerho 2 x viikko','Kerho 2 x viikko','Club 2 x week',NULL,'CLUB',TRUE,0,1.0,10,FALSE,FALSE,'Kerho 2 x viikko','Kerho 2 x viikko','Kerho 2 x viikko','Kerho 2 x viikko', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a4d6-9ef2-11ec-8548-53f49e3cb87b','Kerho 2 x viikko','Kerho 2 x viikko','Club 2 x week',NULL,'CLUB',FALSE,0,1.0,10,FALSE,FALSE,'Kerho 2 x viikko','Kerho 2 x viikko','Kerho 2 x viikko','Kerho 2 x viikko', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a58a-9ef2-11ec-8549-2f93c47040bf','Kerho 3 x viikko','Kerho 3 x viikko','Club 3 x week',NULL,'CLUB',FALSE,0,1.0,10,FALSE,FALSE,'Kerho 3 x viikko','Kerho 3 x viikko','Kerho 3 x viikko','Kerho 3 x viikko', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a5d0-9ef2-11ec-854a-b33b00871752','Perhekerho','Perhekerho','Family club',NULL,'CLUB',FALSE,0,1.0,10,FALSE,FALSE,'Perhekerho','Perhekerho','Perhekerho','Perhekerho', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a620-9ef2-11ec-854b-13e81732636f','Tilapäinen kerho','Tilapäinen kerho','Temporary club',NULL,'CLUB',FALSE,0,1.0,10,FALSE,FALSE,'Tilapäinen kerho','Tilapäinen kerho','Tilapäinen kerho','Tilapäinen kerho', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, '2024-07-31'::date),
    ('c288a666-9ef2-11ec-854c-8bcc8b9a9307','Yksityinen kerho palse','Yksityinen kerho palse','Private club service voucher',NULL,'CLUB',FALSE,1.0,1.0,10,FALSE,FALSE,'Yksityinen kerho palse','Yksityinen kerho palse','Yksityinen kerho palse','Yksityinen kerho palse', null,1.75, false, 1.0, 1.75, '2000-01-01'::date, '2024-12-31'::date),

    -- TEMPORARY_DAYCARE
    ('de463d32-9f97-11ec-a231-5bcd9b48daeb', 'Tilapäinen varhaiskasvatus','Tilapäinen varhaiskasvatus','Temporary early childhood education',NULL,'TEMPORARY_DAYCARE',TRUE,1.0,1.0,40,FALSE,FALSE,'Tilapäinen varhaiskasvatus','Tilapäinen varhaiskasvatus','Tilapäinen varhaiskasvatus','Tilapäinen varhaiskasvatus', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a6ac-9ef2-11ec-854d-8ba46873165a','Tilapäinen varhaiskasvatus','Tilapäinen varhaiskasvatus','Temporary early childhood education',NULL,'TEMPORARY_DAYCARE',FALSE,1.0,1.0,40,FALSE,FALSE,'Tilapäinen varhaiskasvatus','Tilapäinen varhaiskasvatus','Tilapäinen varhaiskasvatus','Tilapäinen varhaiskasvatus', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),

    -- TEMPORARY_DAYCARE_PART_DAY
    ('7dedd9a8-a04c-11ec-a72b-3779d97aac97','Tilapäinen osa-aikainen varhaiskasvatus','Tilapäinen osa-aikainen varhaiskasvatus','temporary part-time early childhood education',NULL,'TEMPORARY_DAYCARE_PART_DAY',TRUE,1.0,1.0,20,TRUE,FALSE,'Tilapäinen osa-aikainen varhaiskasvatus','Tilapäinen osa-aikainen varhaiskasvatus','Tilapäinen osa-aikainen varhaiskasvatus','Tilapäinen osa-aikainen varhaiskasvatus', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a706-9ef2-11ec-854e-8fec0360d917','Tilapäinen osa-aikainen varhaiskasvatus','Tilapäinen osa-aikainen varhaiskasvatus','temporary part-time early childhood education',NULL,'TEMPORARY_DAYCARE_PART_DAY',FALSE,1.0,1.0,20,TRUE,FALSE,'Tilapäinen osa-aikainen varhaiskasvatus','Tilapäinen osa-aikainen varhaiskasvatus','Tilapäinen osa-aikainen varhaiskasvatus','Tilapäinen osa-aikainen varhaiskasvatus', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),

    -- PREPARATORY
    ('de463d82-9f97-11ec-a232-a782189e7c84', 'Valmistava esiopetus','Valmistava esiopetus','Preparatory pre-school education',NULL,'PREPARATORY',TRUE,0,1.0,25,TRUE,FALSE,'Valmistava esiopetus','Valmistava esiopetus','Valmistava esiopetus','Valmistava esiopetus', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('c288a74c-9ef2-11ec-854f-935955a88b27','Valmistava esiopetus','Valmistava esiopetus','Preparatory pre-school education',NULL,'PREPARATORY',FALSE,0,1.0,25,TRUE,FALSE,'Valmistava esiopetus','Valmistava esiopetus','Valmistava esiopetus','Valmistava esiopetus', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),

    -- PREPARATORY_DAYCARE
    ('05999cd6-1fa3-11ed-b545-8f2c5e506764','Valmistava esiopetus + varhaiskasvatus','Valmistava esiopetus + varhaiskasvatus','Preparatory pre-school education + early childhood education',NULL,'PREPARATORY_DAYCARE',TRUE,0.6,1.0,25,TRUE,FALSE,'Valmistava esiopetus + varhaiskasvatus','Valmistava esiopetus + varhaiskasvatus','Valmistava esiopetus + varhaiskasvatus','Valmistava esiopetus + varhaiskasvatus', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('53bad240-1fa3-11ed-b546-eb62f017d64a','Valmistava esiopetus + varhaiskasvatus','Valmistava esiopetus + varhaiskasvatus','Preparatory pre-school education + early childhood education',NULL,'PREPARATORY_DAYCARE',FALSE,0.6,1.0,25,TRUE,FALSE,'Valmistava esiopetus + varhaiskasvatus','Valmistava esiopetus + varhaiskasvatus','Valmistava esiopetus + varhaiskasvatus','Valmistava esiopetus + varhaiskasvatus', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('adff4b3e-4607-11ed-b4df-b338fa9c8022','Valmistava esiopetus + varhaiskasvatus 10 pv/kk','Valmistava esiopetus + varhaiskasvatus 10 pv/kk','Preparatory pre-school education + early childhood education 10 d/m',10,'PREPARATORY_DAYCARE',FALSE,0.3,1.0,9.5,FALSE,FALSE,'Valmistava esiopetus + varhaiskasvatus 10 pv/kk','Valmistava esiopetus + varhaiskasvatus 10 pv/kk','Valmistava esiopetus + varhaiskasvatus 10 pv/kk','Valmistava esiopetus + varhaiskasvatus 10 pv/kk', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('274acbd0-4608-11ed-b4e0-07ecacd56b03','Valmistava esiopetus + varhaiskasvatus 13 pv/kk','Valmistava esiopetus + varhaiskasvatus 13 pv/kk','Preparatory pre-school education + early childhood education 13 d/m',13,'PREPARATORY_DAYCARE',FALSE,0.45,1.0,12.4,FALSE,FALSE,'Valmistava esiopetus + varhaiskasvatus 13 pv/kk','Valmistava esiopetus + varhaiskasvatus 13 pv/kk','Valmistava esiopetus + varhaiskasvatus 13 pv/kk','Valmistava esiopetus + varhaiskasvatus 13 pv/kk', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null),
    ('4e62e91e-4608-11ed-b4e1-ab7b3db89639','Valmistava esiopetus + varhaiskasvatus alle 3h','Valmistava esiopetus + varhaiskasvatus alle 3h','Preparatory pre-school education + early childhood education less than 3h',NULL,'PREPARATORY_DAYCARE',FALSE,0.45,1.0,20,FALSE,FALSE,'Valmistava esiopetus + varhaiskasvatus alle 3h','Valmistava esiopetus + varhaiskasvatus alle 3h','Valmistava esiopetus + varhaiskasvatus alle 3h','Valmistava esiopetus + varhaiskasvatus alle 3h', null,1.75, true, 1.0, 1.75, '2000-01-01'::date, null)


    ON CONFLICT (id) DO
UPDATE SET
    name_fi = EXCLUDED.name_fi,
    name_sv = EXCLUDED.name_sv,
    name_en = EXCLUDED.name_en,
    contract_days_per_month = EXCLUDED.contract_days_per_month,
    valid_placement_type = EXCLUDED.valid_placement_type,
    default_option = EXCLUDED.default_option,
    fee_coefficient = EXCLUDED.fee_coefficient,
    occupancy_coefficient = EXCLUDED.occupancy_coefficient,
    daycare_hours_per_week = EXCLUDED.daycare_hours_per_week,
    part_day = EXCLUDED.part_day,
    part_week = EXCLUDED.part_week,
    fee_description_fi = EXCLUDED.fee_description_fi,
    fee_description_sv = EXCLUDED.fee_description_sv,
    voucher_value_description_fi = EXCLUDED.voucher_value_description_fi,
    voucher_value_description_sv = EXCLUDED.voucher_value_description_sv,
    display_order = EXCLUDED.display_order,
    occupancy_coefficient_under_3y = EXCLUDED.occupancy_coefficient_under_3y,
    show_for_citizen = EXCLUDED.show_for_citizen,
    realized_occupancy_coefficient = EXCLUDED.realized_occupancy_coefficient,
    realized_occupancy_coefficient_under_3y = EXCLUDED.realized_occupancy_coefficient_under_3y,
    valid_from = EXCLUDED.valid_from,
    valid_to = EXCLUDED.valid_to
WHERE
    service_need_option.name_fi <> EXCLUDED.name_fi OR
    service_need_option.name_sv <> EXCLUDED.name_sv OR
    service_need_option.name_en <> EXCLUDED.name_en OR
    service_need_option.contract_days_per_month <> EXCLUDED.contract_days_per_month OR
    service_need_option.valid_placement_type <> EXCLUDED.valid_placement_type OR
    service_need_option.default_option <> EXCLUDED.default_option OR
    service_need_option.fee_coefficient <> EXCLUDED.fee_coefficient OR
    service_need_option.occupancy_coefficient <> EXCLUDED.occupancy_coefficient OR
    service_need_option.daycare_hours_per_week <> EXCLUDED.daycare_hours_per_week OR
    service_need_option.part_day <> EXCLUDED.part_day OR
    service_need_option.part_week <> EXCLUDED.part_week OR
    service_need_option.fee_description_fi <> EXCLUDED.fee_description_fi OR
    service_need_option.fee_description_sv <> EXCLUDED.fee_description_sv OR
    service_need_option.voucher_value_description_fi <> EXCLUDED.voucher_value_description_fi OR
    service_need_option.voucher_value_description_sv <> EXCLUDED.voucher_value_description_sv OR
    service_need_option.display_order <> EXCLUDED.display_order OR
    service_need_option.occupancy_coefficient_under_3y <> EXCLUDED.occupancy_coefficient_under_3y OR
    service_need_option.show_for_citizen <> EXCLUDED.show_for_citizen OR
    service_need_option.realized_occupancy_coefficient <> EXCLUDED.realized_occupancy_coefficient OR
    service_need_option.realized_occupancy_coefficient_under_3y <> EXCLUDED.realized_occupancy_coefficient_under_3y OR
    service_need_option.valid_from <> EXCLUDED.valid_from OR
    service_need_option.valid_to <> EXCLUDED.valid_to OR
    (service_need_option.valid_to IS NULL AND EXCLUDED.valid_to IS NOT NULL);
