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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.application.Platform;
import javafx.geometry.Pos;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.days.Day;
import rjc.jplanner.plan.days.Day.FIELD;
import rjc.jplanner.plan.days.Days;
import rjc.table.data.IDataInsertDeleteRows;
import rjc.table.data.IDataSwapRows;
import rjc.table.data.TableData;
import rjc.table.demo.edit.EditableTableRow;
import rjc.table.view.Colours;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/**************************** Table data source for showing day-types ****************************/
/*************************************************************************************************/

public class DaysData extends TableData implements IDataSwapRows, IDataInsertDeleteRows
{
  private Days       m_days;           // array of day-types to be shown on table
  private CellVisual m_disabledVisual; // cell visuals for disabled cells

  /**************************************** constructor ******************************************/
  public DaysData( Days days )
  {
    // construct data model
    m_days = days;
    setRowCount( m_days.size() );
    setColumnCount( calculateColumnCount() );

    // visual for disabled cells
    m_disabledVisual = new CellVisual();
    m_disabledVisual.cellBackground = Colours.CELL_DISABLED_BACKGROUND;
    m_disabledVisual.textPaint = null;
  }

  /************************************ calculateColumnCount *************************************/
  private int calculateColumnCount()
  {
    // if max periods not yet determined, check each day-type to find most
    int maxPeriods = -1;
    for ( Day day : m_days )
      if ( day.getWorkPeriods().size() > maxPeriods )
        maxPeriods = day.getWorkPeriods().size();

    // return calculated column count
    return Day.FIELD.Start.ordinal() + 2 * maxPeriods;
  }

  /****************************************** getValue *******************************************/
  @Override
  public Object getValue( int dataColumn, int dataRow )
  {
    // return blank for corner cell
    if ( dataRow == HEADER && dataColumn == HEADER )
      return null;

    // return column header
    if ( dataRow == HEADER )
    {
      // return start/end and period number
      if ( dataColumn >= FIELD.Start.ordinal() )
      {
        int count = ( dataColumn - FIELD.Start.ordinal() ) / 2;
        return FIELD.values()[dataColumn - count * 2] + " " + ( count + 1 );
      }

      // otherwise return field name
      return FIELD.values()[dataColumn];
    }

    // return row header
    if ( dataColumn == HEADER )
      return dataRow + 1;

    // otherwise return value from day-types array
    return m_days.get( dataRow ).getValue( dataColumn );
  }

  /***************************************** getVisual *******************************************/
  @Override
  public CellVisual getVisual( int dataColumn, int dataRow )
  {
    // return disabled-cells-visual for period starts/ends beyond those defined
    if ( dataRow > HEADER )
      if ( m_days.get( dataRow ).isDisabled( dataColumn ) )
        return m_disabledVisual;

    // otherwise return adjusted default cell visuals
    var visual = super.getVisual( dataColumn, dataRow );
    if ( dataColumn == Day.FIELD.Name.ordinal() )
      visual.textAlignment = Pos.CENTER_LEFT;
    else
      visual.textAlignment = Pos.CENTER;

    return visual;
  }

  /****************************************** setValue *******************************************/
  @Override
  protected String setValue( int dataColumn, int dataRow, Object newValue, boolean commit )
  {
    // after committing new work-periods recalculate table column count
    if ( commit && dataColumn == FIELD.Periods.ordinal() )
      Platform.runLater( () -> setColumnCount( calculateColumnCount() ) );

    // test if value can/could be set
    return m_days.setValue( dataRow, dataColumn, newValue, commit );
  }

  /****************************************** swapRows *******************************************/
  /**
   * IDataSwapRows - Swaps two rows in the data model to support row reordering/sorting.
   *
   * @param row1  data-based index of the first row
   * @param row2  data-based index of the second row
   * @return {@code true} always, as the swap cannot fail
   */
  @Override
  public boolean swapRows( int row1, int row2 )
  {
    Collections.swap( m_days, row1, row2 );
    return true;
  }

  /***************************************** insertRows ******************************************/
  /**
   * IDataInsertDeleteRows - Inserts one or more rows at the specified position.
   *
   * @param insertIndex  data-based index at which to insert; must be &gt;= 0
   * @param rowData      non-null list of row data; {@code null} elements receive default content
   * @return {@code true} if at least one row was inserted, {@code false} if the list is empty
   * @throws IllegalArgumentException if {@code insertIndex} is out of range or
   *                                  {@code rowData} is {@code null}
   */
  @Override
  public boolean insertRows( int insertIndex, List<Object> rowData )
  {
    // build the list of rows to insert, substituting defaults for null elements
    var toInsert = new ArrayList<Day>( rowData.size() );
    for ( var data : rowData )
      toInsert.add( data == null ? new Day() : (Day) data );

    // insert the new rows and update row count
    m_days.addAll( insertIndex, toInsert );
    setRowCount( getRowCount() + toInsert.size() );

    // ensure all inserted days have unique names by appending suffix if necessary
    for ( int i = 0; i < toInsert.size(); i++ )
    {
      var day = toInsert.get( i );
      String name = day.getName();
      int suffix = 1;
      while ( setValue( FIELD.Name.ordinal(), insertIndex + i, name, true ) != null )
        name = day.getName() + " " + suffix++;
    }

    return true;
  }

  /***************************************** deleteRows ******************************************/
  /**
   * IDataInsertDeleteRows - Deletes a contiguous range of rows from the data model.
   *
   * @param deleteIndex  data-based index of the first row to delete; must be &gt;= 0
   * @param count        number of consecutive rows to delete; must be &gt; 0
   * @return a list of the deleted {@link EditableTableRow} rows, or {@code null} if the
   *         operation failed
   * @throws IllegalArgumentException if {@code deleteIndex} is out of range or
   *                                  {@code count} is not positive
   */
  @Override
  public List<Object> deleteRows( int deleteIndex, int count )
  {
    // snapshot the days being removed before clearing the sublist
    var deleted = new ArrayList<Object>( m_days.subList( deleteIndex, deleteIndex + count ) );
    m_days.subList( deleteIndex, deleteIndex + count ).clear();
    setRowCount( getRowCount() - count );
    return deleted;
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
    // check if any of the specified day-types are used by any calendar
    var messages = new ArrayList<String>();
    var calendars = ( (PlanContext) getUserData() ).getPlan().getCalendars();

    for ( var dataRow : dataRows )
    {
      var day = m_days.get( dataRow );
      var usedBy = calendars.stream()
          .filter( calendar -> calendar.getNormals().contains( day ) || calendar.getExceptions().containsValue( day ) )
          .map( calendar -> "'" + calendar.getName() + "'" ).toList();

      if ( !usedBy.isEmpty() )
        messages.add( "Day-type '" + day.getName() + "' is used by calendar " + String.join( ", ", usedBy ) );
    }

    if ( !messages.isEmpty() )
      return String.join( "\n", messages );

    // if we get here, all day-types are deletable
    return null;
  }

}
