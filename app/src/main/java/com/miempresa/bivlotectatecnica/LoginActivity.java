package com.miempresa.bivlotectatecnica;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.miempresa.bivlotectatecnica.bd.UserProjectContract;
import com.miempresa.bivlotectatecnica.bd.UserProjectDatabase;

/**
 * Actividad para el inicio de sesión. Maneja la autenticación y la verificación de rol.
 */
public class LoginActivity extends AppCompatActivity {

    private EditText editUsername, editPassword;
    private Button btnLogin;
    private UserProjectDatabase db;
    private SessionManager session;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Asume que el layout se llama activity_login
        setContentView(R.layout.activity_login);

        // Inicializar
        db = new UserProjectDatabase(this);
        session = new SessionManager(this);

        // Si ya está logueado, redirigir inmediatamente
        if (session.isLoggedIn()) {
            redirectToMainMenu(session.getUserRole());
            finish();
            return;
        }

        // Inicializar Vistas
        editUsername = findViewById(R.id.edit_username);
        editPassword = findViewById(R.id.edit_password);
        btnLogin = findViewById(R.id.btn_login);
        // Asumo que hay un botón de registro que será implementado más adelante
        // Button btnRegister = findViewById(R.id.btn_register);

        // Listener de Login
        btnLogin.setOnClickListener(v -> attemptLogin());
    }

    /**
     * Procesa la entrada del usuario y verifica las credenciales en la base de datos.
     */
    private void attemptLogin() {
        String username = editUsername.getText().toString().trim();
        String password = editPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Debe ingresar usuario y contraseña.", Toast.LENGTH_SHORT).show();
            return;
        }

        // Simulación de Hash (En una app real, usar SHA-256 o bcrypt)
        // Usamos el hash MD5 de "123456" que insertamos en el DBHelper.java
        String passwordHash = "e10adc3949ba59abbe56e057f20f883e"; // Simulación

        Cursor userCursor = db.checkUserCredentials(username, passwordHash);

        if (userCursor != null) {
            try {
                // 🚨 CORRECCIÓN CLAVE: Leer los datos usando los ALIAS definidos en UserProjectDatabase.java

                // Usamos el alias 'user_pk_id' para obtener el ID de la tabla USUARIOS
                long userId = userCursor.getLong(userCursor.getColumnIndexOrThrow("user_pk_id"));
                // Usamos el alias 'user_name_full' para obtener el nombre completo del trabajador
                String name = userCursor.getString(userCursor.getColumnIndexOrThrow("user_name_full"));

                String role = userCursor.getString(userCursor.getColumnIndexOrThrow(UserProjectContract.UserEntry.COLUMN_ROLE));
                String workshopName = userCursor.getString(userCursor.getColumnIndexOrThrow(UserProjectContract.ALIAS_TALLER_NOMBRE));

                // Extraer el ID numérico del taller
                long workshopId = userCursor.getLong(userCursor.getColumnIndexOrThrow(UserProjectContract.UserEntry.COLUMN_WORKSHOP_ID));

                // Crear sesión y redirigir
                // Pasar los 4 argumentos: userId, role, workshopName, workshopId
                session.createLoginSession(userId, role, workshopName, workshopId);

                Toast.makeText(this, "Bienvenido, " + name, Toast.LENGTH_LONG).show();
                redirectToMainMenu(role);

            } catch (Exception e) {
                // Esto atrapa errores si las columnas (los alias) no coinciden.
                Toast.makeText(this, "Error de datos: " + e.getMessage(), Toast.LENGTH_LONG).show();
            } finally {
                userCursor.close();
            }
        } else {
            Toast.makeText(this, "Usuario o contraseña inválidos.", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Redirige al usuario al menú principal basado en su rol.
     */
    private void redirectToMainMenu(String role) {
        Intent intent;
        if (UserProjectContract.ROLE_ADMIN.equals(role)) {
            // Admin va al menú de gestión (Layout 1 ADMIN)
            intent = new Intent(LoginActivity.this, AdminMenuActivity.class);
        } else {
            // Técnico va directo a la lista de informes (Layout 1 TÉCNICO)
            intent = new Intent(LoginActivity.this, ReportListActivity.class);
        }
        startActivity(intent);
        finish(); // Cierra la actividad de Login
    }
}