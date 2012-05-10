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

import java.util.concurrent.RecursiveAction;
import kanzi.ByteSorter;
import kanzi.IntSorter;
import kanzi.util.sort.BucketSort;
import kanzi.util.sort.FlashSort;


public class SortTask extends RecursiveAction
{
   private static final int MIN_THRESHOLD = 8192;
   private static final int MAX_THRESHOLD = MIN_THRESHOLD << 1;

   private final transient IntSorter iDelegate;
   private final transient ByteSorter bDelegate;
   private final int[] iSrc;
   private final int[] iDst;
   private final byte[] bSrc;
   private final byte[] bDst;
   private final int size;
   private final int startIdx;
   private final int threshold;


   public SortTask(int[] array, int len, int startIdx)
   {
      this(array, len, startIdx, new int[len]);
   }


   protected SortTask(int[] array, int len, int startIdx, int[] buffer)
   {
      this.size = len;
      this.startIdx = startIdx;
      this.iDelegate = new FlashSort(len);
      this.iDst = buffer;
      this.iSrc = array;
      this.bDelegate = null;
      this.bDst = null;
      this.bSrc = null;

      while (len >= MAX_THRESHOLD)
          len >>= 1;

      this.threshold = (len < MIN_THRESHOLD) ? MIN_THRESHOLD : len;
   }


   public SortTask(byte[] array, int len, int startIdx)
   {
      this(array, len, startIdx, new byte[len]);
   }


   protected SortTask(byte[] array, int len, int startIdx, byte[] buffer)
   {
      this.size = len;
      this.startIdx = startIdx;
      this.bDelegate = new BucketSort(len, 8);
      this.bDst = buffer;
      this.bSrc = array;
      this.iDelegate = null;
      this.iDst = null;
      this.iSrc = null;

      while (len >= MAX_THRESHOLD)
          len >>= 1;

      this.threshold = (len < MIN_THRESHOLD) ? MIN_THRESHOLD : len;
   }


   @Override
   protected void compute()
   {
      if (this.iSrc != null)
        this.sortInts();
      else
        this.sortBytes();
   }


   protected void sortBytes()
   {
      if (this.size < this.threshold)
      {
         this.bDelegate.sort(this.bSrc, this.startIdx);
         return;
      }

      int half = this.size >> 1;
      SortTask lowerHalfTask = new SortTask(this.bSrc, half, this.startIdx, this.bDst);
      SortTask upperHalfTask = new SortTask(this.bSrc, this.size-half, this.startIdx+half, this.bDst);

      // Fork
      invokeAll(lowerHalfTask, upperHalfTask);

      // Join
      byte[] source = this.bSrc;
      byte[] dest = this.bDst;
      int idx  = this.startIdx;
      int idx1 = this.startIdx;
      int idx2 = idx1 + half;
      int end1 = idx2;
      int end2 = this.startIdx + this.size;

      while ((idx1 < end1) && (idx2 < end2))
      {
         dest[idx++] = (source[idx1] < source[idx2]) ? source[idx1++] : source[idx2++];
      }

      while (idx1 < end1)
          dest[idx++] = source[idx1++];

      while (idx2 < end2)
          dest[idx++] = source[idx2++];

      System.arraycopy(dest, this.startIdx, source, this.startIdx, this.size);
   }


   protected void sortInts()
   {
      if (this.size < this.threshold)
      {
         this.iDelegate.sort(this.iSrc, this.startIdx);
         return;
      }

      int half = this.size >> 1;
      SortTask lowerHalfTask = new SortTask(this.iSrc, half, this.startIdx, this.iDst);
      SortTask upperHalfTask = new SortTask(this.iSrc, this.size-half, this.startIdx+half, this.iDst);

      // Fork
      invokeAll(lowerHalfTask, upperHalfTask);

      // Join
      int[] source = this.iSrc;
      int[] dest = this.iDst;
      int idx  = this.startIdx;
      int idx1 = this.startIdx;
      int idx2 = idx1 + half;
      int end1 = idx2;
      int end2 = this.startIdx + this.size;

      while ((idx1 < end1) && (idx2 < end2))
      {
         dest[idx++] = (source[idx1] < source[idx2]) ? source[idx1++] : source[idx2++];
      }

      while (idx1 < end1)
          dest[idx++] = source[idx1++];

      while (idx2 < end2)
          dest[idx++] = source[idx2++];

      System.arraycopy(dest, this.startIdx, source, this.startIdx, this.size);
   }

}
