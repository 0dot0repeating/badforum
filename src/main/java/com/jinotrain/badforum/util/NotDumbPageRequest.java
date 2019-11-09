package com.jinotrain.badforum.util;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public class NotDumbPageRequest implements Pageable
{
    private long offset;
    private int  size;
    private Sort sort;

    public NotDumbPageRequest(long start, long end)
    {
        this(start, end, Sort.unsorted());
    }

    public NotDumbPageRequest(long start, long end, Sort sort)
    {
        long size = end - start;
        this.size = size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)size;
        this.offset = start;
        this.sort = sort;
    }

    @Override
    public long getOffset() { return offset; }

    @Override
    public int getPageSize() { return size; }

    @Override
    public int getPageNumber() { return 0; }

    @Override
    public Sort getSort() { return sort; }

    @Override
    public boolean hasPrevious() { return offset > 0; }


    @Override
    public Pageable first()
    {
        return new NotDumbPageRequest(0, size);
    }

    @Override
    public Pageable previousOrFirst()
    {
        if (size >= offset) { return new NotDumbPageRequest(0, size); }
        return new NotDumbPageRequest(offset-size, offset);
    }

    @Override
    public Pageable next()
    {
        return new NotDumbPageRequest(offset+size, offset+(size*2));
    }
}
