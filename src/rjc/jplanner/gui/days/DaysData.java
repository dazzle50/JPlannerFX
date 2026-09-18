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

package rjc.jplanner.gui.days;

import java.time.DateTimeException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import javafx.application.Platform;
import javafx.geometry.Pos;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.days.DayType;
import rjc.jplanner.plan.days.DayTypes;
import rjc.jplanner.plan.days.WorkPeriod;
import rjc.table.data.IDataInsertDeleteRows;
import rjc.table.data.IDataSwapRows;
import rjc.table.data.TableData;
import rjc.table.data.types.Time;
import rjc.table.view.Colours;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/**************************** Table data source for showing day-types ****************************/
/*************************************************************************************************/

public class DaysData extends TableData implements IDataSwapRows, IDataInsertDeleteRows
{
  // table fields are defined here to keep the day-type model independent of column layout
  public enum FIELD
  {
    Name, Work, Periods, Start, End
  }

  private final DayTypes   m_days;                              // day-types to be shown on table
  private final CellVisual m_disabledVisual = new CellVisual(); // cell visuals for disabled cells

  /**************************************** constructor ******************************************/
  public DaysData( DayTypes days )
  {
    // construct data model
    m_days = Objects.requireNonNull( days );
    setRowCount( m_days.size() );
    setColumnCount( calculateColumnCount() );

    // visual for disabled cells
    m_disabledVisual.cellBackground = Colours.CELL_DISABLED_BACKGROUND;
    m_disabledVisual.textPaint = null;
  }

  /************************************ calculateColumnCount *************************************/
  private int calculateColumnCount()
  {
    // find most work-periods, retaining Name, Work and Periods columns even for an empty table
    int maxPeriods = 0;
    for ( DayType day : m_days )
      maxPeriods = Math.max( maxPeriods, day.getWorkPeriods().size() );

    // return calculated column count
    return FIELD.Start.ordinal() + 2 * maxPeriods;
  }

  /**************************************** updateColumns ****************************************/
  private void updateColumns()
  {
    // recalculate column count after the current cell editor has finished its commit
    Platform.runLater( () -> setColumnCount( calculateColumnCount() ) );
  }

  /****************************************** getValue *******************************************/
  @Override
  public Object getValue( int dataColumn, int dataRow )
  {
    // return blank for corner cell
    if ( dataRow == HEADER && dataColumn == HEADER )
      return null;

    // return column header with field name or start/end and period number
    if ( dataRow == HEADER )
      return dataColumn < FIELD.Start.ordinal() ? FIELD.values()[dataColumn]
          : ( ( dataColumn - FIELD.Start.ordinal() ) % 2 == 0 ? "Start " : "End " )
              + ( 1 + ( dataColumn - FIELD.Start.ordinal() ) / 2 );

    // return row header
    if ( dataColumn == HEADER )
      return dataRow + 1;

    // otherwise return value from day-type
    DayType day = m_days.get( dataRow );
    if ( dataColumn == FIELD.Name.ordinal() )
      return day.getName();
    if ( dataColumn == FIELD.Work.ordinal() )
      return day.getEquivalentDays();
    if ( dataColumn == FIELD.Periods.ordinal() )
      return day.getWorkPeriods().size();

    // return work-period start or end, or blank if the period is not defined
    int index = ( dataColumn - FIELD.Start.ordinal() ) / 2;
    if ( index >= day.getWorkPeriods().size() )
      return null;
    WorkPeriod period = day.getWorkPeriods().get( index );
    return ( dataColumn - FIELD.Start.ordinal() ) % 2 == 0 ? period.getStart() : period.getEnd();
  }

  /***************************************** getVisual *******************************************/
  @Override
  public CellVisual getVisual( int dataColumn, int dataRow )
  {
    // disable undefined period cells and hide Work on empty days without changing its stored value
    if ( dataRow > HEADER && ( dataColumn >= FIELD.Start.ordinal() + 2 * m_days.get( dataRow ).getWorkPeriods().size()
        || dataColumn == FIELD.Work.ordinal() && !m_days.get( dataRow ).hasWorkingTime() ) )
      return m_disabledVisual;

    // otherwise return adjusted default cell visuals
    CellVisual visual = super.getVisual( dataColumn, dataRow );
    visual.textAlignment = dataColumn == FIELD.Name.ordinal() ? Pos.CENTER_LEFT : Pos.CENTER;
    return visual;
  }

