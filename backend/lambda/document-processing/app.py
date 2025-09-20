import json
import boto3
import os
import logging
import tempfile
from botocore.exceptions import ClientError
from pdfplumber import pdfplumber
from docx import Document
import clamav

# Configure logging
logger = logging.getLogger()
logger.setLevel(logging.INFO)

s3_client = boto3.client('s3')
clamav_client = clamav.ClamAV()  # Assuming clamav python client is available

def extract_text_from_pdf(file_path):
    text = ""
    try:
        with pdfplumber.open(file_path) as pdf:
            for page in pdf.pages:
                text += page.extract_text() + "\n"
    except Exception as e:
        logger.error(f"Error extracting text from PDF: {e}")
        raise
    return text

def extract_text_from_docx(file_path):
    text = ""
    try:
        doc = Document(file_path)
        for para in doc.paragraphs:
            text += para.text + "\n"
    except Exception as e:
        logger.error(f"Error extracting text from DOCX: {e}")
        raise
    return text

def virus_scan(file_path):
    try:
        result = clamav_client.scan_file(file_path)
        if result.is_infected:
            raise Exception("Virus detected in file")
    except Exception as e:
        logger.error(f"Virus scan failed or virus detected: {e}")
        raise

def lambda_handler(event, context):
    try:
        bucket = event['Records'][0]['s3']['bucket']['name']
        key = event['Records'][0]['s3']['object']['key']

        with tempfile.TemporaryDirectory() as tmpdir:
            download_path = os.path.join(tmpdir, os.path.basename(key))
            s3_client.download_file(bucket, key, download_path)

            # Virus scan
            virus_scan(download_path)

            # Extract text based on file type
            if key.lower().endswith('.pdf'):
                text = extract_text_from_pdf(download_path)
            elif key.lower().endswith('.docx'):
                text = extract_text_from_docx(download_path)
            else:
                raise Exception("Unsupported file type")

            # Log extracted text length
            logger.info(f"Extracted text length: {len(text)}")

            # TODO: Send extracted text to translation service or store for further processing

            return {
                'statusCode': 200,
                'body': json.dumps('Document processed successfully')
            }
    except ClientError as e:
        logger.error(f"AWS ClientError: {e}")
        return {
            'statusCode': 500,
            'body': json.dumps('Error processing document')
        }
    except Exception as e:
        logger.error(f"Error: {e}")
        return {
            'statusCode': 500,
            'body': json.dumps('Error processing document')
        }
