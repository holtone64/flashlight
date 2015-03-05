package cirestudio.flashlight;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private CameraManager mManager;
    private CameraDevice mCamera;
    private String[] mCameraList;
    private String mCameraId;
    private CameraDevice.StateCallback mCallback;
    private Handler mHandler;
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private CameraCaptureSession mSession;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private List<Surface> mSurfaceList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            setContentView(R.layout.activity_main);

            mCallback = new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    try {
                        mCaptureRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);

                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
                        mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);

                        // Even though we will not be showing any pictures, createCaptureSession requires a list of surfaces
                        // We will create a list and add a single surface to it
                        // the surfaces and list will be saved as member variables, since these same surfaces
                        // will be used while the camera is open.
                        List surfaceList = new ArrayList<Surface>();

                        // The int required by SurfaceTexture is just an ID.  since we only need one, i picked something unique
                        mSurfaceTexture = new SurfaceTexture(42);

                        // Now, we ned to determine the smallest output size available to the camera device since we want the
                        // smallest surface possible.
                        Size[] outputSizes = mManager.getCameraCharacteristics(mCameraId)
                                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                                .getOutputSizes(SurfaceTexture.class);

                        Size smallestOutputSize = outputSizes[0];
                        for (Size i : outputSizes) {
                            if (smallestOutputSize.getWidth() >= i.getWidth() && smallestOutputSize.getHeight() >= i.getHeight()) {
                                smallestOutputSize = i;
                            }
                        }

                        mSurfaceTexture.setDefaultBufferSize(smallestOutputSize.getWidth(), smallestOutputSize.getHeight());
                        mSurface = new Surface(mSurfaceTexture);
                        surfaceList.add(mSurface);
                        mCaptureRequestBuilder.addTarget(mSurface);

                        CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                mSession = session;
                                try {
                                    // Need this to avoid having to send too many capture requests
                                    // should hold the 'preview' in place
                                    mSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
                                } catch (CameraAccessException e) {
                                    e.printStackTrace();
                                }
                            }

                            @Override
                            public void onConfigureFailed(CameraCaptureSession session) {

                            }
                        };

                        camera.createCaptureSession(surfaceList, stateCallback, mHandler);


                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onDisconnected(CameraDevice camera) {

                }

                @Override
                public void onError(CameraDevice camera, int error) {

                }
            };

            // Why on earth do we need a handler to open the camera??
            mHandler = new Handler();

            mManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                mCameraList = mManager.getCameraIdList();

                for (String idString : mCameraList) {
                    Integer m = mManager.getCameraCharacteristics(idString).get(CameraCharacteristics.LENS_FACING);

                    if ((mManager.getCameraCharacteristics(idString).get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK) &&
                            (mManager.getCameraCharacteristics(idString).get(CameraCharacteristics.FLASH_INFO_AVAILABLE))) {
                        mCameraId = idString;
                        mManager.openCamera(mCameraId, mCallback, mHandler);
                        break;
                    }
                }

            } catch (CameraAccessException e) {
                e.printStackTrace();
            }

            if (mCameraId == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder
                        .setMessage("Device does not support Camera flash")
                        .setCancelable(false)
                        .setNeutralButton("Ok.", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                MainActivity.this.finish();
                            }
                        });
                AlertDialog error = builder.create();
                error.show();
            }
            ActionBar actionBar = getSupportActionBar();

        }

        else {

            Context context = this.getApplicationContext();
            if ( context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH ) ) {
                // This works with deprecated android.hardware.Camera class
                Camera camera = Camera.open();
                Camera.Parameters p = camera.getParameters();
                p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                camera.setParameters(p);
                camera.startPreview();

                ActionBar actionBar = getSupportActionBar();

            }
        }

    }




    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
