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

package kanzi.transform;

import kanzi.IntTransform;


   /*  This implementation is based on an algorithm described in
    *  C. Loeffler, A. Ligtenberg and G. Moschytz, "Practical Fast 1-D DCT
    *  Algorithms with 11 Multiplications"
    *  Proceedings of the IEEE International Conference on Acoustics,
    *  Speech, and Signal Processing 1989 (ICASSP 89).
    */

public final class DCT8 implements IntTransform
{
    private static final int CONST_BITS = 13;
    private static final int PASS1_BITS = 2;
    private static final int SUB_BITS = CONST_BITS - PASS1_BITS;
    private static final int ADD_BITS = CONST_BITS + PASS1_BITS;
    private static final int SCALE_SUB = 1 << (SUB_BITS-1);
    private static final int SCALE_ADD = 1 << (ADD_BITS-1);
    private static final int SCALE_PASS1 = 1 << (PASS1_BITS-1);
    
    private static final int CONST_0_298631336 =  2446;	 // 8192 * 0.298631336
    private static final int CONST_0_390180644 =  3196;	 // 8192 * 0.390180644
    private static final int CONST_0_541196100 =  4433;	 // 8192 * 0.541196100
    private static final int CONST_0_765366865 =  6270;	 // 8192 * 0.765366865
    private static final int CONST_0_899976223 =  7373;	 // 8192 * 0.899976223
    private static final int CONST_1_175875602 =  9633;	 // 8192 * 1.175875602
    private static final int CONST_1_501321110 = 12299;	 // 8192 * 1.501321110
    private static final int CONST_1_847759065 = 15137;	 // 8192 * 1.847759065
    private static final int CONST_1_961570560 = 16069;	 // 8192 * 1.961570560
    private static final int CONST_2_053119869 = 16819;	 // 8192 * 2.053119869
    private static final int CONST_2_562915447 = 20995;	 // 8192 * 2.562915447
    private static final int CONST_3_072711026 = 25172;	 // 8192 * 3.072711026
    
