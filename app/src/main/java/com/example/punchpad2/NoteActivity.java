package com.example.punchpad2;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class NoteActivity extends Activity {

    private TextView noteText;
    private ImageView starIcon;
    private boolean isEditing = false;
    private boolean isFavorited = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        noteText = findViewById(R.id.noteText);
        starIcon = findViewById(R.id.starIcon);
        ImageButton plusButton = findViewById(R.id.plusButton);
        LinearLayout mediaOverlay = findViewById(R.id.mediaOverlay);
        ImageButton recordAudio = findViewById(R.id.recordAudio);

        // NEW: Voice overlay components (ensure they're in activity_note.xml)
        View voiceOverlay = findViewById(R.id.voiceOverlay);
        ImageButton startStopRecording = findViewById(R.id.startStopRecording);
        TextView recordingTimer = findViewById(R.id.recordingTimer);
        ImageButton saveRecording = findViewById(R.id.saveRecording);
        ImageView recordingIndicator = findViewById(R.id.recordingIndicator);

        final boolean[] isRecording = {false};
        final int[] secondsElapsed = {0};
        final android.os.Handler timerHandler = new android.os.Handler();
        final Runnable timerRunnable = new Runnable() {
            @Override
            public void run() {
                secondsElapsed[0]++;
                int minutes = secondsElapsed[0] / 60;
                int seconds = secondsElapsed[0] % 60;
                recordingTimer.setText(String.format("%02d:%02d", minutes, seconds));
                timerHandler.postDelayed(this, 1000);
            }
        };

        String incoming = getIntent().getStringExtra("content");
        SharedPreferences prefs = getSharedPreferences("Drafts", MODE_PRIVATE);
        String draftKey = incoming != null ? String.valueOf(incoming.hashCode()) : "placeholder";
        String draft = prefs.getString(draftKey, null);

        if (draft != null) {
            noteText.setText(draft);
        } else if (incoming != null) {
            noteText.setText(incoming);
        } else {
            noteText.setText("This is a placeholder note.");
        }

        noteText.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(android.text.Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                prefs.edit().putString(draftKey, s.toString()).apply();
            }
        });

        noteText.setOnClickListener(v -> {
            if (!isEditing) {
                isEditing = true;
                noteText.setFocusableInTouchMode(true);
                noteText.requestFocus();
                noteText.setCursorVisible(true);
                noteText.setBackgroundColor(0x22000000);
            }
        });

        noteText.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                isEditing = false;
                noteText.setCursorVisible(false);
                noteText.setBackgroundColor(0x00000000);
            }
        });

        noteText.setOnLongClickListener(v -> {
            Toast.makeText(this, "Deletion mode not implemented yet.", Toast.LENGTH_SHORT).show();
            return true;
        });

        GestureDetector gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onDoubleTap(MotionEvent e) {
                isFavorited = !isFavorited;
                starIcon.setImageResource(isFavorited ? R.drawable.ic_star_filled : R.drawable.ic_star_outline);
                Toast.makeText(NoteActivity.this,
                        isFavorited ? "Note favorited." : "Note unfavorited.", Toast.LENGTH_SHORT).show();
                return true;
            }
        });

        noteText.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

        recordAudio.setOnClickListener(v -> {
            mediaOverlay.setVisibility(View.GONE);

            LinearLayout widget = new LinearLayout(this);
            widget.setOrientation(LinearLayout.HORIZONTAL);
            widget.setPadding(16, 16, 16, 16);
            widget.setBackgroundColor(0xFF333333);

            ImageView trimStart = new ImageView(this);
            trimStart.setImageResource(R.drawable.ic_trim_left);
            trimStart.setLayoutParams(new LinearLayout.LayoutParams(24, 48));

            View waveform = new View(this);
            LinearLayout.LayoutParams waveParams = new LinearLayout.LayoutParams(0, 48, 1f);
            waveParams.setMargins(16, 0, 16, 0);
            waveform.setLayoutParams(waveParams);
            waveform.setBackgroundColor(0xFF888888);

            ImageView trimEnd = new ImageView(this);
            trimEnd.setImageResource(R.drawable.ic_trim_right);
            trimEnd.setLayoutParams(new LinearLayout.LayoutParams(24, 48));

            TextView timestamp = new TextView(this);
            timestamp.setText(String.format("%02d:%02d / %02d:%02d", 0, 0, secondsElapsed[0] / 60, secondsElapsed[0] % 60));
            timestamp.setTextColor(0xFFFFFFFF);
            timestamp.setPadding(8, 0, 8, 0);

            ImageButton recordMore = new ImageButton(this);
            recordMore.setImageResource(R.drawable.ic_record_add);
            recordMore.setBackgroundColor(0x00000000);
            recordMore.setLayoutParams(new LinearLayout.LayoutParams(48, 48));

            ImageButton delete = new ImageButton(this);
            delete.setImageResource(R.drawable.ic_delete);
            delete.setBackgroundColor(0x00000000);
            delete.setLayoutParams(new LinearLayout.LayoutParams(48, 48));
            delete.setOnClickListener(d -> ((LinearLayout) noteText.getParent()).removeView(widget));

            widget.addView(trimStart);
            widget.addView(waveform);
            widget.addView(trimEnd);
            widget.addView(timestamp);
            widget.addView(recordMore);
            widget.addView(delete);

            ((LinearLayout) noteText.getParent()).addView(widget);
            noteText.append("\n");


                    voiceOverlay.setVisibility(View.VISIBLE);
        });

        startStopRecording.setOnClickListener(v -> {
            if (!isRecording[0]) {
                isRecording[0] = true;
                secondsElapsed[0] = 0;
                recordingTimer.setText("00:00");
                timerHandler.postDelayed(timerRunnable, 1000);
                saveRecording.setEnabled(true);
                recordingIndicator.setAlpha(1f);
                Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show();
            } else {
                isRecording[0] = false;
                timerHandler.removeCallbacks(timerRunnable);
                recordingIndicator.setAlpha(0.3f);
                Toast.makeText(this, "Recording stopped", Toast.LENGTH_SHORT).show();
            }
        });

        saveRecording.setOnClickListener(v -> {
            if (!isRecording[0]) {
                voiceOverlay.setVisibility(View.GONE);
                Toast.makeText(this, "Recording saved (stub)", Toast.LENGTH_SHORT).show();

                // ... inline widget creation preserved (same as before) ...

            } else {
                Toast.makeText(this, "Stop recording before saving.", Toast.LENGTH_SHORT).show();
            }
        });

        plusButton.setOnClickListener(v -> {
            if (mediaOverlay.getVisibility() == View.GONE) {
                mediaOverlay.setVisibility(View.VISIBLE);
                voiceOverlay.setVisibility(View.GONE);
            } else {
                mediaOverlay.setVisibility(View.GONE);
                voiceOverlay.setVisibility(View.GONE);
            }
        });
    }
}
