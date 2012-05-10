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

package kanzi.filter.seam;


import kanzi.VideoEffect;
import kanzi.IntSorter;
import kanzi.filter.SobelFilter;
import kanzi.util.sort.BucketSort;
import kanzi.util.sort.RadixSort;


// Based on algorithm by Shai Avidan, Ariel Shamir
// Described in [Seam Carving for Content-Aware Image Resizing]
//
// This implementation is focused on speed and is indeed very fast (but it does
// only calculate an approximation of the energy minimizing paths because finding
// the absolute best paths takes too much time)
// It is also possible to calculate the seams on a subset of the image which is
// useful to iterate over the same (shrinking) image.
//
// Note: the name seam carving is a bit unfortunate, what the algo achieves
// is detection and removal of the paths of least resistance (energy wise) in
// the image. These paths really are geodesics.
public class ContextResizer implements VideoEffect
{
    // Possible directions
    public static final int HORIZONTAL = 1;
    public static final int VERTICAL = 2;

    // Possible actions
    public static final int SHRINK = 1;
    public static final int EXPAND = 2;

    private static final int USED_MASK = 0x80000000;
    private static final int VALUE_MASK = USED_MASK - 1;
    private static final int DEFAULT_BEST_COST = 0x0FFFFFFF;
    private static final int DEFAULT_MAX_COST_PER_PIXEL = 256;

    private int width;
    private int height;
    private final int stride;
    private final int direction;
    private final int maxSearches;
    private final int maxAvgGeoPixCost;
    private final int[] costs;
    private final int nbGeodesics;
    private int offset;
    private int action;
    private boolean debug;
    private final IntSorter sorter;


    public ContextResizer(int width, int height, int direction, int action)
    {
        this(width, height, 0, width, direction, action, 1, 1);
    }


    // width, height, offset and stride allow to apply the filter on a subset of an image
    public ContextResizer(int width, int height, int offset, int stride,
            int direction, int action, int maxSearches, int nbGeodesics)
    {
        this(width, height, offset, stride, direction, action, maxSearches,
                nbGeodesics, DEFAULT_MAX_COST_PER_PIXEL);
    }


    // width, height, offset and stride allow to apply the filter on a subset of an image
    // maxAvgGeoPixCost allows to limit the cost of geodesics: only those with an
    // average cost per pixel less than maxAvgGeoPixCost are allowed (it may be
    // less than nbGeodesics).
    public ContextResizer(int width, int height, int offset, int stride,
            int direction, int action, int maxSearches, int nbGeodesics,
            int maxAvgGeoPixCost)
    {
        if (height < 8)
            throw new IllegalArgumentException("The height must be at least 8");

        if (width < 8)
            throw new IllegalArgumentException("The width must be at least 8");

        if (offset < 0)
            throw new IllegalArgumentException("The offset must be at least 0");

        if (stride < 8)
            throw new IllegalArgumentException("The stride must be at least 8");

        if (maxAvgGeoPixCost < 1)
            throw new IllegalArgumentException("The max average pixel cost in a geodesic must be at least 1");

        if (nbGeodesics < 1)
            throw new IllegalArgumentException("The number of geodesics must be at least 1");

        if (nbGeodesics > width)
            throw new IllegalArgumentException("The number of geodesics must be at most 'width'");

        if (((direction & HORIZONTAL) == 0) && ((direction & VERTICAL) == 0))
            throw new IllegalArgumentException("Invalid direction parameter (must be VERTICAL or HORIZONTAL)");

        if ((action != SHRINK) && (action != EXPAND))
            throw new IllegalArgumentException("Invalid action parameter (must be SHRINK or EXPAND)");

        if ((direction & HORIZONTAL) == 0)
        {
           if ((maxSearches < 1) || (maxSearches > width))
              throw new IllegalArgumentException("The number of checks must be in the [1.."+width+"] range");
        }
        else
        {
           if ((maxSearches < 1) || (maxSearches > height))
              throw new IllegalArgumentException("The number of checks must be in the [1.."+height+"] range");
        }

        this.height = height;
        this.width = width;
        this.offset = offset;
        this.stride = stride;
        this.direction = direction;
        this.maxSearches = maxSearches;
        this.costs = new int[stride*height];
        this.nbGeodesics = nbGeodesics;
        this.maxAvgGeoPixCost = maxAvgGeoPixCost;
        this.action = action;
        int dim = (height >= width) ? height : width;
        int log = 0;

        for (long val=dim+1; val>1; val>>=1)
          log++;

        if ((dim & (dim-1)) != 0)
            log++;

        // Used to sort coordinates of geodesics
        this.sorter = (log < 12) ? new BucketSort(0, log) : new RadixSort(8, 0, log);
    }