  /****************************************** setValue *******************************************/
  @Override
  protected String setValue( int dataColumn, int dataRow, Object newValue, boolean commit )
  {
    // test if value can/could be set, changing the day-type only when committing
    try
    {
      DayType day = m_days.get( dataRow );
      if ( dataColumn == FIELD.Name.ordinal() )
      {
        // validate cleaned name and uniqueness before committing
        String name = m_days.validatedRename( day, newValue == null ? "" : newValue.toString() );
        if ( commit && !name.equals( day.getName() ) )
          m_days.rename( day, name );
        return null;
      }

      if ( dataColumn == FIELD.Work.ordinal() )
      {
        // check equivalent-day weight is a valid number
        if ( !( newValue instanceof Double weight ) )
          throw new IllegalArgumentException( "Work must be a number" );
        DayType.validateEquivalentDays( weight );

        if ( commit )
          day.setEquivalentDays( weight );
        return null;
      }

      List<WorkPeriod> proposal;
      if ( dataColumn == FIELD.Periods.ordinal() )
      {
        // preview accepts a count; committed edits and undo/redo require complete period lists
        if ( newValue instanceof Integer count )
        {
          proposal = proposedPeriodCount( dataRow, count );

          // reject count commits because a scalar undo command cannot restore the original times
          if ( commit )
            throw new IllegalArgumentException( "Cannot commit integer" );
        }
        else if ( newValue instanceof List<?> list )
        {
          // check each list element is a work-period before validating the complete list
          var typed = new ArrayList<WorkPeriod>();
          for ( Object element : list )
          {
            if ( !( element instanceof WorkPeriod period ) )
              throw new IllegalArgumentException( "Expected a work period" );
            typed.add( period );
          }
          proposal = DayType.validatedPeriods( typed );
        }
        else
          throw new IllegalArgumentException( "Expected a period count or list" );
      }
      else
      {
        // check work-period start or end time
        if ( !( newValue instanceof Time time ) )
          throw new IllegalArgumentException( "Expected a time" );
        int index = ( dataColumn - FIELD.Start.ordinal() ) / 2;
        var replacement = new ArrayList<>( day.getWorkPeriods() );
        if ( index < 0 || index >= replacement.size() )
          throw new IllegalArgumentException( "Work period is not defined" );

        WorkPeriod old = replacement.get( index );
        // replace the whole period so saved undo lists retain their original times
        replacement.set( index, ( dataColumn - FIELD.Start.ordinal() ) % 2 == 0 ? new WorkPeriod( time, old.getEnd() )
            : new WorkPeriod( old.getStart(), time ) );
        proposal = DayType.validatedPeriods( replacement );
      }

      // commit changed work-periods and update columns only when necessary
      if ( commit && !proposal.equals( day.getWorkPeriods() ) )
      {
        day.setWorkPeriods( proposal );
        if ( dataColumn == FIELD.Periods.ordinal() )
          updateColumns();
      }
      return null;
    }
    catch ( IllegalArgumentException | NullPointerException | DateTimeException exception )
    {
      // return validation problem as an editor message
      return exception.getMessage();
    }
  }

  /************************************* proposedPeriodCount *************************************/
  // generate an immutable validated period list for editor preview and the period-count command
  // row is a data index; reject counts outside 0 through 8 or additions that cannot fit
  public List<WorkPeriod> proposedPeriodCount( int row, int count )
  {
    // check proposed count is in range
    if ( count < 0 || count > 8 )
      throw new IllegalArgumentException( "Number of periods must be between 0 and 8" );

    var proposed = new ArrayList<>( m_days.get( row ).getWorkPeriods() );
    // reduce count without changing retained period times, including seconds and milliseconds
    if ( count <= proposed.size() )
      return List.copyOf( proposed.subList( 0, count ) );

    int end = proposed.isEmpty() ? 0 : proposed.getLast().getEnd().toMillisecondsOfDay();
    int available = Time.MILLIS_PER_DAY - end;
    // calculate equal work/gap increments with space after the last generated period
    int step = available / ( 1 + 2 * ( count - proposed.size() ) );

    // choose familiar editor spacing in minutes; the day-type model also permits adjacency
    int[] choices = { 480, 240, 120, 60, 30, 10, 5, 1 };
    int increment = Time.MILLIS_PER_MINUTE;
    for ( int minutes : choices )
      if ( step >= minutes * Time.MILLIS_PER_MINUTE )
      {
        increment = minutes * Time.MILLIS_PER_MINUTE;
        break;
      }

    // check space for one-minute periods and gaps, allowing the final period to end at 24:00
    if ( ( 2 * ( count - proposed.size() ) ) * increment > available )
      throw new IllegalArgumentException( "Cannot add work periods with a one-minute period and gap" );

    // add new work-periods
    int start = end + increment;
    while ( proposed.size() < count )
    {
      proposed.add( new WorkPeriod( Time.ofMilliseconds( start ), Time.ofMilliseconds( start + increment ) ) );
      start += 2 * increment;
    }
    return DayType.validatedPeriods( proposed );
  }

