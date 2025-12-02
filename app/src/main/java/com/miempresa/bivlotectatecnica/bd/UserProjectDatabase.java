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
        // Usa el paquete de DBHelper (asumimos que ya lo corrigió)
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

        // 🚨 CORRECCIÓN CLAVE: Prefijamos TODAS las columnas ambiguas con el alias de la tabla (U.)
        String[] projection = {
                "U." + UserEntry._ID + " AS user_pk_id", // PK ID del usuario
                "U." + UserEntry.COLUMN_NAME + " AS user_name_full", // Nombre completo del usuario
                "U." + UserEntry.COLUMN_ROLE, // Rol
                "U." + UserEntry.COLUMN_WORKSHOP_ID, // ID numérico del taller
                // Alias para obtener el nombre del taller del usuario logueado (Columna T.nombre)
                "T." + TallerEntry.COLUMN_NAME + " AS " + UserProjectContract.ALIAS_TALLER_NOMBRE
        };

        // Unir la tabla de usuarios con la tabla de talleres para obtener el nombre del taller
        String tables = UserEntry.TABLE_NAME + " U " +
                "LEFT JOIN " + TallerEntry.TABLE_NAME + " T ON U." + UserEntry.COLUMN_WORKSHOP_ID + " = T." + TallerEntry._ID;

        String selection = "U." + UserEntry.COLUMN_USERNAME + " = ? AND " +
                "U." + UserEntry.COLUMN_PASSWORD_HASH + " = ? AND " +
                "U." + UserEntry.COLUMN_IS_ACTIVE + " = 1"; // Solo usuarios activos

        String[] selectionArgs = { username, passwordHash };

        Cursor cursor = db.query(
                tables,
                projection,
                selection,
                selectionArgs,
                null,
                null,
                null
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

                // Insertar y obtener la nueva ID
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
     * Obtiene los detalles de un informe específico para el encabezado del PDF.
     * La consulta se basa en el ID (Primary Key) del primer paso/registro del informe.
     */
    public Cursor getReportDetails(long reportId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();

        // Tablas: Informes (I), Componentes (C), Usuarios (U), Talleres (T)
        qb.setTables(
                InformeEntry.TABLE_NAME + " I " +
                        "INNER JOIN " + ComponenteEntry.TABLE_NAME + " C ON I." + InformeEntry.COLUMN_COMPONENTE_ID + " = C." + ComponenteEntry._ID +
                        " INNER JOIN " + UserEntry.TABLE_NAME + " U ON I." + InformeEntry.COLUMN_USER_ID + " = U." + UserEntry._ID +
                        // Unimos a Taller a través del componente, no del usuario (para ser más robustos)
                        " LEFT JOIN " + TallerEntry.TABLE_NAME + " T ON C." + ComponenteEntry.COLUMN_WORKSHOP_ID + " = T." + TallerEntry._ID
        );

        // Columnas a devolver (incluyendo los alias necesarios para ReportPDFGenerator)
        String[] projectionIn = {
                "I." + InformeEntry.COLUMN_ACTION_TYPE,
                "I." + InformeEntry.COLUMN_DATE_LOGGED,
                "C." + ComponenteEntry.COLUMN_NAME, // Nombre del componente
                "C." + ComponenteEntry.COLUMN_CODIGO_INVENTARIO, // Código
                "U." + UserEntry.COLUMN_NAME + " AS " + UserProjectContract.ALIAS_TECNICO_NOMBRE, // Nombre del Técnico
                "T." + TallerEntry.COLUMN_NAME + " AS " + UserProjectContract.ALIAS_TALLER_NOMBRE // Nombre del Taller
        };

        // Seleccionar solo el registro inicial (Primary Key) del informe
        String selection = "I." + InformeEntry._ID + " = ?";
        String[] selectionArgs = { String.valueOf(reportId) };

        Cursor cursor = qb.query(db, projectionIn, selection, selectionArgs, null, null, InformeEntry.COLUMN_DATE_LOGGED + " ASC", "1"); // LIMIT 1

        return cursor;
    }

    /**
     * 🚨 CORRECCIÓN CRÍTICA: Obtiene todos los pasos (registros fotográficos) de un informe.
     * Utiliza una consulta en dos pasos para garantizar que se recuperan todos los registros
     * que pertenecen al mismo Componente y Tipo de Acción que el registro inicial (reportId).
     */
    public Cursor getStepsForReport(long reportId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor initialCursor = null;
        String componenteId = null;
        String actionType = null;

        // 1. Encontrar el COMPONENTE_ID y ACTION_TYPE usando el ID del primer registro (reportId)
        try {
            initialCursor = db.query(
                    InformeEntry.TABLE_NAME,
                    new String[]{InformeEntry.COLUMN_COMPONENTE_ID, InformeEntry.COLUMN_ACTION_TYPE},
                    InformeEntry._ID + " = ?",
                    new String[]{String.valueOf(reportId)},
                    null, null, null, "1" // Limitado a 1
            );

            if (initialCursor != null && initialCursor.moveToFirst()) {
                // Obtener los datos clave para el filtro
                componenteId = initialCursor.getString(initialCursor.getColumnIndexOrThrow(InformeEntry.COLUMN_COMPONENTE_ID));
                actionType = initialCursor.getString(initialCursor.getColumnIndexOrThrow(InformeEntry.COLUMN_ACTION_TYPE));
            } else {
                Log.w(TAG, "No se encontró el registro inicial del informe para determinar el grupo de pasos: ID " + reportId);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error al buscar el Componente ID y Action Type para el reporte: " + reportId, e);
            return null;
        } finally {
            if (initialCursor != null) {
                initialCursor.close();
            }
        }

        // 2. Si se encuentran los datos, consultar TODOS los pasos con ese COMPONENTE_ID y ACTION_TYPE
        if (componenteId != null && actionType != null) {
            String[] projection = {
                    InformeEntry._ID,
                    InformeEntry.COLUMN_STEP_NUMBER,
                    InformeEntry.COLUMN_DESCRIPTION,
                    InformeEntry.COLUMN_PHOTO_URI,
                    InformeEntry.COLUMN_DATE_LOGGED
            };

            // Filtrar por Componente ID y Tipo de Acción para agrupar todos los pasos del reporte lógico
            String selection = InformeEntry.COLUMN_COMPONENTE_ID + " = ? AND " + InformeEntry.COLUMN_ACTION_TYPE + " = ?";
            String[] selectionArgs = { componenteId, actionType };

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

        return null;
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
    // Implementación de: updateStep, deleteReport, etc.
}