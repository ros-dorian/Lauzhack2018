package ch.bobsthack.bobsthack2018;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.ar.core.AugmentedImage;
import com.google.ar.core.Frame;
import com.google.ar.core.TrackingState;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.ux.ArFragment;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import ch.bobsthack.bobsthack2018.tracker.AugmentedImageNode;

public class MainActivity extends AppCompatActivity {

    public final static int FACE_RIGHT = 0;
    public final static int FACE_FRONT = 1;
    public final static int FACE_TOP = 2;

    private ArFragment mArFragment;
    private ImageView mFitToScanView;

    private Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();
    private Map<AugmentedImage, Vector3> facePositions = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mArFragment = (ArFragment)getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        mFitToScanView = findViewById(R.id.image_view_fit_to_scan);

        mArFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

        new Thread(() -> {
            while(true) {
                Data data = getData();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // setInfo(data); TODO
                    }
                });
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }}
        ).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (augmentedImageMap.isEmpty()) {
            mFitToScanView.setVisibility(View.VISIBLE);
        }
    }

    private void onUpdateFrame(FrameTime frameTime) {
        Frame frame = mArFragment.getArSceneView().getArFrame();

        // If there is no frame or ARCore is not tracking yet, just return.
        if (frame == null || frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
            return;
        }

        Collection<AugmentedImage> updatedAugmentedImages = frame.getUpdatedTrackables(AugmentedImage.class);
        for (AugmentedImage augmentedImage : updatedAugmentedImages) {
            switch (augmentedImage.getTrackingState()) {
                case PAUSED:
                    // When an image is in PAUSED state, but the camera is not PAUSED, it has been detected,
                    // but not yet tracked.
                    String text = "Detected Image " + augmentedImage.getIndex();
                    Toast.makeText(this, text, Toast.LENGTH_LONG);
                    break;

                case TRACKING:
                    // Have to switch to UI Thread to update View.
                    mFitToScanView.setVisibility(View.GONE);

                    // Create a new anchor for newly found images.
                    if (!augmentedImageMap.containsKey(augmentedImage)) {
                        AugmentedImageNode node = new AugmentedImageNode(this);
                        node.setImage(augmentedImage);
                        augmentedImageMap.put(augmentedImage, node);
                        mArFragment.getArSceneView().getScene().addChild(node);

                        Vector3 position = null;
                        switch(augmentedImage.getIndex()) {
                            case 0: position = new Vector3(FACE_FRONT, -1/6f, 1f);
                            case 1: position = new Vector3(FACE_FRONT, 1/6f, 0.5f);
                            case 2: position = new Vector3(FACE_FRONT, -1f, 0.5f);
                            case 3: position = new Vector3(FACE_RIGHT, 0, 0);
                            case 4: position = new Vector3(FACE_TOP, 2/6f, 0);
                        }
                        if(position != null) {
                            facePositions.put(augmentedImage, position);
                        }

                        Vector3[] centers = calculateCenters();
                    }
                    break;

                case STOPPED:
                    augmentedImageMap.remove(augmentedImage);
                    facePositions.remove(augmentedImage);
                    break;
            }
        }
    }

    private Vector3[] calculateCenters() {
        Vector3[] result = new Vector3[3];

        List<Pair<Vector3, Vector3>> availablePositions = new ArrayList<>();
        for(Map.Entry<AugmentedImage, AugmentedImageNode> entry : augmentedImageMap.entrySet()) {
            availablePositions.add(new Pair(entry.getValue().getWorldPosition(), facePositions.get(entry.getKey())));
        }

        for(Pair<Vector3, Vector3> item : availablePositions) {

        }

        return result;
    }

    public Data getData() {
        //JSon query
        String url = "http://www.duggan.ch/~akv_lauzhack/mastercut.php?MachineName=MasterCut";
        JsonQuery jsonQuery = new JsonQuery();
        JSONObject json = null;
        try {
            json = jsonQuery.getJson(url).get();
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        if (json != null)
            Log.i("Lauzhack", json.toString());

        Data data = null;

        try {
            if (json != null)
                data = new Data(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return data;
    }
}
