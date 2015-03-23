package cirestudio.flashlight;

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
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Size;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.WindowManager;
import android.widget.Switch;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends ActionBarActivity {

    private CameraManager mCameraManager;
    private String[] mCameraList;
    private String mCameraId;
    private CameraDevice.StateCallback mCallback;
    private Handler mHandler;
    private CaptureRequest.Builder mBuilder;
    private CameraCaptureSession mSession;
    private SurfaceTexture mSurfaceTexture;
    private Surface mSurface;
    private List<Surface> mSurfaceList;
    private Switch mSwitch;
    private Camera.Parameters parameters;
    private Camera mCamera;
    private CameraDevice mCameraDevice;
    private boolean ledSwitch;
    private boolean backlightSwitch;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if ( Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP ) {
            setContentView(R.layout.activity_main);

//            mCallback = new CameraDevice.StateCallback() {
//                @Override
//                public void onOpened( CameraDevice mCamera ) {
//                    try {
//                        mCaptureRequestBuilder = mCamera.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
//
//                        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AE_MODE_ON);
//                        mCaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
//
//                        // Even though we will not be showing any pictures, createCaptureSession requires a list of surfaces
//                        // We will create a list and add a single surface to it
//                        // the surfaces and list will be saved as member variables, since these same surfaces
//                        // will be used while the mCamera is open.
//                        List surfaceList = new ArrayList<Surface>();
//
//                        // The int required by SurfaceTexture is just an ID.  since we only need one, i picked something unique
//                        mSurfaceTexture = new SurfaceTexture(42);
//
//                        // Now, we ned to determine the smallest output size available to the mCamera device since we want the
//                        // smallest surface possible.
//                        Size[] outputSizes = mCameraManager.getCameraCharacteristics(mCameraId)
//                                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
//                                .getOutputSizes(SurfaceTexture.class);
//
//                        Size smallestOutputSize = outputSizes[0];
//                        for (Size i : outputSizes) {
//                            if (smallestOutputSize.getWidth() >= i.getWidth() && smallestOutputSize.getHeight() >= i.getHeight()) {
//                                smallestOutputSize = i;
//                            }
//                        }
//
//                        mSurfaceTexture.setDefaultBufferSize(smallestOutputSize.getWidth(), smallestOutputSize.getHeight());
//                        mSurface = new Surface(mSurfaceTexture);
//                        surfaceList.add(mSurface);
//                        mCaptureRequestBuilder.addTarget(mSurface);
//
//                        CameraCaptureSession.StateCallback stateCallback = new CameraCaptureSession.StateCallback() {
//                            @Override
//                            public void onConfigured(CameraCaptureSession session) {
//                                mSession = session;
//                                try {
//                                    // Need this to avoid having to send too many capture requests
//                                    // should hold the 'preview' in place
//                                    mSession.setRepeatingRequest(mCaptureRequestBuilder.build(), null, null);
//                                } catch (CameraAccessException e) {
//                                    e.printStackTrace();
//                                }
//                            }
//
//                            @Override
//                            public void onConfigureFailed(CameraCaptureSession session) {
//
//                            }
//                        };
//
//                        mCamera.createCaptureSession(surfaceList, stateCallback, mHandler);
//
//
//                    } catch (CameraAccessException e) {
//                        e.printStackTrace();
//                    }
//                }
//
//                @Override
//                public void onDisconnected(CameraDevice mCamera) {
//
//                }
//
//                @Override
//                public void onError(CameraDevice mCamera, int error) {
//
//                }
//            };

            // According to camera2 example, we do not need
            mHandler = new Handler();

            mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            try {
                mCameraList = mCameraManager.getCameraIdList();

                for (String idString : mCameraList) {
                    Integer m = mCameraManager.getCameraCharacteristics(idString).get(CameraCharacteristics.LENS_FACING);

                    if (( mCameraManager.getCameraCharacteristics(idString).get(CameraCharacteristics.LENS_FACING) == CameraMetadata.LENS_FACING_BACK ) &&
                            ( mCameraManager.getCameraCharacteristics(idString).get(CameraCharacteristics.FLASH_INFO_AVAILABLE ) ) ) {
                        mCameraId = idString;
                        mCameraManager.openCamera(mCameraId, new MyCameraDeviceStateCallback(), null);
                        break;
                    }
                }

            } catch ( CameraAccessException e ) {
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
        }

        else {
            Context context = this.getApplicationContext();
            if ( context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH ) ) {
                // This works with deprecated android.hardware.Camera class
                mCamera = Camera.open();
                parameters = mCamera.getParameters();
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                mCamera.setParameters(parameters);
                mCamera.startPreview();

                // hold value of the main led_switch sinc e we have to do an image switch instead
                // of using a thumb switch.
                ledSwitch  = true;
                backlightSwitch = true;

            }
            else {
            }
        }
    }


    class MyCameraDeviceStateCallback extends CameraDevice.StateCallback {
        @Override
        public void onOpened(CameraDevice camera) {
            mCameraDevice = camera;
            //get builder
            try {
                mBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_MANUAL);
                //flash on, default is on
                mBuilder.set(CaptureRequest.CONTROL_AE_MODE, CameraMetadata.CONTROL_AF_MODE_AUTO);
                mBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                List<Surface> list = new ArrayList<Surface>();
                mSurfaceTexture = new SurfaceTexture(1);
                Size size = getSmallestSize(mCameraDevice.getId());
                mSurfaceTexture.setDefaultBufferSize(size.getWidth(), size.getHeight());
                mSurface = new Surface(mSurfaceTexture);
                list.add(mSurface);
                mBuilder.addTarget(mSurface);
                camera.createCaptureSession(list, new MyCameraCaptureSessionStateCallback(), null);
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
    }

    private Size getSmallestSize(String cameraId) throws CameraAccessException {
        Size[] outputSizes = mCameraManager.getCameraCharacteristics(cameraId)
                .get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                .getOutputSizes(SurfaceTexture.class);
        if (outputSizes == null || outputSizes.length == 0) {
            throw new IllegalStateException(
                    "Camera " + cameraId + "doesn't support any outputSize.");
        }
        Size chosen = outputSizes[0];
        for (Size s : outputSizes) {
            if (chosen.getWidth() >= s.getWidth() && chosen.getHeight() >= s.getHeight()) {
                chosen = s;
            }
        }
        return chosen;
    }

    class MyCameraCaptureSessionStateCallback extends CameraCaptureSession.StateCallback {
        @Override
        public void onConfigured(CameraCaptureSession session) {
            mSession = session;
            try {
                mSession.setRepeatingRequest(mBuilder.build(), null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
        @Override
        public void onConfigureFailed(CameraCaptureSession session) {
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        //mSwitch = (Switch) findViewById(R.id.mainLight);


        /**
         *    removing the switch for now.  too much hassle to maintain while preserving
         *    compatibility back to api level 7
         */
//        final MenuItem myswitch = menu.findItem(R.id.myswitch);
//        final Switch mSwitch = (Switch) myswitch.getActionView();
//
//        //set the switch to ON
//        mSwitch.setChecked(true);
//
//        //attach a listener to check for changes in state
//        mSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
//
//            @Override
//            public void onCheckedChanged(CompoundButton buttonView,
//                                         boolean isChecked) {
//
//                if(isChecked){
//                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
//                    mCamera.setParameters( parameters );
//                    mCamera.startPreview();
//                }
//                else {
//                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);-
//                    mCamera.setParameters( parameters );
//                    mCamera.stopPreview();
//                }
//            }
//        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
//        if (id == R.id.action_settings) {
//            return true;
//        }

        switch ( item.getItemId() ) {

            case R.id.led_switch:
                if ( ledSwitch == true ) {
                    // set boolean LED check to false
                    ledSwitch = false;

                    // if the LED light is already on, turn it off
                    parameters.setFlashMode( Camera.Parameters.FLASH_MODE_OFF );
                    mCamera.setParameters(parameters);
                    mCamera.stopPreview();

                    // dynamically switch the image in the action bar
                    item.setIcon ( R.drawable.ic_flashlight_off);
                }
                else {
                    // set boolean LED check to true
                    ledSwitch = true;

                    // if the LED light is off, turn it on
                    parameters.setFlashMode( Camera.Parameters.FLASH_MODE_TORCH );
                    mCamera.setParameters(parameters);
                    mCamera.startPreview();

                    // dynamically switch the image in the action bar
                    item.setIcon ( R.drawable.ic_flashlight_on );

                }
                break;

            case R.id.action_backlight:
                if ( backlightSwitch == true ) {
                    backlightSwitch = false;
                    adjustBacklight( 0 );
                }
                else {
                    backlightSwitch = true;
                    adjustBacklight( -1 );
                }
        }
        return super.onOptionsItemSelected(item);
    }
    public void adjustBacklight( float value ) {
        WindowManager.LayoutParams layout = getWindow().getAttributes();
        layout.screenBrightness = value;
        getWindow().setAttributes( layout );

        // not needed
//        findViewById( android.R.id.content ).invalidate();
//        findViewById( android.R.id.content ).requestLayout();

        // this code does not work
//        View view = findViewById(R.id.mainLayout);
//        view.invalidate();
    }
}
