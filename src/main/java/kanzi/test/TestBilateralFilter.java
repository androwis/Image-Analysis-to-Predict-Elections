/*
Copyright 2011 Frederic Langlet
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
you may obtain a copy of the License at

                http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package kanzi.test;

import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import kanzi.VideoEffect;
import kanzi.filter.BilateralFilter;
import kanzi.filter.FastBilateralFilter;


public class TestBilateralFilter
{

    public static void main(String[] args)
    {
    	for (int iii=0; iii<10; iii++){
	        String fileName = (args.length > 0) ? args[0] : "/Dropbox/development/workspaces/school/ElectionPrediction/src/romney-c-"+iii+".jpg";
	        
	        ImageIcon icon = new ImageIcon(fileName);
	        Image image = icon.getImage();
	        int w = image.getWidth(null);
	        w=w-w%8;
	        int h = image.getHeight(null);
	        h=h-h%8;
	      
	        
	        GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
	        GraphicsConfiguration gc = gs.getDefaultConfiguration();
	        
	        BufferedImage img = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
	        	img.getGraphics().drawImage(image, 0, 0, null);
	        
	        BufferedImage img2 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
	        BufferedImage img3 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
	        
	        int[] src = new int[w*h];
	        int[] dst1 = new int[w*h];
	        int[] dst2 = new int[w*h];
			
	        // Do NOT use img.getRGB(): it is more than 10 times slower than
	        // img.getRaster().getDataElements()
	        img.getRaster().getDataElements(0, 0, w, h, src);
	        float sigmaR = 30.0f;
	        float sigmaD = 0.03f;
	        VideoEffect fbf = new FastBilateralFilter(w, h, sigmaR, sigmaD);
	        fbf.apply(src, dst1);
	        
			//calculate the difference in the images.
	        double dif =0.0;
			for(int i=0; i< src.length; i++){
				dst2[i]=dst1[i];
				dif+=Math.pow(Math.pow(src[i]-dst1[i],2)/2,.5);
				dst1[i]=src[i]-dst1[i];
			}
			
			System.out.println(dif/src.length/100000);
    	}
    	
    	}
}
