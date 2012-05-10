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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import kanzi.InputBitStream;
import kanzi.OutputBitStream;
import kanzi.bitstream.DebugOutputBitStream;
import kanzi.bitstream.DefaultInputBitStream;
import kanzi.bitstream.DefaultOutputBitStream;


public class TestDefaultBitStream
{
    public static void main(String[] args)
    {
      // Test correctness (byte aligned)
      {
        int[] values = new int[] { 1, 7, -9, 123, 0, 12, -63, -64, 275, -555, 100000, 123 };
        byte[] input = new byte[values.length * 4];
//        Random rnd = new Random();
        
//        for (int i=0; i<4*values.length; i+=4)
//        {
//            values[i/4] = rnd.nextInt();
//            input[i]   = (byte) (values[i/4] & 0xFF000000);
//            input[i+1] = (byte) (values[i/4] & 0x00FF0000);
//            input[i+2] = (byte) (values[i/4] & 0x0000FF00);
//            input[i+3] = (byte) (values[i/4] & 0x000000FF);
//        }

         System.out.println("Initial");

         for (int i=0; i<values.length; i++)
             System.out.print(" "+values[i]);

        System.out.println();
        
        try
        {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(4*values.length);
            OutputStream os = new BufferedOutputStream(baos);
            OutputBitStream obs = new DefaultOutputBitStream(os, 16384);
            DebugOutputBitStream dbs = new DebugOutputBitStream(obs, System.out);
            dbs.showByte(true);
            
            for (int i=0; i<values.length; i++)
            {
                int x = values[i];                     
                dbs.writeBits(x, 32);
            }
                        
            // Close first to force flush()
            obs.close();
            byte[] output = baos.toByteArray();
            System.out.println("\nWritten: "+obs.written());
            
            ByteArrayInputStream bais = new ByteArrayInputStream(output);
            InputStream is = new BufferedInputStream(bais);
            InputBitStream ibs = new DefaultInputBitStream(is, 16384);
            System.out.println("Read: ");
            boolean ok = true;
            
            for (int i=0; i<values.length; i++)
            {                
                int x = (int) ibs.readBits(32);
                System.out.print(" "+x);
                ok &= (x == values[i]);
            }
            
            System.out.println("\n"+((ok)?"Success\n":"Failure\n"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
      }

      // Test correctness (not byte aligned)
      {
        int[] values = new int[16384];
        Random rnd = new Random();

        for (int i=0; i<values.length; i++)
            values[i] = rnd.nextInt() & (1 + (i & 63));


         System.out.println("Initial");

         for (int i=0; i<values.length; i++)
         {
            System.out.print(((+values[i] < 10) ? " 0" : " ") + values[i]);

            if ((i & 63) == 63)
               System.out.println();
         }

         try
         {
            ByteArrayOutputStream baos = new ByteArrayOutputStream(values.length);
            OutputStream os = new BufferedOutputStream(baos);
            OutputBitStream obs = new DefaultOutputBitStream(os, 16384);

            for (int i=0; i<values.length; i++)
            {
                int x = values[i];
                obs.writeBits(x, (1 + (i & 63)));
            }

            // Close first to force flush()
            obs.close();
            byte[] output = baos.toByteArray();
            System.out.println("\nWritten: "+obs.written());

            ByteArrayInputStream bais = new ByteArrayInputStream(output);
            InputStream is = new BufferedInputStream(bais);
            InputBitStream ibs = new DefaultInputBitStream(is, 16384);
            System.out.println("\nRead: ");
            boolean ok = true;

            for (int i=0; i<values.length; i++)
            {
                int x = (int) ibs.readBits((1 + (i & 63)));
                System.out.print(((x<10)?" 0":" ")+x+" ("+values[i]+") ");
                ok &= (x == values[i]);

                if ((i & 63) == 63)
                    System.out.println();
            }

            System.out.println("\n"+((ok)?"Success\n":"Failure\n"));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
      }

      // Test speed
      {
        String fileName = (args.length > 0) ? args[0] : "c:\\temp\\output.bin";
        int[] values = new int[] { 3, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5, 8, 9, 7, 9, 3,
                                   31, 14, 41, 15, 59, 92, 26, 65, 53, 35, 58, 89, 97, 79, 93, 32 };

        try
        {
            FileOutputStream faos = new FileOutputStream(new File(fileName));
            OutputStream os = new BufferedOutputStream(faos);
            OutputBitStream obs = new DefaultOutputBitStream(os, 16384);
            int nn = 100000 * values.length;
            long before, after;
            System.out.println("\nWriting ...");
            before = System.nanoTime();

            for (int i=0; i<nn; i++)
            {
                obs.writeBits(values[i%values.length], 1+(i&63));
            }

            // Display bits written BEFORE closing (otherwise padding may be reported)
            System.out.println(obs.written()+ " bits written");

            // Close first to force flush()
            obs.close();

            after = System.nanoTime();
            System.out.println("Elapsed time [ms]: "+((after-before)/1000000L));

            FileInputStream fais = new FileInputStream(new File(fileName));
            InputStream is = new BufferedInputStream(fais);
            InputBitStream ibs = new DefaultInputBitStream(is, 16384);
            System.out.println("\nReading ...");
            before = System.nanoTime();

            for (int i=0; i<nn; i++)
            {
               ibs.readBits(1+(i&63));
           }

            System.out.println(ibs.read()+ " bits read");
            after = System.nanoTime();
            System.out.println("Elapsed time [ms]: "+((after-before)/1000000L));
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
      }
    }
}
