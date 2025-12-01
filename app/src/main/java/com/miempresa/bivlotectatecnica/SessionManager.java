package com.miempresa.bivlotectatecnica;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

/**
 * Gestiona la sesión del usuario (ID, Rol, Taller) utilizando SharedPreferences.
 * Esto mantiene el estado de acceso después del Login.
 */
public class SessionManager {

    private static final String PREF_NAME = "UserSession";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_ROLE = "userRole";
    private static final String KEY_WORKSHOP_NAME = "workshopName";
    // 🚨 CLAVE AÑADIDA: Clave para el ID numérico del taller
    private static final String KEY_WORKSHOP_ID = "workshopId";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";

    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;

    public SessionManager(Context context) {
        this.context = context;
        // Nombre del archivo de SharedPreferences
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
    }

    /**
     * Crea la sesión de login del usuario.
     * @param id ID del trabajador.
     * @param role Rol (Admin/Tecnico).
     * @param workshopName Nombre legible del taller.
     * @param workshopId ID numérico del taller (AÑADIDO).
     */
    public void createLoginSession(long id, String role, String workshopName, long workshopId) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putLong(KEY_USER_ID, id);
        editor.putString(KEY_USER_ROLE, role);
        editor.putString(KEY_WORKSHOP_NAME, workshopName);
        editor.putLong(KEY_WORKSHOP_ID, workshopId); // 🚨 Guardar el ID del taller
        editor.commit();
    }

    /**
     * Verifica si el usuario ha iniciado sesión.
     * @return true si el usuario está logueado, false en caso contrario.
     */
    public boolean isLoggedIn() {
        return pref.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    /**
     * Cierra la sesión del usuario.
     */
    public void logoutUser() {
        editor.clear();
        editor.commit();
        // Opcional: Redirigir al usuario a la pantalla de Login después de cerrar sesión
        // NOTA: Para que esto funcione, necesitarás crear la clase LoginActivity.
        /*
        Intent i = new Intent(context, LoginActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
        */
    }

    /**
     * Obtiene el ID del usuario actual.
     */
    public long getUserId() {
        return pref.getLong(KEY_USER_ID, -1);
    }

    /**
     * Obtiene el Rol del usuario (Admin/Tecnico).
     */
    public String getUserRole() {
        return pref.getString(KEY_USER_ROLE, "");
    }

    /**
     * Obtiene el Nombre del Taller.
     */
    public String getWorkshopName() {
        return pref.getString(KEY_WORKSHOP_NAME, "Sin Asignar");
    }

    /**
     * 🚨 NUEVO MÉTODO: Obtiene el ID numérico del Taller.
     */
    public long getWorkshopId() {
        return pref.getLong(KEY_WORKSHOP_ID, -1);
    }
}