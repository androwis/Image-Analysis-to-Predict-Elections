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

public interface ColorModelConverter
{
    public boolean convertRGBtoYUV444(int[] rgb, int[] y, int[] u, int[] v);


    public boolean convertYUV444toRGB(int[] y, int[] u, int[] v, int[] rgb);
    

    // In YUV422 format the U and V color components are supersampled 2:1 horizontally
    public boolean convertYUV422toYUV444(int[] y, int[] u, int[] v);

    // In YUV422 format the U and V color components are subsampled 1:2 horizontally
    public boolean convertYUV444toYUV422(int[] y, int[] u, int[] v);

    // In YUV420 format the U and V color components are supersampled 2:1 horizontally
    // and 2:1 vertically
    public boolean convertYUV420toYUV444(int[] y, int[] u, int[] v);

    // In YUV420 format the U and V color components are subsampled 1:2 horizontally
    // and 1:2 vertically
    public boolean convertYUV444toYUV420(int[] y, int[] u, int[] v);

    // In YUV420 format the U and V color components are subsampled 1:2 horizontally
    // and 1:2 vertically
    public boolean convertYUV420toRGB(int[] y, int[] u, int[] v, int[] rgb);

    // In YUV420 format the U and V color components are subsampled 1:2 horizontally
    // and 1:2 vertically
    public boolean convertRGBtoYUV420(int[] rgb, int[] y, int[] u, int[] v);

    // In YUV422 format the U and V color components are subsampled 1:2 horizontally
    public boolean convertYUV422toRGB(int[] y, int[] u, int[] v, int[] rgb);

    // In YUV422 format the U and V color components are subsampled 1:2 horizontally
    public boolean convertRGBtoYUV422(int[] rgb, int[] y, int[] u, int[] v);
}
