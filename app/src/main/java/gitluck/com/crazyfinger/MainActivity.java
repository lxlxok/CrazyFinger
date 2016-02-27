package gitluck.com.crazyfinger;

import android.app.Activity;
import android.support.v4.view.MotionEventCompat;
import android.os.Bundle;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;

import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.SurfaceView;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;


import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity implements CvCameraViewListener2 {

    private CameraBridgeViewBase mOpenCvCameraView;
    private static final String TAG = "MainActivityTag";
    private static final int SAMPLE_NUME = 10;

    private Mat matRgba;
    private Mat matBin;
    private HandGesture handGesture;



    private boolean handColorIsSampled = false;

    private static final Scalar scalarRed = new Scalar(255, 0, 0, 255);
    private static final Scalar scalarGreen = new Scalar(0, 255, 0, 255);
    private static final Scalar scalarBlue = new Scalar(0, 0, 255, 255);


    private Scalar[] lowerThreshold = new Scalar[SAMPLE_NUME];
    private Scalar[] upperThreshold = new Scalar[SAMPLE_NUME];

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View v, MotionEvent event) {
                            int activity = MotionEventCompat.getActionMasked(event);
                            switch (activity) {
                                case (MotionEvent.ACTION_DOWN) :
                                    if (handColorIsSampled == false) {
                                        handColorIsSampled = true;
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
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.OpenCvMainView);
        mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCvCameraView.setCvCameraViewListener(this);

        // initialize global variable
        for (int i = 0; i < SAMPLE_NUME; i++) {
            lowerThreshold[i] = new Scalar(0,0,0);
            upperThreshold[i] = new Scalar(0,0,0);
        }

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
        matRgba = new Mat();
        matBin = new Mat();
        handGesture = new HandGesture();

    }

    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        matRgba = inputFrame.rgba();
        Core.flip(matRgba, matRgba, 1);

        if (handColorIsSampled == false) {
            sampleHand(matRgba, lowerThreshold, upperThreshold);
        } else {
            binImgProcess(matRgba, matBin);
            produceContours(matBin);
        }

        return matRgba;
    }


    public void sampleHand(Mat input, Scalar[] lowerThresholdOut, Scalar[] upperThresholdOut) {
        Mat matHLS = new Mat();
        Imgproc.cvtColor(input, matHLS, Imgproc.COLOR_RGB2HLS);

        int rows = matHLS.rows();
        int cols = matHLS.cols();
        int windowsLen = rows / 30;
        Point[][] pointSample = new Point[SAMPLE_NUME][2];

        for (int i = 0; i < SAMPLE_NUME; i++) {
            for (int j = 0; j < 2; j++) {
                pointSample[i][j] = new Point();
            }
        }


        pointSample[0][0].x = cols * 0.7;
        pointSample[0][0].y = rows * 0.6;

        pointSample[1][0].x = cols * 0.5;
        pointSample[1][0].y = rows * 0.5;

        pointSample[2][0].x = cols * 0.4;
        pointSample[2][0].y = rows * 0.4;

        pointSample[3][0].x = cols * 0.5;
        pointSample[3][0].y = rows * 0.3;

        pointSample[4][0].x = cols * 0.6;
        pointSample[4][0].y = rows * 0.2;

        pointSample[5][0].x = cols * 0.75;
        pointSample[5][0].y = rows * 0.45;

        pointSample[6][0].x = cols * 0.825;
        pointSample[6][0].y = rows * 0.35;

        pointSample[7][0].x = cols * 0.75;
        pointSample[7][0].y = rows * 0.25;

        pointSample[8][0].x = cols * 0.9;
        pointSample[8][0].y = rows * 0.45;

        pointSample[9][0].x = cols * 0.9;
        pointSample[9][0].y = rows * 0.25;


        for (int i = 0; i < SAMPLE_NUME; i++) {
            pointSample[i][1].x = pointSample[i][0].x + windowsLen;
            pointSample[i][1].y = pointSample[i][0].y + windowsLen;
        }

        // set the green rectangle to the camera view to help detect and sample hand color value
        for (int i = 0; i < SAMPLE_NUME; i++) {
            Core.rectangle(input, pointSample[i][0], pointSample[i][1], scalarGreen, 3);

        }

        // sample the color value in the square
        double[][] sampleColor = new double[SAMPLE_NUME][3];
        for (int i = 0; i < SAMPLE_NUME; i++) {
            for (int j = 0; j < 3; j++) {
                sampleColor[i][j] = averageColor(matHLS, pointSample[i][0], pointSample[i][1])[j];
            }
        }

        // the value of lowerBound and upperBound is refered from internet

        double[][] lowerBound = new double[SAMPLE_NUME][3];
        double[][] upperBound = new double[SAMPLE_NUME][3];

        lowerBound[0][0] = 12;
        lowerBound[0][1] = 30;
        lowerBound[0][2] = 80;
        upperBound[0][0] = 7;
        upperBound[0][1] = 40;
        upperBound[0][2] = 80;

        for (int i = 1; i < SAMPLE_NUME; i++) {
            for (int j = 0; j < 3; j++) {
                lowerBound[i][j] = lowerBound[0][j];
                upperBound[i][j] = upperBound[0][j];
            }
        }


        // get the Threshold value for each sample point
        for (int i = 0; i < SAMPLE_NUME; i++) {
            for (int j = 0; j < 3; j++) {
                if (sampleColor[i][j] - lowerBound[i][j] < 0) {
                    lowerBound[i][j] = sampleColor[i][j];
                }
                if (sampleColor[i][j] + upperBound[i][j] > 255){
                    upperBound[i][j] = 255 - sampleColor[i][j];
                }
            }
            lowerThresholdOut[i].set(new double[]{sampleColor[i][0] - lowerBound[i][0], sampleColor[i][1] - lowerBound[i][1], sampleColor[i][2] - lowerBound[i][2]});
            upperThresholdOut[i].set(new double[]{sampleColor[i][0] + upperBound[i][0], sampleColor[i][1] + upperBound[i][1], sampleColor[i][2] + upperBound[i][2]});

        }

    }

    public double[] averageColor(Mat input, Point leftUp, Point rightDown) {
        Mat matTemp = new Mat(input, new Rect(leftUp, rightDown));
        MatOfDouble mean = new MatOfDouble();
        MatOfDouble stdDv = new MatOfDouble();
        Core.meanStdDev(matTemp, mean, stdDv);
        return mean.toArray();
    }


    public void binImgProcess(Mat input, Mat output) {
        Mat matHLS = new Mat();
        Mat matHalf = new Mat();
        Mat matTemp = new Mat();

        Imgproc.pyrDown(input, matHalf);
        Imgproc.cvtColor(matHalf, matHLS,Imgproc.COLOR_RGB2HLS);

        Mat[] matSample = new Mat[SAMPLE_NUME];
        for (int i = 0; i < SAMPLE_NUME; i++) {
            matSample[i] = new Mat();
        }

        for (int i = 0; i < SAMPLE_NUME; i++) {

            Core.inRange(matHLS, lowerThreshold[i], upperThreshold[i], matTemp);
            matTemp.convertTo(matSample[i], CvType.CV_8U);
        }

        matSample[0].copyTo(output);

        for (int i = 1; i < SAMPLE_NUME; i++) {
            Core.add(output, matSample[i], output);
        }

        Imgproc.medianBlur(output, output, 9);
        Imgproc.pyrUp(output, output);

    }


    public void produceContours(Mat input) {
        // find contours
        handGesture.contours.clear();
        Imgproc.findContours(input, handGesture.contours, handGesture.hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_NONE);
        // find the convex hull object for each contour
        handGesture.maxIndex = handGesture.indexOfLongestContours();

        if (handGesture.maxIndex != -1 && handGesture.contours.size() > handGesture.maxIndex) {
            handGesture.longestContours = handGesture.contours.get(handGesture.maxIndex);
            handGesture.boundingRect = Imgproc.boundingRect(handGesture.longestContours);
            Imgproc.convexHull(handGesture.longestContours, handGesture.intHull, false);
            handGesture.pointHull.clear();
            for (int i = 0; i < handGesture.contours.size(); i++) {
                handGesture.pointHull.add(new MatOfPoint());
            }
            List<Point> contourPointLists = handGesture.longestContours.toList();
            List<Integer> hullIntLists = handGesture.intHull.toList();
            List<Point> hullPointLists = new ArrayList<Point>();
            for (int i = 0; i < hullIntLists.size(); i++) {
                hullPointLists.add(contourPointLists.get(hullIntLists.get(i)));
            }
            handGesture.pointHull.get(handGesture.maxIndex).fromList(hullPointLists);

            if (handGesture.longestContours.toList().size() > 3) {
                Imgproc.convexityDefects(handGesture.longestContours, handGesture.intHull, handGesture.convexityDefects);
                handGesture.filterDefects();
            }

            if (handGesture.isHandDetection(matRgba)) {
                Core.rectangle(matRgba, handGesture.boundingRect.tl(), handGesture.boundingRect.br(), scalarGreen, 4);
                Imgproc.drawContours(matRgba, handGesture.pointHull, handGesture.maxIndex, scalarBlue, 4);
                //Imgproc.drawContours(matRgba, handGesture.contours, handGesture.maxIndex, scalarBlue, 6);
                handGesture.drawDefects(matRgba, handGesture.longestContours, handGesture.convexityDefects);


            }

        }
    }


}
