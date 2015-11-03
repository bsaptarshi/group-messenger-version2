package edu.buffalo.cse.cse486586.groupmessenger2;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

/**
 * GroupMessengerProvider is a key-value table. Once again, please note that we do not implement
 * full support for SQL as a usual ContentProvider does. We re-purpose ContentProvider's interface
 * to use it as a key-value table.
 * 
 * Please read:
 * 
 * http://developer.android.com/guide/topics/providers/content-providers.html
 * http://developer.android.com/reference/android/content/ContentProvider.html
 * 
 * before you start to get yourself familiarized with ContentProvider.
 * 
 * There are two methods you need to implement---insert() and query(). Others are optional and
 * will not be tested.
 * 
 * @author stevko
 *
 */
public class GroupMessengerProvider extends ContentProvider {
    public static Uri URI_obj;
    private static String ATTR_key;
    private static String ATTR_value;
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public String getType(Uri uri) {
        // You do not need to implement this.
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        /*
         * TODO: You need to implement this method. Note that values will have two columns (a key
         * column and a value column) and one row that contains the actual (key, value) pair to be
         * inserted.
         * 
         * For actual storage, you can use any option. If you know how to use SQL, then you can use
         * SQLite. But this is not a requirement. You can use other storage options, such as the
         * internal storage option that we used in PA1. If you want to use that option, please
         * take a look at the code for PA1.
         */
        try {
            String attr_key_val_as_filename = values.getAsString(ATTR_key);
            String attr_value_val = values.getAsString(ATTR_value);
            String filepath = getContext().getFilesDir().getAbsolutePath();
            FileWriter fw = new FileWriter(new File(filepath, attr_key_val_as_filename));
            fw.write(attr_value_val);
            fw.close();

        } catch(Exception ex) {
            Log.e("insert", ex.getMessage()+" -- File write failed");
        }
        Log.v("insert", values.toString());
        return uri;
    }

    @Override
    public boolean onCreate() {
        // If you need to perform any one-time initialization task, please do it here.
        Uri.Builder URI_builder = new Uri.Builder();
        URI_builder.authority("edu.buffalo.cse.cse486586.groupmessenger2.provider");
        URI_builder.scheme("content");
        URI_obj = URI_builder.build();

        ATTR_key = new String("key");
        ATTR_value = new String("value");
        return false;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        // You do not need to implement this.
        return 0;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        /*
         * TODO: You need to implement this method. Note that you need to return a Cursor object
         * with the right format. If the formatting is not correct, then it is not going to work.
         *
         * If you use SQLite, whatever is returned from SQLite is a Cursor object. However, you
         * still need to be careful because the formatting might still be incorrect.
         *
         * If you use a file storage option, then it is your job to build a Cursor * object. I
         * recommend building a MatrixCursor described at:
         * http://developer.android.com/reference/android/database/MatrixCursor.html
         */
        try {
            String[] attributes = {ATTR_key, ATTR_value};
            String data_read;
            MatrixCursor mCursor = new MatrixCursor(attributes);
            String filepath = getContext().getFilesDir().getAbsolutePath();
            BufferedReader br = new BufferedReader(new FileReader(new File(filepath, selection)));

            data_read = br.readLine();
            mCursor.addRow(new String[]{selection,data_read});
            br.close();
            return mCursor;
        } catch (Exception ex) {
            Log.e("File_Query", ex.getMessage()+" -- File read failed");
        }
        Log.v("query", selection);
        return null;
    }
}
