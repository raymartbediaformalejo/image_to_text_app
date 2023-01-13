package com.hfad.imagetotextapp;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;

public class MainActivity extends AppCompatActivity {

    //UI View
    private MaterialButton inputImageBtn;
    private MaterialButton recognizedTextBtn;
    private ShapeableImageView imageIv;
    private EditText recognizedTextEt;

    //TAG
    private static final String TAG = "MAIN_TAG";

    //Uri of the image that we will take from Camera/Gallery
    private Uri imageUri = null;

    //to handle the result of Camera/Gallery permissions
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 101;

    //arrays of permission required to pick image from Camera, Gallery
    private String[] cameraPermissions;
    private String[] storagePermissions;

    //progress dialog
    private ProgressDialog progressDialog;

    //TextRecognizer
    private TextRecognizer textRecognizer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //init UI View
        inputImageBtn = findViewById(R.id.inputImageBtn);
        recognizedTextBtn = findViewById(R.id.recognizeTextBtn);
        imageIv = findViewById(R.id.imageIv);
        recognizedTextEt = findViewById(R.id.recognizedTextEt);

        //init arrays of permissions required for camera, gallery
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        //init setup the progress dialog, show while text from image is being recognized
        progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Please wait");
        progressDialog.setCanceledOnTouchOutside(false);

        //init TextRecognizer
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