  /****************************************** swapRows *******************************************/
  /**
   * IDataSwapRows - Swaps two rows in the data model to support row reordering/sorting.
   *
   * @param row1  data-based index of the first row
   * @param row2  data-based index of the second row
   * @return {@code true} after swapping the rows
   * @throws IndexOutOfBoundsException if either row index is out of range
   */
  @Override
  public boolean swapRows( int row1, int row2 )
  {
    // swap day-types in the collection
    m_days.swap( row1, row2 );
    return true;
  }

  /**************************************** prepareRows ******************************************/
  // prepare an immutable row list without inserting, preserving supplied day-type objects
  // reject a null list, invalid row types or duplicate names; null elements receive defaults
  private List<DayType> prepareRows( List<Object> rowData )
  {
    // collect existing day-type names
    Objects.requireNonNull( rowData, "rowData" );
    var names = new HashSet<String>();
    for ( DayType day : m_days )
      names.add( day.getName() );

    // reserve supplied names first so generated defaults cannot take a later supplied row's name
    for ( Object value : rowData )
      if ( value != null )
      {
        if ( !( value instanceof DayType day ) )
          throw new IllegalArgumentException( "Expected a day type" );
        if ( !names.add( day.getName() ) )
          throw new IllegalArgumentException( "Duplicate day type or name: " + day.getName() );
      }

    // build the list of rows to insert, substituting defaults for null elements
    var result = new ArrayList<DayType>();
    for ( Object value : rowData )
      if ( value instanceof DayType day )
        result.add( day );
      else
      {
        String name = "New";
        // append suffix until unique and reserve the name for the rest of the batch
        for ( int suffix = 1; !names.add( name ); suffix++ )
          name = "New " + suffix;
        result.add( new DayType( name, 0, List.of() ) );
      }
    return List.copyOf( result );
  }

  /***************************************** insertRows ******************************************/
  /**
   * IDataInsertDeleteRows - Inserts one or more rows at the specified position.
   *
   * @param insertIndex  data-based insertion index, from 0 through the current row count
   * @param rowData      non-null list of day-types; {@code null} elements receive default content
   * @return {@code true} if at least one row was inserted, {@code false} if the list is empty
   * @throws IndexOutOfBoundsException if {@code insertIndex} is out of range
   * @throws IllegalArgumentException if supplied rows have invalid types or duplicate names
   * @throws NullPointerException if {@code rowData} is {@code null}
   */
  @Override
  public boolean insertRows( int insertIndex, List<Object> rowData )
  {
    // prepare and validate all rows before changing the collection
    Objects.checkIndex( insertIndex, m_days.size() + 1 );
    List<DayType> prepared = prepareRows( rowData );
    if ( prepared.isEmpty() )
      return false;

    m_days.insertAll( insertIndex, prepared );

    // update table dimensions after successful insertion
    setRowCount( m_days.size() );
    updateColumns();
    return true;
  }

  /***************************************** deleteRows ******************************************/
  /**
   * IDataInsertDeleteRows - Deletes a contiguous range of rows from the data model.
   *
   * @param deleteIndex  data-based index of the first row to delete
   * @param count        number of consecutive rows to delete; must be &gt; 0
   * @return list of the deleted day-type objects for undo
   * @throws IndexOutOfBoundsException if the requested range is outside the collection
   * @throws IllegalArgumentException if {@code count} is not positive
   */
  @Override
  public List<Object> deleteRows( int deleteIndex, int count )
  {
    // user deletion is checked by checkRowsDeletable; undo also needs this low-level removal
    if ( count <= 0 )
      throw new IllegalArgumentException( "Count must be positive" );

    var removed = new ArrayList<Object>( m_days.removeRange( deleteIndex, count ) );
    setRowCount( m_days.size() );
    updateColumns();
    return removed;
  }

  /************************************** checkRowsDeletable *************************************/
  /**
   * IDataInsertDeleteRows - Checks if the specified rows can be deleted.
   *
   * @param dataRows  array of data-row indices to check for deletability
   * @return {@code null} if all rows are deletable, otherwise a message describing why the
   *         specified rows cannot be deleted
   */
  @Override
  public String checkRowsDeletable( int[] dataRows )
  {
    // check if any of the specified day-types are used by calendar cycles or date exceptions
    var messages = new ArrayList<String>();
    var calendars = ( (PlanContext) getUserData() ).getPlan().getCalendars();
    for ( int dataRow : dataRows )
    {
      DayType day = m_days.get( dataRow );
      var usedBy = calendars.stream()
          .filter( calendar -> calendar.getNormals().contains( day ) || calendar.getExceptions().containsValue( day ) )
          .map( calendar -> "'" + calendar.getName() + "'" ).toList();
      if ( !usedBy.isEmpty() )
        messages.add( "Day-type '" + day.getName() + "' is used by calendar " + String.join( ", ", usedBy ) );
    }

    // return any deletion problems, or null if all day-types are deletable
    return messages.isEmpty() ? null : String.join( "\n", messages );
  }

}
