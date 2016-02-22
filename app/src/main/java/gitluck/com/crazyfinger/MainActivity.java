package gitluck.com.crazyfinger;

import android.app.Activity;
import android.support.v4.view.MotionEventCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import android.util.Log;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.SurfaceView;
import android.widget.ImageButton;

import org.opencv.core.Core;
import org.opencv.core.Mat;


import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    private static final String TAG = "MainActivity";
    private static final int SAMPLE_MODE = 1;
    private static final int DETECT_MODE = 2;


    private Mat matRgba = null;
    private Mat matRgb = null;
    private Mat matInter = null;
    private Mat matBin = null;
    private Mat matBinTmp = null;
    private Mat[] matSample = null;
    private HandGesture handGesture = null;

    private Point[][] pointSample = null;
    private double[][] doubleColor = null;
    private int mode = SAMPLE_MODE;

    private static final Scalar scalarRed = new Scalar(255, 0, 0, 255);
    private static final Scalar scalarGreen = new Scalar(0, 255, 0, 255);
    private static final Scalar scalarBlue = new Scalar(0, 0, 255, 255);

    private double[][] lowerBound = new double[7][3];
    private double[][] upperBound = new double[7][3];

    private Scalar lowerThreshold = new Scalar(0, 0, 0);
    private Scalar upperThreshold = new Scalar(0, 0, 0);

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();

                    mOpenCvCameraView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            int action = MotionEventCompat.getActionMasked(event);
                            switch (action) {
                                case (MotionEvent.ACTION_DOWN) :
                                    Log.i(TAG, "ACTION_DOWN");
                                    if (mode == SAMPLE_MODE) {
                                        mode = DETECT_MODE;
                                    }
                                    return false;
                                default:
                                    return true;
                            }
                        }
                    });

                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.HelloOpenCvView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);
        //setContentView(R.layout.activity_main);

        //initial private data
        pointSample = new Point[7][2];
        for (int i = 0; i < 7; i++)  {
            for (int j = 0; j < 2; j++) {
                pointSample[i][j] = new Point();
            }
        }

        doubleColor = new double[7][3];
        initialBound(50, 50, 10, 10, 10, 10);
    }

    @Override
    public void onResume()
    {
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_9, this, mLoaderCallback);
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
        Log.i(TAG, "enter onCameraViewStarted");

        if (matRgb == null) {
            matRgb = new Mat();
        }

        if (matInter == null) {
            matInter = new Mat();
        }

        if (matBin == null) {
            matBin = new Mat();
        }

        if (matSample == null) {
            matSample = new Mat[7];
            for (int i = 0; i < 7; i++) {
                matSample[i] = new Mat();
            }
        }

        if (matBinTmp == null) {
            matBinTmp = new Mat();
        }

        if (handGesture == null) {
            handGesture = new HandGesture();
        }
    }

    public void onCameraViewStopped() {
        Log.i(TAG, "enter onCameraViewStopped");
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matRgba = inputFrame.rgba();
        Core.flip(matRgba, matRgba, 1);
        Imgproc.GaussianBlur(matRgba, matRgba, new Size(5, 5), 5, 5);
        Imgproc.cvtColor(matRgba, matRgb, Imgproc.COLOR_RGBA2BGR);
        Imgproc.cvtColor(matRgba, matInter,Imgproc.COLOR_RGB2Lab);
        if (mode == SAMPLE_MODE) {
            sampleHand(matRgba);
        } else if (mode == DETECT_MODE) {
            binImgProcess(matInter, matBin);
            makeContours();

            return matRgba;
        }

        return matRgba;
    }

    public void sampleHand(Mat input) {
        int rows = input.rows();
        int cols = input.cols();
        int windowsLen = rows/25;

        pointSample[0][0].x = cols*13/24;
        pointSample[0][0].y = rows*6/24;

        pointSample[1][0].x = cols*11/24;
        pointSample[1][0].y = rows*10/24;

        pointSample[2][0].x = cols*14/24;
        pointSample[2][0].y = rows*10/24;

        pointSample[3][0].x = cols*13/24;
        pointSample[3][0].y = rows*13/24;

        pointSample[4][0].x = cols*17/24;
        pointSample[4][0].y = rows*13/24;

        pointSample[5][0].x = cols*17/24;
        pointSample[5][0].y = rows*6/24;

        pointSample[6][0].x = cols*17/24;
        pointSample[6][0].y = rows*10/24;

        for (int i = 0; i < 7; i++) {
            pointSample[i][1].x = pointSample[i][0].x + windowsLen;
            pointSample[i][1].y = pointSample[i][0].y + windowsLen;
        }

        for (int i = 0; i < 7; i++) {
            Core.rectangle(input, pointSample[i][0], pointSample[i][1], scalarGreen, 1);

        }

        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 3; j++) {
                doubleColor[i][j] = (matInter.get((int)(pointSample[i][0].y + windowsLen / 2), (int)(pointSample[i][0].x)))[j]; // important
            }
        }
    }

    public void binImgProcess(Mat input, Mat output) {
        int cols = input.cols();
        int rows = input.rows();

        bounderyLimit();
        backgroundSubtract(input, matBinTmp);
        matBinTmp.copyTo(output);
    }

    public void initialBound(double l0, double h0, double l1, double h1, double l2, double h2) {
        lowerBound[0][0] = l0;
        lowerBound[0][1] = l1;
        lowerBound[0][2] = l2;
        upperBound[0][0] = h0;
        upperBound[0][1] = h1;
        upperBound[0][2] = h2;
    }

    public void bounderyLimit() {

        for (int i = 1; i < 7; i++) {
            for (int j = 0; j < 3; j++) {
                lowerBound[i][j] = lowerBound[0][j];
                upperBound[i][j] = upperBound[0][j];
            }
        }

        for (int i = 0; i < 7; i++) {
            for (int j = 0; j < 3; j++) {
                if (doubleColor[i][j] - lowerBound[i][j] < 0) {
                    lowerBound[i][j] = doubleColor[i][j];
                }

                if (doubleColor[i][j] + upperBound[i][j] > 255) {
                    upperBound[i][j] = 255 - doubleColor[i][j];
                }
            }
        }
    }

    public void backgroundSubtract(Mat input, Mat output) {
        for (int i = 0; i < 7; i++) {
            lowerThreshold.set(new double[]{doubleColor[i][0] - lowerBound[i][0], doubleColor[i][1] - lowerBound[i][1],
            doubleColor[i][2] - lowerBound[i][2]});
            upperThreshold.set(new double[]{doubleColor[i][0] + upperBound[i][0], doubleColor[i][1] + upperBound[i][1],
            doubleColor[i][2] + upperBound[i][2]});

            Core.inRange(input, lowerThreshold, upperThreshold, matSample[i]);
        }

        output.release();
        matSample[0].copyTo(output);

        for (int i = 1; i < 7; i++) {
            Core.add(output, matSample[i], output);
        }

        Imgproc.medianBlur(output, output, 3);
    }

    public void makeContours() {
        // find contours
        handGesture.contours.clear();
        Imgproc.findContours(matBin, handGesture.contours, handGesture.hie, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        // find the convex hull object for each contour
        handGesture.maxIndex = handGesture.findBiggestContours();

        if (handGesture.maxIndex > -1) {
            //handGesture.approximates.fromList(handGesture.contours.get(handGesture.maxIndex).toList());
            //Imgproc.approxPolyDP(handGesture.approximates, handGesture.approximates, 2, true);

            //Imgproc.drawContours(matRgba, handGesture.contours, handGesture.maxIndex, scalarRed, 3);
            handGesture.bRect = Imgproc.boundingRect(handGesture.contours.get(handGesture.maxIndex));

            Imgproc.convexHull(handGesture.contours.get(handGesture.maxIndex), handGesture.hullI, false);
            handGesture.hullP.clear();
            for (int i = 0; i < handGesture.contours.size(); i++) {
                handGesture.hullP.add(new MatOfPoint());
            }
            int[] indexHull = handGesture.hullI.toArray();
            List<Point> listPoint = new ArrayList<>();
            Point[] maxContour = handGesture.contours.get(handGesture.maxIndex).toArray();
            for (int i = 0; i < indexHull.length; i++) {
                listPoint.add(maxContour[indexHull[i]]);
            }
            handGesture.hullP.get(handGesture.maxIndex).fromList(listPoint);
            listPoint.clear();

            if (handGesture.isHandDetection(matRgba)) {
                Core.rectangle(matRgba, handGesture.bRect.tl(), handGesture.bRect.br(), scalarGreen, 4);
                Imgproc.drawContours(matRgba, handGesture.hullP, handGesture.maxIndex, scalarBlue, 4);
            }

        }
    }





}
