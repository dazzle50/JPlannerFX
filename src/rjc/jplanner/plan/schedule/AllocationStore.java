/**************************************************************************
 *  Copyright (C) 2026 by Richard Crook                                   *
 *  https://github.com/dazzle50/JPlannerFX                                *
 *                                                                        *
 *  This program is free software: you can redistribute it and/or modify  *
 *  it under the terms of the GNU General Public License as published by  *
 *  the Free Software Foundation, either version 3 of the License, or     *
 *  (at your option) any later version.                                   *
 *                                                                        *
 *  This program is distributed in the hope that it will be useful,       *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *  GNU General Public License for more details.                          *
 *                                                                        *
 *  You should have received a copy of the GNU General Public License     *
 *  along with this program.  If not, see http://www.gnu.org/licenses/    *
 **************************************************************************/

package rjc.jplanner.plan.schedule;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Objects;

import rjc.jplanner.plan.resources.Resource;
import rjc.jplanner.plan.tasks.Task;
import rjc.table.data.types.DateTime;

/*************************************************************************************************/
/*********************** Immutable primitive rows of resource allocations ************************/
/*************************************************************************************************/

public final class AllocationStore
{
  private static final AllocationStore             EMPTY = new AllocationStore(); // shared initial store without participants

  private final Task[]                             m_tasks;                       // task identities in captured participant order
  private final Resource[]                         m_resources;                   // resource identities, with Unassigned at index zero
  private final IdentityHashMap<Task, Integer>     m_tasksByIdentity;             // participant lookup by task identity
  private final IdentityHashMap<Resource, Integer> m_resourcesByIdentity;         // participant lookup by resource identity
  private final int[]                              m_taskIndexes;                 // task participant indexes, independent of plan row numbers
  private final int[]                              m_resourceIndexes;             // resource participant indexes, with zero for Unassigned
  private final long[]                             m_starts;                      // inclusive serial-millisecond starts
  private final long[]                             m_ends;                        // exclusive serial-millisecond ends
  private final double[]                           m_quantities;                  // constant positive allocated quantities
  private final double[]                           m_workMillis;                  // credited resource-milliseconds, including completion clipping
  private final double[]                           m_workDays;                    // credited equivalent resource-days using effective calendars
  private final int[]                              m_taskRows;                    // canonical row indexes grouped by task
  private final int[]                              m_taskOffsets;                 // task slice boundaries, including the final row count
  private final int[]                              m_resourceRows;                // canonical row indexes grouped by resource
  private final int[]                              m_resourceOffsets;             // resource slice boundaries, including the final row count
  private final Selection                          m_all;                         // read-only view of all canonical rows
  private final Selection                          m_empty;                       // empty selection bound to this store

  /***************************************** constructor *****************************************/
  private AllocationStore()
  {
    // initialise immutable allocation columns, participant indexes and read-only selections
    m_tasks = new Task[0];
    m_resources = new Resource[0];
    m_tasksByIdentity = new IdentityHashMap<>();
    m_resourcesByIdentity = new IdentityHashMap<>();
    m_taskIndexes = new int[0];
    m_resourceIndexes = new int[0];
    m_starts = new long[0];
    m_ends = new long[0];
    m_quantities = new double[0];
    m_workMillis = new double[0];
    m_workDays = new double[0];
    m_taskRows = new int[0];
    m_taskOffsets = new int[1];
    m_resourceRows = new int[0];
    m_resourceOffsets = new int[1];
    m_all = new Selection( this, null, 0, 0 );
    m_empty = m_all;
  }

