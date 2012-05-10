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


public interface BitStream
{
    // Processes the least significant bit of the input integer
    public boolean writeBit(int bit) throws BitStreamException;
    
    public int writeBits(long bits, int length) throws BitStreamException;
    
    // Returns 1 or 0
    public int readBit() throws BitStreamException;
    
    public long readBits(int length) throws BitStreamException;
    
    public void flush() throws BitStreamException;
    
    public void close() throws BitStreamException;
    
    // Number of bits written
    public long written();
    
    // Number of bits read
    public long read();    
    
    // Return false when the bitstream is closed or the End-Of-Stream has been reached
    public boolean hasMoreToRead();
}
