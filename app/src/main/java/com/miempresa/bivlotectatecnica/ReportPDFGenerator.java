package com.miempresa.bivlotectatecnica;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap; // Importación necesaria
import android.graphics.BitmapFactory; // Importación necesaria
import android.graphics.Canvas;
import android.graphics.Matrix; // Importación necesaria para escalar
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Environment; // Aún para obtener getExternalStoragePublicDirectory aunque se use getExternalFilesDir
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.miempresa.bivlotectatecnica.bd.UserProjectContract;
import com.miempresa.bivlotectatecnica.bd.UserProjectDatabase;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream; // Importación necesaria para cargar imágenes
import java.util.Locale;

/**
 * Utilidad para generar un archivo PDF a partir de un Informe de Procedimiento.
 * Este PDF se guarda en un directorio privado de la aplicación.
 */
public class ReportPDFGenerator {

    private static final String TAG = "ReportPDFGenerator";
    private final Context context;
    private final UserProjectDatabase db;

    // Dimensiones del PDF (A4)
    private static final int PAGE_WIDTH = 595;
    private static final int PAGE_HEIGHT = 842;
    private static final int MARGIN_X = 40; // Margen izquierdo/derecho
    private static final int IMAGE_MAX_WIDTH = PAGE_WIDTH - (2 * MARGIN_X); // Ancho máximo de la imagen
    private static final int IMAGE_MAX_HEIGHT = 200; // Altura máxima para las imágenes

    public ReportPDFGenerator(Context context, UserProjectDatabase db) {
        this.context = context;
        this.db = db;
    }

    /**
     * Genera el documento PDF para un informe específico.
     * @param reportId ID del informe a documentar.
     */
    public boolean generate(long reportId) {

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
        int x = MARGIN_X; // Margen izquierdo

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
                // Verificar si necesitamos una nueva página antes de dibujar el paso
                // Consideramos espacio para texto y posible imagen
                if (y > PAGE_HEIGHT - (IMAGE_MAX_HEIGHT + 100)) {
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

                // 🚨 AÑADIR IMAGEN
                if (!TextUtils.isEmpty(uriPath)) {
                    try {
                        Uri imageUri = Uri.parse(uriPath);
                        InputStream imageStream = context.getContentResolver().openInputStream(imageUri);
                        Bitmap originalBitmap = BitmapFactory.decodeStream(imageStream);
                        if (imageStream != null) imageStream.close();

                        if (originalBitmap != null) {
                            // Escalar el bitmap para que quepa en el PDF
                            Bitmap scaledBitmap = getScaledBitmap(originalBitmap, IMAGE_MAX_WIDTH, IMAGE_MAX_HEIGHT);

                            // Verificar espacio nuevamente si la imagen es grande
                            if (y + scaledBitmap.getHeight() + 30 > PAGE_HEIGHT) {
                                document.finishPage(page);
                                page = document.startPage(pageInfo);
                                canvas = page.getCanvas();
                                y = 50; // Reiniciar Y
                            }

                            canvas.drawBitmap(scaledBitmap, x, y, paint);
                            y += scaledBitmap.getHeight() + 10; // Espacio después de la imagen
                            scaledBitmap.recycle(); // Liberar memoria del bitmap escalado
                            originalBitmap.recycle(); // Liberar memoria del bitmap original
                        } else {
                            canvas.drawText("Error al cargar imagen (Bitmap nulo).", x, y, paint); y += 15;
                        }
                    } catch (IOException | SecurityException e) {
                        Log.e(TAG, "Error al cargar o dibujar la imagen para el paso " + stepNum + ": " + e.getMessage(), e);
                        canvas.drawText("No se pudo cargar la imagen. Revise permisos: " + e.getMessage(), x, y, paint); y += 15;
                    }
                } else {
                    canvas.drawText("Foto Adjunta: No", x, y, paint); y += 15; // Si no hay foto
                }

                y += 20; // Espacio entre pasos
            } while (stepsCursor.moveToNext());
        } else {
            paint.setTextSize(12f);
            canvas.drawText("No se registraron pasos para este informe.", x, y, paint);
        }

        // --- 5. FINALIZAR Y GUARDAR EN DESCARGAS PÚBLICAS ---

        document.finishPage(page);

        // Directorio de almacenamiento PÚBLICO: carpeta 'Download'.
        // NOTA IMPORTANTE: Para usar esta ruta, la app debe tener el permiso WRITE_EXTERNAL_STORAGE
        // declarado en el Manifest y solicitado en tiempo de ejecución.
        // Además, el FileProvider debe configurarse con <external-storage-path>.
        File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!downloadsDir.exists()) {
            downloadsDir.mkdirs(); // Crear el directorio si no existe
        }

        String fileName = String.format(Locale.getDefault(), "INFORME_%s_%s.pdf", inventoryCode, actionType);
        File file = new File(downloadsDir, fileName);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            document.writeTo(fos);
            document.close();
            fos.close();

            openPdfFile(file);

            Toast.makeText(context, "PDF guardado en: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error al guardar PDF: " + e.getMessage());
            Toast.makeText(context, "Error al guardar el PDF.", Toast.LENGTH_LONG).show();
            return false;
        } finally {
            reportCursor.close();
            if (stepsCursor != null) stepsCursor.close();
        }
    }

    /**
     * Escala un Bitmap para que quepa dentro de las dimensiones máximas especificadas,
     * manteniendo la relación de aspecto.
     */
    private Bitmap getScaledBitmap(Bitmap bitmap, int maxWidth, int maxHeight) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();

        float scale = Math.min((float) maxWidth / width, (float) maxHeight / height);

        Matrix matrix = new Matrix();
        matrix.postScale(scale, scale);

        return Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
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
            Uri pdfUri = FileProvider.getUriForFile(context,
                    context.getApplicationContext().getPackageName() + ".fileprovider",
                    file);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(pdfUri, "application/pdf");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            if (context instanceof android.app.Activity) {
                context.startActivity(intent);
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error de FileProvider al abrir PDF (verifique configuración de paths en XML).", e);
            Toast.makeText(context, "Error de configuración al abrir PDF.", Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(context, "No se encontró una aplicación para abrir el PDF.", Toast.LENGTH_LONG).show();
        }
    }
}