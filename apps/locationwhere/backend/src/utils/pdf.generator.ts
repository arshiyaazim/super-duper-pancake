import PDFDocument from 'pdfkit';

export const generateEmployeeReport = (data: any): Promise<Buffer> => {
  return new Promise((resolve, reject) => {
    const doc = new PDFDocument();
    const chunks: Buffer[] = [];

    doc.on('data', (chunk) => chunks.push(chunk));
    doc.on('end', () => resolve(Buffer.concat(chunks)));
    doc.on('error', (err) => reject(err));

    doc.fontSize(20).text('Employee Activity Report', { align: 'center' });
    doc.moveDown();
    doc.fontSize(12).text(`Employee: ${data.fullName} (${data.employeeCode})`);
    doc.text(`Period: ${data.period}`);
    doc.moveDown();

    doc.text('Summary of Activities:', { underline: true });
    doc.text(`Total Calls: ${data.totalCalls}`);
    doc.text(`Geofence Breaches: ${data.geofenceBreaches}`);
    doc.moveDown();

    // Add more details from data...

    doc.end();
  });
};
