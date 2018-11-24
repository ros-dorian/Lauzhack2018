package ch.bobsthack.bobsthack2018.tracker;

import android.content.Context;
import android.util.Log;

import com.google.ar.core.AugmentedImage;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.ViewRenderable;

import java.util.concurrent.CompletableFuture;

import ch.bobsthack.bobsthack2018.R;

public class AugmentedImageNode extends AnchorNode {

    private static final String TAG = "IMAGE_NODE";

    private AugmentedImage image;

    private static CompletableFuture<ViewRenderable> mCenter;

    public AugmentedImageNode(Context context) {
        if (mCenter == null) {
            mCenter = ViewRenderable.builder()
                            .setView(context, R.layout.tracker_layout)
                            .build();
        }
    }

    public void setImage(AugmentedImage image) {
        this.image = image;

        // If any of the models are not loaded, then recurse when all are loaded.
        if (!mCenter.isDone()) {
            CompletableFuture.allOf(mCenter)
                    .thenAccept((Void aVoid) -> setImage(image))
                    .exceptionally(
                            throwable -> {
                                Log.e(TAG, "Exception loading", throwable);
                                return null;
                            });
        }

        // Set the anchor based on the center of the image.
        setAnchor(image.createAnchor(image.getCenterPose()));

        // Make the 4 corner nodes.
        Vector3 localPosition = new Vector3();
        Node centerNode;

        // Upper left corner.
        localPosition.set(image.getExtentX(), 0.0f, image.getExtentZ());
        centerNode = new Node();
        centerNode.setParent(this);
        centerNode.setLocalPosition(localPosition);
        centerNode.setLookDirection(new Vector3(0, 0, 1));
        centerNode.setRenderable(mCenter.getNow(null));
    }

    public AugmentedImage getImage() {
        return image;
    }

    @Override
    public void onDeactivate() {
        super.onDeactivate();
    }
}
