package com.beautyfromphoto.androidfacedetection ;

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


public class MainActivity extends Activity {

    private static final int RQS_LOADIMAGE = 1;
    private Button btnLoad, btnDetFace;
    private ImageButton btnSave;
    private ProgressBar spinner;
    private ImageView imgView;
    private Bitmap myBitmap;
    private int[] intValues = new int[96 * 96];
    private static final int IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 255.0f;
    private Bitmap tempBitmap;
    public boolean isFaceFound=false;
    ByteBuffer imgData = ByteBuffer.allocateDirect(
            4 * 1 * 96 * 96 * 3);

    Interpreter interpreter;


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnLoad = (Button)findViewById(R.id.btnLoad);
        btnDetFace = (Button)findViewById(R.id.btnDetectFace);
        imgView = (ImageView)findViewById(R.id.imgview);
        btnSave = (ImageButton)findViewById(R.id.btnSave);
        spinner=(ProgressBar)findViewById(R.id.pBar);
        btnDetFace.setVisibility(View.INVISIBLE);
        btnSave.setVisibility(View.INVISIBLE);
        try {
            interpreter=new Interpreter(loadModelFile(MainActivity.this));
            Log.e("TIME", "Interpreter_started ");
        } catch (IOException e) {
            e.printStackTrace();
            Log.e("TIME", "Interpreter NOT started ");
        }
        btnLoad.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                startActivityForResult(intent, RQS_LOADIMAGE);
            }
        });

        btnDetFace.setOnClickListener(new View.OnClickListener(){
                                          @Override
                                          public void onClick(View v) {
                                              spinner.setVisibility(View.VISIBLE);
                                              if(myBitmap == null){
                                                  Toast.makeText(MainActivity.this,
                                                          "myBitmap == null",
                                                          Toast.LENGTH_LONG).show();
                                              }
                                              else{
                                                  isFaceFound=false;
                                                  new Thread() {
                                                      @Override
                                                      public void run() {
                                                          //Do long operation stuff here search stuff
                                                          tempBitmap=detectFace();
                                                          try {

                                                              // code runs in a thread
                                                              runOnUiThread(new Runnable() {
                                                                                @Override
                                                                                public void run() {
                                                                                    imgView.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));
                                                                                    spinner.setVisibility(View.GONE);
                                                                                    btnSave.setVisibility(View.VISIBLE);
                                                                                }
                                                                            }
                                                              );
                                                          } catch (final Exception ignored) {
                                                          }
                                                      }
                                                  }.start();

                                                  if (isFaceFound=false){
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
                    @Override
                    public void onClick(View v) {
                        //Intent intent = new Intent();
                        if (tempBitmap != null) {
                            //File path = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Images");
                            //File path = new File(getApplicationContext().getCacheDir(), "images");
                            //if (!path.exists()) {
                            //    path.mkdirs();
                            //}
                            Long tsLong = System.currentTimeMillis() / 1000;
                            String ts = tsLong.toString();
                            //File outFile = new File(path, ts + ".jpeg");
                            File cachePath = new File(getApplicationContext().getCacheDir(), "images");
                            if (!cachePath.exists()) {
                                cachePath.mkdirs();
                            }

                            try {
                                //FileOutputStream outputStream = new FileOutputStream(outFile);
                                FileOutputStream outputStream = new FileOutputStream(cachePath + "/image"+ts+".jpeg");
                                tempBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
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
                            File newFile = new File(imagePath, "/image"+ts+".jpeg");
                            Uri contentUri = FileProvider.getUriForFile(getApplicationContext(), "com.beautyfromphoto.androidfacedetection.fileprovider", newFile);


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
        int orientation=-1;
        if (requestCode == RQS_LOADIMAGE
                && resultCode == RESULT_OK){
            try {
                btnDetFace.setVisibility(View.VISIBLE);
                btnSave.setVisibility(View.INVISIBLE);
                Uri imageuri = data.getData();
                assert imageuri != null;
                InputStream inputStream =
                        getContentResolver().openInputStream(imageuri);
                orientation=getExifRotation(inputStream);
                InputStream inputStream2 =
                        getContentResolver().openInputStream(imageuri);

                //orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

                Log.d("ORIENT", "Exif: " +orientation);
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
    private Bitmap detectFace(){

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


        //Create a Canvas object for drawing on
        tempBitmap = Bitmap.createBitmap(myBitmap.getWidth(), myBitmap.getHeight(), Bitmap.Config.RGB_565);
        Canvas tempCanvas = new Canvas(tempBitmap);
        tempCanvas.drawBitmap(myBitmap, 0, 0, null);

        //Detect the Faces
        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext()).build();

        //!!!
        //Cannot resolve method setTrackingEnabled(boolean)
        //skip for now
        //faceDetector.setTrackingEnabled(false);

        Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
        SparseArray<Face> faces = faceDetector.detect(frame);
        Face face;

        float[][] Answer = new float[1][1];

        imgData.order(ByteOrder.nativeOrder());
        //Draw Rectangles on the Faces

        if (faces.size()>0){
            for (int i = 0; i < faces.size(); i++) {
                face = faces.valueAt(i);
                isFaceFound=true;
                float x1 = Math.max(face.getPosition().x,0);
                float y1 = Math.max(face.getPosition().y,0);
                float x2 = Math.min(x1 + face.getWidth(),frame.getBitmap().getWidth());
                float y2 = Math.min(y1 + face.getHeight(),frame.getBitmap().getHeight());
                float reulerZ=face.getEulerZ();
                Bitmap tempbitmap2 = Bitmap.createBitmap(tempBitmap, (int) x1, (int) y1, (int) (x2 - x1), (int) (y2 - y1));
                tempbitmap2=RotateBitmap(tempbitmap2,reulerZ);
                tempbitmap2 = Bitmap.createScaledBitmap(tempbitmap2, 96, 96, true);
                convertBitmapToByteBuffer(tempbitmap2);
                interpreter.run(imgData, Answer);
                String textToShow = String.format("%.1f", (Answer[0][0]*5-1)/4 * 10);
                textToShow = textToShow + "/10";
                int width= tempCanvas.getWidth();
                //int height=tempCanvas.getHeight();
                int fontsize=Math.max(width/20,imgView.getWidth()/20);
                fontPaint.setTextSize(width/15);
                tempCanvas.drawText(textToShow, x1, y1-10, fontPaint);
                tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
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
        for (int i = 0; i < 96; ++i) {
            for (int j = 0; j < 96; ++j) {
                final int val = intValues[pixel++];
                imgData.putFloat((((val >> 16) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val >> 8) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
                imgData.putFloat((((val) & 0xFF)-IMAGE_MEAN)/IMAGE_STD);
            }
        }
        long endTime = SystemClock.uptimeMillis();
        Log.d("TIME", "Timecost to put values into ByteBuffer: " + Long.toString(endTime - startTime));
    }


    private MappedByteBuffer loadModelFile(Activity activity) throws IOException {
        AssetFileDescriptor fileDescriptor = activity.getAssets().openFd("model.tflite");
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
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Here is My BEATYSCORE");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(imagePath));
        startActivity(Intent.createChooser(shareIntent, "Send your image"));
        //Log.e("img", filepath.toString());
        Log.e("img", imagePath.toString());
    }

    public static Bitmap RotateBitmap(Bitmap source, float angle)
    {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);

    }
}