    public int getWidth()
    {
        return this.width;
    }


    public int getHeight()
    {
        return this.height;
    }


    public boolean getDebug()
    {
        return this.debug;
    }


    // Not thread safe
    public boolean setDebug(boolean debug)
    {
        this.debug = debug;
        return true;
    }


    public int getOffset()
    {
        return this.offset;
    }


    // Not thread safe
    public boolean setOffset(int offset)
    {
        if (offset < 0)
            return false;

        this.offset = offset;
        return true;
    }


    public int getAction()
    {
        return this.action;
    }


    // Not thread safe
    public boolean setAction(int action)
    {
        if ((action != SHRINK) && (action != EXPAND))
            return false;

        this.action = action;
        return true;
    }


    public int[] shrink(int[] src, int[] dst)
    {
        this.setAction(SHRINK);
        return this.shrink_(src, dst);
    }


    public int[] expand(int[] src, int[] dst)
    {
        this.setAction(EXPAND);
        return this.expand_(src, dst);
    }


    // Will modify the width and/or height attributes
    // The src image is modified if both directions are selected
    @Override
    public int[] apply(int[] src, int[] dst)
    {
       return (this.action == SHRINK) ? this.shrink_(src, dst) : this.expand_(src, dst);
    }


    // Will increase the width and/or height attributes
    private int[] expand_(int[] src, int[] dst)
    {
        int processed = 0;

        if ((this.direction & VERTICAL) != 0)
        {
            Geodesic[] geodesics = this.computeGeodesics(src, VERTICAL);

            if (geodesics.length > 0)
            {
                processed += geodesics.length;
                this.addGeodesics(geodesics, src, dst, VERTICAL);
            }
        }

        if ((this.direction & HORIZONTAL) != 0)
        {
            if ((this.direction & VERTICAL) != 0)
                src = dst;

            Geodesic[] geodesics = this.computeGeodesics(src, HORIZONTAL);

            if (geodesics.length > 0)
            {
                processed += geodesics.length;
                this.addGeodesics(geodesics, src, dst, HORIZONTAL);
            }
        }

        if (processed == 0)
        {
           System.arraycopy(src, this.offset, dst, this.offset, this.height*this.stride);
        }

        return dst;
    }


    // Will decrease the width and/or height attributes
    private int[] shrink_(int[] src, int[] dst)
    {
        int processed = 0;

        if ((this.direction & VERTICAL) != 0)
        {
            Geodesic[] geodesics = this.computeGeodesics(src, VERTICAL);

            if (geodesics.length > 0)
            {
               processed += geodesics.length;
               this.removeGeodesics(geodesics, src, dst, VERTICAL);
            }
        }

        if ((this.direction & HORIZONTAL) != 0)
        {
            if ((this.direction & VERTICAL) != 0)
            {
                int[] tmp = src;
                src = dst;
                dst = tmp;
            }
            
            Geodesic[] geodesics = this.computeGeodesics(src, HORIZONTAL);

            if (geodesics.length > 0)
            {
               processed += geodesics.length;
               this.removeGeodesics(geodesics, src, dst, HORIZONTAL);
            }
        }

        if (processed == 0)
        {
           System.arraycopy(src, this.offset, dst, this.offset, this.height*this.stride);
        }

        return dst;
    }


