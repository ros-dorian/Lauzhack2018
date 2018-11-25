package ch.bobsthack.bobsthack2018;

import com.google.ar.sceneform.math.Vector3;

public class TrackPointData {

    private Vector3 mWorldPosition;
    private BoxSide mSide;
    private float mSideX;
    private float mSideY;

    public TrackPointData(Vector3 worldPosition, BoxSide side, float sideX, float sideY) {
        mWorldPosition = worldPosition;
        mSide = side;
        mSideX = sideX;
        mSideY = sideY;
    }

    public Vector3 getWorldPosition() {
        return mWorldPosition;
    }

    public BoxSide getBoxSide() {
        return mSide;
    }

    public float getSideX() {
        return mSideX;
    }

    public float getSideY() {
        return mSideY;
    }
}
