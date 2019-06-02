package com.akvashnin.qrcodescan.ui;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cursoradapter.widget.SimpleCursorAdapter;
import androidx.loader.content.CursorLoader;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.AdapterView;
import android.widget.ListView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.akvashnin.qrcodescan.MainActivity;
import com.akvashnin.qrcodescan.R;
import com.akvashnin.qrcodescan.ds.DataStorage;

import java.io.File;

public class PhotoListActivity extends AppCompatActivity {

    private SimpleCursorAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_list);

        ListView listView = findViewById(R.id.lvPhotos);

        // Adapter for barcode ListView
        adapter = new SimpleCursorAdapter(getBaseContext(),
                R.layout.photo_list_layout,
                null,
                new String[]{ DataStorage.Photos.FILE_NAME, DataStorage.Photos.FILE_NAME },
                new int[]{ R.id.iv_photo_view, R.id.tw_photo_name }, 0);

        adapter.setViewBinder(new SimpleCursorAdapter.ViewBinder() {

            public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
                if (view.getId() == R.id.tw_photo_name) {

                    File file = new File(cursor.getString(columnIndex));
                    TextView textView = (TextView) view;
                    textView.setText(file.getName());

                    return true;

                }
                else {
                    return false;
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                Uri uri = ContentUris.withAppendedId(DataStorage.PHOTOS_CONTENT_URI, id);

                String[] projection = { DataStorage.Photos.FILE_NAME };

                Cursor cursor = getContentResolver().query(uri, projection, null, null, null);
                if (cursor == null || !cursor.moveToNext()) {
                    return false;
                }

                File file = new File(cursor.getString(0));
                if (file.exists()) {
                    file.delete();
                }

                getContentResolver().delete(uri, null, null);

                updatePhotosList();

                Toast.makeText(PhotoListActivity.this, R.string.item_del_msg, Toast.LENGTH_LONG).show();

                return true;
            }

        });

        listView.setAdapter(adapter);

        updatePhotosList();
    }


    // Helpful methods -----------------------------------------------------------------------------

    void updatePhotosList() {
        // Create cursor loader for update barcode ListView
        CursorLoader cursorLoader = new CursorLoader(getBaseContext(), DataStorage.PHOTOS_CONTENT_URI,
                null, null, null, null);
        Cursor cursor = cursorLoader.loadInBackground();
        adapter.swapCursor(cursor);
    }
}
