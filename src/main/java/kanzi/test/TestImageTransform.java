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

import kanzi.transform.DCT8;
import kanzi.IntTransform;
import kanzi.transform.WHT4;
import kanzi.transform.WHT8;
import kanzi.util.ImageQualityMonitor;

public class TestImageTransform
{
  public static void main(String[] args)
  {
        javax.swing.ImageIcon icon = new javax.swing.ImageIcon("C:\\temp\\lena.jpg");
        java.awt.Image image = icon.getImage();
        int w = 512; //image.getWidth(null) & 0xFFF8;
        int h = 512; //image.getHeight(null) & 0xFFF8;
        java.awt.GraphicsDevice gs = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        java.awt.GraphicsConfiguration gc = gs.getDefaultConfiguration();
        java.awt.image.BufferedImage img = gc.createCompatibleImage(w, h, java.awt.Transparency.OPAQUE);
        img.getGraphics().drawImage(image, 0, 0, null);

        int[] rgb = new int[w*h];
        // Do NOT use img.getRGB(): it is more than 10 times slower than
        // img.getRaster().getDataElements()
        img.getRaster().getDataElements(0, 0, w, h, rgb);

        for (int i=0; i<rgb.length; i++)
            rgb[i] &= 255;

        img.getRaster().setDataElements(0, 0, w, h, rgb);

        javax.swing.JFrame frame = new javax.swing.JFrame("Original");
        frame.setBounds(200, 100, w, h);
        frame.add(new javax.swing.JLabel(new javax.swing.ImageIcon(img)));
        frame.setVisible(true);

        DCT8 dct8 = new DCT8();
        transform(dct8, w, h, rgb, 8, "Discrete Cosine Transform 8x8");

        WHT8 wht8 = new WHT8();
        transform(wht8, w, h, rgb, 8, "Walsh-Hadamard Transform 8x8");

        WHT4 wht4 = new WHT4();
        transform(wht4, w, h, rgb, 4, "Walsh-Hadamard Transform 4x4");
  }


  private static int[] transform(IntTransform transform, int w, int h, int[] rgb, int dim, String title)
  {
    int len = w * h;
    int[] rgb2 = new int[len];
    int[] data = new int[64];
    long sum = 0L;
    int iter = 1000;

    for (int ii=0; ii<iter; ii++)
    {
       for (int y=0; y<h; y+=dim)
       {
           for (int x=0; x<w; x+=dim)
           {
              int idx = 0;

              for (int j=y; j<y+dim; j++)
              {
                 int offs = j * w;

                 for (int i=x; i<x+dim; i++)
                     data[idx++] = rgb[offs+i];
              }

              long before = System.nanoTime();
              data = transform.forward(data, 0);
              data = transform.inverse(data, 0);
              long after = System.nanoTime();
              sum += (after - before);
              
              idx = 0;

              for (int j=y; j<y+dim; j++)
              {
                 int offs = j * w;

                 for (int i=x; i<x+dim; i++)
                     rgb2[offs+i] = data[idx++];
              }
           }
       }
    }

    System.out.println(title);
    System.out.println("Elapsed time for "+iter+" iterations [ms]: "+sum/1000000L);
    int psnr1024 = new ImageQualityMonitor(w, h).computePSNR(rgb, rgb2);
    int ssim1024 = new ImageQualityMonitor(w, h).computeSSIM(rgb, rgb2);
    //System.out.println("PSNR: "+(float) psnr256 / 256);
    title += " - PSNR: ";
    title += ((float) psnr1024 / 1024);
    title += " - SSIM: ";
    title += ((float) ssim1024 / 1024);

    java.awt.GraphicsDevice gs = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
    java.awt.GraphicsConfiguration gc = gs.getDefaultConfiguration();
    java.awt.image.BufferedImage img = gc.createCompatibleImage(w, h, java.awt.Transparency.OPAQUE);
    img.getRaster().setDataElements(0, 0, w, h, rgb2);
    javax.swing.ImageIcon icon = new javax.swing.ImageIcon(img);
    javax.swing.JFrame frame = new javax.swing.JFrame(title);
    frame.setBounds(600, 100, w, h);
    frame.add(new javax.swing.JLabel(icon));
    frame.setVisible(true);

    return rgb;
  }

}
