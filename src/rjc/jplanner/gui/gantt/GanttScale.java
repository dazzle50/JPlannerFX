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

package rjc.jplanner.gui.gantt;

import javafx.beans.property.DoubleProperty;
import rjc.table.data.types.DateTime;
import rjc.table.data.types.Time;

/*************************************************************************************************/
/*********************** GanttScale define gantt start date-time and scale ***********************/
/*************************************************************************************************/

public class GanttScale
{
  private long           m_start;          // gantt start date-time in milliseconds
  private long           m_millisecondsPP; // milliseconds per pixel
  private DoubleProperty m_offset;         // offset from gantt scroll-bar value

  /**************************************** constructor ******************************************/
  public GanttScale( DateTime start, DoubleProperty scrollBarOffset )
  {
    // initialise the gantt scale
    m_start = start.getMilliseconds();
    m_offset = scrollBarOffset;
    m_millisecondsPP = 6 * Time.ONE_HOUR; // default to 6 hours per pixel
  }

  /****************************************** setScale *******************************************/
  public void setScale( DateTime start, DateTime end, int widthPixels )
  {
    // determine milliseconds per pixel after checking date-times
    if ( end.getMilliseconds() - start.getMilliseconds() < widthPixels )
      throw new IllegalArgumentException( "Invalid scale " + start + " " + end + " " + widthPixels );

    m_start = start.getMilliseconds();
    m_millisecondsPP = ( end.getMilliseconds() - m_start ) / widthPixels;
  }

  /********************************************** x **********************************************/
  public int x( DateTime dt )
  {
    // return x-coordinate from date-time [TODO stretch date-time to full day ?]
    long dtMilliseconds = dt.getMilliseconds();

    if ( dtMilliseconds > m_start )
      return (int) ( ( dtMilliseconds - m_start ) / m_millisecondsPP - m_offset.get() );
    return (int) ( ( m_start - dtMilliseconds ) / -m_millisecondsPP - m_offset.get() );
  }

  /****************************************** datetime *******************************************/
  public DateTime datetime( int x )
  {
    // return date-time for specified x-coordinate
    return new DateTime( m_start + ( x + (int) m_offset.get() ) * m_millisecondsPP );
  }

}
