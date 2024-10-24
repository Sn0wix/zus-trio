package net.sn0wix_.zustrio;

import android.annotation.SuppressLint;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackParameters;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainActivity extends AppCompatActivity {
    private final HashMap<ExoPlayer, CheckBox> players = new HashMap<>();
    private final ArrayList<CheckBox> trackBoxes = new ArrayList<>();
    public CheckBox originalCheckbox;
    public ExoPlayer originalPlayer;
    public EditText tempoBox;
    public EditText startAtBox;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Full screen and padding setup
        EdgeToEdge.enable(this);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.pause_play), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize players and prepare them in advance
        players.put(getPlayer(R.raw.hoist_sail_akordeon), (CheckBox) findViewById(R.id.akordeon));
        players.put(getPlayer(R.raw.hoist_sail_fletna), (CheckBox) findViewById(R.id.fletna));
        players.put(getPlayer(R.raw.hoist_sail_klavir), (CheckBox) findViewById(R.id.klavir));

        trackBoxes.add(findViewById(R.id.akordeon));
        trackBoxes.add(findViewById(R.id.fletna));
        trackBoxes.add(findViewById(R.id.klavir));

        originalCheckbox = findViewById(R.id.original);
        originalPlayer = getPlayer(R.raw.hoist_sail_original);

        tempoBox = findViewById(R.id.tempo);
        startAtBox = findViewById(R.id.start_at);

        // Prepare all players before playback
        prepare();
    }

    public void pausePlayEvent(View button) {
        if (!isPlaying()) {
            int tempo = 120;
            int startAt = 1;

            try {
                tempo = Integer.parseInt(tempoBox.getText().toString());
                startAt = Integer.parseInt(startAtBox.getText().toString());
            } catch (Exception ignored) {
            }

            float seekTo = (((float) 60 / 120) * 6 * (startAt - 1)) * 1000;

            if (originalCheckbox.isChecked()) {
                originalPlayer.seekTo((long) seekTo);
                originalPlayer.setPlaybackParameters(new PlaybackParameters((float) tempo / 120));
                new Handler().postDelayed(() -> {
                    originalPlayer.play();
                    toggleIcon();
                }, 500);
                return;
            }

            int finalTempo = tempo;
            players.forEach((exoPlayer, checkBox) -> exoPlayer.seekTo((long) seekTo));
            players.forEach((exoPlayer, checkBox) -> exoPlayer.setPlaybackParameters(new PlaybackParameters((float) finalTempo / 120)));

            new Handler().postDelayed(() -> {
                players.forEach((exoPlayer, checkBox) -> {
                    if (checkBox.isChecked()) {
                        exoPlayer.play();
                    }
                });
                toggleIcon();
            }, 500);

        } else {
            // Stop all players
            players.forEach((exoPlayer, checkBox) -> {
                exoPlayer.stop();
            });

            originalPlayer.stop();

            prepare();
        }

        toggleIcon();
    }

    public void openPdf(View view) {
        String url = "https://alba-rosa.cz";

        if (view.equals(findViewById(R.id.akordeon_pdf))) {
            url = "https://drive.google.com/file/d/1jugZ2EMJ9jqhj6MzWeTLT7CFHVtT-seV/view?usp=sharing";
        } else if (view.equals(findViewById(R.id.klavir_pdf))) {
            url = "https://drive.google.com/file/d/1sXkCW3MSy9G2WF2C_WLEBEH4Wk5DKOkv/view?usp=sharing";
        } else if (view.equals(findViewById(R.id.fletna_pdf))) {
            url = "https://drive.google.com/file/d/1KFBNlqfjpz09gl6_hwwONGXMENUtXBBV/view?usp=sharing";
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));

        // Check if there is an app to handle the intent
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            Toast.makeText(MainActivity.this, "No browser app found", Toast.LENGTH_SHORT).show();
        }
    }

    public void onOriginalChecked(View button) {
        if (button instanceof CheckBox) {
            trackBoxes.forEach(checkBox -> {
                checkBox.setClickable(!((CheckBox) button).isChecked());
                checkBox.setChecked(!((CheckBox) button).isChecked());
            });
        }
    }

    public boolean isPlaying() {
        AtomicBoolean bl = new AtomicBoolean(false);

        players.forEach((exoPlayer, checkBox) -> {
            if (exoPlayer.isPlaying()) {
                bl.set(true);
            }
        });

        if (!bl.get()) {
            bl.set(originalPlayer.isPlaying());
        }

        return bl.get();
    }


    public void prepare() {
        originalPlayer.prepare();
        originalPlayer.setPlayWhenReady(false);  // Set to false so they don't play immediately
        originalPlayer.seekTo(0);

        players.forEach((exoPlayer, checkBox) -> {
            exoPlayer.prepare();    // Prepare ahead of time
            exoPlayer.setPlayWhenReady(false);  // Set to false so they don't play immediately
            exoPlayer.seekTo(0);
        });
    }

    public ExoPlayer getPlayer(int track) {
        ExoPlayer player = new ExoPlayer.Builder(this).build();
        player.setMediaItem(MediaItem.fromUri(Uri.parse("android.resource://" + getPackageName() + "/" + track)));
        player.addListener(new PlayerListener());
        return player;
    }

    public void toggleIcon() {
        if (isPlaying()) {
            findViewById(R.id.pause_play_button).setForeground(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_pause));
        } else {
            findViewById(R.id.pause_play_button).setForeground(ContextCompat.getDrawable(MainActivity.this, android.R.drawable.ic_media_play));
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release all players
        originalPlayer.release();
        originalPlayer = null;

        players.forEach((exoPlayer, checkBox) -> {
            exoPlayer.release();
        });
        players.clear();
    }


    public class PlayerListener implements Player.Listener {
        @Override
        public void onPlaybackStateChanged(int playbackState) {
            if (playbackState == Player.STATE_ENDED) {
                // Playback has completed
                toggleIcon();
                prepare();
            }
        }
    }
}
