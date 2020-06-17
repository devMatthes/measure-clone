package com.example.arcore_measure;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.ar.core.Anchor;
import com.google.ar.core.ArCoreApk;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Session;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.UnavailableApkTooOldException;
import com.google.ar.core.exceptions.UnavailableArcoreNotInstalledException;
import com.google.ar.core.exceptions.UnavailableDeviceNotCompatibleException;
import com.google.ar.core.exceptions.UnavailableSdkTooOldException;
import com.google.ar.core.exceptions.UnavailableUserDeclinedInstallationException;
import com.google.ar.sceneform.AnchorNode;
import com.google.ar.sceneform.Camera;
import com.google.ar.sceneform.FrameTime;
import com.google.ar.sceneform.Node;
import com.google.ar.sceneform.Scene;
import com.google.ar.sceneform.Sun;
import com.google.ar.sceneform.math.Quaternion;
import com.google.ar.sceneform.math.Vector3;
import com.google.ar.sceneform.rendering.MaterialFactory;
import com.google.ar.sceneform.rendering.ModelRenderable;
import com.google.ar.sceneform.rendering.ShapeFactory;
import com.google.ar.sceneform.rendering.Texture;
import com.google.ar.sceneform.rendering.ViewRenderable;
import com.google.ar.sceneform.ux.ArFragment;
import com.google.ar.sceneform.ux.TransformableNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import static android.graphics.Color.*;
import static com.google.ar.sceneform.math.Vector3.zero;

public class MainActivity extends AppCompatActivity implements Scene.OnUpdateListener {
    private static final double MIN_OPENGL_VERSION = 3.0;
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String SEARCHING_PLANE_MESSAGE = "Searching for surfaces...";

    // Set to true ensures requestInstall() triggers installation if necessary.
    private final SnackbarHelper messageSnackbarHelper = new SnackbarHelper();
    private boolean mUserRequestedInstall = true;
    private Session mSession;
    private ArFragment arFragment;
    private ArrayList<AnchorNode> currentAnchorNode = new ArrayList<>();
    private ArrayList<AnchorNode> labelArray = new ArrayList<>();
    private AnchorNode anchorNodeTemp;
    private ArrayList<Anchor> currentAnchor = new ArrayList<>();
    private float feet = 0, inches = 0;
    private String stringMeasure;
    private ModelRenderable pointRender, aimRender, widthLineRender, heightLineRender;
    public static Dialog dialog;
    public static Dialog dialogSave;
    private MeasurementViewModel measurementViewModel;
    public static Dialog dialogSurfValue;
    private Button btnSave;
    private Vector3 difference;
    private View initInfo;