  /***************************************** constructor *****************************************/
  private AllocationStore( Builder builder )
  {
    // initialise immutable allocation columns, participant indexes and read-only selections
    // buffers belong exclusively to the consumed builder; trim only those with spare capacity
    int size = builder.m_size;
    m_tasks = builder.m_tasks;
    m_resources = builder.m_resources;
    m_tasksByIdentity = builder.m_tasksByIdentity;
    m_resourcesByIdentity = builder.m_resourcesByIdentity;
    m_taskIndexes = builder.m_taskIndexes.length == size ? builder.m_taskIndexes
        : Arrays.copyOf( builder.m_taskIndexes, size );
    m_resourceIndexes = builder.m_resourceIndexes.length == size ? builder.m_resourceIndexes
        : Arrays.copyOf( builder.m_resourceIndexes, size );
    m_starts = builder.m_starts.length == size ? builder.m_starts : Arrays.copyOf( builder.m_starts, size );
    m_ends = builder.m_ends.length == size ? builder.m_ends : Arrays.copyOf( builder.m_ends, size );
    m_quantities = builder.m_quantities.length == size ? builder.m_quantities
        : Arrays.copyOf( builder.m_quantities, size );
    m_workMillis = builder.m_workMillis.length == size ? builder.m_workMillis
        : Arrays.copyOf( builder.m_workMillis, size );
    m_workDays = builder.m_workDays.length == size ? builder.m_workDays : Arrays.copyOf( builder.m_workDays, size );
    for ( int row = 0; row < size; row++ )
      if ( m_starts[row] == m_ends[row] )
        throw new IllegalStateException( "Allocation row has no measured end: " + row );

    sortRows();
    m_taskOffsets = new int[m_tasks.length + 1];
    m_resourceOffsets = new int[m_resources.length + 1];
    m_taskRows = buildParticipantIndex( m_taskIndexes, m_taskOffsets );
    m_resourceRows = buildParticipantIndex( m_resourceIndexes, m_resourceOffsets );
    validateNoPairOverlaps();
    m_all = new Selection( this, null, 0, size );
    m_empty = size == 0 ? m_all : new Selection( this, null, 0, 0 );
  }

  /******************************************** empty ********************************************/
  public static AllocationStore empty()
  {
    // return the shared initial store with no rows or captured participants
    return EMPTY;
  }

  /************************************ buildParticipantIndex ************************************/
  private static int[] buildParticipantIndex( int[] participants, int[] offsets )
  {
    // group canonical row indexes by participant while retaining time order within each slice
    // count, prefix and fill preserve canonical order within each participant's slice
    for ( int participant : participants )
      offsets[participant + 1]++;
    for ( int index = 1; index < offsets.length; index++ )
      offsets[index] += offsets[index - 1];
    int[] next = offsets.clone();
    int[] rows = new int[participants.length];
    for ( int row = 0; row < participants.length; row++ )
      rows[next[participants[row]]++] = row;
    return rows;
  }

  /**************************************** compareRows ******************************************/
  private int compareRows( int first, int second )
  {
    // compare canonical order by start, task participant, resource participant and exclusive end
    int comparison = Long.compare( m_starts[first], m_starts[second] );
    if ( comparison == 0 )
      comparison = Integer.compare( m_taskIndexes[first], m_taskIndexes[second] );
    if ( comparison == 0 )
      comparison = Integer.compare( m_resourceIndexes[first], m_resourceIndexes[second] );
    return comparison != 0 ? comparison : Long.compare( m_ends[first], m_ends[second] );
  }

  /****************************************** sortRows *******************************************/
  private void sortRows()
  {
    // put all columns into canonical order without allocating per-interval row objects
    int size = size();
    int first = 1;
    while ( first < size && compareRows( first - 1, first ) <= 0 )
      first++;
    if ( first >= size )
      return;

    // merge primitive row indexes, then apply cycles without making seven further column copies
    int[] order = new int[size];
    int[] scratch = new int[size];
    for ( int row = 0; row < size; row++ )
      order[row] = row;
    for ( long width = 1; width < size; width *= 2 )
    {
      for ( long begin = 0; begin < size; begin += 2 * width )
      {
        int left = (int) begin;
        int middle = (int) Math.min( begin + width, size );
        int right = middle;
        int end = (int) Math.min( begin + 2 * width, size );
        for ( int output = left; output < end; output++ )
          scratch[output] = right == end || left < middle && compareRows( order[left], order[right] ) <= 0
              ? order[left++]
              : order[right++];
      }
      int[] swap = order;
      order = scratch;
      scratch = swap;
    }
    for ( int row = 0; row < size; row++ )
    {
      if ( order[row] < 0 )
        continue;
      int task = m_taskIndexes[row];
      int resource = m_resourceIndexes[row];
      long start = m_starts[row];
      long end = m_ends[row];
      double quantity = m_quantities[row];
      double work = m_workMillis[row];
      double days = m_workDays[row];
      int target = row;
      while ( order[target] != row )
      {
        int source = order[target];
        m_taskIndexes[target] = m_taskIndexes[source];
        m_resourceIndexes[target] = m_resourceIndexes[source];
        m_starts[target] = m_starts[source];
        m_ends[target] = m_ends[source];
        m_quantities[target] = m_quantities[source];
        m_workMillis[target] = m_workMillis[source];
        m_workDays[target] = m_workDays[source];
        order[target] = -1;
        target = source;
      }
      m_taskIndexes[target] = task;
      m_resourceIndexes[target] = resource;
      m_starts[target] = start;
      m_ends[target] = end;
      m_quantities[target] = quantity;
      m_workMillis[target] = work;
      m_workDays[target] = days;
      order[target] = -1;
    }
  }

