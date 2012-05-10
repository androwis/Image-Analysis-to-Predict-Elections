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

package kanzi.util;

// LONG : empty:7 mode left:3 - err left:16 - mode right:3 - err right:16 - mode indep:3 - err indep:16
// 000 DC L
// 001 DC R
// 010 average L
// 011 average R
// 100 horizontal L
// 101 horizontal R
// 110 vertical
// 111 temporal

// xx0 => left
// 11x => not horizontally dependent

public class IntraPredictor
{
   public enum Mode
   {
      TEMPORAL(0),     // Temporal
      VERTICAL(1),     // Vertical
      HORIZONTAL_L(2), // Horizontal left
      HORIZONTAL_R(3), // Horizontal right
      AVERAGE_UL(4),   // Average upper-left
      DC(5),           // DC
      ORIGIN_BR(6),    // origin bottom right
      ORIGIN_UL(7);    // origin upper left

      // 3 bit value
      private final int value;

      Mode(int value)
      {
         this.value = value;
      }
   };

   private static final int MAX_VALUE_8x8   = (1 << 14) - 1;
   private static final int MAX_VALUE_16x16 = (1 << 16) - 1;

   private final int width;
   private final int height;
   private final int stride;
   private final int dim;


   public IntraPredictor(int dim, int width, int height)
   {
      this(dim, width, height, width);
   }


   public IntraPredictor(int dim, int width, int height, int stride)
   {
     if ((dim != 8) && (dim != 16))
         throw new IllegalArgumentException("The dimension must be 8 or 16");

     if (height < 8)
         throw new IllegalArgumentException("The height must be at least 8");

     if (width < 8)
         throw new IllegalArgumentException("The width must be at least 8");

     if (stride < 8)
         throw new IllegalArgumentException("The stride must be at least 8");

     if ((height & 7) != 0)
         throw new IllegalArgumentException("The height must be a multiple of 8");

     if ((width & 7) != 0)
         throw new IllegalArgumentException("The width must be a multiple of 8");

     if ((stride & 7) != 0)
         throw new IllegalArgumentException("The stride must be a multiple of 8");

     this.height = height;
     this.width = width;
     this.stride = stride;
     this.dim = dim;
   }


   // Return a packed integer or -1
   // [best_mode_left 3bits][best_error_left 13bits][best_mode_right 3bits][best_error_right 13bits]
   // If a right block dependent is possible and better, then use it
   // Otherwise, use left block dependent mode
   // Proceed line by line (for cache) and avoid branches (for speed)
   // Example 8x8
   //   o   a0 a1 a2 a3 a4 a5 a6 a7
   //  b0   x0 x1 x2 x3 x4 x5 x6 x7   c0
   //  b1   x0 x1 x2 x3 x4 x5 x6 x7   c1
   //  b2   x0 x1 x2 x3 x4 x5 x6 x7   c2
   //  b3   x0 x1 x2 x3 x4 x5 x6 x7   c3
   //  b4   x0 x1 x2 x3 x4 x5 x6 x7   c4
   //  b5   x0 x1 x2 x3 x4 x5 x6 x7   c5
   //  b6   x0 x1 x2 x3 x4 x5 x6 x7   c6
   //  b7   x0 x1 x2 x3 x4 x5 x6 x7 p=c7
   // Another block (usually temporal) can be provided optionally
   public int predict(int[] src, int x, int y, int[] other, int xx, int yy)
   {
      int w_max = this.width - this.dim;
      int h_max = this.height - this.dim;

      if ((x > w_max) || (y > h_max))
         return -1;

      if (other != null)
      {
         if ((xx < 0) || (yy < 0) || (x > w_max) || (y > h_max))
            other = null;
      }

      if (this.dim == 8)
         return this.predict8x8(src, x, y, other, xx, yy);

      return this.predict16x16(src, x, y, other, xx, yy);
   }


