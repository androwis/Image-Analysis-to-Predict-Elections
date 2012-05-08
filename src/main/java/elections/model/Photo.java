package elections.model;

// This is the support vector class for images.
//		Support vectors have 9 features
public class Photo {
	private boolean isPositive;
	

	private double faceDetected;
	// The difference between binary filter
	private double binaryFilterDifference;
	
	/* These features measure the % pixels
	 *   _______________
	 *  |	Q1	|	Q2	|
	 * 	 _______________
	 *  |	Q4	|	Q3	|
	 *   _______________
	 */
	private int quadrant1;
	private int quadrant2;
	private int quadrant3;
	private int quadrant4;
	private double HmeanDivMaxHSV;
	private double SmeanDivMaxHSV;
	private double VmeanDivMaxHSV;
	

}