    // dir must be either VERTICAL or HORIZONTAL
    public void addGeodesics(Geodesic[] geodesics, int[] src, int[] dst, int dir)
    {
        if (geodesics.length == 0)
            return;

        if (dir == VERTICAL)
            this.width += geodesics.length;
        else
            this.height += geodesics.length;

        int srcStart = this.offset;
        int dstStart = this.offset;
        int[] linePositions = new int[geodesics.length];

        // Default values for VERTICAL
        int endj = this.height;
        int endi = this.width;
        int incStart = this.stride;
        int incIdx = 1;
        int color = 0xFFFF0000;

        if (dir == HORIZONTAL)
        {
            endj = this.width;
            endi = this.height;
            incStart = 1;
            incIdx = this.stride;
            color = 0xFF0000FF;
        }

        for (int j=0; j<endj; j++)
        {
            // Find all the pixels belonging to geodesics in this line
            for (int k=0; k<linePositions.length; k++)
                linePositions[k] = geodesics[k].positions[j];

            // Sort the pixels by increasing position
            if (linePositions.length > 1)
                this.sorter.sort(linePositions, 0);

            int posIdx = 0;
            int pos = linePositions[posIdx];
            int srcIdx = srcStart;
            int dstIdx = dstStart;
            int lastGeoPixPos = linePositions[linePositions.length-1];

            // Start the line, no test for geodesic pixels required
            if ((dir == VERTICAL) && (pos >= 32))
            {
               // Speed up copy
               System.arraycopy(src, srcIdx, dst, dstIdx, pos);
            }
            else
            {
               // Either incIdx != 1 or not enough pixels for arraycopy to be worth it
               int pos4 = pos & -4;

               for (int i=0; i<pos4; i+=4)
               {
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
               }

               for (int i=pos4; i<pos; i++)
               {
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
               }
            }

            int previous = (srcIdx > srcStart) ? src[srcIdx-incIdx] : src[srcStart];
            int r_prev = (previous >> 16) & 0xFF;
            int g_prev = (previous >> 8)  & 0xFF;
            int b_prev =  previous & 0xFF;

            for (int x=pos; x<=lastGeoPixPos; )
            {
                // Is the pixel part of a geodesic ?
                if (x == pos)
                {
                    // Insert new pixel into the destination
                    if (this.debug == true)
                    {
                       dst[dstIdx] = color;
                    }
                    else
                    {
                       int r = r_prev;
                       int g = g_prev;
                       int b = b_prev;
                       previous = src[srcIdx];
                       r_prev = (previous >> 16) & 0xFF;
                       g_prev = (previous >> 8)  & 0xFF;
                       b_prev =  previous & 0xFF;
                       r = (r + r_prev) >> 1;
                       g = (g + g_prev) >> 1;
                       b = (b + b_prev) >> 1;
                       dst[dstIdx] = (r << 16) | (g << 8) | b;
                    }

                    dstIdx += incIdx;

                    // Get the next pixel insertion position in this line
                    if (x != lastGeoPixPos)
                    {
                        posIdx++;
                        pos = linePositions[posIdx];
                    }

                    x++;
                }
                else
                {
                   int pos4 = (pos-x) & -4;

                   for (int i=0; i<pos4; i+=4)
                   {
                       dst[dstIdx] = src[srcIdx];
                       dstIdx += incIdx;
                       srcIdx += incIdx;
                       dst[dstIdx] = src[srcIdx];
                       dstIdx += incIdx;
                       srcIdx += incIdx;
                       dst[dstIdx] = src[srcIdx];
                       dstIdx += incIdx;
                       srcIdx += incIdx;
                       dst[dstIdx] = src[srcIdx];
                       dstIdx += incIdx;
                       srcIdx += incIdx;
                   }

                   for (int i=pos4; i<pos-x; i++)
                   {
                       dst[dstIdx] = src[srcIdx];
                       dstIdx += incIdx;
                       srcIdx += incIdx;
                   }

                   x = pos;
                }
            }

            // Finish the line, no more test for geodesic pixels required
            if ((dir == VERTICAL) && (endi - lastGeoPixPos >= 32))
            {
               // Speed up copy
               System.arraycopy(src, srcIdx, dst, dstIdx, endi-lastGeoPixPos-1);
            }
            else
            {
               // Either incIdx != 1 or not enough pixels for arraycopy to be worth it
               int endi4 = endi & -4;

               for (int i=lastGeoPixPos+1; i<endi4; i+=4)
               {
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
               }

               for (int i=endi4; i<endi; i++)
               {
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
               }
            }

            srcStart += incStart;
            dstStart += incStart;
        }
    }