   // Check left and upper blocks
   private int predict8x8(int[] src, int x, int y, int[] other, int xx, int yy)
   {
      int st = this.stride;
      int offset1 = (y * st) + x;
      int offset2 = 0;
      int dc = 0;

      if (other != null)
         offset2 = (yy * st) + xx;

      int a0, a1, a2, a3, a4, a5, a6, a7;
      int res0, res1, res2, res3, res4, res5, res6, res7;
      res0 = res1 = res2 = res3 = res4 = res5 = res6 = res7 = 0;
      int w_8 = this.width - 8;

      // abs(x) = ((x + (x >> 31)) ^ (x >> 31));
      if (x < w_8)
      {
         // Get the value of pixel after the last block pixel
         int p = src[offset1+(st<<3)-st+8] & 0xFF;
         res6 -= (p << 6);
      }

      if (y > 0)
      {
         // Row above block
         int offs = offset1 - st;
         a0 = src[offs]   & 0xFF;
         a1 = src[offs+1] & 0xFF;
         a2 = src[offs+2] & 0xFF;
         a3 = src[offs+3] & 0xFF;
         a4 = src[offs+4] & 0xFF;
         a5 = src[offs+5] & 0xFF;
         a6 = src[offs+6] & 0xFF;
         a7 = src[offs+7] & 0xFF;
         dc += (a0 + a1 + a2 + a3 + a4 + a5 + a6 + a7);

         // For 'origin upper left' mode, remove (o = abs(src[x-1,y-1])) * 64
         int o = src[offs-1] & 0xFF;
         res7 -= (o << 6);
      }
      else
      {
         a0 = a1 = a2 = a3 = a4 = a5 = a6 = a7 = 0;
      }

      int px0 = a0;
      int px1 = a1;
      int px2 = a2;
      int px3 = a3;
      int px4 = a4;
      int px5 = a5;
      int px6 = a6;
      int px7 = a7;

      for (int j=0; j<8; j++)
      {
         int x0 = src[offset1]   & 0xFF;
         int x1 = src[offset1+1] & 0xFF;
         int x2 = src[offset1+2] & 0xFF;
         int x3 = src[offset1+3] & 0xFF;
         int x4 = src[offset1+4] & 0xFF;
         int x5 = src[offset1+5] & 0xFF;
         int x6 = src[offset1+6] & 0xFF;
         int x7 = src[offset1+7] & 0xFF;

         if (other != null)
         {
            int xy0 = (other[offset2]   & 0xFF) - x0;
            int xy1 = (other[offset2+1] & 0xFF) - x1;
            int xy2 = (other[offset2+2] & 0xFF) - x2;
            int xy3 = (other[offset2+3] & 0xFF) - x3;
            int xy4 = (other[offset2+4] & 0xFF) - x4;
            int xy5 = (other[offset2+5] & 0xFF) - x5;
            int xy6 = (other[offset2+6] & 0xFF) - x6;
            int xy7 = (other[offset2+7] & 0xFF) - x7;
            offset2 += st;

            // Temporal: abs(y-x)
            res0 += ((xy0 + ((xy0) >> 31)) ^ ((xy0) >> 31));
            res0 += ((xy1 + ((xy1) >> 31)) ^ ((xy1) >> 31));
            res0 += ((xy2 + ((xy2) >> 31)) ^ ((xy2) >> 31));
            res0 += ((xy3 + ((xy3) >> 31)) ^ ((xy3) >> 31));
            res0 += ((xy4 + ((xy4) >> 31)) ^ ((xy4) >> 31));
            res0 += ((xy5 + ((xy5) >> 31)) ^ ((xy5) >> 31));
            res0 += ((xy6 + ((xy6) >> 31)) ^ ((xy6) >> 31));
            res0 += ((xy7 + ((xy7) >> 31)) ^ ((xy7) >> 31));
         }

         if (x > 0)
         {
            // Horizontal left: abs(b-x)
            int b = src[offset1-1] & 0xFF;
            res2 += ((b - x0 + ((b-x0) >> 31)) ^ ((b-x0) >> 31));
            res2 += ((b - x1 + ((b-x1) >> 31)) ^ ((b-x1) >> 31));
            res2 += ((b - x2 + ((b-x2) >> 31)) ^ ((b-x2) >> 31));
            res2 += ((b - x3 + ((b-x3) >> 31)) ^ ((b-x3) >> 31));
            res2 += ((b - x4 + ((b-x4) >> 31)) ^ ((b-x4) >> 31));
            res2 += ((b - x5 + ((b-x5) >> 31)) ^ ((b-x5) >> 31));
            res2 += ((b - x6 + ((b-x6) >> 31)) ^ ((b-x6) >> 31));
            res2 += ((b - x7 + ((b-x7) >> 31)) ^ ((b-x7) >> 31));

            if (y > 0)
            {
               // DC: abs(x) - 64*avg([a0..a7,b0..b7])
               res5 += (x0 + x1 + x2 + x3 + x4 + x5 + x6 + x7);
               dc += b;

               // Average up-left: abs(avg((X,Y-1),(X-1,Y))-(X,Y))
               int avg;
               avg = (b + px0) >> 1;
               res4 += ((avg - x0 + ((avg-x0) >> 31)) ^ ((avg-x0) >> 31));
               avg = (x0 + px1) >> 1;
               res4 += ((avg - x1 + ((avg-x1) >> 31)) ^ ((avg-x1) >> 31));
               avg = (x1 + px2) >> 1;
               res4 += ((avg - x2 + ((avg-x2) >> 31)) ^ ((avg-x2) >> 31));
               avg = (x2 + px3) >> 1;
               res4 += ((avg - x3 + ((avg-x3) >> 31)) ^ ((avg-x3) >> 31));
               avg = (x3 + px4) >> 1;
               res4 += ((avg - x4 + ((avg-x4) >> 31)) ^ ((avg-x4) >> 31));
               avg = (x4 + px5) >> 1;
               res4 += ((avg - x5 + ((avg-x5) >> 31)) ^ ((avg-x5) >> 31));
               avg = (x5 + px6) >> 1;
               res4 += ((avg - x6 + ((avg-x6) >> 31)) ^ ((avg-x6) >> 31));
               avg = (x6 + px7) >> 1;
               res4 += ((avg - x7 + ((avg-x7) >> 31)) ^ ((avg-x7) >> 31));
               px0 = x0;
               px1 = x1;
               px2 = x2;
               px3 = x3;
               px4 = x4;
               px5 = x5;
               px6 = x6;
            }
         }

         if (x < w_8)
         {
            // Horizontal right abs(c-x)
            int c = src[offset1+8] & 0xFF;
            res3 += ((c - x0 + ((c-x0) >> 31)) ^ ((c-x0) >> 31));
            res3 += ((c - x1 + ((c-x1) >> 31)) ^ ((c-x1) >> 31));
            res3 += ((c - x2 + ((c-x2) >> 31)) ^ ((c-x2) >> 31));
            res3 += ((c - x3 + ((c-x3) >> 31)) ^ ((c-x3) >> 31));
            res3 += ((c - x4 + ((c-x4) >> 31)) ^ ((c-x4) >> 31));
            res3 += ((c - x5 + ((c-x5) >> 31)) ^ ((c-x5) >> 31));
            res3 += ((c - x6 + ((c-x6) >> 31)) ^ ((c-x6) >> 31));
            res3 += ((c - x7 + ((c-x7) >> 31)) ^ ((c-x7) >> 31));
         }

         if (y > 0)
         {
            // Vertical abs(a-x)
            res1 += ((a0 - x0 + ((a0-x0) >> 31)) ^ ((a0-x0) >> 31));
            res1 += ((a1 - x1 + ((a1-x1) >> 31)) ^ ((a1-x1) >> 31));
            res1 += ((a2 - x2 + ((a2-x2) >> 31)) ^ ((a2-x2) >> 31));
            res1 += ((a3 - x3 + ((a3-x3) >> 31)) ^ ((a3-x3) >> 31));
            res1 += ((a4 - x4 + ((a4-x4) >> 31)) ^ ((a4-x4) >> 31));
            res1 += ((a5 - x5 + ((a5-x5) >> 31)) ^ ((a5-x5) >> 31));
            res1 += ((a6 - x6 + ((a6-x6) >> 31)) ^ ((a6-x6) >> 31));
            res1 += ((a7 - x7 + ((a7-x7) >> 31)) ^ ((a7-x7) >> 31));
         }

         offset1 += st;
      }

      if (x < w_8)
      {
         // Adjust "origin bottom-right" mode
         res6 += res5;

         if (res6 < 0)
            res6 = -res6;
      }
      else
      {
         res6 = MAX_VALUE_8x8; // HORIZONTAL_R
         res3 = MAX_VALUE_8x8; // ORIGIN_BR
      }

      if (y <= 0)
      {
         res1 = MAX_VALUE_8x8; // VERTICAL
         res4 = MAX_VALUE_8x8; // AVERAGE_UL
         res5 = MAX_VALUE_8x8; // DC
         res7 = MAX_VALUE_8x8; // ORIGIN_UL
      }
      else
      {
         // Adjust "origin up-left" mode
         res7 += res5;

         if (res7 < 0)
            res7 = -res7;

         // For DC, remove average value of 'a' row and 'b' column (16 * 4 = 8 * 8)
         res5 -= (dc << 2);

         if (res5 < 0)
            res5 = -res5;
      }

      if (x <= 0)
      {
         res2 = MAX_VALUE_8x8; // HORIZONTAL_R
         res4 = MAX_VALUE_8x8; // AVERAGE_UL
         res5 = MAX_VALUE_8x8; // DC
         res7 = MAX_VALUE_8x8; // ORIGIN_UL
      }

      if (other == null)
        res0 = MAX_VALUE_8x8;

      // Find minimum error for non-right dependent blocks
      int minL =  res1;
      Mode minModeL = Mode.VERTICAL;

      if (res0 < minL)
      {
         minL = res0;
         minModeL = Mode.TEMPORAL;
      }

      if (res2 < minL)
      {
         minL = res2;
         minModeL = Mode.HORIZONTAL_L;
      }

      if (res4 < minL)
      {
         minL = res4;
         minModeL = Mode.AVERAGE_UL;
      }

      if (res5 < minL)
      {
         minL = res5;
         minModeL = Mode.DC;
      }

      if (res7 < minL)
      {
         minL = res7;
         minModeL = Mode.ORIGIN_UL;
      }

      // Find minimum error fo right dependent blocks
      int minR = res1;
      Mode minModeR = Mode.VERTICAL;

      if (res3 < minR)
      {
         minR = res3;
         minModeR = Mode.HORIZONTAL_R;
      }

      if (res6 < minR)
      {
         minR = res6;
         minModeR = Mode.ORIGIN_BR;
      }

      if (res0 < minR)
      {
         minR = res0;
         minModeR = Mode.TEMPORAL;
      }

      // Return packed value:
      // 3 bits left mode - 13 bits error - 3 bits right mode - 13 bits error
      int res = (minModeL.value << 13) | (minL >> 3);
      res <<= 16;
      res |= (minModeR.value << 13);
      res |= (minR >> 3);

      return res;
   }


