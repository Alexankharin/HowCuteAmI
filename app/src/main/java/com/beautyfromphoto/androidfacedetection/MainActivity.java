package com.beautyfromphoto.androidfacedetection ;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;

import androidx.core.content.FileProvider;

import androidx.exifinterface.media.ExifInterface;

import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import android.os.Environment;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.vision.face.Landmark;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tensorflow.lite.Interpreter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends Activity {

    private static final int RQS_LOADIMAGE = 1;
    private static final int REQUEST_CODE_PHOTO = 2;
    //private Button btnLoad, btnDetFace;
    private FloatingActionButton btnLoad, btnDetFace,btnSave, btnCam, btnLines;;
    private ProgressBar spinner;
    private ImageView imgView;
    private Bitmap myBitmap;
    private int[] intValues = new int[112 * 112];
    private static final int IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 255.0f;
    private Bitmap tempBitmap;
    private Bitmap tempbitmapnolines;
    public boolean isFaceFound = false;
    private Uri tempname;
    private int lineflag=1;
    String currentPhotoPath;


    ByteBuffer imgData = ByteBuffer.allocateDirect(
            4 * 1 * 112 * 112 * 3);

    Interpreter interpreter;





    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnLoad = (FloatingActionButton) findViewById(R.id.fabOpen);
        btnDetFace = (FloatingActionButton) findViewById(R.id.fabRun);
        imgView = (ImageView) findViewById(R.id.imgview);
        btnSave = (FloatingActionButton) findViewById(R.id.fabShare);
        spinner = (ProgressBar) findViewById(R.id.pBar);
        btnDetFace.hide();
        btnSave.hide();
        btnCam=(FloatingActionButton) findViewById(R.id.fabPhoto);
        btnLines=(FloatingActionButton) findViewById(R.id.fabLines);
        try {
            interpreter = new Interpreter(loadModelFile(MainActivity.this));
            Log.e("TIME", "Interpreter_started ");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TIME", "Interpreter NOT started ");
        }
        btnLines.setOnClickListener(new View.OnClickListener(){


            @Override
            public void onClick(View v) {
                if (lineflag==0){
                    Log.d("RADIOBTN", String.valueOf(lineflag));
                    btnLines.setImageResource(android.R.drawable.radiobutton_on_background);
                    lineflag=1;
                    imgView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
                    return;
                }

                if (lineflag==1){
                    Log.d("RADIOBTN", String.valueOf(lineflag));
                    btnLines.setImageResource(android.R.drawable.radiobutton_off_background);
                    lineflag=0;
                    imgView.setImageDrawable(new BitmapDrawable(getResources(), tempbitmapnolines));
                    return;
                }


            }
        });



        btnCam.setOnClickListener(new View.OnClickListener() {
                                      @Override
                                      public void onClick(View v) {
                                          Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                                          intent.putExtra(MediaStore.EXTRA_OUTPUT, generateFileUri());
                                          startActivityForResult(intent, REQUEST_CODE_PHOTO);
                                          btnLoad.hide();
                                          btnCam.hide();
                                          btnDetFace.hide();
                                          btnSave.hide();
                                          btnLoad.show();
                                          btnCam.show();
                                          btnDetFace.show();
                                          btnLoad.show();
                                          btnCam.show();
                                          btnDetFace.show();
                                      }
                                  }
        );

        btnLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, RQS_LOADIMAGE);
            }
        });

        btnDetFace.setOnClickListener(new View.OnClickListener() {
                                          @Override
                                          public void onClick(View v) {
                                              spinner.setVisibility(View.VISIBLE);
                                              if (myBitmap == null) {
                                                  Toast.makeText(MainActivity.this,
                                                          "myBitmap == null",
                                                          Toast.LENGTH_LONG).show();
                                              } else {
                                                  isFaceFound = false;
                                                  new Thread() {
                                                      @Override
                                                      public void run() {
                                                          //Do long operation stuff here search stuff
                                                          tempBitmap = detectFace();
                                                          try {

                                                              // code runs in a thread
                                                              runOnUiThread(new Runnable() {
                                                                                @Override
                                                                                public void run() {
                                                                                    imgView.setImageDrawable(new BitmapDrawable(getResources(), tempBitmap));
                                                                                    spinner.setVisibility(View.GONE);
                                                                                    if(isFaceFound==true){
                                                                                    btnSave.show();
                                                                                    btnLines.show();
                                                                                    }
                                                                                }
                                                                            }
                                                              );
                                                          } catch (final Exception ignored) {
                                                          }
                                                      }
                                                  }.start();

                                                  if (isFaceFound = false) {
                                                      Toast.makeText(MainActivity.this,
                                                              "Faces not found",
                                                              Toast.LENGTH_LONG).show();
                                                  }

                                              }


                                              //spinner.setVisibility(View.GONE);
                                          }
                                      }
        );

        btnSave.setOnClickListener(
                new View.OnClickListener() {
                    Bitmap bitmaptosave;

                    @Override
                    public void onClick(View v) {
                        Bitmap tosavebitmap;
                        if (tempBitmap != null) {
                            if (lineflag == 1){
                                tosavebitmap=Bitmap.createBitmap(tempBitmap);
                            }
                            else{
                                tosavebitmap=Bitmap.createBitmap(tempbitmapnolines);
                            }

                            Long tsLong = System.currentTimeMillis() / 1000;
                            String ts = tsLong.toString();
                            //File outFile = new File(path, ts + ".jpeg");
                            File cachePath = new File(getApplicationContext().getCacheDir(), "images");
                            if (!cachePath.exists()) {
                                cachePath.mkdirs();
                            }

                            try {
                                //FileOutputStream outputStream = new FileOutputStream(outFile);
                                FileOutputStream outputStream = new FileOutputStream(cachePath + "/image"+"temp"+".jpeg");
                                tosavebitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                                outputStream.close();

                                //Toast.makeText(MainActivity.this,
                                //       "saved at " + cachePath + "/image"+ts+".jpeg",
                                //       Toast.LENGTH_LONG).show();


                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            //FileOutputStream outputStream = new FileOutputStream();
                            File imagePath = new File(getApplicationContext().getCacheDir(), "images");
                            File newFile = new File(imagePath, "/image"+"temp"+".jpeg");
                            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "com.beautyfromphoto.androidfacedetection.fileprovider", newFile);
                            Log.d("saved", contentUri.toString());

                            Intent shareIntent = new Intent();
                            shareIntent.setAction(Intent.ACTION_SEND);
                            shareIntent.setType("image/*");
                            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            //shareIntent.putExtra(Intent.EXTRA_TEXT, "Hello, This is test Sharing");
                            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                            startActivity(Intent.createChooser(shareIntent, "Send your image"));

                        } else {
                            Toast.makeText(MainActivity.this,
                                    "Empty file",
                                    Toast.LENGTH_LONG).show();
                        }
                        ;

                    }
                }
        );
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        int orientation = -1;
        // PHOTO CAPTURING
        if (requestCode == REQUEST_CODE_PHOTO &&  resultCode == RESULT_OK) {
            if (data == null) {
                Log.d("NOPHOTO", "Intent is null");
                myBitmap=getimagefromUri(tempname);
                imgView.setImageBitmap(myBitmap);
            } else {
                myBitmap=getimagefromUri(tempname);
                imgView.setImageBitmap(myBitmap);
            }
            btnLines.hide();
            btnSave.hide();
        }
        else if (requestCode == REQUEST_CODE_PHOTO) {
            Log.d("NOPHOTO", String.valueOf(resultCode));
            Log.d("NOPHOTO", "Canceled");
            Log.d("NOPHOTO", tempname.toString());
            Toast.makeText(MainActivity.this,
                    "photo not loaded",
                    Toast.LENGTH_LONG).show();
            btnLines.hide();
            btnDetFace.hide();
            btnSave.hide();
        }




        //IMAGE LOADING
        if (requestCode == RQS_LOADIMAGE
                && resultCode == RESULT_OK) {
            try {
                btnDetFace.show();
                btnLines.hide();
                btnSave.hide();
                Uri imageuri = data.getData();
                assert imageuri != null;
                InputStream inputStream =
                        getContentResolver().openInputStream(imageuri);
                orientation = getExifRotation(inputStream);
                InputStream inputStream2 =
                        getContentResolver().openInputStream(imageuri);

                //orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

                Log.d("ORIENT", "Exif: " + orientation);
                myBitmap = BitmapFactory.decodeStream(inputStream2);
                //orientation=getOrientation(imageuri);
                //Log.d("ORIENT", Integer.toString(orientation));
                if (orientation > 0) {
                    Matrix matrix = new Matrix();
                    matrix.postRotate(orientation);

                    myBitmap = Bitmap.createBitmap(myBitmap, 0, 0, myBitmap.getWidth(),
                            myBitmap.getHeight(), matrix, true);
                }
                imgView.setImageBitmap(myBitmap);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    /*
    reference:
    https://search-codelabs.appspot.com/codelabs/face-detection
     */
    @SuppressLint("DefaultLocale")
    private Bitmap detectFace() {

        //Create a Paint object for drawing with
        Paint myRectPaint = new Paint();
        myRectPaint.setStrokeWidth(5);
        myRectPaint.setColor(Color.GREEN);
        myRectPaint.setStyle(Paint.Style.STROKE);
        Paint fontPaint = new Paint();
        fontPaint.setStrokeWidth(3);
        fontPaint.setTextSize(70);
        fontPaint.setColor(Color.BLUE);
        fontPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        Paint myLinePaint = new Paint();
        myLinePaint.setStrokeWidth(1);
        myLinePaint.setColor(Color.YELLOW);
        myLinePaint.setStyle(Paint.Style.STROKE);

        //Create a Canvas object for drawing on
        tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        //Detect the Faces
        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext()).setLandmarkType(FaceDetector.ALL_LANDMARKS).build();
        //!!!
        //Cannot resolve method setTrackingEnabled(boolean)
        //skip for now
        //faceDetector.setTrackingEnabled(false);

        Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);
        faceDetector.release();
        Face face;
        float[][] Beauty = new float[1][1];
        float[][] Age = new float[1][1];
        float[][] Gender = new float[1][1];
        float[][] Crime = new float[1][1];
        float[][] Answer = new float[1][1];

        imgData.order(ByteOrder.nativeOrder());
        //Draw Rectangles on the Faces

        if (faces.size() > 0) {
            //DRAW RECTANGLES
            for (int i = 0; i < faces.size(); i++) {

                face = faces.valueAt(i);
                isFaceFound = true;
                float x1 = Math.max(face.getPosition().x, 0);
                float y1 = Math.max(face.getPosition().y, 0);
                float x2 = Math.min(x1 + face.getWidth(), frame.getBitmap().getWidth());
                float y2 = Math.min(y1 + face.getHeight(), frame.getBitmap().getHeight());
                float reulerZ=face.getEulerZ();

                //Log.d("DRAW", String.valueOf(landmarks.size()));



                Bitmap tempbitmap2 = Bitmap.createBitmap(tempBitmap, (int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                tempbitmap2=RotateBitmap(tempbitmap2,reulerZ);
                tempbitmap2 = Bitmap.createScaledBitmap(tempbitmap2, 112, 112, true);
                convertBitmapToByteBuffer(tempbitmap2);
                Object[] inputs = {imgData};
                Map<Integer, Object> outputs = new HashMap<>();
                outputs.put(1, Beauty);
                //outputs.put(3, Age);
                outputs.put(0, Gender);
                //outputs.put(2, Crime);
                interpreter.runForMultipleInputsOutputs(inputs, outputs);
                String textToShow = String.format("%.1f", ((Beauty[0][0]*10 + 1.8))/1.18);
                textToShow = textToShow + "/10 ";
                //textToShow=textToShow+String.format("%.0f", (Age[0][0] * 100));
                //textToShow = textToShow + "Y ";
                if (Gender[0][0]>0.5){
                    //textToShow=textToShow+"M";
                    myRectPaint.setColor(Color.GREEN);
                }
                else{
                    //textToShow=textToShow+"F";
                    myRectPaint.setColor(Color.MAGENTA);
                }
                //textToShow=textToShow+String.format("%.0f", Crime[0][0]*100);
                //textToShow = textToShow + "C";

                int width = tempCanvas.getWidth();
                //int height=tempCanvas.getHeight();
                int fontsize = Math.max(width / 20, imgView.getWidth() / 20);
                fontPaint.setTextSize(width / 15);
                myLinePaint.setStrokeWidth(width / 480);
                myRectPaint.setStrokeWidth(width / 240);
                tempCanvas.drawText(textToShow, x1, y1 - 10, fontPaint);
                //Log.d("DRAW", String.valueOf(x1)+" "+String.valueOf(y1)+" "+String.valueOf(x2)+" "+String.valueOf(y2));
                tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
                //Log.d("DRAW", String.valueOf(nose.x) +" "+String.valueOf(nose.y) +" "+String.valueOf(mouth.x)+" "+String.valueOf(mouth.y));
                tempbitmapnolines=tempBitmap.copy(tempBitmap.getConfig(), true);
                //tempCanvas.drawBitmap(tempbitmap2, 0, 0, null);
            }
            for (int i = 0; i < faces.size(); i++)
            {
                face = faces.valueAt(i);
                List<Landmark> landmarks;
                PointF nose = new PointF(0, 0);
                PointF leye= new PointF(0, 0);
                PointF reye= new PointF(0, 0);
                PointF mouth= new PointF(0, 0);
                PointF  lcheek= new PointF(0, 0);
                PointF  rcheek= new PointF(0, 0);
                PointF  lear= new PointF(0, 0);
                PointF  rear= new PointF(0, 0);
                PointF  lmouth= new PointF(0, 0);
                PointF  rmouth= new PointF(0, 0);
                Landmark landmark;
                landmarks=face.getLandmarks();
                //DEFINE LANDMARKS
                for (int j = 0;j <landmarks.size(); j++)
                {
                    landmark= landmarks.get(j);
                    //Log.d("DRAW", String.valueOf(landmark.getType()));

                    if (landmark.getType() == Landmark.BOTTOM_MOUTH)
                    {mouth=landmark.getPosition();
                        //Log.d("POSITION", "MOUTH"+String.valueOf(j));
                    };
                    if (landmark.getType()==Landmark.LEFT_EYE)
                    {leye=landmark.getPosition();
                    };
                    if (landmark.getType()==Landmark.RIGHT_EYE)
                    {reye=landmark.getPosition();
                    };
                    if (landmark.getType()==Landmark.NOSE_BASE)
                    {nose=landmark.getPosition();
                    };
                    if (landmark.getType()==Landmark.LEFT_CHEEK)
                    {lcheek=landmark.getPosition();
                    };
                    if (landmark.getType()==Landmark.RIGHT_CHEEK)
                    {rcheek=landmark.getPosition();
                    };
                    if (landmark.getType()==Landmark.LEFT_EAR)
                    {lear=landmark.getPosition();
                    };
                    if (landmark.getType()==Landmark.RIGHT_EAR)
                    {rear=landmark.getPosition();
                    };
                    if (landmark.getType()==Landmark.LEFT_MOUTH)
                    {lmouth=landmark.getPosition();
                    };
                    if (landmark.getType()==Landmark.RIGHT_MOUTH)
                    {rmouth=landmark.getPosition();
                    };

                }
                //DRAW MASK
                drawLine(nose, mouth, tempCanvas,myLinePaint);
                drawLine(leye, reye, tempCanvas,myLinePaint);
                drawLine(leye, mouth, tempCanvas,myLinePaint);
                drawLine(leye, nose, tempCanvas,myLinePaint);
                drawLine(reye, nose, tempCanvas,myLinePaint);
                drawLine(lear, lcheek, tempCanvas,myLinePaint);
                drawLine(rear, rcheek, tempCanvas,myLinePaint);
                drawLine(lear, mouth, tempCanvas,myLinePaint);
                drawLine(rear, mouth, tempCanvas,myLinePaint);
                drawLine(lear, leye, tempCanvas,myLinePaint);
                drawLine(rear, reye, tempCanvas,myLinePaint);
                drawLine(rear, rmouth, tempCanvas,myLinePaint);
                drawLine(lear, lmouth, tempCanvas,myLinePaint);
                drawLine(rmouth,lmouth,tempCanvas,myLinePaint);
                drawLine(rmouth,mouth,tempCanvas,myLinePaint);
                drawLine(lmouth,mouth,tempCanvas,myLinePaint);
            }

        }
        return tempBitmap;
    }

    private void convertBitmapToByteBuffer(Bitmap bitmap) {
        if (imgData == null) {
            return;
        }
        imgData.rewind();
        bitmap.getPixels(intValues, 0, bitmap.getWidth(), 0, 0, bitmap.getWidth(), bitmap.getHeight());
        // Convert the image to floating point.
        int pixel = 0;
        long startTime = SystemClock.uptimeMillis();
        for (int i = 0; i < 112; ++i) {
            for (int j = 0; j < 112; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF)));
                imgData.putFloat((((val >> 8) & 0xFF)));
                imgData.putFloat((((val) & 0xFF)));
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d("TIME", "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }


    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("modeltodeploy_BAGC.tflite");
        FileInputStream inputStream = new FileInputStream(fileDescriptor.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        long startOffset = fileDescriptor.getStartOffset();
        long declaredLength = fileDescriptor.getDeclaredLength();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }


    public static int getExifRotation(InputStream is) {
        if (is == null) return 0;
        try {
            ExifInterface exif = new ExifInterface(is);
            // We only recognize a subset of orientation tag values
            switch (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                default:
                    return ExifInterface.ORIENTATION_UNDEFINED;
            }
        } catch (IOException e) {
            return 0;
        }
    }

    private void shareImage(File imagePath) {
        //Uri filepath=Uri.parse(imagePath.toString());
        //Uri filepath=Uri.fromFile(imagePath);
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.setType("image/*");
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is My BEAUTYSCORES");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imagePath));
        startActivity(Intent.createChooser(shareIntent, "Send your image"));
        //Log.e("img", filepath.toString());
        Log.e("img", imagePath.toString());
    }
    //BITMAP ROTATION ON ANGLE IN DEGREES
    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }

    //CREATE TEMPFILE FOR PHOTO STORAGE

    private Uri generateFileUri() {
        File imagePath = new File(getApplicationContext().getCacheDir(), "images");
        if (!imagePath.exists()) {
            imagePath.mkdirs();
        }
        Uri newfileperm;
        File newFile = new File(imagePath, "/image"+"temp"+".jpeg");
        if (newFile.exists()){
            newFile.delete(); // here i'm checking if file exists and if yes then i'm deleting it but its not working
        }
        newFile = new File(imagePath, "/image"+"temp"+".jpeg");
        Log.d("FILECREAT", newFile.toString());
        tempname=FileProvider.getUriForFile(getApplicationContext(), "com.beautyfromphoto.androidfacedetection.fileprovider", newFile);
        return tempname;
    }

    //CREATE bitmap from URI
    private Bitmap getimagefromUri(Uri uri){
        int orientation=-1;
        Bitmap localbmp = null;
        try {
            InputStream inputStream =
                    getContentResolver().openInputStream(uri);
            orientation = getExifRotation(inputStream);
            InputStream inputStream2 =
                    getContentResolver().openInputStream(uri);

            //orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            Log.d("ORIENT", "Exif: " + orientation);
            localbmp = BitmapFactory.decodeStream(inputStream2);
            //orientation=getOrientation(imageuri);
            //Log.d("ORIENT", Integer.toString(orientation));
            if (orientation > 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(orientation);

                localbmp = Bitmap.createBitmap(localbmp, 0, 0, localbmp.getWidth(),
                        localbmp.getHeight(), matrix, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return localbmp;
    }
    public static void drawLine(PointF one, PointF two, Canvas todraw, Paint myLinePaint){
        todraw.drawLine(one.x,one.y,two.x, two.y,myLinePaint);
    }
}