    private static final int w1 = 2841;			 // 2048*sqrt(2)*cos(1*pi/16)
    private static final int w2 = 2676;			 // 2048*sqrt(2)*cos(2*pi/16)
    private static final int w3 = 2408;			 // 2048*sqrt(2)*cos(3*pi/16)
    private static final int w5 = 1609;			 // 2048*sqrt(2)*cos(5*pi/16)
    private static final int w6 = 1108;			 // 2048*sqrt(2)*cos(6*pi/16)
    private static final int w7 = 565;                   // 2048*sqrt(2)*cos(7*pi/16)
        
    
    private final int[] data;
    
    
    public DCT8()
    {
        this.data = new int[64];
    }
    
    
    public int[] forward(int[] block)
    {
        return this.forward(block, 0);
    }
    
    
    // Not thread safe
    @Override
    public int[] forward(int[] block, int blkptr)
    {
        int dataptr = 0;
        int end = blkptr + 64;
        
        // Aliasing for speed
        int[] data_ = this.data;
        
        // Pass 1: process rows.
        // Note results are scaled up by sqrt(8) compared to a true DCT;
        
        for (int i=blkptr; i<end; i+=8)
        {
            // Aliasing for speed
            int tmp8  = block[i];
            int tmp9  = block[i+1];
            int tmp10 = block[i+2];
            int tmp11 = block[i+3];
            int tmp12 = block[i+4];
            int tmp13 = block[i+5];
            int tmp14 = block[i+6];
            int tmp15 = block[i+7];
            
            int tmp0 = tmp8 + tmp15;
            int tmp7 = tmp8 - tmp15;
            int tmp1 = tmp9 + tmp14;
            int tmp6 = tmp9 - tmp14;
            int tmp2 = tmp10 + tmp13;
            int tmp5 = tmp10 - tmp13;
            int tmp3 = tmp11 + tmp12;
            int tmp4 = tmp11 - tmp12;
            
            // Even part          
            tmp10 = tmp0 + tmp3;
            tmp13 = tmp0 - tmp3;
            tmp11 = tmp1 + tmp2;
            tmp12 = tmp1 - tmp2;

            data_[dataptr]   = (tmp10 + tmp11) << PASS1_BITS;
            data_[dataptr+4] = (tmp10 - tmp11) << PASS1_BITS;
            
            int tmp = (tmp12 + tmp13) * CONST_0_541196100;
            int x;
            
            // descale
            x = tmp + (tmp13 * CONST_0_765366865);
            data_[dataptr+2] = (x + SCALE_SUB) >> SUB_BITS;
            x = tmp - (tmp12 * CONST_1_847759065);
            data_[dataptr+6] = (x + SCALE_SUB) >> SUB_BITS;
            
            // Odd part           
            int z1 = tmp4 + tmp7;
            int z2 = tmp5 + tmp6;
            int z3 = tmp4 + tmp6;
            int z4 = tmp5 + tmp7;
            int z5 = (z3 + z4) * CONST_1_175875602;	 // sqrt(2) * c3
            
            tmp4 *= CONST_0_298631336;	 // sqrt(2) * (-c1+c3+c5-c7)
            tmp5 *= CONST_2_053119869;	 // sqrt(2) * ( c1+c3-c5+c7)
            tmp6 *= CONST_3_072711026;	 // sqrt(2) * ( c1+c3+c5-c7)
            tmp7 *= CONST_1_501321110;	 // sqrt(2) * ( c1+c3-c5-c7)
            
            z1 *= -CONST_0_899976223;	 // sqrt(2) * (c7-c3)
            z2 *= -CONST_2_562915447;	 // sqrt(2) * (-c1-c3)
            z3 *= -CONST_1_961570560;	 // sqrt(2) * (-c3-c5)
            z4 *= -CONST_0_390180644;	 // sqrt(2) * (c5-c3)
            
            z3 += z5;
            z4 += z5;
            
            // descale
            x = tmp4 + z1 + z3;
            data_[dataptr+7] = (x + SCALE_SUB) >> SUB_BITS;
            x = tmp5 + z2 + z4;
            data_[dataptr+5] = (x + SCALE_SUB) >> SUB_BITS;
            x = tmp6 + z2 + z3;
            data_[dataptr+3] = (x + SCALE_SUB) >> SUB_BITS;
            x = tmp7 + z1 + z4;
            data_[dataptr+1] = (x + SCALE_SUB) >> SUB_BITS;
            
            dataptr += 8;
        }
        
        // Pass 2: process columns.
        // Remove the PASS1_BITS scaling, but leave the results scaled up by 8
        dataptr = 0;
        end = blkptr + 8;
        
        for (int i=blkptr; i<end; i++)
        {
            // Aliasing for speed
            int tmp8  = data_[dataptr];
            int tmp9  = data_[dataptr+8];
            int tmp10 = data_[dataptr+16];
            int tmp11 = data_[dataptr+24];
            int tmp12 = data_[dataptr+32];
            int tmp13 = data_[dataptr+40];
            int tmp14 = data_[dataptr+48];
            int tmp15 = data_[dataptr+56];
            
            int tmp0 = tmp8 + tmp15;
            int tmp7 = tmp8 - tmp15;
            int tmp1 = tmp9 + tmp14;
            int tmp6 = tmp9 - tmp14;
            int tmp2 = tmp10 + tmp13;
            int tmp5 = tmp10 - tmp13;
            int tmp3 = tmp11 + tmp12;
            int tmp4 = tmp11 - tmp12;
            
            // Even part          
            tmp10 = tmp0 + tmp3;
            tmp13 = tmp0 - tmp3;
            tmp11 = tmp1 + tmp2;
            tmp12 = tmp1 - tmp2;
            
            // descale
            int x = tmp10 + tmp11;
            data_[dataptr]     = (x + SCALE_PASS1) >> PASS1_BITS;
            x = tmp10 - tmp11;
            data_[dataptr+32]  = (x + SCALE_PASS1) >> PASS1_BITS;
            
            int tmp = (tmp12 + tmp13) * CONST_0_541196100;
            
            x = tmp + tmp13 * CONST_0_765366865;
            data_[dataptr+16]  = (x + SCALE_ADD) >> ADD_BITS;
            x = tmp - tmp12 * CONST_1_847759065;
            data_[dataptr+48]  = (x + SCALE_ADD) >> ADD_BITS;
            
            // Odd part             
            int z1 = tmp4 + tmp7;
            int z2 = tmp5 + tmp6;
            int z3 = tmp4 + tmp6;
            int z4 = tmp5 + tmp7;
            int z5 = (z3 + z4) * CONST_1_175875602;	 // sqrt(2) * c3
            
            tmp4 *= CONST_0_298631336;	 // sqrt(2) * (-c1+c3+c5-c7)
            tmp5 *= CONST_2_053119869;	 // sqrt(2) * ( c1+c3-c5+c7)
            tmp6 *= CONST_3_072711026;	 // sqrt(2) * ( c1+c3+c5-c7)
            tmp7 *= CONST_1_501321110;	 // sqrt(2) * ( c1+c3-c5-c7)
            z1 *= -CONST_0_899976223;	 // sqrt(2) * ( c7-c3)
            z2 *= -CONST_2_562915447;	 // sqrt(2) * (-c1-c3)
            z3 *= -CONST_1_961570560;	 // sqrt(2) * (-c3-c5)
            z4 *= -CONST_0_390180644;	 // sqrt(2) * ( c5-c3)
            
            z3 += z5;
            z4 += z5;
            
            // descale
            x = tmp4 + z1 + z3;
            data_[dataptr+56]  = (x + SCALE_ADD) >> ADD_BITS;
            x = tmp5 + z2 + z4;
            data_[dataptr+40]  = (x + SCALE_ADD) >> ADD_BITS;
            x = tmp6 + z2 + z3;
            data_[dataptr+24]  = (x + SCALE_ADD) >> ADD_BITS;
            x = tmp7 + z1 + z4;
            data_[dataptr+8]   = (x + SCALE_ADD) >> ADD_BITS;
            
            dataptr++;
        }
        
        end = blkptr + 64;
        dataptr = 0;
        
        // descale
        for (int i=blkptr; i<end; i+=8)
        {
            // Unroll the loop for speed
            block[i]   = (data_[dataptr]   + 4) >> 3;
            block[i+1] = (data_[dataptr+1] + 4) >> 3;
            block[i+2] = (data_[dataptr+2] + 4) >> 3;
            block[i+3] = (data_[dataptr+3] + 4) >> 3;
            block[i+4] = (data_[dataptr+4] + 4) >> 3;
            block[i+5] = (data_[dataptr+5] + 4) >> 3;
            block[i+6] = (data_[dataptr+6] + 4) >> 3;
            block[i+7] = (data_[dataptr+7] + 4) >> 3;
            dataptr += 8;
        }
        
        return block;
    }
    
    
        