  /************************************ validateNoPairOverlaps ***********************************/
  private void validateNoPairOverlaps()
  {
    // reject overlapping intervals for the same task/resource pair while permitting shared boundaries
    // resource slices are time ordered; stamps avoid clearing task state for every resource
    long[] lastEnd = new long[m_tasks.length];
    int[] seen = new int[m_tasks.length];
    for ( int resource = 0; resource < m_resources.length; resource++ )
      for ( int index = m_resourceOffsets[resource]; index < m_resourceOffsets[resource + 1]; index++ )
      {
        int row = m_resourceRows[index];
        int task = m_taskIndexes[row];
        if ( seen[task] == resource + 1 && m_starts[row] < lastEnd[task] )
          throw new IllegalArgumentException(
              "Overlapping allocation rows for task " + task + " and resource " + resource );
        seen[task] = resource + 1;
        lastEnd[task] = m_ends[row];
      }
  }

  /******************************************** size *********************************************/
  public int size()
  {
    // return the number of allocation rows accessible through this store or selection
    return m_taskIndexes.length;
  }

  /*********************************** getTaskParticipantCount ***********************************/
  public int getTaskParticipantCount()
  {
    // return the number of captured task identities, including participants with no allocation rows
    return m_tasks.length;
  }

  /********************************* getResourceParticipantCount *********************************/
  public int getResourceParticipantCount()
  {
    // return the number of captured resources, including Unassigned when participants exist
    return m_resources.length;
  }

  /************************************* getTaskParticipant **************************************/
  public Task getTaskParticipant( int index )
  {
    // return the captured task binding at a store-local participant index
    return m_tasks[index];
  }

  /*********************************** getResourceParticipant ************************************/
  public Resource getResourceParticipant( int index )
  {
    // return the captured resource binding at a store-local participant index
    return m_resources[index];
  }

  /**************************************** getAllocations ***************************************/
  public Selection getAllocations( Task task )
  {
    // select canonical rows for the supplied participant identities without copying allocation values
    Integer index = m_tasksByIdentity.get( Objects.requireNonNull( task, "task" ) );
    return index == null ? m_empty
        : new Selection( this, m_taskRows, m_taskOffsets[index], m_taskOffsets[index + 1] - m_taskOffsets[index] );
  }

  /**************************************** getAllocations ***************************************/
  public Selection getAllocations( Resource resource )
  {
    // select canonical rows for the supplied participant identities without copying allocation values
    Integer index = m_resourcesByIdentity.get( Objects.requireNonNull( resource, "resource" ) );
    return index == null ? m_empty
        : new Selection( this, m_resourceRows, m_resourceOffsets[index],
            m_resourceOffsets[index + 1] - m_resourceOffsets[index] );
  }

  /**************************************** getAllocations ***************************************/
  public Selection getAllocations( Task task, Resource resource )
  {
    // select canonical rows for the supplied participant identities without copying allocation values
    Selection tasks = getAllocations( task );
    Selection resources = getAllocations( resource );
    if ( tasks.size() == 0 || resources.size() == 0 )
      return m_empty;
    Selection smaller = tasks.size() <= resources.size() ? tasks : resources;
    return smaller.selectRows( m_tasksByIdentity.get( task ), m_resourcesByIdentity.get( resource ), Long.MIN_VALUE,
        Long.MAX_VALUE );
  }

