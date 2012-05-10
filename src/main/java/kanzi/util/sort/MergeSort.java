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

import kanzi.IndexedIntArray;
import kanzi.IntSorter;


// A MergeSort is conceptually very simple(divide and merge) but usually not
// very performant ... except for almost sorted data
// This implementation based on OpenJDK avoids the usual trap of many array creations
public class MergeSort implements IntSorter
{
    private final int size;
    private int[] buffer;


    public MergeSort()
    {
       this(0);
    }


    public MergeSort(int size)
    {
       if (size < 0)
          throw new IllegalArgumentException("Invalid size parameter(must be a least 0)");

       this.size = size;
       this.buffer = new int[size];
    }


    // Not thread safe
    @Override
    public void sort(int[] array, int blkptr)
    {
        int sz =(this.size == 0) ? array.length : this.size;

        if (this.buffer.length < sz)
            this.buffer = new int[sz];

        System.arraycopy(array, blkptr, this.buffer, 0, sz);
        IndexedIntArray src = new IndexedIntArray(array, blkptr);
        IndexedIntArray dst = new IndexedIntArray(this.buffer, 0);
        sort(dst, src, blkptr, blkptr+sz);
    }

    private static void sort(IndexedIntArray srcIba, IndexedIntArray dstIba, int start, int end)
    {
        int length = end - start;

        if (length < 2)
            return;

        int[] src = srcIba.array;
        int[] dst = dstIba.array;

        // Insertion sort on smallest arrays
        if (length < 16)
        {
            start += dstIba.index;
            end   += dstIba.index;
            
            for (int i=start; i<end; i++)
            {
                for (int j=i; (j>start) && (dst[j-1]>dst[j]); j--)
                {
                    int tmp = dst[j-1];
                    dst[j-1] = dst[j];
                    dst[j] = tmp;
                }
            }

            return;
        }

        int mid =  (start + end) >>> 1;
        sort(dstIba, srcIba, start, mid);
        sort(dstIba, srcIba, mid, end);
        mid += srcIba.index;
 
        if (src[mid-1] <= src[mid])
        {
            System.arraycopy(src, start, dst, start, length);
            return;
        }

        int starti = start + dstIba.index;
        int endi = end + dstIba.index;
        int j = start + srcIba.index;
        int k = mid;
        int endk = end + srcIba.index;

        for (int i=starti; i<endi; i++)
        {
            if ((k >= endk) || (j < mid) && (src[j] <= src[k]))
                dst[i] = src[j++];
            else
                dst[i] = src[k++];
        }
    }


}

			


