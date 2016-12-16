package diy.ananth.looper;

/**
 * Created by Ananth on 10/6/16.
 */

import java.util.ArrayList;
import java.util.HashMap;

import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

public class PlayListActivity extends ListActivity {
    // Songs list
    public ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
    private String TAG;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist);

        final ArrayList<HashMap<String, String>> songsListData = new ArrayList<HashMap<String, String>>();

        this.songsList = getPlayList();

        if (this.songsList == null)
            return;

        // looping through playlist
        for (int i = 0; i < songsList.size(); i++) {
            // creating new HashMap
            HashMap<String, String> song = songsList.get(i);

            // adding HashList to ArrayList
            songsListData.add(song);
        }

        // Adding menuItems to ListView
        ListAdapter adapter = new SimpleAdapter(this, songsListData,
                R.layout.playlist_item, new String[]{"songTitle"}, new int[]{
                R.id.songTitle});

        setListAdapter(adapter);

        // selecting single ListView item
        ListView lv = getListView();
        // listening to single listitem click
        lv.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                // getting listitem index
                int songIndex = position;

                // Starting new intent
                Intent in = new Intent(getApplicationContext(),
                        MainActivity.class);
                // Sending songIndex to PlayerActivity
                //in.putExtra("songIndex", songIndex);
                in.putExtra("songPath", songsListData.get(position).get("songPath"));
                in.putExtra("songTitle", songsListData.get(position).get("songTitle"));
                setResult(100, in);
                // Closing PlayListView
                finish();
            }
        });
    }

    /**
     * Function to read all mp3 files from sdcard
     * and store the details in ArrayList
     */
    public ArrayList<HashMap<String, String>> getPlayList() {

        ContentResolver cr = getContentResolver();

        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!= 0";
        String[] projection = {
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION
        };
        String sortOrder = MediaStore.Audio.Media.TITLE + " ASC";
        Cursor cur = cr.query(uri, projection, selection, null, sortOrder);
        int count = 0;

        if (cur != null) {
            count = cur.getCount();

            if (count > 0) {
                while (cur.moveToNext()) {
                    HashMap<String, String> song = new HashMap<String, String>();
                    song.put("songTitle", cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                    song.put("songPath", cur.getString(cur.getColumnIndex(MediaStore.Audio.Media.DATA)));

                    // Adding each song to SongList
                    songsList.add(song);
                }

            }
        }

        cur.close();

        // return songs list array
        return songsList;
    }
}


