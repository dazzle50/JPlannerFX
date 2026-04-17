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

package rjc.jplanner.gui.calendars;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.application.Platform;
import rjc.jplanner.gui.PlanContext;
import rjc.jplanner.plan.calenders.Calendar;
import rjc.jplanner.plan.calenders.Calendar.FIELD;
import rjc.jplanner.plan.calenders.Calendars;
import rjc.table.data.IDataInsertDeleteColumns;
import rjc.table.data.IDataSwapColumns;
import rjc.table.data.TableData;
import rjc.table.view.Colours;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/**************************** Table data source for showing calendars ****************************/
/*************************************************************************************************/

public class CalendarsData extends TableData implements IDataSwapColumns, IDataInsertDeleteColumns
{
  private Calendars  m_calendars;      // array of calendars to be shown on table
  private CellVisual m_disabledVisual; // cell visuals for disabled cells

  /**************************************** constructor ******************************************/
  public CalendarsData( Calendars calendars )
  {
    // construct data model
    m_calendars = calendars;
    setColumnCount( m_calendars.size() );
    setRowCount( calculateRowCount() );

    // visual for disabled cells
    m_disabledVisual = new CellVisual();
    m_disabledVisual.cellBackground = Colours.CELL_DISABLED_BACKGROUND;
    m_disabledVisual.textPaint = null;
  }

  /************************************** calculateRowCount **************************************/
  private int calculateRowCount()
  {
    // if max cycles not yet determined, check each calendar to find most
    int maxCycles = -1;
    for ( Calendar cal : m_calendars )
      if ( cal.getNormals().size() > maxCycles )
        maxCycles = cal.getNormals().size();

    // return calculated row count
    return FIELD.Normal.ordinal() + maxCycles;
  }

  /****************************************** getValue *******************************************/
  @Override
  public Object getValue( int dataColumn, int dataRow )
  {
    // return blank for corner cell
    if ( dataRow == HEADER && dataColumn == HEADER )
      return null;

    // return row header
    if ( dataColumn == HEADER )
    {
      // return normal and number
      int normal = FIELD.Normal.ordinal();
      if ( dataRow >= normal )
        return FIELD.values()[normal] + " " + ( dataRow - normal + 1 );

      // otherwise return field name
      return FIELD.values()[dataRow];
    }

    // return column header
    if ( dataRow == HEADER )
      return "Calendar " + ( dataColumn + 1 );

    // otherwise return value from calendars array
    return m_calendars.get( dataColumn ).getValue( dataRow );
  }

  /***************************************** getVisual *******************************************/
  @Override
  public CellVisual getVisual( int dataColumn, int dataRow )
  {
    // return disabled-cells-visual for non-initials field if initials is blank
    if ( dataColumn > HEADER && dataRow > FIELD.Normal.ordinal() )
      if ( m_calendars.get( dataColumn ).isBlank( dataRow ) )
        return m_disabledVisual;

    // otherwise return default cell visuals
    return super.getVisual( dataColumn, dataRow );
  }

  /****************************************** setValue *******************************************/
  @Override
  protected String setValue( int dataColumn, int dataRow, Object newValue, boolean commit )
  {
    // after committing new cycle-length recalculate table row count
    if ( commit && dataRow == Calendar.FIELD.Cycle.ordinal() )
      Platform.runLater( () -> setRowCount( calculateRowCount() ) );

    // test if value can/could be set
    return m_calendars.setValue( dataColumn, dataRow, newValue, commit );
  }

  /***************************************** swapColumns *****************************************/
  /**
   * IDataSwapColumns - Swaps two columns in the data model to support column reordering/sorting.
   *
   * @param column1 data-based index of the first column
   * @param column2 data-based index of the second column
   * @return {@code true} always, as the swap cannot fail
   */
  @Override
  public boolean swapColumns( int column1, int column2 )
  {
    Collections.swap( m_calendars, column1, column2 );
    return true;
  }

  /**************************************** insertColumns ****************************************/
  @Override
  public boolean insertColumns( int insertIndex, List<Object> columnData )
  {
    // build the list of columns to insert, substituting defaults for null elements
    var toInsert = new ArrayList<Calendar>( columnData.size() );
    var defaultDay = ( (PlanContext) getUserData() ).getPlan().getDay( 0 );
    for ( var data : columnData )
      toInsert.add( data == null ? new Calendar( defaultDay ) : (Calendar) data );

    // insert the new columns and update column count
    m_calendars.addAll( insertIndex, toInsert );
    setColumnCount( getColumnCount() + toInsert.size() );

    // ensure all inserted calendars have unique names by appending suffix if necessary
    for ( int i = 0; i < toInsert.size(); i++ )
    {
      var calendar = toInsert.get( i );
      String name = calendar.getName();
      int suffix = 1;
      while ( setValue( insertIndex + i, FIELD.Name.ordinal(), name, true ) != null )
        name = calendar.getName() + " " + suffix++;
    }

    return true;
  }

  /**************************************** deleteColumns ****************************************/
  @Override
  public List<Object> deleteColumns( int deleteIndex, int count )
  {
    // snapshot the calendars being removed before clearing the sublist
    var deleted = new ArrayList<Object>( m_calendars.subList( deleteIndex, deleteIndex + count ) );
    m_calendars.subList( deleteIndex, deleteIndex + count ).clear();
    setColumnCount( getColumnCount() - count );
    return deleted;
  }

  /************************************ checkColumnsDeletable ************************************/
  @Override
  public String checkColumnsDeletable( int[] dataColumns )
  {
    // check if any of the specified calendars are used elsewhere in plan
    var messages = new ArrayList<String>();
    var plan = ( (PlanContext) getUserData() ).getPlan();

    for ( int dataColumn : dataColumns )
    {
      var calendar = m_calendars.get( dataColumn );
      var name = calendar.getName();
      if ( calendar == plan.getDefaultCalendar() )
        messages.add( "Calendar '" + name + "' is the plan default calendar" );

      for ( var resource : plan.getResources() )
        if ( calendar == resource.getCalendar() )
          messages.add( "Calendar '" + name + "' is used by resource '" + resource.getInitials() + "'" );
    }

    if ( !messages.isEmpty() )
      return String.join( "\n", messages );

    // if we get here, all calendars are deletable
    return null;
  }

}
