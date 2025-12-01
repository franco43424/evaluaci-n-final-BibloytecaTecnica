package com.miempresa.bivlotectatecnica;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri; // Importación necesaria
import android.os.Environment;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider; // Importación necesaria

import com.miempresa.bivlotectatecnica.bd.UserProjectContract;
import com.miempresa.bivlotectatecnica.bd.UserProjectDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Utilidad para generar un archivo PDF a partir de un Informe de Procedimiento.
 * Este PDF se guarda en el directorio de Documentos/Descargas del dispositivo.
 */
public class ReportPDFGenerator {

    private static final String TAG = "ReportPDFGenerator";
    private final Context context;
    private final UserProjectDatabase db;

    // Dimensiones del PDF (A4)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;

    public ReportPDFGenerator(Context context, UserProjectDatabase db) {
        this.context = context;
        this.db = db;
    }

    /**
     * Genera el documento PDF para un informe específico.
     * @param reportId ID del informe a documentar.
     */
    public boolean generate(long reportId) {

        // Asumimos que los métodos getReportDetails y getStepsForReport están implementados en el DAO
        Cursor reportCursor = db.getReportDetails(reportId);
        Cursor stepsCursor = db.getStepsForReport(reportId);

        if (reportCursor == null || reportCursor.getCount() == 0) {
            Toast.makeText(context, "Error: Informe no encontrado.", Toast.LENGTH_LONG).show();
            if (reportCursor != null) reportCursor.close();
            return false;
        }

        PdfDocument document = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create();
        PdfDocument.Page page = document.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        int y = 50; // Posición inicial vertical
        int x = 40; // Margen izquierdo

        reportCursor.moveToFirst();

        // --- 2. EXTRAER DATOS DEL ENCABEZADO ---

        String title = reportCursor.getString(reportCursor.getColumnIndexOrThrow(UserProjectContract.ComponenteEntry.COLUMN_NAME));
        String inventoryCode = reportCursor.getString(reportCursor.getColumnIndexOrThrow(UserProjectContract.ComponenteEntry.COLUMN_CODIGO_INVENTARIO));
        String actionType = reportCursor.getString(reportCursor.getColumnIndexOrThrow(UserProjectContract.InformeEntry.COLUMN_ACTION_TYPE));
        String technicianName = reportCursor.getString(reportCursor.getColumnIndexOrThrow(UserProjectContract.ALIAS_TECNICO_NOMBRE));
        String workshopName = reportCursor.getString(reportCursor.getColumnIndexOrThrow(UserProjectContract.ALIAS_TALLER_NOMBRE));
        String dateLogged = reportCursor.getString(reportCursor.getColumnIndexOrThrow(UserProjectContract.InformeEntry.COLUMN_DATE_LOGGED));

        // --- 3. DIBUJAR ENCABEZADO ---

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setTextSize(24f);
        canvas.drawText("INFORME DE PROCEDIMIENTO", x, y, paint);
        y += 40;

        paint.setTextSize(16f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        canvas.drawText("Componente: " + title + " (" + inventoryCode + ")", x, y, paint); y += 20;
        canvas.drawText("Acción: " + actionType, x, y, paint); y += 20;
        canvas.drawText("Técnico Responsable: " + technicianName + " (" + workshopName + ")", x, y, paint); y += 20;
        canvas.drawText("Fecha: " + dateLogged.substring(0, 10), x, y, paint);
        y += 40;

        // --- 4. SECCIÓN DE PASOS FOTOGRÁFICOS ---

        paint.setTextSize(20f);
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        canvas.drawText("SECUENCIA DE PASOS", x, y, paint); y += 30;

        if (stepsCursor != null && stepsCursor.moveToFirst()) {
            int stepNumberIndex = stepsCursor.getColumnIndexOrThrow(UserProjectContract.InformeEntry.COLUMN_STEP_NUMBER);
            int descriptionIndex = stepsCursor.getColumnIndexOrThrow(UserProjectContract.InformeEntry.COLUMN_DESCRIPTION);
            int photoUriIndex = stepsCursor.getColumnIndexOrThrow(UserProjectContract.InformeEntry.COLUMN_PHOTO_URI);

            do {
                if (y > PAGE_HEIGHT - 100) { // Si queda poco espacio, creamos una nueva página
                    document.finishPage(page);
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 50; // Reiniciar Y
                }

                int stepNum = stepsCursor.getInt(stepNumberIndex);
                String desc = stepsCursor.getString(descriptionIndex);
                String uriPath = stepsCursor.getString(photoUriIndex);

                // Título del Paso
                paint.setTextSize(14f);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD_ITALIC));
                canvas.drawText(String.format(Locale.getDefault(), "PASO %d:", stepNum), x, y, paint); y += 18;

                // Descripción
                paint.setTextSize(12f);
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                canvas.drawText("Descripción: " + (TextUtils.isEmpty(desc) ? "Sin descripción registrada" : desc), x, y, paint); y += 15;

                // Ruta de la Foto (Indicador)
                canvas.drawText("Foto Adjunta: " + (TextUtils.isEmpty(uriPath) ? "No" : "Sí"), x, y, paint); y += 30;

            } while (stepsCursor.moveToNext());
        } else {
            paint.setTextSize(12f);
            canvas.drawText("No se registraron pasos para este informe.", x, y, paint);
        }

        // --- 5. FINALIZAR Y GUARDAR EN DESCARGAS ---

        document.finishPage(page);

        // Define la ruta de guardado (Directorio de Documentos/Descargas)
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS); // Usamos DOCUMENTS o DOWNLOADS
        String fileName = String.format(Locale.getDefault(), "INFORME_%s_%s.pdf", inventoryCode, actionType);
        File file = new File(downloadsDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            // 🚨 APERTURA AUTOMÁTICA DEL PDF
            openPdfFile(file);

            Toast.makeText(context, "PDF guardado en: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error al guardar PDF: " + e.getMessage());
            Toast.makeText(context, "Error al guardar el PDF. Asegure el permiso de almacenamiento.", Toast.LENGTH_LONG).show();
            return false;
        } finally {
            reportCursor.close();
            if (stepsCursor != null) stepsCursor.close();
        }
    }

    /**
     * Lanza un Intent para que el usuario pueda ver o compartir el archivo PDF recién creado.
     */
    private void openPdfFile(File file) {
        if (!file.exists()) {
            Toast.makeText(context, "El archivo PDF no se encuentra.", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // 1. Obtener una URI segura (Content URI) usando FileProvider
            Uri pdfUri = FileProvider.getUriForFile(context,
                    context.getApplicationContext().getPackageName() + ".fileprovider",
                    file);

            // 2. Crear el Intent de vista
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");

            // 3. Conceder permisos temporales de lectura a otras aplicaciones
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            // 4. Asegurarse de que el Intent se lanza desde un contexto de Activity
            if (context instanceof android.app.Activity) {
                context.startActivity(intent);
            } else {
                // Si se llama desde un contexto que no es Activity (como un ApplicationContext),
                // se necesita la flag NEW_TASK.
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }

        } catch (IllegalArgumentException e) {
            // Esto ocurre si el FileProvider no está correctamente configurado en el Manifest
            Log.e(TAG, "Error de FileProvider al abrir PDF.", e);
            Toast.makeText(context, "Error de configuración: FileProvider no configurado.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "No se encontró una aplicación para abrir el PDF.", Toast.LENGTH_LONG).show();
        }
    }
}