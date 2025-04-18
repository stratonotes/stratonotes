// ✅ Same imports as before
package com.example.punchpad2;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.*;

import java.io.InputStream;

public class NoteActivity extends Activity {

    private static final int PICK_IMAGE_REQUEST = 101;

    private EditText noteText;
    private ImageView starIcon;
    private boolean isEditing = false;
    private boolean isFavorited = false;
    private LinearLayout noteContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);

        noteText = findViewById(R.id.noteText);
        starIcon = findViewById(R.id.starIcon);
        ImageButton plusButton = findViewById(R.id.plusButton);
        LinearLayout mediaOverlay = findViewById(R.id.mediaOverlay);
        ImageButton recordAudio = findViewById(R.id.recordAudio);
        ImageButton addImage = findViewById(R.id.addImage);
        noteContainer = findViewById(R.id.noteContainer);
        View voiceOverlay = findViewById(R.id.voiceOverlay);
        ImageButton startStopRecording = findViewById(R.id.startStopRecording);
        TextView recordingTimer = findViewById(R.id.recordingTimer);
        ImageButton saveRecording = findViewById(R.id.saveRecording);
        ImageView recordingIndicator = findViewById(R.id.recordingIndicator);

        UndoManager undoManager = new UndoManager();
        undoManager.attach(noteText);

        ImageButton undoButton = findViewById(R.id.undoButton);
        ImageButton redoButton = findViewById(R.id.redoButton);

        undoButton.setOnClickListener(v -> undoManager.undo());
        redoButton.setOnClickListener(v -> undoManager.redo());

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
        DraftManager draftManager = new DraftManager(this);
        String draftKey = incoming != null ? String.valueOf(incoming.hashCode()) : "placeholder";
        String draft = draftManager.loadDraft(draftKey);

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
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                draftManager.saveDraft(draftKey, s.toString());
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
            voiceOverlay.setVisibility(View.VISIBLE);
        });

        addImage.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
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

                GestureDetector widgetGesture = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
                    @Override
                    public boolean onDoubleTap(MotionEvent e) {
                        Toast.makeText(NoteActivity.this, "Trim mode (stub)", Toast.LENGTH_SHORT).show();
                        return true;
                    }
                });

                widget.setOnTouchListener(new View.OnTouchListener() {
                    private float dY;
                    private boolean dragging = false;
                    private Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);

                    @Override
                    public boolean onTouch(View v, MotionEvent event) {
                        if (widgetGesture.onTouchEvent(event)) return true;

                        switch (event.getAction()) {
                            case MotionEvent.ACTION_DOWN:
                                dY = v.getY() - event.getRawY();
                                dragging = false;
                                break;
                            case MotionEvent.ACTION_MOVE:
                                if (event.getEventTime() - event.getDownTime() > 200) {
                                    if (!dragging) {
                                        dragging = true;
                                        if (vibrator != null) vibrator.vibrate(10);
                                    }
                                    v.setY(event.getRawY() + dY);
                                }
                                break;
                            case MotionEvent.ACTION_UP:
                                dragging = false;
                                break;
                        }
                        return true;
                    }
                });

                ((LinearLayout) noteText.getParent()).addView(widget);
                noteText.append("\n");
            } else {
                Toast.makeText(this, "Stop recording before saving.", Toast.LENGTH_SHORT).show();
            }
        });

        // ✅ Sliding plus menu behavior
        plusButton.setOnClickListener(v -> {
            if (mediaOverlay.getVisibility() == View.VISIBLE) {
                mediaOverlay.animate()
                        .translationX(mediaOverlay.getWidth())
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            mediaOverlay.setVisibility(View.GONE);
                            voiceOverlay.setVisibility(View.GONE);
                        });
            } else {
                mediaOverlay.setTranslationX(mediaOverlay.getWidth());
                mediaOverlay.setAlpha(0f);
                mediaOverlay.setVisibility(View.VISIBLE);
                mediaOverlay.animate()
                        .translationX(0f)
                        .alpha(1f)
                        .setDuration(200)
                        .start();
                voiceOverlay.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            try {
                Uri imageUri = data.getData();
                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                ImageView imageView = new ImageView(this);
                imageView.setImageBitmap(bitmap);
                imageView.setAdjustViewBounds(true);
                imageView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                params.setMargins(0, 16, 0, 16);
                imageView.setLayoutParams(params);

                imageView.setOnClickListener(v -> {
                    Intent intent = new Intent(NoteActivity.this, FullscreenImageActivity.class);
                    intent.putExtra("image_uri", imageUri.toString());
                    startActivity(intent);
                });

                noteContainer.addView(imageView, noteContainer.indexOfChild(noteText));
            } catch (Exception e) {
                Toast.makeText(this, "Unsupported image format. Could not import.", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
