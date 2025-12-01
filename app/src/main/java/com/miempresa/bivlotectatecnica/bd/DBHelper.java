package com.miempresa.bivlotectatecnica.bd;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.miempresa.bivlotectatecnica.bd.UserProjectContract.TallerEntry;
import com.miempresa.bivlotectatecnica.bd.UserProjectContract.UserEntry;
import com.miempresa.bivlotectatecnica.bd.UserProjectContract.ComponenteEntry;
import com.miempresa.bivlotectatecnica.bd.UserProjectContract.InformeEntry;
import com.miempresa.bivlotectatecnica.bd.UserProjectContract;

/**
 * Clase que extiende SQLiteOpenHelper. Gestiona la creación de tablas,
 * la actualización de la base de datos y la inserción de datos iniciales.
 */
public class DBHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = UserProjectContract.DATABASE_NAME;
    private static final int DATABASE_VERSION = UserProjectContract.DATABASE_VERSION;
    private static final String TAG = "DBHelper";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Tabla TALLER
        final String SQL_CREATE_TALLER_TABLE = "CREATE TABLE " + TallerEntry.TABLE_NAME + " ("
                + TallerEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + TallerEntry.COLUMN_NAME + " TEXT UNIQUE NOT NULL);";
        db.execSQL(SQL_CREATE_TALLER_TABLE);

        // 2. Tabla USUARIO (Depende de TALLER)
        final String SQL_CREATE_USER_TABLE = "CREATE TABLE " + UserEntry.TABLE_NAME + " ("
                + UserEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + UserEntry.COLUMN_USERNAME + " TEXT UNIQUE NOT NULL,"
                + UserEntry.COLUMN_PASSWORD_HASH + " TEXT NOT NULL,"
                + UserEntry.COLUMN_NAME + " TEXT NOT NULL,"
                + UserEntry.COLUMN_ROLE + " TEXT DEFAULT '" + UserProjectContract.ROLE_TECNICO + "',"
                + UserEntry.COLUMN_IS_ACTIVE + " INTEGER DEFAULT 1,"
                + UserEntry.COLUMN_WORKSHOP_ID + " INTEGER,"
                + " FOREIGN KEY (" + UserEntry.COLUMN_WORKSHOP_ID + ") REFERENCES " +
                TallerEntry.TABLE_NAME + "(" + TallerEntry._ID + ") ON DELETE SET NULL);";
        db.execSQL(SQL_CREATE_USER_TABLE);

        // 3. Tabla COMPONENTE (Depende de TALLER)
        final String SQL_CREATE_COMPONENTE_TABLE = "CREATE TABLE " + ComponenteEntry.TABLE_NAME + " ("
                + ComponenteEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + ComponenteEntry.COLUMN_NAME + " TEXT NOT NULL,"
                + ComponenteEntry.COLUMN_CODIGO_INVENTARIO + " TEXT UNIQUE NOT NULL,"
                + ComponenteEntry.COLUMN_WORKSHOP_ID + " INTEGER NOT NULL,"
                + " FOREIGN KEY (" + ComponenteEntry.COLUMN_WORKSHOP_ID + ") REFERENCES " +
                TallerEntry.TABLE_NAME + "(" + TallerEntry._ID + ") ON DELETE CASCADE);";
        db.execSQL(SQL_CREATE_COMPONENTE_TABLE);

        // 4. Tabla INFORME (Depende de USER y COMPONENTE)
        final String SQL_CREATE_INFORME_TABLE = "CREATE TABLE " + InformeEntry.TABLE_NAME + " ("
                + InformeEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + InformeEntry.COLUMN_ACTION_TYPE + " TEXT NOT NULL,"
                + InformeEntry.COLUMN_STEP_NUMBER + " INTEGER NOT NULL,"
                + InformeEntry.COLUMN_DESCRIPTION + " TEXT,"
                + InformeEntry.COLUMN_PHOTO_URI + " TEXT NOT NULL,"
                + InformeEntry.COLUMN_DATE_LOGGED + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + InformeEntry.COLUMN_USER_ID + " INTEGER NOT NULL,"
                + InformeEntry.COLUMN_COMPONENTE_ID + " INTEGER NOT NULL,"
                + " UNIQUE (" + InformeEntry.COLUMN_COMPONENTE_ID + ", " + InformeEntry.COLUMN_STEP_NUMBER + "),"
                + " FOREIGN KEY (" + InformeEntry.COLUMN_USER_ID + ") REFERENCES " +
                UserEntry.TABLE_NAME + "(" + UserEntry._ID + ") ON DELETE CASCADE,"
                + " FOREIGN KEY (" + InformeEntry.COLUMN_COMPONENTE_ID + ") REFERENCES " +
                ComponenteEntry.TABLE_NAME + "(" + ComponenteEntry._ID + ") ON DELETE CASCADE);";
        db.execSQL(SQL_CREATE_INFORME_TABLE);

        // Insertar datos iniciales de talleres y usuarios
        insertInitialData(db);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TAG, "Actualizando la base de datos de la versión " + oldVersion + " a " + newVersion);
        // Eliminar tablas en orden inverso a la creación para respetar FK
        db.execSQL("DROP TABLE IF EXISTS " + InformeEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ComponenteEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UserEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + TallerEntry.TABLE_NAME);
        onCreate(db);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    /**
     * Método para insertar los talleres y un usuario administrador inicial.
     */
    private void insertInitialData(SQLiteDatabase db) {
        // 1. Insertar Talleres
        ContentValues cv = new ContentValues();
        cv.put(TallerEntry.COLUMN_NAME, "Electromecánico");
        long tallerElectroId = db.insert(TallerEntry.TABLE_NAME, null, cv);
        cv.clear();
        cv.put(TallerEntry.COLUMN_NAME, "Eléctrico");
        db.insert(TallerEntry.TABLE_NAME, null, cv);
        cv.clear();
        cv.put(TallerEntry.COLUMN_NAME, "Hidráulico");
        db.insert(TallerEntry.TABLE_NAME, null, cv);

        // 2. Insertar Administrador Inicial (User: admin / Pass: 123456)
        ContentValues adminValues = new ContentValues();
        adminValues.put(UserEntry.COLUMN_USERNAME, "admin");
        adminValues.put(UserEntry.COLUMN_PASSWORD_HASH, "e10adc3949ba59abbe56e057f20f883e"); // Hash de '123456'
        adminValues.put(UserEntry.COLUMN_NAME, "Administrador General");
        adminValues.put(UserEntry.COLUMN_ROLE, UserProjectContract.ROLE_ADMIN);
        db.insert(UserEntry.TABLE_NAME, null, adminValues);

        // 3. Insertar Técnico de prueba (User: tecnico / Pass: 123456)
        ContentValues tecnicoValues = new ContentValues();
        tecnicoValues.put(UserEntry.COLUMN_USERNAME, "tecnico");
        tecnicoValues.put(UserEntry.COLUMN_PASSWORD_HASH, "e10adc3949ba59abbe56e057f20f883e"); // Hash de '123456'
        tecnicoValues.put(UserEntry.COLUMN_NAME, "Juan Técnico Pérez");
        tecnicoValues.put(UserEntry.COLUMN_ROLE, UserProjectContract.ROLE_TECNICO);
        tecnicoValues.put(UserEntry.COLUMN_WORKSHOP_ID, tallerElectroId);
        db.insert(UserEntry.TABLE_NAME, null, tecnicoValues);
    }
}