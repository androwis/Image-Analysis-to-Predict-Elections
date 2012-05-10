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
import kanzi.util.sampling.DecimateDownSampler;

//  See http://en.wikipedia.org/wiki/Bilateral_filter.
//  Implementation of the O(1) bilateral filtering method presented in the following reference:
//  [Qingxiong Yang, Kar-Han Tan and Narendra Ahuja, Real-time O(1) Bilateral Filtering,
//  IEEE Conference on Computer Vision and Pattern Recognition (CVPR) 2009]
//
//  The algorithm provided here is an approximation of the bilateral filter based
//  on interpolation of spatial filters at different pixel intensity levels. It
//  runs in constant time.
//  The pixel intensity levels are quantized and only the filters for the quantized
//  values are calculated. The implementation also includes (optional) spatial
//  sub-sampling to increase the overall speed.
//  This implementation is initially based on the C code available at http://www.cs.cityu.edu.hk/~qiyang/
//
public final class FastBilateralFilter implements VideoEffectWithOffset
{
    private final int width;
    private final int height;
    private final int stride;
    private final int downSampling;
    private final int radius;
    private final float[] box;
    private final float[][] jk;
    private final float[] wk;
    private final float[] grayscale;
    private final int[] colors;
    private final int[] buffer1;
    private final int[] buffer2;
    private final int channels;
    private int offset;

    
    // sigmaR = sigma Range (for pixel intensities)
    // sigmaD = sigma Distance (for pixel locations)
    public FastBilateralFilter(int width, int height, float sigmaR, float sigmaD)
    {
       this(width, height, 0, width, sigmaR, sigmaD, 4, 3, 3);
    }

    
    // sigmaR = sigma Range (for pixel intensities)
    // sigmaD = sigma Distance (for pixel locations)
    public FastBilateralFilter(int width, int height, int offset, int stride,
            float sigmaR, float sigmaD)
    {
       this(width, height, offset, stride, sigmaR, sigmaD, 4, 3, 3);
    }


    // sigmaR = sigma Range (for pixel intensities)
    // sigmaD = sigma Distance (for pixel locations)
    public FastBilateralFilter(int width, int height, int offset, int stride,
            float sigmaR, float sigmaD, int rangeSampling, int downSampling, int channels)
    {
        if (height < 8)
            throw new IllegalArgumentException("The height must be at least 8");

        if (width < 8)
            throw new IllegalArgumentException("The width must be at least 8");

        if (offset < 0)
            throw new IllegalArgumentException("The offset must be at least 0");

        if (stride < 8)
            throw new IllegalArgumentException("The stride must be at least 8");

        if ((downSampling < 0) || (downSampling > 3))
            throw new IllegalArgumentException("The down sampling factor must be in [0..3]");

        if ((sigmaR < 1) && (sigmaR > 32))
            throw new IllegalArgumentException("The range sigma must be in [1..32]");

        if ((sigmaD < 1) && (sigmaD > 32))
            throw new IllegalArgumentException("The distance sigma must be in [1..32]");

        if ((channels < 1) || (channels > 3))
            throw new IllegalArgumentException("The number of image channels must be in [1..3]");

        this.height = height;
        this.width = width;
        this.offset = offset;
        this.stride = stride;
        int scaledH = this.height >> downSampling;
        int scaledW = this.width >> downSampling;
        this.box = new float[scaledW*scaledH];
        this.jk = new float[][] { new float[scaledW*scaledH], new float[scaledW*scaledH] };
        this.wk = new float[scaledW*scaledH];
        this.channels = channels;
        this.downSampling = downSampling;
        this.grayscale = new float[rangeSampling];
        this.radius = (int) ((2 * sigmaD * Math.min(scaledW, scaledH) + 1) / 2);
        this.buffer1 = new int[scaledH*scaledW];
        this.buffer2 = new int[scaledH*scaledW];
        this.colors = new int[256];

        for (int i=0; i<this.colors.length; i++)
          this.colors[i] = (int) (256f * Math.exp(-(float)(i*i)/(2*sigmaR*sigmaR)));
    }


