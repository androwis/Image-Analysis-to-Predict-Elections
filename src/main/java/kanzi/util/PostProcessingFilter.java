package kanzi.util;


// Utility class to post process the frame after inverse transform and dequantization
// The filter performs deblocking of the frame data
public class PostProcessingFilter
{
   private final int width;
   private final int height;
   private final int stride;
   private final int mask;

   private static final int RGB_MASK = 0x000000FF;
   private static final int YCC_MASK = 0xFFFFFFFF;
   private static final int THRESHOLD_DEBLOCKING = 16;


   public PostProcessingFilter(int width, int height, int stride, boolean isRGB)
   {
     if (height < 8)
         throw new IllegalArgumentException("The height must be at least 8");

     if (width < 8)
         throw new IllegalArgumentException("The width must be at least 8");

     if (stride < 8)
         throw new IllegalArgumentException("The stride must be at least 8");

     if (stride > width)
         throw new IllegalArgumentException("The stride must be at most " + width);

     if ((height & 7) != 0)
         throw new IllegalArgumentException("The height must be a multiple of 8");

     if ((width & 7) != 0)
         throw new IllegalArgumentException("The width must be a multiple of 8");

     this.height = height;
     this.width = width;
     this.stride = stride;
     this.mask = (isRGB == true) ? RGB_MASK : YCC_MASK;
   }


   public void apply(int[] data, int blkptr, int x, int y, int blockDim /*, int quantizer*/)
   {
      if (x < 4)
         return;

      for (int i=0; i<blockDim*blockDim; i+=64)
      {
         final int xx = x + ((i >> 6) & 1);
         final int yy = y + (i >> 7);

         if (xx < this.width)
         {
           this.deblock8x8_H(data, blkptr, xx, yy);
           this.deblock8x8_V(data, blkptr, xx, yy);
         }
      }
   }


   private void deblock8x8_V(int[] data, int blkptr, int x, int y)
   {
      // Upper block: coefficients ai                 a34   a44
      //                                              ...   ...
      // Lower block: coefficients bi                 a37   a47
      //                                  ---------------------------------
      // Left block: coefficients xi                  x70 | y00
      //                                              ... | ...
      // Right block: coefficients yi                 x77 | y07
      //                                  ---------------------------------
      //                                              b30   b40
      //                                              ...   ...
      final int dim = 8;
      final int halfDim = dim >> 1;
      int offs1 = blkptr + (y * this.stride) + x;
      int offs2 = (y > 0) ? offs1 - (halfDim * this.stride) : offs1;
      int offs3 = blkptr + ((y + dim - 1) * this.stride) + x;
      int offs4 = (y < this.height - dim) ? offs3 + (halfDim * this.stride) : offs3;
      int delta_a = 0;
      int delta_b = 0;
      int adjust = 0;

      for (int i=0; i<halfDim; i++)
      {
         delta_a += ((data[offs1+adjust] & this.mask) - (data[offs1+adjust-1] & this.mask));
         delta_a += ((data[offs2+adjust] & this.mask) - (data[offs2+adjust-1] & this.mask));
         delta_b += ((data[offs3-adjust] & this.mask) - (data[offs3-adjust-1] & this.mask));
         delta_b += ((data[offs4-adjust] & this.mask) - (data[offs4-adjust-1] & this.mask));
         adjust += this.stride;
      }

      delta_a /= dim;
      delta_b /= dim;
      final int abs_delta_a = (delta_a >= 0) ? delta_a : -delta_a;
      final int abs_delta_b = (delta_b >= 0) ? delta_b : -delta_b;
      adjust = 0;

      if ((abs_delta_a > 1) && (abs_delta_a < THRESHOLD_DEBLOCKING))
      {
        delta_a >>= 1;

        if ((abs_delta_b > 1) && (abs_delta_b < THRESHOLD_DEBLOCKING))
        {
          delta_b >>= 1;

          for (int i=0; i<halfDim; i++)
          {
             int val1, val2;
             val1 = data[offs1+adjust]   & this.mask;
             val2 = data[offs1+adjust-1] & this.mask;
             data[offs1+adjust]   = (val1 + val2) >> 1;
             data[offs1+adjust-1] = (val1 + val2) >> 1;
             val1 = data[offs2+adjust]   & this.mask;
             val2 = data[offs2+adjust-1] & this.mask;
             data[offs2+adjust]   = (val1 + val2) >> 1;
             data[offs2+adjust-1] = (val1 + val2) >> 1;
             val1 = data[offs3-adjust]   & this.mask;
             val2 = data[offs3-adjust-1] & this.mask;
             data[offs3-adjust]   = (val1 + val2) >> 1;
             data[offs3-adjust-1] = (val1 + val2) >> 1;
//             data[offs1+adjust]   += delta_a;
//             data[offs1+adjust-1] += delta_a;
//             data[offs1+adjust+1] += delta_a;
//             data[offs1+adjust-2] += delta_a;
//             data[offs2+adjust]   -= delta_a;
//             data[offs2+adjust-1] += delta_a;
//             data[offs2+adjust+1] += delta_a;
//             data[offs2+adjust-2] += delta_a;
//             data[offs3-adjust]   -= delta_b;
//             data[offs3-adjust-1] += delta_b;
//             data[offs3-adjust+1] += delta_b;
//             data[offs3-adjust-2] += delta_b;
//             data[offs4-adjust]   -= delta_b;
//             data[offs4-adjust-1] += delta_b;
//             data[offs4-adjust+1] += delta_b;
//             data[offs4-adjust-2] += delta_b;
             adjust += this.stride;
          }

          // Adjust y34<->y44 and x34<->x44
          adjust = (halfDim - 1) * this.stride;
          final int y34 = data[offs1+adjust]   & this.mask;
          final int x34 = data[offs1+adjust-1] & this.mask;
          final int y44 = data[offs3-adjust]   & this.mask;
          final int x44 = data[offs3-adjust-1] & this.mask;
          data[offs1+adjust]   += (y44 - y34) >> 1;
          data[offs1+adjust-1] += (x44 - x34) >> 1;
          data[offs3-adjust]   -= (y44 - y34) >> 1;
          data[offs3-adjust-1] -= (x44 - x34) >> 1;
        }
        else
        {
          for (int i=0; i<halfDim; i++)
          {
             data[offs1+adjust]   -= delta_a;
             data[offs1+adjust-1] += delta_a;
             data[offs2+adjust]   -= delta_a;
             data[offs2+adjust-1] += delta_a;
             adjust += this.stride;
          }
        }
      }
      else if ((abs_delta_b > 1) && (abs_delta_b < THRESHOLD_DEBLOCKING))
      {
          delta_b >>= 1;
          
          for (int i=0; i<halfDim; i++)
          {
             data[offs3-adjust]   -= delta_b;
             data[offs3-adjust-1] += delta_b;
             data[offs4-adjust]   -= delta_b;
             data[offs4-adjust-1] += delta_b;
             adjust += this.stride;
          }
      }
   }


