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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import kanzi.ColorModelType;
import kanzi.IndexedByteArray;
import kanzi.IndexedIntArray;
import kanzi.EntropyDecoder;
import kanzi.EntropyEncoder;
import kanzi.InputBitStream;
import kanzi.OutputBitStream;
import kanzi.bitstream.DefaultInputBitStream;
import kanzi.bitstream.DefaultOutputBitStream;
import kanzi.entropy.RangeDecoder;
import kanzi.entropy.RangeEncoder;
import kanzi.function.wavelet.WaveletBandFilter;
import kanzi.transform.DWT_CDF_9_7;
import kanzi.function.BlockCodec;
import kanzi.util.color.YSbSrColorModelConverter;
import kanzi.util.ImageQualityMonitor;


public class TestPipeline
{
    public static final int Q0 = 310;
    public static final int Q1 = 40;

    public static void main(String[] args)
    {
        try
        {
            int blockSize = 100000;
            boolean debug = true;
            String fileName = "c:\\temp\\lena.jpg";
            boolean fileProvided = false;

            for (String arg : args)
            {
               arg = arg.trim();
               
               if (arg.equals("-help"))
               {
                   System.out.println("-help             : display this message");
                   System.out.println("-debug            : display debug information");
                   System.out.println("-file=<filename>  : name of the input file to encode or decode");
                   System.out.println("-block=<size>     : size of the block (max 16 MB)");
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
               else if (arg.startsWith("-block="))
               {
                  arg = arg.substring(7);
                  
                  try
                  {
                     int blksz = Integer.parseInt(arg);
                     
                     if (blksz < 256)
                     {
                         System.err.println("The minimum block size is 256, the provided value is "+arg);
                         System.exit(1);
                     }
                     else if (blksz > 16 * 1024 * 1024 - 7)
                     {
                         System.err.println("The maximum block size is 16777209, the provided value is  "+arg);
                         System.exit(1);
                     }
                     else
                         blockSize = blksz;                     
                  }
                  catch (NumberFormatException e)
                  {
                     System.err.println("Invalid block size provided on command line: "+arg);
                  }
               }
               else
               {
                   System.out.println("Warning: unknown option: ["+ arg + "]");
               }
            }
            
            if (fileProvided == false)
                System.out.println("No input file name provided on command line, using default value");
            
            System.out.println("Input file name set to '" + fileName + "'");
            System.out.println("Block size set to "+blockSize);
            ImageIcon icon = new ImageIcon(fileName);
            Image image = icon.getImage();
            int w = image.getWidth(null);
            int h = image.getHeight(null);
            System.out.println(w + "x" + h);


            if ((w < 0) || (h < 0))
            {
                System.err.println("Could not load image '" + fileName +"'");
                System.exit(1);
            }

            GraphicsDevice gs = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
            GraphicsConfiguration gc = gs.getDefaultConfiguration();
            BufferedImage img = gc.createCompatibleImage(w, h, Transparency.OPAQUE);

            img.getGraphics().drawImage(image, 0, 0, null);
            int[] rgb = new int[w*h];
            int[] rgb2 = new int[w*h];
            int[] u = new int[rgb.length];
            int[] y = new int[rgb.length];
            int[] v = new int[rgb.length];

            // Do NOT use img.getRGB(): it is more than 10 times slower than
            // img.getRaster().getDataElements()
            img.getRaster().getDataElements(0, 0, w, h, rgb);

            encode(fileName, rgb, y, u, v, w, h, debug, blockSize);
            decode(fileName, rgb2, y, u, v, w, h, debug, blockSize);

            int psnr1024 = new ImageQualityMonitor(w, h).computePSNR(rgb, rgb2);
            System.out.println("PSNR: "+(float) psnr1024 / 1024);
            int ssim1024 = new ImageQualityMonitor(w, h).computeSSIM(rgb, rgb2);
            System.out.println("SSIM: "+(float) ssim1024 / 1024);

            // Do NOT use img.setRGB(): it is more than 10 times slower than
            // img.getRaster().setDataElements()
            //img.getRaster().setDataElements(0, 0, w, h, rgb);
            BufferedImage img2 = gc.createCompatibleImage(w, h, Transparency.OPAQUE);
            img2.getRaster().setDataElements(0, 0, w, h, rgb2);
            icon = new ImageIcon(img);

            JFrame frame = new JFrame("Before");
            frame.setBounds(50, 30, w, h);
            frame.add(new JLabel(icon));
            frame.setVisible(true);
            JFrame frame2 = new JFrame("After");
            frame2.setBounds(600, 30, w, h);
            ImageIcon newIcon = new ImageIcon(img2);
            frame2.add(new JLabel(newIcon));
            frame2.setVisible(true);

            try
            {
                Thread.sleep(35000);
            }
            catch (Exception e)
            {
            }

           System.exit(0);
        }
        catch (Exception e)
        {
           e.printStackTrace();
        }
    }


    private static void encode(String fileName, int[] rgb, int[] y, int[] u, int[] v, 
            int w, int h, boolean debug, int blockSize) throws Exception
    {
        File output = new File(fileName.substring(0, fileName.length()-3).concat("bin"));
        FileOutputStream fos = new FileOutputStream(output);
        OutputBitStream dbs = new DefaultOutputBitStream(fos, 16384);

        DWT_CDF_9_7 ydwt = new DWT_CDF_9_7(w, h);
        DWT_CDF_9_7 uvdwt = new DWT_CDF_9_7(w/2, h/2);
        EntropyEncoder entropyEncoder = new RangeEncoder(dbs);
        YSbSrColorModelConverter cvt = new YSbSrColorModelConverter(w, h);
        long before = System.nanoTime();
        int iter = 1;

        for (int ii=0; ii<iter; ii++)
        {
            // Color space conversion
            cvt.convertRGBtoYUV(rgb, y, u, v, ColorModelType.YUV420);

            // Discrete Wavelet Transform
            encode_(y, ydwt, entropyEncoder, blockSize);
            encode_(u, uvdwt, entropyEncoder, blockSize);
            encode_(v, uvdwt, entropyEncoder, blockSize);
        }

        entropyEncoder.dispose();
        dbs.close();
        long after = System.nanoTime();
        
        if (debug == true)
        {
          System.out.println("Encoding time [ms]: "+(after-before)/(iter*1000000));
          System.out.println("Read: "+(w*h*3*iter));
          System.out.println("Written: "+(dbs.written() >> 3));
        }
        
        float r = (float) (w*h*3*iter) / (dbs.written() >> 3);
        System.out.println("Compression ratio: "+ r);
    }


     private static void encode_(int[] data, DWT_CDF_9_7 dwt, EntropyEncoder encoder, int blockSize)
     {
        int levels = dwt.getLevels();

        // Perform Discrete Wavelet Transform band by band
        dwt.forward(data, 0);

        IndexedIntArray source = new IndexedIntArray(data, 0);
        int[] buffer = new int[dwt.getWidth()*dwt.getHeight()];
        IndexedIntArray destination = new IndexedIntArray(buffer, 0);

        // Quantization
        int[] quantizers = new int[levels+1];
        quantizers[0] = Q0;
        quantizers[1] = Q1;

        for (int i=2; i<quantizers.length; i++)
        {
            // Derive quantizer values for higher bands
            quantizers[i] = ((quantizers[i-1]) * 17 + 2) >> 4;
        }

//        WaveletRateDistorsionFilter filter = new WaveletRateDistorsionFilter(
//                dimImage, levels, 100, dimImage, true);
        WaveletBandFilter filter = new WaveletBandFilter(dwt.getWidth(),
                dwt.getHeight(), levels, quantizers);

        // The filter guarantees that all coefficients have been shrunk to byte values
        filter.forward(source, destination);

        IndexedByteArray block = new IndexedByteArray(new byte[destination.index], 0);

        for (int i=0; i<destination.index; i++)
           block.array[i] = (byte) destination.array[i];

        // Block encoding
        BlockCodec bc = new BlockCodec();
        int length = destination.index;
        block.index = 0;

        while (length > 0)
        {
            int blkSize = (length < blockSize) ? length : blockSize;
            bc.setSize(blkSize);
            int encoded = bc.encode(block, encoder);

            if (encoded < 0)
            {
              System.out.println("Error during block encoding");
              System.exit(1);
            }

            length -= blkSize;
        }

        // End of block: add empty block: 0x80
        encoder.encodeByte((byte) 0x80);
    }


    private static void decode(String fileName, int[] rgb, int[] y, int[] u, int[] v, 
            int w, int h, boolean debug, int blockSize) throws Exception
    {
        File input = new File(fileName.substring(0, fileName.length()-3).concat("bin"));
        FileInputStream is = new FileInputStream(input);
        InputBitStream dbs = new DefaultInputBitStream(is, 16384);
        DWT_CDF_9_7 ydwt = new DWT_CDF_9_7(w, h);
        DWT_CDF_9_7 uvdwt = new DWT_CDF_9_7(w/2, h/2);
        EntropyDecoder entropyDecoder = new RangeDecoder(dbs);
        YSbSrColorModelConverter cvt = new YSbSrColorModelConverter(w, h);
        long before = System.nanoTime();
        int iter = 1;

        for (int ii=0; ii<iter; ii++)
        {
            // Discrete Wavelet Inverse Transform
            decode_(y, ydwt, entropyDecoder, blockSize);
            decode_(u, uvdwt, entropyDecoder, blockSize);
            decode_(v, uvdwt, entropyDecoder, blockSize);

            // Color space conversion
            cvt.convertYUVtoRGB(y, u, v, rgb, ColorModelType.YUV420);
        }

        long after = System.nanoTime();
        entropyDecoder.dispose();
        dbs.close();
        
        if (debug == true)
        {
          System.out.println("Decoding time [ms]: "+(after-before)/(iter*1000000));
          System.out.println("Read: "+(dbs.read() >> 3));
        }
    }


    private static void decode_(int[] data, DWT_CDF_9_7 dwt, EntropyDecoder decoder, int blockSize)
    {
        // Block decoding
        IndexedByteArray buffer = new IndexedByteArray(new byte[dwt.getWidth()*dwt.getHeight()], 0);
        BlockCodec bd = new BlockCodec(blockSize);
        int decoded = 0;

        do
        {
           decoded = bd.decode(buffer, decoder);

           if (decoded < 0)
           {
              System.out.println("Error during block decoding");
              System.exit(1);
           }
        }
        while (decoded > 0);

        int levels = dwt.getLevels();
        
        // Dequantization

        // Quantizers must be known by the decoder !!!
        // TODO: transmit quantizers
        int[] quantizers = new int[levels+1];
        quantizers[0] = Q0;
        quantizers[1] = Q1;

        for (int i=2; i<quantizers.length; i++)
        {
            // Derive quantizer values for higher bands
            quantizers[i] = ((quantizers[i-1]) * 17 + 2) >> 4;
        }

        // Inverse quantization
        WaveletBandFilter filter = new WaveletBandFilter(dwt.getWidth(),
                dwt.getHeight(), levels, quantizers);

        IndexedIntArray destination = new IndexedIntArray(data, 0);
        IndexedIntArray source = new IndexedIntArray(new int[dwt.getWidth()*dwt.getHeight()], 0);

        for (int i=0; i<buffer.index; i++)
           source.array[i] = (int) buffer.array[i];

        filter.inverse(source, destination);

        // Perform inverse Discrete Wavelet Transform band by band
        dwt.inverse(data, 0);
    }
}
