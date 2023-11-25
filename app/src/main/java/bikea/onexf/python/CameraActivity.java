package bikea.onexf.python;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import android.Manifest;
import android.app.Activity;
import android.content.ClipData;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;
import com.getbase.floatingactionbutton.BuildConfig;
import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import bikea.onexf.python.camera.CameraRulerView;
import bikea.onexf.python.camera.Circle;
import bikea.onexf.python.camera.Line;
import bikea.onexf.python.camera.Point;
import bikea.onexf.python.camera.Polygon;
import bikea.onexf.python.camera.Tetragon;
import bikea.onexf.python.database.PFASQLiteHelper;
import bikea.onexf.python.database.ReferenceManager;
import bikea.onexf.python.database.ReferenceObject;
import bikea.onexf.python.database.UserDefinedReferences;

import static android.os.Build.VERSION.SDK_INT;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;

public class CameraActivity extends BaseActivity {

    private Status status = Status.MODE_CHOICE;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int ACTIVITY_REQUEST_CODE = 101;

    private Activity thisActivity = this;
    private ImageButton cameraButton;
    private ImageButton galleryButton;
    private ImageView pictureView;
    private LinearLayout btnValue;
    private ConstraintLayout lyOutput;
    private CameraRulerView drawView;
    private FloatingActionsMenu newMeasureButton;
    private FloatingActionButton confirmButton;
    private Menu refsMenu;
    private RelativeLayout modeChoiceLayout;
    private Toolbar toolbar;
    private View discriptorText;
    private View cameraLabel;
    private View galleryLabel;
    private Bitmap photo;
    private Uri mPhotoUri;
    String mCurrentPhotoPath;
    private LinearLayout TPButton, LDButton, PBButton, TPPButton;
    private TextView TPText, LDText, PBText, TPPText, OutputText;
    private Button FinishButton;

    private ArrayList<ReferenceObject> refs;
    private ArrayList<UserDefinedReferences> udrefs;
    DisplayMetrics displayMetrics = new DisplayMetrics();
    private String referenceObjectShape = "tetragon";
    private String referenceObjectName = "Stiker";
    private float referenceObjectSize = 101.6f*101.6f;
    private int valueTP, valueLD, valuePB, valueTPP = 0;

    // These matrices will be used to move and zoom image
    Matrix tmpMatrix = new Matrix();
    Matrix savedMatrix = new Matrix();
    Matrix zoomMatrix = new Matrix();

    // We can be in one of these 3 states
    static final int NONE = 0;
    static final int DRAG = 1;
    static final int ZOOM = 2;
    int mode = NONE;
    // Remember some things for zooming
    PointF start = new PointF();
    PointF mid = new PointF();
    float oldDist = 1f;
    Python python;

    public enum Status {
        MODE_CHOICE,
        REFERENCE,
        MEASUREMENT
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        refs = ReferenceManager.getAllActiveRefPredefObjects(getBaseContext());
        PFASQLiteHelper dbHelper = new PFASQLiteHelper(getBaseContext());
        udrefs = dbHelper.getAllUDefRef();
        for (int i = udrefs.size() - 1; i >= 0; i--) {
            if (!udrefs.get(i).getUDR_ACTIVE()) {
                udrefs.remove(i);
            }
        }

        prefManager.putLastMode("camera");
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        if (! Python.isStarted()) {
            Python.start(new AndroidPlatform(this));
        }

        python = Python.getInstance();
        cameraButton = (ImageButton) findViewById(R.id.from_camera_button);
        galleryButton = (ImageButton) findViewById(R.id.from_gallery_button);
        pictureView = (ImageView) findViewById(R.id.pictureView);
        btnValue = (LinearLayout) findViewById(R.id.buttonValue);
        confirmButton = (FloatingActionButton) findViewById(R.id.confirm_reference);
        modeChoiceLayout = (RelativeLayout) findViewById(R.id.camera_ruler_layout);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        discriptorText = findViewById(R.id.camera_gallery_choice_text);
        cameraLabel = findViewById(R.id.camera_button_label);
        galleryLabel = findViewById(R.id.gallery_button_label);
        FinishButton = findViewById(R.id.buttonFinish);
        lyOutput = findViewById(R.id.lyOutput);
        OutputText = findViewById(R.id.outputText);