   private void deblock8x8_H(int[] data, int blkptr, int x, int y)
   {
      // Upper block: coefficients ai     a07 a17 a27 a37   a47 a57 a67 a77
      //                                  ---------------------------------
      // Lower block: coefficients bi     x40 x50 x60 x70 | y00 y10 y20 y30
      // Left block: coefficients xi           ...        |       ...
      // Right block: coefficients yi     x47 x57 x67 x77 | y07 y17 y27 y37
      //                                  ---------------------------------
      //                                  b07 b17 b27 b37   b47 b57 b67 b77
      final int dim = 8;
      final int halfDim = dim >> 1;
      int offs1 = blkptr + (y * this.stride) + x;
      int offs2 = (y > 0) ? offs1 - this.stride : offs1;
      int offs3 = blkptr + ((y + dim - 1)* this.stride) + x;
      int offs4 = (y < this.height - dim) ? offs3 + this.stride : offs3;
      int delta_ax = 0;
      int delta_bx = 0;

      for (int i=0; i<halfDim; i++)
      {
         final int j = i - halfDim;
         delta_ax += ((data[offs1+i] & this.mask) - (data[offs2+i] & this.mask));
         delta_ax += ((data[offs1+j] & this.mask) - (data[offs2+j] & this.mask));
         delta_bx += ((data[offs3+i] & this.mask) - (data[offs4+i] & this.mask));
         delta_bx += ((data[offs3+j] & this.mask) - (data[offs4+j] & this.mask));
      }

      delta_ax /= dim;
      delta_bx /= dim;
      int abs_delta_ax = (delta_ax >= 0) ? delta_ax : -delta_ax;
      int abs_delta_bx = (delta_bx >= 0) ? delta_bx : -delta_bx;

      if ((abs_delta_ax > 1) && (abs_delta_ax < THRESHOLD_DEBLOCKING))
      {
         delta_ax >>= 1;

         if ((abs_delta_bx > 1) && (abs_delta_bx < THRESHOLD_DEBLOCKING))
         {
            delta_bx >>= 1;

            for (int i=0; i<halfDim; i++)
            {
               data[offs1+i] -= delta_ax;
               data[offs2+i] += delta_ax;
               data[offs1+i-halfDim] -= delta_ax;
               data[offs2+i-halfDim] += delta_ax;
               data[offs3+i] -= delta_bx;
               data[offs4+i] += delta_bx;
               data[offs3+i-halfDim] -= delta_bx;
               data[offs4+i-halfDim] += delta_bx;
            }
         }
         else
         {
            for (int i=0; i<halfDim; i++)
            {
               data[offs1+i] -= delta_ax;
               data[offs2+i] += delta_ax;
               data[offs1+i-halfDim] -= delta_ax;
               data[offs2+i-halfDim] += delta_ax;
            }
         }
      }
      else if ((abs_delta_bx > 1) && (abs_delta_bx < THRESHOLD_DEBLOCKING))
      {
         delta_bx >>= 1;

         for (int i=0; i<halfDim; i++)
         {
            data[offs3+i] -= delta_bx;
            data[offs4+i] += delta_bx;
            data[offs3+i-halfDim] -= delta_bx;
            data[offs4+i-halfDim] += delta_bx;
         }
      }
   }


