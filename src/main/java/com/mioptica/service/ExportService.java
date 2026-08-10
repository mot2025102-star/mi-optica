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
                                 String tituloReporte, LocalDate fi, LocalDate ff,
                                 String usuarioGenerador, String nombreSucursal) throws Exception {
        // Estilos
        CellStyle estiloNombre = workbook.createCellStyle();
        Font fNombre = workbook.createFont();
        fNombre.setBold(true);
        fNombre.setFontHeightInPoints((short) 16);
        estiloNombre.setFont(fNombre);

        CellStyle estiloSlogan = workbook.createCellStyle();
        Font fSlogan = workbook.createFont();
        fSlogan.setFontHeightInPoints((short) 11);
        fSlogan.setItalic(true);
        estiloSlogan.setFont(fSlogan);

        CellStyle estiloNormal = workbook.createCellStyle();
        Font fNormal = workbook.createFont();
        fNormal.setFontHeightInPoints((short) 11);
        estiloNormal.setFont(fNormal);

        CellStyle estiloTitulo = workbook.createCellStyle();
        Font fTitulo = workbook.createFont();
        fTitulo.setBold(true);
        fTitulo.setFontHeightInPoints((short) 14);
        estiloTitulo.setFont(fTitulo);

        int rowIdx = 0;

        // El logo es el título, así que no escribimos "Mi Óptica" en texto.
        // Espacio para el logo (Filas 0 y 1)
        rowIdx += 2; 

        Row r2 = sheet.createRow(rowIdx++);
        Cell c2 = r2.createCell(0);
        c2.setCellValue("Dirección: 6A Avenida 3-81, Cdad. de Guatemala 01001, Guatemala");
        c2.setCellStyle(estiloNormal);

        Row r3 = sheet.createRow(rowIdx++);
        Cell c3 = r3.createCell(0);
        c3.setCellValue("Teléfono: 4599-4217 | Correo: mioptica2020@gmail.com");
        c3.setCellStyle(estiloNormal);

        rowIdx++; // Línea vacía

        Row r5 = sheet.createRow(rowIdx++);
        Cell c5 = r5.createCell(0);
        c5.setCellValue(tituloReporte);
        c5.setCellStyle(estiloTitulo);

        if (fi != null && ff != null) {
            Row r6 = sheet.createRow(rowIdx++);
            r6.createCell(0).setCellValue("Período: " + fi + " al " + ff);
        }

        Row r7 = sheet.createRow(rowIdx++);
        java.time.format.DateTimeFormatter dtf = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        r7.createCell(0).setCellValue("Fecha y Hora de Generación: " + java.time.LocalDateTime.now().format(dtf));

        Row r8 = sheet.createRow(rowIdx++);
        r8.createCell(0).setCellValue("Generado por: " + (usuarioGenerador != null ? usuarioGenerador : "Sistema"));

        Row r9 = sheet.createRow(rowIdx++);
        r9.createCell(0).setCellValue("Sucursal: " + (nombreSucursal != null ? nombreSucursal : "Todas"));

        rowIdx++; // Línea vacía separadora

        // Insertar Logo en A1:C2
        try {
            InputStream logoStream = new ClassPathResource("templates/logo/Logo123.png").getInputStream();
            byte[] logoBytes = IOUtils.toByteArray(logoStream);
            int logoIdx = workbook.addPicture(logoBytes, XSSFWorkbook.PICTURE_TYPE_PNG);
            Drawing<?> drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = workbook.getCreationHelper().createClientAnchor();
            
            // FILAS: Inicia en fila 0, termina en fila 3 (ocupa 3 filas: 0, 1 y 2)
            anchor.setRow1(0); 
            anchor.setRow2(2); 
            
            anchor.setCol1(0); 
            anchor.setCol2(1); //donde empieza y termina 
            
            drawing.createPicture(anchor, logoIdx);
        } catch (Exception e) {
            System.err.println("No se pudo cargar el logo: " + e.getMessage());
        }

        return rowIdx;
    }

    // ─── EXPORTAR EXCEL ───────────────────────────────────────────
    public byte[] exportarExcel(List<VentaDetalleDTO> ventas, LocalDate fi, LocalDate ff, String usuario, String sucursal) throws Exception {

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
            int rowNum = insertarMembrete(workbook, sheet, "Reporte de Ventas", fi, ff, usuario, sucursal);
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
            sheet.setColumnWidth(0, 27 * 256); // Fija el ancho de la columna A automáticamente

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    // ─── EXPORTAR PDF ─────────────────────────────────────────────
    public byte[] exportarPdf(List<VentaDetalleDTO> ventas, LocalDate fi, LocalDate ff, String usuario, String sucursal) throws Exception {

        Document document = new Document(PageSize.A4.rotate());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfWriter.getInstance(document, out);
        document.open();

        // Agregar Logo
        try {
            InputStream logoStream = new ClassPathResource("templates/logo/Logo123.png").getInputStream();
            com.itextpdf.text.Image logo = com.itextpdf.text.Image.getInstance(IOUtils.toByteArray(logoStream));
            logo.scaleToFit(200, 60);
            logo.setAlignment(Element.ALIGN_LEFT);
            document.add(logo);
        } catch (Exception e) {
            System.err.println("No se pudo cargar el logo en PDF: " + e.getMessage());
        }

        // Agregar Dirección y Teléfono
        com.itextpdf.text.Font textFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.NORMAL, BaseColor.BLACK);
        Paragraph address = new Paragraph("Dirección: 6A Avenida 3-81, Cdad. de Guatemala 01001, Guatemala\nTeléfono: 4599-4217 | Correo: mioptica2020@gmail.com", textFont);
        address.setAlignment(Element.ALIGN_LEFT);
        address.setSpacingAfter(15);
        document.add(address);

        // Título del Reporte
        com.itextpdf.text.Font tituloFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD, BaseColor.BLACK);
        Paragraph titulo = new Paragraph("Reporte de Ventas", tituloFont);
        titulo.setAlignment(Element.ALIGN_LEFT);
        document.add(titulo);

        // Metadatos
        String fechaGen = java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        String metaText = "Período: " + fi + " al " + ff + "\n" +
                          "Fecha y Hora de Generación: " + fechaGen + "\n" +
                          "Generado por: " + (usuario != null ? usuario : "Sistema") + "\n" +
                          "Sucursal: " + (sucursal != null ? sucursal : "Todas las sucursales");
        
        Paragraph meta = new Paragraph(metaText, textFont);
        meta.setAlignment(Element.ALIGN_LEFT);
        meta.setSpacingAfter(15);
        document.add(meta);

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
public byte[] exportarFichasClinicas(List<FichaClinica> fichas, LocalDate fi, LocalDate ff, String usuario, String sucursal) throws Exception {
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
        int rowNum = insertarMembrete(workbook, sheet, "Fichas Clínicas", fi, ff, usuario, sucursal);
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
        sheet.setColumnWidth(0, 27 * 256); // Fija el ancho de la columna A automáticamente

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}

