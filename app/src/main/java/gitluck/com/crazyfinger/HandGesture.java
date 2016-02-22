package gitluck.com.crazyfinger;

import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Rect;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiao on 2/21/16.
 */
public class HandGesture {
    public Mat hie = new Mat();
    public List<MatOfPoint> hullP = new ArrayList<MatOfPoint>();
    public MatOfInt hullI = new MatOfInt();
    public MatOfInt4 defects = new MatOfInt4();
    public MatOfPoint2f approximates = new MatOfPoint2f();
    public List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
    public int maxIndex = -1;
    public Rect bRect;
    public boolean isHand = false;

    // change the value of maxIndex to indexOfBiggestContour
    public int findBiggestContours() {
        int indexOfBiggestContour = -1;
        int sizeOfBiggestContour = 0;

        for (int i = 0; i < contours.size(); i++) {
            int size = contours.get(i).toList().size();
            if ( size > sizeOfBiggestContour) {
                sizeOfBiggestContour = size;
                indexOfBiggestContour = i;
            }
        }

        return indexOfBiggestContour;
    }

    public boolean isHandDetection(Mat input) {
        int center_x = 0;
        if (bRect != null) {
            center_x = bRect.x + bRect.width / 2;
        }
        if (maxIndex == -1) {
            isHand = false;
        } else if (bRect == null) {
            isHand = false;
        } else if ((bRect.width == 0) || (bRect.height == 0)) {
            isHand = false;
        } else if ((center_x < input.cols() / 4) || (center_x > input.cols() * 3 / 4)) {
            isHand = false;
        } else {
            isHand = true;
        }
         return isHand;
    }


}
