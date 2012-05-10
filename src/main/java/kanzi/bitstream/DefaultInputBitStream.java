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
package kanzi.bitstream;

import kanzi.BitStreamException;
import java.io.IOException;
import java.io.InputStream;
import kanzi.InputBitStream;


public final class DefaultInputBitStream implements InputBitStream
{
   private final InputStream is;
   private final byte[] buffer;
   private int position;
   private int bitIndex; // index of current bit to read
   private long read;
   private boolean closed;
   private int maxPosition;


   public DefaultInputBitStream(InputStream is, int bufferSize)
   {
      this.is = is;
      this.buffer = new byte[bufferSize];
      this.bitIndex = 7;
      this.maxPosition = -1;
   }


   // Return 1 or 0
   @Override
   public synchronized int readBit() throws BitStreamException
   {
      if (this.bitIndex == 7)
      {
         if (++this.position > this.maxPosition)
            this.readFromInputStream(0, this.buffer.length);
      }

      final int bit = (this.buffer[this.position] >> this.bitIndex) & 1;
      this.bitIndex = (this.bitIndex + 7) & 7;
      this.read++;
      return bit;
   }


   private synchronized int readFromInputStream(int offset, int length) throws BitStreamException
   {
      if (this.closed == true)
         throw new BitStreamException("Stream closed", BitStreamException.STREAM_CLOSED);

      try
      {
         this.position = 0;
         final int size = this.is.read(this.buffer, offset, length);

         if (size < 0)
         {
            throw new BitStreamException("Nore more data to read in the bitstream",
                    BitStreamException.END_OF_STREAM);
         }

         this.maxPosition = offset + size - 1;
         return size;
      }
      catch (IOException e)
      {
         throw new BitStreamException(e.getMessage(), BitStreamException.INPUT_OUTPUT);
      }
   }


   @Override
   public synchronized long readBits(int length) throws BitStreamException
   {
      if ((length == 0) || (length > 64))
          throw new IllegalArgumentException("Invalid length: "+length+" (must be in [1..64])");

      int remaining = length;
      long res = 0;

      try
      {
         // Extract bits from the current location in buffer
         if (this.bitIndex != 7)
         {
            int idx = this.bitIndex;
            final int len = (remaining <= idx + 1) ? remaining : idx + 1;
            remaining -= len;
            final long bits = (this.buffer[this.position] >> (idx + 1 - len)) & ((1 << len) - 1);
            res |= (bits << remaining);
            idx = (idx + 8 - len) & 7;
            this.read += len;
            this.bitIndex = idx;
         }

         // Need to read more bits ?
         if (this.bitIndex == 7)
         {
            // We are byte aligned, fast track
            while (remaining >= 8)
            {
               if (++this.position > this.maxPosition)
                  this.readFromInputStream(0, this.buffer.length);

               final long value = this.buffer[this.position] & 0xFF;
               remaining -= 8;
               this.read += 8;
               res |= (value << remaining);
            }

            // Extract last bits from the current location in buffer
            if (remaining > 0)
            {
               if (++this.position > this.maxPosition)
                  this.readFromInputStream(0, this.buffer.length);

               final int value = this.buffer[this.position] & 0xFF;
               final long bits = (value >> (8 - remaining)) & ((1 << remaining) - 1);
               res |= bits;
               this.read += remaining;
               this.bitIndex -= remaining;
            }
         }
      }
      catch (ArrayIndexOutOfBoundsException e)
      {
         throw new BitStreamException("No more data to read", BitStreamException.END_OF_STREAM);
      }

      return res;
   }


   @Override
   public synchronized void close()
   {
      if (this.closed == true)
         return;

      // Reset fields to force a readFromInputStream() (that will throw an exception)
      // on readBit() or readBits()
      this.closed = true;
      this.bitIndex = 7;
      this.maxPosition = -1;
      this.position = 0;

      try
      {
        this.is.close();
      }
      catch (IOException e)
      {
         throw new BitStreamException(e, BitStreamException.INPUT_OUTPUT);
      }
   }


   // Return number of bits read so far
   @Override
   public synchronized long read()
   {
      return this.read;
   }


   @Override
   public synchronized boolean hasMoreToRead()
   {
      if (this.closed == true)
         return false;

      if (this.position > this.maxPosition)
      {
         try
         {
            this.readFromInputStream(0, this.buffer.length);
         }
         catch (BitStreamException e)
         {
            return false;
         }
      }

      return (this.position <= this.maxPosition);
   }
}