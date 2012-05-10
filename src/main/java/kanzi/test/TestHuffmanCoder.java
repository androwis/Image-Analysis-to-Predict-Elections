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

import kanzi.BitStreamException;
import kanzi.bitstream.DebugInputBitStream;
import kanzi.entropy.HuffmanDecoder;
import kanzi.entropy.HuffmanEncoder;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import kanzi.InputBitStream;
import kanzi.OutputBitStream;
import kanzi.bitstream.DebugOutputBitStream;
import kanzi.bitstream.DefaultInputBitStream;
import kanzi.bitstream.DefaultOutputBitStream;


public class TestHuffmanCoder
{
    public static void main(String[] args)
    {
        System.out.println("TestHuffmanCoder");

        // Test behavior
        for (int ii=1; ii<20; ii++)
        {
            System.out.println("\n\nTest "+ii);
            try
            {
                int[] values;
                java.util.Random random = new java.util.Random();

                if (ii == 3)
                     values = new int[] { 0, 0, 32, 15, -4, 16, 0, 16, 0, 7, -1, -4, -32, 0, 31, -1 };
                else if (ii == 2)
                     values = new int[] { 0x3d, 0x4d, 0x54, 0x47, 0x5a, 0x36, 0x39, 0x26, 0x72, 0x6f, 0x6c, 0x65, 0x3d, 0x70, 0x72, 0x65 };
                else if (ii == 1)
                     values = new int[] { 65, 71, 74, 66, 76, 65, 69, 77, 74, 79, 68, 75, 73, 72, 77, 68, 78, 65, 79, 79, 78, 66, 77, 71, 64, 70, 74, 77, 64, 67, 71, 64 };
                else
                {
                     values = new int[32];

                     for (int i=0; i<values.length; i++)
                          values[i] = 64 + (random.nextInt() & 31);
                }

                System.out.println("Original:");

                for (int i=0; i<values.length; i++)
                    System.out.print(values[i]+" ");

                System.out.println("\nEncoded:");
                ByteArrayOutputStream os = new ByteArrayOutputStream(16384);
                OutputBitStream bs = new DefaultOutputBitStream(os, 16384);
                DebugOutputBitStream dbgbs = new DebugOutputBitStream(bs, System.out);
                dbgbs.showByte(true);
                dbgbs.setMark(true);
                int[] freq = new int[256];

                for (int i=0; i<values.length; i++)
                    freq[values[i] & 255]++;

                HuffmanEncoder rc = new HuffmanEncoder(dbgbs);
                rc.updateFrequencies(freq);

                for (int i=0; i<values.length; i++)
                {
                    if (rc.encodeByte((byte) (values[i] & 255)) == false)
                        break;
                }

                rc.dispose();
                dbgbs.close();
                System.out.println();
                byte[] buf = os.toByteArray();
                InputBitStream bs2 = new DefaultInputBitStream(new ByteArrayInputStream(buf), 16384);
                DebugInputBitStream dbgbs2 = new DebugInputBitStream(bs2, System.out);
                dbgbs2.setMark(true);
                HuffmanDecoder rd = new HuffmanDecoder(dbgbs2);
                rd.readLengths();
                System.out.println("\nDecoded:");
                int len = values.length; // buf.length >> 3;
                boolean ok = true;
                int[] values2 = new int[values.length];

                for (int i=0, j=0; i<len; i++)
                {
                    try
                    {
                        int n = rd.decodeByte();
                        values2[j] = n;

                        if (values[j++] != n)
                           ok = false;
                    }
                    catch (BitStreamException e)
                    {
                        e.printStackTrace();
                        break;
                    }
                }

                System.out.println();

                for (int i=0; i<values2.length; i++)
                    System.out.print(values2[i]+" ");

                System.out.println("\n"+((ok == true) ? "Identical" : "! *** Different *** !"));
                rd.dispose();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }


        // Test speed
        System.out.println("\n\nSpeed Test");
        
        for (int jj=0; jj<3; jj++)
        {
            byte[] values1 = new byte[50000];
            byte[] values2 = new byte[50000];
            int[] freq = new int[256];
            long delta = 0;

            for (int ii=0; ii<2000; ii++)
            {
                for (int i=0; i<256; i++)
                   freq[i] = 0;
                
                for (int i=0; i<values1.length; i++)
                {
                    values1[i] = (byte) (i & 255);
                    freq[values1[i] & 0xFF]++;
                }
                 
                // Encode
                ByteArrayOutputStream os = new ByteArrayOutputStream(16384);
                OutputBitStream bs = new DefaultOutputBitStream(os, 16384);
                HuffmanEncoder rc = new HuffmanEncoder(bs);
                long before = System.nanoTime();
                rc.encode(values1, 0, values1.length);
                rc.dispose();
                bs.close();
                
                // Decode
                byte[] buf = os.toByteArray();
                InputBitStream bs2 = new DefaultInputBitStream(new ByteArrayInputStream(buf), 1000);
                HuffmanDecoder rd = new HuffmanDecoder(bs2);
                rd.decode(values2, 0, values2.length);
                long after = System.nanoTime();
                delta += (after - before);  
                rd.dispose();

                // Sanity check
                for (int i=0; i<values1.length; i++)
                {
                   if (values1[i] != values2[i])
                   {
                      System.out.println("Error at index "+i+" ("+values1[i]
                              +"<->"+values2[i]+")");
                      break;
                   }
                }
            }
 
            System.out.println("Elapsed [ms]: "+delta/1000000);
        }
    }
}