    // dir must be either VERTICAL or HORIZONTAL
    public void removeGeodesics(Geodesic[] geodesics, int[] src, int[] dst, int dir)
    {
        if (geodesics.length == 0)
            return;

        int[] linePositions = new int[geodesics.length];

        // Default values for VERTICAL
        int endj = this.height;
        int endLine = this.width;
        int incIdx = 1;
        int incStart = this.stride;
        int color = 0xFFFF0000;

        if (dir == HORIZONTAL)
        {
            endj = this.width;
            endLine = this.height;
            incIdx = this.stride;
            incStart = 1;
            color = 0xFF0000FF;
        }

        int srcStart = this.offset;
        int dstStart = this.offset;

        for (int j=0; j<endj; j++)
        {
            // Find all the pixels belonging to geodesics in this line
            for (int k=0; k<linePositions.length; k++)
                linePositions[k] = geodesics[k].positions[j];

            // Sort the pixels by increasing position
            if (linePositions.length > 1)
                this.sorter.sort(linePositions, 0);

            int srcIdx = srcStart;
            int dstIdx = dstStart;
            int posIdx = 0;
            int nextPos = linePositions[posIdx];
            int endPosIdx = linePositions.length - 1;
            int pos = 0;

            while (pos < endLine)
            {
                int len = nextPos - pos;

                // Is the pixel part of a geodesic ?
                if (len != 0)
                {
                    if ((dir == VERTICAL) && (len >= 32))
                    {
                       // Speed up copy
                       System.arraycopy(src, srcIdx, dst, dstIdx, len);
                       srcIdx += len;
                       dstIdx += len;
                    }
                    else
                    {
                       // Either incIdx != 1 or not enough pixels for arraycopy to be worth it
                       int len4 = len & -4;

                       for (int i=0; i<len4; i+=4)
                       {
                           dst[dstIdx] = src[srcIdx];
                           dstIdx += incIdx;
                           srcIdx += incIdx;
                           dst[dstIdx] = src[srcIdx];
                           dstIdx += incIdx;
                           srcIdx += incIdx;
                           dst[dstIdx] = src[srcIdx];
                           dstIdx += incIdx;
                           srcIdx += incIdx;
                           dst[dstIdx] = src[srcIdx];
                           dstIdx += incIdx;
                           srcIdx += incIdx;
                       }

                       for (int i=len4; i<len; i++)
                       {
                           dst[dstIdx] = src[srcIdx];
                           dstIdx += incIdx;
                           srcIdx += incIdx;
                       }
                    }

                    pos = nextPos;
                }

                // Color the pixel or remove it ?
                if (this.debug == true)
                {
                    dst[dstIdx] = color;
                    dstIdx += incIdx;
                }

                pos++;
                srcIdx += incIdx;

                // Get the next pixel removal position in this line
                if (posIdx >= endPosIdx)
                    break;

                posIdx++;
                nextPos = linePositions[posIdx];
            }

            int len = endLine - pos;

            // Finish the line, no more test for geodesic pixels required
            if ((dir == VERTICAL) && (len >= 32))
            {
               // Speed up copy
               System.arraycopy(src, srcIdx, dst, dstIdx, len);
               srcIdx += len;
               dstIdx += len;
            }
            else
            {
               // Either incIdx != 1 or not enough pixels for arraycopy to be worth it
               int len4 = len & -4;

               for (int i=0; i<len4; i+=4)
               {
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
               }

               for (int i=len4; i<len; i++)
               {
                   dst[dstIdx] = src[srcIdx];
                   dstIdx += incIdx;
                   srcIdx += incIdx;
               }
            }

            srcStart += incStart;
            dstStart += incStart;
        }

//        if (this.debug == false)
//        {
//            if (dir == VERTICAL)
//                this.width -= geodesics.length;
//            else
//                this.height -= geodesics.length;
//        }
    }


