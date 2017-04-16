package net.imglib2.cache.examplehttp;

import java.util.function.Function;
import java.util.stream.IntStream;

import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;

public class FunctorLoader< A > implements CacheLoader< Long, Cell< A > >
{
	
	public interface Functor< T, R > {
		
		public R call( T t ) throws Exception;
		
	}
	
	private final CellGrid grid;

	private final Functor< Interval, A > functor;

	public FunctorLoader( final CellGrid grid, final Functor< Interval, A > functor )
	{
		this.grid = grid;
		this.functor = functor;
	}

	@Override
	public Cell< A > get( final Long key ) throws Exception
	{
		final long index = key;

		final int n = grid.numDimensions();
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		grid.getCellDimensions( index, cellMin, cellDims );
		final long[] cellMax = IntStream.range( 0, n ).mapToLong( d -> cellMin[ d ] + cellDims[ d ] - 1 ).toArray();
		final A result = functor.call( new FinalInterval( cellMin, cellMax ) );
		if ( result == null )
			return null;
		else
			return new Cell<>( cellDims, cellMin, result );
	}

}
