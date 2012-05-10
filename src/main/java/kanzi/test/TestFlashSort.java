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

import kanzi.util.sort.FlashSort;


public class TestFlashSort
{
    public static void main(String[] args)
    {
        System.out.println("TestFlashSort");

        // Test behavior
        for (int ii=1; ii<20; ii++)
        {
            System.out.println("\n\nTest "+ii);
            int idx = 10;
            final int[] array = new int[64];
            java.util.Random random = new java.util.Random();

            for (int i=idx; i<array.length; i++)
                array[i] = 64+(random.nextInt() & 31);

            byte[] b = new byte[array.length];

            for (int i=idx; i<b.length; i++)
                b[i] = (byte) (array[i] & 255);

            System.out.println(new String(b));

            for (int i=idx; i<array.length; i++)
                System.out.print(array[i]+" ");

            System.out.println();
            new FlashSort(array.length-idx).sort(array, idx);

            for (int i=idx; i<b.length; i++)
                b[i] = (byte) (array[i] & 255);

            System.out.println(new String(b));

            for (int i=idx; i<array.length; i++)
                System.out.print(array[i]+" ");
        }

        // Test speed
        {
            System.out.println("\n\nSpeed test");
            int[] array = new int[10000];
            int[] array2 = new int[10000];
            int[] rnd = new int[10000];
            java.util.Random random = new java.util.Random();
            long before, after;

             FlashSort fSort = new FlashSort(10000);

            for (int k=0; k<3; k++)
            {
                long sum = 0;
                long sum2 = 0;

                for (int i=0; i<rnd.length; i++)
                    rnd[i] = Math.abs(random.nextInt());

                for (int ii=0; ii<20000; ii++)
                {
                    for (int i=0; i<array.length; i++)
                        array[i] = rnd[i] & 255;

                     for (int i=0; i<array.length; i++)
                        array2[i] = rnd[i] & 255;

                    before = System.nanoTime();
                    fSort.sort(array, 0);
                    after = System.nanoTime();
                    sum += (after - before);
                    before = System.nanoTime();
                    java.util.Arrays.sort(array2);
                    after = System.nanoTime();
                    sum2 += (after - before);
                }

                System.out.println("Elapsed flashSort   [ms]: "+sum/1000000);
                System.out.println("Elapsed arrays.sort [ms]: "+sum2/1000000);
            }
        }
    }

}