    @Override
    public int[] apply(int[] src, int[] dst)
    {
        // Some aliasing ...
        final int[] buf1 = this.buffer1;
        final float[] wk_ = this.wk;
        final int ds = this.downSampling;
        final int scaledH = this.height >> ds;
        final int scaledW = this.width >> ds;
        int[] buf2 = src;

        if (ds > 0)
        {
           buf2 = this.buffer2;
           DecimateDownSampler sampler = new DecimateDownSampler(this.width,
                   this.height, this.stride, this.offset, 1<<ds);
           sampler.subSample(src, buf2);
        }
        else if ((this.offset != 0) || (this.stride != this.width))
        {
           buf2 = this.buffer2;
           int iOffs = this.offset;
           int oOffs = 0;

           for (int y=this.height; y>0; y--)
           {
              System.arraycopy(src, iOffs, buf2, oOffs, this.width);
              iOffs += this.stride;
              oOffs += this.width;
           }
        }

        for (int channel=0; channel<this.channels; channel++)
        {
           final int shift = channel << 3;
           int min = 255;
           int max = 0;

           // Extract channel and min,max for this channel
           for (int i=0; i<buf1.length; i++)
           {
              final int val = (buf2[i] >> shift) & 0xFF;
              max = max - (((max - val) >> 31) & (max - val));
              min = min + (((val - min) >> 31) & (val - min));
              buf1[i] = val;
           }

           final int maxGrayIdx = this.grayscale.length - 1;
           this.grayscale[0] = (float) min;
           this.grayscale[maxGrayIdx] = (float) max;
           final float delta = (float) (max - min);

           // Create scale of gray tones
           for (int i=1; i<maxGrayIdx;i++)
              this.grayscale[i] = (float) min + (i * (delta / maxGrayIdx));

           int jk_idx0 = 0;
           int jk_idx1 = 1;
           float[] jk_ = this.jk[0];
           final float shift_inv = 1.0f / (1 << ds);
           final float delta_scale = (float) maxGrayIdx / delta;

           // For each gray level
           for (int grayRangeIdx=0; grayRangeIdx<=maxGrayIdx; grayRangeIdx++)
           {
             int offs = 0;
             final float gray = this.grayscale[grayRangeIdx];

             // Compute Principle Bilateral Filtered Image Component Jk (and Wk)
             for (int y=0; y<scaledH; y++)
             {
               final int end = offs + scaledW;

               for (int x=offs; x<end; x++)
               {
                   final int val = buf1[x] & 0xFF;
                   final int colorIdx = (int) (Math.abs(gray-val)+0.5f);
                   final int color = this.colors[colorIdx];
                   jk_[x] = color * val;
                   wk_[x] = color;
               }

               offs += scaledW;
             }

             gaussianRecursive(jk_, this.box, scaledW, scaledH, this.radius);
             gaussianRecursive(wk_, this.box, scaledW, scaledH, this.radius);
             final int scaledSize = scaledW * scaledH;
             final float maxW = (float) (scaledW - 2);
             final float maxH = (float) (scaledH - 2);

             for (int n=0; n<scaledSize; n++)
                jk_[n] /= wk_[n];

             if (grayRangeIdx != 0)
             {
                offs = this.offset;

                // Calculate the bilateral filtering value by linear interpolation of Jk and Jk+1
                for (int y=0; y<this.height; y++)
                {
                   float ys = Math.min(((float) y)*shift_inv, maxH);

                   for (int x=0; x<this.width; x++)
                   {
                      final float kf = (((float)((src[offs+x] >> shift) &0xFF) - (float) min) * delta_scale);
                      final int k = (int) kf;

                      if (k == (grayRangeIdx-1))
                      {
                          final float alpha = (float) (k+1) - kf;
                          final float xs = Math.min(((float) x)*shift_inv, maxW);
                          final int val = (int) interpolateLinearXY2(this.jk[jk_idx0], this.jk[jk_idx1], alpha, xs, ys, scaledW);
                          dst[offs+x] &= ~(0xFF << shift); //src can be the same buffer as dst
                          dst[offs+x] |= ((val & 0xFF) << shift);
                      }
                      else if ((k == grayRangeIdx) && (grayRangeIdx == maxGrayIdx))
                      {
                          final float xs = Math.min(((float) x)*shift_inv, maxW);
                          final int val = (int) (interpolateLinearXY(this.jk[jk_idx1], xs , ys, scaledW) + 0.5f);
                          dst[offs+x] &= ~(0xFF << shift); //src can be the same buffer as dst
                          dst[offs+x] |= ((val & 0xFF) << shift);
                      }
                   }

                   offs += this.stride;
                }

                jk_idx1 = jk_idx0;
                jk_idx0 = 1 - jk_idx1;
              }

              jk_= this.jk[jk_idx1];
           }
        }

        return dst;
    }


