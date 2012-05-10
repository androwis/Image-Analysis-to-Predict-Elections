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


// Based on algorithm from http://www.blackpawn.com/texts/blur/default.html

public final class BlurFilter implements VideoEffectWithOffset
{
    private final int width;
    private final int height;
    private final int stride;
    private final int radius;
    private final int iterations;
    private final int[] line;
    private int offset;
    
    
    public BlurFilter(int width, int height, int radius)
    {
        this(width, height, 0, width, radius, 4);
    }
    
    
    public BlurFilter(int width, int height, int offset, int stride, int radius)
    {
        this(width, height, offset, stride, radius, 4);
    }
    
    
    public BlurFilter(int width, int height, int offset, int stride, int radius, int iterations)
    {
        if (height < 8)
            throw new IllegalArgumentException("The height must be at least 8");
        
        if (width < 8)
            throw new IllegalArgumentException("The width must be at least 8");
        
        if (offset < 0)
            throw new IllegalArgumentException("The offset must be at least 0");
        
        if (stride < 8)
            throw new IllegalArgumentException("The stride must be at least 8");
        
        if (radius < 1)
            throw new IllegalArgumentException("The radius must be at least 1");
        
        if (iterations < 1)
            throw new IllegalArgumentException("The iterations must be at least 1");
        
        if (iterations > 100)
            throw new IllegalArgumentException("The iterations must be at most 100");
        
        this.height = height;
        this.width = width;
        this.offset = offset;
        this.stride = stride;
        this.radius = radius;
        this.iterations = iterations;
        int size = (this.width > this.height) ? this.width : this.height;
        this.line = new int[size];
    }
    

    @Override
    public int[] apply(int[] src, int[] dst)
    {
        this.blurHorizontal(src, dst);
        this.blurVertical(dst, dst);
        
        for (int i=0; i<this.iterations-1; i++)
        {
            this.blurHorizontal(dst, dst);
            this.blurVertical(dst, dst);
        }
        
        // Is there a part umodified ? If so copy pixels from src to dst
        int remaining = this.stride - this.width - this.offset;
        
        if ((remaining > 0) || (this.offset > 0))
        {
            int startLine = 0;
            
            for (int j=0; j<this.height; j++)
            {
                if (this.offset > 0)
                    System.arraycopy(src, startLine, dst, startLine, this.offset);
                
                if (remaining > 0)
                {
                    int start = startLine + this.offset + this.width;
                    System.arraycopy(src, start, dst, start, remaining);
                }
                
                startLine += this.stride;
            }
        }
        
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
    
    
    // Implementation using a sliding box to reduce the number of operations
    private int[] blurHorizontal(int[] src, int[] dst)
    {
        int boxSize = (2 * this.radius) + 1;
        int startLine = this.offset;
        
        for (int j=0; j<this.height; j++)
        {
            // First pixel of each line: calculate the sum over the whole box
            int pixel = src[startLine];
            
            // Pixel 0: sum 'negative' x pixels ('radius' times)
            int totalR = this.radius * ((pixel >> 16) & 0xFF);
            int totalG = this.radius * ((pixel >>  8) & 0xFF);
            int totalB = this.radius * ( pixel & 0xFF);
            
            for (int i=0, n=0; i<=this.radius; i++)
            {
                pixel = src[startLine+n];
                totalR += ((pixel >> 16) & 0xFF);
                totalG += ((pixel >>  8) & 0xFF);
                totalB +=  (pixel & 0xFF);
                
                if (n < this.width - 1)
                    n++;
            }
            
            // Subsequent pixels: update the sum by sliding the whole box
            for (int i=0; i<this.width; i++)
            {
                int val;
                val  = (totalR / boxSize) << 16;
                val |= (totalG / boxSize) <<  8;
                val |= (totalB / boxSize);
                this.line[i] = val;
                
                // Limit lastIdx to positive or null values
                int lastIdx = i - this.radius;
                lastIdx = lastIdx & (-lastIdx >> 31);
                
                // Limit newIdx to values less than width
                int newIdx = i + this.radius + 1;
                int mask = (newIdx - this.width) >>> 31;
                newIdx = (newIdx & -mask) | ((this.width - 1) & (mask - 1));
                
                int enteringPixel = src[startLine+newIdx];
                int leavingPixel  = src[startLine+lastIdx];
                
                // Update sums of sliding window
                totalR += ((enteringPixel >> 16) & 0xFF);
                totalG += ((enteringPixel >>  8) & 0xFF);
                totalB +=  (enteringPixel & 0xFF);
                totalR -= ((leavingPixel >> 16) & 0xFF);
                totalG -= ((leavingPixel >>  8) & 0xFF);
                totalB -=  (leavingPixel & 0xFF);
            }
            
            for (int i=0, n=startLine; i<this.width; i++, n++)
                dst[n] = this.line[i];
            
            startLine += this.stride;
        }
        
        return dst;
    }
    
    
    
    // Implementation using a sliding box to reduce the number of operations
    private int[] blurVertical(int[] src, int[] dst)
    {
        int len = this.stride * this.height;
        int boxSize = (2 * this.radius) + 1;
        int startLine = this.offset;
        
        for (int j=0; j<this.width; j++)
        {
            // First pixel of each line: calculate the sum over the whole box
            int pixel = src[startLine];
            
            // Pixel 0: sum 'negative' x pixels ('radius' times)
            int totalR = this.radius * ((pixel >> 16) & 0xFF);
            int totalG = this.radius * ((pixel >>  8) & 0xFF);
            int totalB = this.radius * ( pixel & 0xFF);
            
            for (int i=0, n=0; i<=this.radius; i++)
            {
                pixel = src[startLine+n];
                totalR += ((pixel >> 16) & 0xFF);
                totalG += ((pixel >>  8) & 0xFF);
                totalB +=  (pixel & 0xFF);
                
                if (n + this.stride < len)
                    n += this.stride;
            }
            
            // Subsequent pixels: update the sum by sliding the window
            for (int i=0; i<this.height; i++)
            {
                int val;
                val  = (totalR / boxSize) << 16;
                val |= (totalG / boxSize) <<  8;
                val |= (totalB / boxSize);
                this.line[i] = val;
                
                // Limit lastIdx to positive or null values
                int lastIdx = i - this.radius;
                lastIdx = lastIdx & (-lastIdx >> 31);
                lastIdx *= this.stride;
                
                // Limit newIdx to values less than height
                int newIdx  = i + this.radius + 1;
                int mask = (newIdx - this.height) >>> 31;
                newIdx = (newIdx & -mask) | ((this.height - 1) & (mask - 1));
                newIdx *= this.stride;
                
                int enteringPixel = src[startLine+newIdx];
                int leavingPixel  = src[startLine+lastIdx];
                
                // Update sums of sliding window
                totalR += ((enteringPixel >> 16) & 0xFF);
                totalG += ((enteringPixel >> 8)  & 0xFF);
                totalB +=  (enteringPixel & 0xFF);
                totalR -= ((leavingPixel >> 16) & 0xFF);
                totalG -= ((leavingPixel >> 8)  & 0xFF);
                totalB -=  (leavingPixel & 0xFF);
            }
            
            for (int i=0, n=startLine; i<this.height; i++, n+=this.stride)
                dst[n] = this.line[i];
            
            startLine++;
        }
        
        return dst;
    }
       
}