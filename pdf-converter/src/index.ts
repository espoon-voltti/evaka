// SPDX-FileCopyrightText: 2017-2025 City of Espoo
//
// SPDX-License-Identifier: LGPL-2.1-or-later

import express, { Request, Response } from 'express'
import multer from 'multer'
import { spawn } from 'child_process'
import { config } from './config'

const app = express()

// Use memory storage to avoid writing to disk
const storage = multer.memoryStorage()
const upload = multer({ 
  storage,
  limits: { fileSize: 10 * 1024 * 1024 } // 10MB limit
})

app.get('/health', (_: Request, res: Response) => {
  res.sendStatus(200)
})

// Endpoint for PDF to PDF/A conversion
app.post('/convert', upload.single('pdf'), async (req: Request, res: Response): Promise<void> => {
  try {
    if (!req.file) {
      res.status(400).send('No PDF file provided')
      return
    }

    if (req.file.mimetype !== 'application/pdf') {
      res.status(400).send('File must be a PDF')
      return
    }

    // Buffer to store the converted PDF
    const chunks: Buffer[] = []
    
    // Create a Ghostscript process with quiet flags to suppress verbose output
    const gs = spawn('gs', [
      '-q',           // Quiet mode - suppress startup messages
      '-dQUIET',      // Even more quiet - suppress most messages
      '-dPDFA',
      '-dBATCH',
      '-dNOPAUSE',
      '-sColorConversionStrategy=UseDeviceIndependentColor',
      '-sDEVICE=pdfwrite',
      '-dPDFACompatibilityPolicy=2',
      '-sOutputFile=-', // Output to stdout
      '-' // Input from stdin
    ])

    // Handle process errors
    gs.on('error', (err: Error) => {
      console.error('Failed to start Ghostscript process:', err)
      if (!res.headersSent) {
        res.status(500).send('Error converting PDF: Failed to start Ghostscript')
      }
    })

    // Collect stdout data (converted PDF)
    gs.stdout.on('data', (chunk: Buffer) => {
      chunks.push(Buffer.from(chunk))
    })

    // Log stderr output
    gs.stderr.on('data', (data: Buffer) => {
      console.error('Ghostscript stderr:', data.toString())
    })

    // Handle process completion
    gs.on('close', (code: number) => {
      if (code !== 0) {
        console.error(`Ghostscript process exited with code ${code}`)
        if (!res.headersSent) {
          res.status(500).send(`Error converting PDF: Ghostscript exited with code ${code}`)
        }
        return
      }

      // Concatenate all chunks into a single buffer
      const resultPdf = Buffer.concat(chunks)
      
      // Validate that the output looks like a PDF file
      if (!resultPdf.length || !resultPdf.toString('ascii', 0, 5).startsWith('%PDF-')) {
        console.error('Invalid PDF output received from Ghostscript')
        if (!res.headersSent) {
          res.status(500).send('Error converting PDF: Invalid output format')
        }
        return
      }
      
      // Set appropriate headers
      res.setHeader('Content-Type', 'application/pdf')
      res.setHeader('Content-Disposition', 'attachment; filename="converted.pdf"')
      
      // Send the converted PDF
      res.send(resultPdf)
    })

    // Write the input PDF to stdin
    gs.stdin.write(req.file.buffer)
    gs.stdin.end()

  } catch (error) {
    console.error('Conversion error:', error)
    if (!res.headersSent) {
      res.status(500).send('Error converting PDF')
    }
  }
})

app.listen(config.HTTP_PORT, () => {
  console.log(`pdf-converter listening on port ${config.HTTP_PORT}`)
}) 