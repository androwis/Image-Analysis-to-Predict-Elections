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

package kanzi.entropy;

import kanzi.BitStreamException;
import kanzi.InputBitStream;



public class HuffmanDecoder extends AbstractDecoder
{
    private final InputBitStream bitstream;
    private final int[] buffer;
    private HuffmanTree tree;


    public HuffmanDecoder(InputBitStream bitstream) throws BitStreamException
    {
        if (bitstream == null)
            throw new NullPointerException("Invalid null bitstream parameter");
      
        this.bitstream = bitstream;
        this.buffer = new int[256];

        // Default lengths
        for (int i=0; i<256; i++)
           this.buffer[i] = 8;
         
        this.tree = new HuffmanTree(this.buffer, 8);
    }
       
    
    public boolean readLengths() throws BitStreamException
    {
        final int[] buf = this.buffer;
        int maxSize = 0;
        buf[0] = (int) this.bitstream.readBits(5);
        ExpGolombDecoder egdec = new ExpGolombDecoder(this.bitstream, true);
       
        // Read lengths
        for (int i=1; i<buf.length; i++)
        {
           buf[i] = buf[i-1] + egdec.decodeByte();

           if (maxSize < buf[i])
              maxSize = buf[i];
        }

        // Create Huffman tree
        this.tree = new HuffmanTree(buf, maxSize);        
        return true;
    }


    // Rebuild the Huffman tree for each block of data before decoding
    @Override
    public int decode(byte[] array, int blkptr, int len)
    {
       if ((array == null) || (blkptr + len > array.length) || (blkptr < 0) || (len < 0))
         return -1;

       this.readLengths();
       final int end2 = blkptr + len;
       final int end1 = end2 - HuffmanTree.DECODING_BATCH_SIZE;
       int i = blkptr;

       try
       {       
          // Decode fast by reading one byte at a time from the bitstream
          while (i < end1)
             array[i++] = this.tree.fastDecodeByte(this.bitstream);

          // Regular decoding by reading one bit at a time from the bitstream
          while (i < end2)
             array[i++] = this.tree.decodeByte(this.bitstream);
       }
       catch (BitStreamException e)
       {
          return i - blkptr;
       }

       return len;
    }
       
    
    // The data block header must have been read before
    @Override
    public final byte decodeByte()
    {
       return this.tree.decodeByte(this.bitstream);
    }


    @Override
    public InputBitStream getBitStream()
    {
       return this.bitstream;
    }
}