    // dir must be either VERTICAL or HORIZONTAL
    public Geodesic[] computeGeodesics(int[] src, int dir)
    {
        int dim = (dir == HORIZONTAL) ? this.height : this.width;
        int[] firstPositions = new int[this.maxSearches];
        int n = 0;

        // Spread the first position along 'direction' for better uniformity
        // Should improve speed by detecting faster low cost paths and reduce
        // geodesic crossing management.
        // It will improve quality by spreading the search over the whole image
        // if maxSearches is small.
        for (int i=0; ((n<this.maxSearches) && (i<24)); i+=3)
        {
            // i & 7 shuffles the start position : 0, 3, 6, 1, 4, 7, 2, 5
            for (int j=(i & 7); ((n<this.maxSearches) && (j<dim)); j+=8)
                firstPositions[n++] = j;
        }

        return this.computeGeodesics_(src, dir, firstPositions, this.maxSearches);
    }


    // Compute the geodesics but give a constraint on where to start from
    // All first position values must be different
    // dir must be either VERTICAL or HORIZONTAL
    public Geodesic[] computeGeodesics(int[] src, int dir, int[] firstPositions)
    {
        return this.computeGeodesics_(src, dir, firstPositions, this.maxSearches);
    }


    private Geodesic[] computeGeodesics_(int[] src, int dir, int[] firstPositions, int maxSearches)
    {
        if ((maxSearches == 0) || (src == null) || (firstPositions == null))
            return new Geodesic[0];

        // Limit searches if there are not enough starting positions
        if (maxSearches > firstPositions.length)
            maxSearches = firstPositions.length;

        int geoLength;
        int inc;
        int incLine;
        int lineLength;

        if (dir == HORIZONTAL)
        {
            geoLength = this.width;
            lineLength = this.height;
            inc = this.stride;
            incLine = 1;
        }
        else
        {
            geoLength = this.height;
            lineLength = this.width;
            inc = 1;
            incLine = this.stride;
        }

        // Calculate cost at each pixel
        this.calculateCosts(src, this.costs);
        final int maxGeo = (this.nbGeodesics > maxSearches) ? maxSearches : this.nbGeodesics;

        // Queue of geodesics sorted by cost
        // The queue size could be less than firstPositions.length
        final GeodesicSortedQueue queue = new GeodesicSortedQueue(maxGeo);
        final int geoLength4 = geoLength & -4;
        boolean consumed = true;
        Geodesic geodesic = null;
        Geodesic last = null; // last in queue
        int maxCost = geoLength * this.maxAvgGeoPixCost;
        final int[] costs_ = this.costs; // aliasing

        // Calculate path and cost for each geodesic
        for (int i=0; i<maxSearches; i++)
        {
            if (consumed == true)
                geodesic = new Geodesic(dir, geoLength);

            consumed = false;
            int bestLinePos = firstPositions[i];
            int costIdx = this.offset + (inc * bestLinePos);
            geodesic.positions[0] = bestLinePos;
            geodesic.cost = costs_[costIdx];

            // Process each row/column
            for (int pos=1; pos<geoLength; pos++)
            {
                costIdx += incLine;
                int startCostIdx = costIdx;
                int bestCost = DEFAULT_BEST_COST;
                int startBestLinePos = bestLinePos;

                if ((costs_[costIdx] & USED_MASK) == 0)
                    bestCost = costs_[costIdx];

                if (bestCost > 0)
                {
                    // Check left/upper pixel, skip already used pixels
                    int idx = startCostIdx - inc;

                    for (int linePos=startBestLinePos-1; linePos>=0; idx-=inc, linePos--)
                    {
                        int cost = costs_[idx];

                        // Skip pixels in use
                        if ((cost & USED_MASK) != 0)
                           continue;

                        if (cost < bestCost)
                        {
                            bestCost = cost;
                            bestLinePos = linePos;
                            costIdx = idx;
                        }

                        break;
                    }
                }

                if (bestCost > 0)
                {
                    // Check right/lower pixel, skip already used pixels
                    int idx = startCostIdx + inc;

                    for (int linePos=startBestLinePos+1; linePos<lineLength; idx+=inc, linePos++)
                    {
                        int cost = costs_[idx];

                        if ((cost & USED_MASK) != 0)
                           continue;

                         if (cost < bestCost)
                         {
                             bestCost = cost;
                             bestLinePos = linePos;
                             costIdx = idx;
                         }

                         break;
                    }

                    geodesic.cost += bestCost;

                    // Skip, this path is already too expensive
                    if (geodesic.cost >= maxCost)
                       break;
                }

                geodesic.positions[pos] = bestLinePos;
            }

            if (geodesic.cost < maxCost)
            {
                 // Add geodesic (in increasing cost order). It is sure to succeed
                 // (it may evict the current tail) because geodesic.cos < maxCost
                 // and maxCost is adjusted to tail.value
                 Geodesic newLast = queue.add(geodesic);

                 // Prevent geodesics from sharing pixels by marking the used pixels
                 // Only the pixels of the geodesics in the queue are marked as used
                 if (this.nbGeodesics > 1)
                 {
                     // If the previous last element has been expelled from the queue,
                     // the corresponding pixels can be reused by other geodesics
                     int startLine = this.offset;
                     final int[] gp = geodesic.positions;

                     if (last != null)
                     {
                        final int[] lp = last.positions;

                        // Tag old pixels as 'free' and new pixels as 'used'
                        for (int k=0; k<geoLength4; k+=4)
                        {
                            costs_[startLine+(inc*gp[k])]   |= USED_MASK;
                            costs_[startLine+(inc*lp[k])]   &= VALUE_MASK;
                            startLine += incLine;
                            costs_[startLine+(inc*gp[k+1])] |= USED_MASK;
                            costs_[startLine+(inc*lp[k+1])] &= VALUE_MASK;
                            startLine += incLine;
                            costs_[startLine+(inc*gp[k+2])] |= USED_MASK;
                            costs_[startLine+(inc*lp[k+2])] &= VALUE_MASK;
                            startLine += incLine;
                            costs_[startLine+(inc*gp[k+3])] |= USED_MASK;
                            costs_[startLine+(inc*lp[k+3])] &= VALUE_MASK;
                            startLine += incLine;
                        }

                        for (int k=geoLength4; k<geoLength; k++)
                        {
                            costs_[startLine+(inc*gp[k])] |= USED_MASK;
                            costs_[startLine+(inc*lp[k])] &= VALUE_MASK;
                            startLine += incLine;
                        }
                     }
                     else
                     {
                        for (int k=0; k<geoLength4; k+=4)
                        {
                            costs_[startLine+(inc*gp[k])]   |= USED_MASK;
                            startLine += incLine;
                            costs_[startLine+(inc*gp[k+1])] |= USED_MASK;
                            startLine += incLine;
                            costs_[startLine+(inc*gp[k+2])] |= USED_MASK;
                            startLine += incLine;
                            costs_[startLine+(inc*gp[k+3])] |= USED_MASK;
                            startLine += incLine;
                        }

                        for (int k=geoLength4; k<geoLength; k++)
                        {
                            costs_[startLine+(inc*gp[k])] |= USED_MASK;
                            startLine += incLine;
                        }
                     }
                 }

                 // Be green, recycle
                 if (last == null)
                    consumed = true;
                 else
                    geodesic = last;

                 // Update maxCost
                 if (queue.isFull())
                 {
                    last = newLast;
                    maxCost = newLast.cost;
                 }
            }

            // All requested geodesics have been found with a cost of 0 => done !
            if ((maxCost == 0) && (queue.isFull() == true))
                break;
        }

        return queue.toArray(new Geodesic[queue.size()]);
    }


    private int[] calculateCosts(int[] src, int[] costs_)
    {
        SobelFilter gradientFilter = new SobelFilter(this.width, this.height,
                this.offset, this.stride, SobelFilter.HORIZONTAL | SobelFilter.VERTICAL,
                SobelFilter.THREE_CHANNELS, SobelFilter.COST);
        gradientFilter.apply(src, costs_);
        
        // Add a quadratic contribution to the cost
        // Favor straight lines if costs of neighbors are all low
        for (int i=0; i<costs_.length; i++)
        {
           final int c = costs_[i];           
           costs_[i] = (c < 5) ? 0 :  c + ((c * c) >> 8); 
        }
        
        return costs_;
    }

}
