package vn.edu.usth.camer;

import android.os.AsyncTask;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;


public class TranslationHelper {

    // Đọc từ build.gradle(.kts) -> defaultConfig -> buildConfigField
    private static final String API_KEY  = BuildConfig.AZURE_TRANSLATOR_KEY;
    private static final String REGION   = BuildConfig.AZURE_TRANSLATOR_REGION;

    // Endpoint cố định của Microsoft Translator (Text v3)
    private static final String ENDPOINT = "https://api.cognitive.microsofttranslator.com/translate?api-version=3.0";

    public interface TranslationCallback {
        void onSuccess(String translatedText);
        void onError(String error);
    }

    /** Gọi dịch: nếu fromLang là "" hoặc "auto" -> để API tự detect */
    public void translate(String text, String fromLang, String toLang, TranslationCallback callback) {
        new TranslateTask(callback, fromLang, toLang).execute(text);
    }

    private static class TranslateTask extends AsyncTask<String, Void, String> {
        private final TranslationCallback callback;
        private final String fromLang;
        private final String toLang;
        private String error;

        TranslateTask(TranslationCallback callback, String fromLang, String toLang) {
            this.callback = callback;
            this.fromLang = fromLang == null ? "" : fromLang.trim();
            this.toLang   = toLang   == null ? "" : toLang.trim();
        }

        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection conn = null;
            try {
                String text = params[0];

                // Build URL: luôn có "to", "from" tùy chọn
                StringBuilder urlSb = new StringBuilder(ENDPOINT);
                if (!toLang.isEmpty()) {
                    urlSb.append("&to=").append(URLEncoder.encode(toLang, "UTF-8"));
                }
                if (!fromLang.isEmpty() && !"auto".equalsIgnoreCase(fromLang)) {
                    urlSb.append("&from=").append(URLEncoder.encode(fromLang, "UTF-8"));
                }

                URL url = new URL(urlSb.toString());
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Ocp-Apim-Subscription-Key", API_KEY);
                conn.setRequestProperty("Ocp-Apim-Subscription-Region", REGION);
                conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
                conn.setRequestProperty("Accept", "application/json");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(20000);
                conn.setDoOutput(true);

                // Body: [{ "text": "..." }]
                JSONArray body = new JSONArray();
                JSONObject item = new JSONObject();
                item.put("text", text);
                body.put(item);

                byte[] payload = body.toString().getBytes(StandardCharsets.UTF_8);
                try (OutputStream os = conn.getOutputStream()) {
                    os.write(payload);
                }

                int code = conn.getResponseCode();
                InputStream is = (code >= 200 && code < 300) ? conn.getInputStream() : conn.getErrorStream();

                StringBuilder resp = new StringBuilder();
                try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = br.readLine()) != null) resp.append(line);
                }

                if (code < 200 || code >= 300) {
                    error = "HTTP " + code + ": " + resp;
                    return null;
                }

                // Parse JSON: [ { "translations": [ { "text": "...", "to": "xx" } ] } ]
                JSONArray responseArray = new JSONArray(resp.toString());
                JSONObject first = responseArray.getJSONObject(0);
                JSONArray translations = first.getJSONArray("translations");
                if (translations.length() == 0) {
                    error = "No translation returned";
                    return null;
                }
                JSONObject translation = translations.getJSONObject(0);
                return translation.getString("text");

            } catch (Exception e) {
                error = e.getMessage();
                return null;
            } finally {
                if (conn != null) conn.disconnect();
            }
        }

        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                callback.onSuccess(result);
            } else {
                callback.onError(error != null ? error : "Translation failed");
            }
        }
    }
}
