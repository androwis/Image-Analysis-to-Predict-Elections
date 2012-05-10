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

import kanzi.ArrayComparator;
import kanzi.util.sort.DefaultArrayComparator;
import kanzi.util.sort.QuickSort;
import java.util.LinkedList;
import java.util.TreeMap;
import kanzi.InputBitStream;
import kanzi.OutputBitStream;

// Tree utility class for a canonical implementation of Huffman codec
/*package*/ class HuffmanTree
{
    public static final int DECODING_BATCH_SIZE = 10; // in bits

    private final int[] codes;
    private final int[] sizes; // Cache for speed purpose
    private final Node root;
    private final CacheData[] decodingCache;
    private CacheData current;

    
    // Used by encoder
    /*package*/ HuffmanTree(int[] frequencies)
    {
       this.codes = new int[256];
       this.sizes = new int[256];
       this.decodingCache = null;
       this.current = null;

       // Create tree from frequencies
       this.root = this.createTreeFromFrequencies(frequencies);

       // Create sizes and codes from tree
       this.fillTree(this.root, 0, 0);

       // Create canonical codes
       this.generateCanonicalCodes();
    }


    // Used by decoder
    /*package*/ HuffmanTree(int[] sizes, int maxSize)
    {
       this.codes = new int[256];
       this.sizes = new int[256];
       System.arraycopy(sizes, 0, this.sizes, 0, sizes.length);

       // Create canonical codes
       this.generateCanonicalCodes();

       // Create tree from code sizes
       this.root = this.createTreeFromCodes(maxSize);
       this.decodingCache = this.createDecodingCache();
       this.current = new CacheData(this.root); // point to root
    }


    private CacheData[] createDecodingCache()
    {
       LinkedList<CacheData> nodes = new LinkedList<CacheData>();
       final int end = 1 << DECODING_BATCH_SIZE;
       CacheData previousData = null;

       // Create an array storing a list of Nodes for each input byte value
       for (int val=0; val<end; val++)
       {
          int shift = DECODING_BATCH_SIZE - 1;
          boolean firstAdded = false;

          while (shift >= 0)
          {
             // Start from root
             Node currentNode = this.root;

             // Process next bit
             while ((shift >= 0) && ((currentNode.left != null) || (currentNode.right != null)))
             {
                currentNode = (((val >> shift) & 1) == 0) ? currentNode.left : currentNode.right;
                shift--;
             }

             final CacheData currentData = new CacheData(currentNode);

             // The list is made of linked nodes
             if (previousData != null)
                previousData.next = currentData;

             previousData = currentData;

             if (firstAdded == false)
             {
                // Add first node of list to array (whether it is a leaf or not)
                nodes.addLast(currentData);
                firstAdded = true;
             }
          }

          previousData.next = new CacheData(this.root);
          previousData = previousData.next;
       }

       return nodes.toArray(new CacheData[nodes.size()]);
    }


    private Node createTreeFromFrequencies(int[] frequencies)
    {
       int[] array = new int[256];

       for (int i=0; i<array.length; i++)
          array[i] = i;

       // Sort by frequency
       QuickSort sorter = new QuickSort(array.length, new DefaultArrayComparator(frequencies));
       sorter.sort(array, 0);

       // Create Huffman tree of (present) symbols
       LinkedList<Node> queue1 = new LinkedList<Node>();
       LinkedList<Node> queue2 = new LinkedList<Node>();
       Node[] nodes = new Node[2];

       for (int i=array.length-1; i>=0; i--)
       {
          final int val = array[i];

          if (frequencies[val] != 0)
             queue1.addFirst(new Node((byte) val, frequencies[val]));
       }

       while (queue1.size() + queue2.size() > 1)
       {
          // Extract 2 minimum nodes
          for (int i=0; i<2; i++)
          {
             if (queue1.size() == 0)
             {
                nodes[i] = queue2.removeFirst();
                continue;
             }

             if (queue2.size() == 0)
             {
                nodes[i] = queue1.removeFirst();
                continue;
             }

             if (queue1.getFirst().weight <= queue2.getFirst().weight)
                nodes[i] = queue1.removeFirst();
             else
                nodes[i] = queue2.removeFirst();
          }

          // Merge minimum nodes and enqueue result
          final Node left = nodes[0];
          final Node right = nodes[1];
          final Node merged = new Node(left.weight + right.weight, left, right);
          queue2.addLast(merged);
       }

       return ((queue1.isEmpty()) ? queue2.removeFirst() : queue1.removeFirst());
    }


    // Fill size and code arrays
    private void fillTree(Node node, int value, int depth)
    {
       if ((node.left == null) && (node.right == null))
       {
          this.sizes[node.symbol & 0xFF] = depth;
          this.codes[node.symbol & 0xFF] = value;
       }
       else
       {
          if (node.left != null)
             this.fillTree(node.left, value << 1, depth + 1);

          if (node.right != null)
             this.fillTree(node.right, (value << 1) | 1, depth + 1);
       }
    }


    private Node createTreeFromCodes(int maxSize)
    {
       TreeMap<Key, Node> codeMap = new TreeMap<Key, Node>();
       final int sum = 1 << maxSize;
       codeMap.put(new Key(0, 0), new Node((byte) 0, sum));

       // Create node for each (present) symbol and add to map
       for (int i=0; i<this.sizes.length; i++)
       {
          final int size = this.sizes[i];

          if (size <= 0)
             continue;

          final Key key = new Key(size, this.codes[i]);
          final Node value = new Node((byte) i, sum >> size);
          codeMap.put(key, value);
       }

       // Process each element of the map except the root node
       while (codeMap.size() > 1)
       {
          final Key key = codeMap.lastKey();
          final Node node = codeMap.remove(key);
          final Key upKey = new Key(key.length-1, (key.code >> 1) & 0xFF);
          Node upNode = codeMap.get(upKey);

          // Create superior node if it does not exist (length gap > 1)
          if (upNode == null)
          {
             upNode = new Node((byte) 0, sum >> upKey.length);
             codeMap.put(upKey, upNode);
          }

          // Add the current node to its parent at the correct place
          if ((key.code & 1) == 1)
             upNode.right = node;
          else
             upNode.left = node;
       }

       // Return the last element of the map (root node)
       return codeMap.firstEntry().getValue();
    }


    private int[] generateCanonicalCodes()
    {
       final int[] array = new int[this.sizes.length];

       for (int i=0; i<array.length; i++)
          array[i] = i;

       // Sort by decreasing size (first key) and increasing value (second key)
       QuickSort sorter = new QuickSort(array.length, new HuffmanArrayComparator(this.sizes));
       sorter.sort(array, 0);

       for (int i=this.codes.length-1; i>=0; i--)
          this.codes[i] = 0;
        
       int code = 0;
       int len = this.sizes[array[0]];

       for (int i=0; i<array.length; i++)
       {
          final int idx = array[i];
          final int currentSize = this.sizes[idx];

          // Since the sizes are decreasing, exit on the first occurence of 0
          if (currentSize == 0)
             break;

          while (len > currentSize)
          {
             code >>= 1;
             len--;
          }

          this.codes[idx] = code;
          code++;
       }

       return this.codes;
    }


    /*package*/ int getCode(int val)
    {
       return this.codes[val];
    }


    /*package*/ int getSize(int val)
    {
       return this.sizes[val];
    }


    /*package*/ boolean encodeByte(OutputBitStream bitstream, byte val)
    {
       final int idx = val & 0xFF;
       return (bitstream.writeBits(this.codes[idx], this.sizes[idx]) == this.sizes[idx]);
    }

    
    /*package*/ byte decodeByte(InputBitStream bitstream)
    {
       // Empty cache
       Node currNode = this.current.value;

       if (currNode != this.root)
          this.current = this.current.next;
       
       while ((currNode.left != null) || (currNode.right != null))
       {
          currNode = (bitstream.readBit() == 0) ? currNode.left : currNode.right;
       }

       return currNode.symbol;
    }


    // DECODING_BATCH_SIZE bits must be available in the bitstream
    /*package*/ byte fastDecodeByte(InputBitStream bitstream)
    {
       Node currNode = this.current.value;

       // Use the cache to find a good starting point in the tree
       if (currNode == this.root)
       {
          // Read more bits from the bitstream and fetch starting point from cache
          final int idx = (int) bitstream.readBits(DECODING_BATCH_SIZE);
          this.current = this.decodingCache[idx];
          currNode = this.current.value;
       }

       while ((currNode.left != null) || (currNode.right != null))
       {
          currNode = (bitstream.readBit() == 0) ? currNode.left : currNode.right;
       }

       this.current = this.current.next;
       return currNode.symbol;
    }


    // Huffman node
    private static class Node
    {
       protected final int weight;
       protected final byte symbol;
       protected Node left;
       protected Node right;


       // Leaf
       Node(byte symbol, int frequency)
       {
          this.weight = frequency;
          this.symbol = symbol;
       }


       // Not leaf
       Node(int frequency, Node node1, Node node2)
       {
          this.weight = frequency;
          this.symbol = 0;
          this.left  = node1;
          this.right = node2;
       }
    }


    // Class used to build the tree in canonical Huffman
    private static class Key implements Comparable<Key>
    {
       protected final int length;
       protected final int code;

       Key(int length, int code)
       {
          this.code = code;
          this.length = length;
       }

       
       @Override
       public int compareTo(Key key)
       {
           try
           {
             if (this == key)
                 return 0;

             int len = this.length - key.length;

             if (len != 0)
                return len;

             return this.code - key.code;
           }
           catch (NullPointerException e)
           {
               return 1;
           }
           catch (ClassCastException e)
           {
               return 1;
           }
       }


       @Override
       public boolean equals(Object obj)
       {
           try
           {
              if (this == obj)
                 return true;

              Key key = (Key) obj;

              if (this.length != key.length)
                 return false;

              return (this.code == key.code);
           }
           catch (NullPointerException e)
           {
               return false;
           }
           catch (ClassCastException e)
           {
               return false;
           }
       }


       @Override
       public int hashCode()
       {
           return this.length | (this.code << 16);
       }
    }


    // Array comparator used to sort keys and values to generate canonical codes
    private static class HuffmanArrayComparator implements ArrayComparator
    {
        private final int[] array;


        public HuffmanArrayComparator(int[] array)
        {
            if (array == null)
                throw new NullPointerException("Invalid null array parameter");

            this.array = array;
        }


        @Override
        public int compare(int lidx, int ridx)
        {
            // Check sizes (reverse order) as first key
            if (this.array[ridx] != this.array[lidx])
                return this.array[ridx] - this.array[lidx];

            // Check value (natural order) as second key
            return lidx - ridx;
        }
    }


    private static class CacheData
    {
       Node value;
       CacheData next;

       CacheData(Node value)
       {
          this.value = value;
       }
    }
}