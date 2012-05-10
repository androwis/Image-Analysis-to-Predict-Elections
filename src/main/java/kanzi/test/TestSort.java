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

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import kanzi.util.sort.BitonicSort;
import kanzi.util.sort.BucketSort;
import kanzi.util.sort.HeapSort;
import kanzi.util.sort.InsertionSort;
import kanzi.util.sort.QuickSort;
import kanzi.util.sort.RadixSort;
import kanzi.util.sort.FlashSort;
import kanzi.util.sort.MergeSort;


public class TestSort
{
   public static void main(String[] args)
   {
       int len = 20000;
       int[] array = new int[len];
       int[] rnd = new int[len];
       int[] copy = new int[len];
       java.util.Random random = new java.util.Random();
       long before, after;
       BucketSort bucketSort = new BucketSort(array.length, 8);
       HeapSort heapSort = new HeapSort();
       InsertionSort insertionSort = new InsertionSort(array.length);
       RadixSort radix4Sort = new RadixSort(4, array.length, 8); //radix 4
       RadixSort radix8Sort = new RadixSort(8, array.length, 8); //radix 8
       QuickSort quickSort = new QuickSort(array.length);
       FlashSort flashSort = new FlashSort(array.length);
       MergeSort mergeSort = new MergeSort(array.length);
       BitonicSort bitonicSort = new BitonicSort(array.length);
       ForkJoinPool pool = new ForkJoinPool();

       long sum0  = 0;
       long sum1  = 0;
//       long sum2  = 0;
       long sum3  = 0;
       long sum4  = 0;
       long sum5  = 0;
       long sum6  = 0;
       long sum7  = 0;
       long sum8  = 0;
       long sum9  = 0;
       long sum10 = 0;
       int max = 20;
       int iter = 1000;

        for (int k=0; k<max; k++)
        {
            System.out.println("Iteration "+k+" of "+max);
            
            for (int i=0; i<rnd.length; i++)
                rnd[i] = Math.abs(random.nextInt()) & 0xFF;

            System.arraycopy(rnd, 0, copy, 0, rnd.length);

            {
                // Validation test
                System.arraycopy(copy, 0, array, 0, array.length);
                bucketSort.sort(array, 0);
                check("Bucket Sort", array);
                System.arraycopy(copy, 0, array, 0, array.length);
                heapSort.sort(array, 0);
                check("Heap Sort", array);
                System.arraycopy(copy, 0, array, 0, array.length);
                insertionSort.sort(array, 0);
                check("Insertion Sort", array);
                System.arraycopy(copy, 0, array, 0, array.length);
                radix4Sort.sort(array, 0);
                check("Radix 4 Sort", array);
                System.arraycopy(copy, 0, array, 0, array.length);
                radix4Sort.sort(array, 0);
                check("Radix 8 Sort", array);
                System.arraycopy(copy, 0, array, 0, array.length);
                quickSort.sort(array, 0);
                check("Quick Sort", array);
                System.arraycopy(copy, 0, array, 0, array.length);
                flashSort.sort(array, 0);
                check("Flash Sort", array);
                System.arraycopy(copy, 0, array, 0, array.length);
                bitonicSort.sort(array, 0);
                check("Bitonic Sort", array);
                System.arraycopy(copy, 0, array, 0, array.length);
                SortTask sortTask = new SortTask(array, array.length, 0);
                pool.invoke(sortTask);
                check("Parallel Sort", array);
                System.arraycopy(copy, 0, array, 0, array.length);
                mergeSort.sort(array, 0);
                check("Merge Sort", array);

            for (int ii=0; ii<iter; ii++)
            {
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                bucketSort.sort(array, 0);
                after = System.nanoTime();
                sum0 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                heapSort.sort(array, 0);
                after = System.nanoTime();
                sum1 += (after - before);
                before = System.nanoTime();
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                radix4Sort.sort(array, 0);
                after = System.nanoTime();
                sum3 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                radix8Sort.sort(array, 0);
                after = System.nanoTime();
                sum4 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                quickSort.sort(array, 0);
                after = System.nanoTime();
                sum5 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                Arrays.sort(array);
                after = System.nanoTime();
                sum6 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                flashSort.sort(array, 0);
                after = System.nanoTime();
                sum7 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                sortTask = new SortTask(array, array.length, 0);
                pool.invoke(sortTask);
                after = System.nanoTime();
                sum8 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                mergeSort.sort(array, 0);
                after = System.nanoTime();
                sum9 += (after - before);
                before = System.nanoTime();
                bitonicSort.sort(array, 0);
                after = System.nanoTime();
                sum10 += (after - before);
            }
          }
       }

       System.out.println("\n\n -------------------------------------- \n");
       System.out.println("Speed test - byte values");
       System.out.println((iter*max)+" iterations\n");
       System.out.println("BucketSort      Elapsed [ms]: " + (sum0  / 1000000));
       System.out.println("HeapSort        Elapsed [ms]: " + (sum1  / 1000000));
       System.out.println("InsertionSort   Elapsed [ms]: " + "too slow, skipped");//(sum2  / 1000000));
       System.out.println("Radix4Sort      Elapsed [ms]: " + (sum3  / 1000000));
       System.out.println("Radix8Sort      Elapsed [ms]: " + (sum4  / 1000000));
       System.out.println("QuickSort       Elapsed [ms]: " + (sum5  / 1000000));
       System.out.println("Arrays.sort     Elapsed [ms]: " + (sum6  / 1000000));
       System.out.println("FlashSort       Elapsed [ms]: " + (sum7  / 1000000));
       System.out.println("BitonicSort     Elapsed [ms]: " + (sum10 / 1000000));
       System.out.println("MergeSort       Elapsed [ms]: " + (sum9  / 1000000));
       System.out.println("ParallelSort    Elapsed [ms]: " + (sum8  / 1000000));
       System.out.println("");

       sum0 = 0;
       sum1 = 0;
//       sum2 = 0;
       sum3 = 0;
       sum4 = 0;
       sum5 = 0;
       sum6 = 0;
       sum7 = 0;
       sum8 = 0;
       sum9 = 0;
       sum10 = 0;

        RadixSort radixSort = new RadixSort(8, array.length); // radix 8

        for (int k=0; k<max; k++)
        {
             System.out.println("Iteration "+k+" of "+max);

             for (int i=0; i<rnd.length; i++)
                 rnd[i] = random.nextInt(Integer.MAX_VALUE) & -2;

             System.arraycopy(rnd, 0, copy, 0, rnd.length);

             // Validation test
             System.arraycopy(copy, 0, array, 0, array.length);
             radixSort.sort(array, 0);
             check("Radix 8 Sort", array);
             System.arraycopy(copy, 0, array, 0, array.length);
             quickSort.sort(array, 0);
             check("Quick Sort", array);
             System.arraycopy(copy, 0, array, 0, array.length);
             flashSort.sort(array, 0);
             check("Flash Sort", array);
             System.arraycopy(copy, 0, array, 0, array.length);
             mergeSort.sort(array, 0);
             check("Merge Sort", array);
             System.arraycopy(copy, 0, array, 0, array.length);
             bitonicSort.sort(array, 0);
             check("Bitonic Sort", array);
             System.arraycopy(copy, 0, array, 0, array.length);
             SortTask sortTask = new SortTask(array, array.length, 0);
             pool.invoke(sortTask);
             check("Parallel Sort", array);


            for (int ii=0; ii<iter; ii++)
            {
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                radixSort.sort(array, 0);
                after = System.nanoTime();
                sum3 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                quickSort.sort(array, 0);
                after = System.nanoTime();
                sum5 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                Arrays.sort(array);
                after = System.nanoTime();
                sum6 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                flashSort.sort(array, 0);
                after = System.nanoTime();
                sum7 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                sortTask = new SortTask(array, array.length, 0);
                pool.invoke(sortTask);
                after = System.nanoTime();
                sum8 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                mergeSort.sort(array, 0);
                after = System.nanoTime();
                sum9 += (after - before);
                System.arraycopy(copy, 0, array, 0, array.length);
                before = System.nanoTime();
                bitonicSort.sort(array, 0);
                after = System.nanoTime();
                sum10 += (after - before);
            }
       }

       System.out.println("\n\n -------------------------------------- \n");
       System.out.println("Speed test - integer values\n");
       System.out.println((iter*max)+" iterations\n");
       System.out.println("Radix8Sort      Elapsed [ms]: " + (sum3  / 1000000));
       System.out.println("QuickSort       Elapsed [ms]: " + (sum5  / 1000000));
       System.out.println("Arrays.sort     Elapsed [ms]: " + (sum6  / 1000000));
       System.out.println("FlashSort       Elapsed [ms]: " + (sum7  / 1000000));
       System.out.println("Bitonic         Elapsed [ms]: " + (sum10 / 1000000));
       System.out.println("MergeSort       Elapsed [ms]: " + (sum9  / 1000000));
       System.out.println("ParallelSort    Elapsed [ms]: " + (sum8  / 1000000));
   }


   private static boolean check(String name, int[] array)
   {
     boolean res = true;

     for (int i=1; i<array.length; i++)
     {
         if (array[i] < array[i-1])
         {
             System.out.println("Error '"+name+"' at index "+i);

             for (int j=0; j<=i; j++)
                 System.out.print(array[j]+" ");

             System.out.println("");
             res = false;
             break;
         }
     }

     return res;
   }
}
