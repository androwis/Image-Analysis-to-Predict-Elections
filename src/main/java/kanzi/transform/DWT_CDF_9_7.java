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


// Discrete Wavelet Transform Cohen-Daubechies-Fauveau 9/7 for 2D signals
public class DWT_CDF_9_7 implements IntTransform
{
    private static final int SHIFT  = 12;
    private static final int ADJUST = 1 << (SHIFT - 1);

    private static final int PREDICT_1 = 6497; // with SHIFT = 12
    private static final int UPDATE_1  = 217;  // with SHIFT = 12
    private static final int PREDICT_2 = 3616; // with SHIFT = 12
    private static final int UPDATE_2  = 1817; // with SHIFT = 12
    private static final int SCALING_1 = 4709; // with SHIFT = 12
    private static final int SCALING_2 = 3562; // with SHIFT = 12

    private final int[] data;
    private final int width;
    private final int height;
    private final int steps;


    // dim (dimension of the whole image) must be a power of 2
    public DWT_CDF_9_7(int dim)
    {
       this(dim, dim, 5);
    }
    
    
    // dim (dimension of the whole image) must be a power of 2
    public DWT_CDF_9_7(int width, int height)
    {
       this(width, height, 5);
    }


    public DWT_CDF_9_7(int width, int height, int steps)
    {
        if (width < 8)
            throw new IllegalArgumentException("Invalid transform width (must"
                    + " be at least 8)");

        if (height < 8)
            throw new IllegalArgumentException("Invalid transform width (must"
                    + " be at least 8)");

        if (steps < 2)
            throw new IllegalArgumentException("Invalid number of iterations "
                    + "(must be a least 2)");

        if ((width >> steps) < 8)
            throw new IllegalArgumentException("Invalid width for band L0 (must"
                    + " be at least 8)");

        if ((height >> steps) < 8)
            throw new IllegalArgumentException("Invalid height for band L0 (must"
                    + " be at least 8)");

        if (((width >> steps) << steps) != width)
            throw new IllegalArgumentException("Invalid parameters: change width or number of steps (" 
                    + width + " divided by 2^" + steps + " is not an integer value)");

        if (((height >> steps) << steps) != height)
            throw new IllegalArgumentException("Invalid parameters: change height or number of steps (" 
                    + height + " divided by 2^" + steps + " is not an integer value)");

        this.width = width;
        this.height = height;
        this.steps = steps;
        this.data = new int[width*height];
    }


    public int getWidth()
    {
        return this.width;
    }


    public int getHeight()
    {
        return this.height;
    }

    
    public int getLevels()
    {
        return this.steps;
    }


    // Calculate the forward discrete wavelet transform of the 2D input signal
    // Not thread safe because this.data is modified
    @Override
    public int[] forward(int[] block, int blkptr)
    {
        for (int i=0; i<this.steps; i++)
        {
           // First, vertical transform
           block = forward(block, blkptr, this.width, 1, this.width>>i, this.height>>i);

           // Then horizontal transform on the updated signal
           block = forward(block, blkptr, 1, this.width, this.height>>i, this.width>>i);
        }

        return block;
    }


    private int[] forward(int[] block, int blkptr, int stride, int inc, int dim1, int dim2)
    {
        final int stride2 = stride << 1;
        final int endOffs = blkptr + (dim1 * inc);
        final int half = stride * (dim2  >> 1);
        
        for (int offset=blkptr; offset<endOffs; offset+=inc)
        {
            final int end = offset + (dim2 - 2) * stride;
            long tmp;
            int prev = block[offset];
            
            // First lifting stage : Predict 1
            for (int i=offset+stride; i<end; i+=stride2)
            {
                final int next = block[i+stride];
                tmp = (PREDICT_1 * (prev + next));
                block[i] -= ((tmp + ADJUST) >> SHIFT);
                prev = next;
            }

            tmp = PREDICT_1 * block[end] ;
            block[end+stride] -= (((tmp + tmp) + ADJUST) >> SHIFT);
            prev = block[offset+stride];

            // Second lifting stage : Update 1
            for (int i=offset+stride2; i<=end; i+=stride2)
            {
                int next = block[i+stride];
                tmp = UPDATE_1 * (prev + next);
                block[i] -= ((tmp + ADJUST) >> SHIFT);
                prev = next;
            }

            tmp = UPDATE_1 * block[offset+stride];
            block[offset] -= (((tmp + tmp) + ADJUST) >> SHIFT);
            prev = block[offset];

            // Third lifting stage : Predict 2
            for (int i=offset+stride; i<end; i+=stride2)
            {
                final int next = block[i+stride];
                tmp = PREDICT_2 * (prev + next);
                block[i] += ((tmp + ADJUST) >> SHIFT);
                prev = next;
            }

            tmp = PREDICT_2 * block[end];
            block[end+stride] += (((tmp + tmp) + ADJUST) >> SHIFT);
            prev = block[offset+stride];

            // Fourth lifting stage : Update 2
            for (int i=offset+stride2; i<=end; i+=stride2)
            {
                final int next = block[i+stride];
                tmp = (UPDATE_2 * (prev + next));
                block[i] += ((tmp + ADJUST) >> SHIFT);
                prev = next;
            }

            tmp = UPDATE_2 * block[offset+stride];
            block[offset] += (((tmp + tmp) + ADJUST) >> SHIFT);

            // Scale
            for (int i=offset; i<=end; i+=stride2)
            {
                tmp = block[i] * SCALING_1;
                block[i] = (int) ((tmp + ADJUST) >> SHIFT);
                tmp = block[i+stride] * SCALING_2;
                block[i+stride] = (int) ((tmp + ADJUST) >> SHIFT);
            }

            // De-interleave sub-bands
            final int endj = offset + half;

            for (int i=offset, j=offset; j<endj; i+=stride2, j+=stride)
            {
                this.data[j] = block[i];
                this.data[half+j] = block[i+stride];
            }

            block[end+stride] = this.data[end+stride];

            for (int i=offset; i<=end; i+=stride)
                block[i] = this.data[i];
        }

        return block;
    }


