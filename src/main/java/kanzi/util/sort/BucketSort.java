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

import kanzi.ByteSorter;
import kanzi.IntSorter;


// Bucket sort is a very simple very fast sorting algorithm based on bucket partition
// It is a simplified radix sort with buckets of width one
// Due to this design, the max value of the data to sort is limited to 0xFFFF
// For bigger values use a radix sort
public class BucketSort implements IntSorter, ByteSorter
{
    private final int[] count;
    private final int size;
    
    
    public BucketSort()
    {
        this.size = 0;
        this.count = new int[256];
    }


    public BucketSort(int size)
    {
        this(size, 8);
    }

    
    // Limit size to handle shorts
    public BucketSort(int size, int logMaxValue)
    {
        if (size < 0)
            throw new IllegalArgumentException("The size parameter must be at least 0");

        if (logMaxValue < 2)
            throw new IllegalArgumentException("The log data size parameter must be at least 2");
        
        if (logMaxValue > 16)
            throw new IllegalArgumentException("The log data size parameter must be at most 16");

        this.size = size;
        this.count = new int[1 << logMaxValue];
    }
    
    
    // Not thread safe
    // all input data must be smaller than 1 << logMaxValue
    @Override
    public void sort(int[] input, int blkptr)
    {
        final int sz = (this.size == 0) ? input.length : this.size;
        final int len16 = sz & -16;
        final int end16 = blkptr + len16;
        final int[] c = this.count;
        int len = c.length;

        // Unroll loop
        for (int i=blkptr; i<end16; i+=16)
        {
            c[input[i]]++;
            c[input[i+1]]++;
            c[input[i+2]]++;
            c[input[i+3]]++;
            c[input[i+4]]++;
            c[input[i+5]]++;
            c[input[i+6]]++;
            c[input[i+7]]++;
            c[input[i+8]]++;
            c[input[i+9]]++;
            c[input[i+10]]++;
            c[input[i+11]]++;
            c[input[i+12]]++;
            c[input[i+13]]++;
            c[input[i+14]]++;
            c[input[i+15]]++;
        }

        for (int i=len16; i<sz; i++)
            c[input[blkptr+i]]++;

        for (int i=0, j=blkptr; i<len; i++)
        {
            final int val = c[i];

            if (val == 0)
                continue;

            c[i] = 0;
            final int val16 = val & -16;

            for (int k=val; k>val16; k--)
                input[j++] = i;

            if (val16 > 0)
            {
                int j0 = j;
                input[j]    = i;
                input[j+1]  = i;
                input[j+2]  = i;
                input[j+3]  = i;
                input[j+4]  = i;
                input[j+5]  = i;
                input[j+6]  = i;
                input[j+7]  = i;
                input[j+8]  = i;
                input[j+9]  = i;
                input[j+10] = i;
                input[j+11] = i;
                input[j+12] = i;
                input[j+13] = i;
                input[j+14] = i;
                input[j+15] = i;
                j += 16;

                // Native copy for improved speed
                for (int k=val16-16; k>0; k-=16, j+=16)
                    System.arraycopy(input, j0, input, j, 16);
            }
        }
    }


    // Not thread safe
    @Override
    public void sort(byte[] input, int blkptr)
    {
        final int sz = (this.size == 0) ? input.length : this.size;
        final int len16 = sz & -16;
        final int end16 = blkptr + len16;
        final int[] c = this.count;
        final int len = c.length;

        // Unroll loop
        for (int i=blkptr; i<end16; i+=16)
        {
            c[input[i]]++;
            c[input[i+1]]++;
            c[input[i+2]]++;
            c[input[i+3]]++;
            c[input[i+4]]++;
            c[input[i+5]]++;
            c[input[i+6]]++;
            c[input[i+7]]++;
            c[input[i+8]]++;
            c[input[i+9]]++;
            c[input[i+10]]++;
            c[input[i+11]]++;
            c[input[i+12]]++;
            c[input[i+13]]++;
            c[input[i+14]]++;
            c[input[i+15]]++;
        }

        for (int i=len16; i<sz; i++)
            c[input[blkptr+i]]++;

        for (int i=0, j=blkptr; i<len; i++)
        {
            final int val = c[i];

            if (val == 0)
                continue;
            
            final int val16 = val & -16;
            c[i] = 0;

            for (int k=val; k>val16; k--)
                input[j++] = (byte) i;

            if (val16 > 0)
            {
                int j0 = j;
                input[j]    = (byte) i;
                input[j+1]  = (byte) i;
                input[j+2]  = (byte) i;
                input[j+3]  = (byte) i;
                input[j+4]  = (byte) i;
                input[j+5]  = (byte) i;
                input[j+6]  = (byte) i;
                input[j+7]  = (byte) i;
                input[j+8]  = (byte) i;
                input[j+9]  = (byte) i;
                input[j+10] = (byte) i;
                input[j+11] = (byte) i;
                input[j+12] = (byte) i;
                input[j+13] = (byte) i;
                input[j+14] = (byte) i;
                input[j+15] = (byte) i;
                j += 16;

                // Native copy for improved speed
                for (int k=val16-16; k>0; k-=16, j+=16)
                    System.arraycopy(input, j0, input, j, 16);
            }
        }
    }
    
}