    public int[] inverse(int[] block)
    {
        return this.inverse(block, 0);
    }
    
    
    @Override
    public int[] inverse(int[] block, int blkptr)
    {
        int end = blkptr + 64;
        
        // idct rows
        for (int i=blkptr; i<end; i+=8)
        {
            int x0, x8;
            int x1 = block[i+4] << 11;
            int x2 = block[i+6];
            int x3 = block[i+2];
            int x4 = block[i+1];
            int x5 = block[i+7];
            int x6 = block[i+5];
            int x7 = block[i+3];
            
            if ((x1 | x2 | x3 | x4 | x5 | x6 | x7) == 0)
            {
                int val = block[i] << 3;
                block[i]   = val;
                block[i+1] = val;
                block[i+2] = val;
                block[i+3] = val;
                block[i+4] = val;
                block[i+5] = val;
                block[i+6] = val;
                block[i+7] = val;
            }
            else
            {
                // for proper rounding in the fourth stage
                x0 = (block[i] << 11) + 128;
                
                // first stage
                x8 = w7 * (x4 + x5);
                x4 = x8 + (w1 - w7) * x4;
                x5 = x8 - (w1 + w7) * x5;
                x8 = w3 * (x6 + x7);
                x6 = x8 - (w3 - w5) * x6;
                x7 = x8 - (w3 + w5) * x7;
                
                // second stage
                x8 = x0 + x1;
                x0 -= x1;
                x1 = w6 * (x3 + x2);
                x2 = x1 - (w2 + w6) * x2;
                x3 = x1 + (w2 - w6) * x3;
                x1 = x4 + x6;
                x4 -= x6;
                x6 = x5 + x7;
                x5 -= x7;
                
                // third stage
                x7 = x8 + x3;
                x8 -= x3;
                x3 = x0 + x2;
                x0 -= x2;
                x2 = (181 * (x4 + x5) + 128) >> 8;
                x4 = (181 * (x4 - x5) + 128) >> 8;
                
                // fourth stage
                block[i]   = (x7 + x1) >> 8;
                block[i+1] = (x3 + x2) >> 8;
                block[i+2] = (x0 + x4) >> 8;
                block[i+3] = (x8 + x6) >> 8;
                block[i+4] = (x8 - x6) >> 8;
                block[i+5] = (x0 - x4) >> 8;
                block[i+6] = (x3 - x2) >> 8;
                block[i+7] = (x7 - x1) >> 8;
            }
        }
        
        end = blkptr + 8;
        
        // idct columns
        for (int i=blkptr; i<end; i++)
        {
            int x0, x8;
            int x1 = block[i+32] << 8;
            int x2 = block[i+48];
            int x3 = block[i+16];
            int x4 = block[i+8];
            int x5 = block[i+56];
            int x6 = block[i+40];
            int x7 = block[i+24];
            
            if ((x1 | x2 | x3 | x4 | x5 | x6 | x7) == 0)
            {
                int val = (block[i]+32) >> 6;
                val = (val >= 255) ? 255 : (val < -256) ? -256 : val;
                block[i]    = val;
                block[i+8]  = val;
                block[i+16] = val;
                block[i+24] = val;
                block[i+32] = val;
                block[i+40] = val;
                block[i+48] = val;
                block[i+56] = val;
            }
            else
            {
                x0 = (block[i] << 8) + 8192;
                
                // first stage
                x8 = w7 * (x4 + x5) + 4;
                x4 = (x8 + (w1 - w7) * x4) >> 3;
                x5 = (x8 - (w1 + w7) * x5) >> 3;
                x8 = w3 * (x6 + x7) + 4;
                x6 = (x8 - (w3 - w5) * x6) >> 3;
                x7 = (x8 - (w3 + w5) * x7) >> 3;
                
                // second stage
                x8 = x0 + x1;
                x0 -= x1;
                x1 = w6 * (x3 + x2) + 4;
                x2 = (x1 - (w2 + w6) * x2) >> 3;
                x3 = (x1 + (w2 - w6) * x3) >> 3;
                x1 = x4 + x6;
                x4 -= x6;
                x6 = x5 + x7;
                x5 -= x7;
                
                // third stage
                x7 = x8 + x3;
                x8 -= x3;
                x3 = x0 + x2;
                x0 -= x2;
                x2 = (181 * (x4 + x5) + 128) >> 8;
                x4 = (181 * (x4 - x5) + 128) >> 8;
                
                // fourth stage
                int val;
                val = (x7 + x1) >> 14;
                block[i]    = (val >= 255) ? 255 : (val < -256) ? -256 : val;
                val = (x3 + x2) >> 14;
                block[i+8]  = (val >= 255) ? 255 : (val < -256) ? -256 : val;
                val = (x0 + x4) >> 14;
                block[i+16] = (val >= 255) ? 255 : (val < -256) ? -256 : val;
                val = (x8 + x6) >> 14;
                block[i+24] = (val >= 255) ? 255 : (val < -256) ? -256 : val;
                val = (x8 - x6) >> 14;
                block[i+32] = (val >= 255) ? 255 : (val < -256) ? -256 : val;
                val = (x0 - x4) >> 14;
                block[i+40] = (val >= 255) ? 255 : (val < -256) ? -256 : val;
                val = (x3 - x2) >> 14;
                block[i+48] = (val >= 255) ? 255 : (val < -256) ? -256 : val;
                val = (x7 - x1) >> 14;
                block[i+56] = (val >= 255) ? 255 : (val < -256) ? -256 : val;
            }
        }
        
        return block;
    }
    
}