   // Use to generate a missing half block (4x8 or 4x16) at the beginning or end
   // of a row during decompression of a frame. It happens if the encoder alternates
   // the first position in each row (x=0 or x=4) to use the shifted block data
   // during vertical deblocking.
   // The half block is re-created by interpolation of pixels in adjacent blocks
   public final void interpolateHalfBlock(int[] data, int blkptr, int x, int y, int blockDim)
   {
      int inc, start;

      if (x == 0)
      {
         // First half-block in row, point to start of next block
         start = 4;
         inc = -1;
      }
      else
      {
         // Last half-block in row, point to end of previous block
         start = - 1;
         inc = 1;
      }

      // a0, ... a3 are the pixels just above the first line of the current half block
      // b0, ... b3 are the pixels just below the last line of the current half block
      int a0, a1, a2, a3, b0, b1, b2, b3;

      if (y == 0) // first line
      {
         int offs = blkptr + ((y + blockDim) * this.stride) + x + start;
         offs += inc;
         b3 = data[offs] & this.mask;
         offs += inc;
         b2 = data[offs] & this.mask;
         offs += inc;
         b1 = data[offs] & this.mask;
         offs += inc;
         b0 = data[offs] & this.mask;
         a0 = b0;
         a1 = b1;
         a2 = b2;
         a3 = b3;
      }
      else if (y == this.height - blockDim) // last line
      {
         int offs = blkptr + ((y - 1) * this.stride) + x + start;
         offs += inc;
         a3 = data[offs] & this.mask;
         offs += inc;
         a2 = data[offs] & this.mask;
         offs += inc;
         a1 = data[offs] & this.mask;
         offs += inc;
         a0 = data[offs] & this.mask;
         b0 = a0;
         b1 = a1;
         b2 = a2;
         b3 = a3;
      }
      else
      {
         int offs1 = blkptr + ((y - 1) * this.stride) + x + start;
         offs1 += inc;
         a3 = data[offs1] & this.mask;
         offs1 += inc;
         a2 = data[offs1] & this.mask;
         offs1 += inc;
         a1 = data[offs1] & this.mask;
         offs1 += inc;
         a0 = data[offs1] & this.mask;
         int offs2 = blkptr + ((y + blockDim) * this.stride) + x + start;
         b3 = data[offs2] & this.mask;
         offs2 += inc;
         b2 = data[offs2] & this.mask;
         offs2 += inc;
         b1 = data[offs2] & this.mask;
         offs2 += inc;
         b0 = data[offs2] & this.mask;
      }

      // Point to beginning/end of next/previous block
      int offs1 = blkptr + (y * this.stride) + x + start;
      int offs2 = blkptr + ((y + blockDim - 1) * this.stride) + x + start;
      final int half = blockDim >> 1;
      final int lineInc = this.stride - (inc << 2);
      final int lineDec = this.stride + (inc << 2);

      for (int j=0; j<half; j++)
      {
        final int val1 = data[offs1] & this.mask;
        final int val2 = data[offs2] & this.mask;
        offs1 += inc;
        offs2 += inc;
        a3 = (val1 + a3) >> 1;
        data[offs1] = a3;
        offs1 += inc;
        a2 = (a3 + a2) >> 1;
        data[offs1] = a2;
        offs1 += inc;
        a1 = (a2 + a1) >> 1;
        data[offs1] = a1;
        offs1 += inc;
        a0 = (a1 + a0) >> 1;
        data[offs1] = a0;

        b3 = (val2 + b3) >> 1;
        data[offs2] = b3;
        offs2 += inc;
        b2 = (b3 + b2) >> 1;
        data[offs2] = b2;
        offs2 += inc;
        b1 = (b2 + b1) >> 1;
        data[offs2] = b1;
        offs2 += inc;
        b0 = (b1 + b0) >> 1;
        data[offs2] = b0;

        // Moving down to next line
        offs1 += lineInc;

        // Moving up to next line
        offs2 -= lineDec;
      }
   }
}
