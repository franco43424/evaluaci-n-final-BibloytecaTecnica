package com.miempresa.bivlotectatecnica.bd;

import android.provider.BaseColumns;

/**
 * Contrato de la base de datos para el proyecto de registro fotográfico.
 * Define las tablas: TALLER, USUARIO, COMPONENTE, INFORME.
 */
public final class UserProjectContract {

    private UserProjectContract() {}

    public static final String DATABASE_NAME = "mecanico_log.db";
    public static final int DATABASE_VERSION = 1;

    // Constantes para Roles
    public static final String ROLE_ADMIN = "Admin";
    public static final String ROLE_TECNICO = "Tecnico";

    // Alias para uso en consultas JOIN
    public static final String ALIAS_TECNICO_NOMBRE = "tecnico_nombre";
    public static final String ALIAS_TALLER_NOMBRE = "taller_nombre";


    // -------------------------------------------------------------
    // TABLA TALLER (Workshop)
    public static abstract class TallerEntry implements BaseColumns {
        public static final String TABLE_NAME = "taller";
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_NAME = "nombre"; // Ej: "Electromecánico"
    }

    // -------------------------------------------------------------
    // TABLA USUARIO (Trabajador)
    public static abstract class UserEntry implements BaseColumns {
        public static final String TABLE_NAME = "users";
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_USERNAME = "username"; // Para Login
        public static final String COLUMN_PASSWORD_HASH = "password_hash";
        public static final String COLUMN_NAME = "nombre";
        public static final String COLUMN_ROLE = "rol"; // Admin, Tecnico
        public static final String COLUMN_IS_ACTIVE = "is_active"; // 1 o 0
        public static final String COLUMN_WORKSHOP_ID = "taller_id"; // FK a TallerEntry
    }

    // -------------------------------------------------------------
    // TABLA COMPONENTE (Catálogo de Maquinaria)
    public static abstract class ComponenteEntry implements BaseColumns {
        public static final String TABLE_NAME = "componentes";
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_NAME = "nombre"; // Ej: "Motor Trifásico"
        public static final String COLUMN_CODIGO_INVENTARIO = "codigo_inventario";
        public static final String COLUMN_WORKSHOP_ID = "taller_id"; // FK a TallerEntry
    }

    // -------------------------------------------------------------
    // TABLA INFORME (Registro Fotográfico/Paso)
    public static abstract class InformeEntry implements BaseColumns {
        public static final String TABLE_NAME = "informes";
        public static final String _ID = BaseColumns._ID;
        public static final String COLUMN_USER_ID = "user_id"; // FK
        public static final String COLUMN_COMPONENTE_ID = "componente_id"; // FK
        public static final String COLUMN_ACTION_TYPE = "tipo_accion"; // 'Armado' o 'Desarme'
        public static final String COLUMN_STEP_NUMBER = "numero_paso";
        public static final String COLUMN_DESCRIPTION = "descripcion";
        public static final String COLUMN_PHOTO_URI = "foto_uri";
        public static final String COLUMN_DATE_LOGGED = "date_logged"; // TIMESTAMP
    }
}