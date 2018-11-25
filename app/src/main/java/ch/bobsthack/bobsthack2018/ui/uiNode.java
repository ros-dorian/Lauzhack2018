package ch.bobsthack.bobsthack2018.ui;

import android.content.Context;
import android.util.Log;
import android.view.MotionEvent;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.HitTestResult;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.NodeParent;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.Color;
import com.google.ar.sceneform.rendering.Material;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;

import ch.bobsthack.bobsthack2018.R;

public class uiNode extends Node {

    private static final String TAG = "IMAGE_NODE";

    private Context mContext;

    private CompletableFuture<ViewRenderable> mLayout;

    public uiNode(Context context, int layout) {
        mContext = context;

        mLayout = ViewRenderable.builder()
                .setView(context, layout)
                .build();
    }

    public void setPosition(Vector3 relativePosition, Node parent) {

        if (!mLayout.isDone()) {
            CompletableFuture.allOf(mLayout)
                    .thenAccept((Void aVoid) -> setPosition(relativePosition, parent))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
        }

        setParent(parent);

        setLocalPosition(relativePosition);

        setLocalRotation(Quaternion.axisAngle(new Vector3(1, 0, 0), -90));

        setRenderable(mLayout.getNow(null));



    }
}
