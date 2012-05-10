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

import kanzi.ArrayComparator;
import kanzi.IntSorter;


// HeapSort is a comparison sort with O(n ln n) complexity. Practically, it is
// usually slower than QuickSort.
public final class HeapSort implements IntSorter
{
    private final ArrayComparator cmp;
    private final int size;
    
    
    public HeapSort()
    {
        this(0, null);
    }
    
    
    public HeapSort(int size)
    {
        this(size, null);
    }
    
    
    public HeapSort(int size, ArrayComparator cmp)
    {
        if (size < 0)
            throw new IllegalArgumentException("Invalid size parameter (must be a least 0)");
        
        this.cmp = cmp;
        this.size = size;
    }
    
    
    @Override
    public void sort(int[] array, int blkptr)
    {
        int length = (this.size == 0) ? array.length : this.size;
        
        if (this.cmp != null)
        {
            for (int k=length>>1; k>0; k--)
            {
                this.sortWithComparator(array, blkptr+k, length);
            }
            
            for (int i=length-1; i>0; i--)
            {
                int temp = array[0];
                array[0] = array[i];
                array[i] = temp;
                this.sortWithComparator(array, blkptr+1, i);
            }
        }
        else
        {
            for (int k=length>>1; k>0; k--)
            {
                this.sortNoComparator(array, blkptr+k, length);
            }
            
            for (int i=length-1; i>0; i--)
            {
                int temp = array[0];
                array[0] = array[i];
                array[i] = temp;
                this.sortNoComparator(array, blkptr+1, i);
            }
        }
        
    }
    
    
    private void sortWithComparator(int[] array, int idx, int count)
    {
        int k = idx;
        final int temp = array[k-1];
        final int n = count >> 1;
        final ArrayComparator c = this.cmp;
        
        while (k <= n)
        {
            int j = k << 1;
            
            if ((j < count) && (c.compare(array[j-1], array[j]) < 0))
                j++;
            
            if (temp >= array[j-1])
                break;
            
            array[k-1] = array[j-1];
            k = j;
        }
        
        array[k-1] = temp;
    }
    
    
    private void sortNoComparator(int[] array, int idx, int count)
    {
        int k = idx;
        final int temp = array[k-1];
        final int n = count >> 1;
        
        while (k <= n)
        {
            int j = k << 1;
            
            if ((j < count) && (array[j-1] < array[j]))
                j++;
            
            if (temp >= array[j-1])
                break;
            
            array[k-1] = array[j-1];
            k = j;
        }
        
        array[k-1] = temp;
    }
    
}
