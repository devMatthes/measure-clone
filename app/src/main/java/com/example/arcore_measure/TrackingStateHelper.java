package com.example.arcore_measure;

import com.google.ar.core.Camera;
import com.google.ar.core.TrackingFailureReason;

final class TrackingStateHelper {
    private static final String INSUFFICIENT_FEATURES_MESSAGE =
            "Can't find anything. Aim device at a surface with more texture or color.";
    private static final String EXCESSIVE_MOTION_MESSAGE =
            "Too fast. Move device slowly from left to right.";
    private static final String INSUFFICIENT_LIGHT_MESSAGE =
            "Too dark. Find a well-lit textured surface.";
    private static final String BAD_STATE_MESSAGE =
            "Tracking lost due to bad internal state. Please try restarting the app.";

    public static String getTrackingFailureReasonString(Camera camera) {
        TrackingFailureReason reason = camera.getTrackingFailureReason();
        switch(reason) {
            case NONE:
                return "";
            case BAD_STATE:
                return BAD_STATE_MESSAGE;
            case INSUFFICIENT_LIGHT:
                return INSUFFICIENT_LIGHT_MESSAGE;
            case EXCESSIVE_MOTION:
                return EXCESSIVE_MOTION_MESSAGE;
            case INSUFFICIENT_FEATURES:
                return INSUFFICIENT_FEATURES_MESSAGE;
        }
        return "Unknown tracking failure reason: " + reason;
    }
}