   // Check left and upper blocks
   private int predict16x16(int[] src, int x, int y, int[] other, int xx, int yy)
   {
      final int st = this.stride;
      int offset1 = (y * st) + x;
      int offset2 = 0;
      int dc = 0;

      if (other != null)
         offset2 = (yy * st) + xx;

      int a0, a1, a2, a3, a4, a5, a6, a7;
      int a8, a9, a10, a11, a12, a13, a14, a15;
      int res0, res1, res2, res3, res4, res5, res6, res7;
      res0 = res1 = res2 = res3 = res4 = res5 = res6 = res7 = 0;
      int w_16 = this.width - 16;

      // abs(x) = ((x + (x >> 31)) ^ (x >> 31));
      if (x < w_16)
      {
         // Get the value of the  pixel after the last block pixel
         int p = src[offset1+(st<<4)-st+16] & 0xFF;
         res6 -= (p << 8);
      }

      if (y > 0)
      {
         // Row above block
         int offs = offset1 - st;
         a0  = src[offs]    & 0xFF;
         a1  = src[offs+1]  & 0xFF;
         a2  = src[offs+2]  & 0xFF;
         a3  = src[offs+3]  & 0xFF;
         a4  = src[offs+4]  & 0xFF;
         a5  = src[offs+5]  & 0xFF;
         a6  = src[offs+6]  & 0xFF;
         a7  = src[offs+7]  & 0xFF;
         a8  = src[offs+8]  & 0xFF;
         a9  = src[offs+9]  & 0xFF;
         a10 = src[offs+10] & 0xFF;
         a11 = src[offs+11] & 0xFF;
         a12 = src[offs+12] & 0xFF;
         a13 = src[offs+13] & 0xFF;
         a14 = src[offs+14] & 0xFF;
         a15 = src[offs+15] & 0xFF;
         dc += (a0 + a1 + a2 + a3 + a4 + a5 + a6 + a7);
         dc += (a8 + a9 + a10 + a11 + a12 + a13 + a14 + a15);

         // For 'origin upper left' mode, remove (o = abs(src[x-1,y-1])) * 256
         int o = src[offs-1] & 0xFF;
         res7 -= (o << 8);
      }
      else
      {
         a0 = a1 = a2 = a3 = a4 = a5 = a6 = a7 = 0;
         a8 = a9 = a10 = a11 = a12 = a13 = a14 = a15 = 0;
      }

      int px0  = a0;
      int px1  = a1;
      int px2  = a2;
      int px3  = a3;
      int px4  = a4;
      int px5  = a5;
      int px6  = a6;
      int px7  = a7;
      int px8  = a8;
      int px9  = a9;
      int px10 = a10;
      int px11 = a11;
      int px12 = a12;
      int px13 = a13;
      int px14 = a14;
      int px15 = a15;

      for (int j=0; j<16; j++)
      {
         int x0  = src[offset1]    & 0xFF;
         int x1  = src[offset1+1]  & 0xFF;
         int x2  = src[offset1+2]  & 0xFF;
         int x3  = src[offset1+3]  & 0xFF;
         int x4  = src[offset1+4]  & 0xFF;
         int x5  = src[offset1+5]  & 0xFF;
         int x6  = src[offset1+6]  & 0xFF;
         int x7  = src[offset1+7]  & 0xFF;
         int x8  = src[offset1+8]  & 0xFF;
         int x9  = src[offset1+9]  & 0xFF;
         int x10 = src[offset1+10] & 0xFF;
         int x11 = src[offset1+11] & 0xFF;
         int x12 = src[offset1+12] & 0xFF;
         int x13 = src[offset1+13] & 0xFF;
         int x14 = src[offset1+14] & 0xFF;
         int x15 = src[offset1+15] & 0xFF;

         if (other != null)
         {
            int xy0  = (other[offset2]    & 0xFF) - x0;
            int xy1  = (other[offset2+1]  & 0xFF) - x1;
            int xy2  = (other[offset2+2]  & 0xFF) - x2;
            int xy3  = (other[offset2+3]  & 0xFF) - x3;
            int xy4  = (other[offset2+4]  & 0xFF) - x4;
            int xy5  = (other[offset2+5]  & 0xFF) - x5;
            int xy6  = (other[offset2+6]  & 0xFF) - x6;
            int xy7  = (other[offset2+7]  & 0xFF) - x7;
            int xy8  = (other[offset2+8]  & 0xFF) - x8;
            int xy9  = (other[offset2+9]  & 0xFF) - x9;
            int xy10 = (other[offset2+10] & 0xFF) - x10;
            int xy11 = (other[offset2+11] & 0xFF) - x11;
            int xy12 = (other[offset2+12] & 0xFF) - x12;
            int xy13 = (other[offset2+13] & 0xFF) - x13;
            int xy14 = (other[offset2+14] & 0xFF) - x14;
            int xy15 = (other[offset2+15] & 0xFF) - x15;
            offset2 += st;

            // Temporal: abs(y-x)
            res0 += ((xy0  + ((xy0)  >> 31)) ^ ((xy0)  >> 31));
            res0 += ((xy1  + ((xy1)  >> 31)) ^ ((xy1)  >> 31));
            res0 += ((xy2  + ((xy2)  >> 31)) ^ ((xy2)  >> 31));
            res0 += ((xy3  + ((xy3)  >> 31)) ^ ((xy3)  >> 31));
            res0 += ((xy4  + ((xy4)  >> 31)) ^ ((xy4)  >> 31));
            res0 += ((xy5  + ((xy5)  >> 31)) ^ ((xy5)  >> 31));
            res0 += ((xy6  + ((xy6)  >> 31)) ^ ((xy6)  >> 31));
            res0 += ((xy7  + ((xy7)  >> 31)) ^ ((xy7)  >> 31));
            res0 += ((xy8  + ((xy8)  >> 31)) ^ ((xy8)  >> 31));
            res0 += ((xy9  + ((xy9)  >> 31)) ^ ((xy9)  >> 31));
            res0 += ((xy10 + ((xy10) >> 31)) ^ ((xy10) >> 31));
            res0 += ((xy11 + ((xy11) >> 31)) ^ ((xy11) >> 31));
            res0 += ((xy12 + ((xy12) >> 31)) ^ ((xy12) >> 31));
            res0 += ((xy13 + ((xy13) >> 31)) ^ ((xy13) >> 31));
            res0 += ((xy14 + ((xy14) >> 31)) ^ ((xy14) >> 31));
            res0 += ((xy15 + ((xy15) >> 31)) ^ ((xy15) >> 31));
         }

         if (x > 0)
         {
            // Horizontal left: abs(b-x)
            int b = src[offset1-1] & 0xFF;
            res2 += ((b - x0  + ((b-x0)  >> 31)) ^ ((b-x0)  >> 31));
            res2 += ((b - x1  + ((b-x1)  >> 31)) ^ ((b-x1)  >> 31));
            res2 += ((b - x2  + ((b-x2)  >> 31)) ^ ((b-x2)  >> 31));
            res2 += ((b - x3  + ((b-x3)  >> 31)) ^ ((b-x3)  >> 31));
            res2 += ((b - x4  + ((b-x4)  >> 31)) ^ ((b-x4)  >> 31));
            res2 += ((b - x5  + ((b-x5)  >> 31)) ^ ((b-x5)  >> 31));
            res2 += ((b - x6  + ((b-x6)  >> 31)) ^ ((b-x6)  >> 31));
            res2 += ((b - x7  + ((b-x7)  >> 31)) ^ ((b-x7)  >> 31));
            res2 += ((b - x8  + ((b-x8)  >> 31)) ^ ((b-x8)  >> 31));
            res2 += ((b - x9  + ((b-x9)  >> 31)) ^ ((b-x9)  >> 31));
            res2 += ((b - x10 + ((b-x10) >> 31)) ^ ((b-x10) >> 31));
            res2 += ((b - x11 + ((b-x11) >> 31)) ^ ((b-x11) >> 31));
            res2 += ((b - x12 + ((b-x12) >> 31)) ^ ((b-x12) >> 31));
            res2 += ((b - x13 + ((b-x13) >> 31)) ^ ((b-x13) >> 31));
            res2 += ((b - x14 + ((b-x14) >> 31)) ^ ((b-x14) >> 31));
            res2 += ((b - x15 + ((b-x15) >> 31)) ^ ((b-x15) >> 31));

            if (y > 0)
            {
               // DC: abs(x) - 64*avg([a0..a15,b0..b15])
               res5 += (x0 + x1 + x2 + x3 + x4 + x5 + x6 + x7);
               res5 += (x8 + x9 + x10 + x11 + x12 + x13 + x14 + x15);
               dc += b;

               // Average up-left: abs(avg((X,Y-1),(X-1,Y))-(X,Y))
               int avg = b;
               avg = (avg + px0) >> 1;
               res4 += ((avg - x0  + ((avg-x0)  >> 31)) ^ ((avg-x0)  >> 31));
               avg = (avg + px1) >> 1;
               res4 += ((avg - x1  + ((avg-x1)  >> 31)) ^ ((avg-x1)  >> 31));
               avg = (avg + px2) >> 1;
               res4 += ((avg - x2  + ((avg-x2)  >> 31)) ^ ((avg-x2)  >> 31));
               avg = (avg + px3) >> 1;
               res4 += ((avg - x3  + ((avg-x3)  >> 31)) ^ ((avg-x3)  >> 31));
               avg = (avg + px4) >> 1;
               res4 += ((avg - x4  + ((avg-x4)  >> 31)) ^ ((avg-x4)  >> 31));
               avg = (avg + px5) >> 1;
               res4 += ((avg - x5  + ((avg-x5)  >> 31)) ^ ((avg-x5)  >> 31));
               avg = (avg + px6) >> 1;
               res4 += ((avg - x6  + ((avg-x6)  >> 31)) ^ ((avg-x6)  >> 31));
               avg = (avg + px7) >> 1;
               res4 += ((avg - x7  + ((avg-x7)  >> 31)) ^ ((avg-x7)  >> 31));
               avg = (avg + px8) >> 1;
               res4 += ((avg - x8  + ((avg-x8)  >> 31)) ^ ((avg-x8)  >> 31));
               avg = (avg + px9) >> 1;
               res4 += ((avg - x9  + ((avg-x9)  >> 31)) ^ ((avg-x9)  >> 31));
               avg = (avg + px10) >> 1;
               res4 += ((avg - x10 + ((avg-x10) >> 31)) ^ ((avg-x10) >> 31));
               avg = (avg + px11) >> 1;
               res4 += ((avg - x11 + ((avg-x11) >> 31)) ^ ((avg-x11) >> 31));
               avg = (avg + px12) >> 1;
               res4 += ((avg - x12 + ((avg-x12) >> 31)) ^ ((avg-x12) >> 31));
               avg = (avg + px13) >> 1;
               res4 += ((avg - x13 + ((avg-x13) >> 31)) ^ ((avg-x13) >> 31));
               avg = (avg + px14) >> 1;
               res4 += ((avg - x14 + ((avg-x14) >> 31)) ^ ((avg-x14) >> 31));
               avg = (avg + px15) >> 1;
               res4 += ((avg - x15 + ((avg-x15) >> 31)) ^ ((avg-x15 ) >> 31));
               px0  = x0;
               px1  = x1;
               px2  = x2;
               px3  = x3;
               px4  = x4;
               px5  = x5;
               px6  = x6;
               px7  = x7;
               px8  = x1;
               px9  = x9;
               px10 = x10;
               px11 = x11;
               px12 = x12;
               px13 = x13;
            }
         }

         if (x < w_16)
         {
            // Horizontal right abs(c-x)
            int c = src[offset1+8] & 0xFF;
            res3 += ((c - x0  + ((c-x0)  >> 31)) ^ ((c-x0)  >> 31));
            res3 += ((c - x1  + ((c-x1)  >> 31)) ^ ((c-x1)  >> 31));
            res3 += ((c - x2  + ((c-x2)  >> 31)) ^ ((c-x2)  >> 31));
            res3 += ((c - x3  + ((c-x3)  >> 31)) ^ ((c-x3)  >> 31));
            res3 += ((c - x4  + ((c-x4)  >> 31)) ^ ((c-x4)  >> 31));
            res3 += ((c - x5  + ((c-x5)  >> 31)) ^ ((c-x5)  >> 31));
            res3 += ((c - x6  + ((c-x6)  >> 31)) ^ ((c-x6)  >> 31));
            res3 += ((c - x7  + ((c-x7)  >> 31)) ^ ((c-x7)  >> 31));
            res3 += ((c - x8  + ((c-x8)  >> 31)) ^ ((c-x8)  >> 31));
            res3 += ((c - x9  + ((c-x9)  >> 31)) ^ ((c-x9)  >> 31));
            res3 += ((c - x10 + ((c-x10) >> 31)) ^ ((c-x10) >> 31));
            res3 += ((c - x11 + ((c-x11) >> 31)) ^ ((c-x11) >> 31));
            res3 += ((c - x12 + ((c-x12) >> 31)) ^ ((c-x12) >> 31));
            res3 += ((c - x13 + ((c-x13) >> 31)) ^ ((c-x13) >> 31));
            res3 += ((c - x14 + ((c-x14) >> 31)) ^ ((c-x14) >> 31));
            res3 += ((c - x15 + ((c-x15) >> 31)) ^ ((c-x15) >> 31));
         }

         if (y > 0)
         {
            // Vertical abs(a-x)
            res1 += ((a0  - x0  + ((a0-x0)   >> 31)) ^ ((a0-x0)   >> 31));
            res1 += ((a1  - x1  + ((a1-x1)   >> 31)) ^ ((a1-x1)   >> 31));
            res1 += ((a2  - x2  + ((a2-x2)   >> 31)) ^ ((a2-x2)   >> 31));
            res1 += ((a3  - x3  + ((a3-x3)   >> 31)) ^ ((a3-x3)   >> 31));
            res1 += ((a4  - x4  + ((a4-x4)   >> 31)) ^ ((a4-x4)   >> 31));
            res1 += ((a5  - x5  + ((a5-x5)   >> 31)) ^ ((a5-x5)   >> 31));
            res1 += ((a6  - x6  + ((a6-x6)   >> 31)) ^ ((a6-x6)   >> 31));
            res1 += ((a7  - x7  + ((a7-x7)   >> 31)) ^ ((a7-x7)   >> 31));
            res1 += ((a8  - x8  + ((a8-x8)   >> 31)) ^ ((a8-x8)   >> 31));
            res1 += ((a9  - x9  + ((a9-x9)   >> 31)) ^ ((a9-x9)   >> 31));
            res1 += ((a10 - x10 + ((a10-x10) >> 31)) ^ ((a10-x10) >> 31));
            res1 += ((a11 - x11 + ((a11-x11) >> 31)) ^ ((a11-x11) >> 31));
            res1 += ((a12 - x12 + ((a12-x12) >> 31)) ^ ((a12-x12) >> 31));
            res1 += ((a13 - x13 + ((a13-x13) >> 31)) ^ ((a13-x13) >> 31));
            res1 += ((a14 - x14 + ((a14-x14) >> 31)) ^ ((a14-x14) >> 31));
            res1 += ((a15 - x15 + ((a15-x15) >> 31)) ^ ((a15-x15) >> 31));
         }

         offset1 += st;
      }

      if (x < w_16)
      {
         // Adjust "origin bottom-right" mode
         res6 += res5;

         if (res6 < 0)
            res6 = -res6;
      }
      else
      {
         res6 = MAX_VALUE_16x16; // HORIZONTAL_R
         res3 = MAX_VALUE_16x16; // ORIGIN_BR
      }

      if (y <= 0)
      {
         res1 = MAX_VALUE_16x16; // VERTICAL
         res4 = MAX_VALUE_16x16; // AVERAGE_UL
         res5 = MAX_VALUE_16x16; // DC
         res7 = MAX_VALUE_16x16; // ORIGIN_UL
      }
      else
      {
         // Adjust "origin up-left" mode
         res7 += res5;

         if (res7 < 0)
            res7 = -res7;

         // For DC, remove average value of 'a' row and 'b' column (32 * 8 = 16 * 16)
         res5 -= (dc << 3);

         if (res5 < 0)
            res5 = -res5;
      }

      if (x <= 0)
      {
         res2 = MAX_VALUE_16x16; // HORIZONTAL_R
         res4 = MAX_VALUE_16x16; // AVERAGE_UL
         res5 = MAX_VALUE_16x16; // DC
         res7 = MAX_VALUE_16x16; // ORIGIN_UL
      }

      if (other == null)
        res0 = MAX_VALUE_16x16; // TEMPORAL

      // Find minimum error for non-right dependent blocks
      int minL = res1;
      Mode minModeL = Mode.VERTICAL;

      if (res0 < minL)
      {
         minL = res0;
         minModeL = Mode.TEMPORAL;
      }

      if (res2 < minL)
      {
         minL = res2;
         minModeL = Mode.HORIZONTAL_L;
      }

      if (res4 < minL)
      {
         minL = res4;
         minModeL = Mode.AVERAGE_UL;
      }

      if (res5 < minL)
      {
         minL = res5;
         minModeL = Mode.DC;
      }

      if (res7 < minL)
      {
         minL = res7;
         minModeL = Mode.ORIGIN_UL;
      }

      // Find minimum error fo right dependent blocks
      int minR = res1;
      Mode minModeR = Mode.VERTICAL;

      if (res3 < minR)
      {
         minR = res3;
         minModeR = Mode.HORIZONTAL_R;
      }

      if (res6 < minR)
      {
         minR = res6;
         minModeR = Mode.ORIGIN_BR;
      }

      if (res0 < minR)
      {
         minR = res0;
         minModeR = Mode.TEMPORAL;
      }

      // Return packed value:
      // 3 bits left mode - 13 bits error - 3 bits right mode - 13 bits error
      int res = (minModeL.value << 13) | (minL >> 3);
      res <<= 16;
      res |= (minModeR.value << 13);
      res |= (minR >> 3);

      return res;
   }


