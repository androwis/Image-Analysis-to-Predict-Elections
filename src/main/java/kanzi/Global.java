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

package kanzi;


public class Global
{
   public final static int INFINITE_VALUE = 0;
   
   // array with 256 elements: 4096*Math.log2(x)
   private static final int[] LOG2_256 =
   {
          0,     0,  4096,  6492,  8192,  9510, 10588, 11498, 12288, 12984, 13606, 14169, 14684, 15157, 15594, 16002,
      16384, 16742, 17080, 17399, 17702, 17990, 18265, 18528, 18780, 19021, 19253, 19476, 19690, 19898, 20098, 20292,
      20480, 20661, 20838, 21009, 21176, 21337, 21495, 21649, 21798, 21944, 22086, 22225, 22361, 22494, 22624, 22751,
      22876, 22997, 23117, 23234, 23349, 23461, 23572, 23680, 23786, 23891, 23994, 24095, 24194, 24292, 24388, 24482,
      24576, 24667, 24757, 24846, 24934, 25020, 25105, 25189, 25272, 25353, 25433, 25513, 25591, 25668, 25745, 25820,
      25894, 25968, 26040, 26112, 26182, 26252, 26321, 26390, 26457, 26524, 26590, 26655, 26720, 26784, 26847, 26910,
      26972, 27033, 27093, 27153, 27213, 27272, 27330, 27387, 27445, 27501, 27557, 27613, 27668, 27722, 27776, 27829,
      27882, 27935, 27987, 28039, 28090, 28141, 28191, 28241, 28290, 28339, 28388, 28436, 28484, 28531, 28578, 28625,
      28672, 28717, 28763, 28808, 28853, 28898, 28942, 28986, 29030, 29073, 29116, 29159, 29201, 29243, 29285, 29326,
      29368, 29408, 29449, 29489, 29529, 29569, 29609, 29648, 29687, 29726, 29764, 29803, 29841, 29878, 29916, 29953,
      29990, 30027, 30064, 30100, 30136, 30172, 30208, 30243, 30278, 30314, 30348, 30383, 30417, 30452, 30486, 30520,
      30553, 30587, 30620, 30653, 30686, 30719, 30751, 30784, 30816, 30848, 30880, 30912, 30943, 30974, 31006, 31037,
      31068, 31098, 31129, 31159, 31189, 31219, 31249, 31279, 31309, 31338, 31368, 31397, 31426, 31455, 31483, 31512,
      31541, 31569, 31597, 31625, 31653, 31681, 31709, 31736, 31764, 31791, 31818, 31845, 31872, 31899, 31925, 31952,
      31978, 32005, 32031, 32057, 32083, 32109, 32135, 32160, 32186, 32211, 32237, 32262, 32287, 32312, 32337, 32361,
      32386, 32411, 32435, 32460, 32484, 32508, 32532, 32556, 32580, 32604, 32627, 32651, 32674, 32698, 32721, 32744
   };


   // array with 10 elements: 10 * (4096*Math.log10(x))
   private static final int[] TEN_LOG10_100 =
   {
          0,     0, 12330, 19542, 24660, 28629, 31873, 34615, 36990, 39085,
      40960, 42655, 44203, 45627, 46945, 48172, 49320, 50399, 51415, 52377,
      53290, 54158, 54985, 55776, 56533, 57259, 57957, 58628, 59275, 59899,
      60502, 61086, 61650, 62198, 62729, 63245, 63746, 64233, 64707, 65170,
      65620, 66059, 66488, 66906, 67315, 67715, 68106, 68489, 68863, 69230,
      69589, 69942, 70287, 70626, 70958, 71285, 71605, 71920, 72230, 72534,
      72833, 73127, 73416, 73700, 73981, 74256, 74528, 74796, 75059, 75319,
      75575, 75827, 76076, 76321, 76563, 76802, 77038, 77270, 77500, 77726,
      77950, 78171, 78389, 78605, 78818, 79028, 79237, 79442, 79646, 79847,
      80045, 80242, 80436, 80629, 80819, 81007, 81193, 81378, 81560, 81741
   };


   // Return 1024 * 10 * log10(x)
   public static int ten_log10(int x) throws ArithmeticException
   {
      if (x <= 0)
         throw new ArithmeticException("Cannot calculate log of a negative or null value");

      if (x < 100)
         return TEN_LOG10_100[x] >> 2;
      
      return (log2(x) * 6165) >> 11; // 10 * 1/log2(10)
   }


