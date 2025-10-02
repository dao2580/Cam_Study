package vn.edu.usth.camer;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.*;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.*;

public class MainActivity extends AppCompatActivity {

    // UI
    private Button btnCamera, btnGallery, btnTranslate, btnSpeak, btnSave, btnHistory, btnLogout;
    private ImageView ivPreview;
    private TextView tvDetected, tvTranslated;
    private Spinner spinnerFrom, spinnerTo;
    private ProgressBar progressBar;
    private ListView lvHistory;

    // Helpers
    private DatabaseHelper db;
    private ObjectDetectorHelper objectDetector;
    private TranslationHelper translationHelper;
    private TextToSpeech tts;

    // Data
    private int userId;
    private String currentImagePath = "";
    private String detectedText = "";
    private String translatedText = "";
    private Bitmap currentBitmap;


    private EditText etManual;


    // Launchers
    private ActivityResultLauncher<Void> takePreviewLauncher;
    private ActivityResultLauncher<String> pickImageLauncher;

    private static final int REQ_PERMS = 100;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_main);

        userId = getSharedPreferences("UserPrefs", MODE_PRIVATE).getInt("user_id", -1);

        // Views
        btnCamera   = findViewById(R.id.btnCamera);
        btnGallery  = findViewById(R.id.btnGallery);
        btnTranslate= findViewById(R.id.btnTranslate);
        btnSpeak    = findViewById(R.id.btnSpeak);
        btnSave     = findViewById(R.id.btnSave);
        btnHistory  = findViewById(R.id.btnHistory);
        btnLogout   = findViewById(R.id.btnLogout);
        ivPreview   = findViewById(R.id.ivPreview);
        tvDetected  = findViewById(R.id.tvDetected);
        tvTranslated= findViewById(R.id.tvTranslated);
        spinnerFrom = findViewById(R.id.spinnerFrom);
        spinnerTo   = findViewById(R.id.spinnerTo);
        progressBar = findViewById(R.id.progressBar);
        lvHistory   = findViewById(R.id.lvHistory);
        lvHistory.setVisibility(View.GONE);
        etManual = findViewById(R.id.etManual);


        // Helpers
        db = new DatabaseHelper(this);
        objectDetector = new ObjectDetectorHelper(this);
        translationHelper = new TranslationHelper();
        tts = new TextToSpeech(this, st -> { if (st == TextToSpeech.SUCCESS) tts.setLanguage(Locale.ENGLISH); });

        // Spinners
        String[] langs = {"English","Vietnamese","Japanese","Korean","Chinese"};
        ArrayAdapter<String> adp = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, langs);
        adp.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFrom.setAdapter(adp); spinnerTo.setAdapter(adp);
        spinnerFrom.setSelection(0); spinnerTo.setSelection(1);

        // Launchers
        takePreviewLauncher = registerForActivityResult(
                new ActivityResultContracts.TakePicturePreview(),
                bmp -> {
                    if (bmp == null) { toast("Không nhận được ảnh từ camera"); return; }
                    currentImagePath = saveImageToFile(bmp);
                    processImage(bmp);
                });

        pickImageLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> { if (uri != null) loadFromUriAndProcess(uri); });

        // Listeners
        btnCamera.setOnClickListener(v -> openCamera());
        btnGallery.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnTranslate.setOnClickListener(v -> translateText());
        btnSpeak.setOnClickListener(v -> speakText());
        btnSave.setOnClickListener(v -> saveToHistory());
        btnHistory.setOnClickListener(v -> toggleHistory());
        btnLogout.setOnClickListener(v -> logout());

        checkAndRequestPermissions(); // ← đã đổi tên để không trùng API
    }

    /* ===== Core flows ===== */

    private void openCamera() {
        if (!ensurePerms()) return;
        takePreviewLauncher.launch(null); // mở camera, nhận bitmap preview
    }

    private void loadFromUriAndProcess(Uri uri) {
        try {
            Bitmap bmp = decodeBitmapFromUri(uri, 1280, 1280);
            if (bmp == null) { toast("Không đọc được ảnh"); return; }
            currentImagePath = saveImageToFile(bmp);
            processImage(bmp);
        } catch (Exception e) { toast("Lỗi đọc ảnh: " + e.getMessage()); }
    }

    private void processImage(Bitmap bmp) {
        currentBitmap = bmp;
        ivPreview.setImageBitmap(bmp);
        progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            try {
                String res;
                try { res = (objectDetector != null) ? objectDetector.detectObject(bmp) : ""; }
                catch (Throwable t) { res = ""; } // chống crash JNI
                detectedText = (res == null) ? "" : res;

                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    tvDetected.setText("Detected: " + (detectedText.isEmpty() ? "(none)" : detectedText));
                    toast(detectedText.isEmpty() ? "Không phát hiện được vật thể" : ("Object: " + detectedText));
                });
            } catch (Exception e) {
                runOnUiThread(() -> { progressBar.setVisibility(View.GONE); toast("Lỗi xử lý ảnh: " + e.getMessage()); });
            }
        }).start();
    }

    private void translateText() {
        String source = (etManual.getText() != null) ? etManual.getText().toString().trim() : "";
        if (source.isEmpty()) source = detectedText;

        if (source.isEmpty()) {
            toast("Hãy nhập nội dung hoặc chụp/chọn ảnh để nhận dạng trước");
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        translationHelper.translate(
                source,
                codeOf(spinnerFrom.getSelectedItem().toString()),
                codeOf(spinnerTo.getSelectedItem().toString()),
                new TranslationHelper.TranslationCallback() {
                    @Override public void onSuccess(String r) {
                        progressBar.setVisibility(View.GONE);
                        translatedText = r;
                        tvTranslated.setText("Translated: " + r);
                    }
                    @Override public void onError(String err) {
                        progressBar.setVisibility(View.GONE);
                        toast("Error: " + err);
                    }
                });
    }


    private void speakText() {
        if (translatedText.isEmpty()) { toast("Vui lòng dịch trước"); return; }
        tts.setLanguage(localeOf(codeOf(spinnerTo.getSelectedItem().toString())));
        tts.speak(translatedText, TextToSpeech.QUEUE_FLUSH, null, null);
    }

    private void saveToHistory() {
        if (translatedText.isEmpty()) { toast("Vui lòng dịch trước"); return; }
        boolean ok = db.saveHistory(userId, currentImagePath, detectedText, translatedText,
                spinnerFrom.getSelectedItem().toString(), spinnerTo.getSelectedItem().toString());
        toast(ok ? "Đã lưu lịch sử" : "Lưu thất bại");
    }

    private void toggleHistory() {
        if (lvHistory.getVisibility() == View.VISIBLE) {
            lvHistory.setVisibility(View.GONE); btnHistory.setText("Show History"); return;
        }
        ArrayList<HashMap<String,String>> list = db.getHistory(userId);
        if (list.isEmpty()) { toast("Chưa có lịch sử"); return; }

        lvHistory.setAdapter(new SimpleAdapter(this, list, R.layout.item_history,
                new String[]{"detected_text","translated_text","source_lang","target_lang"},
                new int[]{R.id.tvOriginal,R.id.tvTranslated,R.id.tvSource,R.id.tvTarget}));

        lvHistory.setOnItemLongClickListener((p,v,pos,id)->{
            int hid = Integer.parseInt(list.get(pos).get("id"));
            new AlertDialog.Builder(this).setTitle("Delete").setMessage("Delete this item?")
                    .setPositiveButton("Yes",(d,w)->{ db.deleteHistory(hid); toggleHistory(); toggleHistory(); })
                    .setNegativeButton("No",null).show();
            return true;
        });

        lvHistory.setVisibility(View.VISIBLE);
        btnHistory.setText("Hide History");
    }

    private void logout() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        prefs.edit().clear().apply();
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    /* ===== Permissions & helpers ===== */

    private void checkAndRequestPermissions() {
        List<String> need = new ArrayList<>();
        if (needs(Manifest.permission.CAMERA)) need.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= 33) {
            if (needs(Manifest.permission.READ_MEDIA_IMAGES)) need.add(Manifest.permission.READ_MEDIA_IMAGES);
        } else {
            if (needs(Manifest.permission.READ_EXTERNAL_STORAGE)) need.add(Manifest.permission.READ_EXTERNAL_STORAGE);
        }
        if (!need.isEmpty()) ActivityCompat.requestPermissions(this, need.toArray(new String[0]), REQ_PERMS);
    }
    private boolean ensurePerms() {
        if (needs(Manifest.permission.CAMERA)) { checkAndRequestPermissions(); return false; }
        return true;
    }
    private boolean needs(String p){ return ContextCompat.checkSelfPermission(this,p)!= PackageManager.PERMISSION_GRANTED; }

    private Bitmap decodeBitmapFromUri(Uri uri, int reqW, int reqH) throws Exception {
        if (Build.VERSION.SDK_INT >= 28) {
            ImageDecoder.Source src = ImageDecoder.createSource(getContentResolver(), uri);
            return ImageDecoder.decodeBitmap(src, (d,i,s) -> {
                d.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                d.setOnPartialImageListener(e -> true);
            });
        } else {
            BitmapFactory.Options b = new BitmapFactory.Options(); b.inJustDecodeBounds = true;
            try (InputStream is = getContentResolver().openInputStream(uri)) { BitmapFactory.decodeStream(is,null,b); }
            BitmapFactory.Options o = new BitmapFactory.Options(); o.inSampleSize = calcInSampleSize(b, reqW, reqH);
            try (InputStream is2 = getContentResolver().openInputStream(uri)) { return BitmapFactory.decodeStream(is2,null,o); }
        }
    }
    private int calcInSampleSize(BitmapFactory.Options op, int reqW, int reqH) {
        int s = 1, h = op.outHeight, w = op.outWidth; if (h<=0||w<=0) return 1;
        while (h/s > reqH || w/s > reqW) s *= 2; return s;
    }
    private String saveImageToFile(Bitmap bmp) {
        try {
            File f = new File(getFilesDir(), "img_"+System.currentTimeMillis()+".jpg");
            try (FileOutputStream o = new FileOutputStream(f)) { bmp.compress(Bitmap.CompressFormat.JPEG, 90, o); }
            return f.getAbsolutePath();
        } catch (Exception e) { return ""; }
    }
    private String codeOf(String lang) {
        switch (lang) {
            case "Vietnamese": return "vi";
            case "Japanese":   return "ja";
            case "Korean":     return "ko";
            case "Chinese":    return "zh";
            default:           return "en";
        }
    }
    private Locale localeOf(String code) {
        switch (code) {
            case "vi": return new Locale("vi","VN");
            case "ja": return Locale.JAPANESE;
            case "ko": return Locale.KOREAN;
            case "zh": return Locale.CHINESE;
            default:   return Locale.ENGLISH;
        }
    }
    private void toast(String s){ Toast.makeText(this, s, Toast.LENGTH_SHORT).show(); }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) tts.shutdown();
        if (objectDetector != null) objectDetector.close();
        if (currentBitmap != null && !currentBitmap.isRecycled()) { currentBitmap.recycle(); currentBitmap = null; }
    }
}
