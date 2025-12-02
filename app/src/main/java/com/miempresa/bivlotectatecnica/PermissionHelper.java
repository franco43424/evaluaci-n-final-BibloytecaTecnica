package com.miempresa.bivlotectatecnica;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

/**
 * Clase estática para manejar la verificación y solicitud de permisos en tiempo de ejecución
 * (Cámara y Almacenamiento), adaptándose a las reglas de permisos de Android moderno.
 */
public class PermissionHelper {

    public static final int REQUEST_CODE_CAMERA_GALLERY = 100;

    /**
     * Verifica y solicita los permisos necesarios para la cámara y la galería.
     * @return true si los permisos ya están concedidos.
     */
    public static boolean checkAndRequestPermissions(Activity activity) {
        String[] requiredPermissions;

        // --- Lógica de permisos por versión de Android ---

        // API 33+ (Android 13+): Se requiere READ_MEDIA_IMAGES
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requiredPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_MEDIA_IMAGES
            };
        }
        // API 29 a 32 (Android 10 a 12): Permite FileProvider sin WRITE_EXTERNAL,
        // pero incluimos READ para galería.
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            requiredPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
        // API 28 y anteriores: Se necesita WRITE_EXTERNAL_STORAGE
        else {
            requiredPermissions = new String[]{
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };
        }

        boolean allGranted = true;
        for (String permission : requiredPermissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (!allGranted) {
            ActivityCompat.requestPermissions(activity, requiredPermissions, REQUEST_CODE_CAMERA_GALLERY);
            return false;
        }
        return true;
    }
}