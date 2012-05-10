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

import kanzi.util.sort.BitonicSort;


public class TestBitonicSort
{
    public static void main(String[] args)
    {
        System.out.println("TestBitonicSort");

        // Test behavior
        {
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
            new BitonicSort().sort(array, 0);

            for (int i=0; i<b.length; i++)
                b[i] = (byte) (array[i] & 255);

            System.out.println(new String(b));

            for (int i=0; i<array.length; i++)
                System.out.print(array[i]+" ");
        }

        // Test speed
        {
            System.out.println("\nSpeed test");
            int[] array = new int[10000];
            int[] rnd = new int[10000];
            java.util.Random random = new java.util.Random();
            long before, after;

            BitonicSort iSort = new BitonicSort();

            for (int k=0; k<5; k++)
            {
                long sum = 0;

                for (int i=0; i<rnd.length; i++)
                    rnd[i] = Math.abs(random.nextInt());

                for (int ii=0; ii<10000; ii++)
                {
                    for (int i=0; i<array.length; i++)
                        array[i] = rnd[i] & 255;

                    before = System.nanoTime();
                    iSort.sort(array, 0);
                    after = System.nanoTime();
                    sum += (after - before);
                }

                System.out.println("Elapsed [ms]: "+sum/1000000);
            }
        }
    }



}
