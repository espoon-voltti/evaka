<!--
SPDX-FileCopyrightText: 2017-2025 City of Espoo

SPDX-License-Identifier: LGPL-2.1-or-later
-->

# PDF to PDF/A Converter

Simple service that exposes a REST API for converting PDF documents to PDF/A format using Ghostscript.

## API

### Health Check

```
GET /health
```

Returns HTTP 200 if the service is healthy.

### Convert PDF to PDF/A

```
POST /convert
```

**Request:**
- Content-Type: multipart/form-data
- Field: pdf - The PDF file to convert

**Response:**
- Content-Type: application/pdf
- The converted PDF/A document

## Development

```bash
# Install dependencies
npm install

# Run in development mode
npm run dev

# Build for production
npm run build
```

## Docker

The service is included in the main docker-compose configuration and can be started along with the other services:

```bash
docker-compose up pdf-converter
```

## Testing

```bash
curl -X POST -F "pdf=@document.pdf" -o converted.pdf http://localhost:9091/convert
```
