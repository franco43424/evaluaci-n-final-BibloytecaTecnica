package com.miempresa.bivlotectatecnica;

import androidx.appcompat.app.AppCompatActivity;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;

import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.miempresa.bivlotectatecnica.bd.UserProjectContract;
import com.miempresa.bivlotectatecnica.bd.UserProjectDatabase;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Actividad para registrar la secuencia de pasos (fotos y descripción) para un informe.
 */
public class StepRegisterActivity extends AppCompatActivity {

    private static final String TAG = "StepRegisterActivity";
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_IMAGE_PICK = 2;
    private static final int REQUEST_PERMISSION_CODE = PermissionHelper.REQUEST_CODE_CAMERA_GALLERY;

    private UserProjectDatabase db;
    private SessionManager session;
    private String currentPhotoPath; // Ruta temporal para la foto capturada
    private StepData currentPhotoStep; // El objeto StepData que solicitó la foto

    // UI Elements
    private TextView reportTitleHeader;
    private LinearLayout dynamicStepsContainer;
    private Button btnAddNextStep, btnFinalizeReport;

    // Estado del Informe
    private long currentReportId = -1;
    private long currentUserId;
    private long currentWorkshopId;
    private int nextStepNumber = 1; // Contador para el siguiente paso a crear

    // Datos del Informe Fijo (Componente, Inventario)
    private EditText editComponentName, editInventoryCode;
    private Button btnActionAssemble, btnActionDisassemble;
    private String selectedActionType = null;
    private long selectedComponentId = -1;

    // Lista para almacenar las referencias a los datos de los pasos dinámicos
    private final ArrayList<StepData> currentStepsData = new ArrayList<>();

    // Clase interna para manejar datos de cada paso dinámico
    private static class StepData {
        int stepNumber;
        EditText descriptionView;
        ImageView imageView;
        String photoUri; // URI de la foto
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_step_register);

        db = new UserProjectDatabase(this);
        session = new SessionManager(this);

        if (!session.isLoggedIn()) {
            Toast.makeText(this, "Sesión inválida.", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        currentUserId = session.getUserId();
        currentWorkshopId = session.getWorkshopId();


        // 1. Inicializar Vistas Fijas
        reportTitleHeader = findViewById(R.id.report_title_header);
        dynamicStepsContainer = findViewById(R.id.dynamic_steps_container);
        btnAddNextStep = findViewById(R.id.btn_add_another_step);
        btnFinalizeReport = findViewById(R.id.btn_finalize_report);

        editComponentName = findViewById(R.id.edit_component_name);
        editInventoryCode = findViewById(R.id.edit_inventory_code);
        btnActionAssemble = findViewById(R.id.btn_action_assemble);
        btnActionDisassemble = findViewById(R.id.btn_action_disassemble);

        // --- Manejo del Modo Edición/Creación ---
        long receivedReportId = getIntent().getLongExtra("REPORT_ID", -1);

        if (receivedReportId != -1) {
            currentReportId = receivedReportId;
            reportTitleHeader.setText(String.format(Locale.getDefault(), "Añadir Pasos al Informe #%d", currentReportId));
            // 🚨 PENDIENTE: loadExistingReportDetails(currentReportId);
        } else {
            reportTitleHeader.setText("Crear Nuevo Informe de Taller");
            addStepView();
        }

        // --- Listeners de Acción ---
        btnActionAssemble.setOnClickListener(v -> setActionType(v, "ARMADO"));
        btnActionDisassemble.setOnClickListener(v -> setActionType(v, "DESARME"));

        btnAddNextStep.setOnClickListener(v -> {
            addStepView();
        });

        btnFinalizeReport.setOnClickListener(v -> {
            saveReportAndFinalize();
        });

        // Verificar permisos al inicio
        PermissionHelper.checkAndRequestPermissions(this);
    }

    // ===================================================================
    // LÓGICA DE PERMISOS Y CÁMARA
    // ===================================================================

    /**
     * Lanza el Intent para capturar o seleccionar una foto.
     */
    private void startImageIntent(StepData stepData) {
        if (!PermissionHelper.checkAndRequestPermissions(this)) {
            Toast.makeText(this, "Permisos necesarios para cámara/almacenamiento.", Toast.LENGTH_SHORT).show();
            return;
        }

        currentPhotoStep = stepData;

        // Intent 1: Cámara
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try {
            photoFile = createImageFile(); // Crear el archivo temporal
        } catch (IOException ex) {
            Log.e(TAG, "Error al crear archivo de imagen", ex);
        }

        if (photoFile != null) {
            // 🚨 CORRECCIÓN CLAVE: Autoridad fija del FileProvider
            Uri photoURI = FileProvider.getUriForFile(this,
                    "com.miempresa.bivlotectatecnica.fileprovider", // Autoridad fija
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);

            // 🚨 CONCESIÓN DE PERMISOS CRÍTICA para evitar el fallo de "Permiso Denegado"
            takePictureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }

        // Intent 2: Galería
        Intent pickPhotoIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickPhotoIntent.setType("image/*");

        // Crear Chooser (Permite al usuario elegir entre cámara o galería)
        Intent chooserIntent = Intent.createChooser(pickPhotoIntent, "Seleccionar Fuente de Imagen");
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{takePictureIntent});

        startActivityForResult(chooserIntent, REQUEST_IMAGE_CAPTURE);
    }