// ─── EXCEL SALDOS PENDIENTES ──────────────────────────────────
public byte[] exportarSaldosPendientes(List<FichaClinica> fichas, String usuario, String sucursal) throws Exception {
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
       int rowNum = insertarMembrete(workbook, sheet, "Saldos Pendientes", null, null, usuario, sucursal);
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
        sheet.setColumnWidth(0, 27 * 256); // Fija el ancho de la columna A automáticamente

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
// ─── EXCEL VENTAS POR CLIENTE ─────────────────────────────────
public byte[] exportarVentasPorCliente(List<Object[]> rows, LocalDate fi, LocalDate ff, String usuario, String sucursal) throws Exception {
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

        DataFormat format = workbook.createDataFormat();

        CellStyle dataStyleCurrency = workbook.createCellStyle();
        dataStyleCurrency.cloneStyleFrom(dataStyle);
        dataStyleCurrency.setDataFormat(format.getFormat("\"Q\" #,##0.00"));

        CellStyle subtotalStyle = workbook.createCellStyle();
        Font subtotalFont = workbook.createFont();
        subtotalFont.setBold(true);
        subtotalStyle.setFont(subtotalFont);
        subtotalStyle.setDataFormat(format.getFormat("\"Q\" #,##0.00"));

        CellStyle subtotalLabelStyle = workbook.createCellStyle();
        subtotalLabelStyle.setFont(subtotalFont);
        subtotalLabelStyle.setAlignment(HorizontalAlignment.RIGHT);

        // Título
        int rowNum = insertarMembrete(workbook, sheet, "Ventas por cliente", fi, ff, usuario, sucursal);
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
                    Cell subLabelCell = subRow.createCell(6);
                    subLabelCell.setCellValue("TOTAL " + clienteActual + ":");
                    subLabelCell.setCellStyle(subtotalLabelStyle);

                    Cell subCell = subRow.createCell(7);
                    subCell.setCellValue(subtotalCliente.doubleValue());
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

            Cell cellId = dataRow.createCell(3);
            try { cellId.setCellValue(Long.parseLong(idProducto)); } catch(Exception e) { cellId.setCellValue(idProducto); }

            dataRow.createCell(4).setCellValue(descripcion);

            Cell cellPrecio = dataRow.createCell(5);
            cellPrecio.setCellValue(precio.doubleValue());

            Cell cellCantidad = dataRow.createCell(6);
            cellCantidad.setCellValue(cantidad.doubleValue());

            Cell cellTotal = dataRow.createCell(7);
            cellTotal.setCellValue(subtotal.doubleValue());

            for (int i = 0; i <= 7; i++) {
                Cell c = dataRow.getCell(i);
                if (i == 5 || i == 7) {
                    c.setCellStyle(dataStyleCurrency);
                } else {
                    c.setCellStyle(dataStyle);
                }
            }

            subtotalCliente = subtotalCliente.add(subtotal);
        }

        // Subtotal del último cliente
        if (clienteActual != null) {
            Row subRow = sheet.createRow(rowNum);
            Cell subLabelCell = subRow.createCell(6);
            subLabelCell.setCellValue("TOTAL " + clienteActual + ":");
            subLabelCell.setCellStyle(subtotalLabelStyle);

            Cell subCell = subRow.createCell(7);
            subCell.setCellValue(subtotalCliente.doubleValue());
            subCell.setCellStyle(subtotalStyle);
        }

        for (int i = 0; i <= 7; i++) sheet.autoSizeColumn(i);
        sheet.setColumnWidth(0, 27 * 256); // Fija el ancho de la columna A automáticamente

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
// ─── EXCEL PRODUCTOS ARMAZONES Y LENTES ──────────────────────
public byte[] exportarProductosArmazonesLentes(List<Inventario> inventarios, String usuario, String sucursal) throws Exception {
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
        int rowNum = insertarMembrete(workbook, sheet, "Armazones y Lentes", null, null, usuario, sucursal);
        
        


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
        sheet.setColumnWidth(0, 27 * 256); // Fija el ancho de la columna A automáticamente

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
}
}
