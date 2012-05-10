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

import kanzi.util.sort.RadixSort;


public class TestRadixSort 
{
    public static void main(String[] args)
    {
        System.out.println("TestRadixSort");

        // Test behavior
        for (int ii=0; ii<20; ii++)
        {
            System.out.println("\n\nTest "+ii);
            final int[] array = new int[64];
            java.util.Random random = new java.util.Random();
            
            for (int i=0; i<array.length; i++)
                array[i] = 64+(random.nextInt() & 31);
            
            byte[] b = new byte[array.length];
            
            for (int i=0; i<b.length; i++)
                b[i] = (byte) (array[i] & 255);
            
            System.out.println(new String(b));
            
            for (int i=0; i<array.length; i++)
                System.out.print(array[i]+" ");
            
            System.out.println();
            
            // Alternate radix 1, 2, 4 & 8
            int radix = 1 << (ii & 3);
            System.out.println("Radix "+radix);
            new RadixSort(radix).sort(array, 0);
            
            for (int i=0; i<b.length; i++)
                b[i] = (byte) (array[i] & 255);
            
            System.out.println(new String(b));
            
            for (int i=0; i<array.length; i++)
                System.out.print(array[i]+" ");
        }

        for (int ii=0; ii<5; ii++)
        {
            int[] array = new int[64];
            java.util.Random random = new java.util.Random();
            System.out.println("\n");

            if (ii == 0)
            {
               array = new int[] { 0x1996c2e, 0x9212e61, 0x9fdde5a, 0x256115c0, 0x3cdc324d, 0x28c20cca, 0x20421d71, 0x10cef1b6,
                                   0x2dd1eeca, 0x11df1223, 0x14a53f86, 0x101a5c90, 0x13b440c5, 0x349b7258, 0x359d2629, 0x358fb185,
                                   0xa8fadfa, 0x20e8a1ba, 0x22885723, 0x2a05afae, 0x31c575e, 0x227fe38d, 0x1afe5c80, 0x3eaa73dd,
                                   0x5d5b6d6, 0x3971464, 0x7557c29, 0x1ff21abf, 0x185ee6f4, 0x14f69d50, 0x15f1b009, 0x16f74053,
                                   0x3bf805c9, 0x13684cdb, 0x31a657cb, 0x3b4be1ad, 0x5e153c6, 0x2d15a2d6, 0x29f2ad20, 0x267c760f,
                                   0x2c11bfad, 0x2d27ebf6, 0x39b89397, 0x18af6449, 0x2fb0a092, 0x5a55a41, 0x28cded09, 0x34284970,
                                   0xa5d6eee, 0x20ec174a, 0x6022b90, 0x3258d5e5, 0x38c5dba, 0x341f1d3f, 0x28746a11, 0x3be055a8,
                                   0x212bc4a8, 0x362873c7, 0x1ff1e455, 0x5b748fc, 0xc745c5e, 0xc713dac, 0x3d1937b0, 0x1e15c25f };

            }
            else
            {
               for (int i=0; i<array.length; i++)
               {
                  array[i] = random.nextInt(1<<30);
               }
            }

            for (int i=0; i<array.length; i++)
            {
               System.out.print(Integer.toHexString(array[i])+" ");
            }

            System.out.println("");
            new RadixSort().sort(array, 0);

            for (int i=0; i<array.length; i++)
                System.out.print(Integer.toHexString(array[i])+" ");

            System.out.println();

            for (int i=1; i<array.length; i++)
            {
               if (array[i] >= array[i-1])
                 System.out.print(".");
               else 
               {
                 System.err.print(i+":"+Integer.toHexString(array[i-1])+" "+Integer.toHexString(array[i])+" !!!");
                 break;
               }
            }

            System.out.println("");
        }
        

        // Test speed -- radix 4
        {
            System.out.println("\n\nSpeed test (radix 4)");
            int[] array = new int[20000];
            int[] rnd = new int[20000];
            java.util.Random random = new java.util.Random();
            long before, after;
            
            RadixSort rSort = new RadixSort(4);
            
            for (int k=0; k<3; k++)
            {
                long sum = 0;
                
                for (int i=0; i<rnd.length; i++)
                    rnd[i] = Math.abs(random.nextInt());
                
                for (int ii=0; ii<8000; ii++)
                {
                    for (int i=0; i<array.length; i++)
                        array[i] = rnd[i];// & 255;
                    
                    before = System.nanoTime();
                    rSort.sort(array, 0);
                    after = System.nanoTime();
                    sum += (after - before);
                }
                
                System.out.println("Elapsed [ms]: "+sum/1000000);
            }
        }
            
        // Test speed -- radix 8
        {
            System.out.println("\n\nSpeed test (radix 8)");
            int[] array = new int[20000];
            int[] rnd = new int[20000];
            java.util.Random random = new java.util.Random();
            long before, after;
            
            RadixSort rSort = new RadixSort(8);
            
            for (int k=0; k<3; k++)
            {
                long sum = 0;
                
                for (int i=0; i<rnd.length; i++)
                    rnd[i] = Math.abs(random.nextInt());
                
                for (int ii=0; ii<8000; ii++)
                {
                    for (int i=0; i<array.length; i++)
                        array[i] = rnd[i];// & 255;
                    
                    before = System.nanoTime();
                    rSort.sort(array, 0);
                    after = System.nanoTime();
                    sum += (after - before);
                }
                
                System.out.println("Elapsed [ms]: "+sum/1000000);
            }
        }
    }
    
}

