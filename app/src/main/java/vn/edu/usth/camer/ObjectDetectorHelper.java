package vn.edu.usth.camer;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import com.google.mediapipe.framework.image.BitmapImageBuilder;
import com.google.mediapipe.framework.image.MPImage;
import com.google.mediapipe.tasks.core.BaseOptions;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetector;
import com.google.mediapipe.tasks.vision.core.RunningMode;
import com.google.mediapipe.tasks.vision.objectdetector.ObjectDetectorResult;

public class ObjectDetectorHelper {
    private static final String TAG = "ObjectDetectorHelper";
    private ObjectDetector objectDetector;
    private Context context;

    public ObjectDetectorHelper(Context context) {
        this.context = context;
    }

    public void initialize() {
        try {
            BaseOptions baseOptions = BaseOptions.builder()
                    .setModelAssetPath("model.tflite")
                    .build();

            ObjectDetector.ObjectDetectorOptions options =
                    ObjectDetector.ObjectDetectorOptions.builder()
                            .setBaseOptions(baseOptions)
                            .setMaxResults(1)
                            .setScoreThreshold(0.5f)
                            .setRunningMode(RunningMode.IMAGE)
                            .build();

            objectDetector = ObjectDetector.createFromOptions(context, options);
            Log.d(TAG, "ObjectDetector initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing ObjectDetector: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public String detectObject(Bitmap bitmap) {
        if (objectDetector == null) {
            initialize();
        }

        if (objectDetector == null) {
            return "Detector not available";
        }

        try {
            MPImage mpImage = new BitmapImageBuilder(bitmap).build();
            ObjectDetectorResult result = objectDetector.detect(mpImage);

            if (result.detections() != null && result.detections().size() > 0) {
                String label = result.detections().get(0).categories().get(0).categoryName();
                float score = result.detections().get(0).categories().get(0).score();
                Log.d(TAG, "Detected: " + label + " (score: " + score + ")");
                return label;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error detecting object: " + e.getMessage());
            e.printStackTrace();
        }
        return "No object detected";
    }

    public void close() {
        if (objectDetector != null) {
            objectDetector.close();
            objectDetector = null;
        }
    }
}