  /**************************************** overlapping ******************************************/
  public Selection overlapping( long from, long to )
  {
    // select complete rows intersecting the half-open window without clipping their measured work
    return m_all.overlapping( from, to );
  }

  /**************************************** getInterval ******************************************/
  public AllocationInterval getInterval( int index )
  {
    // create an optional row handle only on request; numeric reads need no per-row object
    // create convenience handles only when requested; numeric reads use the columns directly
    return new AllocationInterval( this, index );
  }

  /*********************************** getTaskParticipantIndex ***********************************/
  public int getTaskParticipantIndex( int index )
  {
    // return the store-local task participant index for this allocation row
    return m_taskIndexes[index];
  }

  /********************************* getResourceParticipantIndex *********************************/
  public int getResourceParticipantIndex( int index )
  {
    // return the store-local resource participant index for this allocation row
    return m_resourceIndexes[index];
  }

  /******************************************* getTask *******************************************/
  public Task getTask( int index )
  {
    // return the captured task identity for this allocation row, independently of live plan ordering
    return m_tasks[m_taskIndexes[index]];
  }

  /***************************************** getResource *****************************************/
  public Resource getResource( int index )
  {
    // return the captured resource identity for this allocation row, including participant zero
    return m_resources[m_resourceIndexes[index]];
  }

  /*************************************** getStartMillis ****************************************/
  public long getStartMillis( int index )
  {
    // return the row's inclusive serial-millisecond start without constructing a datetime
    return m_starts[index];
  }

  /**************************************** getEndMillis *****************************************/
  public long getEndMillis( int index )
  {
    // return the row's exclusive serial-millisecond end without constructing a datetime
    return m_ends[index];
  }

  /**************************************** getQuantity ******************************************/
  public double getQuantity( int index )
  {
    // return the row's constant occupied capacity without recomputing resource availability
    return m_quantities[index];
  }

  /**************************************** getWorkMillis ****************************************/
  public double getWorkMillis( int index )
  {
    // return measured resource-millisecond credit independently of occupied time or later calendar edits
    // effort is in resource-milliseconds, independently of whole-millisecond timeline boundaries
    return m_workMillis[index];
  }

  /***************************************** getWorkDays *****************************************/
  public double getWorkDays( int index )
  {
    // return measured equivalent resource-days without recomputing or rounding the stored credit
    return m_workDays[index];
  }

  /****************************************** toString *******************************************/
  @Override
  public String toString()
  {
    // summarise row and participant counts in constant time without traversing allocation rows
    return "AllocationStore[intervals=" + size() + ", tasks=" + m_tasks.length + ", resources=" + m_resources.length
        + "]";
  }

  // read-only canonical rows retained through a store slice or an optional filtered row index.
  public static final class Selection
  {
    private final AllocationStore m_store;  // store retained independently of later Plan replacement
    private final int[]           m_rows;   // shared row index, or null for a canonical slice
    private final int             m_offset; // first selected position in the row index
    private final int             m_size;   // number of selected rows

    /***************************************** constructor *****************************************/
    private Selection( AllocationStore store, int[] rows, int offset, int size )
    {
      // retain a canonical slice or shared row index without copying the stored allocation columns
      m_store = store;
      m_rows = rows;
      m_offset = offset;
      m_size = size;
    }

    /******************************************** size *********************************************/
    public int size()
    {
      // return the number of allocation rows accessible through this store or selection
      return m_size;
    }

    /****************************************** getStore *******************************************/
    public AllocationStore getStore()
    {
      // return the retained store so this selection remains valid after a Plan allocation replacement
      return m_store;
    }

    /***************************************** getRowIndex *****************************************/
    public int getRowIndex( int index )
    {
      // translate a selection position to its canonical store row after checking the selection bounds
      int position = m_offset + Objects.checkIndex( index, m_size );
      return m_rows == null ? position : m_rows[position];
    }

    /**************************************** getInterval ******************************************/
    public AllocationInterval getInterval( int index )
    {
      // create an optional row handle only on request; numeric reads need no per-row object
      return m_store.getInterval( getRowIndex( index ) );
    }

    /*********************************** getTaskParticipantIndex ***********************************/
    public int getTaskParticipantIndex( int index )
    {
      // return the store-local task participant index for this allocation row
      return m_store.getTaskParticipantIndex( getRowIndex( index ) );
    }

