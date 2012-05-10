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


// Walsh-Hadamard transform of dimension 8
public final class WHT8 implements IntTransform
{
    private final int[] data;


    public WHT8()
    {
        this.data = new int[64];
    }


    public int[] forward(int[] block)
    {
        return this.compute(block, 0);
    }


    // Not thread safe
    @Override
    public int[] forward(int[] block, int blkptr)
    {
        return this.compute(block, blkptr);
    }


    // Not thread safe
    // Result multiplied by sqrt(2)
    private int[] compute(int[] block, int blkptr)
    {
        int dataptr = 0;
        int end = blkptr + 64;
        int[] buffer = this.data;

        // Pass 1: process rows.
        for (int i=blkptr; i<end; i+=8)
        {
            // Aliasing for speed
            int x0 = block[i];
            int x1 = block[i+1];
            int x2 = block[i+2];
            int x3 = block[i+3];
            int x4 = block[i+4];
            int x5 = block[i+5];
            int x6 = block[i+6];
            int x7 = block[i+7];

            int a0 = x0 + x1;
            int a1 = x2 + x3;
            int a2 = x4 + x5;
            int a3 = x6 + x7;
            int a4 = x0 - x1;
            int a5 = x2 - x3;
            int a6 = x4 - x5;
            int a7 = x6 - x7;

            int b0 = a0 + a1;
            int b1 = a2 + a3;
            int b2 = a4 + a5;
            int b3 = a6 + a7;
            int b4 = a0 - a1;
            int b5 = a2 - a3;
            int b6 = a4 - a5;
            int b7 = a6 - a7;

            buffer[dataptr]   = b0 + b1;
            buffer[dataptr+1] = b2 + b3;
            buffer[dataptr+2] = b4 + b5;
            buffer[dataptr+3] = b6 + b7;
            buffer[dataptr+4] = b0 - b1;
            buffer[dataptr+5] = b2 - b3;
            buffer[dataptr+6] = b4 - b5;
            buffer[dataptr+7] = b6 - b7;

            dataptr += 8;
        }

        dataptr = 0;
        end = blkptr + 8;

        // Pass 2: process columns.
        for (int i=blkptr; i<end; i++)
        {
            // Aliasing for speed
            int x0 = buffer[dataptr];
            int x1 = buffer[dataptr+8];
            int x2 = buffer[dataptr+16];
            int x3 = buffer[dataptr+24];
            int x4 = buffer[dataptr+32];
            int x5 = buffer[dataptr+40];
            int x6 = buffer[dataptr+48];
            int x7 = buffer[dataptr+56];

            int a0 = x0 + x1;
            int a1 = x2 + x3;
            int a2 = x4 + x5;
            int a3 = x6 + x7;
            int a4 = x0 - x1;
            int a5 = x2 - x3;
            int a6 = x4 - x5;
            int a7 = x6 - x7;

            int b0 = a0 + a1;
            int b1 = a2 + a3;
            int b2 = a4 + a5;
            int b3 = a6 + a7;
            int b4 = a0 - a1;
            int b5 = a2 - a3;
            int b6 = a4 - a5;
            int b7 = a6 - a7;

            block[i]    = (b0 + b1 + 4) >> 3;
            block[i+8]  = (b2 + b3 + 4) >> 3;
            block[i+16] = (b4 + b5 + 4) >> 3;
            block[i+24] = (b6 + b7 + 4) >> 3;
            block[i+32] = (b0 - b1 + 4) >> 3;
            block[i+40] = (b2 - b3 + 4) >> 3;
            block[i+48] = (b4 - b5 + 4) >> 3;
            block[i+56] = (b6 - b7 + 4) >> 3;

            dataptr++;
        }

        return block;
    }


    // Yep, the transform is symetric
    public int[] inverse(int[] block)
    {
        return this.compute(block, 0);
    }


    @Override
    public int[] inverse(int[] block, int blkptr)
    {
        return this.compute(block, blkptr);
    }

}