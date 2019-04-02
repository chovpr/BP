/*
 * Copyright 2016 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

    // THIS FILE IS MADE BY PŘEMYSL CHOVANEČEK. FILE CONTAINS METHODS LICENSED BY THE LICENSE ABOVE.
/*
 *
 * Copyright 2019 Přemysl Chovaneček. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.premca.mydetection.activity;


import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.SystemClock;
import android.os.Trace;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.example.premca.mydetection.R;
import com.example.premca.mydetection.component.OverlayView;
import com.example.premca.mydetection.detector.BorderedText;
import com.example.premca.mydetection.detector.Classifier;
import com.example.premca.mydetection.detector.ImageUtils;
import com.example.premca.mydetection.detector.MultiBoxTracker;
import com.example.premca.mydetection.detector.TFLiteObjectDetectionAPIModel;
import com.example.premca.mydetection.fragment.CameraFragment;
import com.example.premca.mydetection.model.DetectionPosition;
import com.example.premca.mydetection.model.PhotoObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import io.realm.Realm;


public class MainActivity extends BaseActivity implements ImageReader.OnImageAvailableListener, Camera.PreviewCallback {

    private static final int TF_OD_API_INPUT_SIZE = 300;
    private static final boolean TF_OD_API_IS_QUANTIZED = true;
    private static final String TF_OD_API_MODEL_FILE = "detect.tflite";
    private static final String TF_OD_API_LABELS_FILE = "labels.txt";
    private static final boolean MAINTAIN_ASPECT = false;

    private byte[][] yuvBytes = new byte[3][];
    private int[] rgbBytes = null;
    private int yRowStride;
    private byte[] luminanceCopy;
    private boolean computingDetection = false;
    private Handler handler;
    private HandlerThread handlerThread;

    private enum DetectorMode {
        TF_OD_API
    }


    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.6f;
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final DetectorMode MODE = DetectorMode.TF_OD_API;

    protected int previewWidth = 0;
    protected int previewHeight = 0;

    private long lastProcessingTimeMs;
    private long timestamp = 0;

    private Integer sensorOrientation;
    OverlayView trackingOverlay;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;
    private boolean isProcessingFrame = false;

    private MultiBoxTracker tracker;
    private BorderedText borderedText;

    private Runnable postInferenceCallback;
    private Runnable imageConverter;

    private ArrayList<RectF> mResults = new ArrayList<>();

    private int state = STATE_FREE;

    private static final int STATE_FREE = 0;
    private static final int STATE_CREATE_CAPTURE = 1;
    private static final int STATE_CAPTURING = 2;

    private static final float TEXT_SIZE_DIP = 10;
    private Classifier detector;
    public Button capture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        setContentView(R.layout.activity_main);
        setFragment();

        capture = findViewById(R.id.button_capture);
        capture.setOnClickListener(new View.OnClickListener() {
            // Button for making a photo.
            @Override
            public void onClick(View view) {
                Log.e("Cam", "onClick");
                if (state == STATE_FREE) {
                    Log.e("Cam", "set capture");
                    state = STATE_CREATE_CAPTURE;
                }
            }
        });
    }

    private void setFragment() {
        CameraFragment fragment = new CameraFragment(new CameraFragment.ConnectionCallback() {
            @Override
            public void onPreviewSizeChosen(final Size size, final int rotation) {
                previewHeight = size.getHeight();
                previewWidth = size.getWidth();
                MainActivity.this.onPreviewSizeChosen(size, rotation);
            }
        },
                this);

        getFragmentManager()
                .beginTransaction()
                .replace(R.id.container, fragment)
                .commit();
    }


    @Override
    public synchronized void onResume() {
        super.onResume();

        startBackgroundThread();

        handlerThread = new HandlerThread("inference");
        handlerThread.start();
        handler = new Handler(handlerThread.getLooper());
    }

    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        int cropSize = TF_OD_API_INPUT_SIZE;

        try {
            detector =
                    TFLiteObjectDetectionAPIModel.create(
                            getAssets(),
                            TF_OD_API_MODEL_FILE,
                            TF_OD_API_LABELS_FILE,
                            TF_OD_API_INPUT_SIZE,
                            TF_OD_API_IS_QUANTIZED);
            cropSize = TF_OD_API_INPUT_SIZE;
        } catch (final IOException e) {


            finish();
        }

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();

        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Bitmap.Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = findViewById(R.id.tracking_overlay);

        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        tracker.drawDebug(canvas);

                        final Vector<String> lines = new Vector<String>();
                        // write inference time
                        lines.add("");
                        lines.add("Inference time: " + lastProcessingTimeMs + "ms");

                        borderedText.drawLines(canvas, 10, canvas.getHeight() - 10, lines);
                    }
                });
    }

    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        byte[] originalLuminance = getLuminance();
        tracker.onFrame(
                previewWidth,
                previewHeight,
                getLuminanceStride(),
                sensorOrientation,
                originalLuminance,
                timestamp);
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;


        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        if (luminanceCopy == null) {
            luminanceCopy = new byte[originalLuminance.length];
        }
        System.arraycopy(originalLuminance, 0, luminanceCopy, 0, originalLuminance.length);
        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }

        // Runnable - applying detections, calculating inference time.
        runInBackground(
                new Runnable() {
                    @Override
                    public void run() {

                        final long startTime = SystemClock.uptimeMillis();

                        // making detections
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);

                        // calculation the inference time
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);


                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();
                        mResults.clear();
                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                mResults.add(location);
                                cropToFrameTransform.mapRect(location);
                                result.setLocation(location);
                                mappedRecognitions.add(result);
                            }
                        }

                        tracker.trackResults(mappedRecognitions, luminanceCopy, currTimestamp);
                        trackingOverlay.postInvalidate();
                        //requestRender();
                        computingDetection = false;
                    }
                });
    }

    public void onPreviewFrame(final byte[] bytes, final Camera camera) {
        if (isProcessingFrame) {
            return;
        }

        try {
            // Initialize the storage bitmaps once when the resolution is known.
            if (rgbBytes == null) {
                Camera.Size previewSize = camera.getParameters().getPreviewSize();
                previewHeight = previewSize.height;
                previewWidth = previewSize.width;
                rgbBytes = new int[previewWidth * previewHeight];
                onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
            }
        } catch (final Exception e) {

            return;
        }

        isProcessingFrame = true;
        yuvBytes[0] = bytes;
        yRowStride = previewWidth;

        imageConverter =
                new Runnable() {
                    @Override
                    public void run() {
                        ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
                    }
                };

        postInferenceCallback =
                new Runnable() {
                    @Override
                    public void run() {
                        camera.addCallbackBuffer(bytes);
                        isProcessingFrame = false;
                    }
                };
        processImage();
    }


    protected int[] getRgbBytes() {
        imageConverter.run();
        return rgbBytes;
    }

    protected int getLuminanceStride() {
        return yRowStride;
    }

    protected byte[] getLuminance() {
        return yuvBytes[0];
    }

    // preview
    protected int getScreenOrientation() {
        switch (getWindowManager().getDefaultDisplay().getRotation()) {
            case Surface.ROTATION_270:
                return 270;
            case Surface.ROTATION_180:
                return 180;
            case Surface.ROTATION_90:
                return 90;
            default:
                return 0;
        }
    }

    protected void readyForNextImage() {
        if (postInferenceCallback != null) {
            postInferenceCallback.run();
        }
    }

    protected synchronized void runInBackground(final Runnable r) {
        if (handler != null) {
            handler.post(r);
        }
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        //       mtx.postRotate(degree);
        mtx.setRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    // processing the image - making photo during the click on the button
    @Override
    public void onImageAvailable(ImageReader reader) {
        if (previewWidth == 0 || previewHeight == 0) {
            return;
        }
        if (rgbBytes == null) {
            rgbBytes = new int[previewWidth * previewHeight];
        }
        try {
            final Image image = reader.acquireLatestImage();
            if (state == STATE_CREATE_CAPTURE) {
                state = STATE_CAPTURING;
                final Image.Plane[] capturePlanes = image.getPlanes().clone();
                saveImage(capturePlanes, image);
                state = STATE_FREE;
            }

            if (image == null) {
                return;
            }

            if (isProcessingFrame) {
                image.close();
                return;
            }
            isProcessingFrame = true;
            Trace.beginSection("imageAvailable");
            final Image.Plane[] planes = image.getPlanes();
            fillBytes(planes, yuvBytes);
            yRowStride = planes[0].getRowStride();
            final int uvRowStride = planes[1].getRowStride();
            final int uvPixelStride = planes[1].getPixelStride();

            imageConverter =
                    new Runnable() {
                        @Override
                        public void run() {
                            ImageUtils.convertYUV420ToARGB8888(
                                    yuvBytes[0],
                                    yuvBytes[1],
                                    yuvBytes[2],
                                    previewWidth,
                                    previewHeight,
                                    yRowStride,
                                    uvRowStride,
                                    uvPixelStride,
                                    rgbBytes);
                        }
                    };

            postInferenceCallback =
                    new Runnable() {
                        @Override
                        public void run() {
                            image.close();
                            isProcessingFrame = false;
                        }
                    };

            processImage();
        } catch (final Exception e) {

            Trace.endSection();
            return;
        }
        Trace.endSection();
    }


    // saving images, creating annotations
    private void saveImage(Image.Plane[] imagePlanes, Image image) {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        mFile = new File(this.getExternalFilesDir(null), File.separator + timeStamp + ".jpeg");
        String filename = File.separator + timeStamp + ".xml";
        // Creating a file for the photo.
        createData(mFile.getPath(), System.currentTimeMillis());
        (new ImageSaver(imagePlanes, mFile)).saveImage(image);

        // New xml file for annotation.
        mFile1 = new File(this.getExternalFilesDir(null), filename);
        FileOutputStream fos = null;

        // Following logic takes care about annotations and write them into the xml file.

        String content = "<annotation>\n\t<folder>set</folder>\n<filename>"
                    + filename +"</filename><path>"+mFile1.getPath()+"</path>\n" +
                    "<source>\n" +
                    "    <database>Unknown</database>\n" +
                    "  </source>\n" +
                    "  <size>\n" +
                    "    <width>"+CameraFragment.usedWidth+"</width>\n" +
                    "    <height>"+CameraFragment.usedHeight+"\"</height>\n" +
                    "    <depth>3</depth>\n" +
                    "  </size>\n" +
                    "  <segmented>0</segmented>";
           int xmin = 0;
           int xmax = 0;
           int d1, d2;
        for (RectF rectF : mResults) {
            // Locating the rectangle according the axis y.
            if(rectF.bottom>CameraFragment.usedWidth/2 && rectF.top<CameraFragment.usedWidth/2) {
                 d1 = (int)rectF.bottom - CameraFragment.usedWidth/2;
                 d2 = CameraFragment.usedWidth/2 - (int)rectF.top;
                 xmin = CameraFragment.usedWidth/2 - d1;
                 xmax = d2 + CameraFragment.usedWidth/2;
            }
            else if(rectF.bottom>CameraFragment.usedWidth/2 && rectF.top>CameraFragment.usedWidth/2) {
                d1 = (int)rectF.bottom - CameraFragment.usedWidth/2;
                d2 = (int)rectF.top - CameraFragment.usedWidth/2;
                xmin = CameraFragment.usedWidth/2 - d1;
                xmax = CameraFragment.usedWidth/2 - d2;
            }
            else if(rectF.bottom<CameraFragment.usedWidth/2 && rectF.top<CameraFragment.usedWidth/2) {
                d1 = CameraFragment.usedWidth/2 - (int)rectF.bottom;
                d2 = CameraFragment.usedWidth/2 - (int)rectF.top;
                xmin = CameraFragment.usedWidth/2 + d1;
                xmax = CameraFragment.usedWidth/2 + d2;
            }

            content = content + "\n" +
                    "<object>\n" +
                    "    <name>tag</name>\n" +
                    "    <pose>Unspecified</pose>\n" +
                    "    <truncated>0</truncated>\n" +
                    "    <difficult>0</difficult>\n" +
                    "    <bndbox>\n" +
                    "<xmin>"+xmin+"</xmin>\n"+
                    "      <ymin>"+(int)rectF.left+"</ymin>\n" +
                      "      <xmax>"+xmax+"</xmax>\n"+
                    "      <ymax>"+(int)rectF.right+"</ymax>\n" +
                    "    </bndbox>\n" +
                    "  </object>";




        }


        content = content + "\n </annotation>";

            try {
                fos = new FileOutputStream(mFile1);
                fos.write(content.getBytes());
                Toast.makeText(this, "Uloženo do" + getFilesDir() + "/" + filename,
                        Toast.LENGTH_LONG).show();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }

    }

    // pushing to the database
    private void createData(String path, long miliTimestamp) {
        Realm mRealm = Realm.getDefaultInstance();
        mRealm.executeTransaction(realm -> {
            PhotoObject fotoObject = new PhotoObject();
            fotoObject.setPicturePath(path);
            fotoObject.setMiliTimestamp(miliTimestamp);
            fotoObject.setId(PhotoObject.getNextId(realm));
            fotoObject.setLat(String.valueOf(this.lat));
            fotoObject.setLng(String.valueOf(this.lng));
            realm.copyToRealmOrUpdate(fotoObject);

            int id = DetectionPosition.getNextId(realm);

            for (RectF rectF : mResults) {
                DetectionPosition position = new DetectionPosition();
                //id = DetectionPosition.getNextId(realm);
                position.setId(id++);
                position.setBottom(rectF.bottom);
                position.setLeft(rectF.left);
                position.setRight(rectF.right);
                position.setTop(rectF.top);

                realm.copyToRealmOrUpdate(position);
                fotoObject.addPosition(position);
            }
            realm.copyToRealmOrUpdate(fotoObject);
        });
    }

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    private void startBackgroundThread() {
        backgroundThread = new HandlerThread("Cam");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    private File mFile;
    private File mFile1;


    // class for imageSaving - inspired by android example
    private static class ImageSaver {

        private final Image.Plane[] imagePlanes;
        private final File mFile;

        ImageSaver(Image.Plane[] imagePlanes, File file) {
            this.imagePlanes = imagePlanes;
            mFile = file;
        }


        public void saveImage(Image image) {
            ByteBuffer buffer = this.imagePlanes[0].getBuffer();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            FileOutputStream output = null;

            // converting the preview format

            byte[] data = NV21toJPEG(YUV420toNV21(image), image.getWidth(), image.getHeight(), 100);
            try {
                output = new FileOutputStream(mFile);
                Bitmap realImage = BitmapFactory.decodeByteArray(data, 0, data.length);

                ExifInterface exif = new ExifInterface(mFile.toString());

                // Rotate the image before saving.
                Log.d("EXIF value", exif.getAttribute(ExifInterface.TAG_ORIENTATION));
                if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")) {
                    realImage = rotate(realImage, 90);
                } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")) {
                    realImage = rotate(realImage, 270);
                } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")) {
                    realImage = rotate(realImage, 180);
                } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("0")) {
                    realImage = rotate(realImage, 90);
                }
                boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 100, output);
                output.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                if (null != output) {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    // CONVERT TO PREVIEW FORMAT
    private static byte[] NV21toJPEG(byte[] nv21, int width, int height, int quality) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        YuvImage yuv = new YuvImage(nv21, ImageFormat.NV21, width, height, null);
        yuv.compressToJpeg(new Rect(0, 0, width, height), quality, out);
        return out.toByteArray();
    }

    private static byte[] YUV420toNV21(Image image) {
        Rect crop = image.getCropRect();
        int format = image.getFormat();
        int width = crop.width();
        int height = crop.height();
        Image.Plane[] planes = image.getPlanes();
        byte[] data = new byte[width * height * ImageFormat.getBitsPerPixel(format) / 8];
        byte[] rowData = new byte[planes[0].getRowStride()];

        int channelOffset = 0;
        int outputStride = 1;
        for (int i = 0; i < planes.length; i++) {
            switch (i) {
                case 0:
                    channelOffset = 0;
                    outputStride = 1;
                    break;
                case 1:
                    channelOffset = width * height + 1;
                    outputStride = 2;
                    break;
                case 2:
                    channelOffset = width * height;
                    outputStride = 2;
                    break;
            }

            ByteBuffer buffer = planes[i].getBuffer();
            int rowStride = planes[i].getRowStride();
            int pixelStride = planes[i].getPixelStride();

            int shift = (i == 0) ? 0 : 1;
            int w = width >> shift;
            int h = height >> shift;
            buffer.position(rowStride * (crop.top >> shift) + pixelStride * (crop.left >> shift));
            for (int row = 0; row < h; row++) {
                int length;
                if (pixelStride == 1 && outputStride == 1) {
                    length = w;
                    buffer.get(data, channelOffset, length);
                    channelOffset += length;
                } else {
                    length = (w - 1) * pixelStride + 1;
                    buffer.get(rowData, 0, length);
                    for (int col = 0; col < w; col++) {
                        data[channelOffset] = rowData[col * pixelStride];
                        channelOffset += outputStride;
                    }
                }
                if (row < h - 1) {
                    buffer.position(buffer.position() + rowStride - length);
                }
            }
        }
        return data;
    }

    protected void fillBytes(final Image.Plane[] planes, final byte[][] yuvBytes) {

        for (int i = 0; i < planes.length; ++i) {
            final ByteBuffer buffer = planes[i].getBuffer();
            if (yuvBytes[i] == null) {

                yuvBytes[i] = new byte[buffer.capacity()];
            }
            try {
                buffer.get(yuvBytes[i]);
            } catch (Exception ignored) {

            }
        }
    }
}
