ALTER TYPE preschool_assistance_level ADD VALUE 'CHILD_SUPPORT';
ALTER TYPE preschool_assistance_level ADD VALUE 'CHILD_SUPPORT_AND_EXTENDED_COMPULSORY_EDUCATION';
ALTER TYPE preschool_assistance_level ADD VALUE 'GROUP_SUPPORT';

ALTER TYPE koski_preschool_input_data ADD ATTRIBUTE child_support datemultirange;
ALTER TYPE koski_preschool_input_data ADD ATTRIBUTE child_support_and_extended_compulsory_education datemultirange;
