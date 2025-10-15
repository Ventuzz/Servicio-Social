package ambu.excel;

import javax.swing.JTable;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class CsvExporter {
    private CsvExporter() {}

    public static void exportJTableToCSV(JTable table, File file) throws IOException {
        try (OutputStream os = new FileOutputStream(file);
             OutputStreamWriter osw = new OutputStreamWriter(os, StandardCharsets.UTF_8);
             PrintWriter pw = new PrintWriter(osw)) {

            // BOM para que Excel (Windows) detecte UTF-8 y acentos
            os.write(new byte[] {(byte)0xEF,(byte)0xBB,(byte)0xBF});

            // Encabezados
            for (int c = 0; c < table.getColumnCount(); c++) {
                if (c > 0) pw.print(',');
                pw.print(csv(table.getColumnName(c)));
            }
            pw.println();

            // Filas visibles (respeta orden y filtros del JTable)
            for (int vr = 0; vr < table.getRowCount(); vr++) {
                for (int c = 0; c < table.getColumnCount(); c++) {
                    if (c > 0) pw.print(',');
                    Object v = table.getValueAt(vr, c);
                    pw.print(csv(format(v)));
                }
                pw.println();
            }
        }
    }

    private static String format(Object v) {
        if (v == null) return "";
        if (v instanceof Date) return new SimpleDateFormat("yyyy-MM-dd HH:mm").format((Date) v);
        return v.toString();
    }

    private static String csv(String s) {
        String q = s.replace("\"", "\"\"");
        boolean needs = q.contains(",") || q.contains("\"") || q.contains("\n") || q.contains("\r");
        return needs ? ("\"" + q + "\"") : q;
    }
}


