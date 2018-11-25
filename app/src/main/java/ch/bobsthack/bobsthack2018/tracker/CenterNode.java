package ch.bobsthack.bobsthack2018.tracker;

import android.content.Context;
import android.util.Log;

import com.google.ar.core.Anchor;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;

import ch.bobsthack.bobsthack2018.R;

public class CenterNode extends Node {

    private static final String TAG = "CENTER_NODE";

    private Context mContext;

    private static CompletableFuture<ViewRenderable> mPointer;

    public CenterNode(Context context) {
        mContext = context;

        if (mPointer == null) {
            mPointer = ViewRenderable.builder()
                    .setView(context, R.layout.tracker_layout)
                    .build();
        }
    }

    public void setPosition(Vector3 worldPosition) {
        // If any of the models are not loaded, then recurse when all are loaded.
        if (!mPointer.isDone()) {
            CompletableFuture.allOf(mPointer)
                    .thenAccept((Void aVoid) -> setPosition(worldPosition))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
        }

        setWorldPosition(worldPosition);
        //setRenderable(mPointer.getNow(null));
    }
}
