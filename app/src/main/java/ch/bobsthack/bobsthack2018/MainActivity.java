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
import ch.bobsthack.bobsthack2018.tracker.CenterNode;
import ch.bobsthack.bobsthack2018.ui.uiNode;

public class MainActivity extends AppCompatActivity {

    private ArFragment mArFragment;
    private ImageView mFitToScanView;

    private CenterNode mCenterRight;
    private CenterNode mCenterTop;
    private CenterNode mCenterFront;

    private Map<AugmentedImage, AugmentedImageNode> augmentedImageMap = new HashMap<>();
    private Map<AugmentedImage, TrackPointData> facePositions = new HashMap<>();

    private final static float BOX_WIDTH = 0.5f;
    private final static float BOX_HEIGHT = 0.3f;
    private final static float BOX_SIDE = 0.1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mArFragment = (ArFragment)getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        mFitToScanView = findViewById(R.id.image_view_fit_to_scan);

        mArFragment.getArSceneView().getScene().addOnUpdateListener(this::onUpdateFrame);

        mCenterRight = null;
        mCenterTop = null;
        mCenterFront = null;

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

                        uiNode mainUi = new uiNode(this, R.layout.main_ui);

                        mainUi.setPosition(new Vector3(0,0,0),node);

                        Vector3 position = null;

                        TrackPointData pointData = null;
                        switch(augmentedImage.getIndex()) {
                            case 0: pointData = new TrackPointData(node.getWorldPosition(), BoxSide.RIGHT, -0.4f, 1f); break;
                            case 1: pointData = new TrackPointData(node.getWorldPosition(), BoxSide.RIGHT, -0.2f, -0.5f); break;
                            case 2: pointData = new TrackPointData(node.getWorldPosition(), BoxSide.RIGHT, -0.2f, -0.9f); break;
                            case 3: pointData = new TrackPointData(node.getWorldPosition(), BoxSide.FRONT, 0, 0); break;
                            case 4: pointData = new TrackPointData(node.getWorldPosition(), BoxSide.TOP, 4f, 0); break;
                        }
                        if(pointData != null) {
                            facePositions.put(augmentedImage, pointData);
                        }
                    }
                    break;

                case STOPPED:
                    augmentedImageMap.remove(augmentedImage);
                    facePositions.remove(augmentedImage);
                    break;
            }
        }

        Vector3[] centers = calculateCenterPosition();
        for(int i = 0; i < centers.length; i++) {
            if(centers[i] != null) {
                switch(i) {
                    case 0:
                        if(mCenterRight != null) {
                            mArFragment.getArSceneView().getScene().onRemoveChild(mCenterRight);
                        }
                        mCenterRight = new CenterNode(this);
                        mCenterRight.setPosition(centers[i]);
                        mArFragment.getArSceneView().getScene().addChild(mCenterRight);
                        break;
                    case 1:
                        if(mCenterTop != null) {
                            mArFragment.getArSceneView().getScene().onRemoveChild(mCenterTop);
                        }
                        mCenterTop = new CenterNode(this);
                        mCenterTop.setPosition(centers[i]);
                        mArFragment.getArSceneView().getScene().addChild(mCenterTop);
                        break;
                    case 2:
                        if(mCenterFront != null) {
                            mArFragment.getArSceneView().getScene().onRemoveChild(mCenterFront);
                        }
                        mCenterFront = new CenterNode(this);
                        mCenterFront.setPosition(centers[i]);
                        mArFragment.getArSceneView().getScene().addChild(mCenterFront);
                        break;
                }
            }
        }
    }

    private Vector3[] calculateCenterPosition() {
        Vector3[] result = new Vector3[3];

        for(Map.Entry<AugmentedImage, TrackPointData> item : facePositions.entrySet()) {
            TrackPointData point = item.getValue();
            AugmentedImageNode imageNode = augmentedImageMap.get(item.getKey());
            int side = point.getBoxSide().ordinal();
            Vector3 xVector;
            Vector3 yVector;
            switch(side) {
                case 0:
                    xVector = imageNode.getForward().normalized();
                    yVector = imageNode.getRight().normalized();
                    if(result[side] == null) {
                        xVector = xVector.scaled(BOX_WIDTH * point.getSideX());
                        yVector = yVector.scaled(BOX_HEIGHT * point.getSideY());
                        result[side] = Vector3.add(point.getWorldPosition(), Vector3.add(xVector, yVector));
                    } else {
                        xVector = xVector.scaled(BOX_WIDTH * point.getSideX());
                        yVector = yVector.scaled(BOX_HEIGHT * point.getSideY());
                        result[side] = Vector3.lerp(
                                result[side],
                                Vector3.add(point.getWorldPosition(), Vector3.add(xVector, yVector)),
                                0.5f);
                    }
                    break;
                case 1:
                    xVector = imageNode.getRight().normalized();
                    yVector = imageNode.getForward().normalized();
                    if(result[side] == null) {
                        xVector = xVector.scaled(BOX_SIDE * point.getSideX());
                        yVector = yVector.scaled(BOX_HEIGHT * point.getSideY());
                        result[side] = Vector3.add(point.getWorldPosition(), Vector3.add(xVector, yVector));
                    } else {
                        xVector = xVector.scaled(BOX_SIDE * point.getSideX());
                        yVector = yVector.scaled(BOX_HEIGHT * point.getSideY());
                        result[side] = Vector3.lerp(
                                result[side],
                                Vector3.add(point.getWorldPosition(), Vector3.add(xVector, yVector)),
                                0.5f);
                    }
                    break;
                case 2:
                    xVector = imageNode.getRight().normalized();
                    yVector = imageNode.getForward().normalized();
                    if(result[side] == null) {
                        xVector = xVector.scaled(BOX_WIDTH * point.getSideX());
                        yVector = yVector.scaled(BOX_SIDE * point.getSideY());
                        result[side] = Vector3.add(point.getWorldPosition(), Vector3.add(xVector, yVector));
                    } else {
                        xVector = xVector.scaled(BOX_WIDTH * point.getSideX());
                        yVector = yVector.scaled(BOX_SIDE * point.getSideY());
                        result[side] = Vector3.lerp(
                                result[side],
                                Vector3.add(point.getWorldPosition(), Vector3.add(xVector, yVector)),
                                0.5f);
                    }
                    break;
            }
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
