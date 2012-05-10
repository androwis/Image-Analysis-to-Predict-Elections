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


public class JNI_VideoEffect implements VideoEffectWithOffset
{
    static { System.loadLibrary("jniVideoEffect"); }

    private final int width;
    private final int height;
    private final int stride;
    private int offset;


    public JNI_VideoEffect(int width, int height, int offset, int stride)
    {
        if (height < 8)
            throw new IllegalArgumentException("The height must be at least 8");

        if (width < 8)
            throw new IllegalArgumentException("The width must be at least 8");

        if (offset < 0)
            throw new IllegalArgumentException("The offset must be at least 0");

        if (stride < 8)
            throw new IllegalArgumentException("The stride must be at least 8");

        this.height = height;
        this.width = width;
        this.stride = stride;
        this.offset = offset;
    }


    // Implement the filter in C/C++/ASM !
    public native int[] native_apply(int width, int height, int stride, int[] src, int[] dst);


    @Override
    public int[] apply(int[] src, int[] dst)
    {
        return native_apply(this.width, this.height, this.stride, src, dst);
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