    private static void gaussianRecursiveX(float[] od, float[] id, int w, int h,
            float a0, float a1, float a2, float a3, float b1, float b2,
            float coefp, float coefn)
    {
       int offs = 0;

       for (int y=0; y<h; y++)
       {
          // forward pass
	  float xp = id[offs];
          float yb = coefp * xp;
          float yp = yb;

          for (int x=0; x<w; x++)
          {
             float xc = id[offs+x];
             float yc = a0*xc + a1*xp - b1*yp - b2*yb;
             od[offs+x] = yc;
             xp = xc;
             yb = yp;
             yp = yc;
          }

          // reverse pass: ensure response is symmetrical
          float xn = id[offs+w-1];
          float xa = xn;
          float yn = coefn * xn;
          float ya = yn;

          for (int x=w-1; x>=0; x--)
          {
             float xc = id[offs+x];
             float yc = a2*xn + a3*xa - b1*yn - b2*ya;
             od[offs+x] += yc;
             xa = xn;
             xn = xc;
             ya = yn;
             yn = yc;
          }

          offs += w;
       }
    }


    private static void gaussianRecursiveY(float[] od, float[] id, int w, int h,
          float a0, float a1, float a2, float a3, float b1, float b2,
          float coefp, float coefn)
    {
       for (int x=0; x<w; x++)
       {
          // forward pass
          int offs = 0;
	  float xp = id[x];
          float yb = coefp * xp;
          float yp = yb;

          for (int y=0; y<h; y++)
	  {
	     float xc = id[offs+x];
	     float yc = a0*xc + a1*xp - b1*yp - b2*yb;
	     od[offs+x] = yc;
	     xp = xc;
             yb = yp;
             yp = yc;
             offs += w;
          }
          
          // reverse pass: ensure response is symmetrical
          offs = (h-1) * w;
          float xn = id[offs+x];
          float xa = xn;
          float yn = coefn * xn;
          float ya = yn;

          for (int y=h-1; y>=0; y--)
          {
	     float xc = id[offs+x];
	     float yc = a2*xn + a3*xa - b1*yn - b2*ya;
	     od[offs+x] += yc;
	     xa = xn;
             xn = xc;
             ya = yn;
             yn = yc;
             offs -= w;
	  }
       }
    }


    private static void gaussianRecursive(float[] image, float[] temp, int w, int h, float sigma)
    {
       final float nsigma = (sigma < 0.1f) ? 0.1f : sigma;
       final float alpha = 1.695f / nsigma;
       final float ema = (float) Math.exp(-alpha);
       final float ema2 = (float) Math.exp(-2*alpha);
       final float b1 = -2*ema;
       final float b2 = ema2;
       final float k = (1-ema)*(1-ema)/(1+2*alpha*ema-ema2);
       final float a0 = k;
       final float a1 = k*(alpha-1)*ema;
       final float a2 = k*(alpha+1)*ema;
       final float a3 = -k*ema2;
       final float coefp = (a0+a1) / (1+b1+b2);
       final float coefn = (a2+a3) / (1+b1+b2);
       gaussianRecursiveX(temp, image, w, h, a0, a1, a2, a3, b1, b2, coefp, coefn);
       gaussianRecursiveY(image, temp, w, h, a0, a1, a2, a3, b1, b2, coefp, coefn);
    }


    private static float interpolateLinearXY(float[] image, float x, float y, int w)
    {
	final int x0 = (int) x;
        final int xt = x0 + 1;
        final int y0 = (int) y;
        final int yt = y0 + 1;
	final float dx  = x - x0;
        final float dy  = y - y0;
        final float dx1 = 1 - dx;
        final float dy1 = 1 - dy;
        final float d00 = dx1 * dy1;
        final float d0t = dx * dy1;
        final float dt0 = dx1 * dy;
        final float dtt = dx * dy;
        final int offs0 = y0 * w;
        final int offst = yt * w;
	return ((d00*image[offs0+x0]) + (d0t*image[offs0+xt]) +
               (dt0*image[offst+x0]) + (dtt*image[offst+xt]));
    }

    
    private static float interpolateLinearXY2(float[] image1, float[] image2, float alpha, float x, float y, int w)
    {
	final int x0 = (int) x;
        final int xt = x0 + 1;
        final int y0 = (int) y;
        final int yt = y0 + 1;
	final float dx  = x - x0;
        final float dy  = y - y0;
        final float dx1 = 1 - dx;
        final float dy1 = 1 - dy;
        final float d00 = dx1 * dy1;
        final float d0t = dx * dy1;
        final float dt0 = dx1 * dy;
        final float dtt = dx * dy;
        final int offs0 = y0 * w;
        final int offst = yt * w;
	float res1 = ((d00*image1[offs0+x0]) + (d0t*image1[offs0+xt]) +
               (dt0*image1[offst+x0]) + (dtt*image1[offst+xt]));
	float res2 = ((d00*image2[offs0+x0]) + (d0t*image2[offs0+xt]) +
               (dt0*image2[offst+x0]) + (dtt*image2[offst+xt]));
        return alpha * res1 + (1.0f-alpha) * res2;
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