    /********************************* getResourceParticipantIndex *********************************/
    public int getResourceParticipantIndex( int index )
    {
      // return the store-local resource participant index for this allocation row
      return m_store.getResourceParticipantIndex( getRowIndex( index ) );
    }

    /******************************************* getTask *******************************************/
    public Task getTask( int index )
    {
      // return the captured task identity for this allocation row, independently of live plan ordering
      return m_store.getTask( getRowIndex( index ) );
    }

    /***************************************** getResource *****************************************/
    public Resource getResource( int index )
    {
      // return the captured resource identity for this allocation row, including participant zero
      return m_store.getResource( getRowIndex( index ) );
    }

    /*************************************** getStartMillis ****************************************/
    public long getStartMillis( int index )
    {
      // return the row's inclusive serial-millisecond start without constructing a datetime
      return m_store.getStartMillis( getRowIndex( index ) );
    }

    /**************************************** getEndMillis *****************************************/
    public long getEndMillis( int index )
    {
      // return the row's exclusive serial-millisecond end without constructing a datetime
      return m_store.getEndMillis( getRowIndex( index ) );
    }

    /**************************************** getQuantity ******************************************/
    public double getQuantity( int index )
    {
      // return the row's constant occupied capacity without recomputing resource availability
      return m_store.getQuantity( getRowIndex( index ) );
    }

    /**************************************** getWorkMillis ****************************************/
    public double getWorkMillis( int index )
    {
      // return measured resource-millisecond credit independently of occupied time or later calendar edits
      return m_store.getWorkMillis( getRowIndex( index ) );
    }

    /***************************************** getWorkDays *****************************************/
    public double getWorkDays( int index )
    {
      // return measured equivalent resource-days without recomputing or rounding the stored credit
      return m_store.getWorkDays( getRowIndex( index ) );
    }

    /**************************************** overlapping ******************************************/
    public Selection overlapping( long from, long to )
    {
      // select complete rows intersecting the half-open window without clipping their measured work
      if ( from > to )
        throw new IllegalArgumentException( "Allocation window end precedes its start" );
      return from == to ? m_store.m_empty : selectRows( -1, -1, from, to );
    }

    /****************************************** selectRows *****************************************/
    private Selection selectRows( int task, int resource, long from, long to )
    {
      // filter this selection by participant indexes and time window while preserving canonical row order
      int count = 0;
      for ( int index = 0; index < m_size; index++ )
        if ( matchesRow( getRowIndex( index ), task, resource, from, to ) )
          count++;
      if ( count == 0 )
        return m_store.m_empty;
      if ( count == m_size )
        return this;

      // only filtered row indexes are materialised; the seven numeric columns remain shared
      int[] rows = new int[count];
      int output = 0;
      for ( int index = 0; index < m_size; index++ )
      {
        int row = getRowIndex( index );
        if ( matchesRow( row, task, resource, from, to ) )
          rows[output++] = row;
      }
      return new Selection( m_store, rows, 0, count );
    }

    /****************************************** matchesRow *****************************************/
    private boolean matchesRow( int row, int task, int resource, long from, long to )
    {
      // test participant filters and half-open overlap against the complete stored row
      return ( task < 0 || m_store.m_taskIndexes[row] == task )
          && ( resource < 0 || m_store.m_resourceIndexes[row] == resource ) && m_store.m_ends[row] > from
          && m_store.m_starts[row] < to;
    }
  }

  // single-use private buffers for measured intervals; building consumes them even when validation fails.
  public static final class Builder
  {
    private static final int                   MAX_CAPACITY = Integer.MAX_VALUE - 8; // conservative primitive-array bound

    private Task[]                             m_tasks;                              // captured task bindings
    private Resource[]                         m_resources;                          // captured resource bindings, with Unassigned first
    private IdentityHashMap<Task, Integer>     m_tasksByIdentity;                    // task participant lookup
    private IdentityHashMap<Resource, Integer> m_resourcesByIdentity;                // resource participant lookup
    private boolean[]                          m_assigned;                           // captured assignment presence for Unassigned validation
    private int[]                              m_taskIndexes;                        // private task column
    private int[]                              m_resourceIndexes;                    // private resource column
    private long[]                             m_starts;                             // private inclusive starts
    private long[]                             m_ends;                               // latest measured ends; equal to start until updated
    private double[]                           m_quantities;                         // private constant quantities
    private double[]                           m_workMillis;                         // private measured resource-millisecond credits
    private double[]                           m_workDays;                           // private measured equivalent-day credits
    private int                                m_size;                               // number of appended rows
    private boolean                            m_finished;                           // construction has been consumed, successfully or otherwise

