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

import kanzi.Global;
import kanzi.VideoEffectWithOffset;


public class LightingEffect implements VideoEffectWithOffset
{
    private final int width;
    private final int height;
    private final int radius;
    private final int stride;
    private int offset;
    private int savedOffset;
    private final int[] normalXY;
    private final int[] distanceMap;
    private int lightX;
    private int lightY;
    private int[] heightMap;
    private final boolean bumpMapping;
       
   
    public LightingEffect(int width, int height, int lightX, int lightY, int radius, boolean bumpMapping)
    {
       this(width, height, 0, width, lightX, lightY, radius, 100, bumpMapping);
    }


    // power in % (max pixel intensity)
    public LightingEffect(int width, int height, int offset, int stride, int lightX, int lightY, int radius,
             int power, boolean bumpMapping)
    {
        if (height < 8)
            throw new IllegalArgumentException("The height must be at least 8");
        
        if (width < 8)
            throw new IllegalArgumentException("The width must be at least 8");
       
        if (offset < 0)
            throw new IllegalArgumentException("The offset must be at least 0");
        
        if (stride < 8)
            throw new IllegalArgumentException("The stride must be at least 8");

        if ((power < 0) || (power > 1000))
            throw new IllegalArgumentException("The power must be in the range [0..1000]");

        this.lightX = lightX;
        this.lightY = lightY;
        this.radius = radius;
        this.width  = width;
        this.height = height;
        this.bumpMapping = bumpMapping;
        this.stride = stride;
        this.offset = offset;
        this.savedOffset = offset-1;
        this.distanceMap = new int[radius*radius];
        this.normalXY = (this.bumpMapping == true) ? new int[width*height] : null;
        
        // Initialize the distance table
        final int rd = this.radius;
        final int top = 1 << 10;
        final int maxIntensity = (255*power) / 100;
      
        for (int y=0; y<rd; y++)
        {
            final int y2 = y * y;
            final int startLine = rd * y;
            
            for (int x=0; x<rd; x++)
            {
                // 1 - alpha * (sqrt((x*x+y*y)/2)/(rd-1))
                // alpha is used to cutoff before the end of the window (set to 5/4)
                int d = top - (5 * (Global.sqrt((x*x+y2)/2) / (rd-1)) >> 2);
                d = (maxIntensity * d) >> 10;
                d = (d > 0) ? d : 0;
                d = (d < 255) ? d : 255;
                this.distanceMap[startLine+x] = d;          
            }
        }
    }
        
    
    private void calculateNormalMap(int[] rgb)
    {
        // Calculation previously completed ?
        if (this.savedOffset == this.offset)
           return;

        // Initialize the normal table
        final int length = this.width * this.height;
        int idx = 0;
        int startLine = this.offset;
        
        if (this.heightMap == null)
            this.heightMap = new int[length];

        final int[] map = this.heightMap;
        final int w = this.width;
        final int[] normals = this.normalXY;

        for (int j=0; j<this.height; j++)
        {
            for (int i=0; i<w; i++)
            {
                // Height of the pixel based on the grey scale
                final int pixel = rgb[startLine+i];
                final int r = (pixel >> 16) & 0xFF;
                final int g = (pixel >>  8) & 0xFF;
                final int b =  pixel & 0xFF;
                map[idx++] = (r + g + b) / 3;
            }
            
            startLine += this.stride;
        }
        
        // First and last lines
        for (int i=0; i<w; i++)
        {
           normals[i] = 0;
           normals[length-1-i] = 0;
        }

        final int hh = this.height - 1;
        final int ww = this.width - 1;
        int offs = this.width;
        
        for (int y=1; y<hh; y++)
        {
            // First in line (normalX = 0)
            int delta = map[offs+w] - map[offs-w];
            normals[offs] = delta & 0xFFFF;
            offs++;
            
            for (int x=1; x<ww; x++, offs++)
            {
                // Pack normalX and normalY into one integer (16 bits + 16 bits)
                delta = map[offs+1] - map[offs-1];
                final int tmp = (delta & 0xFFFF) << 16;
                delta = map[offs+w] - map[offs-w];
                normals[offs] = tmp | (delta & 0xFFFF);
            }
            
            // Last in line (normalX = 0)
            delta = map[offs+w] - map[offs-w];
            normals[offs] = delta & 0xFFFF;
            offs++;
        }
    }
    
    
    @Override
    public int[] apply(int[] src, int[] dst)
    {
        final int lx = this.lightX;
        final int ly = this.lightY;
        int lineStart = this.offset + (lx - this.radius) + this.stride * (ly - this.radius);
        final int x0 = (lx >= this.radius) ? lx - this.radius : 0;
        final int x1 = (lx + this.radius) < this.width ? lx + this.radius : this.width;
        final int y0 = (ly >= this.radius) ? ly - this.radius : 0;
        final int y1 = (ly + this.radius) < this.height ? ly + this.radius : this.height;
        final int[] normals = this.normalXY;
        final int[] intensities = this.distanceMap;
        final int rd = this.radius;
        final int w = this.width;
        final int h = this.height;
        
        if (y0 > 0)
        {
           // First lines are black
           int offs = this.offset; 
           
           for (int yy=0; yy<y0; yy++)
           {
              for (int xx=0; xx<w; xx++)
                 dst[offs+xx] = 0;

              offs += this.stride;
           }
        }
        
        if (y1 < h)
        {
           // Last lines are black
           int offs = this.offset + this.stride*h; 

           for (int yy=h; yy>=y1; yy--)
           {
              for (int xx=0; xx<w; xx++)
                 dst[offs+xx] = 0;

              offs -= this.stride;
           }
        }
       
        // Is there a bump mapping effect ?
        if (this.bumpMapping == true)
        {
            this.calculateNormalMap(src);
            
            for (int y=y0; y<y1; y++)
            {
                int idx = lineStart-x0;
                int offs = y * w;
                
                if (x0 > 0)
                {
                   // First pixels
                   for (int xx=0; xx<x0; xx++)
                      dst[idx++] = 0;
                }        
               
                for (int x=x0; x<x1; x++)
                {
                    final int normal = normals[offs+x];
                    
                    // First, extract the normal X coord. (16 upper bits) out of normalXY
                    // Use a short first, then expand to an int (takes care of negative
                    // number expansion)
                    short tmp = (short) (normal >> 16);
                    int val = tmp - x + lx;
                    int dx = (val > 0) ? val : -val;
                    dx = (dx < rd) ? dx : rd-1;
                    
                    // Extract the normal Y coord. as a short then expand to an int
                    // (takes care of negative number expansion)
                    tmp = (short) (normal & 0xFFFF);
                    val = tmp - y + ly;
                    int dy = (val > 0) ? val : -val;
                    dy = (dy < rd) ? dy : rd-1;
                    
                    final int intensity = intensities[dy*rd+dx];
                    final int pixel = src[idx];
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >>  8) & 0xFF;
                    int b =  pixel & 0xFF;
                    r = (intensity * r) >> 8;
                    g = (intensity * g) >> 8;
                    b = (intensity * b) >> 8;                    
                    dst[idx++] = (r << 16) | (g << 8) | b;
                }

                if (x1 < w)
                {
                   // First pixels
                   for (int xx=x1; xx<w; xx++)
                      dst[idx++] = 0;
                }

                lineStart += this.stride;
            }
        }
        else // No bump mapping: just lighting
        {            
            for (int y=y0; y<y1; y++)
            {
                int idx = lineStart-x0;
                
                if (x0 > 0)
                {
                   // First pixels
                   for (int xx=0; xx<x0; xx++)
                      dst[idx++] = 0;
                }        

                int dy = (y > ly) ? y - ly : ly - y;
                dy = (dy < rd) ? dy : rd - 1;
                final int yy = dy * rd;
                
                for (int x=x0; x<x1; x++)
                {
                    int dx = (x > lx) ? x - lx : lx - x;
                    dx = (dx < rd) ? dx : rd - 1;
                    final int intensity = intensities[yy+dx];
                    final int pixel = src[idx];
                    int r = (pixel >> 16) & 0xFF;
                    int g = (pixel >>  8) & 0xFF;
                    int b =  pixel & 0xFF;
                    r = (intensity * r) >> 8;
                    g = (intensity * g) >> 8;
                    b = (intensity * b) >> 8;                    
                    dst[idx++] = (r << 16) | (g << 8) | b;
                }

                if (x1 < w)
                {
                   // First pixels
                   for (int xx=x1; xx<w; xx++)
                      dst[idx++] = 0;
                }

                lineStart += this.stride;
            }
        }
        
        return dst;
    }
    
    
    // Not thread safe
    public void moveLight(int x, int y)
    {
        this.lightX = x;
        this.lightY = y;
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

        this.savedOffset = this.offset;
        this.offset = offset;
        return true;
    }
}