    // Calculate the reverse discrete wavelet transform of the 2D input signal
    // Not thread safe because this.data is modified
    @Override
    public int[] inverse(int[] block, int blkptr)
    {
        for (int i=this.steps-1; i>=0; i--)
        {
           // First horizontal transform
           block = inverse(block, blkptr, 1, this.width, this.height>>i, this.width>>i);

           // Then vertical transform on the updated signal
           block = inverse(block, blkptr, this.width, 1, this.width>>i, this.height>>i);
        }

        return block;
    }


    private int[] inverse(int[] block, int blkptr, int stride, int inc, int dim1, int dim2)
    {
        final int stride2 = stride << 1;
        final int endOffs = blkptr + (dim1 * inc);
        final int half = stride * (dim2 >> 1);

        for (int offset=blkptr; offset<endOffs; offset+=inc)
        {
            final int end = offset + (dim2 - 2) * stride;
            final int endj = offset + half;
            long tmp;

            // Interleave sub-bands
            for (int i=offset; i<=end; i+=stride)
                this.data[i] = block[i];

            this.data[end+stride] = block[end+stride];

            for (int i=offset, j=offset; j<endj; i+=stride2, j+=stride)
            {
                block[i] = this.data[j];
                block[i+stride] = this.data[half+j];
            }

            // Reverse scale
            for (int i=offset; i<=end; i+=stride2)
            {
                tmp = block[i] * SCALING_2;
                block[i] = (int) ((tmp + ADJUST) >> SHIFT);
                tmp = block[i+stride] * SCALING_1;
                block[i+stride] = (int) ((tmp + ADJUST) >> SHIFT);
            }

            // Reverse Update 2
            int prev = block[offset+stride];

            for (int i=offset+stride2; i<=end; i+=stride2)
            {
                int next = block[i+stride];
                tmp = (UPDATE_2 * (prev + next));
                block[i] -= ((tmp + ADJUST) >> SHIFT);
                prev = next;
            }

            tmp = UPDATE_2 * block[offset+stride];
            block[offset] -= (((tmp + tmp) + ADJUST) >> SHIFT);
            prev = block[offset];

            // Reverse Predict 2
            for (int i=offset+stride; i<end; i+=stride2)
            {
                final int next = block[i+stride];
                tmp = PREDICT_2 * (prev + next);
                block[i] -= ((tmp + ADJUST) >> SHIFT);
                prev = next;
            }

            tmp = PREDICT_2 * block[end];
            block[end+stride] -= (((tmp + tmp) + ADJUST) >> SHIFT);
            prev = block[offset+stride];

            // Reverse Update 1
            for (int i=offset+stride2; i<=end; i+=stride2)
            {
                final int next = block[i+stride];
                tmp = UPDATE_1 * (prev + next);
                block[i] += ((tmp + ADJUST) >> SHIFT);
                prev = next;
            }

            tmp = UPDATE_1 * block[offset+stride];
            block[offset] += (((tmp + tmp) + ADJUST) >> SHIFT);
            prev = block[offset];

            // Reverse Predict 1
            for (int i=offset+stride; i<end; i+=stride2)
            {
                final int next = block[i+stride];
                tmp = PREDICT_1 * (prev + next);
                block[i] += ((tmp + ADJUST) >> SHIFT);
                prev = next;
            }

            tmp = PREDICT_1 * block[end];
            block[end+stride] += (((tmp + tmp) + ADJUST) >> SHIFT);
        }

        return block;
    }

}