    /***************************************** constructor *****************************************/
    public Builder( List<Task> tasks, List<Resource> resources )
    {
      // capture participant identities and allocate private buffers for a single-use interval builder
      this( tasks, resources, 16 );
    }

    /***************************************** constructor *****************************************/
    public Builder( List<Task> tasks, List<Resource> resources, int initialCapacity )
    {
      // capture participant identities and allocate private buffers for a single-use interval builder
      if ( initialCapacity < 0 || initialCapacity > MAX_CAPACITY )
        throw new IllegalArgumentException( "Invalid allocation capacity" );
      m_tasks = Objects.requireNonNull( tasks, "tasks" ).toArray( new Task[0] );
      m_resources = Objects.requireNonNull( resources, "resources" ).toArray( new Resource[0] );
      if ( m_resources.length == 0 )
        throw new IllegalArgumentException( "Resource participant zero is required" );
      m_tasksByIdentity = new IdentityHashMap<>();
      m_resourcesByIdentity = new IdentityHashMap<>();
      m_assigned = new boolean[m_tasks.length];
      for ( int index = 0; index < m_tasks.length; index++ )
      {
        Task task = Objects.requireNonNull( m_tasks[index], "task" );
        if ( m_tasksByIdentity.put( task, index ) != null )
          throw new IllegalArgumentException( "Duplicate task identity" );
        m_assigned[index] = task.getValue( Task.FIELD.Resources.ordinal() ) != null;
      }
      for ( int index = 0; index < m_resources.length; index++ )
        if ( m_resourcesByIdentity.put( Objects.requireNonNull( m_resources[index], "resource" ), index ) != null )
          throw new IllegalArgumentException( "Duplicate resource identity" );
      m_taskIndexes = new int[initialCapacity];
      m_resourceIndexes = new int[initialCapacity];
      m_starts = new long[initialCapacity];
      m_ends = new long[initialCapacity];
      m_quantities = new double[initialCapacity];
      m_workMillis = new double[initialCapacity];
      m_workDays = new double[initialCapacity];
    }

    /***************************************** addInterval *****************************************/
    public int addInterval( Task task, Resource resource, long start, long end, double quantity, double workMillis,
        double workDays )
    {
      // validate and append a measured interval, returning its builder row for later progress updates
      checkActive();
      Integer taskIndex = m_tasksByIdentity.get( Objects.requireNonNull( task, "task" ) );
      Integer resourceIndex = m_resourcesByIdentity.get( Objects.requireNonNull( resource, "resource" ) );
      if ( taskIndex == null || resourceIndex == null )
        throw new IllegalArgumentException( "Allocation participant was not captured by this builder" );
      return addInterval( taskIndex, resourceIndex, start, end, quantity, workMillis, workDays );
    }

    /***************************************** addInterval *****************************************/
    public int addInterval( int task, int resource, long start, long end, double quantity, double workMillis,
        double workDays )
    {
      // validate and append a measured interval, returning its builder row for later progress updates
      checkActive();
      validateIntervalStart( task, resource, start, quantity );
      validateMeasuredEnd( start, end, workMillis, workDays );
      return appendInterval( task, resource, start, end, quantity, workMillis, workDays );
    }

    /***************************************** openInterval ****************************************/
    public int openInterval( int task, int resource, long start, double quantity )
    {
      // reserve a builder row with no measured end yet; progress must be supplied before building the store
      checkActive();
      validateIntervalStart( task, resource, start, quantity );
      return appendInterval( task, resource, start, start, quantity, 0, 0 );
    }

