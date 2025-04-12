/**************************************************************************
 *  Copyright (C) 2025 by Richard Crook                                   *
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

import rjc.jplanner.plan.Calendar;
import rjc.jplanner.plan.Calendar.FIELD;
import rjc.jplanner.plan.Calendars;
import rjc.table.data.TableData;

/*************************************************************************************************/
/**************************** Table data source for showing calendars ****************************/
/*************************************************************************************************/

public class CalendarsData extends TableData
{
  Calendars m_calendars;      // array of calendars to be shown on table
  int       m_maxCycles = -1; // highest number of cycles that calendars have (or -1 if TBD)

  /**************************************** constructor ******************************************/
  public CalendarsData( Calendars calendars )
  {
    // construct data model
    m_calendars = calendars;
    setColumnCount( m_calendars.size() );
    setRowCount( calculateRowCount() );
  }

  /************************************** calculateRowCount **************************************/
  private int calculateRowCount()
  {
    // if max cycles not yet determined, check each calendar to find most
    if ( m_maxCycles < 0 )
      for ( Calendar cal : m_calendars )
        if ( cal.getNormals().size() > m_maxCycles )
          m_maxCycles = cal.getNormals().size();

    // return calculated column count
    return FIELD.Normal.ordinal() + m_maxCycles;
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

}
