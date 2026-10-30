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
import java.util.Map;

import javafx.geometry.Pos;
import rjc.jplanner.plan.calendars.Calendar;
import rjc.jplanner.plan.days.DayType;
import rjc.jplanner.plan.days.DayTypes;
import rjc.table.data.TableData;
import rjc.table.data.types.Date;
import rjc.table.view.cell.CellVisual;

/*************************************************************************************************/
/************************ Chronological table of draft calendar exceptions ***********************/
/*************************************************************************************************/

public class CalendarExceptionsData extends TableData
{
  private final Map<Date, DayType> m_exceptions;                // draft owned by the dialog
  private final DayTypes           m_days;                      // plan-owned choices accepted by the editor
  private final ArrayList<Date>    m_dates = new ArrayList<>(); // dates in display order

  /**************************************** constructor ******************************************/
  public CalendarExceptionsData( Map<Date, DayType> exceptions, DayTypes days )
  {
    m_exceptions = exceptions;
    m_days = days;
    setColumnCount( 2 );
    refresh();
  }

  /******************************************* refresh *******************************************/
  public void refresh()
  {
    // sort only after an edit has committed and its cell editor has closed
    m_dates.clear();
    m_dates.addAll( m_exceptions.keySet() );
    Collections.sort( m_dates );
    setRowCount( m_dates.size() );
    signalTableChanged();
  }

  /******************************************* getDate *******************************************/
  public Date getDate( int row )
  {
    return row >= 0 && row < m_dates.size() ? m_dates.get( row ) : null;
  }

  /******************************************* indexOf *******************************************/
  public int indexOf( Date date )
  {
    // a date edit may temporarily leave the rows unsorted until its editor has closed
    return m_dates.indexOf( date );
  }

  /****************************************** getValue *******************************************/
  @Override
  public Object getValue( int dataColumn, int dataRow )
  {
    if ( dataColumn == HEADER )
      return dataRow == HEADER ? null : dataRow + 1;
    if ( dataRow == HEADER )
      return dataColumn == 0 ? "Date" : "Exception day type";

    Date date = getDate( dataRow );
    if ( date == null )
      return null;
    return dataColumn == 0 ? date : m_exceptions.get( date );
  }

  /****************************************** setValue *******************************************/
  @Override
  protected String setValue( int dataColumn, int dataRow, Object value, boolean commit )
  {
    Date oldDate = getDate( dataRow );
    if ( oldDate == null )
      return "Select an exception to edit";
    if ( dataColumn == 0 )
    {
      if ( !( value instanceof Date date ) )
        return "Enter a valid date";
      if ( !date.equals( oldDate ) && m_exceptions.containsKey( date ) )
        return "An exception already exists for this date";
      if ( commit && !date.equals( oldDate ) )
      {
        // move the existing exception; do not overwrite another date or duplicate the old entry
        DayType day = m_exceptions.remove( oldDate );
        m_exceptions.put( date, day );
        m_dates.set( dataRow, date );
      }
      return null;
    }

    if ( !( value instanceof DayType day ) || m_days.indexOf( day ) < 0 )
      return "Choose a day type from this plan";
    if ( commit )
      m_exceptions.put( oldDate, day );
    return null;
  }

  /**************************************** addException *****************************************/
  public Date addException( Date date, Calendar calendar )
  {
    // find the next unused date without stepping past the supported date range
    while ( m_exceptions.containsKey( date ) )
    {
      if ( date.equals( Date.MAX_VALUE ) )
        return null;
      date = date.plusDays( 1 );
    }
    // use the normal cycle even if this draft removed an exception still present in the live calendar
    m_exceptions.put( date, calendar.getNormalDayType( date ) );
    refresh();
    return date;
  }

  /************************************** removeExceptions ***************************************/
  public void removeExceptions( List<Date> dates )
  {
    for ( Date date : dates )
      m_exceptions.remove( date );
    refresh();
  }

  /***************************************** getVisual *******************************************/
  @Override
  public CellVisual getVisual( int dataColumn, int dataRow )
  {
    var visual = super.getVisual( dataColumn, dataRow );
    visual.textAlignment = dataColumn == HEADER ? Pos.CENTER : Pos.CENTER_LEFT;
    return visual;
  }
}
