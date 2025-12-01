package com.miempresa.bivlotectatecnica.bd;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.TextView;
import android.widget.ImageButton;
import android.widget.Toast;
import android.util.Log;

import com.miempresa.bivlotectatecnica.R;
import com.miempresa.bivlotectatecnica.bd.UserProjectContract.InformeEntry;
import com.miempresa.bivlotectatecnica.bd.UserProjectContract;

/**
 * Adaptador personalizado para mostrar los Informes en la ListView.
 * Une los datos del Informe, Componente, y Técnico de la consulta JOIN.
 */
public class ReportCursorAdapter extends CursorAdapter {

    private static final String TAG = "ReportCursorAdapter";
    private final String userRole;
    private final OnReportActionListener listener; // 🚨 Nueva Interfaz

    // 🚨 INTERFAZ DE LISTENER para comunicar el clic del botón de vuelta a la Activity
    public interface OnReportActionListener {
        void onGeneratePdfClicked(long reportId);
    }

    // 🚨 CONSTRUCTOR CORREGIDO: Acepta el rol y el listener (4 argumentos)
    public ReportCursorAdapter(Context context, Cursor c, String userRole, OnReportActionListener listener) {
        super(context, c, 0 /* flags */);
        this.userRole = userRole;
        this.listener = listener;
    }

    // Constructor simplificado (para mantener compatibilidad si se usa con solo 2 argumentos)
    public ReportCursorAdapter(Context context, Cursor c) {
        this(context, c, UserProjectContract.ROLE_TECNICO, null);
    }


    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        // Infla el layout de la fila list_item_report.xml
        // Usamos el layout del contexto que ya tiene el R
        return LayoutInflater.from(context).inflate(R.layout.list_item_report, parent, false);
    }

    /**
     * Vincula los datos del Cursor a las vistas de la fila.
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // IDs de las vistas en list_item_report.xml
        TextView tvTitle = view.findViewById(R.id.tv_report_title);
        TextView tvSubtitle = view.findViewById(R.id.tv_report_subtitle);
        TextView tvTechnician = view.findViewById(R.id.tv_report_technician);
        ImageButton btnGeneratePdfItem = view.findViewById(R.id.btn_generate_pdf_item); // Botón de PDF en la fila

        try {
            // 1. Obtener índices de columna (usando los ALIAS de UserProjectDatabase.getReportsList())
            int reportIdIndex = cursor.getColumnIndexOrThrow(UserProjectContract.InformeEntry._ID);
            int componenteNombreIndex = cursor.getColumnIndexOrThrow("componente_nombre");
            int codigoInventarioIndex = cursor.getColumnIndexOrThrow(UserProjectContract.ComponenteEntry.COLUMN_CODIGO_INVENTARIO);
            int tipoAccionIndex = cursor.getColumnIndexOrThrow(InformeEntry.COLUMN_ACTION_TYPE);
            int tecnicoNombreIndex = cursor.getColumnIndexOrThrow(UserProjectContract.ALIAS_TECNICO_NOMBRE);
            int dateLoggedIndex = cursor.getColumnIndexOrThrow(InformeEntry.COLUMN_DATE_LOGGED);

            // Obtener el ID de la fila para las acciones
            final long reportId = cursor.getLong(reportIdIndex);

            // 2. Extraer valores
            String componenteNombre = cursor.getString(componenteNombreIndex);
            String codigoInventario = cursor.getString(codigoInventarioIndex);
            String tipoAccion = cursor.getString(tipoAccionIndex);
            String tecnicoNombre = cursor.getString(tecnicoNombreIndex);
            String dateLogged = cursor.getString(dateLoggedIndex);

            // 3. Formatear los textos
            String fullTitle = componenteNombre + " (" + codigoInventario + ")";
            String subtitle = tipoAccion + " - " + dateLogged;

            // 4. Asignar los valores a las vistas
            tvTitle.setText(fullTitle);
            tvSubtitle.setText(subtitle);
            tvTechnician.setText("Por: " + tecnicoNombre);

            // 5. LÓGICA DEL BOTÓN DE PDF (Solo visible para Admin)
            if (UserProjectContract.ROLE_ADMIN.equals(userRole)) {
                btnGeneratePdfItem.setVisibility(View.VISIBLE);

                // 🚨 Asignar el Listener que llama a la Activity a través de la interfaz
                btnGeneratePdfItem.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onGeneratePdfClicked(reportId);
                    }
                });
            } else {
                btnGeneratePdfItem.setVisibility(View.GONE);
                // Asegurarse de que no haya listener residual
                btnGeneratePdfItem.setOnClickListener(null);
            }

        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Error al enlazar vistas: Columna no encontrada. Verifica los alias del DAO.", e);
            tvTitle.setText("ERROR AL CARGAR DATOS");
            tvSubtitle.setText("Error: " + e.getMessage());
            btnGeneratePdfItem.setVisibility(View.GONE);
        }
    }
}