    @SuppressLint("ClickableViewAccessibility")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkIsSupportedDeviceOrFinish(this)) {
            Toast.makeText(getApplicationContext(), "Device not supported", Toast.LENGTH_LONG).show();
        }
        setContentView(R.layout.activity_main);
        arFragment = (ArFragment) getSupportFragmentManager().findFragmentById(R.id.ux_fragment);
        arFragment.getPlaneDiscoveryController().hide();

        ViewGroup container = findViewById(R.id.sceneform_hand_layout);
        container.removeAllViews();

        initInfo = getLayoutInflater().inflate(R.layout.initinfo_layout, container, true);
        arFragment.getPlaneDiscoveryController().setInstructionView(initInfo);

        btnSave = findViewById(R.id.btnSave);
        btnSave.setOnClickListener(v -> showSavedRecs(MainActivity.this));

        initModel();
        arFragment.setOnTapArPlaneListener(this::refreshAim);
        measurementViewModel = new ViewModelProvider(this).get(MeasurementViewModel.class);
    }

    public boolean checkIsSupportedDeviceOrFinish(final Activity activity) {
        String openGlVersionString = ((ActivityManager) Objects.requireNonNull(activity.getSystemService(Context.ACTIVITY_SERVICE)))
                .getDeviceConfigurationInfo()
                .getGlEsVersion();
        if (Double.parseDouble(openGlVersionString) < MIN_OPENGL_VERSION) {
            Log.e(TAG, "Sceneform requires OpenGL 3.0 or later");
            Toast.makeText(activity, "Sceneform requires OpenGL 3.0 or later", Toast.LENGTH_LONG)
                    .show();
            activity.finish();
            return false;
        }
        return true;
    }
    //Rendering aim, lines
    private void initModel() {
        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(WHITE))
                .thenAccept(material -> {
                    pointRender = ShapeFactory.makeCylinder(0.018f, 0.0001f, zero(), material);
                    pointRender.setShadowCaster(false);
                    pointRender.setShadowReceiver(false);
                });

        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(WHITE))
                .thenAccept(material -> {
                    heightLineRender = ShapeFactory.makeCylinder(0.01f, 0.01f, zero(), material);
                    heightLineRender.setShadowCaster(false);
                    heightLineRender.setShadowReceiver(false);
                });

        Texture.builder()
                .setSource(getApplicationContext(), R.drawable.aim)
                .build().
                thenAccept(texture -> {
                    MaterialFactory.makeTransparentWithTexture(getApplicationContext(), texture)
                            .thenAccept(material -> {
                                aimRender = ShapeFactory.makeCylinder(0.08f, 0f, zero(), material);
                                aimRender.setShadowCaster(false);
                                aimRender.setShadowReceiver(false);
                            });
                });

        MaterialFactory.makeOpaqueWithColor(this, new com.google.ar.sceneform.rendering.Color(WHITE))
                .thenAccept(material -> {
                    widthLineRender = ShapeFactory.makeCube(new Vector3(.015f, 0, 1f), zero(), material);
                    widthLineRender.setShadowCaster(false);
                    widthLineRender.setShadowReceiver(false);
                });
    }

    //Rendering labels
    void initTextBox(float meters, TransformableNode tN) {
        ViewRenderable.builder()
                .setView(this, R.layout.distance)
                .build()
                .thenAccept(renderable -> {
                    renderable.setShadowCaster(false);
                    renderable.setShadowReceiver(false);
                    renderable.setVerticalAlignment(ViewRenderable.VerticalAlignment.BOTTOM);
                    TextView distanceInMeters = (TextView) renderable.getView();
                    String metersString;
                    if (meters < 1f)
                        metersString = String.format(Locale.ENGLISH, "%.0f", meters*100) + " cm";
                    else
                        metersString = String.format(Locale.ENGLISH, "%.2f", meters) + " m";
                    distanceInMeters.setText(metersString);
                    tN.setRenderable(renderable);
                });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // ARCore requires camera permission to operate.
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            CameraPermissionHelper.requestCameraPermission(this);
            return;
        }
        try {
            if (mSession == null) {
                switch (ArCoreApk.getInstance().requestInstall(this, mUserRequestedInstall)) {
                    case INSTALLED:
                        // Success, create the AR session.
                        mSession = new Session(this);
                        break;
                    case INSTALL_REQUESTED:
                        // Ensures next invocation of requestInstall() will either return
                        // INSTALLED or throw an exception.
                        mUserRequestedInstall = false;
                        return;
                }
            }
        } catch (UnavailableUserDeclinedInstallationException | UnavailableDeviceNotCompatibleException e) {
            // Display an appropriate message to the user and return gracefully.
            Toast.makeText(this, "TODO: handle exception " + e, Toast.LENGTH_LONG)
                    .show();
            return;
        } catch (UnavailableArcoreNotInstalledException e) {
            e.printStackTrace();
        } catch (UnavailableSdkTooOldException e) {
            e.printStackTrace();
        } catch (UnavailableApkTooOldException e) {
            e.printStackTrace();
        }
        return;  // mSession is still null.
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        if (!CameraPermissionHelper.hasCameraPermission(this)) {
            Toast.makeText(this, "Camera permission is needed to run this application", Toast.LENGTH_LONG)
                    .show();
            if (!CameraPermissionHelper.shouldShowRequestPermissionRationale(this)) {
                // Permission denied with checking "Do not ask again".
                CameraPermissionHelper.launchPermissionSettings(this);
            }
            finish();
        }
    }

    @Override
    public void onUpdate(FrameTime frameTime) {
        labelsRotation();
        touchScreenCenterConstantly();
        try {
            Frame frame = mSession.update();
            com.google.ar.core.Camera camera = frame.getCamera();

            if (camera.getTrackingState() == TrackingState.PAUSED) {
                messageSnackbarHelper.showMessage(this, TrackingStateHelper.getTrackingFailureReasonString(camera));
                return;
            }

            if (!hasTrackingPlane()) {
                messageSnackbarHelper.showMessage(this, SEARCHING_PLANE_MESSAGE);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Exception", t);
        }
    }

    public void clearAnchors(View view) {
        List<Node> children = new ArrayList<>(arFragment.getArSceneView().getScene().getChildren());
        for (Node node : children) {
            if (node instanceof AnchorNode) {
                if (((AnchorNode) node).getAnchor() != null) {
                    ((AnchorNode) node).getAnchor().detach();
                    node.setParent(null);
                    node.setRenderable(null);
                }
            }
            if (!(node instanceof Camera) && !(node instanceof Sun)) {
                node.setParent(null);
            }
        }
        currentAnchorNode.clear();
        currentAnchor.clear();
        labelArray.clear();
    }

    private void labelsRotation() {
        Vector3 cameraPosition = arFragment.getArSceneView().getScene().getCamera().getWorldPosition();
        if (labelArray != null) {
            for (AnchorNode labelNode : labelArray) {
                Vector3 labelPosition = labelNode.getWorldPosition();
                Vector3 direction = Vector3.subtract(cameraPosition, labelPosition);
                Quaternion lookRotation = Quaternion.lookRotation(direction, Vector3.up());
                labelNode.setWorldRotation(lookRotation);
            }
        }
    }

    //Refreshing the aim (simulating move on device screen)
    private void refreshAim(HitResult hitResult, Plane plane, MotionEvent motionEvent) {
        if (aimRender == null)
            return;

        if (motionEvent.getMetaState() == 0) {
            if (anchorNodeTemp != null)
                anchorNodeTemp.getAnchor().detach();

            Anchor anchor = hitResult.createAnchor();
            AnchorNode anchorNode = new AnchorNode(anchor);
            anchorNode.setParent(arFragment.getArSceneView().getScene());
            TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
            transformableNode.setRenderable(aimRender);
            transformableNode.setParent(anchorNode);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this);
            arFragment.getArSceneView().getScene().addChild(anchorNode);
            anchorNodeTemp = anchorNode;
        }
    }

    //Adding points, lines and labels
    public void addFromAim(View view) {
        if (anchorNodeTemp != null) {
            Vector3 worldPosition = anchorNodeTemp.getWorldPosition();
            Quaternion worldRotation = anchorNodeTemp.getWorldRotation();
            worldPosition.x += 0.0000001f;
            AnchorNode confirmedAnchorNode = new AnchorNode();
            confirmedAnchorNode.setWorldPosition(worldPosition);
            confirmedAnchorNode.setWorldRotation(worldRotation);
            Anchor anchor = confirmedAnchorNode.getAnchor();
            confirmedAnchorNode.setParent(arFragment.getArSceneView().getScene());
            TransformableNode transformableNode = new TransformableNode(arFragment.getTransformationSystem());
            transformableNode.setRenderable(pointRender);
            transformableNode.setParent(confirmedAnchorNode);
            arFragment.getArSceneView().getScene().addOnUpdateListener(this);
            arFragment.getArSceneView().getScene().addChild(confirmedAnchorNode);
            currentAnchor.add(anchor);
            currentAnchorNode.add(confirmedAnchorNode);
            if (currentAnchorNode.size() >= 2) {
                Vector3 node1Pos = currentAnchorNode.get(currentAnchorNode.size() - 2).getWorldPosition();
                Vector3 node2Pos = currentAnchorNode.get(currentAnchorNode.size() - 1).getWorldPosition();
                difference = Vector3.subtract(node1Pos, node2Pos);
                stringMeasure = Float.toString(Math.round(difference.length()*100));
                final Quaternion rotationFromAToB =
                        Quaternion.lookRotation(difference.normalized(), Vector3.up());
                //Setting lines connecting nodes
                AnchorNode lineBetween = new AnchorNode();
                lineBetween.setParent(arFragment.getArSceneView().getScene());
                lineBetween.setWorldPosition(Vector3.add(node1Pos, node2Pos).scaled(.5f));
                lineBetween.setWorldRotation(rotationFromAToB);
                lineBetween.setLocalScale(new Vector3(1f, 1f, difference.length()));
                TransformableNode lineNode = new TransformableNode(arFragment.getTransformationSystem());
                lineNode.setParent(lineBetween);
                lineNode.setRenderable(widthLineRender);
                //Setting length labels
                AnchorNode lengthLabel = new AnchorNode();
                lengthLabel.setParent(arFragment.getArSceneView().getScene());
                lengthLabel.setWorldPosition(Vector3.add(node1Pos, node2Pos).scaled(.5f));
                TransformableNode distanceNode = new TransformableNode(arFragment.getTransformationSystem());
                distanceNode.setParent(lengthLabel);
                initTextBox(difference.length(), distanceNode);
                labelArray.add(lengthLabel);
                showMeasureDialog(MainActivity.this);
            }
        }
    }

    //Simulating touches on center of device screen
    private void touchScreenCenterConstantly() {
        long downTime = SystemClock.uptimeMillis();
        long eventTime = SystemClock.uptimeMillis() + 10;

        float x = (float)(this.getResources().getDisplayMetrics().widthPixels) / 2;
        float y = (float)(this.getResources().getDisplayMetrics().heightPixels) / 2;
        MotionEvent motionEvent = MotionEvent.obtain(
                downTime,
                eventTime,
                MotionEvent.ACTION_UP,
                x,
                y,
                0
        );
        arFragment.getArSceneView().dispatchTouchEvent(motionEvent);
    }

    private void convertCMtoFtInch() {
        feet = (float) ((Math.round(0.0328*difference.length()*100)) / 12);
        inches = (float) ((Math.round(0.0328*difference.length()*100)) % 12);
    }

    //Dialog with info about measure
    public void  showMeasureDialog (Activity activity){
        dialogSurfValue = new Dialog(activity);
        dialogSurfValue.setCancelable(false);
        dialogSurfValue.setContentView(R.layout.dialog_measure);
        dialogSurfValue.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        TextView textViewMeasureValue = dialogSurfValue.findViewById(R.id.resultMeasure);
        convertCMtoFtInch();
        textViewMeasureValue.setText(stringMeasure + " cm");
        String stringFeet = Float.toString(feet);
        String stringInches = Float.toString(inches);
        TextView textViewMeasureToFeetInch = dialogSurfValue.findViewById(R.id.resultFI);
        textViewMeasureToFeetInch.setText(stringFeet + " ft " + stringInches + " in");
        TextView textViewMeasureToFeet = dialogSurfValue.findViewById(R.id.resultF);
        textViewMeasureToFeet.setText(stringFeet + " ft");
        Button btnOk = dialogSurfValue.findViewById(R.id.btnExitMeasure);
        btnOk.setOnClickListener(v -> dialogSurfValue.dismiss());

        Button btnSave = dialogSurfValue.findViewById(R.id.btnSaveMeasure);
        btnSave.setOnClickListener(v -> {
            dialogSurfValue.dismiss();
            showSaveDialog(MainActivity.this);
        });
        dialogSurfValue.show();
    }

    //Dialog with save option
    public void showSaveDialog (Activity activity){
        dialogSave = new Dialog(activity);
        dialogSave.setCancelable(false);
        dialogSave.setContentView(R.layout.dialog_save);
        dialogSave.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        Button btnExit = dialogSave.findViewById(R.id.btndialogExit);
        btnExit.setOnClickListener(v -> dialogSave.dismiss());

        Button btnSave = dialogSave.findViewById(R.id.btndialogSave);
        EditText nameSurf = dialogSave.findViewById(R.id.editTextNameSufr);
        TextView textViewSurfValue = dialogSave.findViewById(R.id.surfValueTextView);
        textViewSurfValue.setText(stringMeasure + " cm");

        btnSave.setOnClickListener(v -> {
            Measurements measure = new Measurements(nameSurf.getText().toString(), Float.valueOf(stringMeasure));
            measurementViewModel.insert(measure);
            showSavedRecs(MainActivity.this);
            dialogSave.dismiss();
        });
        dialogSave.show();
    }

    //Dialog with saved records
    public void showSavedRecs (Activity activity){
        dialog = new Dialog(activity);
        dialog.setCancelable(false);
        dialog.setContentView(R.layout.dialog_recycler);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        RecyclerView recyclerView = dialog.findViewById(R.id.recycler);
        final MeasurementListAdapter adapter = new MeasurementListAdapter(this);
        recyclerView.setAdapter(adapter);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        measurementViewModel.getAllMeasurements().observe(this, measurements -> adapter.setMeasurements(measurements));
        recyclerView.setOnClickListener(v -> {});

        Button btnOK = dialog.findViewById(R.id.btnOK);
        btnOK.setOnClickListener(v -> dialog.dismiss());

        Button btnClear = dialog.findViewById(R.id.btnClear);
        btnClear.setOnClickListener(v -> measurementViewModel.clear());

        dialog.show();
    }
    //Checking if any plane was tracked (detected)
    private boolean hasTrackingPlane() {
        for (Plane plane : mSession.getAllTrackables(Plane.class)) {
            if (plane.getTrackingState() == TrackingState.TRACKING) {
                return true;
            }
        }
        return false;
    }
}
