package diy.ananth.looper;

/**
 * Created by Ananth on 10/6/16.
 */

import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.design.widget.Snackbar;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.EditText;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;

public class PlayListActivity extends ListActivity {
    // Songs list
    public ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
    private static ArrayList<HashMap<String, String>> searchResults;
    private EditText songNameSearch;
    private Activity mActivity;
    private String TAG;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlist);

        mActivity = this;
        final ArrayList<HashMap<String, String>> songsListData = new ArrayList<HashMap<String, String>>();
        songNameSearch = findViewById(R.id.song_name);

        if (getIntent().hasExtra("LoopOrNot")) {
            if (getIntent().getBooleanExtra("LoopOrNot", false)) {
                this.songsList = getLoops();
                if (songsList.size() == 0)
                    closeActivity(200);
            } else {
                this.songsList = getPlayList();
                if (songsList.size() == 0)
                    closeActivity(300);
            }
        } else
            this.songsList = getPlayList();
        if (songsList.size() == 0)
            closeActivity(300);

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
        final ListAdapter[] adapter = {new SimpleAdapter(this, songsListData,
                R.layout.playlist_item, new String[]{"songTitle"}, new int[]{
                R.id.songTitle})};

        setListAdapter(adapter[0]);

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

        searchResults = new ArrayList<HashMap<String, String>>();
        songNameSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence cs, int arg1, int arg2, int arg3) {
                // When user changed the Text
                try {
                    int textlength = songNameSearch.getText().length();
                    String searchString = songNameSearch.getText().toString();
                    searchResults.clear();
                    String attr = null;
                    for (int i = 0; i < songsList.size(); i++) {
                        attr = songsList.get(i).get("songTitle").toLowerCase().trim();
                        if (textlength <= attr.length()) {
                            if (attr.contains(searchString)) {
                                searchResults.add(songsList.get(i));
                            }
                        }
                    }
                    adapter[0] = new SimpleAdapter(mActivity, searchResults,
                            R.layout.playlist_item, new String[]{"songTitle"}, new int[]{
                            R.id.songTitle});

                    setListAdapter(adapter[0]);
                } catch (Exception e){
                    e.printStackTrace();
                    Snackbar.make(mActivity.findViewById(android.R.id.content),
                            "Search Failed",
                            Snackbar.LENGTH_INDEFINITE).show();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
                                          int arg3) {
                // TODO Auto-generated method stub

            }

            @Override
            public void afterTextChanged(Editable arg0) {
                // TODO Auto-generated method stub
            }
        });
    }

    private void closeActivity(int i) {
        // Starting new intent
        Intent in = new Intent(getApplicationContext(),
                MainActivity.class);
        setResult(i, in);
        // Closing PlayListView
        finish();
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

    public ArrayList<HashMap<String, String>> getLoops() {
        String path = Environment.getExternalStorageDirectory().toString() + "/Looper/";
        File directory = new File(path);
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                HashMap<String, String> song = new HashMap<String, String>();
                song.put("songTitle", file.getName());
                song.put("songPath", file.getPath());

                // Adding each song to SongList
                songsList.add(song);
            }
        }

        // return songs list array
        return songsList;
    }
}