    /**************************************** updateProgress ***************************************/
    public void updateProgress( int row, long end, double workMillis, double workDays )
    {
      // advance a builder row's end and cumulative credits without changing its start or occupied quantity
      checkActive();
      Objects.checkIndex( row, m_size );
      validateMeasuredEnd( m_starts[row], end, workMillis, workDays );
      if ( end < m_ends[row] || workMillis < m_workMillis[row] || workDays < m_workDays[row] )
        throw new IllegalArgumentException( "Measured allocation progress cannot move backwards" );
      m_ends[row] = end;
      m_workMillis[row] = workMillis;
      m_workDays[row] = workDays;
    }

    /************************************ validateIntervalStart ************************************/
    private void validateIntervalStart( int task, int resource, long start, double quantity )
    {
      // validate captured participant indexes, start range, positive quantity and Unassigned eligibility
      if ( task < 0 || task >= m_tasks.length || resource < 0 || resource >= m_resources.length )
        throw new IllegalArgumentException( "Allocation participant index is outside the captured tables" );
      if ( start < DateTime.MIN_VALUE.toSerialMilliseconds() || start >= DateTime.MAX_VALUE.toSerialMilliseconds() )
        throw new IllegalArgumentException( "Allocation start is outside the supported interval range" );
      if ( !Double.isFinite( quantity ) || quantity <= 0 )
        throw new IllegalArgumentException( "Allocation quantity must be finite and positive" );
      if ( resource == 0 && ( m_assigned[task] || quantity > 1 ) )
        throw new IllegalArgumentException( "Unassigned capacity requires an unassigned task and at most one unit" );
    }

    /************************************* validateMeasuredEnd *************************************/
    private static void validateMeasuredEnd( long start, long end, double workMillis, double workDays )
    {
      // validate a positive measured interval and finite non-negative work credits
      if ( end <= start || end > DateTime.MAX_VALUE.toSerialMilliseconds() )
        throw new IllegalArgumentException( "Allocation dates must form a supported positive interval" );
      if ( !Double.isFinite( workMillis ) || workMillis < 0 || !Double.isFinite( workDays ) || workDays < 0 )
        throw new IllegalArgumentException( "Allocation work must be finite and non-negative" );
    }

    /**************************************** appendInterval ***************************************/
    private int appendInterval( int task, int resource, long start, long end, double quantity, double workMillis,
        double workDays )
    {
      // append validated values to private columns, growing their buffers together when needed
      if ( m_size == MAX_CAPACITY )
        throw new IllegalStateException( "Too many allocation intervals" );
      if ( m_size == m_taskIndexes.length )
      {
        int capacity = (int) Math.min( MAX_CAPACITY, Math.max( (long) m_size + 1, m_size + (long) m_size / 2 ) );
        m_taskIndexes = Arrays.copyOf( m_taskIndexes, capacity );
        m_resourceIndexes = Arrays.copyOf( m_resourceIndexes, capacity );
        m_starts = Arrays.copyOf( m_starts, capacity );
        m_ends = Arrays.copyOf( m_ends, capacity );
        m_quantities = Arrays.copyOf( m_quantities, capacity );
        m_workMillis = Arrays.copyOf( m_workMillis, capacity );
        m_workDays = Arrays.copyOf( m_workDays, capacity );
      }
      int row = m_size++;
      m_taskIndexes[row] = task;
      m_resourceIndexes[row] = resource;
      m_starts[row] = start;
      m_ends[row] = end;
      m_quantities[row] = quantity;
      m_workMillis[row] = workMillis;
      m_workDays[row] = workDays;
      return row;
    }

    /******************************************** build ********************************************/
    public AllocationStore build()
    {
      // consume this builder and produce an immutable ordered store; failure also consumes the builder
      checkActive();
      m_finished = true;
      try
      {
        return new AllocationStore( this );
      }
      finally
      {
        // release every alias to adopted buffers, including on validation failure
        m_tasks = null;
        m_resources = null;
        m_tasksByIdentity = null;
        m_resourcesByIdentity = null;
        m_assigned = null;
        m_taskIndexes = null;
        m_resourceIndexes = null;
        m_starts = null;
        m_ends = null;
        m_quantities = null;
        m_workMillis = null;
        m_workDays = null;
      }
    }

    /***************************************** checkActive *****************************************/
    private void checkActive()
    {
      // reject every builder operation after construction has been consumed, including a failed build
      if ( m_finished )
        throw new IllegalStateException( "Allocation builder has been consumed" );
    }
  }
}