    /**
     * Crea un archivo de imagen temporal para la foto capturada.
     */
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath(); // Guardar la ruta temporal
        return image;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PermissionHelper.REQUEST_CODE_CAMERA_GALLERY && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permiso concedido, intentamos de nuevo iniciar la captura de imagen
            if (currentPhotoStep != null) {
                startImageIntent(currentPhotoStep);
            }
        } else {
            Toast.makeText(this, "Permisos denegados. No se puede adjuntar la foto.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && currentPhotoStep != null) {
            Uri selectedImageUri = null;

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                // Opción 1: Foto capturada. La URI está en currentPhotoPath.
                selectedImageUri = Uri.fromFile(new File(currentPhotoPath));

            } else if (requestCode == REQUEST_IMAGE_PICK && data != null && data.getData() != null) {
                // Opción 2: Imagen seleccionada de la galería.
                selectedImageUri = data.getData();
            }

            if (selectedImageUri != null) {
                // Actualizar el estado del paso con la URI permanente
                currentPhotoStep.photoUri = selectedImageUri.toString();

                // Mostrar la imagen en el ImageView correcto
                currentPhotoStep.imageView.setImageURI(selectedImageUri);
                currentPhotoStep.imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                Toast.makeText(this, "Foto del Paso " + currentPhotoStep.stepNumber + " adjuntada.", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No se capturó ni seleccionó ninguna imagen.", Toast.LENGTH_SHORT).show();
            }
            // Limpiar la referencia al paso activo
            currentPhotoStep = null;
        }
    }


    /**
     * Define el tipo de acción (Armado/Desarme) y actualiza el estilo del botón.
     */
    private void setActionType(View clickedView, String type) {
        selectedActionType = type;

        // Resetear y seleccionar el botón de acción
        btnActionAssemble.setSelected(false);
        btnActionDisassemble.setSelected(false);

        clickedView.setSelected(true); // Estilo visual para el botón seleccionado
        Toast.makeText(this, "Acción: " + type, Toast.LENGTH_SHORT).show();
    }


    /**
     * Agrega dinámicamente un nuevo bloque de entrada de pasos (foto + descripción) al contenedor.
     */
    private void addStepView() {
        LayoutInflater inflater = LayoutInflater.from(this);
        View stepView = inflater.inflate(R.layout.list_item_step, dynamicStepsContainer, false);

        final StepData stepData = new StepData();
        stepData.stepNumber = nextStepNumber;

        // Inicializar Vistas del Paso Dinámico:
        TextView stepNumberText = stepView.findViewById(R.id.tv_step_number);
        ImageView imagePreview = stepView.findViewById(R.id.image_preview);
        Button btnCapture = stepView.findViewById(R.id.btn_select_photo);
        EditText editDescription = stepView.findViewById(R.id.edit_step_description);

        // Capturar la referencia a la vista y el EditText para el guardado posterior
        stepData.descriptionView = editDescription;
        stepData.imageView = imagePreview;
        stepData.photoUri = null; // Inicializar URI

        stepNumberText.setText(String.format(Locale.getDefault(), "PASO %d", nextStepNumber));
        stepView.setTag(nextStepNumber); // Etiquetar la vista con el número de paso

        // Listener para abrir la cámara/galería
        btnCapture.setOnClickListener(v -> {
            // 🚨 Llamar a la lógica de la cámara, pasando el objeto StepData actual
            if (PermissionHelper.checkAndRequestPermissions(this)) {
                startImageIntent(stepData);
            }
        });

        // Añadir el paso a la lista de control y a la UI
        currentStepsData.add(stepData);
        dynamicStepsContainer.addView(stepView);
        nextStepNumber++;
    }

    /**
     * Valida los datos iniciales, guarda el COMPONENTE (si es nuevo), y luego guarda todos los pasos.
     */
    private void saveReportAndFinalize() {
        String componentName = editComponentName.getText().toString().trim();
        String inventoryCode = editInventoryCode.getText().toString().trim();

        // 1. Validación de datos fijos y acción
        if (TextUtils.isEmpty(componentName) || TextUtils.isEmpty(inventoryCode) || selectedActionType == null) {
            Toast.makeText(this, "Faltan datos iniciales (Nombre, Código, Tipo de Acción).", Toast.LENGTH_LONG).show();
            return;
        }

        try {
            // --- 🚨 PASO 1: CREAR EL COMPONENTE (si no existe) y obtener su ID ---
            long componenteId = db.getOrInsertComponenteId(componentName, inventoryCode, currentWorkshopId);

            if (componenteId == -1) {
                Toast.makeText(this, "Error al crear/obtener el ID del componente.", Toast.LENGTH_LONG).show();
                return;
            }

            // --- 🚨 PASO 2: Guardar todos los pasos dinámicos en la tabla INFORME ---
            int savedSteps = 0;

            for (StepData step : currentStepsData) {
                String description = step.descriptionView.getText().toString().trim();

                // Si el paso está completamente vacío (sin descripción Y sin foto), lo ignoramos.
                if (TextUtils.isEmpty(description) && TextUtils.isEmpty(step.photoUri)) {
                    continue;
                }

                // Guardar el paso en la base de datos
                long stepId = db.insertReportStep(
                        componenteId,
                        step.stepNumber,
                        description,
                        step.photoUri != null ? step.photoUri : "placeholder_uri", // Usar la URI guardada o una por defecto
                        currentUserId,
                        selectedActionType
                );

                if (stepId > 0) {
                    savedSteps++;
                } else {
                    Log.e(TAG, "Error al guardar el paso: " + step.stepNumber);
                }
            }

            // --- PASO 3: Finalizar ---
            if (savedSteps > 0) {
                Toast.makeText(this, String.format("Informe finalizado con %d pasos guardados.", savedSteps), Toast.LENGTH_LONG).show();
                setResult(RESULT_OK); // Indica a ReportListActivity que refresque la lista
                finish();
            } else {
                Toast.makeText(this, "No se guardó ningún paso válido. Informe no creado.", Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Log.e(TAG, "Fallo crítico durante la inserción del informe", e);
            Toast.makeText(this, "Fallo: Error al procesar los datos.", Toast.LENGTH_LONG).show();
        }
    }
}