        TPButton = findViewById(R.id.buttonTP);
        LDButton = findViewById(R.id.buttonLD);
        PBButton = findViewById(R.id.buttonPB);
        TPPButton = findViewById(R.id.buttonTPP);

        TPText = findViewById(R.id.nilaiTP);
        LDText = findViewById(R.id.nilaiLD);
        PBText = findViewById(R.id.nilaiPB);
        TPPText = findViewById(R.id.nilaiTPP);

        TPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentString = toolbar.getSubtitle().toString();
                String[] separated = currentString.split(" ");
                if(separated[2].equals("cm")) {
                    double before = Double.parseDouble(separated[1]);
                    valueTP = (int) (before * 10);
                } else if (separated[2].equals("m")) {
                    double before = Double.parseDouble(separated[1]);
                    valueTP = (int) (before * 1000);
                }
                TPText.setText(separated[1] + " " + separated[2]);
            }});

        LDButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentString = toolbar.getSubtitle().toString();
                String[] separated = currentString.split(" ");
                if(separated[2].equals("cm")) {
                    double before = Double.parseDouble(separated[1]);
                    valueLD = (int) (before * 10);
                } else if (separated[2].equals("m")) {
                    double before = Double.parseDouble(separated[1]);
                    valueLD = (int) (before * 1000);
                }
                LDText.setText(separated[1] + " " + separated[2]);
            }});

        PBButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentString = toolbar.getSubtitle().toString();
                String[] separated = currentString.split(" ");
                if(separated[2].equals("cm")) {
                    double before = Double.parseDouble(separated[1]);
                    valuePB = (int) (before * 10);
                } else if (separated[2].equals("m")) {
                    double before = Double.parseDouble(separated[1]);
                    valuePB = (int) (before * 1000);
                }
                PBText.setText(separated[1] + " " + separated[2]);
            }});

        TPPButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String currentString = toolbar.getSubtitle().toString();
                String[] separated = currentString.split(" ");
                if(separated[2].equals("cm")) {
                    double before = Double.parseDouble(separated[1]);
                    valueTPP = (int) (before * 10);
                } else if (separated[2].equals("m")) {
                    double before = Double.parseDouble(separated[1]);
                    valueTPP = (int) (before * 1000);
                }
                TPPText.setText(separated[1] + " " + separated[2]);
            }});

        FinishButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.e("TAG", "onClick: " + valueTPP + valueTP );
                Log.e("E:", "PBValue : " + valuePB);
                if(valueTP == 0 || valueLD == 0 || valuePB == 0 || valueTPP == 0 ) {
                    Toast.makeText(getApplicationContext(), "Silahkan Masukan Nilai", Toast.LENGTH_SHORT).show();
                } else {
                    lyOutput.setVisibility(VISIBLE);
                    PyObject pyObject = python.getModule("script");
                    Log.e("OBJECT", "ValueTPP:" + valueTPP);
                    PyObject object = pyObject.callAttr("main", (valueTP/10), (valueLD/10), (valuePB/10), (valueTPP/10));
                    double output = Double.parseDouble(object.toString());
                    Log.e("OUTPUT", "OUTPUT : " + output);
                    DecimalFormat df = new DecimalFormat("#.##");
                    OutputText.setText( df.format(output) + " kg");
                }
            }
        });

        newMeasureButton = (FloatingActionsMenu) findViewById(R.id.new_measure_fam);
        FloatingActionButton newTetragonButton = (FloatingActionButton) findViewById(R.id.new_tetragon_fab);
        FloatingActionButton newTriangleButton = (FloatingActionButton) findViewById(R.id.new_triangle_fab);
        FloatingActionButton newCircleButton = (FloatingActionButton) findViewById(R.id.new_circle_fab);
        FloatingActionButton newLineButton = findViewById(R.id.new_line_fab);

        drawView = new CameraRulerView(getBaseContext(), toolbar, this);
        drawView.ctxStatus = status;
        drawView.setVisibility(GONE);
        modeChoiceLayout.addView(drawView);

        pictureView.setImageMatrix(new Matrix());

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mPhotoUri = FileProvider.getUriForFile(CameraActivity.this, "bikea.onexf.python.provider", createImageFile());
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                intent.setClipData(ClipData.newRawUri("A photo", mPhotoUri));
                intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                intent.putExtra(MediaStore.EXTRA_OUTPUT, mPhotoUri);
                startActivityForResult(intent, ACTIVITY_REQUEST_CODE);
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (SDK_INT >= Build.VERSION_CODES.M) {
                    // check if we have the permission we need -> if not request it and turn on the light afterwards
                    if (ContextCompat.checkSelfPermission(thisActivity,
                            Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(thisActivity,
                                new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
                        return;
                    }
                }
                Intent galleryIntent = new Intent();
                galleryIntent.setType("image/*");
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(galleryIntent, PICK_IMAGE_REQUEST);
            }
        });

        confirmButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (drawView.reference instanceof Polygon
                        && ((Polygon) drawView.reference).isSelfIntersecting()) {
                    Toast.makeText(getBaseContext(), getString(R.string.reference_self_intersecting), Toast.LENGTH_LONG).show();
                } else {
                    setReference();
                }
            }
        });

        newTetragonButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newMeasureButton.collapseImmediately();
                drawView.measure = drawView.newTetragon();
                drawView.invalidate();
            }
        });

        newTriangleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newMeasureButton.collapseImmediately();
                drawView.measure = drawView.newTriangle();
                drawView.invalidate();
            }
        });

        newCircleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newMeasureButton.collapseImmediately();
                drawView.measure = drawView.newCircle();
                drawView.invalidate();
            }
        });

        newLineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                newMeasureButton.collapseImmediately();
                drawView.measure = drawView.newLine();
                drawView.invalidate();
            }
        });

        overridePendingTransition(0, 0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int touchPoint = drawView.clickInTouchpoint(event);
        if (mode == NONE && touchPoint >= 0) { //click in touchpoint while no other gesture active
            drawView.activeTouchpoint = touchPoint;
            drawView.executeTouch(event);
        } else if (drawView.activeTouchpoint >= 0) { //further movements of grabbed touchpoint
            drawView.executeTouch(event);
        } else { //not in touchpoint or gesture already active
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_DOWN:
                    start.set(event.getX(), event.getY());
                    mode = DRAG;
                    break;
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = spacing(event);
                    if (oldDist > 10f) {
                        midPoint(mid, event);
                        mode = ZOOM;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    if (drawView.reference != null) {
                        drawView.reference.endMove();
                    }
                    if (drawView.measure != null) {
                        drawView.measure.endMove();
                    }
                    mode = NONE;
                    savedMatrix.set(tmpMatrix);
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (mode == DRAG) {
                        tmpMatrix.set(savedMatrix);
                        tmpMatrix.postTranslate(event.getX() - start.x,
                                event.getY() - start.y);
                        if (drawView.reference != null) {
                            drawView.reference.move(event.getX() - start.x,
                                    event.getY() - start.y);
                        }
                        if (drawView.measure != null) {
                            drawView.measure.move(event.getX() - start.x,
                                    event.getY() - start.y);
                        }
                    } else if (mode == ZOOM) {
                        float newDist = spacing(event);
                        if (newDist > 10f) {
                            tmpMatrix.set(savedMatrix);
                            float scale = newDist / oldDist;
                            tmpMatrix.postScale(scale, scale, mid.x, mid.y);
                            zoomMatrix.setScale(scale, scale, mid.x, mid.y);
                            if (drawView.reference != null) {
                                drawView.reference.zoom(zoomMatrix);
                                setScale();
                            }
                            if (drawView.measure != null) {
                                drawView.measure.zoom(zoomMatrix);
                            }
                        }
                    }
                    break;
            }
            drawView.invalidate();
            pictureView.setImageMatrix(tmpMatrix);
        }
        return true;
    }

    private float spacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt((x * x + y * y));
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        refsMenu = menu;
        for (int i = 0; i < udrefs.size(); i++) {
            menu.add(0, i, Menu.NONE, udrefs.get(i).getUDR_NAME());
            menu.getItem(i).setVisible(false);
        }
        for (int i = 0; i < refs.size(); i++) {
            menu.add(0, i + udrefs.size(), Menu.NONE, refs.get(i).nameId);
            menu.getItem(i).setVisible(false);
        }
        if (referenceObjectShape.equals("tetragon")) {
            drawView.reference = new Tetragon(new Point(400, 400), new Point(800, 400), new Point(800, 800), new Point(400, 800));
        } else if (referenceObjectShape.equals("line")) {
            drawView.reference = new Line(new Point(400, 400), new Point(800, 800));
        }
        drawView.invalidate();
        return true;
    }

    /**
     * Receive response from external camera and gallery apps.
     *
     * @param requestCode code of the request sent to the activity
     * @param resultCode result code returned by the activity
     * @param data data returned by the activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        InputStream stream = null;
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                try {
                    if (photo != null) {
                        photo.recycle();
                    }
                    stream = getContentResolver().openInputStream(data.getData());
                    photo = BitmapFactory.decodeStream(stream);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            pictureView.post(new Runnable() {
                                @Override
                                public void run() {
                                    computeTransformation(photo.getWidth(), photo.getHeight());
                                }
                            });
                        }
                    }).start();

                    pictureView.setImageBitmap(photo);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } finally {
                    if (stream != null) {
                        try {
                            stream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else if (requestCode == ACTIVITY_REQUEST_CODE) {
                pictureView.setImageBitmap(null);

                // Image saved to a generated MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                pictureView.setImageURI(mPhotoUri);
                final Drawable d = pictureView.getDrawable();
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        pictureView.post(new Runnable() {
                            @Override
                            public void run() {
                                computeTransformation(d.getIntrinsicWidth(), d.getIntrinsicHeight());
                            }
                        });
                    }
                }).start();
            }
            new Thread(new Runnable() {
                @Override
                public void run() {
                    pictureView.post(new Runnable() {
                        @Override
                        public void run() {
                            startImageFragment();
                        }
                    });
                }
            }).start();
        } else {
            if (resultCode != RESULT_CANCELED) {
                if (requestCode == ACTIVITY_REQUEST_CODE) {
                    Log.e("Camera App crashed.", "Returned result code: " + resultCode);
                    Toast.makeText(this, R.string.camera_crash, Toast.LENGTH_LONG).show();
                } else {
                    Log.e("Gallery App crashed.", "Returned result code: " + resultCode);
                    Toast.makeText(this, R.string.gallery_crash, Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Computes how a picture should be transformed to best fit the space available in the app.
     * Sets the transformation matrix of pictureView accordingly.
     *
     * @param picWidth  original width of the image
     * @param picHeight original height of the image
     */

    private void computeTransformation(float picWidth, float picHeight) {
        float height = picHeight;
        float width = picWidth;

        Matrix matrix = new Matrix();
        if (height < width) {
            matrix.postRotate(90, 0f, 0f);
            //noinspection SuspiciousNameCombination
            height = width;
            //noinspection SuspiciousNameCombination
            width = picHeight;
            matrix.postTranslate(width, 0f);
        }
        float scaleW = displayMetrics.widthPixels / width;
        float scaleH = modeChoiceLayout.getHeight() / height;
        float scale = Math.max(scaleW, scaleH);
        height *= scale;
        width *= scale;
        matrix.postScale(scale, scale);

        if (scaleW < scaleH) { //width overscaled
            matrix.postTranslate(-(width - displayMetrics.widthPixels) / 2, 0f);
        } else if (scaleH < scaleW) { //height overscaled
            matrix.postTranslate(0f, -(height - modeChoiceLayout.getHeight()) / 2);
        }
        savedMatrix = matrix;
        pictureView.setImageMatrix(matrix);
    }

    /**
     * Starts the first page of the actual camera ruler functionality, the reference selection
     * and confirmation. Hides the explanatory text and the buttons of the source selection phase.
     * Shows the picture view and fills it with the picture fetched from the external app.
     * Shows the view for drawing shapes on top of the picture view, the button for confirming
     * the reference object and the menu for selecting the reference object.
     */
    public void startImageFragment() {
        status = Status.REFERENCE;
        drawView.ctxStatus = status;
        cameraButton.setVisibility(GONE);
        cameraButton.setClickable(false);
        galleryButton.setVisibility(GONE);
        galleryButton.setClickable(false);
        discriptorText.setVisibility(GONE);
        cameraLabel.setVisibility(GONE);
        galleryLabel.setVisibility(GONE);
        btnValue.setVisibility(GONE);
        FinishButton.setVisibility(GONE);
        pictureView.setVisibility(VISIBLE);
        drawView.setVisibility(VISIBLE);
        drawView.setClickable(true);
        drawView.bringToFront();
        confirmButton.setVisibility(VISIBLE);
        toolbar.setTitle(R.string.reference_phase_title);
        toolbar.setSubtitle(referenceObjectName);
        showMenu();
    }

    /**
     * Starts the second phase of teh main camera ruler functionality, the measurement. Computes the
     * real world unit to pixel ration from the size of the reference shape and the active reference
     * object. Hides the reference confirmation button and reference object selection menu. Shows
     * the action button group for measurement shape selection. Sets the current measure to a line.
     * Forces the draw view to redraw.
     */
    public void setReference() {
        setScale();

        status = Status.MEASUREMENT;
        drawView.ctxStatus = status;
        confirmButton.setVisibility(GONE);
        btnValue.setVisibility(VISIBLE);
        FinishButton.setVisibility(VISIBLE);
//        newMeasureButton.setVisibility(GONE);
        drawView.measure = drawView.newLine();
        drawView.reference.active = false;
        toolbar.setTitle(R.string.measurement_phase_title);
        toolbar.setSubtitle(referenceObjectName);
        drawView.invalidate();
        hideMenu();
    }

    private void setScale() {
         drawView.scale = (float) Math.sqrt(referenceObjectSize / ((Polygon) drawView.reference).getArea());
    }

    /**
     * Goes backwards through the phases of camera ruler functionality hiding and showing
     * appropriate UI elements.
     */
    @Override
    public void onBackPressed() {
        if (status == Status.REFERENCE) {
            status = Status.MODE_CHOICE;
            drawView.ctxStatus = status;
            cameraButton.setVisibility(VISIBLE);
            cameraButton.setClickable(true);
            galleryButton.setVisibility(VISIBLE);
            galleryButton.setClickable(true);
            discriptorText.setVisibility(VISIBLE);
            cameraLabel.setVisibility(VISIBLE);
            galleryLabel.setVisibility(VISIBLE);
            drawView.setVisibility(GONE);
            drawView.setClickable(false);
            btnValue.setVisibility(GONE);
            FinishButton.setVisibility(GONE);
            lyOutput.setVisibility(GONE);
            pictureView.setVisibility(VISIBLE);
            pictureView.setImageURI(Uri.EMPTY);
            confirmButton.setVisibility(GONE);
            toolbar.setTitle(R.string.camera_ruler);
            toolbar.setSubtitle("");
            hideMenu();
        } else if (status == Status.MEASUREMENT) {
            status = Status.REFERENCE;
            drawView.ctxStatus = status;
            FinishButton.setVisibility(VISIBLE);
            newMeasureButton.collapseImmediately();
            newMeasureButton.setVisibility(GONE);
            btnValue.setVisibility(GONE);
            lyOutput.setVisibility(GONE);
            FinishButton.setVisibility(GONE);
            drawView.measure = null;
            drawView.reference.active = true;
            drawView.invalidate();
            confirmButton.setVisibility(VISIBLE);
            toolbar.setTitle(R.string.reference_phase_title);
            toolbar.setSubtitle(referenceObjectName);
            resetPoint();
            showMenu();
        } else {
            super.onBackPressed();
        }
    }

    /**
     * Reset Point
     */
    private void resetPoint() {
        valueTP = 0;
        valueLD = 0;
        valuePB = 0;
        valueTPP = 0;
        TPText.setText("-");
        LDText.setText("-");
        PBText.setText("-");
        TPPText.setText("-");
    }

    /**
     * Sets every item in the menu of available reference objects to be visible.
     */
    private void showMenu() {
        for (int i = 0; i < refsMenu.size(); i++) {
            refsMenu.getItem(i).setVisible(true);
        }
    }

    /**
     * Sets every item in the menu of available reference objects to be invisible.
     */
    private void hideMenu() {
        for (int i = 0; i < refsMenu.size(); i++) {
            refsMenu.getItem(i).setVisible(false);
        }
    }

    private File createImageFile() {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(getFilesDir(), "images/");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    @Override
    protected int getNavigationDrawerID() {
        return R.id.nav_camera;
    }
}