   // Return 1024 * log2(x)
   // This method should not be used to find non fractional log2(x)
   public static int log2(int x) throws ArithmeticException
   {
      if (x <= 0)
         throw new ArithmeticException("Cannot calculate log of a negative or null value");

      if (x < 256)
         return LOG2_256[x] >> 2;

      int log = 0;

      for (long y=x+1; y>1; y>>=1)
        log++;

      // x is a power of 2 ?
      if ((x & (x-1)) == 0)
         return log << 10;

      long taylor;

      // Use the fact that log2(x) = log2(2^(log2(x)+1)*y) = log2(2^p)+ 1 + ln(1-z)/ln(2)
      // with z in ]0, 0.5[, it yields log2(x) = p + 1 - (z/1 + z^2/2 + z^3/3 ...)/ln(2)
      // To improve accuracy (keep z in ]0, 0.25[), one can choose either (1+z) or (1-z).
      // EG: log2(257) = log2(256) + log2(1+1/256) is better approximated with Taylor 
      // series expansion than log2(512) + log2(1-255/512)
      if ((x - (1 << log)) > (1 << (log-1)))
      {
         // Select 1 - x Taylor series expansion
         log++;
         long z = (1 << log) - x;
         long z2 = (z*z) >> log;
         taylor = z;
         taylor += (z2 >> 1);
         taylor += (((z*z2) >> log ) / 3);
         taylor += ((z2*z2) >> (log+log+2));
         taylor = -taylor;
      }
      else
      {
         // Select 1 + x Taylor series expansion
         long z = x - (1 << log);
         long z2 = (z*z) >> log;
         taylor = z;
         taylor -= (z2 >> 1);
         taylor += (((z*z2) >> log ) / 3);
         taylor -= ((z2*z2) >> (log+log+2));
      }

      taylor *= 5909; // 4096*1/ln(2)
      return (int) (((log << 12) + (taylor >> log)) >> 2);
   }


    // 8x8 block scan tables
    public static final int[] ZIGZAG_SCAN_TABLE =
    {  0,  1,  8, 16,  9,  2,  3, 10,
      17, 24, 32, 25, 18, 11,  4,  5,
      12, 19, 26, 33, 40, 48, 41, 34,
      27, 20, 13,  6,  7, 14, 21, 28,
      35, 42, 49, 56, 57, 50, 43, 36,
      29, 22, 15, 23, 30, 37, 44, 51,
      58, 59, 52, 45, 38, 31, 39, 46,
      53, 60, 61, 54, 47, 55, 62, 63
    };


    // Use multiplication and rescaling by 256 instead of a division
    public static final int[] QUANTIZATION_INTRA =
    { 32, 16, 13, 12, 10,  9,  9,  8,
      16, 16, 12, 11,  9,  9,  8,  7,
      13, 12, 10,  9,  9,  8,  8,  7,
      12, 12, 10,  9,  9,  8,  7,  6,
      12, 10,  9,  9,  8,  7,  6,  5,
      10,  9,  9,  8,  7,  6,  5,  4,
      10,  9,  9,  8,  7,  6,  5,  4,
       9,  9,  7,  7,  6,  5,  4,  3
    };

    public static final int[] DEQUANTIZATION_INTRA =
    {  8, 16, 19, 22, 26, 27, 29, 34,
      16, 16, 22, 24, 27, 29, 34, 37,
      19, 22, 26, 27, 29, 34, 34, 38,
      22, 22, 26, 27, 29, 34, 37, 40,
      22, 26, 27, 29, 32, 35, 40, 48,
      26, 27, 29, 32, 35, 40, 48, 58,
      26, 27, 29, 34, 38, 46, 56, 69,
      27, 29, 35, 38, 46, 56, 69, 83
    };


    // Use multiplication and rescaling by 256 instead of a division
    public static final int[] QUANTIZATION_NON_INTRA =
    {  16, 15, 14, 13, 12, 11, 10,  9,
       15, 14, 14, 12, 11, 10,  9,  9,
       14, 13, 13, 12, 11, 10,  9,  8,
       13, 13, 12, 11, 10,  9,  9,  8,
       13, 12, 11, 10,  9,  9,  8,  7,
       12, 11, 10,  9,  9,  8,  7,  7,
       11, 10,  9,  9,  8,  8,  7,  6,
       10,  9,  9,  8,  8,  7,  6,  5
    };

    
    public static boolean isIn(int x, int a, int b)
    {
       return (x-a) < (b-a) ? true : false;
    }


    public static int max(int x, int y)
    {
        return x - (((x - y) >> 31) & (x - y));
    }


    public static int min(int x, int y)
    {
        return y + (((x - y) >> 31) & (x - y));
    }


    public static int clip0_255(int x)
    {
        return ((~(x >> 31)) & 255 & (x | ((255-x) >> 31)));
    }

    
    public static int abs(int x)
    {
        // Patented (!) :  return (x ^ (x >> 31)) - (x >> 31);
        return (x + (x >> 31)) ^ (x >> 31);
    }


