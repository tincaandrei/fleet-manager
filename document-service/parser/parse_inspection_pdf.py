import json
import re
import sys
from pathlib import Path
from pypdf import PdfReader


FIELD_PATTERNS = {
    "documentType": r"Document Type:\s*(.+)",
    "inspectionNumber": r"Inspection Number:\s*(.+)",
    "vehicleId": r"Vehicle ID:\s*(\d+)",
    "licensePlate": r"License Plate:\s*(.+)",
    "vin": r"VIN:\s*(.+)",
    "inspectionDate": r"Inspection Date:\s*(\d{4}-\d{2}-\d{2})",
    "expiryDate": r"Expiry Date:\s*(\d{4}-\d{2}-\d{2})",
    "odometer": r"Odometer:\s*(.+)",
    "result": r"Result:\s*(.+)",
    "inspectionCenter": r"Inspection Center:\s*(.+)",
}


def extract_text_from_pdf(pdf_path: Path) -> str:
    reader = PdfReader(str(pdf_path))
    text_parts = []

    for page in reader.pages:
        page_text = page.extract_text()
        if page_text:
            text_parts.append(page_text)

    return "\n".join(text_parts)


def parse_metadata(text: str) -> dict:
    metadata = {}

    for field_name, pattern in FIELD_PATTERNS.items():
        match = re.search(pattern, text)
        metadata[field_name] = match.group(1).strip() if match else None

    if metadata.get("vehicleId") is not None:
        metadata["vehicleId"] = int(metadata["vehicleId"])

    return metadata


def main():
    if len(sys.argv) < 2:
        print(json.dumps({
            "parserStatus": "FAILED",
            "errorMessage": "Missing PDF path argument",
            "data": None
        }))
        sys.exit(1)

    pdf_path = Path(sys.argv[1])

    if not pdf_path.exists():
        print(json.dumps({
            "parserStatus": "FAILED",
            "errorMessage": f"PDF not found: {pdf_path}",
            "data": None
        }))
        sys.exit(1)

    try:
        text = extract_text_from_pdf(pdf_path)
        metadata = parse_metadata(text)

        print(json.dumps({
            "parserStatus": "PARSED",
            "parserName": "inspection-pdf-parser",
            "parserVersion": "1.0.0",
            "confidence": 0.90,
            "data": metadata
        }))

    except Exception as e:
        print(json.dumps({
            "parserStatus": "FAILED",
            "errorMessage": str(e),
            "data": None
        }))
        sys.exit(1)


if __name__ == "__main__":
    main()
