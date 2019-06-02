package com.akvashnin.qrcodescan;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.Toast;

import com.akvashnin.qrcodescan.barcode.BarcodeCaptureActivity;
import com.akvashnin.qrcodescan.ds.DataStorage;
import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.material.snackbar.Snackbar;

public class MainActivity extends AppCompatActivity {

    private static final int RC_BARCODE_CAPTURE = 9001;

    private CheckBox cbAutoFocus;
    private ConstraintLayout maLayout;
    private ListView lvBarcodes;

    private SimpleCursorAdapter adapter;

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

                Log.i("-----------", "onItemLongClick " + position + " " + id);

                getContentResolver().delete(
                        ContentUris.withAppendedId(DataStorage.BARCODE_CONTENT_URI, id), null, null);

                updateBarcodesList();

                Toast.makeText(MainActivity.this, "Item Deleted", Toast.LENGTH_LONG).show();

                return true;
            }

        });





        updateBarcodesList();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RC_BARCODE_CAPTURE) {
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
        else {
            super.onActivityResult(requestCode, resultCode, data);
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

    void updateBarcodesList() {
        // Create cursor loader for update barcode ListView
        CursorLoader cursorLoader = new CursorLoader(getBaseContext(), DataStorage.BARCODE_CONTENT_URI,
                null, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();
        adapter.swapCursor(cursor);
    }

    // UI handlers ---------------------------------------------------------------------------------

    // btn_make_scan handler, run BarcodeCaptureActivity
    public void onBtnMakeScan(View view){
        Intent intent = new Intent(this, BarcodeCaptureActivity.class);
        intent.putExtra(BarcodeCaptureActivity.AutoFocus, cbAutoFocus.isChecked());
        startActivityForResult(intent, RC_BARCODE_CAPTURE);
    }


}
