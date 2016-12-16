package diy.ananth.looper;

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
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RemoteViews;
import android.widget.SeekBar;
import android.widget.TextView;

import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class MainActivity extends AppCompatActivity implements SeekBar.OnSeekBarChangeListener {

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

    private static RemoteViews notificationView;
    private static Notification notification;
    private static NotificationManager notificationManager;

    @OnClick(R.id.btn_ss)
    void songSelect() {
        isStoragePermissionGranted();
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
        }
    }

    /**
     * Function to play a song
     *
     * @param songPath, songTitle
     */
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
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
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
}
