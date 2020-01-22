package com.beautyfromphoto.androidfacedetection ;


import android.app.Activity;
import android.content.Intent;

import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;


import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import com.beautyfromphoto.androidfacedetection.R;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.AdRequest;


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


//import android.support.v7.app.AppCompatActivity;

public class MainActivity extends Activity {

    private static final int RQS_LOADIMAGE = 1;
    private Button btnLoad, btnDetFace, btnSave;
    private ImageView imgView;
    private Bitmap myBitmap;
    private int[] intValues = new int[112 * 112];
    private static final int IMAGE_MEAN = 0;
    private static final float IMAGE_STD = 1.0f;
    private Bitmap tempBitmap;
    public boolean isFaceFound=false;
    ByteBuffer imgData = ByteBuffer.allocateDirect(
            4 * 1 * 112 * 112 * 3);

    Interpreter interpreter;



    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btnLoad = (Button)findViewById(R.id.btnLoad);
        btnDetFace = (Button)findViewById(R.id.btnDetectFace);
        imgView = (ImageView)findViewById(R.id.imgview);
        btnSave = (Button)findViewById(R.id.btnSave);

        // ADS

        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
            }
        });
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);

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
                if(myBitmap == null){
                    Toast.makeText(MainActivity.this,
                            "myBitmap == null",
                            Toast.LENGTH_LONG).show();
                }
                else{
                    isFaceFound=false;
                    detectFace();
                    if (isFaceFound=true){
                    Toast.makeText(MainActivity.this,
                            "Done",
                            Toast.LENGTH_LONG).show();
                    }
                    else
                        {
                            Toast.makeText(MainActivity.this,
                                    "Faces not found",
                                    Toast.LENGTH_LONG).show();
                        }
                }
            }
        });

        btnSave.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                //Intent intent = new Intent();
                if (tempBitmap!=null){
                    File path = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "Images");
                    if(!path.exists()){
                        path.mkdirs();
                    }
                    Long tsLong = System.currentTimeMillis()/1000;
                    String ts = tsLong.toString();
                    File outFile = new File(path, ts + ".jpeg");

                    try {
                        FileOutputStream outputStream = new FileOutputStream(outFile);
                        tempBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                        outputStream.close();
                        Toast.makeText(MainActivity.this,
                                "saved at " +getExternalFilesDir(Environment.DIRECTORY_PICTURES) +"/"+ "Images"+ ts + ".jpeg",
                                Toast.LENGTH_LONG).show();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                else{Toast.makeText(MainActivity.this,
                        "Empty file",
                        Toast.LENGTH_LONG).show();
                };
            }
        }
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RQS_LOADIMAGE
                && resultCode == RESULT_OK){
            try {
                InputStream inputStream =
                        getContentResolver().openInputStream(data.getData());
                myBitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                imgView.setImageBitmap(myBitmap);

            } catch (FileNotFoundException e) {
                e.printStackTrace();
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
    private void detectFace(){

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

        float[][] labelProbArray = new float[1][1];

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
                Bitmap tempbitmap2 = Bitmap.createBitmap(tempBitmap, (int)x1, (int)y1, (int) (x2-x1), (int) (y2-y1));
                tempbitmap2 = Bitmap.createScaledBitmap(tempbitmap2, 112, 112, true);
                convertBitmapToByteBuffer(tempbitmap2);
                interpreter.run(imgData, labelProbArray);
                String textToShow = String.format("%.3f", labelProbArray[0][0] * 10);
                textToShow = textToShow + " ";
                int width= tempCanvas.getWidth();
                //int height=tempCanvas.getHeight();
                int fontsize=Math.max(width/25,imgView.getWidth()/25);
                fontPaint.setTextSize(fontsize);
                tempCanvas.drawText(textToShow, x1, y1-10, fontPaint);
                tempCanvas.drawRoundRect(new RectF(x1, y1, x2, y2), 2, 2, myRectPaint);
            }
            imgView.setImageDrawable(new BitmapDrawable(getResources(),tempBitmap));
        }

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

}


