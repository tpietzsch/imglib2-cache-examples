package net.imglib2.cache.exampleclassifier;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import net.imglib2.Cursor;
import net.imglib2.FinalInterval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.cache.CacheLoader;
import net.imglib2.img.Img;
import net.imglib2.img.array.ArrayImgs;
import net.imglib2.img.basictypeaccess.volatiles.array.VolatileShortArray;
import net.imglib2.img.cell.Cell;
import net.imglib2.img.cell.CellGrid;
import net.imglib2.type.numeric.RealType;
import net.imglib2.type.numeric.integer.UnsignedShortType;
import net.imglib2.util.Intervals;
import net.imglib2.util.Util;
import net.imglib2.view.Views;
import weka.classifiers.Classifier;
import weka.core.Instance;

public class ClassifyingCellLoaderOnList< T extends RealType< T > > implements CacheLoader< Long, Cell< VolatileShortArray > >
{
	private final CellGrid grid;

	private final List< RandomAccessible< T > > features;

	private final Classifier classifier;

	private final int numClasses;

	public ClassifyingCellLoaderOnList(
			final CellGrid grid,
			final List< RandomAccessible< T > > features,
			final Classifier classifier,
			final int numClasses )
	{
		this.grid = grid;
		this.features = features;
		this.classifier = classifier;
		this.numClasses = numClasses;
	}

	@Override
	public Cell< VolatileShortArray > get( final Long key ) throws Exception
	{
		final long index = key;

		final int n = grid.numDimensions();
		final long[] cellMin = new long[ n ];
		final int[] cellDims = new int[ n ];
		grid.getCellDimensions( index, cellMin, cellDims );
		final long[] cellMax = IntStream.range( 0, n ).mapToLong( d -> cellMin[ d ] + cellDims[ d ] - 1 ).toArray();
		final FinalInterval cellInterval = new FinalInterval( cellMin, cellMax );

		final int blocksize = ( int ) Intervals.numElements( cellDims );
		final VolatileShortArray array = new VolatileShortArray( blocksize, true );

		final Img< UnsignedShortType > img = ArrayImgs.unsignedShorts( array.getCurrentStorageArray(), Util.int2long( cellDims ) );
		final ArrayList< RandomAccessibleInterval< T > > featureBlocks = new ArrayList<>();
		for ( final RandomAccessible< T > f : this.features )
			featureBlocks.add( Views.interval( f, cellInterval ) );

		final InstanceView< T, ? > instances = new InstanceView<>( Views.collapseReal( Views.stack( featureBlocks ) ), InstanceView.makeDefaultAttributes( features.size(), numClasses ) );

		final Cursor< Instance > instancesCursor = Views.interval( instances, cellInterval ).cursor();
		final Cursor< UnsignedShortType > imgCursor = img.cursor();
		while ( imgCursor.hasNext() )
			imgCursor.next().set( 1 - ( int ) classifier.classifyInstance( instancesCursor.next() ) );

		return new Cell<>( cellDims, cellMin, array );
	}
}