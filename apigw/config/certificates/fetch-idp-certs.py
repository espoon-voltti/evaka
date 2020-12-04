#!/usr/bin/env python3

# SPDX-FileCopyrightText: 2017-2020 City of Espoo
#
# SPDX-License-Identifier: LGPL-2.1-or-later

import sys
import urllib.request
import xml.etree.ElementTree as ET
from typing import List, Optional

from cryptography import x509
from cryptography.hazmat.backends import default_backend
from cryptography.x509.oid import NameOID
from cryptography.hazmat.primitives import serialization

# Newlines included as f-strings can't contain escapes
PEM_HEADER = "-----BEGIN CERTIFICATE-----\n"
PEM_FOOTER = "\n-----END CERTIFICATE-----"
SAML_NS = "{urn:oasis:names:tc:SAML:2.0:metadata}"
ENTITY_ID_KEY = "entityID"
ENTITY_DESCRIPTOR_TAG = f"{SAML_NS}EntityDescriptor"


def usage_exit():
    print("Usage: pipenv run ./fetch-idp-certs.py <metadata url> [<entity ID>]")
    print()
    print("Fetch and extract public certificates from SAML 2.0 metadata URL")
    exit(0)


def write_cert_to_file(cert_string: str) -> str:
    complete_cert = f"{PEM_HEADER}{cert_string.strip()}{PEM_FOOTER}"

    cert = x509.load_pem_x509_certificate(bytes(complete_cert, "utf-8"), default_backend())

    # Reasonable assumption that there's a single Common Name in the Subject
    common_name = cert.subject.get_attributes_for_oid(NameOID.COMMON_NAME)[0].value

    # Some CNs are silly, like "Microsoft Azure Federated SSO Certificate", let the user decide
    filename = input(f'Select filename for certificate (default: "{common_name}.pem"): ') or f"{common_name}.pem"

    f = open(filename, "wb")
    f.write(cert.public_bytes(serialization.Encoding.PEM))
    f.close()
    return f.name


def select_entity_from_metadata(tree: ET.Element, entity_id: Optional[str] = None) -> ET.Element:
    if tree.tag == ENTITY_DESCRIPTOR_TAG:
        # The metadata only contains a single entity (e.g. Azure AD metadata)
        print(f"Metadata only contains a single entity, proceeding with it: {tree.get(ENTITY_ID_KEY)}")
        return tree

    entities = {x.get(ENTITY_ID_KEY): x for x in tree.findall(ENTITY_DESCRIPTOR_TAG)}
    print(len(entities))

    selected_entity: ET.Element = None
    if entity_id is not None:
        try:
            selected_entity = next((entities[key] for key in entities if key == entity_id))
        except StopIteration:
            print(f'ERROR: No entityID "{entity_id}" found in metadata!')
            exit(1)
    else:
        print("Found entities:", *entities.keys(), sep="\n")
        selected_entity_id = input("Enter ID of entity to use: ")
        try:
            selected_entity = entities[selected_entity_id]
        except KeyError:
            print(f'ERROR: No entity "{selected_entity_id}" in metadata!')
            exit(1)

    return selected_entity


def find_signing_certs(entity: ET.Element) -> List[ET.Element]:
    xPathX509Cert = "".join(
        [
            "./",
            f"{SAML_NS}",
            "IDPSSODescriptor/",
            f"{SAML_NS}",
            'KeyDescriptor[@use="signing"]//',
            "{http://www.w3.org/2000/09/xmldsig#}",
            "X509Certificate",
        ]
    )
    return entity.findall(xPathX509Cert)


def main(metadata_url: str, entity_id: Optional[str] = None):
    response = urllib.request.urlopen(metadata_url).read()
    tree = ET.fromstring(response)

    selected_entity = select_entity_from_metadata(tree, entity_id)

    signing_certs = find_signing_certs(selected_entity)
    if len(signing_certs) < 1:
        print("ERROR: Couldn't find any signing certificates in the metadata!")
        exit(1)

    for cert in signing_certs:
        filename = write_cert_to_file(cert.text)
        print(f"Exported PEM certificate: {filename}")


if len(sys.argv) < 2 or sys.argv[1] in ["--help", "-h"]:
    usage_exit()

metadata_url = sys.argv[1]
entity_id = sys.argv[2] if len(sys.argv) >= 3 else None

main(metadata_url, entity_id)
