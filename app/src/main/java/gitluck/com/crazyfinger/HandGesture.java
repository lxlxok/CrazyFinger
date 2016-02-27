package gitluck.com.crazyfinger;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfInt4;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xiao on 2/21/16.
 */
public class HandGesture {
    public Mat hierarchy = new Mat();
    public List<MatOfPoint> pointHull = new ArrayList<MatOfPoint>();
    public MatOfInt intHull = new MatOfInt();
    public MatOfInt4 convexityDefects = new MatOfInt4();
    public List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
    public MatOfPoint longestContours = new MatOfPoint();
    public int maxIndex = -1;
    public Rect boundingRect;

    // change the value of maxIndex to contour
    public int indexOfLongestContours() {
        int index = maxIndex;
        int maxSize = 0;

        for (int i = 0; i < contours.size(); i++) {
            int size = contours.get(i).toList().size();
            if ( size > maxSize) {
                index = i;
                maxSize = size;
            }
        }

        return index;
    }

    public void filterDefects() {

    }

    public boolean isHandDetection(Mat input) {
        if (maxIndex == -1 || boundingRect == null || boundingRect.width == 0 || boundingRect.height == 0) {
            return false;
        } else {
            return true;
        }
    }

    public void drawDefects(Mat input, MatOfPoint contours, MatOfInt4 defects) {
        List<Integer> defectsIndexLists = defects.toList();
        for (int i = 0; i < defectsIndexLists.size() / 4; i++) {
            int startIndex = defectsIndexLists.get(4 * i);
            int endIndex = defectsIndexLists.get(4 * i + 1);
            int farthestIndex = defectsIndexLists.get(4 * i + 2);
            double fixedDepth = defectsIndexLists.get(4 * i + 3) / 256;
            Point pointStart = contours.toList().get(startIndex);
            Point pointEnd = contours.toList().get(endIndex);
            Point pointFarthest = contours.toList().get(farthestIndex);
            //Core.line(input, pointStart, pointFarthest, new Scalar(255, 0, 0), 4);
            if (fixedDepth > 150) {
                Core.circle(input, pointStart, 20, new Scalar(255, 0, 0), 10);
                Core.circle(input, pointEnd, 20, new Scalar(255, 0, 0), 10);
                Core.circle(input, pointFarthest, 20, new Scalar(0, 255, 0), 10);
            }
        }
    }

}
