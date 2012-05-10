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

package kanzi.filter;

import kanzi.VideoEffectWithOffset;


public final class SobelFilter implements VideoEffectWithOffset
{
    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;

    // Type of Sobel filter
    // Can generate RGB/YCC image or array of costs (cost range = [0..255])
    public static final int IMAGE = 0xFFFFFFFF;
    public static final int COST = 0x0000FF;
    public static final int THREE_CHANNELS = 3;
    public static final int ONE_CHANNEL = 1;

    private final int width;
    private final int height;
    private final int stride;
    private final int direction;
    private final int mask;
    private final int channels;
    private int offset;


    public SobelFilter(int width, int height)
    {
       this(width, height, 0, width, VERTICAL | HORIZONTAL, THREE_CHANNELS, IMAGE);
    }


    public SobelFilter(int width, int height, int offset, int stride)
    {
       this(width, height, offset, stride, VERTICAL | HORIZONTAL, THREE_CHANNELS, IMAGE);
    }


    public SobelFilter(int width, int height, int offset, int stride,
            int direction, int nbChannels, int filterType)
    {
        if (height < 8)
            throw new IllegalArgumentException("The height must be at least 8");

        if (width < 8)
            throw new IllegalArgumentException("The width must be at least 8");

        if (offset < 0)
            throw new IllegalArgumentException("The offset must be at least 0");

        if (stride < 8)
            throw new IllegalArgumentException("The stride must be at least 8");

        if ((direction & (HORIZONTAL | VERTICAL)) == 0)
            throw new IllegalArgumentException("Invalid direction parameter (must be VERTICAL or HORIZONTAL or both)");

        if ((direction & ~(HORIZONTAL | VERTICAL)) != 0)
            throw new IllegalArgumentException("Invalid direction parameter (must be VERTICAL or HORIZONTAL or both)");

        if ((filterType != COST) && (filterType != IMAGE))
            throw new IllegalArgumentException("Invalid filter type parameter (must be IMAGE or COST)");

        if ((nbChannels != THREE_CHANNELS) && (nbChannels != ONE_CHANNEL))
            throw new IllegalArgumentException("Invalid image type parameter (must be ONE_CHANNEL or THREE_CHANNELS)");

        this.height = height;
        this.width = width;
        this.offset = offset;
        this.stride = stride;
        this.direction = direction;
        this.mask = filterType;
        this.channels = nbChannels;
    }