        //handle click, show input image dialog
        inputImageBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInputImageDialog();
            }
        });

        recognizedTextBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (imageUri == null) {
                    Toast.makeText(MainActivity.this, "Pick image first...", Toast.LENGTH_SHORT).show();
                }
                else {
                    recognizeTextFromImage();
                }
            }
        });
    }

    private void recognizeTextFromImage() {
        Log.d(TAG, "recognizeTextFromImage: ");
        //set message and show progress dialog
        progressDialog.setMessage("Preparing image...");
        progressDialog.show();

        try {
            //Prepare InputImage from image uri
            InputImage inputImage = InputImage.fromFilePath(this, imageUri);
            //image prepared, we are about to start text recognition process, change progress message
            progressDialog.setMessage("Recognizing text...");
            //start text recognition process from image
            Task<Text> textTaskResult = textRecognizer.process(inputImage)
                    .addOnSuccessListener(new OnSuccessListener<Text>() {
                        @Override
                        public void onSuccess(Text text) {
                            //process completed, dismiss dialog
                            progressDialog.dismiss();
                            //get the recognized text
                            String recognizedText = text.getText();
                            Log.d(TAG, "onSuccess: recognizedText" + recognizedText);
                            //set the recognized text to edit text
                            recognizedTextEt.setText(recognizedText);
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //failed recognizing text from image, dismiss dialog, show reason is Toast
                            progressDialog.dismiss();
                            Log.e(TAG, "onFailure: ", e);
                            Toast.makeText(MainActivity.this, "Failed recognizing text due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
        } catch (Exception e) {
            //Exception occurred while preparing InputImage, dismiss dialog, show reason in Toast
            progressDialog.dismiss();
            Log.e(TAG, "recognizeTextFromImage: ", e);
            Toast.makeText(this, "Failed preparing image due to "+e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void showInputImageDialog() {
        //init PopupMenu param 1 is context, param 2 is UI View where you want to show PopupMenu
        PopupMenu popupMenu = new PopupMenu(this, inputImageBtn);

        //Add items Camera, Gallery to PopupMenu, param 2 is menu id, param 3 is position of this menu item in menu items list, param 4 is title of the menu
        popupMenu.getMenu().add(Menu.NONE, 1, 2, "CAMERA");
        popupMenu.getMenu().add(Menu.NONE, 2, 1, "GALLERY");

        //Show PopupMenu
        popupMenu.show();

        //Handle PopupMenu item clicks
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                //get item id that is clicked from PopupMenu
                int id = menuItem.getItemId();
                if (id == 1) {
                    //Camera is click, check if camera permissions are granted or not
                    Log.d(TAG, "onMenuItemClick: Camera Clicked...");
                    if (checkCameraPermission()) {
                        //camera permissions granted, we can launch camera intent
                        pickImageCamera();
                    }
                    else {
                        //camera permissions not granted, request the camera permissions
                        requestCameraPermissions();
                    }
                }
                else if (id == 2){
                    //Gallery is clicked, check if storage permissions is granted or not
                    Log.d(TAG, "onMenuItemClick: Gallery Clicked");
                    if (checkStoragePermission()) {
                        //storage permissions granted, we can launch the gallery intent
                        pickImageGallery();
                    }
                    else {
                        //storage permissions not granted, request the storage permission
                        requestStoragePermission();
                    }
                }
                return true;
            }
        });
    }

    private void pickImageGallery() {
        Log.d(TAG, "pickImageGallery: ");
        //Intent to pick image from gallery, will show all resources from where we can pick the image
        Intent intent = new Intent(Intent.ACTION_PICK);
        //set type of file we want to pick i.e. image
        intent.setType("image/*");
        galleryActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> galleryActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will receive the image, if picked
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //image picked
                        imageUri = result.getData().getData();
                        Log.d(TAG, "onActivityResult: imageUri "+imageUri);
                        //set to imageView
                        imageIv.setImageURI(imageUri);
                    }
                    else {
                        Log.d(TAG, "onActivityResult: cancelled");
                        //cancelled
                        Toast.makeText( MainActivity.this, "Cancelled...", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private void pickImageCamera() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Sample Title");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Sample Description");

        imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        cameraActivityResultLauncher.launch(intent);
    }

    private ActivityResultLauncher<Intent> cameraActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    //here we will receive the image, if taken from camera
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        //image is taker from camera
                        //we already have the image in imageUri using function pickImageCamera()
                        Log.d(TAG, "onActivityResult: imageUri "+imageUri);
                        imageIv.setImageURI(imageUri);
                    }
                    else {
                        //cancelled
                        Log.d(TAG, "onActivityResult: cancelled");
                        Toast.makeText(MainActivity.this, "Cancelled", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    private boolean checkStoragePermission() {
        boolean result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return result;
    }

    private void requestStoragePermission() {
        ActivityCompat.requestPermissions(this, storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission() {
        boolean cameraResult = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == (PackageManager.PERMISSION_GRANTED);
        boolean storageResult = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == (PackageManager.PERMISSION_GRANTED);

        return cameraResult && storageResult;
    }

    private void requestCameraPermissions() {
        //request camera permissions (for camera intent)
        ActivityCompat.requestPermissions(
                this,
                cameraPermissions,
                CAMERA_REQUEST_CODE
        );
    }

    //handle permission results


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case CAMERA_REQUEST_CODE:{
                if (grantResults.length > 0) {
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;

                    if (cameraAccepted && storageAccepted) {

                        pickImageCamera();
                    }
                    else {
                        //one or both permissions are denied, can't launch camera intent
                        Toast.makeText(this, "Camera & Storage permissions are required", Toast.LENGTH_SHORT).show();
                    }
                }
                else {
                    //Neither allowed nor denied, rather cancelled
                    Toast.makeText(this, "Cancelled", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            case STORAGE_REQUEST_CODE: {
                //Check if some action from permission dialog performed or not Allow/Deny
                if (grantResults.length > 0) {
                    //Check if storage permissions granted, contains boolean results either true or false
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    //Check if storage permission is granted or not
                    if (storageAccepted) {
                        //storage permission granted, we can launch gallery intent
                        pickImageGallery();
                    }
                    else {
                        //storage permission denied, can't launch gallery intent
                        Toast.makeText(this, "Storage permission is required", Toast.LENGTH_SHORT).show();
                    }
                }
            }
            break;
        }
    }
}