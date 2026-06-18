package com.mioptica.service;

import com.mioptica.dto.VentaDetalleDTO;
import com.mioptica.model.FichaClinica;
import com.mioptica.model.Inventario;
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

import org.apache.poi.util.IOUtils;
import org.springframework.core.io.ClassPathResource;
import java.io.InputStream;


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
    // ─── HELPER: Membrete con logo ────────────────────────────────
private int insertarMembrete(XSSFWorkbook workbook, Sheet sheet, 
                              String tituloReporte, LocalDate fi, LocalDate ff) throws Exception {
    // Logo
    try {
        InputStream logoStream = new ClassPathResource("static/img/logo optica.jpeg").getInputStream();
        byte[] logoBytes = IOUtils.toByteArray(logoStream);
        int logoIdx = workbook.addPicture(logoBytes, XSSFWorkbook.PICTURE_TYPE_JPEG);
        Drawing<?> drawing = sheet.createDrawingPatriarch();
        ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
        anchor.setCol1(0); anchor.setRow1(0);
        anchor.setCol2(2); anchor.setRow2(3);
        drawing.createPicture(anchor, logoIdx);
    } catch (Exception e) {
        System.err.println("No se pudo cargar el logo: " + e.getMessage());
    }

    // Estilo título
    CellStyle estiloNombre = workbook.createCellStyle();
    Font fNombre = workbook.createFont();
    fNombre.setBold(true);
    fNombre.setFontHeightInPoints((short) 16);
    fNombre.setColor(IndexedColors.DARK_TEAL.getIndex());
    estiloNombre.setFont(fNombre);

    CellStyle estiloSlogan = workbook.createCellStyle();
    Font fSlogan = workbook.createFont();
    fSlogan.setFontHeightInPoints((short) 10);
    fSlogan.setItalic(true);
    estiloSlogan.setFont(fSlogan);

    CellStyle estiloTitulo = workbook.createCellStyle();
    Font fTitulo = workbook.createFont();
    fTitulo.setBold(true);
    fTitulo.setFontHeightInPoints((short) 12);
    estiloTitulo.setFont(fTitulo);

    // Filas del membrete
    Row r0 = sheet.createRow(0);
    Cell cNombre = r0.createCell(2);
    cNombre.setCellValue("Mi Óptica");
    cNombre.setCellStyle(estiloNombre);

    Row r1 = sheet.createRow(1);
    Cell cSlogan = r1.createCell(2);
    cSlogan.setCellValue("Mi Mejor Visión");
    cSlogan.setCellStyle(estiloSlogan);

    Row r2 = sheet.createRow(2);
    Cell cTitulo = r2.createCell(2);
    cTitulo.setCellValue(tituloReporte);
    cTitulo.setCellStyle(estiloTitulo);

    Row r3 = sheet.createRow(3);
    if (fi != null && ff != null) {
        r3.createCell(2).setCellValue("Período: " + fi + " al " + ff);
    }

    // Línea separadora vacía
    sheet.createRow(4);

    return 5; // primera fila disponible para datos
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
            int rowNum = insertarMembrete(workbook, sheet, "Reporte de Ventas", fi, ff);
            Row header = sheet.createRow(rowNum++);
            String[] columnas = {"N° Factura", "Fecha", "Vendedor", "Categoría",
                                "Producto / Modelo", "Cantidad", "Total Q", "Forma de Pago"};
            for (int i = 0; i < columnas.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(columnas[i]);
                cell.setCellStyle(headerStyle);
            }
            
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
        int rowNum = insertarMembrete(workbook, sheet, "Fichas Clínicas", fi, ff);
        Row header = sheet.createRow(rowNum++);
        String[] columnas = {"#", "N° Ficha", "Fecha", "NIT", "Cliente",
                            "Optometrista", "Total Q", "Abono Q", "Saldo Q",
                            "Estado Entrega", "Fecha Entrega"};
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(headerStyle);
        }

        // Datos
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
       int rowNum = insertarMembrete(workbook, sheet, "Saldos Pendientes", null, null);
       Row header = sheet.createRow(rowNum++);
        String[] columnas = {"#", "N° Ficha", "Fecha", "Cliente", "Teléfono",
                            "Total Q", "Abono Q", "Saldo Q", "Días Pendiente", "Estado Entrega"};
        for (int i = 0; i < columnas.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(columnas[i]);
            cell.setCellStyle(headerStyle);
        }
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
// ─── EXCEL VENTAS POR CLIENTE ─────────────────────────────────
public byte[] exportarVentasPorCliente(List<Object[]> rows, LocalDate fi, LocalDate ff) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("Ventas por Cliente");

        

        CellStyle clienteStyle = workbook.createCellStyle();
        Font clienteFont = workbook.createFont();
        clienteFont.setBold(true);
        clienteFont.setColor(IndexedColors.WHITE.getIndex());
        clienteStyle.setFont(clienteFont);
        clienteStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        clienteStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        CellStyle subtotalStyle = workbook.createCellStyle();
        Font subtotalFont = workbook.createFont();
        subtotalFont.setBold(true);
        subtotalStyle.setFont(subtotalFont);

        // Título
        int rowNum = insertarMembrete(workbook, sheet, "Ventas por cliente", fi, ff);
        String clienteActual = null;
        int numFila = 1;
        BigDecimal subtotalCliente = BigDecimal.ZERO;

        for (Object[] row : rows) {
            String  cliente      = row[0] != null ? row[0].toString() : "Consumidor Final";
            String  fecha        = row[1] != null ? row[1].toString() : "";
            String  factura      = row[2] != null ? row[2].toString() : "";
            String  idProducto   = row[3] != null ? row[3].toString() : "";
            String  descripcion  = row[4] != null ? row[4].toString() : "";
            BigDecimal precio    = row[5] instanceof Number ? new BigDecimal(row[5].toString()) : BigDecimal.ZERO;
            BigDecimal cantidad  = row[6] instanceof Number ? new BigDecimal(row[6].toString()) : BigDecimal.ZERO;
            BigDecimal subtotal  = row[7] instanceof Number ? new BigDecimal(row[7].toString()) : BigDecimal.ZERO;

            // ── Nuevo cliente ──
            if (!cliente.equals(clienteActual)) {
                // Subtotal del cliente anterior
                if (clienteActual != null) {
                    Row subRow = sheet.createRow(rowNum++);
                    Cell subCell = subRow.createCell(6);
                    subCell.setCellValue("TOTAL " + clienteActual + ": " + formatoQuetzales(subtotalCliente));
                    subCell.setCellStyle(subtotalStyle);
                    rowNum++; // espacio
                }

                // Encabezado del nuevo cliente
                Row clienteRow = sheet.createRow(rowNum++);
                Cell clienteCell = clienteRow.createCell(0);
                clienteCell.setCellValue(cliente.toUpperCase());
                clienteCell.setCellStyle(clienteStyle);

                // Encabezados de columnas
                Row headerRow = sheet.createRow(rowNum++);
                String[] cols = {"#", "Fecha", "N° Factura", "Id Producto", "Descripción", "Precio Unit.", "Cantidad", "Total"};
                for (int i = 0; i < cols.length; i++) {
                    Cell hCell = headerRow.createCell(i);
                    hCell.setCellValue(cols[i]);
                    hCell.setCellStyle(headerStyle);
                }

                clienteActual = cliente;
                subtotalCliente = BigDecimal.ZERO;
                numFila = 1;
            }

            // Fila de datos
            Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(numFila++);
            dataRow.createCell(1).setCellValue(fecha);
            dataRow.createCell(2).setCellValue(factura);
            dataRow.createCell(3).setCellValue(idProducto);
            dataRow.createCell(4).setCellValue(descripcion);
            dataRow.createCell(5).setCellValue(formatoQuetzales(precio));
            dataRow.createCell(6).setCellValue(cantidad.toString());
            dataRow.createCell(7).setCellValue(formatoQuetzales(subtotal));
            for (int i = 0; i <= 7; i++) dataRow.getCell(i).setCellStyle(dataStyle);

            subtotalCliente = subtotalCliente.add(subtotal);
        }

        // Subtotal del último cliente
        if (clienteActual != null) {
            Row subRow = sheet.createRow(rowNum);
            Cell subCell = subRow.createCell(6);
            subCell.setCellValue("TOTAL " + clienteActual + ": " + formatoQuetzales(subtotalCliente));
            subCell.setCellStyle(subtotalStyle);
        }

        for (int i = 0; i <= 7; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
// ─── EXCEL PRODUCTOS ARMAZONES Y LENTES ──────────────────────
public byte[] exportarProductosArmazonesLentes(List<Inventario> inventarios) throws Exception {
    try (XSSFWorkbook workbook = new XSSFWorkbook()) {
        Sheet sheet = workbook.createSheet("Productos");

        
        CellStyle grupoStyle = workbook.createCellStyle();
        Font grupoFont = workbook.createFont();
        grupoFont.setBold(true);
        grupoFont.setColor(IndexedColors.WHITE.getIndex());
        grupoStyle.setFont(grupoFont);
        grupoStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        grupoStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

        CellStyle dataStyle = workbook.createCellStyle();
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        // Título
        int rowNum = insertarMembrete(workbook, sheet, "Armazones y Lentes", null, null);
        
        


        String tipoActual = null;
        int numFila = 1;

        for (Inventario inv : inventarios) {
            String tipo = inv.getProducto().getTipoProducto();
            String tipoLabel = "ARMAZON".equals(tipo) ? "ARMAZONES" : "LENTES";

            // ── Nuevo grupo ──
            if (!tipoLabel.equals(tipoActual)) {
                if (tipoActual != null) rowNum++; // espacio entre grupos

                Row grupoRow = sheet.createRow(rowNum++);
                Cell grupoCell = grupoRow.createCell(0);
                grupoCell.setCellValue(tipoLabel);
                grupoCell.setCellStyle(grupoStyle);

                Row headerRow = sheet.createRow(rowNum++);
                String[] cols = "ARMAZONES".equals(tipoLabel)
                        ? new String[]{"#", "Producto", "Material", "Existencia", "Precio Costo", "Precio Venta", "Margen Q"}
                        : new String[]{"#", "Producto", "Tratamiento", "Existencia", "Precio Costo", "Precio Venta", "Margen Q"};
                for (int i = 0; i < cols.length; i++) {
                    Cell hCell = headerRow.createCell(i);
                    hCell.setCellValue(cols[i]);
                    hCell.setCellStyle(headerStyle);
                }

                tipoActual = tipoLabel;
                numFila = 1;
            }

            // Tratamiento — solo aplica para lentes
           String tratamiento = "—";
           if ("LENTE".equals(tipo) && inv.getProducto().getTratamientoLente() != null) {
               tratamiento = inv.getProducto().getTratamientoLente().getNombre();
           } else if ("ARMAZON".equals(tipo) && inv.getProducto().getMaterialArmazon() != null) {
               tratamiento = inv.getProducto().getMaterialArmazon();
           }

            BigDecimal costo      = inv.getCosto() != null ? inv.getCosto() : BigDecimal.ZERO;
            BigDecimal precioVenta = inv.getPrecioVenta() != null ? inv.getPrecioVenta() : BigDecimal.ZERO;
            BigDecimal margen     = precioVenta.subtract(costo);

            Row dataRow = sheet.createRow(rowNum++);
            dataRow.createCell(0).setCellValue(numFila++);
            dataRow.createCell(1).setCellValue(inv.getProducto().getDetalle());
            dataRow.createCell(2).setCellValue(tratamiento);
            dataRow.createCell(3).setCellValue(inv.getExistencia() != null ? inv.getExistencia().doubleValue() : 0);
            dataRow.createCell(4).setCellValue(formatoQuetzales(costo));
            dataRow.createCell(5).setCellValue(formatoQuetzales(precioVenta));
            dataRow.createCell(6).setCellValue(formatoQuetzales(margen));
            for (int i = 0; i <= 6; i++) dataRow.getCell(i).setCellStyle(dataStyle);
        }

        for (int i = 0; i <= 6; i++) sheet.autoSizeColumn(i);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
}
