package com.akvashnin.qrcodescan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.FileProvider;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.loader.content.CursorLoader;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import com.akvashnin.qrcodescan.barcode.BarcodeCaptureActivity;
import com.akvashnin.qrcodescan.ds.DataStorage;
import com.akvashnin.qrcodescan.ui.PhotoListActivity;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int RC_BARCODE_CAPTURE = 9001;
    private static final int RC_IMAGE_CAPTURE = 9002;

    private CheckBox cbAutoFocus;
    private ConstraintLayout maLayout;
    private ListView lvBarcodes;

    private SimpleCursorAdapter adapter;

    private String currentPhotoPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        cbAutoFocus = findViewById(R.id.cb_use_auto_focus);
        maLayout    = findViewById(R.id.ma_layout);
        lvBarcodes  = findViewById(R.id.lv_barcodes);

        // Adapter for barcode ListView
        adapter = new SimpleCursorAdapter(getBaseContext(),
                android.R.layout.simple_list_item_1,
                null,
                new String[] { DataStorage.Barcode.VALUE },
                new int[] { android.R.id.text1 }, 0);

        lvBarcodes.setAdapter(adapter);

        lvBarcodes.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                getContentResolver().delete(
                        ContentUris.withAppendedId(DataStorage.BARCODE_CONTENT_URI, id), null, null);

                updateBarcodesList();

                Toast.makeText(MainActivity.this, R.string.item_del_msg, Toast.LENGTH_LONG).show();

                return true;
            }

        });

        updateBarcodesList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode) {
            case RC_BARCODE_CAPTURE:
                rcBarcodeCapture(resultCode, data);
                break;
            case RC_IMAGE_CAPTURE:
                rcImageCapture(resultCode, data);
                break;
            default:
                super.onActivityResult(requestCode, resultCode, data);
                break;
        }
    }

    // Helpful methods -----------------------------------------------------------------------------

    void showResultMessage(String message) {
        Snackbar.make(maLayout, message, Snackbar.LENGTH_LONG).show();
    }

    void addBarcodeToDataStorage(String barcode) {
        // Defines an object to contain the new values to insert
        ContentValues values = new ContentValues();
        values.put(DataStorage.Barcode.VALUE, barcode);

        // Defines a new Uri object that receives the result of the insertion
        getContentResolver().insert(
                DataStorage.BARCODE_CONTENT_URI,    // the data storage barcode content URI
                values                              // the values to insert
        );
        updateBarcodesList();
    }

    void addFileToDataStorage(String filePath) {
        // Defines an object to contain the new values to insert
        ContentValues values = new ContentValues();
        values.put(DataStorage.Photos.FILE_NAME, filePath);

        // Defines a new Uri object that receives the result of the insertion
        getContentResolver().insert(
                DataStorage.PHOTOS_CONTENT_URI,    // the data storage barcode content URI
                values                              // the values to insert
        );
        Log.i("-----", "Add new photo " + filePath);
    }

    void updateBarcodesList() {
        // Create cursor loader for update barcode ListView
        CursorLoader cursorLoader = new CursorLoader(getBaseContext(), DataStorage.BARCODE_CONTENT_URI,
                null, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();
        adapter.swapCursor(cursor);
    }

    private void rcBarcodeCapture(int resultCode, Intent data) {
        if (resultCode == CommonStatusCodes.SUCCESS) {
            if (data != null) {
                Barcode barcode = data.getParcelableExtra(BarcodeCaptureActivity.BarcodeObject);
                showResultMessage(getString(R.string.barcode_success));
                addBarcodeToDataStorage(barcode.displayValue);
            } else {
                showResultMessage(getString(R.string.barcode_failure));
            }
        } else {
            showResultMessage(String.format(getString(R.string.barcode_error),
                    CommonStatusCodes.getStatusCodeString(resultCode)));
        }
    }

    private void rcImageCapture(int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            addFileToDataStorage(currentPhotoPath);
        }
        else {
            File f = new File(currentPhotoPath);
            if (f.exists()) {
                f.delete();
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,  ".jpg", storageDir);

        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // UI handlers ---------------------------------------------------------------------------------

    // btn_make_scan handler, run BarcodeCaptureActivity
    public void onBtnMakeScan(View view){
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, cbAutoFocus.isChecked());
        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }

    // btn_take_photo handler, run MediaStore.ACTION_IMAGE_CAPTURE activity
    public void onBtnTakePhoto(View view) {
        Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePhotoIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(MainActivity.this, "Error while saving picture!", Toast.LENGTH_LONG).show();
            }

            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.akvashnin.qrcodescan.fileprovider", photoFile);
                takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePhotoIntent, RC_IMAGE_CAPTURE);
            }
        }
    }

    public void onBtnShowPhotos(View view) {
        Intent intent = new Intent(this, PhotoListActivity.class);
        startActivity(intent);
    }


}
