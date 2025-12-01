package com.miempresa.bivlotectatecnica;

import android.os.Bundle;
import android.widget.Button; // 💡 Importación correcta para el botón
import android.widget.ListView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.miempresa.bivlotectatecnica.bd.UserProjectContract;
import com.miempresa.bivlotectatecnica.bd.UserProjectDatabase;
// Importación del adaptador para el listado de técnicos (se asume que existe)
// import com.miempresa.bivlotectatecnica.bd.TechnicianCursorAdapter;
// Si el layout usa FloatingActionButton, se debería usar:
// import com.google.android.material.floatingactionbutton.FloatingActionButton;

/**
 * Actividad para mostrar la lista de técnicos y permitir al administrador gestionar usuarios.
 * (Layout 3 ADMIN).
 */
public class TechnicianListActivity extends AppCompatActivity {

    private UserProjectDatabase db;
    private SessionManager session;
    private ListView techniciansListView;
    private Button btnAddTechnician; // 💡 CORREGIDO: Usamos Button para evitar error de casteo

    // private TechnicianCursorAdapter adapter; // Adaptador para los técnicos

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Asume que el layout se llama activity_tech_list
        setContentView(R.layout.activity_tech_list);

        db = new UserProjectDatabase(this);
        session = new SessionManager(this);

        // 1. Seguridad estricta: Solo Admins pueden acceder a esta vista
        if (!UserProjectContract.ROLE_ADMIN.equals(session.getUserRole())) {
            Toast.makeText(this, "Acceso Prohibido. Solo Administradores.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 2. Inicializar Vistas
        techniciansListView = findViewById(R.id.list_technicians);
        // 💡 CORREGIDO: findViewById ahora busca un Button (o MaterialButton)
        btnAddTechnician = findViewById(R.id.btn_add_technician);

        // 3. Configurar el adaptador (se implementará en onResume)
        // loadTechniciansList();

        // 4. Listener para Agregar Nuevo Técnico (Pendiente: Crear NewTechnicianActivity.java)
        btnAddTechnician.setOnClickListener(v -> {
            // Intent intent = new Intent(TechnicianListActivity.this, NewTechnicianActivity.class);
            // startActivity(intent);
            Toast.makeText(this, "Funcionalidad de añadir técnico pendiente.", Toast.LENGTH_SHORT).show();
        });

        // NOTA: La lógica de clic en la lista (para editar/eliminar) se agrega aquí.
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTechniciansList();
    }

    /**
     * Carga todos los usuarios (técnicos y admins) para mostrarlos en la lista.
     */
    private void loadTechniciansList() {
        // 🚨 Pendiente: Implementar db.getAllUsers() en UserProjectDatabase.java
        // Cursor cursor = db.getAllUsers();

        // if (adapter != null) {
        //     adapter.changeCursor(cursor);
        //     // Simulación de carga real:
        // }

        Toast.makeText(this, "Cargando lista de técnicos...", Toast.LENGTH_SHORT).show();
    }
}