   private int[] computeDiff8x8(int[] input, int iIdx, int[] output, int oIdx,
           int[] other, int offset, int predictionModeValue)
   {
      final int st = this.stride;
      final int end = iIdx + (st << 3);
      int k = oIdx;

      if (predictionModeValue == Mode.TEMPORAL.value)
      {
         if (other ==null)
         {
             // Simple copy
             for (int i=iIdx; i<end; i+=st)
             {
                output[k++] = input[i];
                output[k++] = input[i+1];
                output[k++] = input[i+2];
                output[k++] = input[i+3];
                output[k++] = input[i+4];
                output[k++] = input[i+5];
                output[k++] = input[i+6];
                output[k++] = input[i+7];
             }
         }
         else
         {
             int j = offset;
             
             for (int i=iIdx; i<end; i+=st)
             {
                output[k++] = (input[i]   & 0xFF) - (other[j]   & 0xFF);
                output[k++] = (input[i+1] & 0xFF) - (other[j+1] & 0xFF);
                output[k++] = (input[i+2] & 0xFF) - (other[j+2] & 0xFF);
                output[k++] = (input[i+3] & 0xFF) - (other[j+3] & 0xFF);
                output[k++] = (input[i+4] & 0xFF) - (other[j+4] & 0xFF);
                output[k++] = (input[i+5] & 0xFF) - (other[j+5] & 0xFF);
                output[k++] = (input[i+6] & 0xFF) - (other[j+6] & 0xFF);
                output[k++] = (input[i+7] & 0xFF) - (other[j+7] & 0xFF);
                j += 8;
             }
         }
         
         return output;
      }

      if ((predictionModeValue == Mode.VERTICAL.value) || (predictionModeValue == Mode.DC.value))
      {
         int above = iIdx - st;
         int a0 = input[above]   & 0xFF;
         int a1 = input[above+1] & 0xFF;
         int a2 = input[above+2] & 0xFF;
         int a3 = input[above+3] & 0xFF;
         int a4 = input[above+4] & 0xFF;
         int a5 = input[above+5] & 0xFF;
         int a6 = input[above+6] & 0xFF;
         int a7 = input[above+7] & 0xFF;

         if (predictionModeValue == Mode.VERTICAL.value)
         {
            for (int i=iIdx; i<end; i+=st)
            {
               output[k++] = (input[i]   & 0xFF) - a0;
               output[k++] = (input[i+1] & 0xFF) - a1;
               output[k++] = (input[i+2] & 0xFF) - a2;
               output[k++] = (input[i+3] & 0xFF) - a3;
               output[k++] = (input[i+4] & 0xFF) - a4;
               output[k++] = (input[i+5] & 0xFF) - a5;
               output[k++] = (input[i+6] & 0xFF) - a6;
               output[k++] = (input[i+7] & 0xFF) - a7;
            }

            return output;
         }

         int dc = (a0 + a1 + a2 + a3 + a4 + a5 + a6 + a7);
         dc += (input[iIdx-1]  & 0xFF);
         dc += (input[iIdx+7]  & 0xFF);
         dc += (input[iIdx+15] & 0xFF);
         dc += (input[iIdx+23] & 0xFF);
         dc += (input[iIdx+31] & 0xFF);
         dc += (input[iIdx+39] & 0xFF);
         dc += (input[iIdx+47] & 0xFF);
         dc += (input[iIdx+55] & 0xFF);
         dc = (dc + 4) >> 4;

         for (int i=iIdx; i<end; i+=st)
         {
            output[k++] = (input[i]   & 0xFF) - dc;
            output[k++] = (input[i+1] & 0xFF) - dc;
            output[k++] = (input[i+2] & 0xFF) - dc;
            output[k++] = (input[i+3] & 0xFF) - dc;
            output[k++] = (input[i+4] & 0xFF) - dc;
            output[k++] = (input[i+5] & 0xFF) - dc;
            output[k++] = (input[i+6] & 0xFF) - dc;
            output[k++] = (input[i+7] & 0xFF) - dc;
         }

         return output;
      }

      if ((predictionModeValue == Mode.HORIZONTAL_L.value) || (predictionModeValue == Mode.HORIZONTAL_R.value))
      {
         int idx = (predictionModeValue == Mode.HORIZONTAL_L.value) ? iIdx-1 : iIdx+8;

         for (int i=iIdx; i<end; i+=st)
         {
            int b = input[idx] & 0xFF;
            idx += st;
            output[k++] = (input[i]   & 0xFF) - b;
            output[k++] = (input[i+1] & 0xFF) - b;
            output[k++] = (input[i+2] & 0xFF) - b;
            output[k++] = (input[i+3] & 0xFF) - b;
            output[k++] = (input[i+4] & 0xFF) - b;
            output[k++] = (input[i+5] & 0xFF) - b;
            output[k++] = (input[i+6] & 0xFF) - b;
            output[k++] = (input[i+7] & 0xFF) - b;
         }

         return output;
      }

      if (predictionModeValue == Mode.ORIGIN_UL.value)
      {
         int o = input[iIdx-st-1] & 0xFF;

         for (int i=iIdx; i<end; i+=st)
         {
            output[k++] = (input[i]   & 0xFF) - o;
            output[k++] = (input[i+1] & 0xFF) - o;
            output[k++] = (input[i+2] & 0xFF) - o;
            output[k++] = (input[i+3] & 0xFF) - o;
            output[k++] = (input[i+4] & 0xFF) - o;
            output[k++] = (input[i+5] & 0xFF) - o;
            output[k++] = (input[i+6] & 0xFF) - o;
            output[k++] = (input[i+7] & 0xFF) - o;
         }

         return output;
      }

      if (predictionModeValue == Mode.ORIGIN_BR.value)
      {
         int p = input[iIdx+(st<<3)-st+8] & 0xFF;

         for (int i=iIdx; i<end; i+=st)
         {
            output[k++] = (input[i]   & 0xFF) - p;
            output[k++] = (input[i+1] & 0xFF) - p;
            output[k++] = (input[i+2] & 0xFF) - p;
            output[k++] = (input[i+3] & 0xFF) - p;
            output[k++] = (input[i+4] & 0xFF) - p;
            output[k++] = (input[i+5] & 0xFF) - p;
            output[k++] = (input[i+6] & 0xFF) - p;
            output[k++] = (input[i+7] & 0xFF) - p;
         }

         return output;
      }

      if (predictionModeValue == Mode.AVERAGE_UL.value)
      {
         int above = iIdx - st;
         int px0 = input[above]   & 0xFF;
         int px1 = input[above+1] & 0xFF;
         int px2 = input[above+2] & 0xFF;
         int px3 = input[above+3] & 0xFF;
         int px4 = input[above+4] & 0xFF;
         int px5 = input[above+5] & 0xFF;
         int px6 = input[above+6] & 0xFF;
         int px7 = input[above+7] & 0xFF;
         int idx = iIdx - 1;

         for (int i=iIdx; i<end; i+=st)
         {
            int x0 = (input[i]   & 0xFF);
            int x1 = (input[i+1] & 0xFF);
            int x2 = (input[i+2] & 0xFF);
            int x3 = (input[i+3] & 0xFF);
            int x4 = (input[i+4] & 0xFF);
            int x5 = (input[i+5] & 0xFF);
            int x6 = (input[i+6] & 0xFF);
            int x7 = (input[i+7] & 0xFF);
            int b = input[idx] & 0xFF;
            idx += st;
            int avg;
            avg = (b + px0) >> 1;
            output[k++] = avg - x0;
            avg = (x0 + px1) >> 1;
            output[k++] = avg - x1;
            avg = (x1 + px2) >> 1;
            output[k++] = avg - x2;
            avg = (x2 + px3) >> 1;
            output[k++] = avg - x3;
            avg = (x3 + px4) >> 1;
            output[k++] = avg - x4;
            avg = (x4 + px5) >> 1;
            output[k++] = avg - x5;
            avg = (x5 + px6) >> 1;
            output[k++] = avg - x6;
            avg = (x6 + px7) >> 1;
            output[k++] = avg - x7;
            px0 = x0;
            px1 = x1;
            px2 = x2;
            px3 = x3;
            px4 = x4;
            px5 = x5;
            px6 = x6;
            px7 = x7;
         }

         return output;
      }

      return null;
   }


   
   public int[] computeDifferences(int[] input, int iIdx, int[] output, int oIdx,
           int[] other, int offset, int predictionModeValue)
   {
      if (this.dim == 8)
         return this.computeDiff8x8(input, iIdx, output, oIdx, other, offset, predictionModeValue);

      return this.computeDiff16x16(input, iIdx, output, oIdx, other, offset, predictionModeValue);
   }

      
   private int[] computeDiff16x16(int[] input, int iIdx, int[] output, int oIdx,
           int[] other, int offset, int predictionModeValue)
   {
      int st = this.stride;
      int end = iIdx + (st << 4);
      int k = oIdx;

      if (predictionModeValue == Mode.TEMPORAL.value)
      {
         if (other == null)
         {
             // Simple copy
             for (int i=iIdx; i<end; i+=st)
             {
                output[k++] = input[i];
                output[k++] = input[i+1];
                output[k++] = input[i+2];
                output[k++] = input[i+3];
                output[k++] = input[i+4];
                output[k++] = input[i+5];
                output[k++] = input[i+6];
                output[k++] = input[i+7];
                output[k++] = input[i+8];
                output[k++] = input[i+9];
                output[k++] = input[i+10];
                output[k++] = input[i+11];
                output[k++] = input[i+12];
                output[k++] = input[i+13];
                output[k++] = input[i+14];
                output[k++] = input[i+15];
             }
         }
         else
         {
             int j = offset;
             
             for (int i=iIdx; i<end; i+=st)
             {
                output[k++] = (input[i]    & 0xFF) - (other[j]    & 0xFF);
                output[k++] = (input[i+1]  & 0xFF) - (other[j+1]  & 0xFF);
                output[k++] = (input[i+2]  & 0xFF) - (other[j+2]  & 0xFF);
                output[k++] = (input[i+3]  & 0xFF) - (other[j+3]  & 0xFF);
                output[k++] = (input[i+4]  & 0xFF) - (other[j+4]  & 0xFF);
                output[k++] = (input[i+5]  & 0xFF) - (other[j+5]  & 0xFF);
                output[k++] = (input[i+6]  & 0xFF) - (other[j+6]  & 0xFF);
                output[k++] = (input[i+7]  & 0xFF) - (other[j+7]  & 0xFF);
                output[k++] = (input[i+8]  & 0xFF) - (other[j+8]  & 0xFF);
                output[k++] = (input[i+9]  & 0xFF) - (other[j+9]  & 0xFF);
                output[k++] = (input[i+10] & 0xFF) - (other[j+10] & 0xFF);
                output[k++] = (input[i+11] & 0xFF) - (other[j+11] & 0xFF);
                output[k++] = (input[i+12] & 0xFF) - (other[j+12] & 0xFF);
                output[k++] = (input[i+13] & 0xFF) - (other[j+13] & 0xFF);
                output[k++] = (input[i+14] & 0xFF) - (other[j+14] & 0xFF);
                output[k++] = (input[i+15] & 0xFF) - (other[j+15] & 0xFF);
                j += 16;
             }
         }

         return output;
      }

      if ((predictionModeValue == Mode.VERTICAL.value) || (predictionModeValue == Mode.DC.value))
      {
         int above = iIdx - st;
         int a0  = input[above]    & 0xFF;
         int a1  = input[above+1]  & 0xFF;
         int a2  = input[above+2]  & 0xFF;
         int a3  = input[above+3]  & 0xFF;
         int a4  = input[above+4]  & 0xFF;
         int a5  = input[above+5]  & 0xFF;
         int a6  = input[above+6]  & 0xFF;
         int a7  = input[above+7]  & 0xFF;
         int a8  = input[above+8]  & 0xFF;
         int a9  = input[above+9]  & 0xFF;
         int a10 = input[above+10] & 0xFF;
         int a11 = input[above+11] & 0xFF;
         int a12 = input[above+12] & 0xFF;
         int a13 = input[above+13] & 0xFF;
         int a14 = input[above+14] & 0xFF;
         int a15 = input[above+15] & 0xFF;

         if (predictionModeValue == Mode.VERTICAL.value)
         {
            for (int i=iIdx; i<end; i+=st)
            {
               output[k++] = (input[i]    & 0xFF) - a0;
               output[k++] = (input[i+1]  & 0xFF) - a1;
               output[k++] = (input[i+2]  & 0xFF) - a2;
               output[k++] = (input[i+3]  & 0xFF) - a3;
               output[k++] = (input[i+4]  & 0xFF) - a4;
               output[k++] = (input[i+5]  & 0xFF) - a5;
               output[k++] = (input[i+6]  & 0xFF) - a6;
               output[k++] = (input[i+7]  & 0xFF) - a7;
               output[k++] = (input[i+8]  & 0xFF) - a8;
               output[k++] = (input[i+9]  & 0xFF) - a9;
               output[k++] = (input[i+10] & 0xFF) - a10;
               output[k++] = (input[i+11] & 0xFF) - a11;
               output[k++] = (input[i+12] & 0xFF) - a12;
               output[k++] = (input[i+13] & 0xFF) - a13;
               output[k++] = (input[i+14] & 0xFF) - a14;
               output[k++] = (input[i+15] & 0xFF) - a15;
            }

            return output;
         }

         int dc = a0 + a1 + a2 + a3 + a4 + a5 + a6 + a7 +a8 + a9 + a10 + a11 + a12 + a13 + a14 + a15;
         dc += (input[iIdx-1]   & 0xFF);
         dc += (input[iIdx+7]   & 0xFF);
         dc += (input[iIdx+15]  & 0xFF);
         dc += (input[iIdx+23]  & 0xFF);
         dc += (input[iIdx+31]  & 0xFF);
         dc += (input[iIdx+39]  & 0xFF);
         dc += (input[iIdx+47]  & 0xFF);
         dc += (input[iIdx+55]  & 0xFF);
         dc += (input[iIdx+63]  & 0xFF);
         dc += (input[iIdx+71]  & 0xFF);
         dc += (input[iIdx+79]  & 0xFF);
         dc += (input[iIdx+87]  & 0xFF);
         dc += (input[iIdx+95]  & 0xFF);
         dc += (input[iIdx+103] & 0xFF);
         dc += (input[iIdx+111] & 0xFF);
         dc += (input[iIdx+119] & 0xFF);
         dc = (dc + 8) >> 5;

         for (int i=iIdx; i<end; i+=st)
         {
            output[k++] = (input[i]    & 0xFF) - dc;
            output[k++] = (input[i+1]  & 0xFF) - dc;
            output[k++] = (input[i+2]  & 0xFF) - dc;
            output[k++] = (input[i+3]  & 0xFF) - dc;
            output[k++] = (input[i+4]  & 0xFF) - dc;
            output[k++] = (input[i+5]  & 0xFF) - dc;
            output[k++] = (input[i+6]  & 0xFF) - dc;
            output[k++] = (input[i+7]  & 0xFF) - dc;
            output[k++] = (input[i+8]  & 0xFF) - dc;
            output[k++] = (input[i+9]  & 0xFF) - dc;
            output[k++] = (input[i+10] & 0xFF) - dc;
            output[k++] = (input[i+11] & 0xFF) - dc;
            output[k++] = (input[i+12] & 0xFF) - dc;
            output[k++] = (input[i+13] & 0xFF) - dc;
            output[k++] = (input[i+14] & 0xFF) - dc;
            output[k++] = (input[i+15] & 0xFF) - dc;
         }

         return output;
      }

      if ((predictionModeValue == Mode.HORIZONTAL_L.value) || (predictionModeValue == Mode.HORIZONTAL_R.value))
      {
         int idx = (predictionModeValue == Mode.HORIZONTAL_L.value) ? iIdx-1 : iIdx+16;

         for (int i=iIdx; i<end; i+=st)
         {
            int b = input[idx] & 0xFF;
            idx += st;
            output[k++] = (input[i]    & 0xFF) - b;
            output[k++] = (input[i+1]  & 0xFF) - b;
            output[k++] = (input[i+2]  & 0xFF) - b;
            output[k++] = (input[i+3]  & 0xFF) - b;
            output[k++] = (input[i+4]  & 0xFF) - b;
            output[k++] = (input[i+5]  & 0xFF) - b;
            output[k++] = (input[i+6]  & 0xFF) - b;
            output[k++] = (input[i+7]  & 0xFF) - b;
            output[k++] = (input[i+8]  & 0xFF) - b;
            output[k++] = (input[i+9]  & 0xFF) - b;
            output[k++] = (input[i+10] & 0xFF) - b;
            output[k++] = (input[i+11] & 0xFF) - b;
            output[k++] = (input[i+12] & 0xFF) - b;
            output[k++] = (input[i+13] & 0xFF) - b;
            output[k++] = (input[i+14] & 0xFF) - b;
            output[k++] = (input[i+15] & 0xFF) - b;
         }

         return output;
      }

      if (predictionModeValue == Mode.ORIGIN_UL.value)
      {
         int o = input[iIdx-st-1] & 0xFF;

         for (int i=iIdx; i<end; i+=st)
         {
            output[k++] = (input[i]    & 0xFF) - o;
            output[k++] = (input[i+1]  & 0xFF) - o;
            output[k++] = (input[i+2]  & 0xFF) - o;
            output[k++] = (input[i+3]  & 0xFF) - o;
            output[k++] = (input[i+4]  & 0xFF) - o;
            output[k++] = (input[i+5]  & 0xFF) - o;
            output[k++] = (input[i+6]  & 0xFF) - o;
            output[k++] = (input[i+7]  & 0xFF) - o;
            output[k++] = (input[i+8]  & 0xFF) - o;
            output[k++] = (input[i+9]  & 0xFF) - o;
            output[k++] = (input[i+10] & 0xFF) - o;
            output[k++] = (input[i+11] & 0xFF) - o;
            output[k++] = (input[i+12] & 0xFF) - o;
            output[k++] = (input[i+13] & 0xFF) - o;
            output[k++] = (input[i+14] & 0xFF) - o;
            output[k++] = (input[i+15] & 0xFF) - o;
         }

         return output;
      }

      if (predictionModeValue == Mode.ORIGIN_BR.value)
      {
         int p = input[iIdx+(st<<4)-st+16] & 0xFF;

         for (int i=iIdx; i<end; i+=st)
         {
            output[k++] = (input[i]    & 0xFF) - p;
            output[k++] = (input[i+1]  & 0xFF) - p;
            output[k++] = (input[i+2]  & 0xFF) - p;
            output[k++] = (input[i+3]  & 0xFF) - p;
            output[k++] = (input[i+4]  & 0xFF) - p;
            output[k++] = (input[i+5]  & 0xFF) - p;
            output[k++] = (input[i+6]  & 0xFF) - p;
            output[k++] = (input[i+7]  & 0xFF) - p;
            output[k++] = (input[i+8]  & 0xFF) - p;
            output[k++] = (input[i+9]  & 0xFF) - p;
            output[k++] = (input[i+10] & 0xFF) - p;
            output[k++] = (input[i+11] & 0xFF) - p;
            output[k++] = (input[i+12] & 0xFF) - p;
            output[k++] = (input[i+13] & 0xFF) - p;
            output[k++] = (input[i+14] & 0xFF) - p;
            output[k++] = (input[i+15] & 0xFF) - p;
         }

         return output;
      }

      if (predictionModeValue == Mode.AVERAGE_UL.value)
      {
         int above = iIdx - st;
         int px0  = input[above]    & 0xFF;
         int px1  = input[above+1]  & 0xFF;
         int px2  = input[above+2]  & 0xFF;
         int px3  = input[above+3]  & 0xFF;
         int px4  = input[above+4]  & 0xFF;
         int px5  = input[above+5]  & 0xFF;
         int px6  = input[above+6]  & 0xFF;
         int px7  = input[above+7]  & 0xFF;
         int px8  = input[above+8]  & 0xFF;
         int px9  = input[above+9]  & 0xFF;
         int px10 = input[above+10] & 0xFF;
         int px11 = input[above+11] & 0xFF;
         int px12 = input[above+12] & 0xFF;
         int px13 = input[above+13] & 0xFF;
         int px14 = input[above+14] & 0xFF;
         int px15 = input[above+15] & 0xFF;
         int idx = iIdx - 1;

         for (int i=iIdx; i<end; i+=st)
         {
            int x0  = (input[i]    & 0xFF);
            int x1  = (input[i+1]  & 0xFF);
            int x2  = (input[i+2]  & 0xFF);
            int x3  = (input[i+3]  & 0xFF);
            int x4  = (input[i+4]  & 0xFF);
            int x5  = (input[i+5]  & 0xFF);
            int x6  = (input[i+6]  & 0xFF);
            int x7  = (input[i+7]  & 0xFF);
            int x8  = (input[i+8]  & 0xFF);
            int x9  = (input[i+9]  & 0xFF);
            int x10 = (input[i+10] & 0xFF);
            int x11 = (input[i+11] & 0xFF);
            int x12 = (input[i+12] & 0xFF);
            int x13 = (input[i+13] & 0xFF);
            int x14 = (input[i+14] & 0xFF);
            int x15 = (input[i+15] & 0xFF);
            int b = input[idx] & 0xFF;
            idx += st;
            int avg;
            avg = (b  + px0) >> 1;
            output[k++] = avg - x0;
            avg = (x0  + px1) >> 1;
            output[k++] = avg - x1;
            avg = (x1  + px2) >> 1;
            output[k++] = avg - x2;
            avg = (x2  + px3) >> 1;
            output[k++] = avg - x3;
            avg = (x3  + px4) >> 1;
            output[k++] = avg - x4;
            avg = (x4  + px5) >> 1;
            output[k++] = avg - x5;
            avg = (x5  + px6) >> 1;
            output[k++] = avg - x6;
            avg = (x6  + px7) >> 1;
            output[k++] = avg - x7;
            avg = (x7  + px8) >> 1;
            output[k++] = avg - x8;
            avg = (x8  + px9) >> 1;
            output[k++] = avg - x9;
            avg = (x9 + px10) >> 1;
            output[k++] = avg - x10;
            avg = (x10 + px11) >> 1;
            output[k++] = avg - x11;
            avg = (x11 + px12) >> 1;
            output[k++] = avg - x12;
            avg = (x12 + px13) >> 1;
            output[k++] = avg - x13;
            avg = (x13 + px14) >> 1;
            output[k++] = avg - x14;
            avg = (x14 + px15) >> 1;
            output[k++] = avg - x15;
            px0  = x0;
            px1  = x1;
            px2  = x2;
            px3  = x3;
            px4  = x4;
            px5  = x5;
            px6  = x6;
            px7  = x7;
            px8  = x8;
            px9  = x9;
            px10 = x10;
            px11 = x11;
            px12 = x12;
            px13 = x13;
            px14 = x14;
            px15 = x15;
         }

         return output;
      }

      return null;
   }
}
