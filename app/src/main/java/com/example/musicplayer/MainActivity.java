package com.example.musicplayer;



import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mediaPlayer;
    private SeekBar seekBar;
    private Button btnPlayPause, btnEnterLink;
    private TextView tvCurrent, tvDuration;

    private Handler handler = new Handler();
    private Runnable updateSeekRunnable;
    private String currentUrl = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initPlayPause();
        initSeekBar();
        initLinkDialog();
    }

    private void initViews() {
        seekBar = findViewById(R.id.seekBar);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        btnEnterLink = findViewById(R.id.btnEnterLink);
        tvCurrent = findViewById(R.id.tvCurrent);
        tvDuration = findViewById(R.id.tvDuration);
    }

    private void initLinkDialog() {
        btnEnterLink.setOnClickListener(v -> {
            android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
            android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_link, null);
            builder.setView(dialogView);

            android.app.AlertDialog dialog = builder.create();
            dialog.show();

            EditText etLink = dialogView.findViewById(R.id.etLink);
            Button btnOk = dialogView.findViewById(R.id.btnOk);

            btnOk.setOnClickListener(view -> {
                String url = etLink.getText().toString().trim();
                if (!url.isEmpty()) {
                    currentUrl = url;
                    preparePlayerFromUrl(url);
                    dialog.dismiss();
                }
            });
        });
    }

    private void preparePlayerFromUrl(String url) {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(url);
            mediaPlayer.setOnPreparedListener(mp -> {
                int duration = mp.getDuration();
                seekBar.setMax(duration);
                tvDuration.setText(formatTime(duration));
                tvCurrent.setText(formatTime(0));
            });
            mediaPlayer.setOnCompletionListener(mp -> {
                handler.removeCallbacks(updateSeekRunnable);
                seekBar.setProgress(0);
                tvCurrent.setText(formatTime(0));
                btnPlayPause.setText("Play");
            });
            mediaPlayer.prepareAsync();
        } catch (Exception e) {
            e.printStackTrace();
        }

        updateSeekRunnable = new Runnable() {
            @Override
            public void run() {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    int pos = mediaPlayer.getCurrentPosition();
                    seekBar.setProgress(pos);
                    tvCurrent.setText(formatTime(pos));
                    handler.postDelayed(this, 500);
                }
            }
        };
    }

    private void initSeekBar() {
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    tvCurrent.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                handler.removeCallbacks(updateSeekRunnable);
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mediaPlayer != null) {
                    mediaPlayer.seekTo(seekBar.getProgress());
                    if (mediaPlayer.isPlaying()) {
                        handler.post(updateSeekRunnable);
                    }
                }
            }
        });
    }

    private void initPlayPause() {
        btnPlayPause.setOnClickListener(v -> {
            if (mediaPlayer == null) return;

            if (mediaPlayer.isPlaying()) {
                mediaPlayer.pause();
                btnPlayPause.setText("Play");
                handler.removeCallbacks(updateSeekRunnable);
            } else {
                mediaPlayer.start();
                btnPlayPause.setText("Pause");
                handler.post(updateSeekRunnable);
            }
        });
    }

    private String formatTime(int millis) {
        int totalSeconds = millis / 1000;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            handler.removeCallbacks(updateSeekRunnable);
            if (mediaPlayer.isPlaying()) {
                mediaPlayer.stop();
            }
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
