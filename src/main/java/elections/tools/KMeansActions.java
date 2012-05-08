package elections.tools;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

public class KMeansActions {

	//use (H,S,V) as the feature vector

	BufferedImage image_temp;
	BufferedImage temp;
	private static Color[] k_means;
	private static int[] k_meansc = new int[4];
	private static int[] k_means1c = new int[4];
	private static int[] k_means2c = new int[4];
	private static int[] k_means3c = new int[4];
	private static int[] k_means4c = new int[4];
	private static int k_meansi[]=new int[5];
	boolean not_terminated;
	int loops, changedPixels;
	int[] histogram;
	int [] lowerbounds;

	public static void SegmentImage(String name, BufferedImage img) throws IOException{
		BufferedImage temp= deepCopy(img);
		initialize(img,5);

		// iterate through the loops.
		for(int ic=0; ic < 20; ic++){

			//loop through the images
			for(int w=0; w < img.getWidth(); w++){
				for(int h=0; h < img.getHeight(); h++){
					Color pixel = new Color(img.getRGB(w, h));
					double a = dist(hsv(pixel),hsv(k_means[0]));
					double b = dist(hsv(pixel),hsv(k_means[1]));
					double c = dist(hsv(pixel),hsv(k_means[2]));
					double d = dist(hsv(pixel),hsv(k_means[3]));
					double e = dist(hsv(pixel),hsv(k_means[4]));
					if(
							a < b 
							&& a < c
							&& a < d
							&& a < e
					){
						k_meansc[1] += pixel.getRed();
						k_meansc[2] += pixel.getGreen();
						k_meansc[3] += pixel.getBlue();
						k_meansi[0]++;
						temp.setRGB(w, h, k_means[0].getRGB());
					}else if(
							b < a
							&& b < c 
							&& b < d
							&& b < e
					){
					
						k_means1c[1] += pixel.getRed();
						k_means1c[2] += pixel.getGreen();
						k_means1c[3] += pixel.getBlue();
						k_meansi[1]++;
						temp.setRGB(w, h, k_means[1].getRGB());
					}
					else if(
							c < a
							&& c < b
							&& c < d
							&& c < e
					){
						k_means2c[1] += pixel.getRed();
						k_means2c[2] += pixel.getGreen();
						k_means2c[3] += pixel.getBlue();
						k_meansi[2]++;
						temp.setRGB(w, h, k_means[2].getRGB());
					}
					else if(
							d < a
							&& d < b
							&& d < c
							&& d < e
							){
						k_means3c[1] += pixel.getRed();
						k_means3c[2] += pixel.getGreen();
						k_means3c[3] += pixel.getBlue();
						k_meansi[3]++;
						temp.setRGB(w,h,k_means[3].getRGB());
					}
					else{
						k_means4c[1] += pixel.getRed();
						k_means4c[2] += pixel.getGreen();
						k_means4c[3] += pixel.getBlue();
						k_meansi[4]++;
						temp.setRGB(w,h,k_means[4].getRGB());
						}
				}

			}
			if(k_meansi[0]>0){
				k_means[0]=avg(k_meansc,k_meansi[0]);	
			}
			if(k_meansi[1]>0){ 
				k_means[1]=avg(k_means1c,k_meansi[1]);	
			}
			if(k_meansi[2]>0){
				k_means[2]=avg(k_means2c,k_meansi[2]);	
			}
			if(k_meansi[3]>0){	
				k_means[3]=avg(k_means3c,k_meansi[3]);	
			}
			if(k_meansi[4]>0){	
				k_means[4]=avg(k_means4c,k_meansi[4]);	
			}
			k_meansc=new int[4];
			k_means1c=new int[4];
			k_means2c=new int[4];
			k_means3c=new int[4];
			k_means4c=new int[4];
			k_meansi=new int[5];

			// Redraw the image with new segments
			//loop through the images
			int mid = temp.getRGB(img.getWidth()/2, img.getHeight()/2);
			for(int w=0; w < img.getWidth(); w++){
				for(int h=0; h < img.getHeight(); h++){
					temp.setRGB(w,h,
						(temp.getRGB(w,h) == mid) ? img.getRGB(w,h) : (w==img.getWidth()/2 || h==img.getHeight()/2) ? 255 : 0
					);
				}
			}
			if(ic%5==0)
				ImageIO.write( temp, "jpg", new File( "src/main/resources/segmented/"+ic+"-"+name) );

		}

		// finalize the image.
		ImageIO.write( temp, "jpg", new File( "src/main/resources/segmented/final-"+name));
	}


	// return the hsv conversion.
	// L = (r+g+b)/3
	// s =( r-b) /2
	// t = (2g-r-b)/4
	static double[] lst(Color c){
		double[] hsv = new double[3];
		hsv[0]=(c.getRed()+c.getBlue()+c.getGreen())/3.;
		hsv[1]=(c.getRed()-c.getBlue())/2.;
		hsv[2]=(2.*c.getGreen()-c.getRed()-c.getBlue())/4.;
		return hsv;  
	}
	
	static float[] hsv(Color c){
		return c.RGBtoHSB(c.getRed(),c.getGreen(), c.getBlue(),null);
		  
	}
	
	// return the euclidean distance.
	static double dist(float[] hsv, float[] hsv0){
		return
		Math.pow(Math.pow(Math.abs(hsv[0]-hsv0[0]),2.) +
				Math.pow(Math.abs(hsv[1]-hsv0[1]),2.) +
				Math.pow(Math.abs(hsv[2]-hsv0[2]),2.),.5);
	}
	
	// return the average color.
	static Color avg(int[] vals,int num){
		return new Color(
				(float)(vals[1]/num/255.),
				(float)(vals[2]/num/255.),
				(float)(vals[3]/num/255.)
		);
	}
	
	
	// random initializaton
	static void initialize(BufferedImage img, int num_centroids){
		k_means = new Color[num_centroids];
		k_means[0] = new Color(img.getRGB(
				(int) (img.getWidth()/2),
				(int)(img.getHeight()/2)
		));		
		for(int i=1; i<num_centroids; i++){
			k_means[i] = new Color(img.getRGB(
					(int)(Math.random()*img.getWidth()),
					(int)(Math.random()*img.getHeight())
			));		
		}
		
	}

	// I was running in on a java problem where the filters were applying to other BufferedImages
	// - BufferedImages doesn't have an exposed .clone() so i found this function online:
	static BufferedImage deepCopy(BufferedImage bi) {
		ColorModel cm = bi.getColorModel();
		boolean isAlphaPremultiplied = cm.isAlphaPremultiplied();
		WritableRaster raster = bi.copyData(null);
		return new BufferedImage(cm, raster, isAlphaPremultiplied, null);
	}

}
