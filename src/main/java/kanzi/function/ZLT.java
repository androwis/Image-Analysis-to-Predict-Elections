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

package kanzi.function;

import kanzi.ByteFunction;
import kanzi.IndexedByteArray;

// Zero Length Encoding is a simple encoding algorithm by Wheeler
// closely related to Run Length Encoding. The main difference is
// that only runs of 0 values are processed. Also, the length is
// encoded in a different way (each digit in a different byte)
// This little algorithm is well adapted to process post BWT/MTFT data

public final class ZLT implements ByteFunction
{
   private int copies;
   private final int size;


   public ZLT()
   {
      this(0);
   }


   public ZLT(int size)
   {
      if (size < 0)
         throw new IllegalArgumentException("Invalid size parameter (must be at least 0)");

      this.size = size;
   }


   public int size()
   {
      return this.size;
   }


   // Not thread safe
   @Override
   public boolean forward(IndexedByteArray source, IndexedByteArray destination)
   {
      int srcIdx = source.index;
      int dstIdx = destination.index;
      final byte[] src = source.array;
      final byte[] dst = destination.array;
      final int end = (this.size == 0) ? src.length : srcIdx + this.size;
      int runLength = this.copies;

      while ((srcIdx < end) && (dstIdx < dst.length))
      {
         int val = src[srcIdx];

         if (val == 0)
         {
            runLength++;
            srcIdx++;

            if ((srcIdx < end) && (runLength < Integer.MAX_VALUE))
                continue;
         }

         if (runLength > 0)
         {
             // Write length
            int log2 = 0;
            final int run = runLength + 1;

            for (int val2=run; val2>1; val2>>=1)
               log2++;

            if (dstIdx <= dst.length - log2)
            {
                // Write every bit as a byte except the most significant one
                while (log2 > 0)
                {
                   log2--;
                   dst[dstIdx++] = (byte) ((run >> log2) & 1);
                }
                
                runLength = 0;
                continue;
            }
            else // will reach end of destination array, must truncate block
            {
                // Can only write the bits that fit into the destination array
                log2 = dst.length - dstIdx;

                // Write every bit as a byte except the most significant one
                while (dstIdx < dst.length)
                   dst[dstIdx++] = (byte) 1;

                // The most significant bit is not encoded, so log2 corresponds
                // to the max value of (1 << ((log2+1) + 1)) - 1
                int delta = (1 << (log2 + 2)) - 1;
                runLength -= delta;
                srcIdx -= delta;
                break;
            }
         }

         val &= 0xFF;

         if (val >= 0xFE)
         {
            if (dstIdx >= dst.length - 1)
               break;

            dst[dstIdx++] = (byte) 0xFF;
            dst[dstIdx++] = (byte) (val - 0xFE);
         }
         else
         {
            dst[dstIdx++] = (byte) (val + 1);
         }

         srcIdx++;
      }

      this.copies = runLength;
      source.index = srcIdx;
      destination.index = dstIdx;
      return true;
   }


   // Not thread safe
   @Override
   public boolean inverse(IndexedByteArray source, IndexedByteArray destination)
   {
      int srcIdx = source.index;
      int dstIdx = destination.index;
      final byte[] src = source.array;
      final byte[] dst = destination.array;
      final int end = (this.size == 0) ? src.length : srcIdx + this.size;
      int runLength = this.copies;

      while ((srcIdx < end) && (dstIdx < dst.length))
      {
         if (runLength > 0)
         {
            runLength--;
            dst[dstIdx++] = 0;
            continue;
         }

         int val = src[srcIdx] & 0xFF;

         if (val <= 1)
         {
            // Generate the run length bit by bit (but force MSB)
            int run = 1;

            // Exit if no more data to read from source array (incomplete length)
            // Calling the method again with reset arrays will resume 'correctly'
            while (val <= 1)
            {
               run = (run << 1) | val;
               srcIdx++;

               if (srcIdx >= end)
                   break;

               val = src[srcIdx] & 0xFF;
            }

            // Update run length
            runLength = run - 1;
            continue;
         }

         // Regular data processing (not >= !!!)
         if (val > 0xFE)
         {
            srcIdx++;
            val += (src[srcIdx] & 0x01);
         }

         dst[dstIdx] = (byte) (val - 1);
         dstIdx++;
         srcIdx++;
      }

      int min = (runLength <= (dst.length-dstIdx)) ? runLength : dst.length-dstIdx;

      while (--min >= 0)
         dst[dstIdx++] = 0;

      this.copies = runLength;
      source.index = srcIdx;
      destination.index = dstIdx;
      return true;
   }
}