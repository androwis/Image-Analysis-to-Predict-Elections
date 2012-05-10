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
import java.io.OutputStream;
import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.util.Arrays;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import kanzi.ColorModelType;
import kanzi.Global;
import kanzi.EntropyEncoder;
import kanzi.OutputBitStream;
import kanzi.bitstream.DefaultOutputBitStream;
import kanzi.entropy.RangeEncoder;
import kanzi.transform.WHT8;
import kanzi.util.color.ColorModelConverter;
import kanzi.util.ImageQualityMonitor;
import kanzi.util.IntraEncoder;
import kanzi.util.PostProcessingFilter;
import kanzi.util.ResidueBlockEncoder;
import kanzi.util.color.YCbCrColorModelConverter;


public class TestWHTImage
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
            GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            BufferedImage img = gc.createCompatibleImage(w, h, Transparency.OPAQUE);

            img.getGraphics().drawImage(image, 0, 0, null);
            System.out.println(w + "x" + h);
            int[] rgb = new int[w*h];
            int[] rgb2 = new int[w*h];
            int[] data = new int[w*h];
            // Do NOT use img.getRGB(): it is more than 10 times slower than
            // img.getRaster().getDataElements()
            img.getRaster().getDataElements(0, 0, w, h, rgb);
            byte[] tmp = new byte[w*h];

            int[] yy = new int[rgb.length];
            int[] uu = new int[rgb.length/4];
            int[] vv = new int[rgb.length/4];

            ColorModelConverter cvt;
            //cvt = new YSbSrColorModelConverter(w, h);
            cvt = new YCbCrColorModelConverter(w, h);
            cvt.convertRGBtoYUV(rgb, yy, uu, vv, ColorModelType.YUV420);

            OutputStream os = new FileOutputStream("C:\\temp\\output.bin");//new ByteArrayOutputStream(w*h);
            OutputBitStream bs = new DefaultOutputBitStream(os, w*h);
            EntropyEncoder ee = new RangeEncoder(bs);
    //        BlockCodec bc = new BlockCodec(w*h);

            int nonZero = 0;
            int ww = new IntraEncoder(w, h, w, Global.QUANTIZATION_INTRA, ee).encode(yy, null, rgb);
            nonZero += forward(yy,   w,   h, data, bs, 0);

            for (int i=0; i<tmp.length; i++)
                tmp[i] = (byte) data[i];

            //bc.encode(iba, ee);
            Arrays.fill(yy, 0);
            reverse(data,   w,   h, yy, 0);
            nonZero += forward(uu, w, h, data, bs, 1);

            for (int i=0; i<tmp.length; i++)
                tmp[i] = (byte) data[i];

            //bc.encode(iba, ee);
            Arrays.fill(uu, 0);
            reverse(data, w, h, uu, 1);
            nonZero += forward(vv, w, h, data, bs, 1);

            for (int i=0; i<tmp.length; i++)
                tmp[i] = (byte) data[i];

            //bc.encode(iba, ee);
            //Arrays.fill(vv, 0);
            reverse(data, w, h, vv, 1);
            //ee.encodeByte((byte) 0x80);
            //ee.dispose();
            bs.close();

            cvt.convertYUVtoRGB(yy, uu, vv, rgb2, ColorModelType.YUV420);

            System.out.println("Encoding size: "+(bs.written() >> 3));
            System.out.println("Not null coeffs: "+nonZero+"/"+((w*h)+(w*h/2)));
            System.out.println("PNSR: "+new ImageQualityMonitor(w, h).computePSNR(rgb, rgb2)/1024.0);
            System.out.println("SSIM: "+new ImageQualityMonitor(w, h).computeSSIM(rgb, rgb2)/1024.0);

            bs.flush();
            bs.close();
            
            BufferedImage img2 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
            img2.getRaster().setDataElements(0, 0, w, h, rgb2);
            icon = new ImageIcon(img);

            JFrame frame = new JFrame("Image");
            frame.setBounds(50, 30, w, h);
            frame.add(new JLabel(icon));
            frame.setVisible(true);
            JFrame frame2 = new JFrame("After");
            frame2.setBounds(600, 30, w, h);
            ImageIcon newIcon = new ImageIcon(img2);
            frame2.add(new JLabel(newIcon));
            frame2.setVisible(true);

            Thread.sleep(35000);      
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        
        System.exit(0);
    }


    private static int forward(int[] data, int w, int h, int[] output, OutputBitStream bs, int shift)
    {
        WHT8 wht = new WHT8();
        int[] block = new int[64];
        int nonZero = 0;
        w >>= shift;
        h >>= shift;
        ResidueBlockEncoder re = new ResidueBlockEncoder(3, bs);

        for (int y=0; y<h; y+=8)
        {
           final int x0 = (shift == 0) ? (((y >> 3) & 1) << 2) : 0; // alternate 0 & 4
           
           for (int x=x0; x<w-x0; x+=8)
           {
              for (int j=0; j<8; j++)
                 System.arraycopy(data, (y+j)*w+x, block, 8*j, 8);

              wht.forward(block);
              System.out.println();

              for (int j=0; j<8; j++)
              {
                  for (int i=0; i<8; i++)
                  {
                     int idx = (j<<3)+i;
                     block[idx] = (block[idx] * Global.QUANTIZATION_INTRA[idx] + 128) >> 8;
                     int val = block[idx];

                     if (val != 0)
                         nonZero++;
                     
                     int abs = Math.abs(val);
                     String s = (val>=0) ? " " : "";
                     s += (abs < 100) ? " " : "";
                     s += (abs < 10) ? " " : "";
                     s += val;
                     System.out.print(s+" ");
                  }

                  System.out.println();
              }
        
        long ww = bs.written();
        re.encode(block, 0);
               System.out.println("+"+(bs.written() - ww));

              for (int j=0; j<8; j++)
                 System.arraycopy(block, 8*j, output, (y+j)*w+x, 8);
          }
        }

        return nonZero;
     }

    
     private static void reverse(int[] data, int w, int h, int[] output, int shift)
     {
        WHT8 wht = new WHT8();
        int[] block = new int[64];
        w >>= shift;
        h >>= shift;

        for (int y=0; y<h; y+=8)
        {
           final int x0 = (shift == 0) ? (((y >> 3) & 1) << 2) : 0; // alternate 0 & 4

           for (int x=x0; x<w-x0; x+=8)
           {
              for (int j=0; j<8; j++)
                 System.arraycopy(data, (y+j)*w+x, block, 8*j, 8);

              for (int j=0; j<8; j++)
              {
                  for (int i=0; i<8; i++)
                  {
                     int idx = (j<<3)+i;
                     block[idx] = (block[idx] * Global.DEQUANTIZATION_INTRA[idx]);
                  }
              }

              wht.inverse(block);

              for (int j=0; j<8; j++)
                 System.arraycopy(block, 8*j, output, (y+j)*w+x, 8);
          }
        }

        if (shift == 0)
        {
           PostProcessingFilter filter = new PostProcessingFilter(w, h, w, false);

           for (int y=0; y<h-8; y+=8)
           {
              if ((((y >> 3) & 1) << 2) == 0)
                 continue;

//              System.out.println("Before");
//              for (int j=-1; j<=8; j++)
//              {
//                 for (int i=0; i<5; i++)
//                    System.out.print(output[(y+j)*w+i]);
//                 System.out.println("");
//              }
              filter.apply(output, 0, 4, y, 8);
//              System.out.println("After");
//              for (int j=-1; j<=8; j++)
//              {
//                 for (int i=0; i<5; i++)
//                    System.out.print(output[(y+j)*w+i]);
//                 System.out.println("");
//              }
//              System.out.println("");
           }
        }
     }
}
