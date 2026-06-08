package com.mioptica.service;

import com.mioptica.dto.VentaDetalleDTO;
import com.mioptica.model.FichaClinica;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import com.mioptica.model.FichaClinica;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Service
public class ExportService {

    private static String formatoQuetzales(BigDecimal valor) {
        if (valor == null) {
            return "Q 0.00";
        }
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "Q " + nf.format(valor.doubleValue());
    }

    // ─── EXPORTAR EXCEL ───────────────────────────────────────────
    public byte[] exportarExcel(List<VentaDetalleDTO> ventas, LocalDate fi, LocalDate ff) throws Exception {

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reporte de Ventas");

            // Estilo encabezado
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Estilo datos
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(BorderStyle.THIN);
            dataStyle.setBorderTop(BorderStyle.THIN);
            dataStyle.setBorderLeft(BorderStyle.THIN);
            dataStyle.setBorderRight(BorderStyle.THIN);

            // Fila título
            Row titulo = sheet.createRow(0);
            Cell celdaTitulo = titulo.createCell(0);
            celdaTitulo.setCellValue("Reporte de Ventas — Del " + fi + " al " + ff);
            CellStyle tituloStyle = workbook.createCellStyle();
            Font tituloFont = workbook.createFont();
            tituloFont.setBold(true);
            tituloFont.setFontHeightInPoints((short) 14);
            tituloStyle.setFont(tituloFont);
            celdaTitulo.setCellStyle(tituloStyle);

            // Fila encabezados
            Row header = sheet.createRow(2);
            String[] columnas = {"N° Factura", "Fecha", "Vendedor", "Categoría",
                                 "Producto / Modelo", "Cantidad", "Total Q", "Forma de Pago"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }

            // Filas de datos
            int rowNum = 3;
            for (VentaDetalleDTO v : ventas) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(v.getNumeroFactura());
                row.createCell(1).setCellValue(v.getFecha() != null ? v.getFecha().toString() : "");
                row.createCell(2).setCellValue(v.getVendedor());
                row.createCell(3).setCellValue(v.getCategoria());
                row.createCell(4).setCellValue(v.getProducto());
                row.createCell(5).setCellValue(v.getCantidad());
                row.createCell(6).setCellValue(formatoQuetzales(v.getPrecioVenta()));
                row.createCell(7).setCellValue(v.getFormaPago());

                for (int i = 0; i <= 7; i++) {
                    row.getCell(i).setCellStyle(dataStyle);
                }
            }

            // Ajustar ancho columnas
            for (int i = 0; i <= 7; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ─── EXPORTAR PDF ─────────────────────────────────────────────
    public byte[] exportarPdf(List<VentaDetalleDTO> ventas, LocalDate fi, LocalDate ff) throws Exception {

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Título
        com.itextpdf.text.Font tituloFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 16,
                com.itextpdf.text.Font.BOLD, BaseColor.DARK_GRAY);
        Paragraph titulo = new Paragraph("Reporte de Ventas", tituloFont);
        titulo.setAlignment(Element.ALIGN_CENTER);
        document.add(titulo);

        // Subtítulo
        com.itextpdf.text.Font subFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 10,
                com.itextpdf.text.Font.NORMAL, BaseColor.GRAY);
        Paragraph sub = new Paragraph("Del " + fi + " al " + ff, subFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingAfter(15);
        document.add(sub);

        // Tabla
        PdfPTable table = new PdfPTable(8);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{2f, 1.5f, 2f, 2f, 3f, 1f, 1.5f, 2f});

        // Color encabezado
        BaseColor headerColor = new BaseColor(27, 94, 92);
        com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 9,
                com.itextpdf.text.Font.BOLD, BaseColor.WHITE);

        // Encabezados
        String[] cols = {"N° Factura", "Fecha", "Vendedor", "Categoría",
                         "Producto", "Cant.", "Total Q", "Forma Pago"};
        for (String col : cols) {
            PdfPCell cell = new PdfPCell(new Phrase(col, headerFont));
            cell.setBackgroundColor(headerColor);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setPadding(6);
            table.addCell(cell);
        }

        // Datos
        com.itextpdf.text.Font dataFont = new com.itextpdf.text.Font(
                com.itextpdf.text.Font.FontFamily.HELVETICA, 8);
        boolean altRow = false;
        BaseColor altColor = new BaseColor(240, 248, 248);

        for (VentaDetalleDTO v : ventas) {
            BaseColor rowColor = altRow ? altColor : BaseColor.WHITE;

            String[] valores = {
                v.getNumeroFactura(),
                v.getFecha() != null ? v.getFecha().toString() : "",
                v.getVendedor(),
                v.getCategoria(),
                v.getProducto(),
                String.valueOf(v.getCantidad()),
                formatoQuetzales(v.getPrecioVenta()),
                v.getFormaPago()
            };

            for (String valor : valores) {
                PdfPCell cell = new PdfPCell(new Phrase(valor != null ? valor : "", dataFont));
                cell.setBackgroundColor(rowColor);
                cell.setPadding(5);
                table.addCell(cell);
            }
            altRow = !altRow;
        }

        document.add(table);
        document.close();
        return out.toByteArray();
    }
    // ─── EXCEL FICHAS CLÍNICAS ────────────────────────────────────