    // Return a picture or a map of costs if costMult64 is not null
    //   Horizontal                               Vertical
    //   -1  0   1        pix00 pix01 pix02        1  2  1
    //   -2  0   2  <-->  pix10 pix11 pix12  <-->  0  0  0
    //   -1  0   1        pix20 pix21 pix22       -1 -2 -1
    // Implementation focused on speed through reduction of array access
    // A naive implementation requires around 10*w*h accesses
    // This implementation requires around 4*w*h accesses
    @Override
    public int[] apply(int[] src, int[] dst)
    {
        int offs = this.offset;
        int startLine = offs;
        final int mask_ = this.mask;
        final int h = this.height;
        final int w = this.width;
        final boolean isVertical = ((this.direction & VERTICAL) != 0) ? true : false;
        final boolean isHorizontal = ((this.direction & HORIZONTAL) != 0) ? true : false;
        final int maxVal = 0x00FFFFFF & mask_;
        final int shift = (isVertical && isHorizontal) ? 1 : 0;
        boolean isPacked = (this.channels == 3) ? true : false;

        for (int y=2; y<h; y++)
        {
           int line = startLine + this.stride;
           int endLine = line + this.stride;
           final int pixel00 = src[startLine];
           final int pixel10 = src[line];
           final int pixel20 = src[endLine];
           final int pixel01 = src[startLine+1];
           final int pixel11 = src[line+1];
           final int pixel21 = src[endLine+1];
           int val00, val01, val10, val11, val20, val21;

           if (isPacked)
           {
              // Gray levels
              val00 = (((pixel00 >> 16) & 0xFF) + ((pixel00 >> 8) & 0xFF) + (pixel00 & 0xFF)) / 3;
              val01 = (((pixel01 >> 16) & 0xFF) + ((pixel01 >> 8) & 0xFF) + (pixel01 & 0xFF)) / 3;
              val10 = (((pixel10 >> 16) & 0xFF) + ((pixel10 >> 8) & 0xFF) + (pixel10 & 0xFF)) / 3;
              val11 = (((pixel11 >> 16) & 0xFF) + ((pixel11 >> 8) & 0xFF) + (pixel11 & 0xFF)) / 3;
              val20 = (((pixel20 >> 16) & 0xFF) + ((pixel20 >> 8) & 0xFF) + (pixel20 & 0xFF)) / 3;
              val21 = (((pixel21 >> 16) & 0xFF) + ((pixel21 >> 8) & 0xFF) + (pixel21 & 0xFF)) / 3;
           }
           else
           {
              val00 = pixel00 & 0xFF;
              val01 = pixel01 & 0xFF;
              val10 = pixel10 & 0xFF;
              val11 = pixel11 & 0xFF;
              val20 = pixel20 & 0xFF;
              val21 = pixel21 & 0xFF;
           }

           for (int x=2; x<w; x++)
           {
             final int pixel02 = src[startLine+x];
             final int pixel12 = src[line+x];
             final int pixel22 = src[endLine+x];
             final int val02, val12, val22;

             if (isPacked)
             {
                // Gray levels
                val02 = (((pixel02 >> 16) & 0xFF) + ((pixel02 >> 8) & 0xFF) + (pixel02 & 0xFF)) / 3;
                val12 = (((pixel12 >> 16) & 0xFF) + ((pixel12 >> 8) & 0xFF) + (pixel12 & 0xFF)) / 3;
                val22 = (((pixel22 >> 16) & 0xFF) + ((pixel22 >> 8) & 0xFF) + (pixel22 & 0xFF)) / 3;
             }
             else
             {
                val02 = pixel02 & 0xFF;
                val12 = pixel12 & 0xFF;
                val22 = pixel22 & 0xFF;
             }

             int valH = 0;
             int valV = 0;

             if (isHorizontal)
             {
                valH = -val00 + val02 - val10 - val10 + val12 + val12 - val20 + val22;
                valH = (valH + (valH >> 31)) ^ (valH >> 31);
             }

             if (isVertical)
             {
                valV = val00 + val01 + val01 + val02 - val20 - val21 - val21 - val22;
                valV = (valV + (valV >> 31)) ^ (valV >> 31);
             }

             int val = (valH + valV) >> shift;

             if (val > 255)
                dst[line+x-1] = maxVal;
             else
                dst[line+x-1] = ((val << 16) | (val << 8) | val) & mask_;

             // Slide the 3x3 window (reassign 6 pixels: left + center columns)
             val00 = val01;
             val01 = val02;
             val10 = val11;
             val11 = val12;
             val20 = val21;
             val21 = val22;
          }

          // Boundary processing, just duplicate pixels
          dst[line] = dst[line+1] & mask_;
          dst[line+w-1] = dst[line+w-2] & mask_;
          startLine = line;
       }

       final int firstLine = this.offset;
       final int lastLine = this.offset + this.stride * (this.height - 1);

       // Duplicate first and last lines
       System.arraycopy(dst, firstLine+this.stride, dst, firstLine, w);
       System.arraycopy(dst, lastLine-this.stride, dst, lastLine, w);

       return dst;
    }


    @Override
    public int getOffset()
    {
        return this.offset;
    }


    // Not thread safe
    @Override
    public boolean setOffset(int offset)
    {
        if (offset < 0)
            return false;

        this.offset = offset;
        return true;
    }
}
