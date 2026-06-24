import type { DocumentType, ReviewFieldDefinition } from '../types/document';

const rovinietaFields: ReviewFieldDefinition[] = [
  { key: 'licensePlate', label: 'License plate', type: 'text', required: true },
  { key: 'vin', label: 'VIN', type: 'text' },
  { key: 'category', label: 'Category', type: 'text' },
  { key: 'validFrom', label: 'Valid from', type: 'date' },
  { key: 'validUntil', label: 'Valid until', type: 'date', required: true },
  { key: 'issuer', label: 'Issuer', type: 'text' },
  { key: 'transactionId', label: 'Transaction ID', type: 'text' },
  { key: 'amount', label: 'Amount', type: 'number' },
  { key: 'currency', label: 'Currency', type: 'select', options: ['RON', 'EUR', 'USD'] },
];

const insuranceFields: ReviewFieldDefinition[] = [
  { key: 'policyNumber', label: 'Policy number', type: 'text' },
  { key: 'insurerName', label: 'Insurer name', type: 'text' },
  { key: 'ownerName', label: 'Owner name', type: 'text' },
  { key: 'licensePlate', label: 'License plate', type: 'text', required: true },
  { key: 'vin', label: 'VIN', type: 'text' },
  { key: 'validFrom', label: 'Valid from', type: 'date' },
  { key: 'validUntil', label: 'Valid until', type: 'date', required: true },
];

const inspectionFields: ReviewFieldDefinition[] = [
  { key: 'inspectionNumber', label: 'Inspection number', type: 'text' },
  { key: 'stationName', label: 'Station name', type: 'text' },
  { key: 'licensePlate', label: 'License plate', type: 'text', required: true },
  { key: 'vin', label: 'VIN', type: 'text' },
  { key: 'inspectionDate', label: 'Inspection date', type: 'date' },
  { key: 'validUntil', label: 'Valid until', type: 'date', required: true },
  { key: 'result', label: 'Result', type: 'select', options: ['PASSED', 'FAILED'] },
  { key: 'odometerKm', label: 'Odometer km', type: 'number' },
];

const invoiceFields: ReviewFieldDefinition[] = [
  { key: 'invoiceNumber', label: 'Invoice number', type: 'text' },
  { key: 'supplierName', label: 'Supplier name', type: 'text' },
  { key: 'supplierTaxId', label: 'Supplier tax ID', type: 'text' },
  { key: 'invoiceDate', label: 'Invoice date', type: 'date' },
  { key: 'totalAmount', label: 'Total amount', type: 'number' },
  { key: 'currency', label: 'Currency', type: 'select', options: ['RON', 'EUR', 'USD'] },
  { key: 'vatAmount', label: 'VAT amount', type: 'number' },
  {
    key: 'expenseCategory',
    label: 'Expense category',
    type: 'select',
    options: ['FUEL', 'SERVICE', 'TIRE_REPLACEMENT', 'CAR_WASH', 'PARKING', 'ROAD_TAX', 'INSURANCE', 'OTHER'],
  },
  { key: 'licensePlate', label: 'License plate', type: 'text' },
  { key: 'vin', label: 'VIN', type: 'text' },
  { key: 'odometerKm', label: 'Odometer km', type: 'number' },
  { key: 'items', label: 'Items', type: 'textarea' },
];

const otherFields: ReviewFieldDefinition[] = [
  { key: 'documentTitle', label: 'Document title', type: 'text' },
  { key: 'licensePlate', label: 'License plate', type: 'text' },
  { key: 'vin', label: 'VIN', type: 'text' },
  { key: 'issueDate', label: 'Issue date', type: 'date' },
  { key: 'summary', label: 'Summary', type: 'textarea' },
];

export function getReviewFields(
  documentType: DocumentType,
  subtype?: string | null,
): ReviewFieldDefinition[] {
  const normalizedSubtype = subtype?.toUpperCase() ?? '';

  if (documentType === 'ROAD_TAX' && (!normalizedSubtype || normalizedSubtype === 'ROVINIETA')) {
    return rovinietaFields;
  }
  if (documentType === 'INSURANCE' && (!normalizedSubtype || normalizedSubtype === 'RCA')) {
    return insuranceFields;
  }
  if (documentType === 'TECHNICAL_INSPECTION' && (!normalizedSubtype || normalizedSubtype === 'ITP')) {
    return inspectionFields;
  }
  if (documentType === 'EXPENSE_INVOICE') {
    return invoiceFields;
  }
  return otherFields;
}
