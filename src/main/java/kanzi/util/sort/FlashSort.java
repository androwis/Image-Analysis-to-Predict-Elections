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

package kanzi.util.sort;

import kanzi.IntSorter;

//
// Karl-Dietrich Neubert's Flashsort1 Algorithm
// See [http://www.neubert.net/Flapaper/9802n.htm]
//

public class FlashSort implements IntSorter
{
   private static final int SHIFT = 15;

   private final int size;
   private int[] buffer;


    public FlashSort()
    {
       this(0);
    }


    public FlashSort(int size)
    {
       if (size < 0)
          throw new IllegalArgumentException("Invalid size parameter (must be a least 0)");

       // Keep the size of the buffer small enough (buffer.length << shiftL < Integer.MAX_VALUE)
       if (size > 65536)
          throw new IllegalArgumentException("Invalid size parameter (must be at most 65536)");

       this.size = size;
       int m = size / 5; // speed optimum m in [0.2n .. m=0.5n]
       this.buffer = new int[(m < 32) ? 32 : (m + 7) & 0xFFFFFFF8];
    }


    public void sort(int[] input, int blkptr)
    {
       int sz = (this.size == 0) ? input.length : this.size;
       int m = sz / 5; // speed optimum m in [0.2n .. m=0.5n]

       if (this.buffer.length < m)
          this.buffer = new int[(m < 32) ? 32 : (m + 7) & 0xFFFFFFF8];

       this.partialFlashSort(input, blkptr, sz);
       new InsertionSort(sz).sort(input, blkptr);
    }


    private void partialFlashSort(int[] input, int blkptr, int sz)
    {
        int min = input[blkptr];
        int max = min;
        int idxMax = blkptr;
        int end = blkptr + sz;

        for (int i=blkptr+1; i<end; i++)
        {
           int val = input[i];

           if (val < min)
              min = val;

           if (val > max)
           {
              max = val;
              idxMax = i;
           }
        }

        if (min == max)
           return;

        // Aliasing for speed
        final int minimum = min;
        final int[] buf = this.buffer;
        final int len = buf.length;
        final int delta = max - minimum;
        final int delta1 = delta + 1;

        // Reset buckets buffer
        for (int i=0; i<len; i+=8)
        {
           buf[i]   = 0;
           buf[i+1] = 0;
           buf[i+2] = 0;
           buf[i+3] = 0;
           buf[i+4] = 0;
           buf[i+5] = 0;
           buf[i+6] = 0;
           buf[i+7] = 0;
        }

        int shiftL = SHIFT;
        final int threshold = Integer.MAX_VALUE >> 1;
        int c1 = 0;
        int num = 0;

        // Find combinations, shiftL, shiftR and c1
        while ((c1 == 0) && (num < threshold))
        {
           shiftL++;
           num = len << shiftL;
           c1 = num / delta1;
        }

        int shiftR = shiftL;

        while (c1 == 0)
        {
           int denum = delta >> (shiftR - shiftL);
           c1 = num / denum;
           shiftR++;
        }

        // Create the buckets
        for (int i=blkptr; i<end; i++)
        {
           int k = (c1 * (input[i] - minimum)) >> shiftR;
           buf[k]++;
        }

        // Create distribution
        for (int i=1; i<len; i++)
           buf[i] += buf[i-1];

        input[idxMax] = input[blkptr];
        input[blkptr] = max;
        int j = 0;
        int k = len - 1;
        int nmove = 1;

        while (nmove < sz)
        {
            while (j >= buf[k])
            {
                j++;
                k = (c1 * (input[blkptr+j] - min) >> shiftR);
            }

            int flash = input[blkptr+j];

            // Speed critical section
            while (buf[k] != j)
            {
                k = (c1 * (flash - minimum)) >> shiftR;
                final int idx = blkptr + buf[k] - 1;
                final int hold = input[idx];
                input[idx] = flash;
                flash = hold;
                buf[k]--;
                nmove++;
            }
        }
    }

}