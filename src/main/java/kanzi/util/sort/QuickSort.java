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

// One implementation of the most famous sorting algorithm

import kanzi.ArrayComparator;
import kanzi.IntSorter;



// There is a lot of litterature about quicksort
// A great reference is http://users.aims.ac.za/~mackay/sorting/sorting.html

public class QuickSort implements IntSorter
{
    private final ArrayComparator cmp;
    private final int size;


    public QuickSort()
    {
        this(0, null);
    }


    public QuickSort(int size)
    {
        this(size, null);
    }


    public QuickSort(int size, ArrayComparator cmp)
    {
        if (size < 0)
            throw new IllegalArgumentException("Invalid size parameter (must be at least 0)");

        this.cmp = cmp;
        this.size = size;
    }


    protected ArrayComparator getComparator()
    {
        return this.cmp;
    }


    protected int size()
    {
        return this.size;
    }


    protected int getThreshold()
    {
        return 2;
    }


    @Override
    public void sort(int[] array, int idx)
    {
        int maxIdx = (this.size == 0) ? array.length - 1 : this.size - 1;

        if (idx >= maxIdx)
            return;

        if (this.cmp != null)
            this.sortWithComparator(array, idx, maxIdx);
        else
            this.sortNoComparator(array, idx, maxIdx);
    }


    // Ternary partitioning
    protected void sortNoComparator(int[] array, int low, int high)
    {
        if (high <= low + this.getThreshold())
        {
            this.sortSmallArrayNoComparator(array, low, high);
            return;
        }

        // Regular path
        // Choose a pivot: this THE most important step of the algorithm since
        // a bad pivot can really ruin the performance (quadratic). Some research
        // papers show that picking a random index in the [low+1 .. high-1] range
        // is a good choice (on average).
        int mid = low + ((high - low) >> 1);
        int pivIdx;

        if (high - low < 40)
        {
            pivIdx = (array[low] < array[mid] ?
            (array[mid] < array[high] ? mid : array[low] < array[high] ? high : low) :
            (array[mid] > array[high] ? mid : array[low] > array[high] ? high : low));
        }
        else
        {
            int s = (high - low) >> 3;

            int l = (array[low] < array[low+s] ?
            (array[low+s] < array[low+s+s] ? low+s : array[low] < array[low+s+s] ? low+s+s : low) :
            (array[low+s] > array[low+s+s] ? low+s : array[low] > array[low+s+s] ? low+s+s : low));
            int m = (array[mid-s] < array[mid] ?
            (array[mid] < array[mid+s] ? mid : array[mid-s] < array[mid+s] ? mid+s : mid-s) :
            (array[mid] > array[mid+s] ? mid : array[mid-s] > array[mid+s] ? mid+s : mid-s));
            int h = (array[high-s-s] < array[high-s] ?
            (array[high-s] < array[high] ? high-s : array[high-s-s] < array[high] ? high : high-s-s) :
            (array[high-s] > array[high] ? high-s : array[high-s-s] > array[high] ? high : high-s-s));

            pivIdx = (array[l] < array[m] ?
            (array[m] < array[h] ? m : array[l] < array[h] ? h : l) :
            (array[m] > array[h] ? m : array[l] > array[h] ? h : l));
        }

        final int pivot = array[pivIdx];
        int i = low;
        int mi = low;
        int j = high;
        int mj = high;

        while (i <= j)
        {
            // Move up
            while (i <= j)
            {
                final int tmp = array[i];

                if (tmp > pivot)
                   break;

                if (tmp == pivot)
                {
                    // Move the pivot value to the low end.
                    array[i] = array[mi];
                    array[mi] = tmp;
                    mi++;
                }

                i++;
            }

            // Move down
            while (i <= j)
            {
                final int tmp = array[j];

                if (pivot > tmp)
                   break;

                if (tmp == pivot)
                {
                    // Move the pivot value to the high end.
                    array[j] = array[mj];
                    array[mj] = tmp;
                    mj--;
                }

                j--;
            }

            if (i <= j)
            {
                final int tmp = array[i];
                array[i] = array[j];
                array[j] = tmp;
                i++;
                j--;
            }
        }

        // Move the pivot values to the middle
        mi--;
        i--;

        for (; mi>=low; mi--, i--)
        {
            int tmp = array[i];
            array[i] = array[mi];
            array[mi] = tmp;
        }

        mj++;
        j++;

        for (; mj<=high; mj++, j++)
        {
            int tmp = array[j];
            array[j] = array[mj];
            array[mj] = tmp;
        }

        // Sort the low and high sub-arrays
        if (i - low >= 1)
           this.sortNoComparator(array, low, i);

        if (high - j >= 1)
           this.sortNoComparator(array, j, high);
    }


