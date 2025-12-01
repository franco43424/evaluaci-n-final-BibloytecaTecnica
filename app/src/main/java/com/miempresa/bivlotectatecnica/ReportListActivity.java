package com.miempresa.bivlotectatecnica;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.miempresa.bivlotectatecnica.bd.UserProjectContract;
import com.miempresa.bivlotectatecnica.bd.UserProjectDatabase;
import com.miempresa.bivlotectatecnica.bd.ReportCursorAdapter;
import com.miempresa.bivlotectatecnica.SessionManager;

/**
 * Muestra la lista de informes de procedimiento. La lista se filtra por rol.
 * Implementa la interfaz para manejar el clic del botón Generar PDF desde la fila.
 */
public class ReportListActivity extends AppCompatActivity implements ReportCursorAdapter.OnReportActionListener {

    private UserProjectDatabase db;
    private SessionManager session;
    private ReportCursorAdapter adapter;
    private ListView reportsListView;
    private Button fabNewReport;
    private Button btnLogout;
    private TextView headerTitle;
    private TextView emptyReportsView;

    private static final String TAG = "ReportListActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_report_list);

        db = new UserProjectDatabase(this);
        session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Sesión expirada. Inicie sesión.", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        // Inicializar Vistas (IDs del layout activity_report_list.xml)
        reportsListView = findViewById(R.id.list_reports);
        fabNewReport = findViewById(R.id.fab_new_report);
        headerTitle = findViewById(R.id.header_title);
        btnLogout = findViewById(R.id.btn_logout);
        emptyReportsView = findViewById(R.id.empty_reports_view);
        reportsListView.setEmptyView(emptyReportsView);

        // 1. Configurar el título y visibilidad de los botones según el rol
        configureViewByRole();

        // 2. Configurar el adaptador (inicia vacío)
        // 🚨 Pasamos el rol y 'this' (la interfaz de escucha) al adaptador
        adapter = new ReportCursorAdapter(this, null, session.getUserRole(), this);
        reportsListView.setAdapter(adapter);

        // 3. Listener para EDICIÓN/VISUALIZACIÓN (Clic Simple - Abre la edición)
        reportsListView.setOnItemClickListener((parent, view, position, id) -> {
            // El clic simple abre la actividad de edición/pasos directamente.
            startEditingReport(id);
        });

        // 4. Listener para CREAR NUEVO INFORME
        fabNewReport.setOnClickListener(v -> {
            Intent intent = new Intent(ReportListActivity.this, StepRegisterActivity.class);
            startActivity(intent);
        });

        // 5. Lógica de Cierre de Sesión
        btnLogout.setOnClickListener(v -> {
            session.logoutUser(); // Limpia SharedPreferences
            Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(ReportListActivity.this, LoginActivity.class);
            loginIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(loginIntent);
            finish();
        });

        // 🚨 ELIMINADO: El clic largo ahora es redundante ya que el clic simple abre la edición.
        reportsListView.setOnItemLongClickListener(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadReportsList(); // Recargar la lista cada vez que vuelve a la actividad (después de crear/editar)
    }

    private void configureViewByRole() {
        String role = session.getUserRole();
        String workshopName = session.getWorkshopName();

        // Configuración de la visibilidad y el título
        if (UserProjectContract.ROLE_ADMIN.equals(role)) {
            headerTitle.setText("ADMIN: Informes Globales");
            fabNewReport.setVisibility(View.GONE);
            // NOTA: El botón de PDF se hace visible DENTRO del adaptador.
        } else {
            // Técnico
            headerTitle.setText("Taller " + workshopName + " | Mis Informes");
            fabNewReport.setVisibility(View.VISIBLE);
        }
    }

    private void loadReportsList() {
        long userId = session.getUserId();
        String role = session.getUserRole();

        // Obtener el Cursor filtrado por rol y ID
        Cursor cursor = db.getReportsList(userId, role);

        if (cursor != null) {
            // Intercambiar el cursor en el adaptador
            adapter.changeCursor(cursor);
        } else {
            Toast.makeText(this, "No se pudieron cargar los informes.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Lógica para iniciar la edición (utilizada por el clic simple en la fila).
     */
    private void startEditingReport(long reportId) {
        Intent intent = new Intent(ReportListActivity.this, StepRegisterActivity.class);
        intent.putExtra("REPORT_ID", reportId);
        startActivity(intent);
    }

    // -------------------------------------------------------------------
    // 🚨 IMPLEMENTACIÓN DE LA INTERFAZ DE ACCIÓN DEL ADAPTADOR (PDF)
    // -------------------------------------------------------------------

    /**
     * Llama al generador de PDF (Método de la interfaz ReportCursorAdapter.OnReportActionListener).
     */
    @Override
    public void onGeneratePdfClicked(long reportId) {
        // Lógica de generación de PDF para el Administrador
        ReportPDFGenerator generator = new ReportPDFGenerator(this, db);
        boolean success = generator.generate(reportId);

        if (success) {
            Toast.makeText(this, "PDF generado con éxito para el Informe #" + reportId, Toast.LENGTH_LONG).show();
        }
    }
}