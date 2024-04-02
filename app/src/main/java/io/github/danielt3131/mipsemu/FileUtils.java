package io.github.danielt3131.mipsemu;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.util.Log;

public class FileUtils {

    public static String getFileName(Context context, Uri fileUri){
        Cursor cursor = context.getContentResolver().query(fileUri, null, null, null, null);
        int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        cursor.moveToFirst();
        return cursor.getString(nameIndex);
    }
}
