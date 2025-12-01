package com.miempresa.bivlotectatecnica.bd;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.util.Log;

// Importaciones de clases internas del contrato
import com.miempresa.bivlotectatecnica.bd.UserProjectContract.TallerEntry;
import com.miempresa.bivlotectatecnica.bd.UserProjectContract.UserEntry;
import com.miempresa.bivlotectatecnica.bd.UserProjectContract.ComponenteEntry;
import com.miempresa.bivlotectatecnica.bd.UserProjectContract.InformeEntry;
import com.miempresa.bivlotectatecnica.bd.UserProjectContract;

import java.util.ArrayList;
import java.util.List;

/**
 * Data Access Object (DAO) para la gestión de las operaciones CRUD de la base de datos.
 * Contiene la lógica de negocio para el Login y la manipulación de Informes.
 */
public class UserProjectDatabase {

    private final DBHelper dbHelper;
    private static final String TAG = "UserProjectDatabase";

    public UserProjectDatabase(Context context) {
        this.dbHelper = new DBHelper(context);
    }

    // ===================================================================
    // LÓGICA DE LOGIN Y AUTENTICACIÓN
    // ===================================================================

    /**
     * Verifica las credenciales de usuario y devuelve un Cursor con la información del trabajador.
     */
    public Cursor checkUserCredentials(String username, String passwordHash) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                "U." + UserEntry._ID + " AS user_pk_id", // PK ID del usuario
                "U." + UserEntry.COLUMN_NAME + " AS user_name_full", // Nombre completo del usuario
                "U." + UserEntry.COLUMN_ROLE, // Rol
                "U." + UserEntry.COLUMN_WORKSHOP_ID, // ID numérico del taller
                // Alias para obtener el nombre del taller del usuario logueado (Columna T.nombre)
                "T." + TallerEntry.COLUMN_NAME + " AS " + UserProjectContract.ALIAS_TALLER_NOMBRE
        };

        String tables = UserEntry.TABLE_NAME + " U " +
                "LEFT JOIN " + TallerEntry.TABLE_NAME + " T ON U." + UserEntry.COLUMN_WORKSHOP_ID + " = T." + TallerEntry._ID;

        String selection = "U." + UserEntry.COLUMN_USERNAME + " = ? AND " +
                "U." + UserEntry.COLUMN_PASSWORD_HASH + " = ? AND " +
                "U." + UserEntry.COLUMN_IS_ACTIVE + " = 1";

        String[] selectionArgs = { username, passwordHash };

        Cursor cursor = db.query(
                tables, projection, selection, selectionArgs, null, null, null
        );

        if (cursor != null && cursor.moveToFirst()) {
            return cursor; // Devuelve el cursor en la primera posición
        }

        if (cursor != null) {
            cursor.close();
        }
        return null;
    }

    // ===================================================================
    // OPERACIONES CRUD DE COMPONENTES E INFORMES
    // ===================================================================

    /**
     * Busca un componente por su código de inventario. Si no existe, lo inserta.
     */
    public long getOrInsertComponenteId(String name, String inventoryCode, long workshopId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        long componenteId = -1;
        Cursor cursor = null;

        try {
            // 1. Intentar buscar por código de inventario
            String[] projection = { ComponenteEntry._ID };
            String selection = ComponenteEntry.COLUMN_CODIGO_INVENTARIO + " = ?";
            String[] selectionArgs = { inventoryCode };

            cursor = db.query(
                    ComponenteEntry.TABLE_NAME,
                    projection,
                    selection,
                    selectionArgs,
                    null, null, null
            );

            if (cursor != null && cursor.moveToFirst()) {
                // Componente encontrado
                componenteId = cursor.getLong(cursor.getColumnIndexOrThrow(ComponenteEntry._ID));
            } else {
                // 2. Componente no encontrado, INSERTAR
                ContentValues values = new ContentValues();
                values.put(ComponenteEntry.COLUMN_NAME, name);
                values.put(ComponenteEntry.COLUMN_CODIGO_INVENTARIO, inventoryCode);
                values.put(ComponenteEntry.COLUMN_WORKSHOP_ID, workshopId);

                componenteId = db.insert(ComponenteEntry.TABLE_NAME, null, values);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error en getOrInsertComponenteId", e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return componenteId;
    }

    /**
     * Inserta un nuevo paso en el informe (un registro fotográfico).
     */
    public long insertReportStep(long componenteId, int stepNumber, String description, String photoUri, long userId, String actionType) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();

        values.put(InformeEntry.COLUMN_COMPONENTE_ID, componenteId);
        values.put(InformeEntry.COLUMN_STEP_NUMBER, stepNumber);
        values.put(InformeEntry.COLUMN_DESCRIPTION, description);
        values.put(InformeEntry.COLUMN_PHOTO_URI, photoUri);
        values.put(InformeEntry.COLUMN_USER_ID, userId);
        values.put(InformeEntry.COLUMN_ACTION_TYPE, actionType);

        long id = db.insert(InformeEntry.TABLE_NAME, null, values);
        return id;
    }

    // ===================================================================
    // CONSULTA DE INFORMES (VISTAS PRINCIPALES)
    // ===================================================================

    /**
     * Obtiene la lista de informes con detalles del componente y técnico (filtrado por rol).
     */
    public Cursor getReportsList(long userId, String role) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // Tablas: Informes (I), Componentes (C), Usuarios (U), Talleres (T)
        qb.setTables(
                InformeEntry.TABLE_NAME + " I " +
                        "INNER JOIN " + ComponenteEntry.TABLE_NAME + " C ON I." + InformeEntry.COLUMN_COMPONENTE_ID + " = C." + ComponenteEntry._ID +
                        " INNER JOIN " + UserEntry.TABLE_NAME + " U ON I." + InformeEntry.COLUMN_USER_ID + " = U." + UserEntry._ID +
                        " INNER JOIN " + TallerEntry.TABLE_NAME + " T ON U." + UserEntry.COLUMN_WORKSHOP_ID + " = T." + TallerEntry._ID
        );

        String orderBy = "I." + InformeEntry.COLUMN_DATE_LOGGED + " DESC";

        String[] projectionIn = {
                "I." + InformeEntry._ID,
                "C." + ComponenteEntry.COLUMN_NAME + " AS componente_nombre",
                "C." + ComponenteEntry.COLUMN_CODIGO_INVENTARIO,
                "I." + InformeEntry.COLUMN_ACTION_TYPE,
                "U." + UserEntry.COLUMN_NAME + " AS " + UserProjectContract.ALIAS_TECNICO_NOMBRE,
                "T." + TallerEntry.COLUMN_NAME + " AS " + UserProjectContract.ALIAS_TALLER_NOMBRE,
                "I." + InformeEntry.COLUMN_DATE_LOGGED
        };

        String selection = null;
        String[] selectionArgs = null;

        // Lógica de filtrado: Si no es Admin, solo muestra los informes que él creó
        if (UserProjectContract.ROLE_TECNICO.equals(role)) {
            selection = "I." + InformeEntry.COLUMN_USER_ID + " = ?";
            selectionArgs = new String[]{ String.valueOf(userId) };
        }

        return qb.query(
                db, projectionIn, selection, selectionArgs, null, null, orderBy
        );
    }

    /**
     * 🚨 NUEVO: Obtiene los detalles de un informe específico para el PDF.
     */
    public Cursor getReportDetails(long reportId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        qb.setTables(
                InformeEntry.TABLE_NAME + " I " +
                        "INNER JOIN " + ComponenteEntry.TABLE_NAME + " C ON I." + InformeEntry.COLUMN_COMPONENTE_ID + " = C." + ComponenteEntry._ID +
                        " INNER JOIN " + UserEntry.TABLE_NAME + " U ON I." + InformeEntry.COLUMN_USER_ID + " = U." + UserEntry._ID +
                        " INNER JOIN " + TallerEntry.TABLE_NAME + " T ON U." + UserEntry.COLUMN_WORKSHOP_ID + " = T." + TallerEntry._ID
        );

        String[] projectionIn = {
                "I." + InformeEntry._ID,
                "I." + InformeEntry.COLUMN_ACTION_TYPE,
                "I." + InformeEntry.COLUMN_DATE_LOGGED,
                "C." + ComponenteEntry.COLUMN_NAME, // Nombre del componente
                "C." + ComponenteEntry.COLUMN_CODIGO_INVENTARIO, // Código
                "U." + UserEntry.COLUMN_NAME + " AS " + UserProjectContract.ALIAS_TECNICO_NOMBRE,
                "T." + TallerEntry.COLUMN_NAME + " AS " + UserProjectContract.ALIAS_TALLER_NOMBRE
        };

        String selection = "I." + InformeEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(reportId) };

        Cursor cursor = qb.query(db, projectionIn, selection, selectionArgs, null, null, null);

        // Es importante cerrar el cursor en la utilidad de PDF, no aquí.
        return cursor;
    }

    /**
     * 🚨 NUEVO: Obtiene todos los pasos (registros fotográficos) de un informe.
     */
    public Cursor getStepsForReport(long reportId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String[] projection = {
                InformeEntry._ID,
                InformeEntry.COLUMN_STEP_NUMBER,
                InformeEntry.COLUMN_DESCRIPTION,
                InformeEntry.COLUMN_PHOTO_URI,
                InformeEntry.COLUMN_DATE_LOGGED
        };

        String selection = InformeEntry.COLUMN_COMPONENTE_ID + " = ?";
        String[] selectionArgs = { String.valueOf(reportId) };

        // Ordenar por número de paso
        return db.query(
                InformeEntry.TABLE_NAME,
                projection,
                selection,
                selectionArgs,
                null, null,
                InformeEntry.COLUMN_STEP_NUMBER + " ASC"
        );
    }

    /**
     * Obtiene todos los usuarios (trabajadores) y su taller asociado.
     */
    public Cursor getAllUsers() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        String tables = UserEntry.TABLE_NAME + " U " +
                "LEFT JOIN " + TallerEntry.TABLE_NAME + " T ON U." + UserEntry.COLUMN_WORKSHOP_ID + " = T." + TallerEntry._ID;

        // Columnas a devolver
        String[] projection = {
                "U." + UserEntry._ID,
                "U." + UserEntry.COLUMN_NAME,
                "U." + UserEntry.COLUMN_ROLE,
                "T." + TallerEntry.COLUMN_NAME + " AS taller_nombre"
        };

        return db.query(
                tables,
                projection,
                null, // No hay filtros
                null,
                null, null,
                UserEntry.COLUMN_NAME + " ASC"
        );
    }

    // --- Métodos de Ayuda CRUD (Pendientes) ---
    // Implementación de: updateStep, deleteReport, y Gestión de Técnicos
}