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
import java.util.Arrays;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import kanzi.filter.seam.ContextResizer;


public class TestContextResizer
{
    public static void main(String[] args)
    {
        try
        {
            String fileName = "c:\\temp\\lena.jpg";
            boolean debug = false;
            boolean vertical = false;
            boolean horizontal = false;
            boolean speed = false;
            int effectPct = 10;
            boolean fileProvided = false;

            for (String arg : args)
            {
               arg = arg.trim();
               
               if (arg.equals("-help"))
               {
                   System.out.println("-help               : display this message");
                   System.out.println("-debug              : display the computed geodesics");
                   System.out.println("-strength=<percent> : number of geodesics to create (in percent of dimension)");
                   System.out.println("-vertical           : process vertical geodesics");
                   System.out.println("-horizontal         : process horizontal geodesics");
                   System.out.println("-speedtest          : run am extra speed test");
                   System.exit(0);
               }
               else if (arg.equals("-debug"))
               {
                   debug = true;
                   System.out.println("Debug set to true");
               }
               else if (arg.startsWith("-file="))
               {
                  fileName = arg.substring(6);
                  fileProvided = true;
               }
               else if (arg.equals("-vertical"))
               {
                   vertical = true;
                   System.out.println("Vertical set to true");
               }
               else if (arg.equals("-horizontal"))
               {
                   horizontal = true;
                   System.out.println("Horizontal set to true");
               }
               else if (arg.startsWith("-strength="))
               {
                  arg = arg.substring(10);
                  
                  try
                  {
                     int pct = Integer.parseInt(arg);
                     
                     if (pct < 1)
                     {
                         System.err.println("The minimum strength is 1%, the provided value is "+arg);
                         System.exit(1);
                     }
                     else if (pct > 90)
                     {
                         System.err.println("The maximum strength is 90%, the provided value is  "+arg);
                         System.exit(1);
                     }
                     else
                         effectPct = pct;                     
                  }
                  catch (NumberFormatException e)
                  {
                     System.err.println("Invalid effect strength (percentage) provided on command line: "+arg);
                  }
               }
               else if (arg.equals("-speedtest"))
               {
                   speed = true;
                   System.out.println("Speed test set to true");
               }
               else
               {
                   System.out.println("Warning: unknown option: ["+ arg + "]");
               }
            }
           
            if ((vertical == false) && (horizontal == false))
            {
               System.out.println("Warning: no direction has been selected, selecting both");
               vertical = true;
               horizontal = true;
            }

            if (fileProvided == false)
                System.out.println("No image file name provided on command line, using default value");

            System.out.println("File name set to '" + fileName + "'");
            System.out.println("Stength set to "+effectPct+"%");
            ImageIcon icon = new ImageIcon(fileName);
            Image image = icon.getImage();
            int w = image.getWidth(null);
            int h = image.getHeight(null);
            
            if ((w<0) || (h<0))
            {
                System.err.println("Cannot read or decode input file '"+fileName+"'");
                System.exit(1);
            }
            
            System.out.println("Image dimensions: "+w+"x"+h);
            GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            BufferedImage img = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
            img.getGraphics().drawImage(image, 0, 0, null);
            JFrame frame = new JFrame("Original");
            frame.setBounds(50, 50, w, h);
            frame.add(new JLabel(icon));
            frame.setVisible(true);
            int[] src = new int[w*h];
            int[] dst = new int[w*h];

            // Do NOT use img.getRGB(): it is more than 10 times slower than
            // img.getRaster().getDataElements()
            img.getRaster().getDataElements(0, 0, w, h, src);
            ContextResizer effect;

            Arrays.fill(dst, 0);
            int dir = 0;
            
            if (vertical == true) 
                dir |= ContextResizer.VERTICAL;
            
            if (horizontal == true)
                dir |= ContextResizer.HORIZONTAL;
            
            int min = Math.min(h, w);
            effect = new ContextResizer(w, h, 0, w, dir,
                    ContextResizer.SHRINK, min, min * effectPct / 100);            
            effect.setDebug(debug);
            effect.apply(src, dst);

            BufferedImage img2 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
            img2.getRaster().setDataElements(0, 0, w, h, dst);
            JFrame frame2 = new JFrame("Filter");
            frame2.setBounds(500, 80, w, h);
            ImageIcon icon2 = new ImageIcon(img2);
            frame2.add(new JLabel(icon2));
            frame2.setVisible(true);

            // Speed test
            if (speed == true)
            {
                System.out.println("Speed test");
                long sum = 0;
                int iter = 1000;

                for (int ii=0; ii<iter; ii++)
                {
                   img.getRaster().getDataElements(0, 0, w, h, src);
                   long before = System.nanoTime();
                   effect = new ContextResizer(w, h, 0, w, dir,
                        ContextResizer.SHRINK, min, min * effectPct/100);
                   effect.apply(src, dst);
                   long after = System.nanoTime();
                   sum += (after - before);
                }

                System.out.println("Elapsed [ms]: "+ sum/1000000+" ("+iter+" iterations)");
            }

            Thread.sleep(40000);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        System.exit(0);
    }
}