    // Ternary partitioning: the performance may be seriously degraded due to the
    // numerous calls to the array comparator.
    protected void sortWithComparator(int[] array, int low, int high)
    {
        if (high <= low + this.getThreshold())
        {
            this.sortSmallArrayWithComparator(array, low, high);
            return;
        }

        // Regular path
        // Choose a pivot: this THE most important step of the algorithm since
        // a bad pivot can really ruin the performance (quadratic). Some research
        // papers show that picking a random index in the [low+1 .. high-1] range
        // is a good choice (on average). Practically however, there is no gain
        // because of the time it takes to 'compute' a (pseudo) random value.
        int mid = low + ((high - low) >> 1);
        int pivIdx;

        if (high - low < 40)
        {
            pivIdx = (array[low] < array[mid] ?
            (array[mid] < array[high] ? mid : array[low] < array[high] ? high : low) :
            (array[mid] > array[high] ? mid : array[low] > array[high] ? high : low));
        }
        else
        {
            int s = (high - low) >> 3;
            int lows = low + s;
            int highs = high - s;

            int l = (array[low] < array[low+s] ?
            (array[lows] < array[lows+s] ? lows : array[low] < array[lows+s] ? lows+s : low) :
            (array[lows] > array[lows+s] ? lows : array[low] > array[lows+s] ? lows+s : low));
            int m = (array[mid-s] < array[mid] ?
            (array[mid] < array[mid+s] ? mid : array[mid-s] < array[mid+s] ? mid+s : mid-s) :
            (array[mid] > array[mid+s] ? mid : array[mid-s] > array[mid+s] ? mid+s : mid-s));
            int h = (array[highs-s] < array[highs] ?
            (array[highs] < array[high] ? highs : array[highs-s] < array[high] ? high : highs-s) :
            (array[highs] > array[high] ? highs : array[highs-s] > array[high] ? high : highs-s));

            pivIdx = (array[l] < array[m] ?
            (array[m] < array[h] ? m : array[l] < array[h] ? h : l) :
            (array[m] > array[h] ? m : array[l] > array[h] ? h : l));
        }

        int pivot = array[pivIdx];
        int i = low;
        int mi = low;
        int j = high;
        int mj = high;

        while (i <= j)
        {
            // Move up
            while ((i <= j) && (this.cmp.compare(array[i], pivot) <= 0))
            {
                if (array[i] == pivot)
                {
                    // Move the pivot value to the low end.
                    int tmp = array[i];
                    array[i] = array[mi];
                    array[mi] = tmp;
                    mi++;
                }

                i++;
            }

            // Move down
            while ((i <= j) && (this.cmp.compare(pivot, array[j]) <= 0))
            {
                if (array[j] == pivot)
                {
                    // Move the pivot value to the high end.
                    int tmp = array[j];
                    array[j] = array[mj];
                    array[mj] = tmp;
                    mj--;
                }

                j--;
            }

            if (i <= j)
            {
                int tmp = array[i];
                array[i] = array[j];
                array[j] = tmp;
                i++;
                j--;
            }
        }

        // Move the pivot values to the middle
        mi--;
        i--;

        for (; mi>=low; mi--, i--)
        {
            int tmp = array[i];
            array[i] = array[mi];
            array[mi] = tmp;
        }

        mj++;
        j++;

        for (; mj<=high; mj++, j++)
        {
            int tmp = array[j];
            array[j] = array[mj];
            array[mj] = tmp;
        }

        // Sort the low and high sub-arrays
        if (i - low >= 1)
           this.sortWithComparator(array, low, i);

        if (high - j >= 1)
           this.sortWithComparator(array, j, high);
    }


    protected void sortSmallArrayWithComparator(int[] array, int low, int high)
    {
        // Shortcut for 2 element-sub-array
        if (high == low + 1)
        {
            if (this.cmp.compare(array[low], array[high]) > 0)
            {
                int tmp = array[low];
                array[low] = array[high];
                array[high] = tmp;
            }

            return;
        }

        // Shortcut for 3 element-sub-array
        if (high == low + 2)
        {
            int a1 = array[low];
            int a2 = array[low+1];
            int a3 = array[high];

            if (this.cmp.compare(a1, a2) <= 0)
            {
                if (this.cmp.compare(a2, a3) <= 0)
                    return;

                if (this.cmp.compare(a3, a1) <= 0)
                {
                    array[low]   = a3;
                    array[low+1] = a1;
                    array[high]  = a2;
                    return;
                }

                array[low+1] = a3;
                array[high]  = a2;
            }
            else
            {
                if (this.cmp.compare(a1, a3) <= 0)
                {
                    array[low]   = a2;
                    array[low+1] = a1;
                    return;
                }

                if (this.cmp.compare(a3, a2) <= 0)
                {
                    array[low]  = a3;
                    array[high] = a1;
                    return;
                }

                array[low]   = a2;
                array[low+1] = a3;
                array[high]  = a1;
            }

            return;
        }

        throw new IllegalArgumentException("Invalid small array size: "+(high-low+1));
    }


    protected void sortSmallArrayNoComparator(int[] array, int low, int high)
    {
        // Shortcut for 2 element-sub-array
        if (high == low + 1)
        {
            if (array[low] > array[high])
            {
                int tmp = array[low];
                array[low] = array[high];
                array[high] = tmp;
            }

            return;
        }

        // Shortcut for 3 element-sub-array
        if (high == low + 2)
        {
            int a1 = array[low];
            int a2 = array[low+1];
            int a3 = array[high];

            if (a1 <= a2)
            {
                if (a2 <= a3)
                    return;

                if (a3 <= a1)
                {
                    array[low]   = a3;
                    array[low+1] = a1;
                    array[high]  = a2;
                    return;
                }

                array[low+1] = a3;
                array[high]  = a2;
            }
            else
            {
                if (a1 <= a3)
                {
                    array[low]   = a2;
                    array[low+1] = a1;
                    return;
                }

                if (a3 <= a2)
                {
                    array[low]  = a3;
                    array[high] = a1;
                    return;
                }

                array[low]   = a2;
                array[low+1] = a3;
                array[high]  = a1;
            }
            return;
        }

        throw new IllegalArgumentException("Invalid small array size: "+(high-low+1));
    }

}