    public static int positiveOrNull(int x)
    {
       // return (x & ((-x) >> 31));
        return (x & (~(x >> 31)));
    }
    
    
    public static boolean isPowerOf2(int x)
    {
        return ((x & (x-1)) == 0) ? true : false;
    }


    private static final int[] SQRT = new int[]
    {
        0,  16,  23,  28,  32,  36,  39,  42,  45,  48,  51,  53,  55,  58,  60,  62,
       64,  66,  68,  70,  72,  73,  75,  77,  78,  80,  82,  83,  85,  86,  88,  89,
       91,  92,  93,  95,  96,  97,  99, 100, 101, 102, 104, 105, 106, 107, 109, 110,
      111, 112, 113, 114, 115, 116, 118, 119, 120, 121, 122, 123, 124, 125, 126, 127,
      128, 129, 130, 131, 132, 133, 134, 135, 136, 137, 138, 139, 139, 140, 141, 142,
      143, 144, 145, 146, 147, 148, 148, 149, 150, 151, 152, 153, 153, 154, 155, 156,
      157, 158, 158, 159, 160, 161, 162, 162, 163, 164, 165, 166, 166, 167, 168, 169,
      169, 170, 171, 172, 172, 173, 174, 175, 175, 176, 177, 177, 178, 179, 180, 180,
      181, 182, 182, 183, 184, 185, 185, 186, 187, 187, 188, 189, 189, 190, 191, 191,
      192, 193, 193, 194, 195, 195, 196, 197, 197, 198, 199, 199, 200, 200, 201, 202,
      202, 203, 204, 204, 205, 206, 206, 207, 207, 208, 209, 209, 210, 210, 211, 212,
      212, 213, 213, 214, 215, 215, 216, 216, 217, 218, 218, 219, 219, 220, 221, 221,
      222, 222, 223, 223, 224, 225, 225, 226, 226, 227, 227, 228, 229, 229, 230, 230,
      231, 231, 232, 232, 233, 234, 234, 235, 235, 236, 236, 237, 237, 238, 238, 239,
      239, 240, 241, 241, 242, 242, 243, 243, 244, 244, 245, 245, 246, 246, 247, 247,
      248, 248, 249, 249, 250, 250, 251, 251, 252, 252, 253, 253, 254, 254, 255, 255
    };


    private static final int THRESHOLD0 =  1 << 8;
    private static final int THRESHOLD1 =  1 << 16;
    private static final int THRESHOLD2 = (1 << 10) - 3;
    private static final int THRESHOLD3 = (1 << 14) - 28;
    private static final int THRESHOLD4 =  1 << 24;
    private static final int THRESHOLD5 =  1 << 20;
    private static final int THRESHOLD6 =  1 << 28;
    private static final int THRESHOLD7 =  1 << 26;
    private static final int THRESHOLD8 =  1 << 30;


    // Integer SQRT implementation based on algorithm at
    // http://guru.multimedia.cx/fast-integer-square-root/
    // Return 1024*sqrt(x) with a precision higher than 0.1%
    public static int sqrt(int x) throws IllegalArgumentException
    {
       if (x < 0)
           throw new IllegalArgumentException("Cannot calculate sqrt of a negative value");

       if (x <= 1)
          return x << 10;

       final int shift = (x < THRESHOLD5) ? ((x < THRESHOLD0) ? 16 : 10) : 0;
       x <<= shift; // scale up for better precision
       
       int val;

       if (x < THRESHOLD1)
       {
           if (x < THRESHOLD2)
           {
              val = SQRT[(x+3)>>2] >> 3;
           }
           else
           {
               if (x < THRESHOLD3)
                  val = SQRT[(x+28)>>6] >> 1;
               else
                  val = SQRT[x>>8];
           }
       }
       else
       {
           if (x < THRESHOLD4)
           {
               if (x < THRESHOLD5)
               {
                   val = SQRT[x>>12];
                   val = ((x/val) >> 3) + (val << 1);
               }
               else
               {
                   val = SQRT[x>>16];
                   val = ((x/val) >> 5) + (val << 3);
               }
           }
           else
           {
               if (x < THRESHOLD6)
               {
                   if (x < THRESHOLD7)
                   {
                       val = SQRT[x>>18];
                       val = ((x/val) >> 6) + (val << 4);
                   }
                   else
                   {
                       val = SQRT[x>>20];
                       val = ((x/val) >> 7) + (val << 5);
                   }
               }
               else
               {
                   if (x < THRESHOLD8)
                   {
                       val = SQRT[x>>22];
                       val = ((x/val) >> 8) + (val << 6);
                   }
                   else
                   {
                       val = SQRT[x>>24];
                       val = ((x/val) >> 9) + (val << 7);
                   }
               }
           }
       }

       // return 1024 * sqrt(x)
       return (val - ((x - (val*val)) >>> 31)) << (10 - (shift >> 1));
    }
    
}