public byte[] exportarFichasClinicas(List<FichaClinica> fichas, LocalDate fi, LocalDate ff) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("Fichas Clínicas");

        // Estilos
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Título
        Row titulo = sheet.createRow(0);
        Cell celdaTitulo = titulo.createCell(0);
        celdaTitulo.setCellValue("Mi Óptica — Fichas Clínicas del " + fi + " al " + ff);
        CellStyle tituloStyle = workbook.createCellStyle();
        Font tituloFont = workbook.createFont();
        tituloFont.setBold(true);
        tituloFont.setFontHeightInPoints((short) 13);
        tituloStyle.setFont(tituloFont);
        celdaTitulo.setCellStyle(tituloStyle);

        // Encabezados
        Row header = sheet.createRow(2);
        String[] columnas = {"#", "N° Ficha", "Fecha", "NIT", "Cliente",
                             "Optometrista", "Total Q", "Abono Q",
                             "Saldo Q", "Estado Entrega", "Fecha Entrega"};
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos
        int rowNum = 3;
        int num = 1;
        for (FichaClinica f : fichas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(num++);
            row.createCell(1).setCellValue(f.getIdFicha());
            row.createCell(2).setCellValue(f.getFecha() != null ? f.getFecha().toString() : "");
            row.createCell(3).setCellValue(f.getCliente().getNit() != null ? f.getCliente().getNit() : "CF");
            row.createCell(4).setCellValue(f.getCliente().getNombre());
            row.createCell(5).setCellValue(f.getOptometrista().getNombreCompleto());
            row.createCell(6).setCellValue(formatoQuetzales(f.getTotal()));
            row.createCell(7).setCellValue(formatoQuetzales(f.getAbono()));
            row.createCell(8).setCellValue(formatoQuetzales(f.getSaldo()));
            row.createCell(9).setCellValue(f.getEstadoEntrega() != null ? f.getEstadoEntrega() : "—");
            row.createCell(10).setCellValue(f.getFechaEntrega() != null ? f.getFechaEntrega().toString() : "—");
            for (int i = 0; i <= 10; i++) row.getCell(i).setCellStyle(dataStyle);
        }

        for (int i = 0; i <= 10; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}

// ─── EXCEL SALDOS PENDIENTES ──────────────────────────────────
public byte[] exportarSaldosPendientes(List<FichaClinica> fichas) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("Saldos Pendientes");

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(IndexedColors.WHITE.getIndex());
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Título
        Row titulo = sheet.createRow(0);
        Cell celdaTitulo = titulo.createCell(0);
        celdaTitulo.setCellValue("Mi Óptica — Saldos Pendientes");
        CellStyle tituloStyle = workbook.createCellStyle();
        Font tituloFont = workbook.createFont();
        tituloFont.setBold(true);
        tituloFont.setFontHeightInPoints((short) 13);
        tituloStyle.setFont(tituloFont);
        celdaTitulo.setCellStyle(tituloStyle);

        // Encabezados
        Row header = sheet.createRow(2);
        String[] columnas = {"#", "N° Ficha", "Fecha", "Cliente", "Teléfono",
                             "Total Q", "Abono Q", "Saldo Q",
                             "Días Pendiente", "Estado Entrega"};
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos
        int rowNum = 3;
        int num = 1;
        for (FichaClinica f : fichas) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(num++);
            row.createCell(1).setCellValue(f.getIdFicha());
            row.createCell(2).setCellValue(f.getFecha() != null ? f.getFecha().toString() : "");
            row.createCell(3).setCellValue(f.getCliente().getNombre());
            row.createCell(4).setCellValue(f.getCliente().getTelefono() != null ? f.getCliente().getTelefono() : "—");
            row.createCell(5).setCellValue(formatoQuetzales(f.getTotal()));
            row.createCell(6).setCellValue(formatoQuetzales(f.getAbono()));
            row.createCell(7).setCellValue(formatoQuetzales(f.getSaldo()));
            row.createCell(8).setCellValue(f.getDiasPendiente());
            row.createCell(9).setCellValue(f.getEstadoEntrega() != null ? f.getEstadoEntrega() : "—");
            for (int i = 0; i <= 9; i++) row.getCell(i).setCellStyle(dataStyle);
        }

        for (int i = 0; i <= 9; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}

}
