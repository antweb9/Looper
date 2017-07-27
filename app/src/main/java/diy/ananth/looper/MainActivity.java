package diy.ananth.looper;

import android.Manifest;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

    public static final int PERMISSIONS_MULTIPLE_REQUEST = 123;

    private static MediaPlayer mediaPlayer;
    private double startTime = 0;
    private double finalTime = 0;
    private double markStart = 0;
    private double markEnd = 0;
    private static Handler myHandler = new Handler();
    private static Utilities utils;
    private String currentSongPath, currentSongTitle;
    public static int oneTimeOnly = 0;
    private String TAG;
    private ArrayList<HashMap<String, String>> songsList = new ArrayList<HashMap<String, String>>();
    private Activity mActivity;


    @Bind(R.id.btn_ss)
    Button SelectSong;

    @Bind(R.id.song_name)
    TextView song_name;

    @Bind(R.id.BottomButtons)
    LinearLayout BottomButtons;

    @Bind(R.id.seekbar)
    SeekBar seekbar;

    @Bind(R.id.songCurrentDurationLabel)
    TextView songCurrentDuration;

    @Bind(R.id.songTotalDurationLabel)
    TextView songTotalDuration;

    @Bind(R.id.btn_pp)
    Button btn_pp;

    @Bind(R.id.btn_mark1)
    Button btn_mark1;

    @Bind(R.id.btn_mark2)
    Button btn_mark2;

    @Bind(R.id.btn_clear)
    Button btn_clear;

    @Bind(R.id.btn_save)
    Button btn_save;

    @OnClick(R.id.btn_save)
    void saveLoop() {
        if (isWritingPermissionGranted()) {
            try {
                if (!btn_mark2.isEnabled()) {
                    final CheapSoundFile.ProgressListener listener = new CheapSoundFile.ProgressListener() {
                        @Override
                        public boolean reportProgress(double fractionComplete) {
                            return true;
                        }
                    };

                    String outPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/Looper/"
                            + currentSongTitle + "-Loop" + System.currentTimeMillis() + ".mp3";
                    File outFile = new File(outPath);

                    CheapSoundFile cheapSoundFile = CheapSoundFile.create(currentSongPath, listener);
                    //CheapMP3 cheapSoundFile = (CheapMP3) CheapMP3.create(currentSongPath, listener);

                    int mSampleRate = cheapSoundFile.getSampleRate();

                    int mSamplesPerFrame = cheapSoundFile.getSamplesPerFrame();

                    int startFrame = Utilities.secondsToFrames(markStart / 1000, mSampleRate, mSamplesPerFrame);

                    int endFrame = Utilities.secondsToFrames(markEnd / 1000, mSampleRate, mSamplesPerFrame);

                    cheapSoundFile.WriteFile(outFile, startFrame, endFrame - startFrame);

                    btn_save.setEnabled(false);
                    btn_save.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));

                /*IConvertCallback callback = new IConvertCallback() {
                    @Override
                    public void onSuccess(File convertedFile) {
                        // So fast? Love it!
                        Toast.makeText(mActivity, "Loop Saved", Toast.LENGTH_LONG).show();
                    }

                    @Override
                    public void onFailure(Exception error) {
                        // Oops! Something went wrong
                        Toast.makeText(mActivity, "Saving Failed", Toast.LENGTH_LONG).show();
                    }
                };
                AndroidAudioConverter.with(this)
                        // Your current audio file
                        .setFile(outFile)
                        // Your desired audio format
                        .setFormat(AudioFormat.MP3)
                        // An callback to know when conversion is finished
                        .setCallback(callback)
                        // Start conversion
                        .convert();*/

                /*Mp3File mp3file = new Mp3File(outPath);
                ID3v2 id3v2Tag;
                if (mp3file.hasId3v2Tag()) {
                    id3v2Tag = mp3file.getId3v2Tag();
                } else {
                    // mp3 does not have an ID3v2 tag, let's create one..
                    id3v2Tag = new ID3v24Tag();
                    mp3file.setId3v2Tag(id3v2Tag);
                }
                id3v2Tag.setPublisher("Antweb");*/

                    Toast.makeText(this, "Loop Saved", Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this, "Saving Error", Toast.LENGTH_LONG).show();
            }
        }
    }

    private static RemoteViews notificationView;
    private static Notification notification;
    private static NotificationManager notificationManager;

    @OnClick(R.id.btn_ss)
    void songSelect() {
        if (isStoragePermissionGranted()) {
            Intent i = new Intent(getApplicationContext(), PlayListActivity.class);
            startActivityForResult(i, 100);
        }
    }

    @OnClick(R.id.btn_pp)
    void buttonClick() {
        if (mediaPlayer.isPlaying()) {
            if (mediaPlayer != null)
                mediaPlayer.pause();
            notificationView.setImageViewResource(R.id.play_pause, R.drawable.play);
            notification.contentView = notificationView;
            notificationManager.notify(1, notification);
        } else {
            if (mediaPlayer != null)
                mediaPlayer.start();
            notificationView.setImageViewResource(R.id.play_pause, R.drawable.pause);
            notification.contentView = notificationView;
            notificationManager.notify(1, notification);
        }
    }

    @OnClick(R.id.btn_mark1)
    void button2Click() {
        markStart = mediaPlayer.getCurrentPosition();
        btn_mark1.setEnabled(false);
        btn_mark1.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
    }

    @OnClick(R.id.btn_mark2)
    void button3Click() {
        markEnd = mediaPlayer.getCurrentPosition();
        btn_mark2.setEnabled(false);
        btn_mark2.setBackgroundColor(getResources().getColor(R.color.unselected_tab_color));
        mediaPlayer.seekTo((int) markStart);
        btn_clear.setEnabled(true);
        btn_clear.setBackgroundColor(getResources().getColor(R.color.primary_color));
        btn_save.setEnabled(true);
        btn_save.setBackgroundColor(getResources().getColor(R.color.primary_color));

        notificationView.setTextViewText(R.id.textView2, "Loop On");
        notification.contentView = notificationView;
        notificationManager.notify(1, notification);

        seekbar.setEnabled(false);
    }

    @OnClick(R.id.btn_clear)
    void button4Click() {
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

        notificationView.setTextViewText(R.id.textView2, "Loop Off");
        notification.contentView = notificationView;
        notificationManager.notify(1, notification);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        mActivity = this;

        /*mActivity = this;

        AndroidAudioConverter.load(this, new ILoadCallback() {
            @Override
            public void onSuccess() {
                // Great!
            }

            @Override
            public void onFailure(Exception error) {
                // FFmpeg is not supported by device
            }
        });*/

        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

        mediaPlayer = new MediaPlayer();
        utils = new Utilities();

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

        startService(new Intent(this, YourService.class));

        MobileAds.initialize(getApplicationContext(), "ca-app-pub-6815878541496227~4563039390");

        AdView mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

        File folder = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Looper");
        boolean success = true;
        if (!folder.exists()) {
            success = folder.mkdir();
        }
        if (success) {
            //Toast.makeText(this, "Folder created", Toast.LENGTH_LONG).show();
        } else {
            //Toast.makeText(this, "Folder not created", Toast.LENGTH_LONG).show();
        }

        checkAndroidVersion();
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
            // play selected song
            playSong(currentSongPath, currentSongTitle);
            initialReset();
        }
    }

    public void playSong(String currentSongPath, String currentSongTitle) {
        // Play song
        try {
            mediaPlayer.reset();
            mediaPlayer.setDataSource(currentSongPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
            // Displaying Song title
            song_name.setText(currentSongTitle);

            setNotification(currentSongTitle);

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

            // Displaying Total Duration time
            songTotalDuration.setText("" + utils.milliSecondsToTimer(totalDuration));
            // Displaying time completed playing
            songCurrentDuration.setText("" + utils.milliSecondsToTimer(currentDuration));

            // Updating progress bar
            int progress = (int) (utils.getProgressPercentage(currentDuration, totalDuration));
            //Log.d("Progress", ""+progress);
            seekbar.setProgress(progress);

            if (!btn_mark2.isEnabled()) {
                if ((markEnd < currentDuration + 100) && (markEnd > currentDuration - 100)) {
                    mediaPlayer.seekTo((int) markStart);
                }
            }

            // Running this thread after 100 milliseconds
            myHandler.postDelayed(this, 100);
        }
    };

    /**
     *
     * */
    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromTouch) {

    }

    /**
     * When user starts moving the progress handler
     */
    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        // remove message Handler from updating progress bar
        myHandler.removeCallbacks(mUpdateTimeTask);
    }

    /**
     * When user stops moving the progress hanlder
     */
    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        myHandler.removeCallbacks(mUpdateTimeTask);
        int totalDuration = mediaPlayer.getDuration();
        int currentPosition = utils.progressToTimer(seekBar.getProgress(), totalDuration);

        // forward or backward to certain seconds
        mediaPlayer.seekTo(currentPosition);

        // update timer progress again
        updateProgressBar();
    }

    @Override
    protected void onDestroy() {
        myHandler.removeCallbacks(mUpdateTimeTask);

        if (notificationManager != null)
            notificationManager.cancelAll();

        if (mediaPlayer != null)
            mediaPlayer.release();

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
        } else { //permission is automatically granted on sdk<23 upon installation
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
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    public void setNotification(String songName) {
        String ns = Context.NOTIFICATION_SERVICE;
        notificationManager = (NotificationManager) getSystemService(ns);

        notification = new Notification(R.mipmap.ic_launcher, null, System.currentTimeMillis());
        notification.flags = Notification.FLAG_ONGOING_EVENT | Notification.FLAG_NO_CLEAR;

        notificationView = new RemoteViews(getPackageName(), R.layout.notification);
        notificationView.setImageViewResource(R.id.play_pause, R.drawable.pause);
        notificationView.setTextViewText(R.id.textView1, songName);
        notificationView.setTextViewText(R.id.textView2, "Loop Off");

        //the intent that is started when the notification is clicked (works)
        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingNotificationIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notification.contentView = notificationView;
        notification.contentIntent = pendingNotificationIntent;

        Intent switchIntent = new Intent("diy.ananth.looper.ACTION_PLAY");
        PendingIntent pendingSwitchIntent = PendingIntent.getBroadcast(this, 0, switchIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        notificationView.setOnClickPendingIntent(R.id.play_pause, pendingSwitchIntent);
        notificationManager.notify(1, notification);
    }

    public static class RemoteControlReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equalsIgnoreCase("diy.ananth.looper.ACTION_PLAY")) {
                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                    notificationView.setImageViewResource(R.id.play_pause, R.drawable.play);
                    notification.contentView = notificationView;
                    notificationManager.notify(1, notification);
                } else {
                    mediaPlayer.start();
                    notificationView.setImageViewResource(R.id.play_pause, R.drawable.pause);
                    notification.contentView = notificationView;
                    notificationManager.notify(1, notification);
                }
            }
        }
    }

    public static class YourService extends Service {

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            return START_STICKY;
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return null;
        }

        @Override
        public void onTaskRemoved(Intent rootIntent) {
            onDestroy();
        }
    }

    private void checkAndroidVersion() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkPermission();
        } else {
            // write your logic here
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

                Snackbar.make(mActivity.findViewById(android.R.id.content),
                        "Please Grant Permissions to upload profile photo",
                        Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                        new View.OnClickListener() {
                            @RequiresApi(api = Build.VERSION_CODES.M)
                            @Override
                            public void onClick(View v) {
                                requestPermissions(
                                        new String[]{android.Manifest.permission
                                                .READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                        PERMISSIONS_MULTIPLE_REQUEST);
                            }
                        }).show();
            } else {
                requestPermissions(
                        new String[]{android.Manifest.permission
                                .READ_EXTERNAL_STORAGE, android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        PERMISSIONS_MULTIPLE_REQUEST);
            }
        } else {
            // write your logic code if permission already granted
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {
            case PERMISSIONS_MULTIPLE_REQUEST:
                if (grantResults.length > 0) {
                    boolean writeExternalFile = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    boolean readExternalFile = grantResults[0] == PackageManager.PERMISSION_GRANTED;

                    if (writeExternalFile && readExternalFile) {
                        // write your logic here
                    } else {
                        Snackbar.make(mActivity.findViewById(android.R.id.content),
                                "Please Grant Permissions",
                                Snackbar.LENGTH_INDEFINITE).setAction("ENABLE",
                                new View.OnClickListener() {
                                    @RequiresApi(api = Build.VERSION_CODES.M)
                                    @Override
                                    public void onClick(View v) {
                                        requestPermissions(
                                                new String[]{android.Manifest.permission
                                                        .READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                                PERMISSIONS_MULTIPLE_REQUEST);
                                    }
                                }).show();
                    }
                }
                break;
        }
    }
}
