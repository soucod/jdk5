/*
 * @(#)Direct-X-Buffer.java	1.48 04/05/03
 *
 * Copyright 2004 Sun Microsystems, Inc. All rights reserved.
 * SUN PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

#warn This file is preprocessed before being compiled

package java.nio;

import sun.misc.Cleaner;
import sun.misc.Unsafe;
import sun.nio.ch.DirectBuffer;
import sun.nio.ch.FileChannelImpl;


class Direct$Type$Buffer$RW$$BO$
#if[rw]
    extends {#if[byte]?Mapped$Type$Buffer:$Type$Buffer}
#else[rw]
    extends Direct$Type$Buffer$BO$
#end[rw]
    implements DirectBuffer
{

#if[rw]

    // Cached unsafe-access object
    protected static final Unsafe unsafe = Bits.unsafe();

    // Cached unaligned-access capability
    protected static final boolean unaligned = Bits.unaligned();

    // Base address, used in all indexing calculations
    // NOTE: moved up to Buffer.java for speed in JNI GetDirectBufferAddress
    //    protected long address;

    // If this buffer is a view of another buffer then we keep a reference to
    // that buffer so that its memory isn't freed before we're done with it
    protected Object viewedBuffer = null;

    public Object viewedBuffer() {
        return viewedBuffer;
    }

#if[byte]

    private static class Deallocator
	implements Runnable
    {

	private static Unsafe unsafe = Unsafe.getUnsafe();

	private long address;
	private int capacity;

	private Deallocator(long address, int capacity) {
	    assert (address != 0);
	    this.address = address;
	    this.capacity = capacity;
	}

	public void run() {
	    if (address == 0) {
		// Paranoia
		return;
	    }
	    unsafe.freeMemory(address);
	    address = 0;
	    Bits.unreserveMemory(capacity);
	}

    }

    private final Cleaner cleaner;

    public Cleaner cleaner() { return cleaner; }

#else[byte]

    public Cleaner cleaner() { return null; }

#end[byte]

#end[rw]

#if[byte]

    // Primary constructor
    //
    Direct$Type$Buffer$RW$(int cap) {			// package-private
#if[rw]
	super(-1, 0, cap, cap, false);
	Bits.reserveMemory(cap);
	int ps = Bits.pageSize();
	long base = 0;
	try {
	    base = unsafe.allocateMemory(cap + ps);
	} catch (OutOfMemoryError x) {
	    Bits.unreserveMemory(cap);
	    throw x;
	}
	unsafe.setMemory(base, cap + ps, (byte) 0);
	if (base % ps != 0) {
	    // Round up to page boundary
	    address = base + ps - (base & (ps - 1));
	} else {
	    address = base;
	}
	cleaner = Cleaner.create(this, new Deallocator(base, cap));
#else[rw]
	super(cap);
#end[rw]
    }

#if[rw]

    // Invoked only by JNI: NewDirectByteBuffer(void*, long)
    //
    private Direct$Type$Buffer(long addr, int cap) {
        super(-1, 0, cap, cap, false);
	address = addr;
	cleaner = null;
    }

#end[rw]

    // For memory-mapped buffers -- invoked by FileChannelImpl via reflection
    //
    protected Direct$Type$Buffer$RW$(int cap, long addr, Runnable unmapper) {
#if[rw]
        super(-1, 0, cap, cap, true);
	address = addr;
        viewedBuffer = null;
	cleaner = Cleaner.create(this, unmapper);
#else[rw]
	super(cap, addr, unmapper);
#end[rw]
    }

#end[byte]

    // For duplicates and slices
    //
    Direct$Type$Buffer$RW$$BO$(DirectBuffer db,	        // package-private
			       int mark, int pos, int lim, int cap,
			       int off)
    {
#if[rw]
	super(mark, pos, lim, cap);
	address = db.address() + off;
	viewedBuffer = db;
#if[byte]
	cleaner = null;
#end[byte]
#else[rw]
	super(db, mark, pos, lim, cap, off);
#end[rw]
    }

    public $Type$Buffer slice() {
	int pos = this.position();
	int lim = this.limit();
	assert (pos <= lim);
	int rem = (pos <= lim ? lim - pos : 0);
	int off = (pos << $LG_BYTES_PER_VALUE$);
        assert (off >= 0);
	return new Direct$Type$Buffer$RW$$BO$(this, -1, 0, rem, rem, off);
    }

    public $Type$Buffer duplicate() {
	return new Direct$Type$Buffer$RW$$BO$(this,
					      this.markValue(),
					      this.position(),
					      this.limit(),
					      this.capacity(),
					      0);
    }

    public $Type$Buffer asReadOnlyBuffer() {
#if[rw]
	return new Direct$Type$BufferR$BO$(this,
					   this.markValue(),
					   this.position(),
					   this.limit(),
					   this.capacity(),
					   0);
#else[rw]
	return duplicate();
#end[rw]
    }

#if[rw]

    public long address() {
	return address;
    }

    private long ix(int i) {
        return address + (i << $LG_BYTES_PER_VALUE$);
    }

    public $type$ get() {
	return $fromBits$($swap$(unsafe.get$Swaptype$(ix(nextGetIndex()))));
    }

    public $type$ get(int i) {
	return $fromBits$($swap$(unsafe.get$Swaptype$(ix(checkIndex(i)))));
    }

    public $Type$Buffer get($type$[] dst, int offset, int length) {
#if[rw]
	if ((length << $LG_BYTES_PER_VALUE$) > Bits.JNI_COPY_TO_ARRAY_THRESHOLD) {
	    checkBounds(offset, length, dst.length);
	    int pos = position();
	    int lim = limit();
	    assert (pos <= lim);
	    int rem = (pos <= lim ? lim - pos : 0);
	    if (length > rem)
		throw new BufferUnderflowException();

	    if (order() != ByteOrder.nativeOrder())
		Bits.copyTo$Memtype$Array(ix(pos), dst,
					  offset << $LG_BYTES_PER_VALUE$,
					  length << $LG_BYTES_PER_VALUE$);
	    else
		Bits.copyToByteArray(ix(pos), dst,
				     offset << $LG_BYTES_PER_VALUE$,
				     length << $LG_BYTES_PER_VALUE$);
	    position(pos + length);
	} else {
	    super.get(dst, offset, length);
	}
	return this;
#else[rw]
	throw new ReadOnlyBufferException();
#end[rw]	
    }

#end[rw]

    public $Type$Buffer put($type$ x) {
#if[rw]
	unsafe.put$Swaptype$(ix(nextPutIndex()), $swap$($toBits$(x)));
	return this;
#else[rw]
	throw new ReadOnlyBufferException();
#end[rw]
    }

    public $Type$Buffer put(int i, $type$ x) {
#if[rw]
	unsafe.put$Swaptype$(ix(checkIndex(i)), $swap$($toBits$(x)));
	return this;
#else[rw]
	throw new ReadOnlyBufferException();
#end[rw]
    }

    public $Type$Buffer put($Type$Buffer src) {
#if[rw]
	if (src instanceof Direct$Type$Buffer$BO$) {
	    if (src == this)
		throw new IllegalArgumentException();
	    Direct$Type$Buffer$RW$$BO$ sb = (Direct$Type$Buffer$RW$$BO$)src;

	    int spos = sb.position();
	    int slim = sb.limit();
	    assert (spos <= slim);
	    int srem = (spos <= slim ? slim - spos : 0);

	    int pos = position();
	    int lim = limit();
	    assert (pos <= lim);
	    int rem = (pos <= lim ? lim - pos : 0);

	    if (srem > rem)
		throw new BufferOverflowException();
 	    unsafe.copyMemory(sb.ix(spos), ix(pos), srem << $LG_BYTES_PER_VALUE$);
 	    sb.position(spos + srem);
 	    position(pos + srem);
	} else if (!src.isDirect()) {

	    int spos = src.position();
	    int slim = src.limit();
	    assert (spos <= slim);
	    int srem = (spos <= slim ? slim - spos : 0);

	    put(src.hb, src.offset + spos, srem);
	    src.position(spos + srem);

	} else {
	    super.put(src);
	}
	return this;
#else[rw]
	throw new ReadOnlyBufferException();
#end[rw]
    }

    public $Type$Buffer put($type$[] src, int offset, int length) {
#if[rw]
	if ((length << $LG_BYTES_PER_VALUE$) > Bits.JNI_COPY_FROM_ARRAY_THRESHOLD) {
	    checkBounds(offset, length, src.length);
	    int pos = position();
	    int lim = limit();
	    assert (pos <= lim);
	    int rem = (pos <= lim ? lim - pos : 0);
	    if (length > rem)
		throw new BufferOverflowException();

	    if (order() != ByteOrder.nativeOrder()) 
		Bits.copyFrom$Memtype$Array(src, offset << $LG_BYTES_PER_VALUE$,
					    ix(pos), length << $LG_BYTES_PER_VALUE$);
	    else
		Bits.copyFromByteArray(src, offset << $LG_BYTES_PER_VALUE$,
				       ix(pos), length << $LG_BYTES_PER_VALUE$);
	    position(pos + length);
	} else {
	    super.put(src, offset, length);
	}
	return this;
#else[rw]
	throw new ReadOnlyBufferException();
#end[rw]
    }
    
    public $Type$Buffer compact() {
#if[rw]
	int pos = position();
	int lim = limit();
	assert (pos <= lim);
	int rem = (pos <= lim ? lim - pos : 0);

 	unsafe.copyMemory(ix(pos), ix(0), rem << $LG_BYTES_PER_VALUE$);
 	position(rem);
	limit(capacity());
	return this;
#else[rw]
	throw new ReadOnlyBufferException();
#end[rw]
    }

    public boolean isDirect() {
	return true;
    }

    public boolean isReadOnly() {
	return {#if[rw]?false:true};
    }


#if[char]

    public String toString(int start, int end) {
	if ((end > limit()) || (start > end))
	    throw new IndexOutOfBoundsException();
	try {
	    int len = end - start;
	    char[] ca = new char[len];
	    CharBuffer cb = CharBuffer.wrap(ca);
	    CharBuffer db = this.duplicate();
	    db.position(start);
	    db.limit(end);
	    cb.put(db);
	    return new String(ca);
	} catch (StringIndexOutOfBoundsException x) {
	    throw new IndexOutOfBoundsException();
	}
    }


    // --- Methods to support CharSequence ---

    public CharSequence subSequence(int start, int end) {
	int pos = position();
	int lim = limit();
	assert (pos <= lim);
	pos = (pos <= lim ? pos : lim);
	int len = lim - pos;

	if ((start < 0) || (end > len) || (start > end))
	    throw new IndexOutOfBoundsException();
	int sublen = end - start;
 	int off = (pos + start) << $LG_BYTES_PER_VALUE$;
        assert (off >= 0);
	return new DirectCharBuffer$RW$$BO$(this, -1, 0, sublen, sublen, off);
    }

#end[char]



#if[!byte]

    public ByteOrder order() {
#if[boS]
	return ((ByteOrder.nativeOrder() == ByteOrder.BIG_ENDIAN)
		? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
#end[boS]
#if[boU]
	return ((ByteOrder.nativeOrder() != ByteOrder.BIG_ENDIAN)
		? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
#end[boU]
    }

#end[!byte]



#if[byte]

    byte _get(int i) {				// package-private
	return unsafe.getByte(address + i);
    }

    void _put(int i, byte b) {			// package-private
#if[rw]
	unsafe.putByte(address + i, b);
#else[rw]
	throw new ReadOnlyBufferException();
#end[rw]
    }

    // #BIN
    //
    // Binary-data access methods  for short, char, int, long, float,
    // and double will be inserted here

#end[byte]

}
