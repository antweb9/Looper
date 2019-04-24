package diy.ananth.looper;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Window;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

import diy.ananth.looper.soundfile.SoundFile;

public class MainActivity extends Activity implements SeekBar.OnSeekBarChangeListener {

    public static final int PERMISSIONS_MULTIPLE_REQUEST = 123;
    public static final String TAG = "MainActivity";

    private static MediaPlayer mediaPlayer;
    private double markStart = 0;
    private double markEnd = 0;
    private static Handler myHandler = new Handler();
    private static Utilities utils;
    private String currentSongPath, currentSongTitle;
    private Activity mActivity;
    private Button SelectSong;
    private TextView song_name;
    private SeekBar seekbar;
    private TextView songCurrentDuration;
    private TextView songTotalDuration;
    private Button btn_pp;
    private Button btn_mark1;
    private Button btn_mark2;
    private Button btn_clear;
    private Button btn_save;
    private long mLoadingLastUpdateTime;
    private boolean mLoadingKeepGoing;
    private boolean mFinishActivity;
    private ProgressDialog mProgressDialog;
    private SoundFile mSoundFile;
    private File mFile;
    private String mArtist;
    private String mTitle;
    private String mCaption = "";
    private Handler mHandler;
    private TextView howToUse;

    private Thread mLoadSoundFileThread;
    private Thread mSaveSoundFileThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
            getActionBar().hide();
        } catch (Exception e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_main);
        mActivity = this;

        mediaPlayer = new MediaPlayer();
        utils = new Utilities();

        mHandler = new Handler();
        mProgressDialog = null;
        mLoadSoundFileThread = null;
        mSaveSoundFileThread = null;

        initViews();

        btn_pp.setEnabled(false);
        btn_mark1.setEnabled(false);
        btn_mark2.setEnabled(false);
        btn_clear.setEnabled(false);
        btn_save.setEnabled(false);

        btn_pp.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
        btn_mark1.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
        btn_mark2.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
        btn_clear.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
        btn_save.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));

        seekbar.setOnSeekBarChangeListener(this);

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-6815878541496227~4563039390");

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Looper");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        checkAndroidVersion();
    }

    private void initViews() {
        SelectSong = findViewById(R.id.btn_ss);
        song_name = findViewById(R.id.song_name);
        seekbar = findViewById(R.id.seekbar);
        songCurrentDuration = findViewById(R.id.songCurrentDurationLabel);
        songTotalDuration = findViewById(R.id.songTotalDurationLabel);
        btn_clear = findViewById(R.id.btn_clear);
        btn_mark1 = findViewById(R.id.btn_mark1);
        btn_mark2 = findViewById(R.id.btn_mark2);
        btn_pp = findViewById(R.id.btn_pp);
        btn_save = findViewById(R.id.btn_save);
        howToUse = findViewById(R.id.how_to_use);
        btn_save.setOnClickListener(v -> {
            if (isWritingPermissionGranted()) {
                try {
                    if (!btn_mark2.isEnabled()) {
                        saveSound(currentSongTitle + "-Looper" + System.currentTimeMillis(), markStart / 1000, markEnd / 1000);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(mActivity, "Saving Error", Toast.LENGTH_LONG).show();
                }
            }
        });
        SelectSong.setOnClickListener(v -> {
            if (isStoragePermissionGranted()) {
                Intent i = new Intent(getApplicationContext(), SelectActivity.class);
                i.putExtra("LoopOrNot", false);
                startActivityForResult(i, 100);
            }
        });
        btn_pp.setOnClickListener(v -> {
            if (mediaPlayer.isPlaying()) {
                if (mediaPlayer != null)
                    mediaPlayer.pause();
            } else {
                if (mediaPlayer != null)
                    mediaPlayer.start();
            }
        });
        btn_mark1.setOnClickListener(v -> {
            markStart = mediaPlayer.getCurrentPosition();
            btn_mark1.setEnabled(false);
            btn_mark1.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
        });
        btn_mark2.setOnClickListener(v -> {
            markEnd = mediaPlayer.getCurrentPosition();
            btn_mark2.setEnabled(false);
            btn_mark2.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
            mediaPlayer.seekTo((int) markStart);
            btn_clear.setEnabled(true);
            btn_clear.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btn_save.setEnabled(true);
            btn_save.setBackgroundColor(getResources().getColor(R.color.primary_color));
            seekbar.setEnabled(false);
        });
        btn_clear.setOnClickListener(v -> {
            btn_mark1.setEnabled(true);
            btn_mark1.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btn_mark2.setEnabled(true);
            btn_mark2.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btn_clear.setEnabled(false);
            btn_clear.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
            btn_save.setEnabled(false);
            btn_save.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
            markStart = markEnd = 0;
            seekbar.setEnabled(true);
        });
        howToUse.setOnClickListener(v -> {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.how_to_use_title))
                    .setMessage(getString(R.string.how_to_use))
                    .setPositiveButton(
                            R.string.alert_ok_button,
                            (dialog, whichButton) -> {
                            })
                    .setCancelable(false)
                    .show();
        });
    }

    void initialReset() {
        btn_mark1.setEnabled(true);
        btn_mark1.setBackgroundColor(getResources().getColor(R.color.primary_color));
        btn_mark2.setEnabled(true);
        btn_mark2.setBackgroundColor(getResources().getColor(R.color.primary_color));
        btn_clear.setEnabled(false);
        btn_clear.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
        btn_save.setEnabled(false);
        btn_save.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
        markStart = markEnd = 0;
        seekbar.setEnabled(true);
    }

    /**
     * Receiving song index from playlist view
     * and play the song
     */
    @Override
    protected void onActivityResult(int requestCode,
                                    int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == 100) {
            currentSongPath = data.getExtras().getString("songPath");
            currentSongTitle = data.getExtras().getString("songTitle");
            initialReset();
            loadFromFile();
        } else if (resultCode == 200) {
            Toast.makeText(mActivity, "No Loops!", Toast.LENGTH_SHORT).show();
        } else if (resultCode == 300) {
            Toast.makeText(mActivity, "No Songs!", Toast.LENGTH_SHORT).show();
        }
    }

    public void playSong(String currentSongPath, String currentSongTitle) {
        // Play song
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(currentSongPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            if (currentSongPath.contains("/Looper/"))
                mediaPlayer.setLooping(true);
            // Displaying Song title
            song_name.setText(currentSongTitle);

            btn_pp.setEnabled(true);
            btn_mark1.setEnabled(true);
            btn_mark2.setEnabled(true);
            btn_pp.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btn_mark1.setBackgroundColor(getResources().getColor(R.color.primary_color));
            btn_mark2.setBackgroundColor(getResources().getColor(R.color.primary_color));

            // set Progress bar values
            seekbar.setProgress(0);
            seekbar.setMax(100);

            // Updating progress bar
            updateProgressBar();
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Update timer on seekbar
     */
    public void updateProgressBar() {
        myHandler.postDelayed(mUpdateTimeTask, 100);
    }

    /**
     * Background Runnable thread
     */
    private Runnable mUpdateTimeTask = new Runnable() {
        public void run() {
            long totalDuration = mediaPlayer.getDuration();
            long currentDuration = mediaPlayer.getCurrentPosition();
            songTotalDuration.setText("" + utils.milliSecondsToTimer(totalDuration));
            songCurrentDuration.setText("" + utils.milliSecondsToTimer(currentDuration));
            int progress = (int) (utils.getProgressPercentage(currentDuration, totalDuration));
            seekbar.setProgress(progress);
            if (!btn_mark2.isEnabled()) {
                if ((markEnd < currentDuration + 100) && (markEnd > currentDuration - 100)) {
                    mediaPlayer.seekTo((int) markStart);
                }
            }
            myHandler.postDelayed(this, 100);
        }
    };


    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

    }


    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        myHandler.removeCallbacks(mUpdateTimeTask);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        myHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mediaPlayer.getDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);
        mediaPlayer.seekTo(currentPosition);
        updateProgressBar();
    }

    @Override
    protected void onDestroy() {
        myHandler.removeCallbacks(mUpdateTimeTask);
        if (mediaPlayer != null)
            mediaPlayer.release();
        closeThread(mLoadSoundFileThread);
        closeThread(mSaveSoundFileThread);
        mLoadSoundFileThread = null;
        mSaveSoundFileThread = null;
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
        super.onDestroy();
    }

    public boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(mActivity, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    public boolean isWritingPermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(mActivity, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else {
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    private void checkAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void checkPermission() {
        if (ContextCompat.checkSelfPermission(mActivity,
                android.Manifest.permission.READ_EXTERNAL_STORAGE) + ContextCompat
                .checkSelfPermission(mActivity,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale
                    (mActivity, android.Manifest.permission.READ_EXTERNAL_STORAGE) ||
                    ActivityCompat.shouldShowRequestPermissionRationale
                            (mActivity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                Toast.makeText(mActivity, getString(R.string.please_grant_permissions), Toast.LENGTH_LONG).show();
            } else {
                requestPermissions(
                        new String[]{android.Manifest.permission
                                .READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_MULTIPLE_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSIONS_MULTIPLE_REQUEST) {
            if (grantResults.length > 0) {
                boolean writeExternalFile = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean readExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                if (!writeExternalFile || !readExternalFile) {
                    Toast.makeText(mActivity, getString(R.string.please_grant_permissions), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    private void saveSound(String title, double startTime, double endTime) {
        final int startFrame = Utilities.secondsToFrames(startTime, mSoundFile.getSampleRate(),
                mSoundFile.getSamplesPerFrame());
        final int endFrame = Utilities.secondsToFrames(endTime, mSoundFile.getSampleRate(),
                mSoundFile.getSamplesPerFrame());
        final int duration = (int) (endTime - startTime + 0.5);

        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setTitle(R.string.progress_dialog_saving);
        mProgressDialog.setIndeterminate(true);
        mProgressDialog.setCancelable(false);
        mProgressDialog.show();

        // Save the sound file in a background thread
        mSaveSoundFileThread = new Thread() {
            public void run() {
                // Try AAC first.
                String outPath = makeFilename(title, ".m4a");
                if (outPath == null) {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            showFinalAlert(new Exception(), R.string.no_unique_filename);
                        }
                    };
                    mHandler.post(runnable);
                    return;
                }
                File outFile = new File(outPath);
                Boolean fallbackToWAV = false;
                try {
                    // Write the new file
                    mSoundFile.WriteFile(outFile, startFrame, endFrame - startFrame);
                } catch (Exception e) {
                    // log the error and try to create a .wav file instead
                    if (outFile.exists()) {
                        outFile.delete();
                    }
                    StringWriter writer = new StringWriter();
                    e.printStackTrace(new PrintWriter(writer));
                    Log.e("Looper", "Error: Failed to create " + outPath);
                    Log.e("Looper", writer.toString());
                    fallbackToWAV = true;
                }

                // Try to create a .wav file if creating a .m4a file failed.
                if (fallbackToWAV) {
                    outPath = makeFilename(title, ".wav");
                    if (outPath == null) {
                        Runnable runnable = new Runnable() {
                            public void run() {
                                showFinalAlert(new Exception(), R.string.no_unique_filename);
                            }
                        };
                        mHandler.post(runnable);
                        return;
                    }
                    outFile = new File(outPath);
                    try {
                        // create the .wav file
                        mSoundFile.WriteWAVFile(outFile, startFrame, endFrame - startFrame);
                    } catch (Exception e) {
                        // Creating the .wav file also failed. Stop the progress dialog, show an
                        // error message and exit.
                        mProgressDialog.dismiss();
                        if (outFile.exists()) {
                            outFile.delete();
                        }

                        CharSequence errorMessage;
                        if (e.getMessage() != null
                                && e.getMessage().equals("No space left on device")) {
                            errorMessage = getResources().getText(R.string.no_space_error);
                            e = null;
                        } else {
                            errorMessage = getResources().getText(R.string.write_error);
                        }
                        final CharSequence finalErrorMessage = errorMessage;
                        final Exception finalException = e;
                        Runnable runnable = new Runnable() {
                            public void run() {
                                showFinalAlert(finalException, finalErrorMessage);
                            }
                        };
                        mHandler.post(runnable);
                        return;
                    }
                }

                // Try to load the new file to make sure it worked
                try {
                    final SoundFile.ProgressListener listener =
                            new SoundFile.ProgressListener() {
                                public boolean reportProgress(double frac) {
                                    // Do nothing - we're not going to try to
                                    // estimate when reloading a saved sound
                                    // since it's usually fast, but hard to
                                    // estimate anyway.
                                    return true;  // Keep going
                                }
                            };
                    SoundFile.create(outPath, listener);
                } catch (final Exception e) {
                    mProgressDialog.dismiss();
                    e.printStackTrace();

                    Runnable runnable = new Runnable() {
                        public void run() {
                            showFinalAlert(e, getResources().getText(R.string.write_error));
                        }
                    };
                    mHandler.post(runnable);
                    return;
                }

                mProgressDialog.dismiss();

                final String finalOutPath = outPath;
                Runnable runnable = new Runnable() {
                    public void run() {
                        afterSavingRingtone(title,
                                finalOutPath,
                                duration);
                    }
                };
                mHandler.post(runnable);
            }
        };
        mSaveSoundFileThread.start();
    }

    private void loadFromFile() {
        mFile = new File(currentSongPath);

        SongMetadataReader metadataReader = new SongMetadataReader(
                this, currentSongPath);
        mTitle = metadataReader.mTitle;
        mArtist = metadataReader.mArtist;

        String titleLabel = mTitle;
        if (mArtist != null && mArtist.length() > 0) {
            titleLabel += " - " + mArtist;
        }
        setTitle(titleLabel);

        mLoadingLastUpdateTime = getCurrentTime();
        mLoadingKeepGoing = true;
        mFinishActivity = false;
        mProgressDialog = new ProgressDialog(MainActivity.this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setTitle(R.string.progress_dialog_loading);
        mProgressDialog.setCancelable(true);
        mProgressDialog.setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        mLoadingKeepGoing = false;
                        mFinishActivity = true;
                    }
                });
        mProgressDialog.show();

        final SoundFile.ProgressListener listener =
                new SoundFile.ProgressListener() {
                    public boolean reportProgress(double fractionComplete) {
                        long now = getCurrentTime();
                        if (now - mLoadingLastUpdateTime > 100) {
                            mProgressDialog.setProgress(
                                    (int) (mProgressDialog.getMax() * fractionComplete));
                            mLoadingLastUpdateTime = now;
                        }
                        return mLoadingKeepGoing;
                    }
                };

        // Load the sound file in a background thread
        mLoadSoundFileThread = new Thread() {
            public void run() {
                try {
                    mSoundFile = SoundFile.create(mFile.getAbsolutePath(), listener);

                    if (mSoundFile == null) {
                        mProgressDialog.dismiss();
                        String name = mFile.getName().toLowerCase();
                        String[] components = name.split("\\.");
                        String err;
                        if (components.length < 2) {
                            err = getResources().getString(
                                    R.string.no_extension_error);
                        } else {
                            err = getResources().getString(
                                    R.string.bad_extension_error) + " " +
                                    components[components.length - 1];
                        }
                        final String finalErr = err;
                        Runnable runnable = new Runnable() {
                            public void run() {
                                showFinalAlert(new Exception(), finalErr);
                            }
                        };
                        mHandler.post(runnable);
                        return;
                    }
                } catch (final Exception e) {
                    mProgressDialog.dismiss();
                    e.printStackTrace();

                    Runnable runnable = new Runnable() {
                        public void run() {
                            showFinalAlert(e, getResources().getText(R.string.read_error));
                        }
                    };
                    mHandler.post(runnable);
                    return;
                }
                mProgressDialog.dismiss();
                if (mLoadingKeepGoing) {
                    Runnable runnable = new Runnable() {
                        public void run() {
                            finishOpeningSoundFile();
                        }
                    };
                    mHandler.post(runnable);
                } else if (mFinishActivity) {
                    MainActivity.this.finish();
                }
            }
        };
        mLoadSoundFileThread.start();
    }

    private void finishOpeningSoundFile() {
        mCaption =
                mSoundFile.getFiletype() + ", " +
                        mSoundFile.getSampleRate() + " Hz, " +
                        mSoundFile.getAvgBitrateKbps() + " kbps, " +
                        getResources().getString(R.string.time_seconds);
        playSong(currentSongPath, currentSongTitle);
    }

    private void showFinalAlert(Exception e, int messageResourceId) {
        showFinalAlert(e, getResources().getText(messageResourceId));
    }

    private void showFinalAlert(Exception e, CharSequence message) {
        CharSequence title;
        if (e != null) {
            title = getResources().getText(R.string.alert_title_failure);
            setResult(RESULT_CANCELED, new Intent());
        } else {
            Log.v("Looper", "Success: " + message);
            title = getResources().getText(R.string.alert_title_success);
        }

        new AlertDialog.Builder(MainActivity.this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(
                        R.string.alert_ok_button,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog,
                                                int whichButton) {
                                finish();
                            }
                        })
                .setCancelable(false)
                .show();
    }

    private long getCurrentTime() {
        return System.nanoTime() / 1000000;
    }

    private String makeFilename(String title, String s) {
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/Looper/"
                + title + s;
    }

    private void closeThread(Thread thread) {
        if (thread != null && thread.isAlive()) {
            try {
                thread.join();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void afterSavingRingtone(String title,
                                     String outPath,
                                     int duration) {
        File outFile = new File(outPath);
        long fileSize = outFile.length();
        if (fileSize <= 512) {
            outFile.delete();
            new AlertDialog.Builder(this)
                    .setTitle(R.string.alert_title_failure)
                    .setMessage(R.string.too_small_error)
                    .setPositiveButton(R.string.alert_ok_button, null)
                    .setCancelable(false)
                    .show();
            return;
        }

        // Create the database record, pointing to the existing file path
        String mimeType;
        if (outPath.endsWith(".m4a")) {
            mimeType = "audio/mp4a-latm";
        } else if (outPath.endsWith(".wav")) {
            mimeType = "audio/wav";
        } else {
            // This should never happen.
            mimeType = "audio/mpeg";
        }

        String artist = "" + getResources().getText(R.string.artist_name);

        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DATA, outPath);
        values.put(MediaStore.MediaColumns.TITLE, title);
        values.put(MediaStore.MediaColumns.SIZE, fileSize);
        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);
        values.put(MediaStore.Audio.Media.ARTIST, artist);
        values.put(MediaStore.Audio.Media.DURATION, duration);

        // Insert it into the database
        Uri uri = MediaStore.Audio.Media.getContentUriForPath(outPath);
        final Uri newUri = getContentResolver().insert(uri, values);
        setResult(RESULT_OK, new Intent().setData(newUri));

        //File Saved
        btn_save.setEnabled(false);
        btn_save.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
        Toast.makeText(mActivity, "Loop Saved", Toast.LENGTH_LONG).show();
    }
}
