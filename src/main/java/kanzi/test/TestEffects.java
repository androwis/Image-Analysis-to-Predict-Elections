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

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.Random;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import kanzi.VideoEffectWithOffset;
import kanzi.filter.ContrastFilter;
import kanzi.filter.FastBilateralFilter;
import kanzi.filter.GaussianFilter;
import kanzi.filter.LightingEffect;
import kanzi.filter.SobelFilter;


public class TestEffects
{
    public static void main(String[] args)
    {
        try
        {
            String fileName = (args.length > 0) ? args[0] : "c:\\temp\\lena.jpg";
            ImageIcon icon = new ImageIcon(fileName);
            Image image = icon.getImage();
            int w = image.getWidth(null);
            int h = image.getHeight(null);            
            w &= -7;
            h &= -7;
            
            if ((w < 256) || (h < 256))
            {
               System.out.println("The image dimensions must be at least 256");
               System.exit(1);
            }
            
            System.out.println(w+"x"+h);
            GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            BufferedImage img = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
            img.getGraphics().drawImage(image, 0, 0, null);
            BufferedImage img2 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
            int[] source = new int[w*h];
            int[] dest = new int[w*h];
            int[] tmp = new int[w*h];
            
            // Sanity check, prefill the destination image
            for (int i=0; i<dest.length; i++)
               dest[i] = i ;
            
            // Do NOT use img.getRGB(): it is more than 10 times slower than
            // img.getRaster().getDataElements()
            img.getRaster().getDataElements(0, 0, w, h, source);
            System.arraycopy(source, 0, dest, 0, w * h);
            System.arraycopy(source, 0, tmp, 0, w * h);

            int x, y, dw, dh;
            dw = 128;
            dh = 128;
            Random rnd = new Random();
            MovingEffect[] effects = new MovingEffect[5];
            x = 64   + rnd.nextInt(10);
            y = 64   + rnd.nextInt(60);
            effects[0] = new MovingEffect(new SobelFilter(dw, dh, y*w+x, w),
                    x, y, 1, 1, "Sobel");
            x = 128 + rnd.nextInt(10);
            y = 192 + rnd.nextInt(60);
            effects[1] = new MovingEffect(new GaussianFilter(dw, dh, y*w+x, w, 100, 3),
                    x, y, 1, -1, "Gaussian");
            x = 192 + rnd.nextInt(10);
            y = 128 + rnd.nextInt(60);
            effects[2] = new MovingEffect(new FastBilateralFilter(dw, dh, y*w+x, w, 30.0f, 0.03f, 4, 0, 3),
                    x, y, -1, 1, "Bilateral");
            x = 256 + rnd.nextInt(10);
            y = 192 + rnd.nextInt(60);
            boolean bump = true;
            effects[3] = new MovingEffect(new LightingEffect(dw, dh, y*w+x, w, 64, 64, 64, 100, bump),
                    x, y, -1, -1, ((bump==false)?"Lighting":"Lighting+Bump"));
            x = 128 + rnd.nextInt(10);
            y =  64 + rnd.nextInt(60);
            effects[4] = new MovingEffect(new ContrastFilter(dw, dh, y*w+x, w, 115),
                    x, y, 2, -1, "Contrast");

            for (MovingEffect e : effects)
            {
               e.effect.apply(tmp, dest);
               int[] t = tmp;
               tmp = dest;
               tmp = t;
            }
            
            img2.getRaster().setDataElements(0, 0, w, h, dest);
                 
            JFrame frame2 = new JFrame("Filters");
            frame2.setBounds(700, 150, w, h);
            ImageIcon newIcon = new ImageIcon(img2);
            frame2.add(new JLabel(newIcon));         
            frame2.setVisible(true);
            frame2.createBufferStrategy(2);

            int nn = 0;
            int nn0 = 0;
            long delta = 0;
            String sfps = "";

            while (++nn < 10000)
            {
               long before = System.nanoTime();
               System.arraycopy(source, 0, dest, 0, w * h);

               for (MovingEffect e : effects)
               {
                  e.effect.apply(tmp, dest);
                  e.x += e.vx;
                  e.y += e.vy;
                  e.effect.setOffset(e.y*w+e.x);

                  if (e.x + dw > (w*15/16))
                     e.vx = - e.vx;

                  if (e.x < (w/16))
                     e.vx = - e.vx;

                  if (e.y + dh > (h*15/16))
                     e.vy = - e.vy;

                  if (e.y < (h/16))
                     e.vy = - e.vy;

                  int[] t = tmp;               
                  tmp = dest;
                  tmp = t;
               }

               img2.getRaster().setDataElements(0, 0, w, h, dest);
               long after = System.nanoTime();
               delta += (after - before);

               if (delta >= 1000000000L)
               {
                  float d = (float) delta / 1000000000L;
                  float fps = (nn - nn0) / d;
                  sfps = String.valueOf(Math.round(fps*100)/(float)100+" FPS");
                  delta = 0;
                  nn0 = nn;
                  frame2.setTitle("Filters - "+sfps);
               }

               BufferStrategy bufferStrategy = frame2.getBufferStrategy();
               // Graphics g = img2.getGraphics();
               Graphics2D g = (Graphics2D) bufferStrategy.getDrawGraphics();
               g.setColor(Color.WHITE);
               g.drawImage(img2, 0, 0, null);  

               for (MovingEffect e : effects)
               {
                  g.drawString(e.name, e.x+4, e.y+12);
                  g.drawRect(e.x, e.y, dw, dh);
               }
            
              //g.drawString(sfps, 32, 50);
               bufferStrategy.show();
               g.dispose();
               //frame2.invalidate();
               //frame2.repaint();
               //Thread.sleep(10);
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        System.exit(0);
    }
    
    
    static class MovingEffect
    {
       VideoEffectWithOffset effect;
       int x;
       int y;
       int vx;
       int vy;
       String name;
       
       MovingEffect(VideoEffectWithOffset effect, int x, int y, int vx, int vy, String name)
       {
          this.effect = effect;
          this.x = x;
          this.y = y;
          this.vx = vx;
          this.vy = vy;
          this.name = name;
       }
    }
            
}
