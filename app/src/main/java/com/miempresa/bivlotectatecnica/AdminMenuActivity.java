package com.miempresa.bivlotectatecnica;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.miempresa.bivlotectatecnica.bd.UserProjectContract;
import com.miempresa.bivlotectatecnica.SessionManager;

/**
 * Menú principal exclusivo para el Administrador (Layout 1 ADMIN).
 */
public class AdminMenuActivity extends AppCompatActivity {

    private SessionManager session;
    private Button btnReports;
    private Button btnTechnicians;
    private Button btnLogout; // 🚨 Botón de Logout

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_menu);

        session = new SessionManager(this);

        // --- 1. Verificación de Seguridad y Sesión ---
        if (!session.isLoggedIn() || !UserProjectContract.ROLE_ADMIN.equals(session.getUserRole())) {
            Toast.makeText(this, "Acceso denegado. Rol no autorizado.", Toast.LENGTH_LONG).show();
            session.logoutUser();
            Intent loginIntent = new Intent(this, LoginActivity.class);
            startActivity(loginIntent);
            finish();
            return;
        }

        // --- 2. Inicialización de Vistas y Listeners ---

        // Botones de Navegación
        btnReports = findViewById(R.id.card_reports);
        btnTechnicians = findViewById(R.id.card_technicians);

        // 🚨 Inicialización del botón de cerrar sesión (ID: btn_logout)
        btnLogout = findViewById(R.id.btn_logout);

        // 1. Botón para Informes
        btnReports.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMenuActivity.this, ReportListActivity.class);
            startActivity(intent);
        });

        // 2. Botón para Gestión de Técnicos
        btnTechnicians.setOnClickListener(v -> {
            Intent intent = new Intent(AdminMenuActivity.this, TechnicianListActivity.class);
            startActivity(intent);
        });

        // 🚨 Lógica de Cierre de Sesión
        btnLogout.setOnClickListener(v -> {
            session.logoutUser(); // Limpia SharedPreferences
            Toast.makeText(this, "Sesión cerrada.", Toast.LENGTH_SHORT).show();
            Intent loginIntent = new Intent(AdminMenuActivity.this, LoginActivity.class);
            startActivity(loginIntent);
            finish(); // Cierra el menú y vuelve al login
        });
    }
}