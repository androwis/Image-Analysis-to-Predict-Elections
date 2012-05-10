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

import kanzi.EntropyEncoder;
import kanzi.entropy.ExpGolombEncoder;
import kanzi.entropy.HuffmanEncoder;
import kanzi.entropy.RangeEncoder;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import kanzi.OutputBitStream;
import kanzi.bitstream.DefaultOutputBitStream;


public class TestEntropyCoders
{
    public static void main(String[] args)
    {
        try
        {
            EntropyEncoder encoder;
            String fileName = (args.length > 0) ? args[0] : "c:\\temp\\lena.jpg";
            FileInputStream fis = new FileInputStream(new File(fileName));
            OutputBitStream obs;
            FileOutputStream fos;
            String outputFileName = fileName;
            long before, after;
            byte[] buffer = new byte[32768];
            int maxIter = 1;

            // Original
            int n, sz = 0;

            do
            {
                n = fis.read(buffer);
                sz += n;
            }
            while (n == buffer.length);

            fis.close();
            System.out.println("Original    size = " + 8 * sz + " bits");
            System.out.println();

            for (int ii = 0; ii < maxIter; ii++)
            {


                // ExpGolomb
                fis = new FileInputStream(new File(fileName));
                outputFileName = outputFileName.substring(0, outputFileName.length() - 3).concat("egl");
                fos = new FileOutputStream(new File(outputFileName));
                obs = new DefaultOutputBitStream(fos, 1000000);
                encoder = new ExpGolombEncoder(obs, true);
                before = System.nanoTime();

                do
                {
                    n = fis.read(buffer);

                    for (int i = 0; i < n; i++)
                        encoder.encodeByte(buffer[i]);
                }
                while (n == buffer.length);

                after = System.nanoTime();
                fis.close();
                obs.close();

                if (ii == 0)
                    System.out.println("Exp-Golomb  size = " + obs.written() + " bits   ratio = "+((float) obs.written()/(8*sz)));
                System.out.println("Exp-Golomb  elapsed [ms] = " + (after - before) / 1000000);
                System.out.println();



                // Huffman
                fis = new FileInputStream(new File(fileName));
                outputFileName = outputFileName.substring(0, outputFileName.length() - 3).concat("huf");
                int[] freq = new int[256];

                do
                {
                    n = fis.read(buffer);

                    for (int i = 0; i < n; i++)
                        freq[buffer[i] & 255]++;
                }
                while (n == buffer.length);

                fis.close();
                fis = new FileInputStream(new File(fileName));
                fos = new FileOutputStream(new File(outputFileName));
                obs = new DefaultOutputBitStream(fos, 16384);
                encoder = new HuffmanEncoder(obs);
                ((HuffmanEncoder) encoder).updateFrequencies(freq);
                before = System.nanoTime();

                do
                {
                    n = fis.read(buffer);

                    for (int i = 0; i < n; i++)
                    {
                        encoder.encodeByte(buffer[i]);
                    }
                }
                while (n == buffer.length);

                after = System.nanoTime();
                fis.close();
                obs.close();

                if (ii == 0)
                    System.out.println("Huffman     size = " + obs.written() + " bits   ratio = "+((float) obs.written()/(8*sz)));
                System.out.println("Huffman     elapsed [ms] = " + (after - before) / 1000000);
                System.out.println();


                // Range
                fis = new FileInputStream(new File(fileName));
                outputFileName = outputFileName.substring(0, outputFileName.length() - 3).concat("rng");
                fos = new FileOutputStream(new File(outputFileName));
                obs = new DefaultOutputBitStream(fos, 16384);
                encoder = new RangeEncoder(obs);
                before = System.nanoTime();

                do
                {
                    n = fis.read(buffer);

                    for (int i = 0; i < n; i++)
                        encoder.encodeByte(buffer[i]);
                }
                while (n == buffer.length);

                after = System.nanoTime();
                fis.close();
                obs.close();

                if (ii == 0)
                    System.out.println("Range       size = " + obs.written() + " bits   ratio = "+((float) obs.written()/(8*sz)));
                System.out.println("Range       elapsed [ms] = " + (after - before) / 1000000);
                System.out.println();
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}