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

package kanzi.function.wavelet;

import kanzi.IndexedIntArray;
import kanzi.IntFunction;


// Very simple filter that removes coefficients in the wavelet domain
// The filter removes coefficients in the outer ring of the high frequency bands
public class WaveletRingFilter implements IntFunction
{
    private final int width;
    private final int height;
    private final int levels;
    private final int ringWidth;

    
    public WaveletRingFilter(int width, int height, int levels, int ringWidth)
    {
        if (width < 8)
            throw new IllegalArgumentException("The width of the image must be at least 8");

        if (height < 8)
            throw new IllegalArgumentException("The height of the image must be at least 8");

        if (levels < 1)
            throw new IllegalArgumentException("The number of wavelet sub-band levels must be at least 1");

        if (levels >= 4)
           throw new IllegalArgumentException("The number of wavelet sub-band levels must be at most 3");

        if (ringWidth < 1)
           throw new IllegalArgumentException("The width of the ring must be at least 1");

        if (ringWidth > (width >> 1))
           throw new IllegalArgumentException("The width of the ring must be at most "+(width >> 1));

        if (ringWidth > (height >> 1))
           throw new IllegalArgumentException("The width of the ring must be at most "+(height >> 1));

        this.width = width;
        this.height = height;
        this.levels = levels;
        this.ringWidth = ringWidth;
    }


    // Remove a ring of coefficients around the borders in the high frequency levels
    // (nullify the details in the outer ring of the frame).
    @Override
    public boolean forward(IndexedIntArray source, IndexedIntArray destination)
    {
       final int w = this.width;
       final int h = this.height;
       final int srcIdx = source.index;
       final int[] dst = destination.array;
       int rw = this.ringWidth;
       int bandW = w;
       int bandH = h;

       // Sub-bands:
       // LL HL
       // LH HH
       for (int level=0; level<this.levels; level++)
       {
          final int halfBandW = bandW >> 1;
          final int halfBandH = bandH >> 1;
          final int startHL = srcIdx + halfBandW;
          final int startLH = srcIdx + (halfBandH * w);
          final int startHH = startLH + halfBandW;
          final int endHL = startHL + ((halfBandH - 1) * w);
          final int endLH = srcIdx + ((h-1) * w);
          final int endHH = endLH + halfBandW;
          int offs = 0;

          for (int j=0; j<rw; j++)
          {
            for (int i=halfBandW-1; i>=0; i--)
            {
               int firstLine = i + offs;
               int lastLine = i - offs;

               // HL quadrant: first and last lines (2 per iteration)
               dst[startHL+firstLine] = 0;
               dst[endHL+lastLine] = 0;

               // HH quadrant: first and last lines (2 per iteration)
               dst[startHH+firstLine] = 0;
               dst[endHH+lastLine] = 0;

               // LH quadrant: first and last lines (2 per iteration)
               dst[startLH+firstLine] = 0;
               dst[endLH+lastLine] = 0;
            }

            offs += w;
          }

          for (int j=rw; j<halfBandH; j++)
          {
            for (int i=0; i<rw; i++)
            {
               int idx1 = offs + i;
               int idx2 = offs + halfBandW - 1 - i;

               // HL quadrant: first and last columns (2 per iteration)
               dst[startHL+idx1] = 0;
               dst[startHL+idx2] = 0;

               // HH quadrant: first and last columns (2 per iteration)
               dst[startHH+idx1] = 0;
               dst[startHH+idx2] = 0;

               // LH quadrant: first and last columns (2 per iteration)
               dst[startLH+idx1] = 0;
               dst[startLH+idx2] = 0;
            }

            offs += w;
          }


          if (rw < 4)
             break;

          rw >>= 2;
          bandW >>= 1;
          bandH >>= 1;
       }

       source.index = w * h;
       destination.index = w * h;
       return true;
    }


    // Cannot be reversed
    @Override
    public boolean inverse(IndexedIntArray source, IndexedIntArray destination)
    {
       return false;
    }
}
